package com.autobookkeeper.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobookkeeper.data.UserPreferences
import com.autobookkeeper.service.PaymentAccessibilityService
import com.autobookkeeper.service.PaymentNotificationListener
import com.autobookkeeper.util.NotificationLogHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isListenerEnabled by remember { mutableStateOf(false) }
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var diagnosticInfo by remember { mutableStateOf("") }
    var notifLogs by remember { mutableStateOf<List<NotificationLogHelper.LogEntry>>(emptyList()) }

    val followSystem by (userPreferences?.followSystem?.collectAsState(initial = true) ?: remember { mutableStateOf(true) })
    val darkModeSetting by (userPreferences?.darkMode?.collectAsState(initial = false) ?: remember { mutableStateOf(false) })
    val paletteName by (userPreferences?.themePalette?.collectAsState(initial = "WarmGreen") ?: remember { mutableStateOf("WarmGreen") })
    var showThemeDialog by remember { mutableStateOf(false) }
    var selFollow by remember { mutableStateOf(followSystem) }
    var selDark by remember { mutableStateOf(darkModeSetting) }

    val manufacturer = Build.MANUFACTURER.lowercase()
    val brand = Build.BRAND.lowercase()
    val isVivo = manufacturer.contains("vivo") || brand.contains("vivo") ||
                 manufacturer.contains("iqoo") || brand.contains("iqoo") ||
                 Build.MODEL.lowercase().contains("iqoo")
    val isXiaomi = manufacturer.contains("xiaomi") || brand.contains("xiaomi") || manufacturer.contains("redmi")
    val isOppo = manufacturer.contains("oppo") || brand.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus")
    val isHuawei = manufacturer.contains("huawei") || brand.contains("huawei") || manufacturer.contains("honor")
    val isSamsung = manufacturer.contains("samsung")

    fun checkPermission() {
        try {
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            isListenerEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationManager.isNotificationListenerAccessGranted(
                    ComponentName(context, PaymentNotificationListener::class.java))
            } else {
                @Suppress("DEPRECATION")
                Settings.Secure.getString(context.contentResolver,
                    // Use string literal to avoid deprecation warning that might break on some SDKs
                @Suppress("DEPRECATION")
                Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners"))
                    ?.contains(context.packageName) == true
            }
        } catch (_: Exception) { isListenerEnabled = false }
        isAccessibilityEnabled = try {
            @Suppress("DEPRECATION")
            val enabledServices = Settings.Secure.getString(context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            enabledServices.contains(context.packageName + "/" + PaymentAccessibilityService::class.java.canonicalName)
        } catch (_: Exception) { false }
        diagnosticInfo = buildString {
            append("包名: ${context.packageName}\n")
            append("SDK: ${Build.VERSION.SDK_INT}\n")
            append("型号: ${Build.MODEL}\n")
            append("品牌: ${Build.BRAND}\n")
            append("通知监听: $isListenerEnabled\n")
            append("无障碍: $isAccessibilityEnabled\n")
            append("通知日志条数: ${notifLogs.size}\n")
        }
    }

    LaunchedEffect(Unit) {
        checkPermission()
        notifLogs = NotificationLogHelper.getLogs(context).reversed().take(50)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置与帮助") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ═══ 深色主题 ═══
            if (userPreferences != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("深色主题", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold)
                            Text(
                                if (followSystem) "跟随系统" else if (darkModeSetting) "深色" else "浅色",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = {
                            selFollow = followSystem
                            selDark = darkModeSetting
                            showThemeDialog = true
                        }) { Text("切换") }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ═══ AI 悬浮按钮 ═══
            if (userPreferences != null) {
                val aiFloatingVisiblePref by userPreferences.aiFloatingVisible.collectAsState(initial = true)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // AI 图标使用主题色渐变
                        val colorScheme = MaterialTheme.colorScheme
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(colorScheme.primary, colorScheme.secondary)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // 高级AI图标
                            AiIcon(size = 24.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("AI 悬浮助手", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                if (aiFloatingVisiblePref) "首页显示可拖动的 AI 入口" else "已隐藏",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = aiFloatingVisiblePref,
                            onCheckedChange = { scope.launch { userPreferences.setAiFloatingVisible(it) } }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ═══ 通知监听状态 ═══
            val isServiceHealthy = remember { mutableStateOf(PaymentNotificationListener.isServiceHealthy()) }
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(5000)
                    isServiceHealthy.value = PaymentNotificationListener.isServiceHealthy()
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    val statusEmoji = when {
                        !isListenerEnabled -> "\uD83D\uDD07"
                        isServiceHealthy.value -> "\uD83D\uDD0A"
                        else -> "\u26A0\uFE0F"
                    }
                    Text(statusEmoji, fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("通知监听", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        val statusText = when {
                            !isListenerEnabled -> "未开启"
                            isServiceHealthy.value -> "运行中"
                            else -> "已断开（需重启）"
                        }
                        Text(statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                !isListenerEnabled -> MaterialTheme.colorScheme.error
                                isServiceHealthy.value -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            })
                    }
                    if (!isServiceHealthy.value && isListenerEnabled) {
                        TextButton(onClick = {
                            // 重启通知监听服务
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        }) { Text("重启", color = MaterialTheme.colorScheme.error) }
                    } else if (isVivo) {
                        Text("iQOO", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // ═══ 无障碍服务状态 ═══
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isAccessibilityEnabled) "\u267F" else "\u274C", fontSize = 24.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("无障碍服务", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(if (isAccessibilityEnabled) "已开启" else "未开启",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isAccessibilityEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error)
                    }
                    if (isVivo) Text("iQOO", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))

            // ═══ 权限指南 ═══
            val brandName = when {
                isVivo -> "iQOO/Vivo"
                isXiaomi -> "小米"
                isOppo -> "OPPO/一加"
                isHuawei -> "华为/荣耀"
                isSamsung -> "三星"
                else -> null
            }
            if (!isListenerEnabled || !isAccessibilityEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!isListenerEnabled)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (!isListenerEnabled) "\uD83D\uDD14" else "\u267F", fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(if (!isListenerEnabled) "开启通知监听" else "开启无障碍服务",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        if (!isListenerEnabled) {
                            Text("前往「设置」→「无障碍」→「已安装的应用」→ AutoBookkeeper → 开启通知监听",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!isAccessibilityEnabled) {
                            Text("前往「设置」→「无障碍」→「已安装的应用」→ AutoBookkeeper → 开启无障碍服务",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK))
                            } catch (_: Exception) { }
                        }) { Text("打开无障碍设置") }
                // 一键开启通知监听
                if (!isListenerEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    android.provider.Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS
                                } else {
                                    android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                                }
                                val cn = ComponentName(context, PaymentNotificationListener::class.java)
                                val uri = android.net.Uri.fromParts("package", context.packageName, null)
                                context.startActivity(Intent(intent).apply {
                                    data = uri
                                    putExtra("android.settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME", cn.flattenToString())
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            } catch (_: Exception) {
                                context.startActivity(Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("一键开启通知监听", modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
                    }
                }
                Spacer(Modifier.height(12.dp))

                // ═══ 品牌专属指引 ═══
                if (brandName != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("\uD83D\uDCF1", fontSize = 24.sp)
                                Spacer(Modifier.width(8.dp))
                                Text("$brandName 专属指引",
                                    style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            when {
                                isVivo -> {
                                    Text("【第一步】开启信任代理权限", style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                    listOf(
                                        "① 打开「设置」",
                                        "②「更多设置」→「无障碍」→「已安装的应用」",
                                        "③ 找到 AutoBookkeeper，开启无障碍",
                                        "④ 开启后，再次点击「AutoBookkeeper」",
                                        "⑤ 开启「信任代理」开关"
                                    ).forEach { Text("  $it", style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 1.dp)) }
                                    Spacer(Modifier.height(8.dp))
                                    Text("【第二步】在通知设置中开启", style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary)
                                    listOf(
                                        "① 点击下方按钮打开无障碍设置",
                                        "②「通知监听」→ 选择 AutoBookkeeper",
                                        "③ 开启后 AutoBookkeeper 图标旁显示绿色对勾"
                                    ).forEach { Text("  $it", style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 1.dp)) }
                                }
                                isXiaomi -> {
                                    listOf(
                                        "① 打开「设置」→「应用设置」→「系统应用设置」",
                                        "② 找到「无障碍」→「已安装的应用」→ AutoBookkeeper",
                                        "③ 开启「无障碍」和「通知权限」",
                                        "④ 如果找不到「通知权限」，去「设置」→「通知与控制中心」→「通知管理」",
                                        "⑤ 找到 AutoBookkeeper，开启「允许通知」和「通知监听」"
                                    ).forEach { Text("  $it", style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 1.dp)) }
                                }
                                isOppo -> {
                                    listOf(
                                        "① 打开「设置」→「应用管理」→「权限管理」",
                                        "② 找到 AutoBookkeeper，开启「通知监听」",
                                        "③ 打开「设置」→「其他设置」→「无障碍」→ AutoBookkeeper",
                                        "④ 开启无障碍服务"
                                    ).forEach { Text("  $it", style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 1.dp)) }
                                }
                                isHuawei -> {
                                    listOf(
                                        "① 打开「设置」→「应用和服务」→「权限管理」",
                                        "② 找到 AutoBookkeeper，开启「通知监听」",
                                        "③ 打开「设置」→「辅助功能」→「无障碍」→ AutoBookkeeper",
                                        "④ 开启无障碍服务"
                                    ).forEach { Text("  $it", style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 1.dp)) }
                                }
                                isSamsung -> {
                                    listOf(
                                        "① 打开「设置」→「辅助功能」→「已安装的应用」→ AutoBookkeeper",
                                        "② 开启「无障碍」和「通知监听」",
                                        "③ Samsung One UI 可能需要额外开启「允许通知访问」"
                                    ).forEach { Text("  $it", style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 1.dp)) }
                                }
                                else -> {
                                    Text("请按以下步骤开启权限：", style = MaterialTheme.typography.bodyMedium)
                                    listOf(
                                        "① 点击下方按钮打开系统设置",
                                        "② 搜索「无障碍」→「已安装的应用」→ AutoBookkeeper",
                                        "③ 开启「无障碍服务」和「通知监听」"
                                    ).forEach { Text("  $it", style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 1.dp)) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // ═══ 已开启提示 ═══
            if (isListenerEnabled && isAccessibilityEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\u2705", fontSize = 32.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("自动记账已就绪", style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold)
                                Text("已授权自动记账读取支付通知，交易会自动记录。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { checkPermission() }) {
                            Text("重新检查权限状态")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ═══ 通知日志 ═══
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDCCB 通知日志",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = {
                            NotificationLogHelper.clear(context)
                            notifLogs = emptyList()
                        }) { Icon(Icons.Default.DeleteOutline, "清除日志",
                            tint = MaterialTheme.colorScheme.error) }
                        IconButton(onClick = {
                            checkPermission()
                            notifLogs = NotificationLogHelper.getLogs(context).reversed().take(50)
                        }) { Icon(Icons.Default.Refresh, "刷新") }
                    }
                    Text("最近收到的目标App通知记录，用于诊断自动记账是否正常",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    if (notifLogs.isEmpty()) {
                        Text("暂无通知记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            "可能原因：",
                            "• 通知监听权限未开启（上方状态应为✅）",
                            "• 更新App后需要重新授权通知监听",
                            "• App 刚安装，还未收到支付通知",
                            "• 目前没有产生交易"
                        ).forEach { Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        notifLogs.forEach { entry ->
                            val statusColor = when (entry.result) {
                                "已记录" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                "已排除" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                colors = CardDefaults.cardColors(containerColor = statusColor),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(Modifier.padding(10.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(entry.title.ifBlank { "通知" },
                                            style = MaterialTheme.typography.bodyMedium)
                                        Spacer(Modifier.width(6.dp))
                                        Text(entry.source,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.weight(1f))
                                        Text(timeFmt.format(Date(entry.time)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (entry.text.isNotBlank()) {
                                        Text(entry.text,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2)
                                    }
                                    Text("结果: ${entry.result}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (entry.result) {
                                            "已记录" -> MaterialTheme.colorScheme.primary
                                            "已排除" -> MaterialTheme.colorScheme.onSurfaceVariant
                                            else -> MaterialTheme.colorScheme.error
                                        })
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // ═══ 支持的来源 ═══
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("支持的自动记账来源",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    val supportedApps = listOf(
                        "微信" to "支付通知、转账通知",
                        "支付宝" to "支付通知、转账通知",
                        "美团" to "外卖/团购支付通知",
                        "拼多多" to "支付通知",
                        "京东" to "支付通知",
                        "淘宝" to "支付通知",
                        "抖音" to "抖音支付/抖音月付通知",
                        "快手" to "快手支付通知",
                        "银行" to "银行扣款/入账通知(陆续接入)",
                        "买单吧" to "信用卡消费通知",
                        "翼支付" to "支付通知",
                        "云闪付" to "支付通知",
                        "华为钱包" to "支付通知",
                        "银联" to "银联支付通知"
                    )
                    supportedApps.forEach { (app, desc) ->
                        Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(app, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(8.dp))
                            Text(desc, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // ═══ 诊断信息 ═══
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("\uD83D\uDD0D 诊断信息",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(diagnosticInfo, style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace)
                }
            }
            Spacer(Modifier.height(12.dp))

            // ═══ 使用说明 ═══
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("使用说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "• 开启通知监听后，支付 App 的交易通知会被自动捕获",
                        "• 微信支付通知 → 自动识别金额、商户、分类",
                        "• 支付宝通知 → 自动识别金额、商户、分类",
                        "• 支持美团、拼多多、京东、淘宝等主流 App",
                        "• 消息通知被捕获后自动记账，无需手动输入",
                        "• 手动输入交易 → 点底部「+」按钮",
                        "• 导入账单 → 在数据管理页面上传",
                        "• 交易数据实时同步到各页面",
                        "",
                        "\uD83D\uDCCC 首次使用建议先去分类管理中配置自己的收支分类"
                    ).forEach {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 1.dp))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ═══ 深色模式弹窗 ═══
    if (showThemeDialog && userPreferences != null) {
        ThemeSettingsDialog(
            initialPalette = paletteName,
            initialFollowSystem = followSystem,
            initialDarkMode = darkModeSetting,
            onDismiss = { showThemeDialog = false },
            onConfirm = { palette, follow, dark ->
                scope.launch {
                    if (follow) userPreferences.setFollowSystem(true)
                    else userPreferences.setDarkMode(dark)
                    userPreferences.setThemePalette(palette)
                }
                showThemeDialog = false
            }
        )
    }
}

// ============================================
// 主题设置对话框
// ============================================
@Composable
private fun ThemeSettingsDialog(
    initialPalette: String,
    initialFollowSystem: Boolean,
    initialDarkMode: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (palette: String, followSystem: Boolean, darkMode: Boolean) -> Unit
) {
    var selTheme by remember { mutableStateOf(initialPalette) }
    var selF by remember { mutableStateOf(initialFollowSystem) }
    var selD by remember { mutableStateOf(initialDarkMode) }
    val themeOptions = listOf(
        Pair("WarmGreen", "\uD83C\uDF43 暖绿"),
        Pair("ForestGreen", "\uD83C\uDF32 深林"),
        Pair("IndigoBlue", "\uD83D\uDC8E 靛蓝"),
        Pair("RoseGold", "\uD83C\uDF38 玫瑰金"),
        Pair("Morandi", "\uD83C\uDFA8 莫兰迪"),
        Pair("PinkWhite", "\uD83E\uDD77 粉白"),
        Pair("SkyBlue", "\u2601\uFE0F 天空蓝"),
        Pair("CreamApricot", "\uD83E\uDFEB 奶杏紫"),
        Pair("CreamMint", "\uD83C\uDF43 奶油薄荷"),
        Pair("PeachCream", "\uD83C\uDF51 蜜桃米白"),
        Pair("Lavender", "\uD83D\uDC9C 香芋紫")
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("主题设置") },
        text = {
            Column {
                Text("主题色", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp))
                themeOptions.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selTheme = key }
                            .background(
                                if (selTheme == key)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selTheme == key) FontWeight.Bold else FontWeight.Normal)
                        if (selTheme == key) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Check, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // 跟随系统开关
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("跟随系统主题", style = MaterialTheme.typography.bodyMedium)
                        Text("自动匹配系统深色/浅色模式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = selF, onCheckedChange = { selF = it })
                }
                if (!selF) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("深色模式", style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f))
                        Switch(checked = selD, onCheckedChange = { selD = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selTheme, selF, selD) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
