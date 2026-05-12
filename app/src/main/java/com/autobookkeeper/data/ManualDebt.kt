package com.autobookkeeper.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * 手动录入的个人负债条目（替代自动债务分析）
 * 由用户在预算管理界面手动增删改
 */
@Entity(tableName = "manual_debts")
data class ManualDebt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,            // 负债名称如"花呗"、"信用卡分期"等
    val totalAmount: Double,     // 总欠款金额
    val monthlyPayment: Double,  // 月还款额
    val interestRate: Float = 0f, // 年利率（可选）
    val notes: String = "",      // 备注
    val sortOrder: Int = 0,      // 排序
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ManualDebtDao {
    @Query("SELECT * FROM manual_debts ORDER BY sortOrder ASC, id ASC")
    fun getAll(): Flow<List<ManualDebt>>

    @Query("SELECT * FROM manual_debts ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllSync(): List<ManualDebt>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(debt: ManualDebt): Long

    @Delete
    suspend fun delete(debt: ManualDebt)

    @Query("DELETE FROM manual_debts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM manual_debts")
    suspend fun deleteAll()
}