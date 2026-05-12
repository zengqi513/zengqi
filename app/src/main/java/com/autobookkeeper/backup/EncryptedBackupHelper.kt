package com.autobookkeeper.backup

import android.content.Context
import android.net.Uri
import com.autobookkeeper.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * 加密备份助手 - 支持密码加密备份文件
 */
object EncryptedBackupHelper {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val ITERATION_COUNT = 100000
    private const val KEY_LENGTH = 256

    private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val FILE_FMT = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * 创建加密备份
     * @param context 上下文
     * @param db 数据库
     * @param password 加密密码
     * @return 加密后的备份文件
     */
    suspend fun createEncryptedBackup(
        context: Context,
        db: AppDatabase,
        password: String
    ): File = withContext(Dispatchers.IO) {
        // 1. 先创建普通备份JSON
        val jsonBackup = BackupHelper.createBackup(context, db)
        val jsonContent = jsonBackup.readText()

        // 2. 加密内容
        val encryptedData = encrypt(jsonContent, password)

        // 3. 创建加密备份文件
        val cacheDir = File(context.cacheDir, "backup")
        cacheDir.mkdirs()
        val file = File(cacheDir, "autobookkeeper_encrypted_${FILE_FMT.format(Date())}.abk")

        file.writeText(encryptedData)
        file
    }

    /**
     * 恢复加密备份
     * @param context 上下文
     * @param db 数据库
     * @param file 加密备份文件
     * @param password 解密密码
     * @return 恢复结果
     */
    suspend fun restoreEncryptedBackup(
        context: Context,
        db: AppDatabase,
        file: File,
        password: String
    ): RestoreResult = withContext(Dispatchers.IO) {
        // 1. 读取加密内容
        val encryptedData = file.readText()

        // 2. 解密
        val jsonContent = decrypt(encryptedData, password)

        // 3. 写入临时文件并恢复
        val tempFile = File(context.cacheDir, "temp_restore_${System.currentTimeMillis()}.json")
        tempFile.writeText(jsonContent)

        try {
            BackupHelper.restoreBackup(context, db, tempFile)
        } finally {
            tempFile.delete()
        }
    }

    /**
     * 加密数据
     */
    private fun encrypt(plaintext: String, password: String): String {
        // 生成随机盐值和IV
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(GCM_IV_LENGTH).apply { SecureRandom().nextBytes(this) }

        // 从密码派生密钥
        val key = deriveKey(password, salt)

        // 加密
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // 组合: salt + iv + ciphertext
        val combined = salt + iv + ciphertext

        // Base64编码
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    /**
     * 解密数据
     */
    private fun decrypt(encryptedData: String, password: String): String {
        val combined = Base64.decode(encryptedData, Base64.DEFAULT)

        // 提取salt, iv, ciphertext
        val salt = combined.copyOfRange(0, 16)
        val iv = combined.copyOfRange(16, 16 + GCM_IV_LENGTH)
        val ciphertext = combined.copyOfRange(16 + GCM_IV_LENGTH, combined.size)

        // 派生密钥
        val key = deriveKey(password, salt)

        // 解密
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val plaintext = cipher.doFinal(ciphertext)

        return String(plaintext, Charsets.UTF_8)
    }

    /**
     * 从密码派生密钥 (PBKDF2)
     */
    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, KEY_ALGORITHM)
    }

    /**
     * 验证备份文件是否为加密格式
     */
    fun isEncryptedBackup(file: File): Boolean {
        return try {
            val content = file.readText()
            // 尝试Base64解码，成功则可能是加密文件
            Base64.decode(content, Base64.DEFAULT)
            // 检查是否不是普通JSON
            !content.trim().startsWith("{")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 验证密码是否正确
     */
    suspend fun verifyPassword(file: File, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            decrypt(file.readText(), password)
            true
        } catch (e: Exception) {
            false
        }
    }
}
