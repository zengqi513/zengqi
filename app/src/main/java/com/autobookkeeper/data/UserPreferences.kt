package com.autobookkeeper.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    
    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val FOLLOW_SYSTEM_KEY = booleanPreferencesKey("follow_system")
        private val DAILY_LIMIT_KEY = stringPreferencesKey("daily_living_limit")
        private val ANNUAL_INCOME_KEY = stringPreferencesKey("annual_income")
        private val MONTHLY_INCOME_KEY = stringPreferencesKey("monthly_income")
        private val THEME_PALETTE_KEY = stringPreferencesKey("theme_palette")
        private val AI_FLOATING_VISIBLE_KEY = booleanPreferencesKey("ai_floating_visible")
        private val AI_FLOATING_X_KEY = stringPreferencesKey("ai_floating_x")
        private val AI_FLOATING_Y_KEY = stringPreferencesKey("ai_floating_y")
        
        // 云备份设置
        private val CLOUD_BACKUP_ENABLED_KEY = booleanPreferencesKey("cloud_backup_enabled")
        private val CLOUD_BACKUP_ENCRYPT_KEY = booleanPreferencesKey("cloud_backup_encrypt")
        private val CLOUD_BACKUP_PASSWORD_KEY = stringPreferencesKey("cloud_backup_password")
        private val CLOUD_BACKUP_PROVIDER_KEY = stringPreferencesKey("cloud_backup_provider")
        private val CLOUD_BACKUP_LAST_TIME_KEY = stringPreferencesKey("cloud_backup_last_time")
    }
    
    /** 是否跟随系统主题 */
    val followSystem: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[FOLLOW_SYSTEM_KEY] ?: true
    }
    
    /** 深色模式（仅当不跟随系统时有效） */
    val darkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[DARK_MODE_KEY] ?: false
    }
    
    /** 主题配色方案名称（默认 WarmGreen） */
    val themePalette: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_PALETTE_KEY] ?: "WarmGreen"
    }

    suspend fun setThemePalette(name: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_PALETTE_KEY] = name
        }
    }

    /** 实际是否使用深色模式 */
    val effectiveDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        val followSystem = prefs[FOLLOW_SYSTEM_KEY] ?: true
        if (followSystem) {
            // 跟随系统时，返回 null 表示需要调用 isSystemInDarkTheme()
            false
        } else {
            prefs[DARK_MODE_KEY] ?: false
        }
    }
    
    suspend fun setFollowSystem(follow: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[FOLLOW_SYSTEM_KEY] = follow
        }
    }
    
    suspend fun setDarkMode(dark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = dark
            prefs[FOLLOW_SYSTEM_KEY] = false  // 手动设置时不再跟随系统
        }
    }

    /** 每日生活费上限（元），null 表示未设置 */
    val dailyLivingLimit: Flow<Double?> = context.dataStore.data.map { prefs ->
        val raw = prefs[DAILY_LIMIT_KEY]
        raw?.toDoubleOrNull()
    }

    suspend fun setDailyLivingLimit(limit: Double?) {
        context.dataStore.edit { prefs ->
            if (limit != null) {
                prefs[DAILY_LIMIT_KEY] = limit.toString()
            } else {
                prefs.remove(DAILY_LIMIT_KEY)
            }
        }
    }

    /** 手动设置的年收入（元），0 表示未设置 */
    val annualIncome: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[ANNUAL_INCOME_KEY]?.toDoubleOrNull() ?: 0.0
    }

    /** 手动设置的月收入（元），0 表示未设置 */
    val monthlyIncome: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[MONTHLY_INCOME_KEY]?.toDoubleOrNull() ?: 0.0
    }

    suspend fun setAnnualIncome(income: Double) {
        context.dataStore.edit { prefs ->
            if (income > 0) {
                prefs[ANNUAL_INCOME_KEY] = String.format("%.0f", income)
            } else {
                prefs.remove(ANNUAL_INCOME_KEY)
            }
        }
    }

    suspend fun setMonthlyIncome(income: Double) {
        context.dataStore.edit { prefs ->
            if (income > 0) {
                prefs[MONTHLY_INCOME_KEY] = String.format("%.0f", income)
            } else {
                prefs.remove(MONTHLY_INCOME_KEY)
            }
        }
    }

    /** AI 悬浮入口是否可见（默认显示） */
    val aiFloatingVisible: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AI_FLOATING_VISIBLE_KEY] ?: true
    }

    suspend fun setAiFloatingVisible(visible: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AI_FLOATING_VISIBLE_KEY] = visible
        }
    }

    /** AI 悬浮入口位置 X（屏幕百分比 0.0-1.0） */
    val aiFloatingX: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[AI_FLOATING_X_KEY]?.toFloatOrNull() ?: 0.85f // 默认右上角
    }

    /** AI 悬浮入口位置 Y（屏幕百分比 0.0-1.0） */
    val aiFloatingY: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[AI_FLOATING_Y_KEY]?.toFloatOrNull() ?: 0.15f
    }

    suspend fun setAiFloatingPosition(x: Float, y: Float) {
        context.dataStore.edit { prefs ->
            prefs[AI_FLOATING_X_KEY] = x.coerceIn(0f, 1f).toString()
            prefs[AI_FLOATING_Y_KEY] = y.coerceIn(0f, 1f).toString()
        }
    }

    // ========== 云备份设置 ==========
    
    /** 云备份设置数据类 */
    data class CloudBackupSettings(
        val enabled: Boolean,
        val encrypt: Boolean,
        val encryptionPassword: String,
        val cloudProvider: String?, // google_drive, onedrive, dropbox
        val lastBackupTime: Long
    )
    
    /** 云备份设置流 */
    val cloudBackupSettings: Flow<CloudBackupSettings> = context.dataStore.data.map { prefs ->
        CloudBackupSettings(
            enabled = prefs[CLOUD_BACKUP_ENABLED_KEY] ?: false,
            encrypt = prefs[CLOUD_BACKUP_ENCRYPT_KEY] ?: false,
            encryptionPassword = prefs[CLOUD_BACKUP_PASSWORD_KEY] ?: "",
            cloudProvider = prefs[CLOUD_BACKUP_PROVIDER_KEY],
            lastBackupTime = prefs[CLOUD_BACKUP_LAST_TIME_KEY]?.toLongOrNull() ?: 0L
        )
    }
    
    suspend fun setCloudBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[CLOUD_BACKUP_ENABLED_KEY] = enabled
        }
    }
    
    suspend fun setCloudBackupEncrypt(encrypt: Boolean, password: String = "") {
        context.dataStore.edit { prefs ->
            prefs[CLOUD_BACKUP_ENCRYPT_KEY] = encrypt
            if (encrypt && password.isNotBlank()) {
                prefs[CLOUD_BACKUP_PASSWORD_KEY] = password
            } else if (!encrypt) {
                prefs.remove(CLOUD_BACKUP_PASSWORD_KEY)
            }
        }
    }
    
    suspend fun setCloudBackupProvider(provider: String?) {
        context.dataStore.edit { prefs ->
            if (provider != null) {
                prefs[CLOUD_BACKUP_PROVIDER_KEY] = provider
            } else {
                prefs.remove(CLOUD_BACKUP_PROVIDER_KEY)
            }
        }
    }
    
    suspend fun updateLastBackupTime(timeMillis: Long) {
        context.dataStore.edit { prefs ->
            prefs[CLOUD_BACKUP_LAST_TIME_KEY] = timeMillis.toString()
        }
    }
}
