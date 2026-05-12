package com.autobookkeeper.data

import java.util.Date

/**
 * 账单分析数据模型
 */
data class MonthlyAnalysisData(
    val year: Int,
    val month: Int,
    val totalExpense: Double,
    val totalIncome: Double,
    val transactionCount: Int,
    val categoryBreakdown: List<CategoryAnalysis>,
    val dailyAverage: Double,
    val topExpenseCategory: String?,
    val topExpenseAmount: Double,
    val budgetStatus: BudgetStatusInfo?,
    val monthOverMonthChange: MonthOverMonthChange?,
    val anomalies: List<AnomalyItem>
)

data class CategoryAnalysis(
    val categoryName: String,
    val categoryIcon: String,
    val amount: Double,
    val percentage: Double,
    val transactionCount: Int,
    val monthOverMonthChange: Double // 环比变化率
)

// BudgetStatus 定义在 Budget.kt 中，这里使用 typealias 或直接使用
// 为避免循环依赖，使用独立的数据类

data class BudgetStatusInfo(
    val totalBudget: Double,
    val spent: Double,
    val remaining: Double,
    val usageRate: Double, // 0-100
    val isOverBudget: Boolean
)

data class MonthOverMonthChange(
    val expenseChangePercent: Double,
    val incomeChangePercent: Double,
    val isExpenseIncreased: Boolean,
    val isIncomeIncreased: Boolean
)

data class AnomalyItem(
    val type: AnomalyType,
    val categoryName: String?,
    val amount: Double,
    val description: String,
    val severity: AnomalySeverity
)

enum class AnomalyType {
    UNUSUAL_HIGH_EXPENSE,    // 某类支出异常高
    UNUSUAL_LOW_EXPENSE,     // 某类支出异常低
    BUDGET_WARNING,          // 预算预警
    INCOME_DROP,             // 收入下降
    FIRST_TIME_MERCHANT,     // 首次消费商户
    RECURRING_SUBSCRIPTION   // 定期订阅扣费
}

enum class AnomalySeverity {
    INFO, WARNING, CRITICAL
}

/**
 * AI 分析结论和建议
 */
data class AIAnalysisResult(
    val summary: String,                    // 总体评价
    val insights: List<String>,             // 关键洞察（3-5条）
    val suggestions: List<SuggestionItem>,  // 具体建议
    val riskAlerts: List<String>,           // 风险提示
    val positiveHighlights: List<String>    // 积极亮点
)

data class SuggestionItem(
    val category: String?,      // 针对哪个分类，null 表示通用
    val title: String,
    val description: String,
    val actionable: Boolean,    // 是否可执行（如"减少外出就餐"vs"关注支出"）
    val potentialSavings: Double? // 预计可节省金额
)

/**
 * 自然语言查询结果
 */
data class NLQueryResult(
    val query: String,
    val answer: String,
    val relatedData: List<RelatedDataPoint>,
    val suggestedFollowUps: List<String>
)

data class RelatedDataPoint(
    val type: String,  // "amount", "percentage", "trend", "comparison"
    val label: String,
    val value: String,
    val context: String?
)

/**
 * 用户消费习惯画像（用于个性化建议）
 */
data class UserSpendingProfile(
    val averageMonthlyExpense: Double,
    val averageMonthlyIncome: Double,
    val stableCategories: List<String>,      // 支出稳定的分类
    val volatileCategories: List<String>,    // 波动大的分类
    val frequentMerchants: List<String>,     // 常去商户
    val spendingPattern: SpendingPattern,    // 消费模式
    val riskTolerance: RiskTolerance         // 风险承受能力（从预算设置推断）
)

enum class SpendingPattern {
    STEADY,         // 稳定型：每月支出波动小
    WEEKEND_HEAVY,  // 周末消费型
    IMPULSE,        // 冲动消费型：某类支出突然激增
    PLANNED         // 计划型：预算执行率高
}

enum class RiskTolerance {
    CONSERVATIVE,   // 保守：预算设置严格，关注节省
    MODERATE,       // 适中
    AGGRESSIVE      // 激进：预算宽松，关注生活品质
}
