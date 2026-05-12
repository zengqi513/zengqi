package com.autobookkeeper.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.autobookkeeper.data.AppDatabase
import com.autobookkeeper.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 云备份管理器 - 管理自动备份和云存储集成
 */
class CloudBackupManager(private val context: Context) {

    companion object {
        private const val TAG = "CloudBackupManager"
        private const val AUTO_BACKUP_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24小时
        private const val MAX_LOCAL_BACKUPS = 10 // 保留最近10个本地备份

        // 云存储Provider
        const val PROVIDER_GOOGLE_DRIVE = "google_drive"
        const val PROVIDER_ONEDRIVE = "onedrive"
        const val PROVIDER_DROPBOX = "dropbox"
    }

    private val userPreferences = UserPreferences(context)
    private val db = (context.applicationContext as com.autobookkeeper.App).database
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * 检查并执行自动备份
     */
    suspend fun checkAndPerformAutoBackup() {
        val settings = userPreferences.cloudBackupSettings.first()
        if (!settings.enabled) return

        val lastBackupTime = settings.lastBackupTime
        val now = System.currentTimeMillis()

        if (now - lastBackupTime < AUTO_BACKUP_INTERVAL_MS) {
            Log.d(TAG, "自动备份间隔未到，跳过")
            return
        }

        Log.d(TAG, "执行自动备份...")
        performBackup(
            encrypt = settings.encrypt,
            password = settings.encryptionPassword,
            uploadToCloud = settings.cloudProvider != null,
            cloudProvider = settings.cloudProvider
        )
    }

    /**
     * 执行备份
     */
    suspend fun performBackup(
        encrypt: Boolean = false,
        password: String? = null,
        uploadToCloud: Boolean = false,
        cloudProvider: String? = null
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            // 1. 创建备份
            val backupFile = if (encrypt && !password.isNullOrBlank()) {
                EncryptedBackupHelper.createEncryptedBackup(context, db, password)
            } else {
                BackupHelper.createBackup(context, db)
            }

            // 2. 清理旧备份
            cleanupOldBackups()

            // 3. 上传到云（通过系统分享）
            if (uploadToCloud && cloudProvider != null) {
                uploadToCloud(backupFile, cloudProvider)
            }

            // 4. 更新最后备份时间
            userPreferences.updateLastBackupTime(System.currentTimeMillis())

            BackupResult.Success(backupFile, uploadToCloud)
        } catch (e: Exception) {
            Log.e(TAG, "备份失败: ${e.message}", e)
            BackupResult.Error(e.message ?: "未知错误")
        }
    }

    /**
     * 上传到云存储（使用系统分享）
     */
    private suspend fun uploadToCloud(file: File, provider: String) = withContext(Dispatchers.Main) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "AutoBookkeeper 备份 ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // 根据provider设置特定包名
            when (provider) {
                PROVIDER_GOOGLE_DRIVE -> {
                    // Google Drive
                    setPackage("com.google.android.apps.docs")
                }
                PROVIDER_ONEDRIVE -> {
                    // OneDrive
                    setPackage("com.microsoft.skydrive")
                }
                PROVIDER_DROPBOX -> {
                    // Dropbox
                    setPackage("com.dropbox.android")
                }
            }
        }

        try {
            val chooser = Intent.createChooser(intent, "上传到云存储")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "启动云存储失败: ${e.message}")
        }
    }

    /**
     * 清理旧备份文件
     */
    private fun cleanupOldBackups() {
        val backupDir = File(context.cacheDir, "backup")
        if (!backupDir.exists()) return

        val files = backupDir.listFiles { f ->
            f.name.startsWith("autobookkeeper_") && (f.name.endsWith(".json") || f.name.endsWith(".abk"))
        } ?: return

        if (files.size <= MAX_LOCAL_BACKUPS) return

        // 按修改时间排序，删除最旧的
        files.sortBy { it.lastModified() }
        val toDelete = files.size - MAX_LOCAL_BACKUPS
        for (i in 0 until toDelete) {
            files[i].delete()
            Log.d(TAG, "删除旧备份: ${files[i].name}")
        }
    }

    /**
     * 获取备份历史
     */
    suspend fun getBackupHistory(): List<BackupInfo> = withContext(Dispatchers.IO) {
        val backupDir = File(context.cacheDir, "backup")
        if (!backupDir.exists()) return@withContext emptyList()

        val files = backupDir.listFiles { f ->
            f.name.startsWith("autobookkeeper_") && (f.name.endsWith(".json") || f.name.endsWith(".abk"))
        } ?: return@withContext emptyList()

        files.map { file ->
            BackupInfo(
                fileName = file.name,
                fileSize = file.length(),
                createdAt = file.lastModified(),
                isEncrypted = file.name.endsWith(".abk"),
                file = file
            )
        }.sortedByDescending { it.createdAt }
    }

    /**
     * 启动自动备份任务
     */
    fun startAutoBackup() {
        scope.launch {
            while (true) {
                checkAndPerformAutoBackup()
                delay(AUTO_BACKUP_INTERVAL_MS)
            }
        }
    }

    /**
     * 恢复备份
     */
    suspend fun restoreBackup(
        file: File,
        password: String? = null
    ): RestoreResult = withContext(Dispatchers.IO) {
        if (EncryptedBackupHelper.isEncryptedBackup(file) && !password.isNullOrBlank()) {
            EncryptedBackupHelper.restoreEncryptedBackup(context, db, file, password)
        } else {
            BackupHelper.restoreBackup(context, db, file)
        }
    }
}

// 备份结果
sealed class BackupResult {
    data class Success(val file: File, val uploadedToCloud: Boolean) : BackupResult()
    data class Error(val message: String) : BackupResult()
}

// 备份信息
data class BackupInfo(
    val fileName: String,
    val fileSize: Long,
    val createdAt: Long,
    val isEncrypted: Boolean,
    val file: File
) {
    fun getFormattedSize(): String {
        return when {
            fileSize < 1024 -> "$fileSize B"
            fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
            else -> "${fileSize / (1024 * 1024)} MB"
        }
    }

    fun getFormattedDate(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(createdAt))
    }
}
