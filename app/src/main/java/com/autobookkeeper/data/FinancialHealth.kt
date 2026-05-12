package com.autobookkeeper.data

import androidx.room.TypeConverter

enum class AlertType {
    DAILY_BUDGET, OVER_BUDGET, PROJECTED_OVER, AUTO_RENEWAL, DEBT_WARNING
}

class AlertTypeConverter {
    @TypeConverter
    fun fromAlertType(value: AlertType): String = value.name
    @TypeConverter
    fun toAlertType(value: String): AlertType = AlertType.valueOf(value)
}

/**
 * 债务分析结果（仅用于手动负债的总览展示）
 */
data class DebtAnalysis(
    val totalDebt: Double,
    val monthlyRepayment: Double,
    val debtItems: List<ManualDebt> = emptyList()
)