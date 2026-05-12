package com.autobookkeeper.util

import android.util.Log
import com.autobookkeeper.data.Source
import com.autobookkeeper.data.Transaction
import com.autobookkeeper.data.TransactionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * 跨来源账单去重过滤器
 * 
 * 核心原则：宁可漏记，不可重记
 * 三层架构：来源内部去重 → 跨来源强匹配 → 跨来源弱匹配
 * 
 * 【新增】来源交叉验证：导入记录与通知记录交叉验证
 * - 导入来源：MANUAL(手动导入)、WECHAT(微信账单)、ALIPAY(支付宝账单)等
 * - 通知来源：WECHAT(微信通知)、ALIPAY(支付宝通知)等
 * - 同一笔交易可能同时被通知监听和账单导入捕获，需要交叉去重
 */
object DuplicateFilter {

    private const val TAG = "DuplicateFilter"

    // ═══════════════════════════════════════════════
    //  时间窗口配置（毫秒）
    // ═══════════════════════════════════════════════
    
    /** 精确匹配窗口：±5秒 */
    private const val WINDOW_EXACT = 5_000L
    
    /** 强匹配窗口：±60秒 */
    private const val WINDOW_STRONG = 60_000L
    
    /** 弱匹配窗口：±5分钟 */
    private const val WINDOW_WEAK = 300_000L
    
    /** 导入-通知交叉验证窗口：±30分钟（账单通知可能有延迟） */
    private const val WINDOW_CROSS_SOURCE = 1_800_000L
    
    /** 金额容差 */
    private const val AMOUNT_TOLERANCE = 0.01

    // ═══════════════════════════════════════════════
    //  来源分组（用于交叉验证）
    // ═══════════════════════════════════════════════
    
    /**
     * 判断来源是否为导入来源
     */
    fun isImportSource(source: Source): Boolean {
        return source == Source.MANUAL
    }
    
    /**
     * 判断来源是否为通知来源
     */
    fun isNotificationSource(source: Source): Boolean {
        return !isImportSource(source) && source != Source.MANUAL
    }
    
    /**
     * 获取来源的平台标识（用于交叉匹配）
     * 例如：微信账单导入(WECHAT) 和 微信通知(WECHAT) 属于同一平台
     */
    fun getPlatformGroup(source: Source): String {
        return when (source) {
            Source.WECHAT, Source.ALIPAY, Source.TAOBAO, Source.TAOBAO_FLASH,
            Source.PDD, Source.JD, Source.DOUYIN, Source.KUAISHOU,
            Source.MEITUAN, Source.DINGDONG, Source.PUPU -> source.name
            
            // 银行类统一分组
            Source.BANK_ICBC, Source.BANK_CCB, Source.BANK_CMB, Source.BANK_BOCOM,
            Source.BANK_ABC, Source.BOC, Source.BANK_SPDB, Source.BANK_CITI,
            Source.BANK_CEB, Source.BANK_CMBC, Source.BANK_CITIC, Source.BANK_HXB,
            Source.BANK_PAB, Source.UNIONPAY, Source.DCEP -> "BANK"
            
            Source.MANUAL -> "MANUAL"
        }
    }
    
    /**
     * 判断两个来源是否可能为同一笔交易的不同来源
     */
    fun isCrossSourceMatch(source1: Source, source2: Source): Boolean {
        // 同一来源不算交叉
        if (source1 == source2) return false
        
        // 一个是导入，一个是通知
        val isImport1 = isImportSource(source1)
        val isImport2 = isImportSource(source2)
        
        // 只有导入vs通知才需要交叉验证
        if (isImport1 == isImport2) return false
        
        // 检查是否属于同一平台
        return getPlatformGroup(source1) == getPlatformGroup(source2)
    }

    // ═══════════════════════════════════════════════
    //  商户别名映射表（可扩展）
    // ═══════════════════════════════════════════════
    
    private val MERCHANT_ALIASES = mapOf(
        "美团" to listOf("美团外卖", "美团点评", "美团打车", "Meituan", "美团外卖_"),
        "苹果" to listOf("Apple", "Apple Store", "苹果商店", "iTunes", "App Store"),
        "滴滴" to listOf("滴滴出行", "滴滴快车", "滴滴专车", "DiDi", "滴滴打车"),
        "淘宝" to listOf("淘宝网", "Taobao", "天猫", "Tmall"),
        "京东" to listOf("京东商城", "JD.com", "京东购物"),
        "拼多多" to listOf("PDD", "拼夕夕"),
        "支付宝" to listOf("Alipay", "支付宝_", "支付宝-"),
        "微信" to listOf("微信支付", "WeChat Pay", "财付通", "财付通-"),
        "抖音" to listOf("抖音商城", "Douyin", "抖音电商"),
        "快手" to listOf("快手商城", "Kuaishou"),
        "朴朴" to listOf("朴朴超市", "Pupu"),
        "叮咚" to listOf("叮咚买菜", "Dingdong"),
        "盒马" to listOf("盒马鲜生", "Hema"),
        "山姆" to listOf("山姆会员店", "Sam's Club"),
        "星巴克" to listOf("Starbucks", "星巴克咖啡"),
        "麦当劳" to listOf("McDonald's", "金拱门"),
        "肯德基" to listOf("KFC", "肯德基餐厅"),
        "喜茶" to listOf("HEYTEA", "喜茶GO"),
        "奈雪" to listOf("奈雪的茶", " Nayuki"),
        "瑞幸" to listOf("Luckin", "瑞幸咖啡"),
        "海底捞" to listOf("Haidilao"),
        "顺丰" to listOf("顺丰速运", "SF Express"),
        "中通" to listOf("中通快递", "ZTO"),
        "圆通" to listOf("圆通速递", "YTO"),
        "韵达" to listOf("韵达快递", "Yunda"),
        "申通" to listOf("申通快递", "STO"),        "京东物流" to listOf("京东快递", "JD Logistics"),
        // ===== 银行类 =====
        "招商银行" to listOf("招行", "CMB", "China Merchants Bank", "招商银行信用卡"),
        "工商银行" to listOf("工行", "ICBC", "中国工商银行"),
        "建设银行" to listOf("建行", "CCB", "中国建设银行"),
        "农业银行" to listOf("农行", "ABC", "中国农业银行"),
        "中国银行" to listOf("中行", "BOC", "Bank of China"),
        "交通银行" to listOf("交行", "Bankcomm", "BCM"),
        "邮储银行" to listOf("邮政储蓄", "PSBC"),
        "中信银行" to listOf("CITIC"),
        "光大银行" to listOf("CEB"),
        "民生银行" to listOf("CMBC"),
        "平安银行" to listOf("PAB"),
        "兴业银行" to listOf("CIB"),
        "浦发银行" to listOf("SPDB"),
        "华夏银行" to listOf("HXB"),
        "广发银行" to listOf("CGB"),
        "北京银行" to listOf("BOB"),
        "上海银行" to listOf("BOS"),
        "微众银行" to listOf("WeBank"),
        "网商银行" to listOf("MYbank", "蚂蚁银行"),
        // ===== 通信运营商 =====
        "中国移动" to listOf("移动", "China Mobile", "CMCC", "中移动"),
        "中国联通" to listOf("联通", "China Unicom", "CU", "中联通"),
        "中国电信" to listOf("电信", "China Telecom", "CT", "中电信"),
        // ===== 出行 =====
        "携程" to listOf("Ctrip", "携程旅行"),
        "去哪儿" to listOf("Qunar"),
        "飞猪" to listOf("Fliggy", "阿里旅行"),
        "同程" to listOf("同程旅行", "Lvmama"),
        "铁路12306" to listOf("12306", "中国铁路"),
        "中国国航" to listOf("国航", "Air China", "CA"),
        "南方航空" to listOf("南航", "China Southern", "CZ"),
        "东方航空" to listOf("东航", "China Eastern", "MU"),
        "海南航空" to listOf("海航", "Hainan Airlines", "HU"),
        "春秋航空" to listOf("Spring Airlines"),
        "神州租车" to listOf("神州"),
        "一嗨租车" to listOf("一嗨", "eHi"),
        "哈啰" to listOf("哈啰出行", "哈罗", "Hellobike"),
        "青桔" to listOf("青桔单车", "滴滴单车"),
        "摩拜" to listOf("美团单车"),
        "高德" to listOf("高德地图", "Amap", "高德打车"),
        "百度地图" to listOf("Baidu Map"),
        "腾讯地图" to listOf("Tencent Map"),
        // ===== 视频/娱乐 =====
        "爱奇艺" to listOf("iQIYI", "奇异果"),
        "腾讯视频" to listOf("Tencent Video", "极光TV"),
        "优酷" to listOf("Youku"),
        "哔哩哔哩" to listOf("B站", "Bilibili", "BL"),
        "芒果TV" to listOf("Mango TV"),
        "网易云音乐" to listOf("网易云", "NetEase Cloud Music"),
        "QQ音乐" to listOf("QQ Music", "腾讯音乐"),
        "喜马拉雅" to listOf("Ximalaya", "喜马拉雅FM"),
        "蜻蜓FM" to listOf("Qingting"),
        // ===== 生活服务 =====
        "美团买菜" to listOf("美团优选"),
        "饿了么" to listOf("Ele.me", "阿里本地生活"),
        "大众点评" to listOf("点评", "Dianping"),
        "口碑" to listOf("Koubei", "支付宝口碑"),
        "58同城" to listOf("58.com"),
        "贝壳找房" to listOf("贝壳", "Beike"),
        "链家" to listOf("Lianjia"),
        "自如" to listOf("Ziroom"),
        "货拉拉" to listOf("Huolala"),
        "快狗打车" to listOf("GOGOX"),
        "UU跑腿" to listOf("UU"),
        // ===== 云服务 =====
        "阿里云" to listOf("Aliyun", "Alibaba Cloud"),
        "腾讯云" to listOf("Tencent Cloud"),
        "华为云" to listOf("Huawei Cloud"),
        "百度云" to listOf("Baidu Cloud"),
        "天翼云" to listOf("CTyun"),
        // ===== 其他常用 =====
        "知乎" to listOf("Zhihu"),
        "小红书" to listOf("RED", "Xiaohongshu"),
        "微博" to listOf("Weibo", "新浪微博"),
        "豆瓣" to listOf("Douban"),
        "得到" to listOf("Dedao"),
        "樊登读书" to listOf("帆书"),
        "Keep" to listOf("Keep运动"),
        "薄荷健康" to listOf("Boohee"),
        "下厨房" to listOf("Xiachufang"),
        "墨迹天气" to listOf("Moji"),
        "WPS" to listOf("金山办公", "Kingsoft"),
        "迅雷" to listOf("Xunlei", "Thunder"),
        "百度网盘" to listOf("Baidu Netdisk"),
        "阿里云盘" to listOf("Aliyun Drive"),
        "夸克" to listOf("Quark"),
        "UC浏览器" to listOf("UC Browser"),
        "QQ浏览器" to listOf("QQ Browser"),
        "搜狗" to listOf("Sogou", "搜狗输入法"),
        "讯飞" to listOf("iFlytek", "科大讯飞"),
        "有道" to listOf("Youdao", "网易有道"),
        "金山词霸" to listOf("Kingsoft PowerWord")
    )

    /** 反向映射：variant → canonical */
    private val ALIAS_REVERSE by lazy {
        val map = mutableMapOf<String, String>()
        MERCHANT_ALIASES.forEach { (canonical, variants) ->
            map[canonical] = canonical
            variants.forEach { variant ->
                map[variant.lowercase()] = canonical
                map[cleanMerchant(variant).lowercase()] = canonical
            }
            map[cleanMerchant(canonical).lowercase()] = canonical
        }
        map
    }

    // ═══════════════════════════════════════════════
    //  数据标准化
    // ═══════════════════════════════════════════════

    /**
     * 标准化交易记录（入库前调用）
     */
    fun normalizeTransaction(tx: Transaction): Transaction {
        val cleanMerchant = cleanMerchant(tx.note)
        val canonicalMerchant = canonicalizeMerchant(cleanMerchant)
        
        return tx.copy(
            merchantRaw = tx.note,
            note = canonicalMerchant.take(50),
            fingerprintStrong = generateStrongFingerprint(tx),
            fingerprintMedium = generateMediumFingerprint(tx, canonicalMerchant)
        )
    }

    /**
     * 清洗商户名：去除常见前后缀和特殊符号
     */
    fun cleanMerchant(merchant: String): String {
        return merchant
            .replace(Regex("[_#]\\d+$"), "")
            .replace(Regex("^.*?[-—]"), "")
            .replace(Regex("支付$|付款$"), "")
            .replace(Regex("[\\s\\t\\n\\r]+"), "")
            .replace(Regex("[^\\u4e00-\\u9fa5a-zA-Z0-9]"), "")
            .trim()
    }

    /**
     * 获取标准化商户名（别名映射）
     */
    fun canonicalizeMerchant(merchant: String): String {
        val clean = cleanMerchant(merchant)
        val lower = clean.lowercase()
        
        ALIAS_REVERSE[lower]?.let { return it }
        
        ALIAS_REVERSE.forEach { (variant, canonical) ->
            if (lower.contains(variant) || variant.contains(lower)) {
                if (lower.length >= 2 && variant.length >= 2) {
                    return canonical
                }
            }
        }
        
        if (clean.length > 4) {
            var bestMatch: String? = null
            var bestDist = Int.MAX_VALUE
            ALIAS_REVERSE.keys.forEach { variant ->
                if (variant.length > 4) {
                    val dist = levenshtein(lower, variant)
                    if (dist < bestDist && dist <= 2) {
                        bestDist = dist
                        bestMatch = ALIAS_REVERSE[variant]
                    }
                }
            }
            bestMatch?.let { return it }
        }
        
        return clean
    }

    // ═══════════════════════════════════════════════
    //  指纹生成
    // ═══════════════════════════════════════════════

    /**
     * 强指纹：sha256(orderNo + amount + direction)
     */
    fun generateStrongFingerprint(tx: Transaction): String? {
        val orderNo = tx.orderNo?.trim()
        if (orderNo.isNullOrBlank() || orderNo.length < 4) return null
        
        val direction = if (tx.amount < 0) "DEBIT" else "CREDIT"
        val raw = "${orderNo}|${String.format("%.2f", abs(tx.amount))}|$direction"
        return sha256(raw)
    }

    /**
     * 中指纹：sha256(amount + direction + merchant + date)
     */
    fun generateMediumFingerprint(tx: Transaction, merchant: String? = null): String {
        val direction = if (tx.amount < 0) "DEBIT" else "CREDIT"
        val amountStr = String.format("%.2f", abs(tx.amount))
        val merchantKey = merchant ?: tx.note
        val dateKey = formatDateKey(tx.date)
        val raw = "$amountStr|$direction|$merchantKey|$dateKey"
        return sha256(raw)
    }

    /**
     * 弱指纹：sha256(amount + direction + date)
     */
    fun generateWeakFingerprint(tx: Transaction): String {
        val direction = if (tx.amount < 0) "DEBIT" else "CREDIT"
        val amountStr = String.format("%.2f", abs(tx.amount))
        val dateKey = formatDateKey(tx.date)
        val raw = "$amountStr|$direction|$dateKey"
        return sha256(raw)
    }

    // ═══════════════════════════════════════════════
    //  三层去重逻辑 + 来源交叉验证
    // ═══════════════════════════════════════════════

    /**
     * 执行完整去重检查（含来源交叉验证）
     */
    suspend fun checkDuplicate(
        dao: TransactionDao,
        tx: Transaction
    ): DuplicateResult = withContext(Dispatchers.IO) {
        val normalized = normalizeTransaction(tx)
        
        // ─── P0: 强指纹匹配（订单号完全匹配）───
        normalized.fingerprintStrong?.let { strongFp ->
            dao.findByStrongFingerprint(strongFp)?.let { existing ->
                Log.d(TAG, "P0 强指纹匹配: 新记录=${tx.note} 金额=${tx.amount} 匹配到 existingId=${existing.id}")
                return@withContext DuplicateResult(
                    original = existing,
                    confidence = 1.0,
                    shouldDiscard = true,
                    reason = "订单号完全匹配"
                )
            }
        }
        
        // ─── P1: 中指纹匹配（金额+商户+同一天）───
        normalized.fingerprintMedium?.let { mediumFp ->
            dao.findByMediumFingerprint(mediumFp)?.let { existing ->
                val timeDiff = abs(tx.date - existing.date)
                if (timeDiff <= WINDOW_STRONG) {
                    Log.d(TAG, "P1 中指纹匹配: 新记录=${tx.note} 匹配到 existingId=${existing.id} 时间差=${timeDiff}ms")
                    return@withContext DuplicateResult(
                        original = existing,
                        confidence = 0.95,
                        shouldDiscard = true,
                        reason = "金额+商户+时间强匹配"
                    )
                }
            }
        }
        
        // ─── 【新增】P1.5: 来源交叉验证（导入 vs 通知）───
        // 如果新记录是导入的，检查是否有对应的通知记录
        // 如果新记录是通知，检查是否有对应的导入记录
        val crossSourceResult = checkCrossSourceDuplicate(dao, tx, normalized)
        if (crossSourceResult != null) {
            return@withContext crossSourceResult
        }
        
        // ─── P2: 时间窗口内相似记录（金额+方向+时间窗口）───
        val startTime = tx.date - WINDOW_WEAK
        val endTime = tx.date + WINDOW_WEAK
        val similarRecords = dao.findSimilarInTimeWindow(tx.amount, startTime, endTime)
        
        for (existing in similarRecords) {
            val timeDiff = abs(tx.date - existing.date)
            val merchantMatch = isMerchantMatch(normalized.note, existing.note)
            
            when {
                merchantMatch && timeDiff <= WINDOW_WEAK -> {
                    Log.d(TAG, "P2a 弱匹配: 新记录=${tx.note} 匹配到 existingId=${existing.id} 时间差=${timeDiff}ms")
                    return@withContext DuplicateResult(
                        original = existing,
                        confidence = 0.80,
                        shouldDiscard = false,
                        reason = "金额+商户+时间弱匹配（需确认）",
                        isSuspicious = true
                    )
                }
                timeDiff <= WINDOW_WEAK && isSameDay(tx.date, existing.date) -> {
                    Log.d(TAG, "P2b 日级匹配: 新记录=${tx.note} 匹配到 existingId=${existing.id}")
                    return@withContext DuplicateResult(
                        original = existing,
                        confidence = 0.60,
                        shouldDiscard = false,
                        reason = "金额+日级匹配（仅关联）",
                        isSuspicious = false
                    )
                }
            }
        }
        
        return@withContext DuplicateResult(
            original = null,
            confidence = 0.0,
            shouldDiscard = false,
            reason = "无重复"
        )
    }
    
    /**
     * 【新增】来源交叉验证
     * 检查导入记录与通知记录是否为同一笔交易
     */
    private suspend fun checkCrossSourceDuplicate(
        dao: TransactionDao,
        tx: Transaction,
        normalized: Transaction
    ): DuplicateResult? {
        // 只处理导入vs通知的交叉场景
        if (tx.source == Source.MANUAL) {
            // 导入记录：查找对应平台的通知记录
            val startTime = tx.date - WINDOW_CROSS_SOURCE
            val endTime = tx.date + WINDOW_CROSS_SOURCE
            
            // 查询同一时间窗口内的所有记录
            val candidates = dao.findSimilarInTimeWindow(tx.amount, startTime, endTime)
            
            for (existing in candidates) {
                // 跳过非通知来源
                if (existing.source == Source.MANUAL) continue
                
                // 检查金额和方向是否一致
                if (abs(tx.amount - existing.amount) > AMOUNT_TOLERANCE) continue
                if ((tx.amount < 0) != (existing.amount < 0)) continue
                
                // 检查商户名是否匹配
                val existingMerchant = cleanMerchant(existing.note)
                val newMerchant = normalized.note
                
                if (isMerchantMatch(newMerchant, existingMerchant)) {
                    val timeDiff = abs(tx.date - existing.date)
                    val confidence = when {
                        timeDiff <= WINDOW_STRONG -> 0.92
                        timeDiff <= WINDOW_WEAK -> 0.85
                        timeDiff <= WINDOW_CROSS_SOURCE -> 0.75
                        else -> 0.0
                    }
                    
                    if (confidence > 0) {
                        Log.d(TAG, "交叉验证匹配: 导入记录=${tx.note} 匹配到通知记录 id=${existing.id} " +
                                "来源=${existing.source} 时间差=${timeDiff}ms 置信度=$confidence")
                        return DuplicateResult(
                            original = existing,
                            confidence = confidence,
                            shouldDiscard = true,
                            reason = "导入记录与${existing.source.label}通知交叉验证匹配"
                        )
                    }
                }
            }
        } else {
            // 通知记录：查找对应的导入记录
            val startTime = tx.date - WINDOW_CROSS_SOURCE
            val endTime = tx.date + WINDOW_CROSS_SOURCE
            
            val candidates = dao.findSimilarInTimeWindow(tx.amount, startTime, endTime)
            
            for (existing in candidates) {
                // 只匹配导入来源
                if (existing.source != Source.MANUAL) continue
                
                if (abs(tx.amount - existing.amount) > AMOUNT_TOLERANCE) continue
                if ((tx.amount < 0) != (existing.amount < 0)) continue
                
                val existingMerchant = cleanMerchant(existing.note)
                val newMerchant = normalized.note
                
                if (isMerchantMatch(newMerchant, existingMerchant)) {
                    val timeDiff = abs(tx.date - existing.date)
                    val confidence = when {
                        timeDiff <= WINDOW_STRONG -> 0.92
                        timeDiff <= WINDOW_WEAK -> 0.85
                        timeDiff <= WINDOW_CROSS_SOURCE -> 0.75
                        else -> 0.0
                    }
                    
                    if (confidence > 0) {
                        Log.d(TAG, "交叉验证匹配: 通知记录=${tx.note} 匹配到导入记录 id=${existing.id} " +
                                "时间差=${timeDiff}ms 置信度=$confidence")
                        return DuplicateResult(
                            original = existing,
                            confidence = confidence,
                            shouldDiscard = true,
                            reason = "通知记录与导入记录交叉验证匹配"
                        )
                    }
                }
            }
        }
        
        return null
    }

    /**
     * 批量去重（用于 CSV/PDF 导入时）
     */
    suspend fun batchDeduplicate(
        dao: TransactionDao,
        transactions: List<Transaction>
    ): BatchDedupResult = withContext(Dispatchers.IO) {
        val kept = mutableListOf<Transaction>()
        val filtered = mutableListOf<FilteredRecord>()
        
        // 第一步：来源内部去重
        val internalDeduped = internalDedup(transactions)
        
        // 第二步：与数据库跨来源去重（含交叉验证）
        for (tx in internalDeduped) {
            val result = checkDuplicate(dao, tx)
            
            when {
                result.shouldDiscard -> {
                    filtered.add(FilteredRecord(tx, result, true))
                    Log.d(TAG, "自动过滤: ${tx.note} ${tx.amount} 原因=${result.reason}")
                }
                result.isSuspicious -> {
                    val markedTx = tx.copy(
                        isDuplicate = true,
                        duplicateOf = result.original?.id
                    )
                    kept.add(markedTx)
                    filtered.add(FilteredRecord(tx, result, false))
                    Log.d(TAG, "标记疑似重复: ${tx.note} ${tx.amount}")
                }
                else -> {
                    kept.add(tx)
                }
            }
        }
        
        BatchDedupResult(kept, filtered)
    }

    /**
     * 来源内部去重
     */
    private fun internalDedup(transactions: List<Transaction>): List<Transaction> {
        val seenOrderNos = mutableSetOf<String>()
        val seenFingerprints = mutableSetOf<String>()
        val result = mutableListOf<Transaction>()
        
        for (tx in transactions) {
            val orderNo = tx.orderNo?.trim()
            if (!orderNo.isNullOrBlank() && orderNo.length >= 4) {
                if (seenOrderNos.contains(orderNo)) {
                    Log.d(TAG, "内部去重-订单号: $orderNo")
                    continue
                }
                seenOrderNos.add(orderNo)
            }
            
            val strongFp = generateStrongFingerprint(tx)
            if (strongFp != null) {
                if (seenFingerprints.contains(strongFp)) {
                    Log.d(TAG, "内部去重-强指纹: ${tx.note}")
                    continue
                }
                seenFingerprints.add(strongFp)
            }
            
            val dupKey = "${tx.amount}|${tx.note}|${tx.date / 1000}"
            if (seenFingerprints.contains(dupKey)) {
                Log.d(TAG, "内部去重-秒级: ${tx.note}")
                continue
            }
            seenFingerprints.add(dupKey)
            
            result.add(tx)
        }
        
        return result
    }

    // ═══════════════════════════════════════════════
    //  商户匹配工具
    // ═══════════════════════════════════════════════

    fun isMerchantMatch(a: String, b: String): Boolean {
        val cleanA = cleanMerchant(a)
        val cleanB = cleanMerchant(b)
        
        if (cleanA == cleanB) return true
        
        if (cleanA.contains(cleanB) || cleanB.contains(cleanA)) {
            if (cleanA.length >= 2 && cleanB.length >= 2) return true
        }
        
        val canonicalA = canonicalizeMerchant(cleanA)
        val canonicalB = canonicalizeMerchant(cleanB)
        if (canonicalA == canonicalB && canonicalA.isNotBlank()) return true
        
        if (cleanA.length > 4 && cleanB.length > 4) {
            val dist = levenshtein(cleanA.lowercase(), cleanB.lowercase())
            if (dist <= 2) return true
        }
        
        return false
    }

    // ═══════════════════════════════════════════════
    //  工具函数
    // ═══════════════════════════════════════════════

    private fun formatDateKey(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
    }

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        return formatDateKey(t1) == formatDateKey(t2)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1]) + 1
                }
            }
        }
        return dp[s1.length][s2.length]
    }
}

// ═══════════════════════════════════════════════
//  数据类
// ═══════════════════════════════════════════════

data class DuplicateResult(
    val original: Transaction?,
    val confidence: Double,
    val shouldDiscard: Boolean,
    val reason: String,
    val isSuspicious: Boolean = false
)

data class FilteredRecord(
    val transaction: Transaction,
    val result: DuplicateResult,
    val autoDiscarded: Boolean
)

data class BatchDedupResult(
    val kept: List<Transaction>,
    val filtered: List<FilteredRecord>
)
