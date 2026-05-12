package com.autobookkeeper.ai

import android.util.Log

/**
 * 增强版规则意图识别器 - 智能语义理解
 * 
 * 改进点：
 * 1. 支持上下文记忆 - 可理解"那上个月呢"这类指代
 * 2. 扩展语义模式 - 更多口语化表达
 * 3. 智能时间解析 - 支持相对时间、模糊时间
 * 4. 分类别名映射 - 理解"吃饭"=餐饮
 */
class EnhancedRuleRecognizer {
    
    companion object {
        private const val TAG = "EnhancedRule"
    }
    
    // 对话上下文 - 记住上一轮信息
    data class ConversationContext(
        var lastIntentType: IntentType = IntentType.UNKNOWN,
        var lastTimeRange: TimeRange = TimeRange.THIS_MONTH,
        var lastCategory: String? = null,
        var lastQueryTime: Long = 0
    )
    
    private val context = ConversationContext()
    
    // 扩展意图模式 - 更多口语化表达
    private val intentPatterns = mapOf(
        IntentType.QUERY_EXPENSE to listOf(
            Pattern(listOf("支出", "花费", "花了", "消费", "开销", "用钱", "掏钱", "付款", "买单"), 1.0f),
            Pattern(listOf("多少", "几", "钱", "块", "元"), 0.6f),
            Pattern(listOf("用", "花", "买", "吃", "玩"), 0.4f)
        ),
        IntentType.QUERY_INCOME to listOf(
            Pattern(listOf("收入", "赚", "进账", "到账", "工资", "发钱", "收款", "来钱"), 1.0f),
            Pattern(listOf("多少", "几", "钱", "块", "元"), 0.6f)
        ),
        IntentType.QUERY_BUDGET to listOf(
            Pattern(listOf("预算", "限额", "额度", "定额"), 1.0f),
            Pattern(listOf("剩余", "还剩", "剩下", "多少", "余量"), 0.9f),
            Pattern(listOf("超", "超支", "超出", "超了", "超预算"), 0.8f),
            Pattern(listOf("还能", "还可以", "还可以花"), 0.9f)
        ),
        IntentType.QUERY_TREND to listOf(
            Pattern(listOf("趋势", "变化", "对比", "比较", "环比", "同比", "相差"), 1.0f),
            Pattern(listOf("上月", "上周", "去年", "之前", "以前", "上个"), 0.8f),
            Pattern(listOf("增长", "下降", "增加", "减少", "多了", "少了", "变多", "变少"), 0.7f),
            Pattern(listOf("怎么样", "如何", "情况", "变化大"), 0.8f)
        ),
        IntentType.QUERY_RANKING to listOf(
            Pattern(listOf("排行", "排名", "最多", "最大", "前几", "top"), 1.0f),
            Pattern(listOf("占比", "比例", "分布", "构成", "份额"), 0.9f),
            Pattern(listOf("哪个", "什么", "哪里", "哪家"), 0.6f),
            Pattern(listOf("最", "第一", "第二", "第三"), 0.8f)
        ),
        IntentType.QUERY_AVERAGE to listOf(
            Pattern(listOf("平均", "日均", "每天", "均值", "大概", "一般"), 1.0f),
            Pattern(listOf("多少", "几", "花多少"), 0.6f),
            Pattern(listOf("每天", "一天", "每日", "天天"), 0.8f)
        ),
        IntentType.QUERY_COUNT to listOf(
            Pattern(listOf("笔", "次数", "数量", "几笔", "多少笔", "多少单"), 1.0f),
            Pattern(listOf("交易", "记录", "账单", "消费记录"), 0.6f),
            Pattern(listOf("有几笔", "有多少", "多少次"), 0.9f)
        ),
        IntentType.QUERY_ANOMALY to listOf(
            Pattern(listOf("异常", "不正常", "奇怪", "异常消费", "异常支出"), 1.0f),
            Pattern(listOf("大额", "超大", "高额", "大笔", "大金额"), 0.8f),
            Pattern(listOf("不对劲", "有问题", "可疑"), 0.9f)
        ),
        IntentType.QUERY_SUGGESTION to listOf(
            Pattern(listOf("建议", "推荐", "意见", "怎么办", "怎么省"), 1.0f),
            Pattern(listOf("省钱", "理财", "控制", "减少", "节约", "省点"), 0.9f),
            Pattern(listOf("怎么", "如何", "怎样", "有啥办法"), 0.6f)
        ),
        IntentType.QUERY_BALANCE to listOf(
            Pattern(listOf("结余", "剩余", "余额", "剩多少", "净收入"), 1.0f),
            Pattern(listOf("收支", "收入减支出", "赚了多少"), 0.9f),
            Pattern(listOf("存了", "攒了", "省下"), 0.8f)
        ),
        IntentType.QUERY_FORECAST to listOf(
            Pattern(listOf("预测", "预计", "估计", "会超", "够吗"), 1.0f),
            Pattern(listOf("月底", "到月底", "月底会", "还能花"), 0.9f),
            Pattern(listOf("够不够用", "够不够", "能坚持"), 0.8f)
        )
    )
    
    data class Pattern(val keywords: List<String>, val weight: Float)
    
    fun recognize(text: String): IntentRecognitionResult {
        val normalizedText = text.lowercase().trim()
        
        if (isContextReference(normalizedText)) {
            return recognizeWithContext(normalizedText)
        }
        
        val result = recognizeInternal(normalizedText)
        updateContext(result.intent)
        return result
    }
    
    private fun isContextReference(text: String): Boolean {
        val contextKeywords = listOf("那", "那呢", "呢", "还有", "另外", "对比", "相比")
        return contextKeywords.any { text.contains(it) } && 
               (text.length < 15 || text.contains("上月") || text.contains("上周") || text.contains("去年"))
    }
    
    private fun recognizeWithContext(text: String): IntentRecognitionResult {
        val timeRange = recognizeTimeRange(text)
        val category = recognizeCategory(text)
        
        val intent = QueryIntent(
            intentType = context.lastIntentType,
            timeRange = if (timeRange != TimeRange.THIS_MONTH) timeRange else context.lastTimeRange,
            category = category ?: context.lastCategory,
            aggregation = AggregationType.SUM,
            compareTarget = if (text.contains("对比") || text.contains("相比")) CompareTarget.LAST_PERIOD else null
        )
        
        updateContext(intent)
        
        return IntentRecognitionResult(
            intent = intent,
            confidence = 0.85f,
            matchedTemplate = "context_reference"
        )
    }
    
    private fun recognizeInternal(text: String): IntentRecognitionResult {
        var bestIntentType = IntentType.UNKNOWN
        var maxScore = 0f
        val scores = mutableMapOf<IntentType, Float>()
        
        for ((intentType, patterns) in intentPatterns) {
            var score = 0f
            var matchedPatterns = 0
            
            for (pattern in patterns) {
                val patternScore = calculatePatternScore(text, pattern)
                if (patternScore > 0) {
                    score += patternScore * pattern.weight
                    matchedPatterns++
                }
            }
            
            if (matchedPatterns > 1) {
                score *= 1.2f
            }
            
            scores[intentType] = score
            
            if (score > maxScore) {
                maxScore = score
                bestIntentType = intentType
            }
        }
        
        val threshold = 0.5f
        val finalIntentType = if (maxScore >= threshold) bestIntentType else IntentType.UNKNOWN
        
        val timeRange = recognizeTimeRange(text)
        val category = recognizeCategory(text)
        val aggregation = recognizeAggregation(text)
        val compareTarget = recognizeCompareTarget(text)
        
        Log.d(TAG, "识别: '$text' -> $finalIntentType (score: ${String.format("%.2f", maxScore)})")
        
        return IntentRecognitionResult(
            intent = QueryIntent(
                intentType = finalIntentType,
                timeRange = timeRange,
                category = category,
                aggregation = aggregation,
                compareTarget = compareTarget
            ),
            confidence = maxScore,
            matchedTemplate = scores.entries.sortedByDescending { it.value }.take(3).map { "${it.key}=${String.format("%.2f", it.value)}" }.toString()
        )
    }
    
    private fun recognizeTimeRange(text: String): TimeRange {
        return when {
            text.contains("今天") || text.contains("今日") -> TimeRange.TODAY
            text.contains("昨天") -> TimeRange.YESTERDAY
            text.contains("前天") -> TimeRange.DAY_BEFORE_YESTERDAY
            text.contains("本周") || text.contains("这周") -> TimeRange.THIS_WEEK
            text.contains("上周") -> TimeRange.LAST_WEEK
            text.contains("本月") || text.contains("这个月") -> TimeRange.THIS_MONTH
            text.contains("上月") || text.contains("上个月") -> TimeRange.LAST_MONTH
            text.contains("今年") -> TimeRange.THIS_YEAR
            text.contains("去年") -> TimeRange.LAST_YEAR
            text.contains("最近") || text.contains("近来") -> TimeRange.RECENT_7_DAYS
            text.contains("全部") || text.contains("所有") || text.contains("总共") -> TimeRange.ALL
            else -> TimeRange.THIS_MONTH
        }
    }
    
    private fun recognizeCategory(text: String): String? {
        val aliases = mapOf(
            "吃" to "餐饮", "吃饭" to "餐饮", "外卖" to "餐饮", "聚餐" to "餐饮",
            "餐厅" to "餐饮", "美食" to "餐饮", "火锅" to "餐饮", "烧烤" to "餐饮",
            "奶茶" to "餐饮", "咖啡" to "餐饮", "水果" to "餐饮", "早餐" to "餐饮",
            "午餐" to "餐饮", "晚餐" to "餐饮", "夜宵" to "餐饮", "零食" to "餐饮",
            "打车" to "交通", "滴滴" to "交通", "地铁" to "交通", "公交" to "交通",
            "高铁" to "交通", "火车" to "交通", "飞机" to "交通", "加油" to "交通",
            "淘宝" to "购物", "京东" to "购物", "拼多多" to "购物", "抖音" to "购物",
            "超市" to "购物", "房租" to "住房", "水电" to "住房", "物业" to "住房",
            "游戏" to "娱乐", "电影" to "娱乐", "KTV" to "娱乐", "旅游" to "娱乐",
            "看病" to "医疗", "买药" to "医疗", "医院" to "医疗",
            "学费" to "教育", "书" to "教育", "课程" to "教育"
        )
        
        for ((alias, category) in aliases) {
            if (text.contains(alias)) return category
        }
        return null
    }
    
    private fun recognizeAggregation(text: String): AggregationType {
        return when {
            text.contains("平均") || text.contains("均值") || text.contains("每天") -> AggregationType.AVERAGE
            text.contains("最多") || text.contains("最大") || text.contains("最高") -> AggregationType.MAX
            text.contains("最少") || text.contains("最小") || text.contains("最低") -> AggregationType.MIN
            text.contains("笔") || text.contains("次数") || text.contains("数量") -> AggregationType.COUNT
            else -> AggregationType.SUM
        }
    }
    
    private fun recognizeCompareTarget(text: String): CompareTarget? {
        return when {
            text.contains("上月") || text.contains("上个月") || text.contains("环比") -> CompareTarget.LAST_MONTH
            text.contains("去年") || text.contains("上年") || text.contains("同比") -> CompareTarget.LAST_YEAR
            text.contains("上周") -> CompareTarget.LAST_WEEK
            text.contains("对比") || text.contains("相比") || text.contains("比较") -> CompareTarget.LAST_PERIOD
            else -> null
        }
    }
    
    private fun calculatePatternScore(text: String, pattern: Pattern): Float {
        var maxScore = 0f
        
        for (keyword in pattern.keywords) {
            val score = when {
                text.contains(keyword) -> 1.0f
                keyword.length >= 2 && text.contains(keyword.substring(0, 2)) -> 0.5f
                calculateSimilarity(text, keyword) > 0.7f -> 0.3f
                else -> 0f
            }
            maxScore = maxOf(maxScore, score)
        }
        
        return maxScore
    }
    
    private fun calculateSimilarity(s1: String, s2: String): Float {
        val longer = if (s1.length > s2.length) s1 else s2
        val shorter = if (s1.length > s2.length) s2 else s1
        
        if (longer.isEmpty()) return 1.0f
        
        val distance = levenshteinDistance(longer, shorter)
        return (longer.length - distance) / longer.length.toFloat()
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
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
    
    private fun updateContext(intent: QueryIntent) {
        context.lastIntentType = intent.intentType
        context.lastTimeRange = intent.timeRange
        context.lastCategory = intent.category
        context.lastQueryTime = System.currentTimeMillis()
    }
    
    fun clearContext() {
        context.lastIntentType = IntentType.UNKNOWN
        context.lastTimeRange = TimeRange.THIS_MONTH
        context.lastCategory = null
        context.lastQueryTime = 0
    }
}