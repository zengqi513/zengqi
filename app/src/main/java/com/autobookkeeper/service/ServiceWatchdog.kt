package com.autobookkeeper.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 服务看门狗 - 增强版保活机制
 * 
 * 策略：
 * 1. 请求电池优化白名单（最关键）
 * 2. 双保险AlarmManager（精确+不精确）
 * 3. JobScheduler 备用保活
 * 4. 启动时强制重新绑定服务
 */
class ServiceWatchdog : BroadcastReceiver() {

    companion object {
        private const val TAG = "ServiceWatchdog"
        private const val ACTION_CHECK_SERVICE = "com.autobookkeeper.action.CHECK_SERVICE"
        private const val ACTION_FORCE_REBIND = "com.autobookkeeper.action.FORCE_REBIND"
        private const val CHECK_INTERVAL_MS = 30000L // 30秒检查一次（更频繁）
        private const val FORCE_REBIND_INTERVAL_MS = 120000L // 2分钟强制重绑
        
        /**
         * 检查是否在电池优化白名单中
         */
        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        
        /**
         * 请求电池优化白名单
         */
        fun requestBatteryOptimizationWhitelist(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    Log.d(TAG, "已请求电池优化白名单")
                } catch (e: Exception) {
                    Log.e(TAG, "请求白名单失败: ${e.message}")
                    // 降级：打开应用电池设置页面
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (e2: Exception) {
                        Log.e(TAG, "打开设置失败: ${e2.message}")
                    }
                }
            }
        }

        /**
         * 启动看门狗监控
         */
        fun start(context: Context) {
            Log.d(TAG, "启动增强版服务看门狗")
            
            // 检查电池优化状态
            if (!isIgnoringBatteryOptimizations(context)) {
                Log.w(TAG, "未在电池优化白名单中，建议用户添加")
            }
            
            // 启动时立即检查并重新绑定
            checkAndRestartService(context, force = true)
            
            // 调度常规检查
            scheduleCheck(context, CHECK_INTERVAL_MS)
            
            // 调度强制重绑（双保险）
            scheduleForceRebind(context, FORCE_REBIND_INTERVAL_MS)
        }

        /**
         * 停止看门狗
         */
        fun stop(context: Context) {
            Log.d(TAG, "停止服务看门狗")
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            
            // 取消常规检查
            val checkIntent = Intent(context, ServiceWatchdog::class.java).apply {
                action = ACTION_CHECK_SERVICE
            }
            val checkPendingIntent = PendingIntent.getBroadcast(
                context, 0, checkIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            checkPendingIntent?.let { alarmManager.cancel(it) }
            
            // 取消强制重绑
            val rebindIntent = Intent(context, ServiceWatchdog::class.java).apply {
                action = ACTION_FORCE_REBIND
            }
            val rebindPendingIntent = PendingIntent.getBroadcast(
                context, 1, rebindIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            rebindPendingIntent?.let { alarmManager.cancel(it) }
        }

        /**
         * 立即检查一次服务状态
         */
        fun checkNow(context: Context) {
            CoroutineScope(Dispatchers.Default).launch {
                delay(1000)
                checkAndRestartService(context, force = false)
            }
        }

        private fun scheduleCheck(context: Context, delayMs: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ServiceWatchdog::class.java).apply {
                action = ACTION_CHECK_SERVICE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val triggerTime = SystemClock.elapsedRealtime() + delayMs

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // 使用setAndAllowWhileIdle，比setExactAndAllowWhileIdle更可靠
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "调度检查失败: ${e.message}")
                // 降级为普通定时
                alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        }
        
        private fun scheduleForceRebind(context: Context, delayMs: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ServiceWatchdog::class.java).apply {
                action = ACTION_FORCE_REBIND
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, 1, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val triggerTime = SystemClock.elapsedRealtime() + delayMs

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "调度强制重绑失败: ${e.message}")
            }
        }

        private fun checkAndRestartService(context: Context, force: Boolean = false) {
            val isHealthy = PaymentNotificationListener.isServiceHealthy()
            val isConnected = PaymentNotificationListener.isConnected

            Log.d(TAG, "服务状态检查 - 健康=$isHealthy, 连接=$isConnected, 强制=$force")

            if (force || !isHealthy || !isConnected) {
                Log.w(TAG, "服务异常或强制重启，尝试恢复...")
                restartService(context)
            }

            // 继续下一次检查
            scheduleCheck(context, CHECK_INTERVAL_MS)
        }

        private fun restartService(context: Context) {
            try {
                // 方法1: 请求重新绑定
                PaymentNotificationListener.requestRebind(context)
                Log.d(TAG, "已请求重新绑定服务")
                
                // 方法2: 通过ComponentName强制重新绑定（更可靠）
                CoroutineScope(Dispatchers.Default).launch {
                    delay(2000) // 等待2秒后检查
                    if (!PaymentNotificationListener.isConnected) {
                        Log.w(TAG, "重新绑定失败，尝试强制重绑...")
                        try {
                            val componentName = ComponentName(context, PaymentNotificationListener::class.java)
                            val pm = context.packageManager
                            pm.setComponentEnabledSetting(
                                componentName,
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                PackageManager.DONT_KILL_APP
                            )
                            delay(500)
                            pm.setComponentEnabledSetting(
                                componentName,
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                PackageManager.DONT_KILL_APP
                            )
                            Log.d(TAG, "已强制切换组件状态")
                        } catch (e: Exception) {
                            Log.e(TAG, "强制重绑失败: ${e.message}")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "重启服务失败: ${e.message}", e)
            }
        }
        
        private fun forceRebindService(context: Context) {
            Log.d(TAG, "执行强制重绑...")
            try {
                val componentName = ComponentName(context, PaymentNotificationListener::class.java)
                val pm = context.packageManager
                
                // 先禁用再启用，强制系统重新绑定
                pm.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                
                CoroutineScope(Dispatchers.Default).launch {
                    delay(1000)
                    pm.setComponentEnabledSetting(
                        componentName,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.d(TAG, "强制重绑完成")
                    
                    // 再次请求重新绑定
                    delay(1000)
                    PaymentNotificationListener.requestRebind(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "强制重绑失败: ${e.message}")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_CHECK_SERVICE -> {
                Log.d(TAG, "收到检查广播")
                checkAndRestartService(context, force = false)
            }
            ACTION_FORCE_REBIND -> {
                Log.d(TAG, "收到强制重绑广播")
                forceRebindService(context)
                // 继续下一次强制重绑
                scheduleForceRebind(context, FORCE_REBIND_INTERVAL_MS)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "设备启动完成，启动看门狗")
                // 延迟启动，等待系统稳定
                CoroutineScope(Dispatchers.Default).launch {
                    delay(10000) // 10秒后启动
                    start(context)
                }
            }
        }
    }
}
