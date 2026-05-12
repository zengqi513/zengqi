package com.autobookkeeper.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.autobookkeeper.App
import com.autobookkeeper.backup.BackupHelper
import com.autobookkeeper.backup.RestoreResult
import com.autobookkeeper.util.BillImporter
import com.autobookkeeper.util.CsvExportHelper
import com.autobookkeeper.util.DouyinPdfParser
import com.autobookkeeper.util.ExportHelper
import com.autobookkeeper.viewmodel.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit,
    onImportComplete: () -> Unit = {},
    onNavigateToDuplicateMerge: () -> Unit = {}
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val db = (appContext as App).database
    val scope = rememberCoroutineScope()

    var isExporting by remember { mutableStateOf(false) }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var lastBackupInfo by remember { mutableStateOf("") }
    var totalTxns by remember { mutableIntStateOf(0) }
    var totalCategories by remember { mutableIntStateOf(0) }
    var restoreResult by remember { mutableStateOf<RestoreResult?>(null) }
    var selectedRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var importResult by remember { mutableStateOf<BillImporter.ImportResult?>(null) }
    var pdfImportResult by remember { mutableStateOf<BillImporter.ImportResult?>(null) }
    
    // 加密备份对话框状态
    var showEncryptDialog by remember { mutableStateOf(false) }
    var encryptPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            totalTxns = db.transactionDao().getCount()
            totalCategories = db.categoryDao().getCount()
        }
    }

    // 文件选择器 - 抖音 PDF
    val douyinPdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            scope.launch(Dispatchers.IO) {
                try {
                    val existingCats = db.categoryDao().getAllSync()
                    val result = DouyinPdfParser.importPdf(appContext, uri, existingCats)
                    withContext(Dispatchers.Main) {
                        pdfImportResult = result
                        isImporting = false
                        totalTxns = db.transactionDao().getCount()
                        if (result.imported > 0) {
                            onImportComplete()
                        }
                        Toast.makeText(appContext, "抖音导入完成: ${result.imported} 条", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isImporting = false
                        importResult = BillImporter.ImportResult(errors = 1, errorMessages = listOf("导入失败: ${e.message}"))
                        Toast.makeText(appContext, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // 文件选择器 - 恢复
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedRestoreUri = uri
        }
    }

    // 文件选择器 - 导入账单
    val billPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            scope.launch(Dispatchers.IO) {
                try {
                    val existingCats = db.categoryDao().getAllSync()
                    val result = BillImporter.importCsv(appContext, uri, existingCats)
                    withContext(Dispatchers.Main) {
                        importResult = result
                        isImporting = false
                        totalTxns = db.transactionDao().getCount()
                        // 触发首页数据刷新
                        if (result.imported > 0) {
                            onImportComplete()
                        }
                        Toast.makeText(appContext, "导入完成: ${result.imported} 条", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isImporting = false
                        Toast.makeText(appContext, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .padding(bottom = 80.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 统计卡片 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("数据库统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("账目录数", style = MaterialTheme.typography.labelMedium)
                            Text("$totalTxns", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("分类数", style = MaterialTheme.typography.labelMedium)
                            Text("$totalCategories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── 导出 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("导出数据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("导出的 Excel 文件可分享到微信、邮箱或保存到下载", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 完整导出
                    Button(
                        onClick = {
                            isExporting = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val allTxns = db.transactionDao().getAllSync()
                                    if (allTxns.isEmpty()) {
                                        withContext(Dispatchers.Main) {
                                            isExporting = false
                                            Toast.makeText(appContext, "没有数据可导出", Toast.LENGTH_SHORT).show()
                                        }
                                        return@launch
                                    }
                                    val file = CsvExportHelper.exportToCsv(appContext, allTxns)
                                    withContext(Dispatchers.Main) {
                                        isExporting = false
                                        CsvExportHelper.shareCsv(appContext, file, "AutoBookkeeper记账数据")
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isExporting = false
                                        Toast.makeText(appContext, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isExporting && totalTxns > 0,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isExporting) {
                            Text("导出中...", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.FileDownload, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isExporting) "导出CSV" else "导出CSV")
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 快速导出本月
                    OutlinedButton(
                        onClick = {
                            isExporting = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val allTxns = db.transactionDao().getAllSync()
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.DAY_OF_MONTH, 1)
                                    cal.set(Calendar.HOUR_OF_DAY, 0)
                                    cal.set(Calendar.MINUTE, 0)
                                    cal.set(Calendar.SECOND, 0)
                                    cal.set(Calendar.MILLISECOND, 0)
                                    val start = cal.timeInMillis
                                    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                                    cal.set(Calendar.HOUR_OF_DAY, 23)
                                    cal.set(Calendar.MINUTE, 59)
                                    cal.set(Calendar.SECOND, 59)
                                    cal.set(Calendar.MILLISECOND, 999)
                                    val end = cal.timeInMillis
                                    val label = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                                    
                                    val range = allTxns.filter { it.date in start..end }
                                    if (range.isEmpty()) {
                                        withContext(Dispatchers.Main) {
                                            isExporting = false
                                            Toast.makeText(appContext, "本月没有数据", Toast.LENGTH_SHORT).show()
                                        }
                                        return@launch
                                    }
                                    val file = CsvExportHelper.exportQuickCsv(appContext, allTxns, start, end, label)
                                    withContext(Dispatchers.Main) {
                                        isExporting = false
                                        CsvExportHelper.shareCsv(appContext, file, "AutoBookkeeper $label 账目")
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isExporting = false
                                        Toast.makeText(appContext, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isExporting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("导出本月")
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 快速导出本周
                    OutlinedButton(
                        onClick = {
                            isExporting = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val allTxns = db.transactionDao().getAllSync()
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                                    cal.set(Calendar.HOUR_OF_DAY, 0)
                                    cal.set(Calendar.MINUTE, 0)
                                    cal.set(Calendar.SECOND, 0)
                                    cal.set(Calendar.MILLISECOND, 0)
                                    val start = cal.timeInMillis
                                    cal.add(Calendar.DAY_OF_YEAR, 6)
                                    cal.set(Calendar.HOUR_OF_DAY, 23)
                                    cal.set(Calendar.MINUTE, 59)
                                    cal.set(Calendar.SECOND, 59)
                                    cal.set(Calendar.MILLISECOND, 999)
                                    val end = cal.timeInMillis
                                    val label = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                    
                                    val range = allTxns.filter { it.date in start..end }
                                    if (range.isEmpty()) {
                                        withContext(Dispatchers.Main) {
                                            isExporting = false
                                            Toast.makeText(appContext, "本周没有数据", Toast.LENGTH_SHORT).show()
                                        }
                                        return@launch
                                    }
                                    val file = CsvExportHelper.exportQuickCsv(appContext, allTxns, start, end, label)
                                    withContext(Dispatchers.Main) {
                                        isExporting = false
                                        CsvExportHelper.shareCsv(appContext, file, "AutoBookkeeper 本周账目")
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isExporting = false
                                        Toast.makeText(appContext, "导出失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isExporting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.DateRange, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("导出本周")
                    }
                }
            }

            // ── 重复记录处理 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("重复记录处理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("查看并处理疑似重复的交易记录，避免重复记账",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onNavigateToDuplicateMerge,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(Icons.Default.MergeType, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("查看重复记录")
                    }
                }
            }

            // ── 备份 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("数据备份", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("支持本地备份、云端备份和加密备份", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 本地备份
                    Button(
                        onClick = {
                            isBackingUp = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val file = BackupHelper.createBackup(appContext, db)
                                    withContext(Dispatchers.Main) {
                                        isBackingUp = false
                                        lastBackupInfo = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                        BackupHelper.shareBackup(appContext, file)
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isBackingUp = false
                                        Toast.makeText(appContext, "备份失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isBackingUp,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isBackingUp) {
                            Text("备份中...", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.Save, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("本地备份")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 云备份
                    OutlinedButton(
                        onClick = {
                            isBackingUp = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val manager = com.autobookkeeper.backup.CloudBackupManager(appContext)
                                    val result = manager.performBackup(
                                        encrypt = false,
                                        uploadToCloud = true,
                                        cloudProvider = com.autobookkeeper.backup.CloudBackupManager.PROVIDER_GOOGLE_DRIVE
                                    )
                                    withContext(Dispatchers.Main) {
                                        isBackingUp = false
                                        when (result) {
                                            is com.autobookkeeper.backup.BackupResult.Success -> {
                                                lastBackupInfo = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                                Toast.makeText(appContext, "备份文件已生成，请选择云存储", Toast.LENGTH_LONG).show()
                                            }
                                            is com.autobookkeeper.backup.BackupResult.Error -> {
                                                Toast.makeText(appContext, "备份失败: ${result.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isBackingUp = false
                                        Toast.makeText(appContext, "备份失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isBackingUp,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("备份到云端")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 加密备份
                    OutlinedButton(
                        onClick = {
                            showEncryptDialog = true
                            encryptPassword = ""
                            confirmPassword = ""
                            passwordError = null
                        },
                        enabled = !isBackingUp,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Lock, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("加密备份")
                    }
                    
                    // 加密备份密码对话框
                    if (showEncryptDialog) {
                        AlertDialog(
                            onDismissRequest = { 
                                if (!isBackingUp) showEncryptDialog = false 
                            },
                            title = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("设置备份密码")
                                }
                            },
                            text = {
                                Column {
                                    Text(
                                        "请设置加密密码（至少6位），密码用于保护您的备份数据",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    OutlinedTextField(
                                        value = encryptPassword,
                                        onValueChange = { 
                                            encryptPassword = it
                                            passwordError = null
                                        },
                                        label = { Text("密码") },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth(),
                                        isError = passwordError != null,
                                        leadingIcon = { Icon(Icons.Default.Password, null) }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    OutlinedTextField(
                                        value = confirmPassword,
                                        onValueChange = { 
                                            confirmPassword = it
                                            passwordError = null
                                        },
                                        label = { Text("确认密码") },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        modifier = Modifier.fillMaxWidth(),
                                        isError = passwordError != null,
                                        leadingIcon = { Icon(Icons.Default.Password, null) }
                                    )
                                    
                                    passwordError?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            it,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        // 验证密码
                                        when {
                                            encryptPassword.length < 6 -> {
                                                passwordError = "密码至少需要6位"
                                                return@Button
                                            }
                                            encryptPassword != confirmPassword -> {
                                                passwordError = "两次输入的密码不一致"
                                                return@Button
                                            }
                                        }
                                        
                                        showEncryptDialog = false
                                        isBackingUp = true
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val manager = com.autobookkeeper.backup.CloudBackupManager(appContext)
                                                val result = manager.performBackup(
                                                    encrypt = true,
                                                    password = encryptPassword,
                                                    uploadToCloud = false
                                                )
                                                withContext(Dispatchers.Main) {
                                                    isBackingUp = false
                                                    when (result) {
                                                        is com.autobookkeeper.backup.BackupResult.Success -> {
                                                            lastBackupInfo = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                                                            com.autobookkeeper.backup.BackupHelper.shareBackup(appContext, result.file)
                                                            Toast.makeText(appContext, "加密备份已生成", Toast.LENGTH_LONG).show()
                                                        }
                                                        is com.autobookkeeper.backup.BackupResult.Error -> {
                                                            Toast.makeText(appContext, "备份失败: ${result.message}", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    isBackingUp = false
                                                    Toast.makeText(appContext, "备份失败: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isBackingUp
                                ) {
                                    Text("确认")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { if (!isBackingUp) showEncryptDialog = false }
                                ) {
                                    Text("取消")
                                }
                            }
                        )
                    }

                    if (lastBackupInfo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("上次备份: $lastBackupInfo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── 恢复 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("数据恢复", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("从备份文件恢复账目和自定义分类（合并策略，不会覆盖已有数据）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // 选择文件按钮
                    OutlinedButton(
                        onClick = {
                            // 使用通配类型，让用户可以选择任意文件
                            filePickerLauncher.launch("*/*")
                        },
                        enabled = !isRestoring,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("选择备份文件")
                    }

                    // 显示选中的文件
                    selectedRestoreUri?.let { uri ->
                        Spacer(modifier = Modifier.height(12.dp))
                        val fileName = uri.lastPathSegment ?: "未知文件"
                        Text(
                            "已选择: $fileName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 确认恢复按钮
                        Button(
                            onClick = {
                                isRestoring = true
                                scope.launch(Dispatchers.IO) {
                                    var success = false
                                    var msg = ""
                                    try {
                                        val inputStream = appContext.contentResolver.openInputStream(uri)
                                        if (inputStream == null) {
                                            msg = "无法打开文件"
                                        } else {
                                            val tempFile = File(appContext.cacheDir, "restore_temp.json")
                                            inputStream.use { input ->
                                                tempFile.outputStream().use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                            
                                            // 验证文件内容
                                            val content = tempFile.readText()
                                            if (!content.contains("\"transactions\"") || !content.contains("\"categories\"")) {
                                                tempFile.delete()
                                                msg = "不是有效的备份文件"
                                            } else {
                                                val result = BackupHelper.restoreBackup(appContext, db, tempFile)
                                                tempFile.delete()
                                                
                                                val newTxnCount = db.transactionDao().getCount()
                                                val newCatCount = db.categoryDao().getCount()
                                                
                                                success = true
                                                msg = "恢复成功：${result.importedTransactions} 条账目，${result.importedCategories} 个分类"
                                                
                                                withContext(Dispatchers.Main) {
                                                    totalTxns = newTxnCount
                                                    totalCategories = newCatCount
                                                    restoreResult = result
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        msg = "恢复失败: ${e.message}"
                                        e.printStackTrace()
                                    }
                                    
                                    withContext(Dispatchers.Main) {
                                        isRestoring = false
                                        if (success) {
                                            selectedRestoreUri = null
                                        }
                                        Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isRestoring,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            if (isRestoring) {
                                Text("恢复中...", style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Icon(Icons.Default.CloudDownload, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("确认恢复")
                        }
                    }

                    // 显示恢复结果
                    restoreResult?.let { result ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("恢复结果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("✅ 导入账目: ${result.importedTransactions} 条")
                                Text("✅ 导入分类: ${result.importedCategories} 个")
                                if (result.skipped > 0) {
                                    Text("⏭️ 跳过重复: ${result.skipped} 条")
                                }
                            }
                        }
                    }
                }
            }

            // ════ 账单导入 ── 微信/支付宝/京东/抖音 CSV ════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileUpload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("导入外部账单", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("支持微信、支付宝、京东、抖音导出的 CSV 账单，自动识别分类和来源",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 平台提示图标 - 使用本地LOGO图片
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BrandLogoItem(drawableName = "ic_logo_wechat", label = "微信")
                        BrandLogoItem(drawableName = "ic_logo_alipay", label = "支付宝")
                        BrandLogoItem(drawableName = "ic_logo_jingdong", label = "京东")
                        BrandLogoItem(drawableName = "ic_logo_douyin", label = "抖音")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            importResult = null
                            billPickerLauncher.launch("*/*")
                        },
                        enabled = !isImporting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        if (isImporting) {
                            Text("导入中...", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.FileUpload, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("选择账单文件 (CSV/XLSX)")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 抖音PDF专用按钮
                    OutlinedButton(
                        onClick = {
                            importResult = null
                            pdfImportResult = null
                            douyinPdfPickerLauncher.launch("application/pdf")
                        },
                        enabled = !isImporting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isImporting) {
                            Text("导入中...", style = MaterialTheme.typography.labelSmall)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.Default.PictureAsPdf, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("导入抖音PDF账单")
                    }

                    // 显示抖音PDF导入结果
                    pdfImportResult?.let { result ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎵", style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.width(4.dp))
                                    Text("抖音账单导入", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                if (result.imported == 0 && result.errors > 0) {
                                    Text("❌ 导入失败", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    if (result.errorMessages.isNotEmpty()) {
                                        result.errorMessages.take(3).forEach { msg ->
                                            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                } else {
                                    Text("✅ 成功导入: ${result.imported} 条")
                                    if (result.skipped > 0) {
                                        Text("⏭️ 跳过重复: ${result.skipped} 条")
                                    }
                                    if (result.errors > 0) {
                                        Text("⚠️ 解析失败: ${result.errors} 行", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // 显示CSV/XLSX导入结果
                    importResult?.let { result ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("导入结果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))

                                // 导入失败时优先显示错误信息
                                if (result.imported == 0 && result.errors > 0) {
                                    Text("❌ 导入失败", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    if (result.errorMessages.isNotEmpty()) {
                                        result.errorMessages.take(3).forEach { msg ->
                                            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    } else {
                                        Text("未找到有效交易记录，请检查文件格式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                    }
                                } else {
                                    Text("✅ 成功导入: ${result.imported} 条")
                                    if (result.totalLines > 0) {
                                        Text("📊 总行数: ${result.totalLines}")
                                    }
                                    if (result.skipped > 0) {
                                        Text("⏭️ 跳过重复: ${result.skipped} 条")
                                    }
                                    if (result.errors > 0) {
                                        Text("⚠️ 解析失败: ${result.errors} 行", color = MaterialTheme.colorScheme.error)
                                    }
                                    if (result.sourceCounts.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("来源分布：")
                                        for ((src, count) in result.sourceCounts.entries.sortedByDescending { it.value }) {
                                            Text("   ${src.label}: $count 条", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    if (result.errorMessages.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        result.errorMessages.take(3).forEach { msg ->
                                            Text(msg, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF6B6B))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 警告 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("注意", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("请定期导出数据，备份文件仅保存在本机，换机前请务必备份", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandLogoItem(drawableName: String, label: String) {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (resId != 0) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = label,
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(label.first().uppercase(), style = MaterialTheme.typography.titleSmall)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
    }
}
