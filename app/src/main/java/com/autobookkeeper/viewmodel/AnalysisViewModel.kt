package com.autobookkeeper.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autobookkeeper.App
import com.autobookkeeper.ai.*
import com.autobookkeeper.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * AI 账单分析 ViewModel - 纯规则引擎
 * 
 * 使用增强规则引擎进行意图识别，无需 ONNX 模型
 */
class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = App.instance.database.transactionDao()
    private val budgetDao = App.instance.database.budgetDao()

    // 意图识别器 - 纯规则引擎
    private val enhancedRecognizer = EnhancedRuleRecognizer()
    private val queryExecutor = QueryExecutor(dao, budgetDao)
    
    // 模型状态
    private val _modelStatus = MutableStateFlow<ModelStatus>(ModelStatus.READY("规则引擎"))
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _currentMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val currentMonth: StateFlow<Int> = _currentMonth.asStateFlow()

    private val _monthlyAnalysisData = MutableStateFlow<MonthlyAnalysisData?>(null)
    val monthlyAnalysisData: StateFlow<MonthlyAnalysisData?> = _monthlyAnalysisData.asStateFlow()

    private val _aiAnalysisResult = MutableStateFlow<AIAnalysisResult?>(null)
    val aiAnalysisResult: StateFlow<AIAnalysisResult?> = _aiAnalysisResult.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMonthAnalysis(_currentYear.value, _currentMonth.value)
    }

    fun loadMonthAnalysis(year: Int, month: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val data = withContext(Dispatchers.IO) { computeMonthlyAnalysis(year, month) }
                _monthlyAnalysisData.value = data
                _aiAnalysisResult.value = withContext(Dispatchers.Default) { generateAIAnalysis(data) }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun previousMonth() {
        var y = _currentYear.value
        var m = _currentMonth.value - 1
        if (m < 1) { m = 12; y-- }
        _currentYear.value = y
        _currentMonth.value = m
        loadMonthAnalysis(y, m)
    }

    fun nextMonth() {
        var y = _currentYear.value
        var m = _currentMonth.value + 1
        if (m > 12) { m = 1; y++ }
        _currentYear.value = y
        _currentMonth.value = m
        loadMonthAnalysis(y, m)
    }

    private suspend fun computeMonthlyAnalysis(year: Int, month: Int): MonthlyAnalysisData {
        val (start, end) = getMonthRange(year, month)
        val txns = dao.getByDateRangeSync(start, end)
        val expenses = txns.filter { it.amount < 0 }
        val incomes = txns.filter { it.amount > 0 }
        val totalExpense = expenses.sumOf { -it.amount }
        val totalIncome = incomes.sumOf { it.amount }

        val catMap = expenses.groupBy { it.categoryName }
        val catBreakdown = catMap.map { (name, list) ->
            val amt = list.sumOf { -it.amount }
            CategoryAnalysis(name, list.firstOrNull()?.categoryIcon ?: "💰", amt,
                if (totalExpense > 0) (amt / totalExpense) * 100 else 0.0, list.size, 0.0)
        }.sortedByDescending { it.amount }

        val (prevStart, prevEnd) = getPreviousMonthRange(year, month)
        val prevExp = dao.getByDateRangeSync(prevStart, prevEnd).filter { it.amount < 0 }.sumOf { -it.amount }
        val prevInc = dao.getByDateRangeSync(prevStart, prevEnd).filter { it.amount > 0 }.sumOf { it.amount }

        val budget = budgetDao.getBudgetForMonthSync(year, month)
        val budgetStatus = budget?.let {
            val budgetAmount = it.amount
            BudgetStatusInfo(budgetAmount, totalExpense, budgetAmount - totalExpense,
                if (budgetAmount > 0) (totalExpense / budgetAmount) * 100 else 0.0, totalExpense > budgetAmount)
        }

        val anomalies = mutableListOf<AnomalyItem>()
        catBreakdown.filter { it.amount > totalExpense * 0.3 && it.amount > 1000 }.forEach {
            anomalies.add(AnomalyItem(AnomalyType.UNUSUAL_HIGH_EXPENSE, it.categoryName, it.amount,
                "${it.categoryName}支出较高，占总支出${String.format("%.1f", it.percentage)}%", AnomalySeverity.WARNING))
        }
        budgetStatus?.let { if (it.isOverBudget) anomalies.add(AnomalyItem(AnomalyType.BUDGET_WARNING, null,
            it.spent - it.totalBudget, "本月超预算${String.format("%.0f", it.spent - it.totalBudget)}元", AnomalySeverity.CRITICAL)) }

        return MonthlyAnalysisData(year, month, totalExpense, totalIncome, txns.size, catBreakdown,
            totalExpense / getDaysInMonth(year, month), catBreakdown.firstOrNull()?.categoryName,
            catBreakdown.firstOrNull()?.amount ?: 0.0, budgetStatus,
            MonthOverMonthChange(
                if (prevExp > 0) ((totalExpense - prevExp) / prevExp) * 100 else 0.0,
                if (prevInc > 0) ((totalIncome - prevInc) / prevInc) * 100 else 0.0,
                totalExpense > prevExp, totalIncome > prevInc
            ), anomalies)
    }

    private fun generateAIAnalysis(data: MonthlyAnalysisData): AIAnalysisResult {
        val insights = mutableListOf<String>()
        val suggestions = mutableListOf<SuggestionItem>()
        val positives = mutableListOf<String>()

        data.monthOverMonthChange?.let { c ->
            if (c.isExpenseIncreased) insights.add("支出较上月增加${String.format("%.1f", c.expenseChangePercent)}%")
            else { insights.add("支出较上月减少${String.format("%.1f", -c.expenseChangePercent)}%"); positives.add("消费控制良好") }
        }
        data.topExpenseCategory?.let { insights.add("${it}是最大支出，占比${String.format("%.1f", data.categoryBreakdown.firstOrNull()?.percentage ?: 0.0)}%") }
        insights.add("日均支出${String.format("%.0f", data.dailyAverage)}元")

        data.budgetStatus?.let { s ->
            if (s.isOverBudget) suggestions.add(SuggestionItem(null, "控制超支",
                "已超预算${String.format("%.0f", s.spent - s.totalBudget)}元", true, s.spent - s.totalBudget))
        }
        data.categoryBreakdown.firstOrNull()?.let { c ->
            if (c.amount > 1000) suggestions.add(SuggestionItem(c.categoryName, "关注${c.categoryName}",
                "该分类支出较高，建议审视必要性", true, c.amount * 0.2))
        }

        return AIAnalysisResult("${data.year}年${data.month}月支出${String.format("%.0f", data.totalExpense)}元，收入${String.format("%.0f", data.totalIncome)}元",
            insights, suggestions, data.anomalies.map { it.description }, positives)
    }

    /**
     * 智能查询 - 纯规则引擎
     */
    fun processQuery(query: String) {
        viewModelScope.launch {
            _chatMessages.value += ChatMessage("user", query, System.currentTimeMillis())
            _isLoading.value = true
            
            try {
                // 规则引擎识别
                val result = enhancedRecognizer.recognize(query)
                val intent = result.intent
                
                // 执行查询
                val answer = queryExecutor.execute(intent)
                
                _chatMessages.value += ChatMessage("assistant", answer, System.currentTimeMillis())
                
            } catch (e: Exception) {
                e.printStackTrace()
                _chatMessages.value += ChatMessage("assistant", "抱歉，查询出错了，请重试", System.currentTimeMillis())
            } finally { 
                _isLoading.value = false 
            }
        }
    }

    /**
     * 快速查询 - 用于常见问题的快速响应
     */
    fun quickQuery(type: QuickQueryType) {
        val query = when (type) {
            QuickQueryType.TODAY_EXPENSE -> "今天花了多少"
            QuickQueryType.THIS_MONTH -> "本月花了多少"
            QuickQueryType.BUDGET_STATUS -> "预算还剩多少"
            QuickQueryType.TOP_CATEGORY -> "支出最多的是什么"
            QuickQueryType.COMPARE_LAST_MONTH -> "和上月比怎么样"
            QuickQueryType.AVERAGE_DAILY -> "平均每天花多少"
        }
        processQuery(query)
    }

    fun clearChat() { _chatMessages.value = emptyList() }

    override fun onCleared() {
        super.onCleared()
        // 无需释放资源
    }

    private fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    private fun getPreviousMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, -1)
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    private fun getDaysInMonth(year: Int, month: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}

data class ChatMessage(val role: String, val content: String, val timestamp: Long)

enum class QuickQueryType {
    TODAY_EXPENSE,
    THIS_MONTH,
    BUDGET_STATUS,
    TOP_CATEGORY,
    COMPARE_LAST_MONTH,
    AVERAGE_DAILY
}

/**
 * 模型状态
 */
sealed class ModelStatus {
    object LOADING : ModelStatus()
    data class READY(val engineName: String) : ModelStatus()
    data class FALLBACK(val engineName: String) : ModelStatus()
}
