package com.autobookkeeper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Handler
import android.os.Looper
import com.autobookkeeper.App
import com.autobookkeeper.data.Source
import com.autobookkeeper.data.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 无障碍服务：监听支付宝/微信支付成功页面，提取金额自动记账
 *
 * 工作原理：
 * 1. 监听窗口状态变化 (TYPE_WINDOW_STATE_CHANGED)
 * 2. 检测当前 Activity 包名（支付宝/微信）
 * 3. 从支付成功页提取金额文本
 * 4. 去重后写入数据库
 */
class PaymentAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "PaymentAS"

        /** 目标支付 App 包名 */
        val TARGET_APPS = setOf(
            "com.eg.android.AlipayGphone",        // 支付宝
            "com.tencent.mm"                       // 微信
        )

        /** 金额正则：匹配 ¥12.34、12.34元、金额 12.34 等 */
        private val AMOUNT_REGEX = Regex("""(?:[¥￥]|金额[：:]?|实付[：:]?)\s*(\d+\.?\d{0,2})""")
        private val AMOUNT_PLAIN_REGEX = Regex("""^(\d+\.\d{2})$""")
        private val AMOUNT_CLEAN_REGEX = Regex("""(\d+\.?\d{0,2})""")

        /** 去重缓存：同一金额+包名 30 秒内不重复记账 */
        private val dedupCache = ConcurrentHashMap<String, Long>()
        private const val DEDUP_WINDOW_MS = 30_000L

        /**
         * 检查无障碍服务是否已启用
         */
        fun isEnabled(context: android.content.Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val componentName = ComponentName(context, PaymentAccessibilityService::class.java)
            val flatName = componentName.flattenToString()
            val shortName = componentName.flattenToShortString()

            return enabledServices.contains(flatName) || enabledServices.contains(shortName)
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    /** 当前聚焦的窗口包名 */
    private var currentPackage = ""
    /** 上一个已处理的页面文本指纹（防止重复处理） */
    private var lastProcessedPage = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "无障碍服务已连接")
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 300

            // 只监听目标 App
            packageNames = TARGET_APPS.toTypedArray()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                        AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            }
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowChange(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // 支付成功页内容变化时再尝试
                handleWindowChange(event)
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    private fun handleWindowChange(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val className = event.className?.toString() ?: ""
        currentPackage = pkg

        // 先看页面文本是否有支付成功字样（不依赖 Activity 类名匹配）
        val root = rootInActiveWindow ?: return
        val pageText = collectAllText(root)
        root.recycle()

        if (pageText.isBlank()) return

        val lower = pageText.lowercase()
        val isPaymentPage = lower.contains("付款成功") || lower.contains("支付成功") ||
                lower.contains("交易成功") || lower.contains("完成付款")

        if (!isPaymentPage) return

        Log.d(TAG, "检测到支付成功页: pkg=$pkg, cls=$className")

        // 页面指纹去重
        val fingerprint = pageText.take(300)
        if (fingerprint == lastProcessedPage) {
            Log.d(TAG, "页面指纹重复，跳过")
            return
        }
        lastProcessedPage = fingerprint

        // 提取金额
        val amount = extractAmount(pageText) ?: run {
            Log.w(TAG, "支付页未提取到金额")
            return
        }
        Log.i(TAG, "提取到金额: $amount")

        // 去重：页面文本 hash + 金额，同页面同金额 10 分钟内不重复
        val dedupKey = "${pkg}_${amount}_${fingerprint.hashCode()}"
        val now = System.currentTimeMillis()
        val lastTime = dedupCache[dedupKey]
        if (lastTime != null && now - lastTime < 600_000L) {
            Log.d(TAG, "去重跳过: $dedupKey")
            return
        }
        dedupCache[dedupKey] = now
        // 清理超过 10 分钟的缓存
        dedupCache.entries.removeIf { now - it.value > 600_000L }

        // 推断来源和分类
        val source = when {
            pkg.contains("alipay") -> Source.ALIPAY
            pkg.contains("tencent.mm") || pkg.contains("微信") -> Source.WECHAT
            else -> Source.ALIPAY
        }
        val note = extractNote(pageText, source)

        // 写入数据库（延迟一点确保 UI 渲染完成）
        handler.postDelayed({
            val root2 = rootInActiveWindow
            if (root2 == null) {
                // 页面已关闭，仍用之前的 pageText 记账
                saveTransaction(pageText, amount, source, note)
                return@postDelayed
            }
            // 对微信支付，用多轮采集 + findAccessibilityNodeInfosByText 深度扫描
            val freshText = if (source == Source.WECHAT) {
                collectTextDeep(root2, collectAllText(root2))
            } else {
                collectAllText(root2)
            }
            root2.recycle()
            saveTransaction(freshText, amount, source, note)
        }, 300)
        // 微信 500ms 后再补一轮，等详情渲染完成
        if (source == Source.WECHAT) {
            handler.postDelayed({
                val root3 = rootInActiveWindow
                if (root3 == null) return@postDelayed
                val lateText = collectTextDeep(root3, collectAllText(root3))
                root3.recycle()
                saveTransaction(lateText, amount, source, note)
            }, 600)
        }
    }

    private fun saveTransaction(pageText: String, amount: Double, source: Source, note: String) {
        scope.launch {
            try {
                // 强匹配：用 pageText + note 合并判断，确保深度求索/API/云服务等不被遗漏
                val combined = (pageText + "\n" + note).lowercase()
                val finalCategory = when {
                    combined.contains("深度求索") || combined.contains("deepseek") ||
                            combined.contains("aliyun") || combined.contains("ningcloud") ||
                            combined.contains("huaweicloud") || combined.contains("云服务") ||
                            combined.contains("服务器") || combined.contains("域名") ||
                            combined.contains("token") || combined.contains("accesskey") ||
                            combined.contains("密钥") || combined.contains("开发者") ||
                            combined.contains("api") && !combined.contains("支付") &&
                            !combined.contains("收款") -> "API"
                    combined.contains("超市") || combined.contains("便利店") ||
                            combined.contains("惠顺多") || combined.contains("佳乐") -> "超市"
                    combined.contains("餐饮") || combined.contains("美食") ||
                            combined.contains("外卖") || combined.contains("饿了么") ||
                            combined.contains("美团外卖") || combined.contains("饭店") ||
                            combined.contains("餐厅") -> "餐饮"
                    combined.contains("医疗") || combined.contains("医院") ||
                            combined.contains("药店") || combined.contains("诊所") ||
                            combined.contains("海王") -> "医疗"
                    combined.contains("交通") || combined.contains("地铁") ||
                            combined.contains("公交") || combined.contains("滴滴") ||
                            combined.contains("出租车") || combined.contains("顺风车") ||
                            combined.contains("高德") -> "交通"
                    combined.contains("购物") || combined.contains("淘宝") ||
                            combined.contains("京东") || combined.contains("拼多多") ||
                            combined.contains("抖音") || combined.contains("数码") ||
                            combined.contains("电子") -> "购物"
                    combined.contains("充值") || combined.contains("话费") ||
                            combined.contains("宽带") || combined.contains("流量") ||
                            combined.contains("通信") -> "通信"
                    combined.contains("游戏") || combined.contains("steam") -> "游戏"
                    else -> {
                        // 最后用 classifyByPageText 的精细化分类逻辑（含来源推断等）
                        val baseCategory = classifyByPageText(pageText)
                        if (baseCategory != "其他") baseCategory else "其他"
                    }
                }
                val categoryIcon = getCategoryIcon(finalCategory)
                val txn = Transaction(
                    amount = -amount,
                    categoryName = finalCategory,
                    categoryIcon = categoryIcon,
                    source = source,
                    note = note,
                    date = System.currentTimeMillis()
                )
                App.instance.transactionDao.insert(txn)
                Log.i(TAG, "自动记账成功: ${amount}元, 分类=$finalCategory, 来源=$source")
            } catch (e: Exception) {
                Log.e(TAG, "自动记账失败", e)
            }
        }
    }

    /** 收集界面所有可见文本 */
    private fun collectAllText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        collectTextRecursive(node, sb)
        return sb.toString()
    }

    private fun collectTextRecursive(node: AccessibilityNodeInfo, sb: StringBuilder) {
        if (node == null) return

        // 获取文本
        if (!TextUtils.isEmpty(node.text)) {
            sb.append(node.text).append("\n")
        }
        if (!TextUtils.isEmpty(node.contentDescription)) {
            sb.append(node.contentDescription).append("\n")
        }

        // 如果有子节点就遍历，某些支付宝页面用自定义布局（子节点可能不可遍历）
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                collectTextRecursive(child, sb)
                child.recycle()
            }
        }
    }

    /** 从页面文本中提取金额 */
    private fun extractAmount(text: String): Double? {
        // 1. 优先匹配 ¥ / 金额: 前缀
        val match = AMOUNT_REGEX.find(text)
        if (match != null) {
            return match.groupValues[1].toDoubleOrNull()
        }

        // 2. 纯数字匹配（格式 X.XX）
        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.length in 4..12 && !trimmed.contains(Regex("""[^\d.]"""))) {
                val plain = AMOUNT_PLAIN_REGEX.find(trimmed)
                if (plain != null) {
                    val v = plain.groupValues[1].toDoubleOrNull()
                    if (v != null && v in 0.01..999999.0) return v
                }
            }
        }

        // 3. 暴力扫描整段文本中所有数字
        val allNumbers = AMOUNT_CLEAN_REGEX.findAll(text).map {
            it.groupValues[1].toDoubleOrNull()
        }.filterNotNull().filter { it in 0.01..999999.0 }

        // 取最大的那个（支付金额通常是页面上最大的数字）
        return allNumbers.maxOrNull()
    }

    /** 从页面文本中提取备注 */
    private fun extractNote(text: String, source: Source): String {
        // 尝试找商品名、商户名
        val lines = text.lines().map { it.trim() }.filter { it.length in 2..30 }
        val keywords = listOf("商品", "订单", "商户", "收款方", "商家", "购买")
        for (keyword in keywords) {
            val found = lines.firstOrNull { it.contains(keyword) }
            if (found != null) return found.take(20)
        }
        // 回退：取第一行非空且非金额的文本
        return lines.firstOrNull { it.length in 2..20 && !it.contains(Regex("""\d+\.\d{2}""")) }?.take(20) ?: ""
    }

    /** 根据页面文本推断分类 */
    private fun classifyByPageText(text: String): String {
        val lower = text.lowercase()
        return when {
            // API 优先（deepseek、阿里云、腾讯云、API 都归到 API 子分类）
            lower.contains("阿里云") || lower.contains("腾讯云") ||
                    lower.contains("华为云") || lower.contains("云服务") ||
                    lower.contains("deepseek") || lower.contains("深度求索") ||
                    lower.contains("api") || lower.contains("服务器") ||
                    lower.contains("域名") || lower.contains("对象存储") ||
                    lower.contains("cdn") || lower.contains("开放平台") ||
                    lower.contains("accesskey") || lower.contains("token") ||
                    lower.contains("开发者") || lower.contains("密钥") -> "API"
            // 通信
            lower.contains("充值") || lower.contains("话费") ||
                    lower.contains("通信") || lower.contains("流量") ||
                    lower.contains("宽带") -> "通信"
            // 餐饮
            lower.contains("餐饮") || lower.contains("美食") ||
                    lower.contains("外卖") || lower.contains("饿了么") ||
                    lower.contains("美团外卖") -> "餐饮"
            // 超市
            lower.contains("超市") || lower.contains("便利店") ||
                    lower.contains("惠顺多") || lower.contains("佳乐") -> "超市"
            // 交通
            lower.contains("交通") || lower.contains("地铁") ||
                    lower.contains("公交") || lower.contains("滴滴") ||
                    lower.contains("出租车") || lower.contains("顺风车") ||
                    lower.contains("高德") -> "交通"
            // 医疗
            lower.contains("医疗") || lower.contains("医院") ||
                    lower.contains("药店") || lower.contains("海王") -> "医疗"
            // 购物
            lower.contains("购物") || lower.contains("淘宝") ||
                    lower.contains("京东") || lower.contains("拼多多") ||
                    lower.contains("抖音") || lower.contains("电子") ||
                    lower.contains("数码") -> "购物"
            // 游戏
            lower.contains("游戏") || lower.contains("steam") -> "游戏"
            else -> "其他"
        }
    }

    /** 根据分类名称返回图标 emoji */
    private fun getCategoryIcon(name: String): String {
        return when (name) {
            "餐饮" -> "\uD83C\uDF54"
            "超市" -> "\uD83C\uDFEA"
            "交通" -> "\uD83D\uDE8C"
            "医疗" -> "\uD83C\uDFE5"
            "购物" -> "\uD83D\uDED2"
            "通信" -> "\uD83D\uDCF1"
            "游戏" -> "\uD83C\uDFAE"
            "数码" -> "\uD83D\uDDA5"
            "API" -> "\uD83D\uDDA5"
            else -> "\uD83D\uDCCD"
        }
    }

    /**
     * 深度采集文本：除了递归的 collectAllText，
     * 还额外用 findAccessibilityNodeInfosByText 搜索特定关键词附近的文本，
     * 解决 WebView / 自定义布局导致普通遍历丢失文本的问题。
     */
    private fun collectTextDeep(root: AccessibilityNodeInfo, baseText: String): String {
        val sb = StringBuilder(baseText)
        sb.append('\n')
        // 搜索常见商户/金额标签
        for (keyword in listOf("¥", "￥", "元", "收款方", "商户", "商品", "深度", "deep", "API")) {
            try {
                val nodes = root.findAccessibilityNodeInfosByText(keyword)
                for (n in nodes) {
                    if (!TextUtils.isEmpty(n.text)) {
                        val t = n.text.trim()
                        if (t !in sb) sb.append(t).append('\n')
                    }
                    if (!TextUtils.isEmpty(n.contentDescription)) {
                        val d = n.contentDescription.trim()
                        if (d !in sb) sb.append(d).append('\n')
                    }
                    // 也取父/子节点的文本
                    var parent = n.parent
                    if (parent != null && parent != root) {
                        val pText = collectAllText(parent)
                        if (pText !in sb) sb.append(pText).append('\n')
                        parent.recycle()
                    }
                }
            } catch (e: Exception) {
                // 某些节点已回收会导致异常，忽略
            }
        }
        return sb.toString()
    }

    override fun onInterrupt() {
        Log.i(TAG, "无障碍服务被中断")
    }
}
