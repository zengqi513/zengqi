package com.autobookkeeper.ai

/**
 * 用户查询意图数据结构
 */
data class QueryIntent(
    val intentType: IntentType,
    val timeRange: TimeRange = TimeRange.CURRENT_MONTH,
    val category: String? = null,
    val aggregation: AggregationType = AggregationType.SUM,
    val compareTarget: CompareTarget? = null,
    val specificDate: Long? = null,
    val limit: Int? = null
)

enum class IntentType {
    QUERY_EXPENSE,      // 查询支出
    QUERY_INCOME,       // 查询收入
    QUERY_BUDGET,       // 查询预算
    QUERY_TREND,        // 查询趋势
    QUERY_RANKING,      // 查询排行
    QUERY_AVERAGE,      // 查询平均值
    QUERY_MAX_MIN,      // 查询最大最小
    QUERY_COUNT,        // 查询笔数
    QUERY_ANOMALY,      // 查询异常
    QUERY_SUGGESTION,   // 请求建议
    QUERY_BALANCE,      // 查询结余
    QUERY_FORECAST,     // 查询预测
    UNKNOWN             // 未知意图
}

enum class TimeRange {
    TODAY,              // 今天
    YESTERDAY,          // 昨天
    DAY_BEFORE_YESTERDAY, // 前天
    THIS_WEEK,          // 本周
    LAST_WEEK,          // 上周
    THIS_MONTH,         // 本月
    LAST_MONTH,         // 上月
    CURRENT_MONTH,      // 本月（兼容旧代码）
    THIS_YEAR,          // 今年
    CURRENT_YEAR,       // 今年（兼容旧代码）
    LAST_YEAR,          // 去年
    RECENT_7_DAYS,      // 最近7天
    RECENT_30_DAYS,     // 最近30天
    ALL,                // 全部
    ALL_TIME,           // 全部时间（兼容旧代码）
    SPECIFIC_MONTH,     // 特定月份（需配合specificDate）
    WEEKEND,            // 周末
    WEEKDAY             // 工作日
}

enum class AggregationType {
    SUM,                // 总和
    AVERAGE,            // 平均
    MAX,                // 最大
    MIN,                // 最小
    COUNT               // 计数
}

enum class CompareTarget {
    LAST_MONTH,         // 与上月比
    LAST_WEEK,          // 与上周比
    LAST_YEAR,          // 与去年比
    LAST_PERIOD,        // 与上期比
    AVERAGE             // 与平均比
}

/**
 * 意图识别结果
 */
data class IntentRecognitionResult(
    val intent: QueryIntent,
    val confidence: Float,
    val matchedTemplate: String? = null
)
