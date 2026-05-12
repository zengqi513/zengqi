package com.autobookkeeper.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import androidx.core.content.FileProvider
import com.autobookkeeper.data.Transaction
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvExportHelper {

    private val DF_DATE = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val DF_TIME = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val DF_FNAME = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    // 用于接收分享完成的广播
    const val ACTION_SHARE_COMPLETED = "com.autobookkeeper.SHARE_COMPLETED"

    fun exportToCsv(context: Context, transactions: List<Transaction>): File {
        val cacheDir = File(context.cacheDir, "exports")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val fileName = "autobookkeeper_${DF_FNAME.format(Date())}.csv"
        val file = File(cacheDir, fileName)

        val sb = StringBuilder()
        sb.append("日期,时间,类型,分类,金额,来源,备注\n")

        transactions.sortedByDescending { it.date }.forEach { txn ->
            val date = DF_DATE.format(Date(txn.date))
            val time = DF_TIME.format(Date(txn.date))
            val type = if (txn.amount < 0) "支出" else "收入"
            val amount = kotlin.math.abs(txn.amount)
            val note = txn.note?.replace(",", "，")?.replace("\n", " ") ?: ""
            
            sb.append("$date,$time,$type,${txn.categoryName},$amount,${txn.source.label},$note\n")
        }

        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    fun exportQuickCsv(context: Context, transactions: List<Transaction>, startDate: Long, endDate: Long, label: String): File {
        val range = transactions.filter { it.date in startDate..endDate }
        val cacheDir = File(context.cacheDir, "exports")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val fileName = "autobookkeeper_${label}_${DF_FNAME.format(Date())}.csv"
        val file = File(cacheDir, fileName)

        val sb = StringBuilder()
        sb.append("日期,时间,类型,分类,金额,来源,备注\n")

        range.sortedByDescending { it.date }.forEach { txn ->
            val date = DF_DATE.format(Date(txn.date))
            val time = DF_TIME.format(Date(txn.date))
            val type = if (txn.amount < 0) "支出" else "收入"
            val amount = kotlin.math.abs(txn.amount)
            val note = txn.note?.replace(",", "，")?.replace("\n", " ") ?: ""
            
            sb.append("$date,$time,$type,${txn.categoryName},$amount,${txn.source.label},$note\n")
        }

        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    fun shareCsv(context: Context, file: File, subject: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            
            // 创建一个回调 Intent，用于接收分享完成/取消的通知
            val callbackIntent = Intent(ACTION_SHARE_COMPLETED).apply {
                setPackage(context.packageName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                callbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // 使用 Intent.createChooser 带 IntentSender 回调
            val chooser = Intent.createChooser(intent, "分享记账数据", pendingIntent.intentSender).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "分享失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
