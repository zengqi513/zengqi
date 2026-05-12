package com.autobookkeeper.backup

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.autobookkeeper.data.AppDatabase
import com.autobookkeeper.data.CategoryData
import com.autobookkeeper.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 数据备份：将账目 + 分类导出为 JSON 文件
 */
object BackupHelper {

    private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val FILE_FMT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    const val ACTION_BACKUP_COMPLETED = "com.autobookkeeper.BACKUP_COMPLETED"

    /**
     * 创建备份 JSON 文件，返回文件对象
     */
    suspend fun createBackup(context: Context, db: AppDatabase): File = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "backup")
        cacheDir.mkdirs()
        val file = File(cacheDir, "autobookkeeper_backup_${FILE_FMT.format(Date())}.json")

        FileWriter(file).use { w ->
            val root = JSONObject()
            root.put("version", 1)
            root.put("createdAt", DATE_FMT.format(Date()))
            root.put("appVersion", try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) { "unknown" })

            // 导出账目
            val transactions = db.transactionDao().getAllSync()
            val txnsArr = JSONArray()
            transactions.forEach { txn ->
                txnsArr.put(JSONObject().apply {
                    put("id", txn.id)
                    put("amount", txn.amount)
                    put("categoryName", txn.categoryName)
                    put("categoryIcon", txn.categoryIcon)
                    put("source", txn.source.name)
                    put("note", txn.note)
                    put("date", txn.date)
                    put("createdAt", txn.createdAt)
                    put("updatedAt", txn.updatedAt)
                })
            }
            root.put("transactions", txnsArr)

            // 导出分类
            val categories = db.categoryDao().getAllSync()
            val catsArr = JSONArray()
            categories.forEach { cat ->
                catsArr.put(JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("icon", cat.icon)
                    put("type", cat.type)
                    put("parentName", cat.parentName ?: JSONObject.NULL)
                    put("isCustom", cat.isCustom)
                })
            }
            root.put("categories", catsArr)

            w.write(root.toString(2))
        }
        file
    }

    /**
     * 恢复备份：从 JSON 文件导入（合并策略：保留本地，新增的插入，冲突的跳过）
     */
    suspend fun restoreBackup(context: Context, db: AppDatabase, file: File): RestoreResult = withContext(Dispatchers.IO) {
        val text = file.readText()
        val root = JSONObject(text)

        val txnCount = root.optInt("txnCount", 0)
        val catCount = root.optInt("catCount", 0)
        var importedTxns = 0
        var importedCats = 0
        var skipped = 0

        // 恢复账目
        val txnsArr = root.optJSONArray("transactions") ?: JSONArray()
        val existingIds = db.transactionDao().getAllSync().map { it.id }.toSet()

        for (i in 0 until txnsArr.length()) {
            val obj = txnsArr.getJSONObject(i)
            val id = obj.optLong("id", 0)
            if (id in existingIds) {
                skipped++
                continue
            }
            val txn = Transaction(
                id = 0, // 重新生成
                amount = obj.getDouble("amount"),
                categoryName = obj.getString("categoryName"),
                categoryIcon = obj.getString("categoryIcon"),
                source = com.autobookkeeper.data.Source.valueOf(obj.optString("source", "MANUAL")),
                note = obj.optString("note", ""),
                date = obj.getLong("date"),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            )
            db.transactionDao().insert(txn)
            importedTxns++
        }

        // 恢复自定义分类
        val catsArr = root.optJSONArray("categories") ?: JSONArray()
        val existingCatNames = db.categoryDao().getAllSync().map { it.name }.toSet()

        for (i in 0 until catsArr.length()) {
            val obj = catsArr.getJSONObject(i)
            if (!obj.optBoolean("isCustom", false)) continue
            val name = obj.getString("name")
            if (name in existingCatNames) {
                skipped++
                continue
            }
            val cat = CategoryData(
                id = 0,
                name = name,
                icon = obj.getString("icon"),
                type = obj.getString("type"),
                parentName = if (obj.isNull("parentName")) null else obj.getString("parentName"),
                isCustom = true
            )
            db.categoryDao().insert(cat)
            importedCats++
        }

        RestoreResult(importedTxns, importedCats, skipped)
    }

    /**
     * 恢复时 pick 文件
     */
    fun shareBackup(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            
            val callbackIntent = Intent(ACTION_BACKUP_COMPLETED).apply {
                setPackage(context.packageName)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                callbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "AutoBookkeeper 备份 ${DATE_FMT.format(Date())}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "分享备份文件", pendingIntent.intentSender).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "分享失败: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun openBackupFilePicker(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/json"
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

data class RestoreResult(
    val importedTransactions: Int,
    val importedCategories: Int,
    val skipped: Int
)
