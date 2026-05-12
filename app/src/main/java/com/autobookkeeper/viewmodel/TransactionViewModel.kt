package com.autobookkeeper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autobookkeeper.App
import com.autobookkeeper.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ─── 报表周期类型 ───
enum class ReportPeriod {
    WEEK,   // 本周
    MONTH,  // 本月
    CUSTOM, // 自定义
    YEAR    // 年度
}

// ─── 日均消费统计 ───
data class DailyAverageStats(
    val avgDailyExpense: Double = 0.0,
    val avgDailyIncome: Double = 0.0,
    val daysWithData: Int = 0,
    val totalDays: Int = 0
)

// ─── 日记账点（折线图用）───
data class DailyPoint(
    val date: Long,        // 该日 00:00:00
    val income: Double,
    val expense: Double
)

// ─── 年度汇总 ───
data class YearSummary(
    val year: Int,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,          // 结余
    val monthlyStats: List<MonthlyStat> = emptyList(),
    val topCategories: List<CategoryStat> = emptyList(),
    val transactionCount: Int = 0,
    val dailyAvg: DailyAverageStats = DailyAverageStats()
)

data class MonthlyStat(
    val month: Int,        // 1~12
    val income: Double,
    val expense: Double
)

// ─── 报表周期汇总 ───
data class PeriodSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val categoryBreakdown: List<CategoryStat> = emptyList(),
    val transactionCount: Int = 0,
    val incomeTransactionCount: Int = 0,  // 收入笔数
    val expenseTransactionCount: Int = 0, // 支出笔数
    val periodLabel: String = "",
    val startDate: Long = 0L,
    val endDate: Long = 0L,
    val dailyPoints: List<DailyPoint> = emptyList(),
    val dailyAvg: DailyAverageStats = DailyAverageStats()
)

// ─── 月度汇总（主页用）───
data class MonthlySummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val categoryBreakdown: List<CategoryStat> = emptyList(),
    val transactionCount: Int = 0,
    val dailyAvg: DailyAverageStats = DailyAverageStats()
)

// ─── 收入分布统计 ───
data class IncomeDistribution(
    val categoryBreakdown: List<CategoryStat> = emptyList(),
    val totalIncome: Double = 0.0,
    val transactionCount: Int = 0
)

// ─── 对比周期类型 ───
enum class ComparePeriodType {
    WEEK, MONTH, QUARTER, YEAR
}

// ─── 对比模式 ───
enum class CompareMode {
    SEQUENTIAL, YOY
}

// ─── 对比数据点 ───
data class CompareDataPoint(
    val label: String,       // 如 "第18周", "5月"
    val income: Double,
    val expense: Double,
    val transactionCount: Int = 0,
    val dateRange: String = ""  // 如 "05/01-05/07"
)

// ─── 对比结果 ───
data class ComparisonResult(
    val periodType: ComparePeriodType,
    val compareMode: CompareMode = CompareMode.SEQUENTIAL,
    val currentLabel: String,
    val previousLabel: String,
    val currentData: CompareDataPoint,
    val previousData: CompareDataPoint,
    val incomeChangeRate: Double,    // 收入变化率
    val expenseChangeRate: Double,   // 支出变化率
    val currentPeriods: List<CompareDataPoint> = emptyList(),
    val previousPeriods: List<CompareDataPoint> = emptyList()
)

// ─── 收支类型（报表用）───
enum class ReportType {
    INCOME, EXPENSE
}

// ─── 搜索结果统计 ───
data class SearchStats(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val incomeCount: Int = 0,
    val expenseCount: Int = 0
)

// ─── 搜索结果 ───
data class SearchableTransaction(
    val id: Long,
    val amount: Double,
    val categoryName: String,
    val categoryIcon: String,
    val source: Source,
    val note: String,
    val date: Long,
    val matchedField: String  // "note" / "category" / "amount"
)

private data class ReportParams(
    val period: ReportPeriod,
    val month: String,
    val start: Long,
    val end: Long,
    val year: Int
)

/**
 * 首页周期模式：整月 / 自定义范围
 */
enum class HomePeriodMode {
    MONTH,       // 按整月
    CUSTOM       // 按自定义起止日期
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as App).database
    private val transactionDao = db.transactionDao()
    private val categoryDao = db.categoryDao()
    
    // 公开 DAO 供重复处理等功能使用
    val dao: TransactionDao get() = transactionDao

    private val _currentMonth = MutableStateFlow(currentYearMonth())
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    // ─── 首页周期模式（整月 vs 自定义起止日） ───
    private val _homePeriodMode = MutableStateFlow(HomePeriodMode.MONTH)
    val homePeriodMode: StateFlow<HomePeriodMode> = _homePeriodMode.asStateFlow()

    // 自定义起止日（首页用，选择日期后自动设为当天所在周）
    private val _homeStartDate = MutableStateFlow(0L)
    val homeStartDate: StateFlow<Long> = _homeStartDate.asStateFlow()

    private val _homeEndDate = MutableStateFlow(0L)
    val homeEndDate: StateFlow<Long> = _homeEndDate.asStateFlow()
    
    // ─── 强制刷新触发器 ───
    private val _refreshTrigger = MutableStateFlow(0)
    fun forceRefresh() { refresh() }
    
    fun setHomeDateRange(start: Long, end: Long) {
        _homePeriodMode.value = HomePeriodMode.CUSTOM
        _homeStartDate.value = start
        _homeEndDate.value = end
    }
    fun clearHomePeriod() {
        _homePeriodMode.value = HomePeriodMode.MONTH
        _homeStartDate.value = 0L
        _homeEndDate.value = 0L
        _homePeriodLabel.value = ""
        _currentMonth.value = currentYearMonth()
    }

    // 首页周期标签（如 "5月1日-5月7日" 或 "2026年5月"）
    private val _homePeriodLabel = MutableStateFlow("")
    val homePeriodLabel: StateFlow<String> = _homePeriodLabel.asStateFlow()

    // ─── 首页 transactions — 根据 (mode + month 或 start/end) 驱动 ───
        // 首页交易列表 — 使用 Room Flow 实时监听，数据库变化自动推送
    val transactions: StateFlow<List<Transaction>> = combine(
        _homePeriodMode, _currentMonth, _homeStartDate, _homeEndDate
    ) { mode, month, start, end ->
        val queryStart: Long
        val queryEnd: Long
        if (mode == HomePeriodMode.CUSTOM && start > 0 && end > 0) {
            queryStart = normalizeStart(start)
            queryEnd = normalizeEnd(end)
        } else {
            val (s, e) = monthToRange(month)
            queryStart = s
            queryEnd = e
        }
        transactionDao.getByDateRange(queryStart, queryEnd)
    }.flatMapLatest { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseCategories: StateFlow<List<CategoryData>> = categoryDao.getByType("expense")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomeCategories: StateFlow<List<CategoryData>> = categoryDao.getByType("income")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ─── 所有分类（用于实时查询图标）───
    val allCategories: StateFlow<List<CategoryData>> = categoryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 实时分类图标映射（分类名 → 最新图标）
    val categoryIconMap: StateFlow<Map<String, String>> = allCategories
        .map { list -> list.associate { it.name to it.icon } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // summary 从 transactions Flow 自动计算——列表变化时统计同步刷新
    val summary: StateFlow<MonthlySummary> = transactions
        .map { computeSummaryFromTransactions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlySummary())

    // 保留 _summary 用于搜索统计的后备（searchSummary 仍依赖它）
    private val _fallbackSummary = MutableStateFlow(MonthlySummary())

    // ─── 搜索状态 ───
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Transaction>>(emptyList())
    val searchResults: StateFlow<List<Transaction>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // ─── 搜索日期筛选 ───
    private val _searchStartDate = MutableStateFlow<Long?>(null)
    val searchStartDate: StateFlow<Long?> = _searchStartDate.asStateFlow()

    private val _searchEndDate = MutableStateFlow<Long?>(null)
    val searchEndDate: StateFlow<Long?> = _searchEndDate.asStateFlow()

    // ─── 搜索结果统计 ───
    val searchStats: StateFlow<SearchStats> = searchResults
        .map { results ->
            val incomeList = results.filter { it.amount > 0 }
            val expenseList = results.filter { it.amount < 0 }
            SearchStats(
                totalIncome = incomeList.sumOf { it.amount },
                totalExpense = kotlin.math.abs(expenseList.sumOf { it.amount }),
                incomeCount = incomeList.size,
                expenseCount = expenseList.size
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchStats())

    // ─── 搜索汇总（搜索时显示搜索结果的数据，否则显示周期汇总）───
    val searchSummary: StateFlow<MonthlySummary> = combine(
        _searchQuery, _searchResults, summary
    ) { query, searchResults, periodSummary ->
        if (query.isBlank()) {
            periodSummary
        } else {
            computeSummaryFromTransactions(searchResults)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlySummary())

    // ─── 批量选择 ───
    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _isManageMode = MutableStateFlow(false)
    val isManageMode: StateFlow<Boolean> = _isManageMode.asStateFlow()

    fun enterManageMode() { _isManageMode.value = true }
    fun exitManageMode() { _isManageMode.value = false; _selectedIds.value = emptySet() }

    // ─── 报表周期相关 ───
    private val _reportPeriod = MutableStateFlow(ReportPeriod.MONTH)
    val reportPeriod: StateFlow<ReportPeriod> = _reportPeriod.asStateFlow()

    private val _customStartDate = MutableStateFlow(getMonthStart())
    val customStartDate: StateFlow<Long> = _customStartDate.asStateFlow()

    private val _customEndDate = MutableStateFlow(getMonthEnd())
    val customEndDate: StateFlow<Long> = _customEndDate.asStateFlow()

    private val _currentYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val currentYear: StateFlow<Int> = _currentYear.asStateFlow()

    private val _periodSummary = MutableStateFlow(PeriodSummary())
    val periodSummary: StateFlow<PeriodSummary> = _periodSummary.asStateFlow()

    // ─── 年度汇总（年度视图用）───
    private val _yearSummary = MutableStateFlow<YearSummary?>(null)
    val yearSummary: StateFlow<YearSummary?> = _yearSummary.asStateFlow()

    // ─── 收入分布（报表用）───
    private val _incomeDistribution = MutableStateFlow(IncomeDistribution())
    val incomeDistribution: StateFlow<IncomeDistribution> = _incomeDistribution.asStateFlow()

    // ─── 对比结果（报表用）───
    private val _comparisonResult = MutableStateFlow<ComparisonResult?>(null)
    val comparisonResult: StateFlow<ComparisonResult?> = _comparisonResult.asStateFlow()

    // ─── 报表页面选中的分类（用于下钻）───
    private val _selectedReportCategory = MutableStateFlow<String?>(null)
    val selectedReportCategory: StateFlow<String?> = _selectedReportCategory.asStateFlow()

    // ─── 报表收支切换 ───
    private val _reportType = MutableStateFlow(ReportType.EXPENSE)
    val reportType: StateFlow<ReportType> = _reportType.asStateFlow()

    fun setReportType(type: ReportType) {
        _reportType.value = type
        loadReportCategoryDistribution()
    }

    fun loadReportCategoryDistribution() {
        viewModelScope.launch {
            val ps = _periodSummary.value
            val start = ps.startDate
            val end = ps.endDate
            if (start > 0 && end > 0) {
                val (ns, ne) = normalizeDateRange(start, end)
                val stats = if (_reportType.value == ReportType.EXPENSE)
                    transactionDao.getCategoryStats(ns, ne)
                else
                    transactionDao.getIncomeCategoryStats(ns, ne)
                val total = if (_reportType.value == ReportType.EXPENSE)
                    kotlin.math.abs(transactionDao.getMonthlyExpense(ns, ne))
                else
                    transactionDao.getMonthlyIncome(ns, ne)
                val count = transactionDao.getTransactionCount(ns, ne)
                _incomeDistribution.value = IncomeDistribution(stats, total, count)
            }
        }
    }

    init {
        // 首页 summary 响应周期变化
        viewModelScope.launch {
            combine(
                _homePeriodMode, _currentMonth, _homeStartDate, _homeEndDate, _refreshTrigger
            ) { mode, month, start, end, _ -> HomePeriodParams(mode, month, start, end) }
                .collect { params -> updateHomeSummary(params) }
        }

        viewModelScope.launch {
            combine(
                _reportPeriod, _currentMonth, _customStartDate, _customEndDate, _currentYear, _refreshTrigger
            ) { array: Array<Any?> ->
                ReportParams(
                    array[0] as ReportPeriod,
                    array[1] as String,
                    array[2] as Long,
                    array[3] as Long,
                    array[4] as Int
                )
            }.collect { params ->
                updatePeriodSummary(params.period, params.month, params.start, params.end, params.year)
            }
        }

        // 搜索防抖
        viewModelScope.launch {
            _searchQuery.debounce(300).collect { query ->
                if (query.isBlank()) {
                    _searchResults.value = emptyList()
                    _isSearching.value = false
                } else {
                    _isSearching.value = true
                    _searchResults.value = performSearch(query)
                    _isSearching.value = false
                }
            }
        }

        // 监听交易数据变化，自动刷新报表
        viewModelScope.launch {
            transactions.collect { _ ->
                // 当交易数据变化时，刷新当前报表数据
                refreshReportData()
            }
        }
    }

    /**
     * 刷新报表数据（用于交易变化后的自动刷新）
     */
    private fun refreshReportData() {
        viewModelScope.launch {
            val params = ReportParams(
                _reportPeriod.value,
                _currentMonth.value,
                _customStartDate.value,
                _customEndDate.value,
                _currentYear.value
            )
            updatePeriodSummary(params.period, params.month, params.start, params.end, params.year)
            // 同时刷新收入分布
            loadReportCategoryDistribution()
            // 刷新年度汇总
            loadYearSummary(_currentYear.value)
            // 刷新对比数据
            triggerReportCompare(CompareMode.SEQUENTIAL)
        }
    }

    private data class HomePeriodParams(
        val mode: HomePeriodMode,
        val month: String,
        val start: Long,
        val end: Long
    )

    private suspend fun updateHomeSummary(params: HomePeriodParams) {
        val (start, end) = if (params.mode == HomePeriodMode.CUSTOM && params.start > 0 && params.end > 0) {
            params.start to params.end
        } else {
            monthToRange(params.month)
        }
        val (normalizedStart, _) = normalizeDateRange(start, end)
        // 更新标签
        if (params.mode == HomePeriodMode.CUSTOM && params.start > 0 && params.end > 0) {
            _homePeriodLabel.value = formatHomeDateRange(params.start, params.end)
        } else {
            val cal = Calendar.getInstance().apply { timeInMillis = normalizedStart }
            _homePeriodLabel.value = "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
        }
    }

    private fun computeSummaryFromTransactions(transactions: List<Transaction>): MonthlySummary {
        val income = transactions.filter { it.amount > 0 }.sumOf { it.amount }
        val expense = kotlin.math.abs(transactions.filter { it.amount < 0 }.sumOf { it.amount })
        val count = transactions.size

        // 分类统计
        val categoryStats = transactions
            .groupBy { it.categoryName }
            .map { (name, txns) ->
                val catTotal = kotlin.math.abs(txns.sumOf { it.amount })
                val catIcon = txns.firstOrNull()?.categoryIcon ?: ""
                CategoryStat(catIcon, name, catTotal)
            }
            .sortedByDescending { it.total }

        // 日均消费（按搜索结果中的实际天数计算）
        val dailyAvg = if (transactions.isEmpty()) {
            DailyAverageStats()
        } else {
            val dates = transactions.map { normalizeStart(it.date) }.distinct()
            val daysWithData = dates.size
            val firstDate = dates.minOrNull() ?: 0L
            val lastDate = dates.maxOrNull() ?: 0L
            val totalDays = if (firstDate > 0 && lastDate >= firstDate) {
                ((lastDate - firstDate) / (24 * 60 * 60 * 1000)).toInt() + 1
            } else 1
            DailyAverageStats(
                avgDailyExpense = if (totalDays > 0) expense / totalDays else 0.0,
                avgDailyIncome = if (totalDays > 0) income / totalDays else 0.0,
                daysWithData = daysWithData,
                totalDays = totalDays
            )
        }

        return MonthlySummary(income, expense, categoryStats, count, dailyAvg)
    }

    private suspend fun performSearch(query: String): List<Transaction> {
        val allTxns = transactionDao.getAll().first()
        val lower = query.lowercase()
        val start = _searchStartDate.value
        val end = _searchEndDate.value

        return allTxns.filter { txn ->
            // 日期筛选
            val dateMatch = if (start != null && end != null) {
                txn.date in start..end
            } else true

            // 关键词匹配
            val keywordMatch = txn.note.lowercase().contains(lower) ||
                    txn.categoryName.lowercase().contains(lower) ||
                    "%.2f".format(kotlin.math.abs(txn.amount)).contains(query) ||
                    txn.source.label.contains(query)

            dateMatch && keywordMatch
        }
    }

    // ─── 搜索日期筛选 ───
    fun setSearchDateRange(start: Long?, end: Long?) {
        _searchStartDate.value = start
        _searchEndDate.value = end
        // 重新触发搜索
        viewModelScope.launch {
            val query = _searchQuery.value
            if (query.isNotBlank()) {
                _isSearching.value = true
                _searchResults.value = performSearch(query)
                _isSearching.value = false
            }
        }
    }

    fun clearSearchDateRange() {
        _searchStartDate.value = null
        _searchEndDate.value = null
        viewModelScope.launch {
            val query = _searchQuery.value
            if (query.isNotBlank()) {
                _isSearching.value = true
                _searchResults.value = performSearch(query)
                _isSearching.value = false
            }
        }
    }

    // ─── 首页周期导航 ───

    /** 导航：上一个周期 */
    fun homePrevPeriod() {
        if (_homePeriodMode.value == HomePeriodMode.CUSTOM && _homeStartDate.value > 0) {
            val range = _homeEndDate.value - _homeStartDate.value
            _homeEndDate.value = normalizeEnd(_homeStartDate.value - 1)
            _homeStartDate.value = normalizeStart(_homeStartDate.value - range - 1)
        } else {
            _currentMonth.value = shiftMonth(_currentMonth.value, -1)
        }
    }

    /** 导航：下一个周期 */
    fun homeNextPeriod() {
        if (_homePeriodMode.value == HomePeriodMode.CUSTOM && _homeEndDate.value > 0) {
            val range = _homeEndDate.value - _homeStartDate.value
            _homeStartDate.value = normalizeStart(_homeEndDate.value + 1)
            _homeEndDate.value = normalizeEnd(_homeEndDate.value + range + 1)
        } else {
            _currentMonth.value = shiftMonth(_currentMonth.value, 1)
        }
    }

    /** 选择自定义起止日期 */
    

    /** 切换整月导航到指定月份 */
    fun switchHomeMonth(yearMonth: String) {
        clearHomePeriod()
        _currentMonth.value = yearMonth
    }

    // ─── 报表周期 ───
    fun setReportPeriod(period: ReportPeriod) {
        _reportPeriod.value = period
        when (period) {
            ReportPeriod.YEAR -> {
                _currentYear.value = Calendar.getInstance().get(Calendar.YEAR)
                loadYearSummary(_currentYear.value)
            }
            ReportPeriod.WEEK -> {
                val (s, e) = getCurrentWeekRange()
                _customStartDate.value = s
                _customEndDate.value = e
            }
            else -> {}
        }
        triggerReportCompare(CompareMode.SEQUENTIAL)
        loadReportCategoryDistribution()
    }

    fun setCustomDateRange(start: Long, end: Long) {
        _customStartDate.value = normalizeStart(start)
        _customEndDate.value = normalizeEnd(end)
        _reportPeriod.value = ReportPeriod.CUSTOM
        triggerReportCompare(CompareMode.SEQUENTIAL)
    }

    fun prevPeriod() {
        when (_reportPeriod.value) {
            ReportPeriod.WEEK -> {
                val cal = Calendar.getInstance().apply { timeInMillis = _customStartDate.value }
                cal.add(Calendar.DAY_OF_YEAR, -7)
                _customStartDate.value = normalizeStart(cal.timeInMillis)
                _customEndDate.value = normalizeEnd(cal.apply { add(Calendar.DAY_OF_YEAR, 6) }.timeInMillis)
            }
            ReportPeriod.MONTH -> {
                _currentMonth.value = shiftMonth(_currentMonth.value, -1)
            }
            ReportPeriod.YEAR -> {
                val newYear = _currentYear.value - 1
                _currentYear.value = newYear
                loadYearSummary(newYear)
            }
            ReportPeriod.CUSTOM -> {
                val range = _customEndDate.value - _customStartDate.value
                _customEndDate.value = _customStartDate.value - 1
                _customStartDate.value = _customStartDate.value - range - 1
            }
        }
        triggerReportCompare(CompareMode.SEQUENTIAL)
    }

    fun nextPeriod() {
        when (_reportPeriod.value) {
            ReportPeriod.WEEK -> {
                val cal = Calendar.getInstance().apply { timeInMillis = _customStartDate.value }
                cal.add(Calendar.DAY_OF_YEAR, 7)
                _customStartDate.value = normalizeStart(cal.timeInMillis)
                _customEndDate.value = normalizeEnd(cal.apply { add(Calendar.DAY_OF_YEAR, 6) }.timeInMillis)
            }
            ReportPeriod.MONTH -> {
                _currentMonth.value = shiftMonth(_currentMonth.value, 1)
            }
            ReportPeriod.YEAR -> {
                val newYear = _currentYear.value + 1
                _currentYear.value = newYear
                loadYearSummary(newYear)
            }
            ReportPeriod.CUSTOM -> {
                val range = _customEndDate.value - _customStartDate.value
                _customStartDate.value = _customEndDate.value + 1
                _customEndDate.value = _customEndDate.value + range + 1
            }
        }
        triggerReportCompare(CompareMode.SEQUENTIAL)
    }

    // ─── 首页 CRUD ───
    fun addTransaction(
        amount: Double,
        categoryName: String,
        categoryIcon: String,
        source: Source,
        note: String,
        date: Long
    ) {
        viewModelScope.launch {
            // 快速记账：用户已确认的交易不做自动去重，直接入库
            transactionDao.insert(
                Transaction(amount = amount, categoryName = categoryName,
                    categoryIcon = categoryIcon, source = source, note = note, date = date)
            )
            refresh()
        }
    }

    fun updateTransaction(txn: Transaction) {
        viewModelScope.launch {
            transactionDao.update(txn.copy(updatedAt = System.currentTimeMillis()))
            refresh()
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            transactionDao.deleteById(id)
            refresh()
        }
    }
    
    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionDao.delete(transaction)
            refresh()
        }
    }

    suspend fun getTransactionByIdSuspend(id: Long): Transaction? {
        return transactionDao.getById(id)
    }

    // ─── 分类 ───
    fun addCategory(name: String, icon: String, type: String, parentName: String?) {
        viewModelScope.launch {
            categoryDao.insert(CategoryData(name = name, icon = icon, type = type, parentName = parentName, isCustom = true))
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch { categoryDao.deleteById(id) }
    }

    fun updateCategory(cat: CategoryData) {
        viewModelScope.launch { categoryDao.update(cat) }
    }

    fun moveCategoryToPosition(category: CategoryData, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        viewModelScope.launch {
            val siblings = categoryDao.getCategoriesByType(category.type)
                .filter { it.parentName.isNullOrEmpty() && !it.isHidden }
                .sortedBy { it.sortOrder }
            if (siblings.isEmpty()) return@launch
            val actualFrom = siblings.indexOfFirst { it.id == category.id }
            if (actualFrom == -1) return@launch
            val actualTo = (actualFrom + (toIndex - fromIndex)).coerceIn(0, siblings.size - 1)
            if (actualFrom == actualTo) return@launch
            val reordered = siblings.toMutableList()
            reordered.removeAt(actualFrom)
            reordered.add(actualTo, category)
            reordered.forEachIndexed { index, cat ->
                val newOrder = (index + 1) * 10
                if (cat.sortOrder != newOrder) {
                    categoryDao.update(cat.copy(sortOrder = newOrder))
                }
            }
        }
    }

    fun swapCategoryOrder(fromCat: CategoryData, toCat: CategoryData) {
        viewModelScope.launch {
            val tempOrder = -1
            categoryDao.update(fromCat.copy(sortOrder = tempOrder))
            categoryDao.update(toCat.copy(sortOrder = fromCat.sortOrder))
            categoryDao.update(fromCat.copy(sortOrder = toCat.sortOrder))
        }
    }

    private fun refresh() {
        _refreshTrigger.value++
        // 全量刷新：所有报表数据重新加载（不仅当前周期）
        viewModelScope.launch {
            loadYearSummary(_currentYear.value)
            // 如果当前有对比结果，维持对比类型和模式重新计算
            val compareState = _comparisonResult.value
            if (compareState != null) {
                try {
                    val curType = compareState.periodType
                    val curMode = compareState.compareMode
                    val (currentStart, currentEnd) = getCurrentPeriodRangeForCompare(curType)
                    val (prevStart, prevEnd) = if (curMode == CompareMode.SEQUENTIAL) {
                        getPreviousPeriodRangeForCompare(curType, currentStart, currentEnd)
                    } else {
                        getYearOverYearRangeForCompare(curType, currentStart, currentEnd)
                    }
                    comparePeriods(curType, currentStart, currentEnd, prevStart, prevEnd, curMode)
                } catch (_: Exception) {
                    // 对比状态不一致时跳过
                }
            }
        }
    }

    // ─── 时间工具（首页用）───
    fun getMonthStart(): Long = normalizeStart(System.currentTimeMillis())
    fun getMonthEnd(): Long = normalizeEnd(System.currentTimeMillis())

    private fun formatHomeDateRange(start: Long, end: Long): String {
        val sdf = SimpleDateFormat("M月d日", Locale.getDefault())
        val calStart = Calendar.getInstance().apply { timeInMillis = start }
        val calEnd = Calendar.getInstance().apply { timeInMillis = end }
        return if (calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR) &&
            calStart.get(Calendar.MONTH) == calEnd.get(Calendar.MONTH)) {
            "${calStart.get(Calendar.MONTH)+1}月${calStart.get(Calendar.DAY_OF_MONTH)}日-${calEnd.get(Calendar.DAY_OF_MONTH)}日"
        } else if (calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR)) {
            "${calStart.get(Calendar.MONTH)+1}月${calStart.get(Calendar.DAY_OF_MONTH)}日-${calEnd.get(Calendar.MONTH)+1}月${calEnd.get(Calendar.DAY_OF_MONTH)}日"
        } else {
            "${sdf.format(Date(start))}-${sdf.format(Date(end))}"
        }
    }

    private suspend fun updatePeriodSummary(period: ReportPeriod, month: String, customStart: Long, customEnd: Long, year: Int) {
        val (start, end, label) = when (period) {
            ReportPeriod.WEEK -> {
                val calStart = Calendar.getInstance().apply { timeInMillis = customStart }
                val calEnd = Calendar.getInstance().apply { timeInMillis = customEnd }
                val weekLabel = if (calStart.get(Calendar.MONTH) == calEnd.get(Calendar.MONTH)) {
                    "${calStart.get(Calendar.MONTH)+1}月${calStart.get(Calendar.DAY_OF_MONTH)}日-${calEnd.get(Calendar.DAY_OF_MONTH)}日"
                } else {
                    "${calStart.get(Calendar.MONTH)+1}月${calStart.get(Calendar.DAY_OF_MONTH)}日-${calEnd.get(Calendar.MONTH)+1}月${calEnd.get(Calendar.DAY_OF_MONTH)}日"
                }
                Triple(customStart, customEnd, weekLabel)
            }
            ReportPeriod.MONTH -> {
                val (s, e) = monthToRange(month)
                val cal = Calendar.getInstance().apply { timeInMillis = s }
                Triple(s, e, "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH)+1}月")
            }
            ReportPeriod.CUSTOM -> {
                Triple(customStart, customEnd, formatDateRange(customStart, customEnd))
            }
            ReportPeriod.YEAR -> {
                val cal = Calendar.getInstance().apply { set(year, Calendar.JANUARY, 1, 0, 0, 0) }
                val s = cal.timeInMillis
                cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
                val e = cal.timeInMillis
                Triple(s, e, "${year}年度")
            }
        }

        val queryStart = normalizeStart(start)
        // MONTH/WEEK 不用 normalizeEnd：monthToRange 的 end 是下个月第1天，normalizeEnd会多包含一天
        val queryEnd = when (period) {
            ReportPeriod.MONTH -> normalizeEnd(end) - 86_400_000L  // end是下月第1天，减1天得本月最后一天23:59:59
            else -> normalizeEnd(end)
        }
        val income = transactionDao.getMonthlyIncome(queryStart, queryEnd)
        val expense = kotlin.math.abs(transactionDao.getMonthlyExpense(queryStart, queryEnd))
        val stats = transactionDao.getCategoryStats(queryStart, queryEnd)
        val count = transactionDao.getTransactionCount(queryStart, queryEnd)
        val incomeCount = transactionDao.getIncomeTransactionCount(queryStart, queryEnd)
        val expenseCount = transactionDao.getExpenseTransactionCount(queryStart, queryEnd)
        val dailyPoints = computeDailyPoints(queryStart, queryEnd)
        val dailyAvg = computeDailyAvg(queryStart, queryEnd)

        _periodSummary.value = PeriodSummary(
            income = income,
            expense = expense,
            categoryBreakdown = stats,
            transactionCount = count,
            incomeTransactionCount = incomeCount,
            expenseTransactionCount = expenseCount,
            periodLabel = label,
            startDate = queryStart,
            endDate = queryEnd,
            dailyPoints = dailyPoints,
            dailyAvg = dailyAvg
        )
        // 自动加载分类分布
        loadReportCategoryDistribution()
    }

    private suspend fun computeDailyAvg(start: Long, end: Long): DailyAverageStats {
        val dailyPoints = computeDailyPoints(start, end)
        val totalDays = ((end - start) / DAY_MS).toInt().coerceAtLeast(1)
        val totalExpense = dailyPoints.sumOf { it.expense }
        val totalIncome = dailyPoints.sumOf { it.income }
        val daysWithData = dailyPoints.count { it.expense > 0 || it.income > 0 }
        // 按总天数平均（不是按有数据的天数）
        val avgExpense = if (totalDays > 0) totalExpense / totalDays else 0.0
        val avgIncome = if (totalDays > 0) totalIncome / totalDays else 0.0
        return DailyAverageStats(avgExpense, avgIncome, daysWithData, totalDays)
    }

    private suspend fun computeDailyPoints(start: Long, end: Long): List<DailyPoint> {
        val points = mutableListOf<DailyPoint>()
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }
        while (!cal.after(endCal)) {
            val dayStart = normalizeStart(cal.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dayEnd = normalizeEnd(cal.timeInMillis - 1)
            val income = transactionDao.getMonthlyIncome(dayStart, dayEnd)
            val expense = kotlin.math.abs(transactionDao.getMonthlyExpense(dayStart, dayEnd))
            points.add(DailyPoint(dayStart, income, expense))
        }
        return points
    }

    fun loadYearSummary(year: Int) {
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply { set(year, Calendar.JANUARY, 1, 0, 0, 0) }
            val start = cal.timeInMillis
            cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
            val end = cal.timeInMillis
            val monthlyStats = (1..12).map { month ->
                val mc = Calendar.getInstance().apply { set(year, month - 1, 1, 0, 0, 0) }
                val ms = mc.timeInMillis
                mc.add(Calendar.MONTH, 1)
                val me = mc.timeInMillis
                val inc = transactionDao.getMonthlyIncome(ms, me)
                val exp = kotlin.math.abs(transactionDao.getMonthlyExpense(ms, me))
                MonthlyStat(month, inc, exp)
            }
            val income = transactionDao.getMonthlyIncome(start, end)
            val expense = kotlin.math.abs(transactionDao.getMonthlyExpense(start, end))
            val stats = transactionDao.getCategoryStats(start, end)
            val count = transactionDao.getTransactionCount(start, end)
            val dailyAvg = computeDailyAvg(start, end)
            _yearSummary.value = YearSummary(
                year = year,
                totalIncome = income,
                totalExpense = expense,
                netSavings = income - expense,
                monthlyStats = monthlyStats,
                topCategories = stats.take(5),
                transactionCount = count,
                dailyAvg = dailyAvg
            )
        }
    }

    // ─── 搜索 ───
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    // ─── 批量选择 ───
    fun toggleSelect(id: Long) {
        _selectedIds.update { current -> if (id in current) current - id else current + id }
    }
    fun selectAll(ids: List<Long>) { _selectedIds.value = ids.toSet() }
    fun clearSelection() { _selectedIds.value = emptySet() }

    fun exitManage() { _isManageMode.value = false; _selectedIds.value = emptySet() }
    fun deleteSelected() {
        viewModelScope.launch {
            transactionDao.deleteByIds(_selectedIds.value.toList())
            _selectedIds.value = emptySet()
            // 删除后不退出管理模式，只刷新页面
            refresh()
        }
    }
    fun batchEditCategory(categoryName: String, categoryIcon: String) {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            transactionDao.batchUpdateCategory(ids, categoryName, categoryIcon)
            refresh()
        }
    }

    fun batchEditDate(newDate: Long) {
        val ids = _selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            transactionDao.batchUpdateDate(ids, newDate)
            refresh()
        }
    }
    suspend fun getAllTransactionsForExport(): List<Transaction> {
        return transactionDao.getAll().first()
    }

    // ─── 收入分布 ───
    fun loadIncomeDistribution(start: Long, end: Long) {
        viewModelScope.launch {
            val (ns, ne) = normalizeDateRange(start, end)
            val stats = transactionDao.getIncomeCategoryStats(ns, ne)
            val total = transactionDao.getMonthlyIncome(ns, ne)
            val count = transactionDao.getTransactionCount(ns, ne)
            _incomeDistribution.value = IncomeDistribution(
                categoryBreakdown = stats,
                totalIncome = total,
                transactionCount = count
            )
        }
    }

    fun clearSelectedReportCategory() {
        _selectedReportCategory.value = null
    }

    fun selectReportCategory(categoryName: String) {
        _selectedReportCategory.value = categoryName
    }

    // ─── 数据对比 ───
    fun comparePeriods(periodType: ComparePeriodType, currentStartMs: Long, currentEndMs: Long, previousStartMs: Long, previousEndMs: Long, compareMode: CompareMode = CompareMode.SEQUENTIAL) {
        viewModelScope.launch {
            // 直接用范围查询，不再二次 normalizeDateRange（避免偏移）
            val currentIncome = transactionDao.getMonthlyIncome(currentStartMs, currentEndMs)
            val currentExpense = kotlin.math.abs(transactionDao.getMonthlyExpense(currentStartMs, currentEndMs))
            val currentCount = transactionDao.getTransactionCount(currentStartMs, currentEndMs)

            val previousIncome = transactionDao.getMonthlyIncome(previousStartMs, previousEndMs)
            val previousExpense = kotlin.math.abs(transactionDao.getMonthlyExpense(previousStartMs, previousEndMs))
            val previousCount = transactionDao.getTransactionCount(previousStartMs, previousEndMs)

            val incomeChangeRate = if (previousIncome > 0) (currentIncome - previousIncome) / previousIncome * 100 else 0.0
            val expenseChangeRate = if (previousExpense > 0) (currentExpense - previousExpense) / previousExpense * 100 else 0.0

            val currentLabel = formatCompareLabel(periodType, currentStartMs, currentEndMs)
            val previousLabel = formatCompareLabel(periodType, previousStartMs, previousEndMs)

            // 获取详细数据点（用于图表）
            val currentPeriods = getCompareDataPoints(periodType, currentStartMs, currentEndMs)
            val previousPeriods = getCompareDataPoints(periodType, previousStartMs, previousEndMs)

            // 计算当前和前期的日期范围
            val currentDateRange = when (periodType) {
                ComparePeriodType.WEEK -> {
                    val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
                    "${sdf.format(Date(currentStartMs))}-${sdf.format(Date(currentEndMs))}"
                }
                ComparePeriodType.MONTH -> {
                    val sdf = SimpleDateFormat("M月", Locale.getDefault())
                    sdf.format(Date(currentStartMs))
                }
                else -> currentLabel
            }
            val previousDateRange = when (periodType) {
                ComparePeriodType.WEEK -> {
                    val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
                    "${sdf.format(Date(previousStartMs))}-${sdf.format(Date(previousEndMs))}"
                }
                ComparePeriodType.MONTH -> {
                    val sdf = SimpleDateFormat("M月", Locale.getDefault())
                    sdf.format(Date(previousStartMs))
                }
                else -> previousLabel
            }

            _comparisonResult.value = ComparisonResult(
                periodType = periodType,
                compareMode = compareMode,
                currentLabel = currentLabel,
                previousLabel = previousLabel,
                currentData = CompareDataPoint(currentLabel, currentIncome, currentExpense, currentCount, currentDateRange),
                previousData = CompareDataPoint(previousLabel, previousIncome, previousExpense, previousCount, previousDateRange),
                incomeChangeRate = incomeChangeRate,
                expenseChangeRate = expenseChangeRate,
                currentPeriods = currentPeriods,
                previousPeriods = previousPeriods
            )
        }
    }

    /** 一键触发对比：根据 compareType 和 compareMode 自动计算区间并执行对比 */
    fun triggerCompare(type: ComparePeriodType, mode: CompareMode) {
        val (currentStart, currentEnd) = getCurrentPeriodRangeForCompare(type)
        val (prevStart, prevEnd) = if (mode == CompareMode.SEQUENTIAL) {
            getPreviousPeriodRangeForCompare(type, currentStart, currentEnd)
        } else {
            getYearOverYearRangeForCompare(type, currentStart, currentEnd)
        }
        comparePeriods(type, currentStart, currentEnd, prevStart, prevEnd, mode)
    }

    /** 根据报表当前选中的周期触发对比 */
    fun triggerReportCompare(mode: CompareMode = CompareMode.SEQUENTIAL) {
        viewModelScope.launch {
            val (currentStart, currentEnd) = getReportCurrentPeriodRange()
            val periodType = when (_reportPeriod.value) {
                ReportPeriod.WEEK -> ComparePeriodType.WEEK
                ReportPeriod.MONTH -> ComparePeriodType.MONTH
                ReportPeriod.YEAR -> ComparePeriodType.YEAR
                ReportPeriod.CUSTOM -> ComparePeriodType.MONTH // 自定义按30天月处理
            }

            val (prevStart, prevEnd) = if (mode == CompareMode.SEQUENTIAL) {
                getPreviousPeriodRangeForCompare(periodType, currentStart, currentEnd)
            } else {
                getYearOverYearRangeForCompare(periodType, currentStart, currentEnd)
            }

            comparePeriods(periodType, currentStart, currentEnd, prevStart, prevEnd, mode)
        }
    }

    /** 获取报表当前选中的周期范围 */
    private fun getReportCurrentPeriodRange(): Pair<Long, Long> {
        return when (_reportPeriod.value) {
            ReportPeriod.WEEK -> {
                _customStartDate.value to _customEndDate.value
            }
            ReportPeriod.MONTH -> {
                val cal = Calendar.getInstance()
                val parts = _currentMonth.value.split("-")
                cal.set(parts[0].toInt(), parts[1].toInt() - 1, 1)
                val start = normalizeStart(cal.timeInMillis)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
            ReportPeriod.YEAR -> {
                val cal = Calendar.getInstance()
                cal.set(_currentYear.value, Calendar.JANUARY, 1)
                val start = normalizeStart(cal.timeInMillis)
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
            ReportPeriod.CUSTOM -> {
                _customStartDate.value to _customEndDate.value
            }
        }
    }

    /** 获取对比的时间区间文本标签 */
    fun getComparePeriodLabel(type: ComparePeriodType, mode: CompareMode): String {
        val (currentStart, currentEnd) = getCurrentPeriodRangeForCompare(type)
        val (prevStart, prevEnd) = if (mode == CompareMode.SEQUENTIAL) {
            getPreviousPeriodRangeForCompare(type, currentStart, currentEnd)
        } else {
            getYearOverYearRangeForCompare(type, currentStart, currentEnd)
        }
        val curLabel = formatCompareLabel(type, currentStart, currentEnd)
        val prevLabel = formatCompareLabel(type, prevStart, prevEnd)
        val modeLabel = if (mode == CompareMode.SEQUENTIAL) "环比" else "同比"
        val periodLabel = when (type) {
            ComparePeriodType.WEEK -> "按周"
            ComparePeriodType.MONTH -> "按月"
            ComparePeriodType.QUARTER -> "按季"
            ComparePeriodType.YEAR -> "按年"
        }
        return "$periodLabel $modeLabel: $curLabel vs $prevLabel"
    }

    private fun formatCompareLabel(periodType: ComparePeriodType, start: Long, end: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        return when (periodType) {
            ComparePeriodType.WEEK -> {
                val week = cal.get(Calendar.WEEK_OF_YEAR)
                "第${week}周"
            }
            ComparePeriodType.MONTH -> {
                "${cal.get(Calendar.MONTH) + 1}月"
            }
            ComparePeriodType.QUARTER -> {
                val month = cal.get(Calendar.MONTH)
                val quarter = month / 3 + 1
                "Q${quarter}"
            }
            ComparePeriodType.YEAR -> {
                "${cal.get(Calendar.YEAR)}年"
            }
        }
    }

    private suspend fun getCompareDataPoints(periodType: ComparePeriodType, start: Long, end: Long): List<CompareDataPoint> {
        val points = mutableListOf<CompareDataPoint>()
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())

        when (periodType) {
            ComparePeriodType.WEEK -> {
                // 按天展示本周7天，包含日期范围
                while (!cal.after(endCal)) {
                    val dayStart = normalizeStart(cal.timeInMillis)
                    val dayLabel = sdf.format(Date(dayStart))
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    val dayEnd = normalizeEnd(cal.timeInMillis - 1)
                    val income = transactionDao.getMonthlyIncome(dayStart, dayEnd)
                    val expense = kotlin.math.abs(transactionDao.getMonthlyExpense(dayStart, dayEnd))
                    points.add(CompareDataPoint(dayLabel, income, expense, dateRange = dayLabel))
                }
            }
            ComparePeriodType.MONTH -> {
                // 按周展示，每周显示日期范围
                val weekSdf = SimpleDateFormat("MM-dd", Locale.getDefault())
                var weekNum = 1
                val tempCal = Calendar.getInstance().apply { timeInMillis = start }
                while (!tempCal.after(endCal)) {
                    val weekStart = normalizeStart(tempCal.timeInMillis)
                    tempCal.add(Calendar.DAY_OF_YEAR, 6)
                    val weekEnd = if (tempCal.after(endCal)) end else normalizeEnd(tempCal.timeInMillis)
                    val income = transactionDao.getMonthlyIncome(weekStart, weekEnd)
                    val expense = kotlin.math.abs(transactionDao.getMonthlyExpense(weekStart, weekEnd))
                    val dateRange = "${weekSdf.format(Date(weekStart))}-${weekSdf.format(Date(weekEnd))}"
                    points.add(CompareDataPoint("第${weekNum}周", income, expense, dateRange = dateRange))
                    weekNum++
                    tempCal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            ComparePeriodType.QUARTER -> {
                // 按月展示
                val monthSdf = SimpleDateFormat("M月", Locale.getDefault())
                val tempCal = Calendar.getInstance().apply { timeInMillis = start }
                while (!tempCal.after(endCal)) {
                    val monthStart = normalizeStart(tempCal.timeInMillis)
                    tempCal.add(Calendar.MONTH, 1)
                    val monthEnd = if (tempCal.after(endCal)) end else normalizeEnd(tempCal.timeInMillis - 1)
                    val income = transactionDao.getMonthlyIncome(monthStart, monthEnd)
                    val expense = kotlin.math.abs(transactionDao.getMonthlyExpense(monthStart, monthEnd))
                    val label = monthSdf.format(Date(monthStart))
                    points.add(CompareDataPoint(label, income, expense, dateRange = label))
                    if (tempCal.after(endCal)) break
                }
            }
            ComparePeriodType.YEAR -> {
                // 按季度展示
                val quarterSdf = SimpleDateFormat("M月", Locale.getDefault())
                var quarterNum = 1
                val tempCal = Calendar.getInstance().apply { timeInMillis = start }
                while (!tempCal.after(endCal)) {
                    val qStart = normalizeStart(tempCal.timeInMillis)
                    tempCal.add(Calendar.MONTH, 3)
                    val qEnd = if (tempCal.after(endCal)) end else normalizeEnd(tempCal.timeInMillis - 1)
                    val income = transactionDao.getMonthlyIncome(qStart, qEnd)
                    val expense = kotlin.math.abs(transactionDao.getMonthlyExpense(qStart, qEnd))
                    val dateRange = "${quarterSdf.format(Date(qStart))}-${quarterSdf.format(Date(qEnd))}"
                    points.add(CompareDataPoint("Q${quarterNum}", income, expense, dateRange = dateRange))
                    quarterNum++
                    if (tempCal.after(endCal)) break
                }
            }
        }
        return points
    }

    fun clearComparison() {
        _comparisonResult.value = null
    }

    // ─── 对比工具：获取当前/上期日期范围（Calendar 精确计算）───
    fun getCurrentPeriodRangeForCompare(type: ComparePeriodType): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (type) {
            ComparePeriodType.WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val start = normalizeStart(cal.timeInMillis)
                cal.add(Calendar.DAY_OF_YEAR, 6)
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
            ComparePeriodType.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = normalizeStart(cal.timeInMillis)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
            ComparePeriodType.QUARTER -> {
                val month = cal.get(Calendar.MONTH)
                val qStart = month / 3 * 3
                cal.set(Calendar.MONTH, qStart)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = normalizeStart(cal.timeInMillis)
                cal.set(Calendar.MONTH, qStart + 2)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
            ComparePeriodType.YEAR -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                val start = normalizeStart(cal.timeInMillis)
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
        }
    }

    /** 获取上一周期（环比用）：用 Calendar 精确偏移 */
    fun getPreviousPeriodRangeForCompare(type: ComparePeriodType, currentStart: Long, currentEnd: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (type) {
            ComparePeriodType.WEEK -> {
                cal.timeInMillis = currentStart
                cal.add(Calendar.WEEK_OF_YEAR, -1)
                val start = normalizeStart(cal.timeInMillis)
                cal.timeInMillis = start
                cal.add(Calendar.DAY_OF_YEAR, 6)
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
            ComparePeriodType.MONTH -> {
                cal.timeInMillis = currentStart
                cal.add(Calendar.MONTH, -1)
                val start = normalizeStart(cal.timeInMillis)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
            ComparePeriodType.QUARTER -> {
                cal.timeInMillis = currentStart
                cal.add(Calendar.MONTH, -3)
                val start = normalizeStart(cal.timeInMillis)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
            ComparePeriodType.YEAR -> {
                cal.timeInMillis = currentStart
                cal.add(Calendar.YEAR, -1)
                val start = normalizeStart(cal.timeInMillis)
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
        }
    }

    /** 获取去年同期（同比用）：用 Calendar 精确偏移 */
    fun getYearOverYearRangeForCompare(type: ComparePeriodType, currentStart: Long, currentEnd: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        return when (type) {
            ComparePeriodType.WEEK -> {
                cal.timeInMillis = currentStart
                cal.add(Calendar.WEEK_OF_YEAR, -52)
                val start = normalizeStart(cal.timeInMillis)
                cal.timeInMillis = start
                cal.add(Calendar.DAY_OF_YEAR, 6)
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
            ComparePeriodType.MONTH -> {
                cal.timeInMillis = currentStart
                cal.add(Calendar.YEAR, -1)
                val start = normalizeStart(cal.timeInMillis)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
            ComparePeriodType.QUARTER -> {
                cal.timeInMillis = currentStart
                cal.add(Calendar.YEAR, -1)
                val start = normalizeStart(cal.timeInMillis)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
            ComparePeriodType.YEAR -> {
                cal.timeInMillis = currentStart
                cal.add(Calendar.YEAR, -1)
                val start = normalizeStart(cal.timeInMillis)
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                val end = normalizeEnd(cal.timeInMillis)
                start to end
            }
        }
    }

    // ─── 报表周期 ───
    // (已在前面上方实现)

    // ─── 时间工具 ───
    private fun normalizeStart(ts: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun normalizeEnd(ts: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    private fun normalizeDateRange(start: Long, end: Long): Pair<Long, Long> {
        return normalizeStart(start) to normalizeEnd(end)
    }

    private fun getCurrentWeekRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        val start = normalizeStart(cal.timeInMillis)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val end = normalizeEnd(cal.timeInMillis)
        return start to end
    }

    private fun formatDateRange(start: Long, end: Long): String {
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        return "${sdf.format(Date(start))} ~ ${sdf.format(Date(end))}"
    }

    private val DAY_MS = 86_400_000L

    private fun currentYearMonth(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${"%02d".format(cal.get(Calendar.MONTH) + 1)}"
    }

    private fun monthToRange(yearMonth: String): Pair<Long, Long> {
        val parts = yearMonth.split("-")
        val cal = Calendar.getInstance().apply { set(parts[0].toInt(), parts[1].toInt() - 1, 1, 0, 0, 0) }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to cal.timeInMillis
    }

    private fun shiftMonth(yearMonth: String, delta: Int): String {
        val parts = yearMonth.split("-")
        val cal = Calendar.getInstance().apply { set(parts[0].toInt(), parts[1].toInt() - 1, 1) }
        cal.add(Calendar.MONTH, delta)
        return "${cal.get(Calendar.YEAR)}-${"%02d".format(cal.get(Calendar.MONTH) + 1)}"
    }

    // ─── 语音记账功能 ───
    private val _voiceRecognitionResult = MutableStateFlow<String?>(null)
    val voiceRecognitionResult: StateFlow<String?> = _voiceRecognitionResult.asStateFlow()
    
    private val _voiceRecognitionError = MutableStateFlow<String?>(null)
    val voiceRecognitionError: StateFlow<String?> = _voiceRecognitionError.asStateFlow()

    fun onVoiceRecognitionResult(result: String) {
        _voiceRecognitionResult.value = result
    }
    
    fun onVoiceRecognitionError(error: String) {
        _voiceRecognitionError.value = error
    }
    
    fun clearVoiceRecognitionError() {
        _voiceRecognitionError.value = null
    }

    fun saveVoiceTransaction(amount: Double, categoryName: String, merchantRaw: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val category = allCategories.value.find { it.name == categoryName } 
                ?: allCategories.value.find { it.name == "其他" }
                ?: allCategories.value.first()
            
            val transaction = Transaction(
                amount = if (amount > 0) -amount else amount,
                categoryName = category.name,
                categoryIcon = category.icon,
                merchantRaw = merchantRaw,
                source = Source.MANUAL,
                date = now
            )
            dao.insert(transaction)
            _voiceRecognitionResult.value = null
        }
    }
}
