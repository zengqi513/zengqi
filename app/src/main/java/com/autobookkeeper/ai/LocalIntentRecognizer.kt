package com.autobookkeeper.ai

import android.content.Context
import android.util.Log

/**
 * 本地意图识别器 - 纯规则引擎
 * 
 * 设计思路：
 * 使用关键词匹配进行意图识别，简单快速可靠
 */
class LocalIntentRecognizer(private val context: Context) {

    // 分类别名映射（用户可能用的口语化表达）
    private val categoryAliases = mutableMapOf<String, String>()

    init {
        loadCategoryAliases()
    }

    private fun loadCategoryAliases() {
        // 餐饮相关
        categoryAliases["吃"] = "餐饮"
        categoryAliases["吃饭"] = "餐饮"
        categoryAliases["外卖"] = "餐饮"
        categoryAliases["聚餐"] = "餐饮"
        categoryAliases["餐厅"] = "餐饮"
        categoryAliases["美食"] = "餐饮"
        categoryAliases["火锅"] = "餐饮"
        categoryAliases["烧烤"] = "餐饮"
        categoryAliases["奶茶"] = "餐饮"
        categoryAliases["咖啡"] = "餐饮"
        categoryAliases["水果"] = "餐饮"

        // 交通相关
        categoryAliases["打车"] = "交通"
        categoryAliases["滴滴"] = "交通"
        categoryAliases["地铁"] = "交通"
        categoryAliases["公交"] = "交通"
        categoryAliases["高铁"] = "交通"
        categoryAliases["火车"] = "交通"
        categoryAliases["飞机"] = "交通"
        categoryAliases["加油"] = "交通"
        categoryAliases["停车"] = "交通"

        // 购物相关
        categoryAliases["淘宝"] = "购物"
        categoryAliases["京东"] = "购物"
        categoryAliases["拼多多"] = "购物"
        categoryAliases["抖音"] = "购物"
        categoryAliases["买衣服"] = "购物"
        categoryAliases["买鞋"] = "购物"
        categoryAliases["化妆品"] = "购物"
        categoryAliases["超市"] = "购物"

        // 住房相关
        categoryAliases["房租"] = "住房"
        categoryAliases["水电"] = "住房"
        categoryAliases["物业"] = "住房"
        categoryAliases["燃气"] = "住房"

        // 娱乐相关
        categoryAliases["游戏"] = "娱乐"
        categoryAliases["电影"] = "娱乐"
        categoryAliases["KTV"] = "娱乐"
        categoryAliases["旅游"] = "娱乐"
        categoryAliases["会员"] = "娱乐"

        // 医疗相关
        categoryAliases["看病"] = "医疗"
        categoryAliases["买药"] = "医疗"
        categoryAliases["医院"] = "医疗"

        // 教育相关
        categoryAliases["学费"] = "教育"
        categoryAliases["书"] = "教育"
        categoryAliases["课程"] = "教育"
        categoryAliases["培训"] = "教育"
    }

    /**
     * 识别用户查询意图（纯规则引擎）
     */
    suspend fun recognize(query: String): QueryIntent {
        val normalized = query.lowercase().trim()
        
        Log.d("LocalIntentRecognizer", "Recognizing query: $query")
        
        return recognizeWithRules(normalized)
    }

    /**
     * 纯规则引擎识别
     */
    private fun recognizeWithRules(query: String): QueryIntent {
        // 1. 识别意图类型
        val intentType = recognizeIntentType(query)

        // 2. 识别时间范围
        val timeRange = recognizeTimeRange(query)

        // 3. 识别分类
        val category = recognizeCategory(query)

        // 4. 识别聚合方式
        val aggregation = recognizeAggregation(query)

        // 5. 识别对比目标
        val compareTarget = recognizeCompareTarget(query)

        return QueryIntent(
            intentType = intentType,
            timeRange = timeRange,
            category = category,
            aggregation = aggregation,
            compareTarget = compareTarget
        )
    }

    private fun recognizeIntentType(query: String): IntentType {
        return when {
            // 支出相关
            query.contains("花了") || query.contains("支出") || query.contains("消费") ||
            query.contains("用了") || query.contains("多少钱") || query.contains("多少") && 
            (query.contains("花") || query.contains("用") || query.contains("买")) -> {
                IntentType.QUERY_EXPENSE
            }

            // 收入相关
            query.contains("收入") || query.contains("赚") || query.contains("工资") ||
            query.contains("到账") || query.contains("收到") || query.contains("进账") -> {
                IntentType.QUERY_INCOME
            }

            // 预算相关
            query.contains("预算") || query.contains("还剩") || query.contains("超支") -> {
                IntentType.QUERY_BUDGET
            }

            // 趋势相关
            query.contains("趋势") || query.contains("变化") || query.contains("走势") ||
            query.contains("增加") || query.contains("减少") || query.contains("多了") ||
            query.contains("少了") -> {
                IntentType.QUERY_TREND
            }

            // 排行相关
            query.contains("最多") || query.contains("最大") || query.contains("排行") ||
            query.contains("排名") || query.contains("前几") || query.contains("哪些") ||
            query.contains("什么") && query.contains("最") ||
            query.contains("哪个") || query.contains("占比") || query.contains("比例") ||
            query.contains("分布") || query.contains("构成") -> {
                IntentType.QUERY_RANKING
            }

            // 平均相关
            query.contains("平均") || query.contains("日均") || query.contains("每天") ||
            query.contains("每笔") -> {
                IntentType.QUERY_AVERAGE
            }

            // 最大最小
            query.contains("最大") || query.contains("最小") || query.contains("最高") ||
            query.contains("最低") || query.contains("最贵") || query.contains("最便宜") -> {
                IntentType.QUERY_MAX_MIN
            }

            // 笔数
            query.contains("几笔") || query.contains("多少笔") || query.contains("次数") ||
            query.contains("频次") -> {
                IntentType.QUERY_COUNT
            }

            // 异常
            query.contains("异常") || query.contains("奇怪") || query.contains("不对") ||
            query.contains("提醒") -> {
                IntentType.QUERY_ANOMALY
            }

            // 建议
            query.contains("建议") || query.contains("怎么办") || query.contains("怎么省") ||
            query.contains("优化") || query.contains("控制") -> {
                IntentType.QUERY_SUGGESTION
            }

            else -> IntentType.UNKNOWN
        }
    }

    private fun recognizeTimeRange(query: String): TimeRange {
        return when {
            // 今天
            query.contains("今天") || query.contains("今日") -> TimeRange.TODAY

            // 昨天
            query.contains("昨天") || query.contains("昨日") -> TimeRange.YESTERDAY

            // 本周/这周
            query.contains("本周") || query.contains("这周") || query.contains("星期") ||
            query.contains("周天") -> TimeRange.THIS_WEEK

            // 上周
            query.contains("上周") || query.contains("上个星期") -> TimeRange.LAST_WEEK

            // 本月/这个月
            query.contains("本月") || query.contains("这个月") || 
            (!query.contains("上个") && !query.contains("下个") && query.contains("月") && 
             !query.contains("年")) -> TimeRange.CURRENT_MONTH

            // 上月/上个月
            query.contains("上月") || query.contains("上个月") -> TimeRange.LAST_MONTH

            // 本年/今年
            query.contains("本年") || query.contains("今年") || query.contains("年度") -> TimeRange.CURRENT_YEAR

            // 去年
            query.contains("去年") -> TimeRange.LAST_YEAR

            // 全部
            query.contains("全部") || query.contains("所有") || query.contains("总共") ||
            query.contains("一共") -> TimeRange.ALL_TIME

            // 周末
            query.contains("周末") || query.contains("周六") || query.contains("周日") ||
            query.contains("星期六") || query.contains("星期天") -> TimeRange.WEEKEND

            // 工作日
            query.contains("工作日") || query.contains("上班") || query.contains("周一") ||
            query.contains("周五") -> TimeRange.WEEKDAY

            else -> TimeRange.CURRENT_MONTH
        }
    }

    private fun recognizeCategory(query: String): String? {
        // 先检查别名映射
        for ((alias, category) in categoryAliases) {
            if (query.contains(alias)) {
                return category
            }
        }

        // 检查标准分类名称
        val standardCategories = listOf(
            "餐饮", "交通", "购物", "住房", "娱乐", "医疗", "教育",
            "通讯", "美容", "宠物", "书籍", "礼物", "运动", "保险",
            "旅游", "数码", "家居", "蔬菜", "零食", "饮料"
        )

        for (cat in standardCategories) {
            if (query.contains(cat)) {
                return cat
            }
        }

        return null
    }

    private fun recognizeAggregation(query: String): AggregationType {
        return when {
            query.contains("平均") || query.contains("日均") || query.contains("每天") ||
            query.contains("每笔") -> AggregationType.AVERAGE

            query.contains("几笔") || query.contains("多少笔") || query.contains("次数") ||
            query.contains("频次") || query.contains("几单") -> AggregationType.COUNT

            query.contains("最大") || query.contains("最高") || query.contains("最贵") -> AggregationType.MAX

            query.contains("最小") || query.contains("最低") || query.contains("最便宜") -> AggregationType.MIN

            else -> AggregationType.SUM
        }
    }

    private fun recognizeCompareTarget(query: String): CompareTarget? {
        return when {
            query.contains("上月") || query.contains("上个月") || query.contains("之前") || 
            query.contains("以前") -> CompareTarget.LAST_MONTH

            query.contains("去年") || query.contains("上年") -> CompareTarget.LAST_YEAR

            query.contains("平均") && !query.contains("日均") -> CompareTarget.AVERAGE

            else -> null
        }
    }

    /**
     * 根据意图生成自然语言回复模板
     */
    fun generateResponseTemplate(intent: QueryIntent): String {
        return when (intent.intentType) {
            IntentType.QUERY_EXPENSE -> buildExpenseResponse(intent)
            IntentType.QUERY_INCOME -> buildIncomeResponse(intent)
            IntentType.QUERY_BUDGET -> buildBudgetResponse(intent)
            IntentType.QUERY_TREND -> buildTrendResponse(intent)
            IntentType.QUERY_RANKING -> buildRankingResponse(intent)
            IntentType.QUERY_AVERAGE -> buildAverageResponse(intent)
            IntentType.QUERY_MAX_MIN -> buildMaxMinResponse(intent)
            IntentType.QUERY_COUNT -> buildCountResponse(intent)
            IntentType.QUERY_ANOMALY -> buildAnomalyResponse(intent)
            IntentType.QUERY_SUGGESTION -> buildSuggestionResponse(intent)
            IntentType.QUERY_BALANCE -> "正在计算收支结余..."
            IntentType.QUERY_FORECAST -> "正在预测月底支出..."
            IntentType.UNKNOWN -> "抱歉，我没理解您的问题。可以试试问：\n• 本月花了多少钱\n• 餐饮支出多少\n• 和上月比怎么样"
        }
    }

    private fun buildExpenseResponse(intent: QueryIntent): String {
        val timeDesc = getTimeRangeDescription(intent.timeRange)
        val catDesc = intent.category?.let { "${it}分类" } ?: ""
        return "正在查询${timeDesc}${catDesc}的支出情况..."
    }

    private fun buildIncomeResponse(intent: QueryIntent): String {
        val timeDesc = getTimeRangeDescription(intent.timeRange)
        return "正在查询${timeDesc}的收入情况..."
    }

    private fun buildBudgetResponse(intent: QueryIntent): String {
        return "正在查询预算使用情况..."
    }

    private fun buildTrendResponse(intent: QueryIntent): String {
        val compareDesc = intent.compareTarget?.let {
            when (it) {
                CompareTarget.LAST_MONTH -> "与上月对比"
                CompareTarget.LAST_WEEK -> "与上周对比"
                CompareTarget.LAST_YEAR -> "与去年对比"
                CompareTarget.LAST_PERIOD -> "与上期对比"
                CompareTarget.AVERAGE -> "与平均值对比"
            }
        } ?: "趋势分析"
        return "正在分析支出${compareDesc}..."
    }

    private fun buildRankingResponse(intent: QueryIntent): String {
        val timeDesc = getTimeRangeDescription(intent.timeRange)
        return "正在查询${timeDesc}支出排行..."
    }

    private fun buildAverageResponse(intent: QueryIntent): String {
        val timeDesc = getTimeRangeDescription(intent.timeRange)
        val catDesc = intent.category?.let { "${it}的" } ?: ""
        return "正在计算${timeDesc}${catDesc}平均支出..."
    }

    private fun buildMaxMinResponse(intent: QueryIntent): String {
        val timeDesc = getTimeRangeDescription(intent.timeRange)
        return "正在查找${timeDesc}最大支出..."
    }

    private fun buildCountResponse(intent: QueryIntent): String {
        val timeDesc = getTimeRangeDescription(intent.timeRange)
        val catDesc = intent.category?.let { "${it}的" } ?: ""
        return "正在统计${timeDesc}${catDesc}交易笔数..."
    }

    private fun buildAnomalyResponse(intent: QueryIntent): String {
        return "正在检查异常支出..."
    }

    private fun buildSuggestionResponse(intent: QueryIntent): String {
        return "正在分析您的消费习惯并生成建议..."
    }

    private fun getTimeRangeDescription(range: TimeRange): String {
        return when (range) {
            TimeRange.TODAY -> "今天"
            TimeRange.YESTERDAY -> "昨天"
            TimeRange.DAY_BEFORE_YESTERDAY -> "前天"
            TimeRange.THIS_WEEK -> "本周"
            TimeRange.LAST_WEEK -> "上周"
            TimeRange.THIS_MONTH, TimeRange.CURRENT_MONTH -> "本月"
            TimeRange.LAST_MONTH -> "上月"
            TimeRange.THIS_YEAR, TimeRange.CURRENT_YEAR -> "今年"
            TimeRange.LAST_YEAR -> "去年"
            TimeRange.RECENT_7_DAYS -> "最近7天"
            TimeRange.RECENT_30_DAYS -> "最近30天"
            TimeRange.ALL, TimeRange.ALL_TIME -> "全部"
            TimeRange.SPECIFIC_MONTH -> "该月"
            TimeRange.WEEKEND -> "周末"
            TimeRange.WEEKDAY -> "工作日"
        }
    }
}
