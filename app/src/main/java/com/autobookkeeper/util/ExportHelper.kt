package com.autobookkeeper.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.autobookkeeper.data.Transaction
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {

    private val DF_DATE = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val DF_MONTH = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    private val DF_FNAME = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun buildExportFile(context: Context, transactions: List<Transaction>): File {
        val cacheDir = File(context.cacheDir, "exports")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val fileName = "autobookkeeper_${DF_FNAME.format(Date())}.xlsx"
        val file = File(cacheDir, fileName)

        val workbook = XSSFWorkbook()
        try {
            val headerStyle = createHeaderStyle(workbook)
            val moneyStyle = createMoneyStyle(workbook)
            val percentStyle = createPercentStyle(workbook)

            // Sheet 1: 明细账目
            val sheet1 = workbook.createSheet("明细账目")
            createHeaderRow(sheet1, arrayOf("日期", "时间", "类型", "分类", "图标", "金额", "来源", "备注"), headerStyle)
            
            transactions.sortedByDescending { it.date }.forEachIndexed { idx, txn ->
                val row = sheet1.createRow(idx + 1)
                val cal = Calendar.getInstance().apply { timeInMillis = txn.date }
                
                row.createCell(0).setCellValue(DF_DATE.format(Date(txn.date)))
                row.createCell(1).setCellValue(String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
                row.createCell(2).setCellValue(if (txn.amount < 0) "支出" else "收入")
                row.createCell(3).setCellValue(txn.categoryName)
                row.createCell(4).setCellValue(txn.categoryIcon)
                row.createCell(5).setCellValue(kotlin.math.abs(txn.amount))
                row.getCell(5).cellStyle = moneyStyle
                row.createCell(6).setCellValue(txn.source.label)
                row.createCell(7).setCellValue(txn.note ?: "")
            }
            for (i in 0..7) sheet1.autoSizeColumn(i)

            // Sheet 2: 月度汇总
            val sheet2 = workbook.createSheet("月度汇总")
            createHeaderRow(sheet2, arrayOf("月份", "收入", "支出", "结余", "笔数"), headerStyle)
            
            val monthly = transactions
                .groupBy { DF_MONTH.format(Date(it.date)) }
                .map { (month, txns) ->
                    val income = txns.filter { it.amount > 0 }.sumOf { it.amount }
                    val expense = kotlin.math.abs(txns.filter { it.amount < 0 }.sumOf { it.amount })
                    Triple(month, income, expense) to txns.size
                }
                .sortedByDescending { it.first.first }

            monthly.forEachIndexed { idx, (m, cnt) ->
                val (month, income, expense) = m
                val row = sheet2.createRow(idx + 1)
                row.createCell(0).setCellValue(month)
                row.createCell(1).setCellValue(income)
                row.getCell(1).cellStyle = moneyStyle
                row.createCell(2).setCellValue(expense)
                row.getCell(2).cellStyle = moneyStyle
                row.createCell(3).setCellValue(income - expense)
                row.getCell(3).cellStyle = moneyStyle
                row.createCell(4).setCellValue(cnt.toDouble())
            }
            for (i in 0..4) sheet2.autoSizeColumn(i)

            // Sheet 3: 分类排行
            val sheet3 = workbook.createSheet("分类排行")
            createHeaderRow(sheet3, arrayOf("分类", "图标", "支出总额", "笔数", "占比"), headerStyle)
            
            val catStats = transactions
                .filter { it.amount < 0 }
                .groupBy { it.categoryName }
                .map { (name, txns) ->
                    val icon = txns.first().categoryIcon
                    val total = kotlin.math.abs(txns.sumOf { it.amount })
                    Triple(name, icon, total) to txns.size
                }
                .sortedByDescending { it.first.third }

            val totalExpense = catStats.sumOf { it.first.third }
            catStats.forEachIndexed { idx, (m, cnt) ->
                val (name, icon, total) = m
                val row = sheet3.createRow(idx + 1)
                row.createCell(0).setCellValue(name)
                row.createCell(1).setCellValue(icon)
                row.createCell(2).setCellValue(total)
                row.getCell(2).cellStyle = moneyStyle
                row.createCell(3).setCellValue(cnt.toDouble())
                row.createCell(4).setCellValue(if (totalExpense > 0) total / totalExpense else 0.0)
                row.getCell(4).cellStyle = percentStyle
            }
            for (i in 0..4) sheet3.autoSizeColumn(i)

            // Sheet 4: 年度汇总
            val sheet4 = workbook.createSheet("年度汇总")
            createHeaderRow(sheet4, arrayOf("年份", "收入", "支出", "结余", "笔数"), headerStyle)
            
            val yearly = transactions
                .groupBy { Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.YEAR) }
                .map { (year, txns) ->
                    val income = txns.filter { it.amount > 0 }.sumOf { it.amount }
                    val expense = kotlin.math.abs(txns.filter { it.amount < 0 }.sumOf { it.amount })
                    year to Triple(income, expense, txns.size)
                }
                .sortedByDescending { it.first }

            yearly.forEachIndexed { idx, (year, m) ->
                val (income, expense, cnt) = m
                val row = sheet4.createRow(idx + 1)
                row.createCell(0).setCellValue(year.toDouble())
                row.createCell(1).setCellValue(income)
                row.getCell(1).cellStyle = moneyStyle
                row.createCell(2).setCellValue(expense)
                row.getCell(2).cellStyle = moneyStyle
                row.createCell(3).setCellValue(income - expense)
                row.getCell(3).cellStyle = moneyStyle
                row.createCell(4).setCellValue(cnt.toDouble())
            }
            for (i in 0..4) sheet4.autoSizeColumn(i)

            // Sheet 5: 来源分析
            val sheet5 = workbook.createSheet("来源分析")
            createHeaderRow(sheet5, arrayOf("来源", "类型", "金额", "笔数"), headerStyle)
            
            var rowIdx = 1
            transactions
                .groupBy { it.source }
                .forEach { (src, txns) ->
                    val income = txns.filter { it.amount > 0 }.sumOf { it.amount }
                    val expense = kotlin.math.abs(txns.filter { it.amount < 0 }.sumOf { it.amount })
                    if (income > 0) {
                        val row = sheet5.createRow(rowIdx++)
                        row.createCell(0).setCellValue(src.label)
                        row.createCell(1).setCellValue("收入")
                        row.createCell(2).setCellValue(income)
                        row.getCell(2).cellStyle = moneyStyle
                        row.createCell(3).setCellValue(txns.size.toDouble())
                    }
                    if (expense > 0) {
                        val row = sheet5.createRow(rowIdx++)
                        row.createCell(0).setCellValue(src.label)
                        row.createCell(1).setCellValue("支出")
                        row.createCell(2).setCellValue(expense)
                        row.getCell(2).cellStyle = moneyStyle
                        row.createCell(3).setCellValue(txns.size.toDouble())
                    }
                }
            for (i in 0..3) sheet5.autoSizeColumn(i)

            FileOutputStream(file).use { out -> workbook.write(out) }
        } finally {
            workbook.close()
        }
        return file
    }

    fun exportQuick(context: Context, transactions: List<Transaction>, startDate: Long, endDate: Long, label: String): File {
        val range = transactions.filter { it.date in startDate..endDate }
        val cacheDir = File(context.cacheDir, "exports")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val fileName = "autobookkeeper_${label}_${DF_FNAME.format(Date())}.xlsx"
        val file = File(cacheDir, fileName)

        val workbook = XSSFWorkbook()
        try {
            val headerStyle = createHeaderStyle(workbook)
            val moneyStyle = createMoneyStyle(workbook)

            val sheet = workbook.createSheet("账目明细")
            createHeaderRow(sheet, arrayOf("日期", "时间", "类型", "分类", "图标", "金额", "来源", "备注"), headerStyle)

            range.sortedByDescending { it.date }.forEachIndexed { idx, txn ->
                val row = sheet.createRow(idx + 1)
                val cal = Calendar.getInstance().apply { timeInMillis = txn.date }

                row.createCell(0).setCellValue(DF_DATE.format(Date(txn.date)))
                row.createCell(1).setCellValue(String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
                row.createCell(2).setCellValue(if (txn.amount < 0) "支出" else "收入")
                row.createCell(3).setCellValue(txn.categoryName)
                row.createCell(4).setCellValue(txn.categoryIcon)
                row.createCell(5).setCellValue(kotlin.math.abs(txn.amount))
                row.getCell(5).cellStyle = moneyStyle
                row.createCell(6).setCellValue(txn.source.label)
                row.createCell(7).setCellValue(txn.note ?: "")
            }
            for (i in 0..7) sheet.autoSizeColumn(i)

            FileOutputStream(file).use { out -> workbook.write(out) }
        } finally {
            workbook.close()
        }
        return file
    }

    fun shareFile(context: Context, file: File, subject: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "分享记账数据").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "分享失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun createHeaderStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        return workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            alignment = HorizontalAlignment.CENTER
            setFont(workbook.createFont().apply { bold = true })
        }
    }

    private fun createMoneyStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        return workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("#,##0.00")
        }
    }

    private fun createPercentStyle(workbook: XSSFWorkbook): XSSFCellStyle {
        return workbook.createCellStyle().apply {
            dataFormat = workbook.createDataFormat().getFormat("0.00%")
        }
    }

    private fun createHeaderRow(sheet: org.apache.poi.ss.usermodel.Sheet, headers: Array<String>, style: XSSFCellStyle) {
        val row = sheet.createRow(0)
        headers.forEachIndexed { idx, header ->
            row.createCell(idx).apply {
                setCellValue(header)
                cellStyle = style
            }
        }
    }
}
