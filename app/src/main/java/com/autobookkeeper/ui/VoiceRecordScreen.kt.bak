package com.autobookkeeper.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobookkeeper.data.Source
import com.autobookkeeper.data.Transaction
import com.autobookkeeper.viewmodel.TransactionViewModel
import com.autobookkeeper.ai.VoiceTransactionParser
import kotlinx.coroutines.launch
import java.util.*

// 解析结果数据类
data class ParsedTransaction(
    val amount: Double,
    val categoryName: String,
    val categoryIcon: String,
    val merchantRaw: String,
    val isExpense: Boolean
)

// 全局回调接口
interface VoiceRecognitionCallback {
    fun onResult(text: String)
    fun onError(message: String)
}

// 全局回调存储
object VoiceRecognitionCallbackHolder {
    var callback: VoiceRecognitionCallback? = null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecordScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit,
    onSaveComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = viewModel.dao
    
    // 检查设备是否支持语音识别
    val isSpeechRecognitionAvailable = remember {
        context.packageManager.queryIntentActivities(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
            PackageManager.MATCH_DEFAULT_ONLY
        ).isNotEmpty()
    }
    
    // 本地状态
    var isListening by remember { mutableStateOf(false) }
    var localRecognizedText by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // 解析结果
    var parsedTransaction by remember { mutableStateOf<ParsedTransaction?>(null) }
    var isParsing by remember { mutableStateOf(false) }
    
    // 保存状态
    var isSaving by remember { mutableStateOf(false) }
    
    // 注册回调
    DisposableEffect(Unit) {
        VoiceRecognitionCallbackHolder.callback = object : VoiceRecognitionCallback {
            override fun onResult(text: String) {
                isListening = false
                localRecognizedText = text
                hasError = false
                // 自动解析
                scope.launch {
                    isParsing = true
                    parsedTransaction = parseVoiceText(text)
                    isParsing = false
                }
            }
            
            override fun onError(message: String) {
                isListening = false
                hasError = true
                errorMessage = message
            }
        }
        
        onDispose {
            VoiceRecognitionCallbackHolder.callback = null
        }
    }
    
    // 启动语音识别
    fun startListening() {
        if (!isSpeechRecognitionAvailable) {
            Toast.makeText(context, "设备不支持语音识别功能", Toast.LENGTH_LONG).show()
            return
        }
        
        isListening = true
        hasError = false
        
        // 发送广播启动语音识别
        val intent = Intent("com.autobookkeeper.START_VOICE_RECOGNITION")
        context.sendBroadcast(intent)
    }
    
    // 保存交易
    fun saveTransaction() {
        val pt = parsedTransaction ?: return
        isSaving = true
        scope.launch {
            try {
                // 金额：支出为负，收入为正
                val finalAmount = if (pt.isExpense) -pt.amount else pt.amount
                
                val transaction = Transaction(
                    amount = finalAmount,
                    categoryName = pt.categoryName,
                    categoryIcon = pt.categoryIcon,
                    source = Source.MANUAL,
                    merchantRaw = pt.merchantRaw,
                    date = System.currentTimeMillis()
                )
                
                dao.insert(transaction)
                onSaveComplete()
            } catch (e: Exception) {
                hasError = true
                errorMessage = "保存失败：" + e.message
            } finally {
                isSaving = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音记账") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 设备不支持语音识别的提示
            if (!isSpeechRecognitionAvailable) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.MicOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "设备不支持语音识别",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "您的设备未安装语音识别服务，无法使用语音记账功能",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("返回")
                        }
                    }
                }
                return@Column
            }
            
            // 语音输入主界面
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isListening) "正在聆听..." else "点击下方按钮说话",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // 麦克风按钮 - 带脉冲动画
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 脉冲动画
                        if (isListening) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.4f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "pulseScale"
                            )
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "pulseAlpha"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha)
                                    )
                            )
                        }
                        
                        // 主按钮
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isListening) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primaryContainer
                                )
                                .clickable(enabled = !isListening) { startListening() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                                contentDescription = if (isListening) "正在聆听" else "开始语音输入",
                                modifier = Modifier.size(48.dp),
                                tint = if (isListening) Color.White 
                                       else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 提示文字
                    Text(
                        text = "• 说出记账内容，如：午餐花了35块\n• 或：今天收入500元工资",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // 错误提示
            if (hasError) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // 识别结果
            if (localRecognizedText.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "识别结果",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            localRecognizedText,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            
            // 解析结果显示
            parsedTransaction?.let { pt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "解析结果",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                pt.categoryIcon,
                                fontSize = 32.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "${pt.categoryName} ${if (pt.isExpense) "支出" else "收入"}",
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "¥${pt.amount}",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pt.isExpense) MaterialTheme.colorScheme.error 
                                            else MaterialTheme.colorScheme.primary
                                )
                                if (pt.merchantRaw.isNotEmpty() && pt.merchantRaw != pt.categoryName) {
                                    Text(
                                        pt.merchantRaw,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row {
                            OutlinedButton(
                                onClick = {
                                    localRecognizedText = ""
                                    parsedTransaction = null
                                }
                            ) {
                                Text("重新输入")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { saveTransaction() },
                                enabled = !isSaving
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("保存")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 解析语音文本 - 使用增强版自然语言理解
private fun parseVoiceText(text: String): ParsedTransaction {
    // 使用增强版解析器
    val result = VoiceTransactionParser.parse(text)
    
    // 转换为 ParsedTransaction
    val isExpense = result.transactionType == VoiceTransactionParser.TransactionType.EXPENSE ||
                    result.transactionType == VoiceTransactionParser.TransactionType.UNKNOWN
    
    // 根据分类获取图标
    val categoryIcon = getCategoryIcon(result.category, isExpense)
    
    return ParsedTransaction(
        amount = result.amount ?: 0.0,
        categoryName = result.category,
        categoryIcon = categoryIcon,
        merchantRaw = result.merchant,
        isExpense = isExpense
    )
}

// 根据分类获取图标
private fun getCategoryIcon(category: String, isExpense: Boolean): String {
    val expenseIcons = mapOf(
        "餐饮" to "🍜",
        "交通" to "🚗",
        "购物" to "🛍️",
        "娱乐" to "🎬",
        "住房" to "🏠",
        "医疗" to "🏥",
        "教育" to "📚",
        "通讯" to "📱",
        "人情" to "🎁",
        "宠物" to "🐱",
        "金融" to "💳",
        "其他" to "📌"
    )
    
    val incomeIcons = mapOf(
        "收入" to "💰",
        "工资" to "💵",
        "奖金" to "🎁",
        "投资" to "📈",
        "其他收入" to "💵"
    )
    
    return if (isExpense) {
        expenseIcons[category] ?: "📌"
    } else {
        incomeIcons[category] ?: "💰"
    }
}
