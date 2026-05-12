package com.autobookkeeper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autobookkeeper.App
import com.autobookkeeper.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val budgetDao: BudgetDao = (application as App).budgetDao
    private val transactionDao: TransactionDao = (application as App).transactionDao
    private val categoryDao: com.autobookkeeper.data.CategoryDao = (application as App).categoryDao
    private val manualDebtDao: ManualDebtDao = (application as App).manualDebtDao

    private val _currentMonth = MutableStateFlow(currentYearMonth())
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    val budgetsOfMonth: StateFlow<List<Budget>> = _currentMonth
        .flatMapLatest { month -> budgetDao.getByMonth(month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<com.autobookkeeper.data.CategoryData>> = categoryDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val manualDebts: StateFlow<List<ManualDebt>> = manualDebtDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _budgetStatuses = MutableStateFlow<List<BudgetStatus>>(emptyList())
    val budgetStatuses: StateFlow<List<BudgetStatus>> = _budgetStatuses.asStateFlow()

    private val _totalBudgetStatus = MutableStateFlow<BudgetStatus?>(null)
    val totalBudgetStatus: StateFlow<BudgetStatus?> = _totalBudgetStatus.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)

    private val _showEditDialog = MutableStateFlow(false)
    val showEditDialog: StateFlow<Boolean> = _showEditDialog.asStateFlow()

    private val _editingBudget = MutableStateFlow<Budget?>(null)
    val editingBudget: StateFlow<Budget?> = _editingBudget.asStateFlow()

    // 负债编辑弹窗状态
    private val _debtDialogState = MutableStateFlow<ManualDebt?>(null)
    val debtDialogState: StateFlow<ManualDebt?> = _debtDialogState.asStateFlow()

    private val _showDebtDialog = MutableStateFlow(false)
    val showDebtDialog: StateFlow<Boolean> = _showDebtDialog.asStateFlow()

    init {
        viewModelScope.launch {
            combine(budgetsOfMonth, _refreshTrigger) { budgets, _ -> budgets }
                .collect { budgets ->
                    computeBudgetStatuses(_currentMonth.value, budgets)
                }
        }
    }

    private suspend fun computeBudgetStatuses(month: String, budgets: List<Budget>) {
        try {
            val (start, end) = monthToRange(month)
            val totalBudget = budgets.find { it.categoryName == null }
            val categoryBudgets = budgets.filter { it.categoryName != null }

            // 总预算状态
            if (totalBudget != null) {
                val totalSpent = kotlin.math.abs(transactionDao.getMonthlyExpense(start, end))
                _totalBudgetStatus.value = BudgetStatus(
                    budgetAmount = totalBudget.amount,
                    spentAmount = totalSpent,
                    categoryName = null,
                    isOverBudget = totalSpent > totalBudget.amount,
                    percentage = if (totalBudget.amount > 0) (totalSpent / totalBudget.amount).toFloat() else 0f
                )
            } else {
                _totalBudgetStatus.value = null
            }

            // 分类预算状态
            val statuses = categoryBudgets.map { budget ->
                val spent = transactionDao.getCategoryExpense(start, end, budget.categoryName!!)
                BudgetStatus(
                    budgetAmount = budget.amount,
                    spentAmount = spent,
                    categoryName = budget.categoryName,
                    isOverBudget = spent > budget.amount,
                    percentage = if (budget.amount > 0) (spent / budget.amount).toFloat() else 0f
                )
            }
            _budgetStatuses.value = statuses
        } catch (e: Exception) {
            android.util.Log.e("BudgetVM", "computeBudgetStatuses failed", e)
        }
    }

    fun navigatePrevMonth() {
        _currentMonth.value = shiftMonth(_currentMonth.value, -1)
    }

    fun navigateNextMonth() {
        _currentMonth.value = shiftMonth(_currentMonth.value, 1)
    }

    fun refresh() {
        _refreshTrigger.value++
        viewModelScope.launch {
            val budgets = budgetDao.getByMonth(_currentMonth.value).first()
            computeBudgetStatuses(_currentMonth.value, budgets)
        }
    }

    fun showAddTotalBudget() {
        _editingBudget.value = Budget(
            yearMonth = _currentMonth.value,
            amount = 0.0,
            categoryName = null
        )
        _showEditDialog.value = true
    }

    fun showAddCategoryBudget(categoryName: String) {
        _editingBudget.value = Budget(
            yearMonth = _currentMonth.value,
            amount = 0.0,
            categoryName = categoryName
        )
        _showEditDialog.value = true
    }

    fun showEditBudget(budget: Budget) {
        _editingBudget.value = budget
        _showEditDialog.value = true
    }

    fun dismissEditDialog() {
        _showEditDialog.value = false
        _editingBudget.value = null
    }

    fun saveBudget(amount: Double, categoryName: String?) {
        viewModelScope.launch {
            val existing = if (categoryName != null) {
                budgetDao.getCategoryBudget(_currentMonth.value, categoryName)
            } else {
                budgetDao.getTotalBudget(_currentMonth.value)
            }
            if (existing != null) {
                budgetDao.upsert(existing.copy(amount = amount, updatedAt = System.currentTimeMillis()))
            } else {
                budgetDao.upsert(Budget(
                    yearMonth = _currentMonth.value,
                    amount = amount,
                    categoryName = categoryName,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    enabled = true
                ))
            }
            dismissEditDialog()
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch { budgetDao.delete(budget) }
    }

    fun toggleBudgetEnabled(budget: Budget) {
        viewModelScope.launch {
            budgetDao.upsert(budget.copy(enabled = !budget.enabled, updatedAt = System.currentTimeMillis()))
        }
    }

    // ═══ 手动负债管理 ═══
    fun showAddDebtDialog() {
        _debtDialogState.value = null
        _showDebtDialog.value = true
    }

    fun showEditDebtDialog(debt: ManualDebt) {
        _debtDialogState.value = debt
        _showDebtDialog.value = true
    }

    fun dismissDebtDialog() {
        _debtDialogState.value = null
        _showDebtDialog.value = false
    }

    fun saveDebt(name: String, totalAmount: Double, monthlyPayment: Double, interestRate: Float, notes: String) {
        viewModelScope.launch {
            val existing = _debtDialogState.value
            if (existing != null) {
                manualDebtDao.upsert(existing.copy(
                    name = name,
                    totalAmount = totalAmount,
                    monthlyPayment = monthlyPayment,
                    interestRate = interestRate,
                    notes = notes
                ))
            } else {
                manualDebtDao.upsert(ManualDebt(
                    name = name,
                    totalAmount = totalAmount,
                    monthlyPayment = monthlyPayment,
                    interestRate = interestRate,
                    notes = notes
                ))
            }
            dismissDebtDialog()
        }
    }

    fun deleteDebt(debt: ManualDebt) {
        viewModelScope.launch { manualDebtDao.delete(debt) }
    }

    private fun currentYearMonth(cal: Calendar = Calendar.getInstance()): String {
        return "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}"
    }

    private fun shiftMonth(yearMonth: String, delta: Int): String {
        val parts = yearMonth.split("-")
        if (parts.size != 2) return yearMonth
        val year = parts[0].toIntOrNull() ?: return yearMonth
        val month = parts[1].toIntOrNull() ?: return yearMonth
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1)
        cal.add(Calendar.MONTH, delta)
        return "${cal.get(Calendar.YEAR)}-${String.format("%02d", cal.get(Calendar.MONTH) + 1)}"
    }

    private fun monthToRange(yearMonth: String): Pair<Long, Long> {
        val parts = yearMonth.split("-")
        if (parts.size != 2) return 0L to 0L
        val year = parts[0].toIntOrNull() ?: return 0L to 0L
        val month = parts[1].toIntOrNull() ?: return 0L to 0L
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.set(year, month - 1, cal.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis
        return start to end
    }
}
