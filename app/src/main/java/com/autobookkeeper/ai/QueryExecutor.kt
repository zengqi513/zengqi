package com.autobookkeeper.ai

import com.autobookkeeper.data.TransactionDao
import com.autobookkeeper.data.BudgetDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * AI查询执行器 - 根据识别的意图执行数据库查询
 * 
 * 支持统一的 QueryIntent data class
 */
class QueryExecutor(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) {

    /**
     * 执行查询并返回自然语言结果
     */
    suspend fun execute(intent: QueryIntent): String = withContext(Dispatchers.IO) {
        try {
            when (intent.intentType) {
                IntentType.QUERY_EXPENSE -> executeExpenseQuery(intent)
                IntentType.QUERY_INCOME -> executeIncomeQuery(intent)
                IntentType.QUERY_BUDGET -> executeBudgetQuery(intent)
                IntentType.QUERY_TREND -> executeTrendQuery(intent)
                IntentType.QUERY_RANKING -> executeRankingQuery(intent)
                IntentType.QUERY_AVERAGE -> executeAverageQuery(intent)
                IntentType.QUERY_MAX_MIN -> executeMaxMinQuery(intent)
                IntentType.QUERY_COUNT -> executeCountQuery(intent)
                IntentType.QUERY_ANOMALY -> executeAnomalyQuery(intent)
                IntentType.QUERY_SUGGESTION -> executeSuggestionQuery(intent)
                IntentType.QUERY_BALANCE -> executeBalanceQuery(intent)
                IntentType.QUERY_FORECAST -> executeForecastQuery(intent)
                IntentType.UNKNOWN -> getHelpMessage()
            }
        } catch (e: Exception) {
            "查询出错了：${e.message}"
        }
    }

    private suspend fun executeExpenseQuery(intent: QueryIntent): String {
        val (start, end) = getTimeRange(intent.timeRange, intent.specificDate)
        val transactions = transactionDao.getByDateRangeSync(start, end)
            .filter { it.amount < 0 }
        
        // 如果有指定分类，进一步过滤
        val filtered = intent.category?.let { cat ->
            transactions.filter { it.categoryName == cat }
        } ?: transactions
        
        val total = filtered.sumOf { -it.amount }
        val count = filtered.size
        
        val timeDesc = getTimeRangeDescription(intent.timeRange)
        val catDesc = intent.category?.let { "【${it}】" } ?: ""
        
        return if (filtered.isEmpty()) {
            "${timeDesc}${catDesc}还没有支出记录"
        } else {
            "${timeDesc}${catDesc}共支出 **${String.format("%.0f", total)}元**，共${count}笔交易\n" +
            "日均支出约 ${String.format("%.0f", total / getDaysInRange(start, end))}元"
        }
    }

    private suspend fun executeIncomeQuery(intent: QueryIntent): String {
        val (start, end) = getTimeRange(intent.timeRange, intent.specificDate)
        val transactions = transactionDao.getByDateRangeSync(start, end)
            .filter { it.amount > 0 }
        
        val total = transactions.sumOf { it.amount }
        val count = transactions.size
        val timeDesc = getTimeRangeDescription(intent.timeRange)
        
        return if (transactions.isEmpty()) {
            "${timeDesc}还没有收入记录"
        } else {
            "${timeDesc}共收入 **${String.format("%.0f", total)}元**，共${count}笔"
        }
    }

    private suspend fun executeBudgetQuery(intent: QueryIntent): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        
        val budget = budgetDao.getBudgetForMonthSync(year, month)
        val (start, end) = getCurrentMonthRange()
        val spent = transactionDao.getByDateRangeSync(start, end)
            .filter { it.amount < 0 }
            .sumOf { -it.amount }
        
        return if (budget != null) {
            val remaining = budget.amount - spent
            val percentage = if (budget.amount > 0) (spent / budget.amount) * 100 else 0.0
            
            when {
                remaining > 0 -> "本月预算 **${String.format("%.0f", budget.amount)}元**\n" +
                    "已支出 ${String.format("%.0f", spent)}元（${String.format("%.1f", percentage)}%）\n" +
                    "还剩 **${String.format("%.0f", remaining)}元**"
                else -> "本月预算 **${String.format("%.0f", budget.amount)}元**\n" +
                    "已超支 **${String.format("%.0f", -remaining)}元**"
            }
        } else {
            "本月未设置预算\n已支出 ${String.format("%.0f", spent)}元"
        }
    }

    private suspend fun executeTrendQuery(intent: QueryIntent): String {
        val (start, end) = getCurrentMonthRange()
        val (prevStart, prevEnd) = getPreviousMonthRange()
        
        val currentExpense = transactionDao.getByDateRangeSync(start, end)
            .filter { it.amount < 0 }
            .sumOf { -it.amount }
        val prevExpense = transactionDao.getByDateRangeSync(prevStart, prevEnd)
            .filter { it.amount < 0 }
            .sumOf { -it.amount }
        
        val change = if (prevExpense > 0) {
            ((currentExpense - prevExpense) / prevExpense) * 100
        } else 0.0
        
        return when {
            change > 0 -> "本月支出 ${String.format("%.0f", currentExpense)}元\n" +
                "比上月增加 ${String.format("%.1f", change)}%"
            change < 0 -> "本月支出 ${String.format("%.0f", currentExpense)}元\n" +
                "比上月减少 ${String.format("%.1f", -change)}%"
            else -> "本月支出与上月持平"
        }
    }

    private suspend fun executeRankingQuery(intent: QueryIntent): String {
        val (start, end) = getTimeRange(intent.timeRange, intent.specificDate)
        val transactions = transactionDao.getByDateRangeSync(start, end)
            .filter { it.amount < 0 }
        
        val categoryMap = transactions.groupBy { it.categoryName }
            .map { (name, list) ->
                name to list.sumOf { -it.amount }
            }
            .sortedByDescending { it.second }
            .take(5)
        
        val total = categoryMap.sumOf { it.second }
        val timeDesc = getTimeRangeDescription(intent.timeRange)
        
        return if (categoryMap.isEmpty()) {
            "${timeDesc}还没有支出记录"
        } else {
            val sb = StringBuilder("${timeDesc}支出排行：\n")
            categoryMap.forEachIndexed { index, (name, amount) ->
                val percentage = if (total > 0) (amount / total) * 100 else 0.0
                sb.append("${index + 1}. ${name}: ${String.format("%.0f", amount)}元 (${String.format("%.1f", percentage)}%)\n")
            }
            sb.toString().trim()
        }
    }

    private suspend fun executeAverageQuery(intent: QueryIntent): String {
        val (start, end) = getTimeRange(intent.timeRange, intent.specificDate)
        val transactions = transactionDao.getByDateRangeSync(start, end)
            .filter { it.amount < 0 }
        
        val total = transactions.sumOf { -it.amount }
        val days = getDaysInRange(start, end)
        val timeDesc = getTimeRangeDescription(intent.timeRange)
        
        return if (transactions.isEmpty()) {
            "${timeDesc}还没有支出记录"
        } else {
            "${timeDesc}日均支出 **${String.format("%.0f", total / days)}元**\n" +
            "总支出 ${String.format("%.0f", total)}元，共${days}天"
        }
    }

    private suspend fun executeMaxMinQuery(intent: QueryIntent): String {
        val (start, end) = getTimeRange(intent.timeRange, intent.specificDate)
        val transactions = transactionDao.getByDateRangeSync(start, end)
            .filter { it.amount < 0 }
        
        return if (transactions.isEmpty()) {
            "${getTimeRangeDescription(intent.timeRange)}还没有支出记录"
        } else {
            val maxTxn = transactions.minByOrNull { it.amount } // amount是负数，最小的是最大支出
            val minTxn = transactions.maxByOrNull { it.amount } // 最大负数是最小支出
            
            "${getTimeRangeDescription(intent.timeRange)}支出统计：\n" +
            "• 最大单笔：${maxTxn?.categoryName} ${String.format("%.0f", -(maxTxn?.amount ?: 0.0))}元\n" +
            "• 最小单笔：${minTxn?.categoryName} ${String.format("%.0f", -(minTxn?.amount ?: 0.0))}元"
        }
    }

    private suspend fun executeCountQuery(intent: QueryIntent): String {
        val (start, end) = getTimeRange(intent.timeRange, intent.specificDate)
        val transactions = transactionDao.getByDateRangeSync(start, end)
        
        val expenseCount = transactions.count { it.amount < 0 }
        val incomeCount = transactions.count { it.amount > 0 }
        val timeDesc = getTimeRangeDescription(intent.timeRange)
        
        return "${timeDesc}共 **${transactions.size}笔** 交易\n" +
            "• 支出：${expenseCount}笔\n" +
            "• 收入：${incomeCount}笔"
    }

    private suspend fun executeAnomalyQuery(intent: QueryIntent): String {
        val (start, end) = getTimeRange(intent.timeRange, intent.specificDate)
        val transactions = transactionDao.getByDateRangeSync(start, end)
            .filter { it.amount < 0 }
        
        if (transactions.isEmpty()) {
            return "${getTimeRangeDescription(intent.timeRange)}还没有支出记录"
        }
        
        val total = transactions.sumOf { -it.amount }
        val avgAmount = total / transactions.size
        
        // 找出异常大额支出（超过平均值3倍且大于500）
        val anomalies = transactions.filter { 
            -it.amount > avgAmount * 3 && -it.amount > 500 
        }
        
        return if (anomalies.isEmpty()) {
            "${getTimeRangeDescription(intent.timeRange)}消费正常，未发现异常大额支出\n" +
            "平均单笔支出 ${String.format("%.0f", avgAmount)}元"
        } else {
            val sb = StringBuilder("发现 **${anomalies.size}笔** 异常大额支出：\n")
            anomalies.sortedBy { it.amount }
                .take(3)
                .forEach { 
                    sb.append("• ${it.categoryName}: ${String.format("%.0f", -it.amount)}元\n")
                }
            sb.toString().trim()
        }
    }

    private suspend fun executeSuggestionQuery(intent: QueryIntent): String {
        val (start, end) = getCurrentMonthRange()
        val transactions = transactionDao.getByDateRangeSync(start, end)
            .filter { it.amount < 0 }
        
        if (transactions.isEmpty()) {
            return "本月还没有支出记录，建议开始记账"
        }
        
        val total = transactions.sumOf { -it.amount }
        val categoryMap = transactions.groupBy { it.categoryName }
            .map { (name, list) -> name to list.sumOf { -it.amount } }
            .sortedByDescending { it.second }
        
        val suggestions = mutableListOf<String>()
        
        // 预算检查
        val cal = Calendar.getInstance()
        val budget = budgetDao.getBudgetForMonthSync(
            cal.get(Calendar.YEAR), 
            cal.get(Calendar.MONTH) + 1
        )
        budget?.also {
            if (total > it.amount) {
                suggestions.add("已超预算 ${String.format("%.0f", total - it.amount)}元，建议控制消费")
            } else if (total > it.amount * 0.8) {
                suggestions.add("预算使用已达 ${String.format("%.0f", (total / it.amount) * 100)}%，注意控制")
            }
        }
        
        // 分类建议
        categoryMap.firstOrNull()?.also { (name, amount) ->
            val percentage = if (total > 0) (amount / total) * 100 else 0.0
            if (percentage > 40) {
                suggestions.add("${name}支出占比 ${String.format("%.1f", percentage)}%，建议审视必要性")
            }
        }
        
        return if (suggestions.isEmpty()) {
            "本月消费状况良好，继续保持！"
        } else {
            suggestions.joinToString("\n")
        }
    }

    private suspend fun executeBalanceQuery(intent: QueryIntent): String {
        val (start, end) = getTimeRange(intent.timeRange, intent.specificDate)
        val transactions = transactionDao.getByDateRangeSync(start, end)
        
        val income = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val expense = transactions.filter { it.amount < 0 }.sumOf { -it.amount }
        val balance = income - expense
        
        val timeDesc = getTimeRangeDescription(intent.timeRange)
        
        return when {
            balance > 0 -> "${timeDesc}结余 **${String.format("%.0f", balance)}元** ✅\n" +
                "收入 ${String.format("%.0f", income)}元，支出 ${String.format("%.0f", expense)}元\n" +
                "存下了收入的 ${String.format("%.1f", (balance / income) * 100)}%，不错！"
            balance < 0 -> "${timeDesc}超支 **${String.format("%.0f", -balance)}元** ⚠️\n" +
                "收入 ${String.format("%.0f", income)}元，支出 ${String.format("%.0f", expense)}元\n" +
                "支出是收入的 ${String.format("%.1f", (expense / income) * 100)}%，建议控制消费"
            else -> "${timeDesc}收支平衡，收入支出都是 ${String.format("%.0f", income)}元"
        }
    }

    private suspend fun executeForecastQuery(intent: QueryIntent): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val remainingDays = daysInMonth - day + 1
        
        val budget = budgetDao.getBudgetForMonthSync(year, month)
        val (start, end) = getCurrentMonthRange()
        val spent = transactionDao.getByDateRangeSync(start, end)
            .filter { it.amount < 0 }
            .sumOf { -it.amount }
        
        return if (budget != null) {
            val remaining = budget.amount - spent
            val dailyBudget = remaining / remainingDays
            val projectedTotal = spent + (spent / day) * remainingDays
            
            when {
                projectedTotal > budget.amount -> {
                    val overAmount = projectedTotal - budget.amount
                    "按当前趋势，月底预计超支 **${String.format("%.0f", overAmount)}元** 📈\n" +
                    "剩余 ${remainingDays}天，建议每天控制在 **${String.format("%.0f", dailyBudget)}元** 以内"
                }
                else -> {
                    val saveAmount = budget.amount - projectedTotal
                    "按当前趋势，月底预计能结余 **${String.format("%.0f", saveAmount)}元** ✅\n" +
                    "剩余 ${remainingDays}天，每天可花 **${String.format("%.0f", dailyBudget)}元**"
                }
            }
        } else {
            val avgDaily = spent / day
            val projected = avgDaily * daysInMonth
            "本月未设预算\n" +
            "已支出 ${String.format("%.0f", spent)}元，日均 ${String.format("%.0f", avgDaily)}元\n" +
            "按此趋势，月底预计支出 ${String.format("%.0f", projected)}元"
        }
    }

    private fun getHelpMessage(): String {
        return "我可以帮您分析账单，试试这样问：\n\n" +
            "**查支出收入**\n" +
            "• 本月花了多少钱 / 收入多少\n" +
            "• 餐饮花了多少 / 昨天花了多少\n\n" +
            "**查预算**\n" +
            "• 预算还剩多少 / 还能花多少\n" +
            "• 月底会不会超支\n\n" +
            "**对比分析**\n" +
            "• 和上月比怎么样 / 比上月多吗\n" +
            "• 支出最多的是什么 / 占比多少\n\n" +
            "**其他**\n" +
            "• 平均每天花多少 / 有几笔交易\n" +
            "• 本月结余多少 / 有什么建议"
    }

    // ============ 工具方法 ============

    private fun getTimeRange(range: TimeRange, specificDate: Long? = null): Pair<Long, Long> {
        return when (range) {
            TimeRange.TODAY -> getTodayRange()
            TimeRange.YESTERDAY -> getYesterdayRange()
            TimeRange.DAY_BEFORE_YESTERDAY -> getDayBeforeYesterdayRange()
            TimeRange.THIS_WEEK -> getThisWeekRange()
            TimeRange.LAST_WEEK -> getLastWeekRange()
            TimeRange.THIS_MONTH, TimeRange.CURRENT_MONTH -> getCurrentMonthRange()
            TimeRange.LAST_MONTH -> getPreviousMonthRange()
            TimeRange.THIS_YEAR, TimeRange.CURRENT_YEAR -> getCurrentYearRange()
            TimeRange.LAST_YEAR -> getLastYearRange()
            TimeRange.RECENT_7_DAYS -> getRecentDaysRange(7)
            TimeRange.RECENT_30_DAYS -> getRecentDaysRange(30)
            TimeRange.ALL, TimeRange.ALL_TIME -> 0L to System.currentTimeMillis()
            TimeRange.SPECIFIC_MONTH -> {
                specificDate?.let { getMonthRangeFromDate(it) } ?: getCurrentMonthRange()
            }
            TimeRange.WEEKEND -> getCurrentMonthRange() // 简化处理
            TimeRange.WEEKDAY -> getCurrentMonthRange() // 简化处理
        }
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

    private fun getTodayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return start to cal.timeInMillis
    }

    private fun getYesterdayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return start to cal.timeInMillis
    }

    private fun getThisWeekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        return start to cal.timeInMillis
    }

    private fun getLastWeekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        val start = cal.timeInMillis
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        return start to cal.timeInMillis
    }

    private fun getCurrentMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    private fun getPreviousMonthRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, -1)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    private fun getCurrentYearRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.YEAR, 1)
        return start to cal.timeInMillis
    }

    private fun getLastYearRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.YEAR, -1)
        val start = cal.timeInMillis
        cal.add(Calendar.YEAR, 1)
        return start to cal.timeInMillis
    }

    private fun getMonthRangeFromDate(date: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    private fun getDayBeforeYesterdayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_MONTH, -2)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return start to cal.timeInMillis
    }

    private fun getRecentDaysRange(days: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.DAY_OF_MONTH, -(days - 1))
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, days + 1)
        return start to cal.timeInMillis
    }

    private fun getDaysInRange(start: Long, end: Long): Int {
        return ((end - start) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
    }
}
