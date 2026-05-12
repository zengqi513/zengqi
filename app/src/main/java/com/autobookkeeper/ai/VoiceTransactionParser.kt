package com.autobookkeeper.ai

import java.util.Calendar
import java.util.regex.Pattern

/**
 * 语音记账自然语言理解增强解析器 v2
 * 支持：复杂表达、模糊时间、多关键词、方言习惯用语、时态理解
 */
object VoiceTransactionParser {

    data class ParsedResult(
        val amount: Double?,
        val category: String,
        val merchant: String,
        val transactionType: TransactionType,
        val confidence: Float,
        val rawEntities: Map<String, String>,
        val timeContext: String = ""  // 时间上下文提示
    )

    enum class TransactionType {
        EXPENSE, INCOME, TRANSFER, UNKNOWN
    }

    // ── 中英文/方言金额表达映射 ──
    private val normalizedMoneyMap = mapOf(
        "块" to "元", "毛" to "角", "分" to "元", "块钱" to "元",
        "大洋" to "元", "RMB" to "元", "rmb" to "元", "人民币" to "元",
        "文" to "元", "蚊" to "元",  // 粤语
        "两" to "二"  // 部分方言
    )

    // ── 模糊金额指示词 ──
    private val fuzzyAmountPatterns = listOf(
        Regex("""大概|大约|约莫|左右|上下|差不多"""),
        Regex("""十来?([一二三四五六七八九十百千万]?)"""),   // 十来块
        Regex("""([一二三四五六七八九十])([一二三四五六七八九十]?)多"""),  // 十多、二十多
        Regex("""小?\s*([\d.]+)\s*来?块"""),  // 小来块
        Regex("""百来[元块]?""") // 百来块
    )

    // ── 分类关键词库（同义词+方言） ──
    private val categoryKeywords = mapOf(
        "餐饮" to listOf(
            "吃", "饭", "餐", "菜", "食", "喝", "咖啡", "奶茶", "茶", "饮料",
            "面", "饺", "包", "饼", "串", "烤", "煮", "炒", "蒸", "炸",
            "早餐", "午饭", "晚餐", "夜宵", "外卖", "食堂", "餐厅", "饭店",
            "火锅", "烧烤", "寿司", "汉堡", "炸鸡", "披萨",
            "肯德基", "麦当劳", "星巴克", "海底捞", "必胜客",
            "茶百道", "喜茶", "奈雪", "蜜雪", "瑞幸", "库迪", "古茗", "沪上",
            "啖", "饮茶", "食饭", "宵夜"  // 粤语
        ),
        "交通" to listOf(
            "车", "地铁", "公交", "打车", "滴滴", "出租车", "高铁", "火车", "飞机",
            "油", "停车", "高速", "充电", "保养", "洗车",
            "共享单车", "电动", "摩托", "通勤", "巴士", "长途", "大巴",
            "搭车", "坐车", "搭地铁", "搭公交", // 粤语
            "骑行", "轮渡", "船票", "过路费", "违章", "罚单"
        ),
        "购物" to listOf(
            "超市", "买", "购", "淘宝", "京东", "拼多多", "抖音", "快手",
            "衣服", "鞋", "包", "化妆品", "护肤品",
            "零食", "水果", "蔬菜", "肉", "蛋", "奶", "酒水",
            "家电", "数码", "手机", "电脑",
            "天猫", "闲鱼", "得物", "小红书", "唯品会",
            "日用品", "文具", "书籍", "礼物", "送礼",
            "买咗", "购物", // 粤语
            "家居", "厨具", "床上用品", "收纳"
        ),
        "房屋水电" to listOf(
            "房租", "水电", "水费", "电费", "燃气", "物业", "装修", "家具",
            "房贷", "月供", "租金", "押金", "中介", "保洁", "搬家",
            "维修", "暖气", "取暖", "物业费"
        ),
        "消费" to listOf(
            "电影", "游戏", "玩", "娱乐", "KTV", "唱歌", "旅游", "旅行",
            "门票", "演出", "演唱会", "话剧", "展览", "游乐园",
            "剧本杀", "密室", "桌游", "麻将",
            "直播", "打赏", "会员", "订阅", "视频", "音乐",
            "爱奇艺", "腾讯视频", "B站", "哔哩哔哩",
            "健身", "游泳", "瑜伽", "打球", "网吧", "网咖",
            "按摩", "足浴", "美容", "美发", "理发",
            "迪士尼", "环球影城", "蹦极", "滑雪"
        ),
        "医疗" to listOf(
            "药", "医院", "看病", "挂号", "体检", "医保", "牙",
            "感冒", "发烧", "打针", "手术", "住院", "检查", "化验",
            "中医", "西医", "诊所", "药店", "口罩", "保健品", "维生素"
        ),
        "教育" to listOf(
            "书", "课", "学费", "培训", "学习", "考试", "教材",
            "网课", "辅导班", "补习班", "考证", "驾照",
            "雅思", "托福", "考研", "题库",
            "文具", "打印", "复印"
        ),
        "通信" to listOf(
            "话费", "流量", "宽带", "手机", "电话", "短信", "套餐",
            "移动", "联通", "电信", "广电", "网络"
        ),
        "收入" to listOf(
            "工资", "奖金", "补贴", "报销", "退款", "红包", "转账",
            "理财收益", "利息", "兼职", "外快", "提成", "分红",
            "收租", "返现", "返利", "佣金"
        ),
        "宠物" to listOf(
            "猫", "狗", "宠物", "猫粮", "狗粮", "猫砂", "驱虫",
            "洗澡", "美容", "寄养", "疫苗", "看病"
        ),
        "其他" to listOf("其他", "杂项", "其他支出", "乱七八糟")
    )

    // ── 交易类型指示词 ──
    private val expenseIndicators = listOf(
        "花", "用", "消费", "支出", "付", "买", "花了", "用了", "付了",
        "支出", "开销", "破费", "破钞", "使", "使了",
        "请客", "买单", "埋单"  // 粤语
    )
    private val incomeIndicators = listOf(
        "收", "收入", "赚", "到账", "发", "领", "收到", "发了", "领了",
        "进账", "入账", "落袋", // 粤语
        "奖金", "报销", "退款", "红包", "转账"  // 这些需要结合上下文
    )
    private val transferIndicators = listOf(
        "转", "转账", "转给", "转去", "转到", "转帐",
        "汇款", "划款"
    )

    // ── 时间表达 ──
    private val timePatterns = listOf(
        Regex("""(今|昨|前|明)(天|日|晚)"""),  // 今天、昨天、明天
        Regex("""(上|这|本|下)个?(周|月|星期)"""),  // 这周、下个月
        Regex("""([早中晚])(上|午|间|餐)"""),  // 早上、中午、晚上
        Regex("""刚才|刚刚|现在|这会儿"""),
        Regex("""(\d{1,2})[:：](\d{2})"""),    // 15:30
        Regex("""(\d{1,2})点""")               // 12点
    )

    // ── 金额数字中文 ──
    private val chineseNumbers = mapOf(
        "零" to 0, "一" to 1, "二" to 2, "两" to 2, "三" to 3,
        "四" to 4, "五" to 5, "六" to 6, "七" to 7, "八" to 8,
        "九" to 9, "十" to 10, "百" to 100, "千" to 1000,
        "万" to 10000, "亿" to 100000000
    )

    /** 主解析入口 */
    fun parse(text: String): ParsedResult {
        var rawEntities = mutableMapOf<String, String>()

        // 标准化
        val normalizedText = normalizeText(text)
        rawEntities["normalizedText"] = normalizedText

        // 提取时间上下文
        val timeContext = extractTimeContext(normalizedText)
        rawEntities["timeContext"] = timeContext

        // 识别交易类型
        val transactionType = detectTransactionType(normalizedText)
        rawEntities["detectedType"] = transactionType.name

        // 提取金额（支持模糊金额）
        val amount = extractAmount(normalizedText, rawEntities)
        rawEntities["amount"] = amount?.toString() ?: "unknown"

        // 金额信息提取辅助信息
        val amountInfo = extractAmountInfo(normalizedText)

        // 识别分类
        val category = detectCategory(normalizedText, transactionType, amountInfo)
        rawEntities["category"] = category

        // 提取商户
        val merchant = extractMerchant(normalizedText, category)
        rawEntities["merchant"] = merchant

        // 置信度
        val confidence = calculateConfidence(amount, category, merchant, normalizedText)
        rawEntities["confidence"] = String.format("%.2f", confidence)

        return ParsedResult(
            amount = amount,
            category = category,
            merchant = merchant,
            transactionType = transactionType,
            confidence = confidence,
            rawEntities = rawEntities,
            timeContext = timeContext
        )
    }

    // ── 文本标准化 ──
    private fun normalizeText(text: String): String {
        var t = text
        for ((key, value) in normalizedMoneyMap) {
            t = t.replace(key, value)
        }
        // 标点变空格
        t = t.replace(Regex("""[，。！？、；：,\.!\?;:]"""), " ")
        // 多余空格压缩
        t = t.replace(Regex("""\s+"""), " ").trim()
        return t
    }

    // ── 时间上下文 ──
    private fun extractTimeContext(text: String): String {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val today = "今天"
        val timeOfDay = when (hour) {
            in 5..8 -> "早上"
            in 9..11 -> "上午"
            in 12..13 -> "中午"
            in 14..17 -> "下午"
            in 18..23 -> "晚上"
            else -> "凌晨"
        }

        for (pattern in timePatterns) {
            val m = pattern.find(text)
            if (m != null) {
                val match = m.value
                return when {
                    match.contains("今") -> "$today$timeOfDay"
                    match.contains("昨") -> "昨天"
                    match.contains("前") -> "前天"
                    match.contains("明") -> "明天"
                    match.contains("上") && (match.contains("周") || match.contains("星期")) -> "上周"
                    match.contains("这") || match.contains("本") -> "$today$timeOfDay"
                    match.contains("下") -> "下周"
                    match.contains("早") -> "早上"
                    match.contains("中") || match.contains("午") -> "中午"
                    match.contains("晚") -> "晚上"
                    else -> "$today$timeOfDay"
                }
            }
        }

        // 根据当前时间推断（早餐场景）
        if (hour in 6..9 && text.contains(Regex("早餐|包子|豆浆|肠粉|煎饼"))) return "早上"
        if (hour in 11..13 && text.contains(Regex("午餐|午饭|快餐|盒饭"))) return "中午"
        if (hour in 17..20 && text.contains(Regex("晚餐|晚饭"))) return "晚上"

        return "$today$timeOfDay"
    }

    // ── 交易类型 ──
    private fun detectTransactionType(text: String): TransactionType {
        // 收入优先
        for (indicator in incomeIndicators) {
            if (text.contains(indicator)) {
                // 排除"支出"场景
                if (text.contains(Regex("花|买|消费|支出|付了"))) continue
                return TransactionType.INCOME
            }
        }
        for (indicator in transferIndicators) {
            if (text.contains(indicator)) return TransactionType.TRANSFER
        }
        for (indicator in expenseIndicators) {
            if (text.contains(indicator)) return TransactionType.EXPENSE
        }
        return TransactionType.EXPENSE
    }

    // ── 金额解析 ──
    private data class AmountInfo(
        val value: Double?,
        val isFuzzy: Boolean,
        val fuzzyRange: Pair<Double, Double>? = null  // 模糊范围
    )

    private fun extractAmountInfo(text: String): AmountInfo {
        val isFuzzy = fuzzyAmountPatterns.any { it.containsMatchIn(text) }

        // 标准数字
        val standard = extractStandardAmount(text)
        if (standard != null) {
            return AmountInfo(standard, isFuzzy)
        }

        // 模糊金额：十来块→10, 二十多→20+
        val fuzzyValue = extractFuzzyAmount(text)
        if (fuzzyValue != null) {
            return AmountInfo(fuzzyValue, true, Pair(fuzzyValue, fuzzyValue * 1.5))
        }

        // 中文数字
        val chineseValue = extractChineseAmount(text)
        if (chineseValue != null) {
            return AmountInfo(chineseValue, isFuzzy)
        }

        return AmountInfo(null, isFuzzy)
    }

    private fun extractAmount(text: String, entities: MutableMap<String, String>): Double? {
        val info = extractAmountInfo(text)
        if (info.isFuzzy) {
            entities["isFuzzyAmount"] = "true"
        }
        return info.value
    }

    private fun extractStandardAmount(text: String): Double? {
        val patterns = listOf(
            // "记一笔 123.45元" / "记一笔 123.45"
            Regex("""记一笔\s*(\d+(?:\.\d{1,2})?)\s*元?"""),
            // "花了/用了/付了 123.45 元"
            Regex("""[花了用付了支出消费][了]?\s*(\d+(?:\.\d{1,2})?)\s*元"""),
            // "收入/收到/到账 123.45 元"
            Regex("""[收入收到到账发领][了]?\s*(\d+(?:\.\d{1,2})?)\s*元"""),
            // "给我转账 123.45"
            Regex("""转账\s*(\d+(?:\.\d{1,2})?)\s*元?"""),
            // "+123.45" / "-123.45"
            Regex("""[+-]?\s*(\d+(?:\.\d{1,2})?)"""),
            // "123.45元"
            Regex("""(\d+(?:\.\d{1,2})?)\s*元"""),
            // "123.45块"
            Regex("""(\d+(?:\.\d{1,2})?)\s*块"""),
            // 单独数字（最后备选）
            Regex("""(\d+(?:\.\d{1,2})?)""")
        )
        for (pattern in patterns) {
            val m = pattern.find(text) ?: continue
            val v = m.groupValues[1].toDoubleOrNull()
            if (v != null && v > 0 && v < 1_000_000_000) return v
        }
        return null
    }

    private fun extractFuzzyAmount(text: String): Double? {
        // "十来块" → 10
        val m1 = Regex("""十\s*来\s*[元块]?""").find(text)
        if (m1 != null) return 10.0

        // "二十多" → 20
        val m2 = Regex("""([一二三四五六七八九十])\s*十\s*多""").find(text)
        if (m2 != null) {
            val num = chineseNumbers[m2.groupValues[1]] ?: return null
            return (num * 10).toDouble()
        }

        // "百来块" → 100
        val m3 = Regex("""百\s*来\s*[元块]?""").find(text)
        if (m3 != null) return 100.0

        return null
    }

    private fun extractChineseAmount(text: String): Double? {
        val p = Regex("""([一二两三四五六七八九十百千万亿]+)\s*[元块]?""")
        val m = p.find(text) ?: return null
        return parseChineseNumber(m.groupValues[1])
    }

    private fun parseChineseNumber(chinese: String): Double? {
        var result = 0.0
        var temp = 0.0
        for (char in chinese) {
            val s = char.toString()
            val num = chineseNumbers[s] ?: continue
            when {
                num >= 10 -> {
                    if (temp == 0.0) temp = 1.0
                    result += temp * num
                    temp = 0.0
                }
                else -> temp = num.toDouble()
            }
        }
        return if (result == 0.0 && temp == 0.0) null else result + temp
    }

    // ── 分类识别 ──
    private fun detectCategory(text: String, transactionType: TransactionType, amountInfo: AmountInfo): String {
        // 收入类型 → 收入
        if (transactionType == TransactionType.INCOME) {
            for (k in categoryKeywords["收入"] ?: emptyList()) {
                if (text.contains(k)) return "收入"
            }
            return "收入"
        }

        // 多关键词打分
        val scores = mutableMapOf<String, Int>()
        for ((category, keywords) in categoryKeywords) {
            if (category == "收入") continue
            var score = 0
            var fullMatchCount = 0
            for (keyword in keywords) {
                if (text.contains(keyword)) {
                    // 完整词匹配得分更高
                    val points = if (keyword.length >= 2) 3 else 1
                    score += points
                    fullMatchCount++
                }
            }
            // 额外加分：关键词占比越高越好
            if (fullMatchCount > 0) {
                score += fullMatchCount
            }
            if (score > 0) {
                scores[category] = score
            }
        }

        return scores.maxByOrNull { it.value }?.key ?: "其他"
    }

    // ── 商户/描述 ──
    private fun extractMerchant(text: String, category: String): String {
        var merchant = text
            .replace(Regex("""记一笔"""), "")
            .replace(Regex("""[花了用付了支出消费收入收到到账发领][了]?"""), "")
            .replace(Regex("""[\d.]+\s*[元块]?"""), "")
            .replace(Regex("""[+-]?\s*[\d.]+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(40)  // 限制长度

        if (merchant.isBlank()) merchant = category
        return merchant
    }

    // ── 置信度 ──
    private fun calculateConfidence(amount: Double?, category: String, merchant: String, text: String): Float {
        var score = 0.0f
        if (amount != null && amount > 0) score += 0.35f
        if (category != "其他") score += 0.3f
        if (merchant.isNotBlank() && merchant != category) score += 0.2f
        if (text.length in 4..60) score += 0.15f
        return score.coerceIn(0f, 1f)
    }

    /** 当置信度低时返回建议 */
    fun getSuggestions(result: ParsedResult): List<String> {
        val suggestions = mutableListOf<String>()
        if (result.amount == null) suggestions.add("请说出金额，如：花了50元买饭")
        if (result.category == "其他") suggestions.add("可以补充用途，如：吃饭、打车、购物")
        if (result.merchant.isBlank() || result.merchant == result.category) {
            suggestions.add("可以补充商户名，如：在星巴克喝咖啡")
        }
        if (result.confidence < 0.5f) {
            suggestions.add("建议说完整句子，如：记一笔，花了35元在便利店买水")
        }
        return suggestions
    }
}
