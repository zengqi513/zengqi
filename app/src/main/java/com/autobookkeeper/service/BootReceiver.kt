package com.autobookkeeper.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

/**
 * 开机启动接收器 —— 确保通知监听服务在开机后自动激活
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "📱 开机完成，检查通知监听服务状态")
            
            // 检查通知监听是否已启用
            val cn = ComponentName(context, PaymentNotificationListener::class.java)
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
            
            val isEnabled = enabledListeners.contains(cn.flattenToString())
            Log.i(TAG, "通知监听状态: ${if (isEnabled) "已启用" else "未启用"}")
            
            if (!isEnabled) {
                // 尝试重新启用（需要用户授权）
                Log.w(TAG, "⚠️ 通知监听未启用，需要用户手动授权")
            }
        }
    }
}
