package com.autobookkeeper.service

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.autobookkeeper.App
import com.autobookkeeper.data.CategoryEngine
import com.autobookkeeper.data.Source
import com.autobookkeeper.data.Transaction
import com.autobookkeeper.data.TransactionDao
import com.autobookkeeper.util.DuplicateFilter
import com.autobookkeeper.util.DuplicateResult
import com.autobookkeeper.util.NotificationLogHelper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

class PaymentNotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "PmtListener"
        private const val DEDUP_WINDOW_MS = 5000L
        private const val SHOPPING_DEDUP_WINDOW_MS = 15000L
        private const val HEARTBEAT_INTERVAL_MS = 30000L // 30秒心跳
        private const val RESTART_DELAY_MS = 3000L // 崩溃后3秒重启

        @Volatile
        var isConnected = false
            private set

        @Volatile
        var lastPingTime = 0L
            private set

        @Volatile
        var serviceInstance: PaymentNotificationListener? = null
            private set

        fun updatePing() {
            lastPingTime = System.currentTimeMillis()
        }

        fun isServiceHealthy(): Boolean {
            if (!isConnected) return false
            val elapsed = System.currentTimeMillis() - lastPingTime
            return elapsed < 120000L // 2分钟内要有活动
        }

        fun requestRebind(context: android.content.Context) {
            try {
                val componentName = android.content.ComponentName(context, PaymentNotificationListener::class.java)
                requestRebind(componentName)
                Log.d(TAG, "已请求重新绑定服务")
            } catch (e: Exception) {
                Log.e(TAG, "重新绑定失败: ${e.message}")
            }
        }

        private val recentRecords = ConcurrentHashMap<String, Long>()
        private val recentShoppingPayments = ConcurrentHashMap<String, Long>()
        // 跨应用去重：同一金额在短时间窗口内只记一次（解决美团+微信/银行卡双重通知问题）
        private val crossAppDedup = ConcurrentHashMap<String, Long>()
        private const val CROSS_APP_DEDUP_WINDOW_MS = 10000L // 10秒窗口

        // fun isConnected() removed - handled by isConnected property
        fun isDuplicate(amount: Double, source: Source, isShoppingSource: Boolean): Boolean {
            val now = System.currentTimeMillis()
            
            // 1. 跨应用去重检查（金额+时间窗口，不区分来源）
            val crossAppKey = String.format("%.2f", amount) // 金额作为key
            val lastCrossApp = crossAppDedup[crossAppKey]
            if (lastCrossApp != null && (now - lastCrossApp) < CROSS_APP_DEDUP_WINDOW_MS) {
                Log.d(TAG, "跨应用去重: 金额=$amount, source=$source")
                return true
            }
            
            // 2. 同应用去重检查
            val key = dedupKey(amount, source)
            val window = if (isShoppingSource) SHOPPING_DEDUP_WINDOW_MS else DEDUP_WINDOW_MS
            val map = if (isShoppingSource) recentShoppingPayments else recentRecords
            val last = map[key]
            if (last != null && (now - last) < window) return true
            
            // 记录到两个map
            map[key] = now
            crossAppDedup[crossAppKey] = now
            cleanup(map, now, window)
            cleanup(crossAppDedup, now, CROSS_APP_DEDUP_WINDOW_MS * 3)
            return false
        }

        fun shouldSkipDuplicatePayment(amount: Double, source: Source, orderNo: String): Boolean {
            val now = System.currentTimeMillis()
            val key = amount.toString() + "_" + source.name + "_" + orderNo
            val last = recentRecords[key]
            if (last != null && (now - last) < 30000L) return true
            recentRecords[key] = now
            cleanup(recentRecords, now, 60000L)
            return false
        }

        private fun dedupKey(amount: Double, source: Source) = "$amount|${source.name}"

        internal fun stripCardNumbers(text: String): String {
            return text.replace(Regex("""\b\d{16,19}\b""")) { "****" }
        }

        private fun cleanup(map: ConcurrentHashMap<String, Long>, now: Long, window: Long) {
            val cutoff = now - window * 3
            map.entries.removeAll { it.value < cutoff }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    // ─── 目标包映射 ───
    private val TARGET_PACKAGES = mapOf(
        "com.tencent.mm" to Source.WECHAT,
        "com.eg.android.AlipayGphone" to Source.ALIPAY,
        "com.unionpay" to Source.UNIONPAY,
        "cn.gov.pbc.dcep" to Source.DCEP,
        // 购物应用
        "com.sankuai.meituan" to Source.MEITUAN,
        "com.sankuai.meituan.takeoutnew" to Source.MEITUAN,
        "com.jingdong.app.mall" to Source.JD,
        "com.taobao.taobao" to Source.TAOBAO,
        "com.xunmeng.pinduoduo" to Source.PDD,
        "com.kuaishou.nebula" to Source.KUAISHOU,
        "com.smile.gifmaker" to Source.KUAISHOU,
        "com.ss.android.ugc.aweme" to Source.DOUYIN,  // 抖音
        "com.pupu.store" to Source.PUPU,               // 朴朴超市
        "com.yaya.zone" to Source.DINGDONG,            // 叮咚买菜
        // 银行
        "com.icbc" to Source.BANK_ICBC,                // 工商银行
        "com.ccb.pb" to Source.BANK_CCB,               // 建设银行
        "cmb.pb" to Source.BANK_CMB,                   // 招商银行
        "com.bankcomm.Bankcomm" to Source.BANK_BOCOM,  // 交通银行
        "com.chinamworld.mbank" to Source.BANK_ABC,    // 农业银行
        "com.boc.bocnet" to Source.BOC,                // 中国银行
        "cn.com.spdb.mobilebank.per" to Source.BANK_SPDB, // 浦发银行
        "com.citibank.mobile" to Source.BANK_CITI,     // 花旗银行
        "com.ceb.mobilebank" to Source.BANK_CEB,       // 光大银行
        "com.cmbc.mbank" to Source.BANK_CMBC,          // 民生银行
        "com.citic.mobilebank" to Source.BANK_CITIC,   // 中信银行
        "com.hxb.mobilebank" to Source.BANK_HXB,       // 华夏银行
        "com.pingan.bank" to Source.BANK_PAB           // 平安银行
    )

    // ─── 严格排除词1（所有来源排除）───
    private val EXCLUDE_KEYWORDS_STRICT = listOf(
        "能量可收", "能量可领", "收取能量", "领取能量",
        "积分可领", "积分可收", "金币可领", "积分到期",
        "优惠券到账", "优惠券领取", "领券成功", "红包封面",
        "红包已退回", "红包已过期", "转账过期", "转账已退回",
        "绑定", "解绑",
        "条新消息", "条通知", "条提醒",
        "腾讯新闻", "天气", "日历",
        "系统更新", "安全扫描", "体检", "优化",
        "消息提醒", "通知提醒",
        "购物订单", "我的订单", "我的足迹"
    )

    // ─── 严格排除词2（营销/活动/非交易，所有来源排除）───
    private val EXCLUDE_KEYWORDS_STRICT2 = listOf(
        "下单提醒",
        "您有新的订单",
        "您的订单已提交",
        "订单签收成功",
        "签收成功", "签收奖励",
        "你有一个红包待领取",
        "红包待领取",
        "来多多视频领取",
        "视频领取",
        "活动邀请", "邀您", "诚邀", "邀请",
        "岗位投递邀约", "岗位邀约", "求职", "招聘",
        "面试邀请",
        "寄递狂欢季",
        "立减", "立减礼包",
        "礼包邀您", "礼包",
        "奖励待到账",
        "余额不足",
        "优惠", "大促", "特惠", "特价", "折扣", "促销", "好价",
        "商品", "又降价了", "降价了", "补货", "到货", "上新", "新品",
        "抽奖", "中奖", "免费领", "0元领", "0元购",
        "体验金", "试用",
        "会员日", "拼团", "砍价",
        "签到", "打卡",
        "积分兑换", "积分",
        "新人专享", "福利",
        "升级", "调研",
        "服务通知",
        "到货通知",
        "开奖提醒",
        "直播",
        "每日福利",
        "限时秒杀", "限时降价", "限时优惠", "限时折扣", "限时活动",
        "降价", "降价提醒", "降价通知",
        "任务奖励",
        "你的专属",
        "闪购", "秒杀",
        // 节日营销/领券类
        "母亲节", "父亲节", "情人节", "圣诞节", "春节", "年货",
        "领券", "领劵", "优惠券", "优惠卷", "代金券", "红包雨",
        "满减", "满赠", "买赠", "买一送一", "第二件半价",
        "下单", "下单立减", "下单返现", "下单返",
        "超值", "精选", "爆款", "热卖", "热销",
        "礼盒", "大礼包", "礼包",
        "时令", "当季", "新鲜上市",
        "最快", "送达", "配送", "包邮", "免邮",
        "立即抢购", "马上抢", "手慢无", "限量", "库存紧张",
        "点击查看", "查看详情", "戳我", "点我",
        "更多优惠", "更多活动", "更多精选", "更多商品",
        // 非真实交易：红包券/活动/拍卖/推广
        "审核成功",
        "无门槛】红包",
        "领取红包",
        "卖房了",
        "起拍价",
        "起拍，",
        "立即捡漏",
        "幸运红包",
        "抢20元",        "补充医保",
        "住院1元起赔",
        "保证领取",
        "年领比例",
        "待开启",
        "待增值",
        "资金待增值",
        "100%保证领取",
        "已被店铺种草",
        "被店铺种草"
    )

    // ─── 营销排除词 ───
    private val EXCLUDE_KEYWORDS_MARKETING = listOf(
        "营销短信", "广告"
    )

    // ─── 支出关键词 ───
    private val TRANSFER_OUT_KEYWORDS = listOf(
        "转账", "转出", "付款", "支付", "消费", "购物",
        "支出", "扣款", "缴费", "充值", "购买",
        "刷卡", "取款", "汇款"
    )

    // ─── 退款关键词 ───
    private val REFUND_KEYWORDS = listOf(
        "退款", "退货", "退票", "退押金", "退租金",
        "退保", "退订", "取消订单", "撤销"
    )

    // ─── 收入关键词 ───
    private val RED_IN_KEYWORDS = listOf("红包")
    private val TRANSFER_IN_KEYWORDS = listOf(
        "收到", "收款", "到账", "入账", "转账收入",
        "转入", "存入", "汇入", "收款通知"
    )
    private val SALARY_KEYWORDS = listOf("工资", "薪资", "薪酬", "奖金", "提成", "津贴", "补贴")
    // 理财收益需要更严格的条件：必须包含收益/分红/利息/到账等词，仅"股票""基金"等不触发
    private val INVEST_KEYWORDS = listOf("理财", "基金", "股票")
    private val INVEST_INCOME_INDICATORS = listOf("收益", "分红", "利息", "到账", "已到账", "发放", "结算")
    private val RECHARGE_IN_KEYWORDS = listOf("充值到账", "充值成功", "缴费到账")

    // ─── 商户相关 ───
    private val MERCHANT_PREFIXES = listOf("商户", "商家", "店铺")
    private val STORE_INDICATORS = listOf("店", "超市", "便利店")

    // ─── 交易关键词 ───
    private val TRANSACTION_INDICATORS = listOf(
        "交易", "消费", "支付", "付款", "收款",
        "到账", "入账", "扣款", "支出", "收入",
        "转账", "退款", "充值", "缴费",
        "金额", "¥", "元"
    )

    // ─── 分类关键词已迁移到 CategoryEngine ───

    // ─── 来源分类 ───
    private val SHOPPING_SOURCES = setOf(
        Source.TAOBAO, Source.PDD, Source.JD, Source.KUAISHOU, Source.MEITUAN
    )

    // ─── 银行卡号模式 ───
    private val CARD_TAIL_PATTERNS = listOf(
        Regex("""尾号\s*(\d{4})"""),
        Regex("""(\d{4})\s*尾号"""),
        Regex("""尾数\s*(\d{4})"""),
        Regex("""(\d{4})\s*尾数"""),
        Regex("""末四位\s*(\d{4})""")
    )
    private val CARD_NUM_CONTEXT_PATTERN = Regex("""(尾号|尾数|末四位|卡号|卡末[四位]?)\s*[：:]\s*\d{4,}""")

    // ─── data class ───
    data class TxnResult(
        val isExpense: Boolean,
        val catName: String,
        val catIcon: String,
        val merchant: String = ""
    )

    // 心跳任务
    private var heartbeatJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
        Log.d(TAG, "服务已创建")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceInstance = null
        isConnected = false
        heartbeatJob?.cancel()
        Log.d(TAG, "服务已销毁")
        // 注意：不要在onDestroy中直接调用requestRebind，可能引发异常
        // 由ServiceWatchdog负责监控和重启
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        updatePing()
        startHeartbeat()
        Log.d(TAG, "已连接通知监听服务")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        heartbeatJob?.cancel()
        Log.d(TAG, "通知监听服务已断开")
        // 注意：不要在onListenerDisconnected中直接调用requestRebind
        // 用户手动关闭权限时会触发此回调，此时不应强制重新绑定
        // 由ServiceWatchdog负责监控和重启
    }
    /**
     * 数据库去重检查（P0/P1/P2层级）
     * 在内存去重之后，插入之前调用
     */
    private suspend fun checkDatabaseDuplicate(
        dao: TransactionDao,
        amount: Double,
        categoryName: String,
        note: String,
        source: Source,
        date: Long
    ): DuplicateResult? {
        // 构建临时Transaction用于去重检查
        val tempTxn = Transaction(
            amount = amount,
            categoryName = categoryName,
            categoryIcon = "",
            source = source,
            note = note,
            date = date
        )
        
        return try {
            DuplicateFilter.checkDuplicate(dao, tempTxn)
        } catch (e: Exception) {
            Log.e(TAG, "数据库去重检查失败: ${e.message}", e)
            null
        }
    }


    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isConnected) {
                kotlinx.coroutines.delay(HEARTBEAT_INTERVAL_MS)
                if (isConnected) {
                    updatePing()
                    Log.d(TAG, "服务心跳正常")
                }
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        updatePing()
        if (!isConnected) {
            Log.d(TAG, "未连接，跳过通知")
            return
        }
        val pkg = sbn.packageName
        val source = TARGET_PACKAGES[pkg] ?: run {
            Log.d(TAG, "非目标包: $pkg")
            NotificationLogHelper.log(this, pkg, "未知", "", "", "非目标包")
            return
        }
        val notification = sbn.notification ?: return
        val fullText = extractAllText(notification)
        val title = notification.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        Log.d(TAG, "收到通知 [$pkg]: $fullText")
        if (fullText.isBlank()) {
            NotificationLogHelper.log(this, pkg, source.name, title, fullText, "空文本")
            return
        }

        val lower = fullText.lowercase(Locale.CHINA)
        if (isExcluded(lower)) {
            Log.d(TAG, "被排除: $fullText")
            NotificationLogHelper.log(this, pkg, source.name, title, fullText, "已排除")
            return
        }

        val actualSource = inferActualSource(lower, source)
        val amount = extractAmount(lower)
        if (amount == null || amount <= 0) {
            Log.d(TAG, "未提取到金额: $fullText")
            NotificationLogHelper.log(this, pkg, actualSource.name, title, fullText, "无金额")
            return
        }
        Log.d(TAG, "提取金额: $amount")

        val isShoppingSource = SHOPPING_SOURCES.contains(actualSource)
        if (isDuplicate(amount, actualSource, isShoppingSource)) {
            Log.d(TAG, "重复去重: $amount")
            NotificationLogHelper.log(this, pkg, actualSource.name, title, fullText, "重复去重")
            return
        }

        val orderNo = extractOrderNo(fullText)
        if (shouldSkipDuplicatePayment(amount, actualSource, orderNo)) {
            Log.d(TAG, "订单号重复: $orderNo")
            NotificationLogHelper.log(this, pkg, actualSource.name, title, fullText, "订单号重复")
            return
        }

        val txn = classify(lower, actualSource)
        if (txn == null) {
            Log.d(TAG, "无法分类: $fullText")
            NotificationLogHelper.log(this, pkg, actualSource.name, title, fullText, "无法分类")
            return
        }
        Log.d(TAG, "分类结果: ${txn.catName} 支出=${txn.isExpense}")

        val signedAmount = if (txn.isExpense) -amount else amount
        val note = buildNote(fullText)

        scope.launch {
            try {
                val db = App.instance.database
                val dao = db.transactionDao()
                
                // 数据库去重检查（P0/P1/P2）
                val signedAmount = if (txn.isExpense) -amount else amount
                val dupResult = checkDatabaseDuplicate(
                    dao = dao,
                    amount = signedAmount,
                    categoryName = txn.catName,
                    note = note,
                    source = actualSource,
                    date = sbn.postTime
                )
                
                if (dupResult?.shouldDiscard == true) {
                    Log.d(TAG, "数据库去重丢弃: ${txn.catName} $amount 原因=${dupResult.reason}")
                    NotificationLogHelper.log(this@PaymentNotificationListener, pkg, actualSource.name, title, fullText, "数据库去重: ${dupResult.reason}")
                    return@launch
                }
                
                val transaction = Transaction(
                    amount = signedAmount,
                    categoryName = txn.catName,
                    categoryIcon = txn.catIcon,
                    source = actualSource,
                    note = note,
                    date = sbn.postTime
                )
                dao.insert(transaction)
                Log.d(TAG, "记账成功: ${txn.catName} $signedAmount")
                NotificationLogHelper.log(this@PaymentNotificationListener, pkg, actualSource.name, title, fullText, "已记录 ${txn.catName} ${amount}元")
            } catch (e: Exception) {
                Log.e(TAG, "记账失败: ${e.message}", e)
                NotificationLogHelper.log(this@PaymentNotificationListener, pkg, actualSource.name, title, fullText, "记账失败: ${e.message}")
            }
        }
    }

    private fun extractAllText(notification: Notification): String {
        val sb = StringBuilder()
        notification.extras?.let { extras ->
            extras.getCharSequence(Notification.EXTRA_TITLE)?.let { sb.append(" ").append(it) }
            extras.getCharSequence(Notification.EXTRA_TEXT)?.let { sb.append(" ").append(it) }
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.let { sb.append(" ").append(it) }
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { sb.append(" ").append(it) }
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach {
                sb.append(" ").append(it)
            }
        }
        return sb.toString().trim()
    }

    private fun extractOrderNo(text: String): String {
        val orderPattern = Regex("""(订单号|订单编号|交易单号|支付单号)[：:]\s*(\w+)""")
        val match = orderPattern.find(text)
        return match?.groupValues?.getOrNull(2) ?: ""
    }

    private fun classify(text: String, source: Source): TxnResult? {
        val lower = text.lowercase(Locale.CHINA)
        
        // 银行通知：检查收支指示词
        if (CategoryEngine.isBankSource(source)) {
            val hasExpense = CategoryEngine.EXPENSE_INDICATORS.any { lower.contains(it) }
            val hasIncome = CategoryEngine.INCOME_INDICATORS.any { lower.contains(it) }
            if (hasExpense) return TxnResult(true, "其他", "💰")
            if (hasIncome) return TxnResult(false, "其他", "💰")
            return null
        }

        // 微信提现
        if (source == Source.WECHAT && lower.contains("提现")) {
            return TxnResult(true, "转账", "💸")
        }

        // 使用统一分类引擎
        val (catName, catIcon) = CategoryEngine.classify(text, source)
        
        // 判断是否为支出（根据分类名推断）
        val isExpense = when (catName) {
            "退款", "红包", "工资", "理财收益", "转账" -> false
            else -> true
        }
        
        return TxnResult(isExpense, catName, catIcon, extractMerchant(text))
    }

    private fun isExcluded(text: String): Boolean {
        val lower = text.lowercase(Locale.CHINA)
        for (kw in EXCLUDE_KEYWORDS_STRICT) { if (lower.contains(kw)) return true }
        for (kw in EXCLUDE_KEYWORDS_STRICT2) { if (lower.contains(kw)) return true }
        for (kw in EXCLUDE_KEYWORDS_MARKETING) { if (lower.contains(kw)) return true }
        return false
    }

    private fun extractAmount(text: String): Double? {
        // 预处理：移除千分位逗号，便于匹配
        val normalizedText = text.replace(",", "")
        
        val patterns = listOf(
            // 银行格式：收入(退款财付通-财付通)5,000元 或 收入5000元
            Regex("""收入\s*\([^)]*\)\s*([\d,]+\.?\d{0,2})\s*元?"""),
            // 银行格式2：支出/收入金额直接跟数字
            Regex("""(?:收入|支出|存入|取出)[^\d]*([\d,]+\.?\d{0,2})\s*元?"""),
            // 支付宝格式：实付¥12.34 或 支付金额¥12.34
            Regex("""(?:实付|支付金额|付款金额|交易金额)[：:\s]*[¥￥]?\s*([\d,]+\.\d{2})"""),
            // 美团/京东格式：支付12.34元 或 共12.34元
            Regex("""(?:支付|共|合计|总计)[：:\s]*([\d,]+\.\d{2})\s*元?"""),
            // 标准格式（支持千分位逗号）
            Regex("""[¥￥]?\s*([\d,]+\.\d{2})\s*元?"""),
            Regex("""[¥￥]?\s*([\d,]+\.\d{2})"""),
            Regex("""金额[：:]?\s*[¥￥]?\s*([\d,]+\.\d{2})"""),
            Regex("""([\d,]+\.\d{2})\s*元"""),
            // 整数金额（支持千分位逗号）
            Regex("""[¥￥]?\s*([\d,]+)\s*元"""),
            Regex("""([\d,]+)\s*元""")
        )

        for (pattern in patterns) {
            val match = pattern.find(normalizedText)
            if (match != null) {
                val amountStr = match.groupValues[1].replace(",", "")
                val amount = amountStr.toDoubleOrNull()
                if (amount != null && amount > 0) {
                    // 退款时返回正数金额，classify会根据退款关键词识别为收入
                    return amount
                }
            }
        }
        return null
    }

    private fun inferSourceFromText(text: String): Source? {
        when {
            text.contains("微信") -> return Source.WECHAT
            text.contains("支付宝") -> return Source.ALIPAY
            text.contains("银联") -> return Source.UNIONPAY
            text.contains("数字人民币") || text.contains("数字人") -> return Source.DCEP
        }
        return null
    }

    private fun inferActualSource(text: String, pkgSource: Source): Source {
        // 使用统一引擎推断实际来源
        return CategoryEngine.inferActualSource(text, pkgSource)
    }

    private fun extractMerchant(text: String): String {
        val merchantPatterns = listOf(
            Regex("""商户[：:]?\s*(\S+)"""),
            Regex("""商家[：:]?\s*(\S+)"""),
            Regex("""店铺[：:]?\s*(\S+)""")
        )
        for (pattern in merchantPatterns) {
            val match = pattern.find(text)
            if (match != null) return match.groupValues[1].trim()
        }
        return ""
    }



    private fun buildNote(text: String): String {
        val cardStripped = stripCardNumbers(text)
        return cardStripped.take(200)
    }
}
