package com.autobookkeeper.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["yearMonth", "categoryName"], unique = true)]
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val yearMonth: String,
    val amount: Double,
    val categoryName: String?,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class BudgetStatus(
    val budgetAmount: Double,
    val spentAmount: Double,
    val categoryName: String?,
    val isOverBudget: Boolean,
    val percentage: Float
)

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth ORDER BY categoryName IS NULL DESC, id")
    fun getByMonth(yearMonth: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth AND categoryName IS NULL LIMIT 1")
    suspend fun getTotalBudget(yearMonth: String): Budget?

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth AND categoryName = :categoryName LIMIT 1")
    suspend fun getCategoryBudget(yearMonth: String, categoryName: String): Budget?

    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth AND categoryName IS NOT NULL")
    suspend fun getCategoryBudgets(yearMonth: String): List<Budget>

    // 用于 AI 分析的同步方法
    suspend fun getBudgetForMonthSync(year: Int, month: Int): Budget? {
        val yearMonth = String.format("%04d-%02d", year, month)
        return getTotalBudget(yearMonth)
    }

    suspend fun getTotalBudgetSync(year: Int, month: Int): Budget? {
        val yearMonth = String.format("%04d-%02d", year, month)
        return getTotalBudget(yearMonth)
    }

    suspend fun getCategoryBudgetsSync(year: Int, month: Int): List<Budget> {
        val yearMonth = String.format("%04d-%02d", year, month)
        return getCategoryBudgets(yearMonth)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: Budget): Long

    @Delete
    suspend fun delete(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM budgets WHERE yearMonth = :yearMonth AND categoryName IS NOT NULL")
    suspend fun deleteCategoryBudgetsByMonth(yearMonth: String)

    @Query("DELETE FROM budgets WHERE yearMonth = :yearMonth")
    suspend fun deleteByMonth(yearMonth: String)
}