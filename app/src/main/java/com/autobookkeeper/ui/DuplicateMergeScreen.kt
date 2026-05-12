package com.autobookkeeper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobookkeeper.data.Transaction
import com.autobookkeeper.viewmodel.TransactionViewModel
import com.autobookkeeper.util.DuplicateFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * 重复交易合并视图
 * 展示疑似重复的交易记录，允许用户选择保留哪条或合并
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateMergeScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    
    var duplicateGroups by remember { mutableStateOf<List<DuplicateGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedGroup by remember { mutableStateOf<DuplicateGroup?>(null) }
    var processedCount by remember { mutableIntStateOf(0) }
    
    // 加载疑似重复记录
    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val groups = loadDuplicateGroups(viewModel)
            duplicateGroups = groups
        }
        isLoading = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("重复记录处理", fontWeight = FontWeight.Bold)
                        if (duplicateGroups.isNotEmpty()) {
                            Text(
                                "${processedCount}/${duplicateGroups.size} 组已处理",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (duplicateGroups.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    // 一键保留所有新记录
                                    withContext(Dispatchers.IO) {
                                        duplicateGroups.forEach { group ->
                                            if (group.decision == null) {
                                                viewModel.updateTransaction(
                                                    group.newRecord.copy(isDuplicate = false, duplicateOf = null)
                                                )
                                            }
                                        }
                                    }
                                    processedCount = duplicateGroups.size
                                }
                            }
                        ) {
                            Text("全部保留")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                duplicateGroups.isEmpty() -> {
                    EmptyDuplicateView(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                selectedGroup != null -> {
                    DuplicateDetailView(
                        group = selectedGroup!!,
                        onDecision = { decision ->
                            scope.launch {
                                handleDecision(viewModel, selectedGroup!!, decision)
                                selectedGroup = null
                                processedCount++
                                // 刷新列表
                                duplicateGroups = duplicateGroups.map { 
                                    if (it.id == selectedGroup?.id) it.copy(decision = decision) else it 
                                }.filter { it.decision == null }
                            }
                        },
                        onBack = { selectedGroup = null }
                    )
                }
                else -> {
                    DuplicateGroupList(
                        groups = duplicateGroups.filter { it.decision == null },
                        onGroupClick = { selectedGroup = it }
                    )
                }
            }
        }
    }
}

/**
 * 空状态视图
 */
@Composable
private fun EmptyDuplicateView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "未发现重复记录",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "系统已自动去重，暂无需要手动处理的重复交易",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 重复记录组列表
 */
@Composable
private fun DuplicateGroupList(
    groups: List<DuplicateGroup>,
    onGroupClick: (DuplicateGroup) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "发现 ${groups.size} 组疑似重复记录，点击查看详情并决定保留哪条",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        items(groups, key = { it.id }) { group ->
            DuplicateGroupCard(
                group = group,
                onClick = { onGroupClick(group) }
            )
        }
    }
}

/**
 * 重复记录组卡片
 */
@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (group.confidence) {
                in 0.8..1.0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                in 0.5..0.8 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 顶部：金额和置信度
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatAmount(group.newRecord.amount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (group.newRecord.amount < 0) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
                
                ConfidenceBadge(confidence = group.confidence)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 新记录摘要
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FiberNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${group.newRecord.note.take(20)}${if (group.newRecord.note.length > 20) "..." else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
            
            // 原记录摘要
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${group.existingRecord.note.take(20)}${if (group.existingRecord.note.length > 20) "..." else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 时间差
            val timeDiff = kotlin.math.abs(group.newRecord.date - group.existingRecord.date)
            Text(
                text = "时间差: ${formatTimeDiff(timeDiff)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 置信度徽章
 */
@Composable
private fun ConfidenceBadge(confidence: Double) {
    val (text, color) = when {
        confidence >= 0.9 -> "极高" to MaterialTheme.colorScheme.error
        confidence >= 0.8 -> "高" to MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
        confidence >= 0.6 -> "中" to MaterialTheme.colorScheme.tertiary
        else -> "低" to MaterialTheme.colorScheme.outline
    }
    
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = "匹配度: $text",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

/**
 * 重复记录详情对比视图
 */
@Composable
private fun DuplicateDetailView(
    group: DuplicateGroup,
    onDecision: (DuplicateDecision) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 返回按钮
        TextButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("返回列表")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 标题
        Text(
            "对比详情",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        ConfidenceBadge(confidence = group.confidence)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 并排对比卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 新记录卡片
            TransactionCompareCard(
                transaction = group.newRecord,
                title = "新记录",
                icon = Icons.Default.FiberNew,
                iconColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            
            // 原记录卡片
            TransactionCompareCard(
                transaction = group.existingRecord,
                title = "已存在",
                icon = Icons.Default.History,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 决策按钮
        Text(
            "选择保留哪条记录：",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 保留新记录
        Button(
            onClick = { onDecision(DuplicateDecision.KEEP_NEW) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.FiberNew, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("保留新记录（替换旧记录）")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 保留原记录
        OutlinedButton(
            onClick = { onDecision(DuplicateDecision.KEEP_EXISTING) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.History, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("保留原记录（删除新记录）")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 保留两条
        OutlinedButton(
            onClick = { onDecision(DuplicateDecision.KEEP_BOTH) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("保留两条（不是重复）")
        }
    }
}

/**
 * 交易对比卡片
 */
@Composable
private fun TransactionCompareCard(
    transaction: Transaction,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 金额
            Text(
                text = formatAmount(transaction.amount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (transaction.amount < 0) 
                    MaterialTheme.colorScheme.error 
                else 
                    MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 详细信息
            DetailRow("商户", transaction.note)
            DetailRow("分类", "${transaction.categoryIcon} ${transaction.categoryName}")
            DetailRow("来源", transaction.source.label)
            DetailRow("时间", formatDateTime(transaction.date))
            transaction.orderNo?.let {
                DetailRow("订单号", it.take(20) + if (it.length > 20) "..." else "")
            }
        }
    }
}

/**
 * 详情行
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2
        )
    }
}

// ═══════════════════════════════════════════════
// 数据类和工具函数
// ═══════════════════════════════════════════════

data class DuplicateGroup(
    val id: String,
    val newRecord: Transaction,
    val existingRecord: Transaction,
    val confidence: Double,
    val reason: String,
    val decision: DuplicateDecision? = null
)

enum class DuplicateDecision {
    KEEP_NEW,       // 保留新记录，删除原记录
    KEEP_EXISTING,  // 保留原记录，删除新记录
    KEEP_BOTH       // 保留两条（不是重复）
}

/**
 * 加载疑似重复记录组
 */
private suspend fun loadDuplicateGroups(viewModel: TransactionViewModel): List<DuplicateGroup> {
    val dao = viewModel.dao
    val allTransactions = dao.getAllSync()
    
    // 找出被标记为重复的记录
    val suspectedDuplicates = allTransactions.filter { it.isDuplicate && it.duplicateOf != null }
    
    val groups = mutableListOf<DuplicateGroup>()
    
    for (newRecord in suspectedDuplicates) {
        val existingRecord = allTransactions.find { it.id == newRecord.duplicateOf }
        if (existingRecord != null) {
            // 计算置信度
            val result = DuplicateFilter.checkDuplicate(dao, newRecord)
            groups.add(
                DuplicateGroup(
                    id = "${newRecord.id}_${existingRecord.id}",
                    newRecord = newRecord,
                    existingRecord = existingRecord,
                    confidence = result.confidence,
                    reason = result.reason
                )
            )
        }
    }
    
    return groups.sortedByDescending { it.confidence }
}

/**
 * 处理用户决策
 */
private suspend fun handleDecision(
    viewModel: TransactionViewModel,
    group: DuplicateGroup,
    decision: DuplicateDecision
) {
    when (decision) {
        DuplicateDecision.KEEP_NEW -> {
            // 保留新记录，删除原记录
            viewModel.deleteTransaction(group.existingRecord)
            viewModel.updateTransaction(
                group.newRecord.copy(isDuplicate = false, duplicateOf = null)
            )
        }
        DuplicateDecision.KEEP_EXISTING -> {
            // 保留原记录，删除新记录
            viewModel.deleteTransaction(group.newRecord)
        }
        DuplicateDecision.KEEP_BOTH -> {
            // 保留两条，取消重复标记
            viewModel.updateTransaction(
                group.newRecord.copy(isDuplicate = false, duplicateOf = null)
            )
        }
    }
}

/**
 * 格式化金额
 */
private fun formatAmount(amount: Double): String {
    return if (amount < 0) {
        "-¥${String.format("%.2f", kotlin.math.abs(amount))}"
    } else {
        "+¥${String.format("%.2f", amount)}"
    }
}

/**
 * 格式化日期时间
 */
private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * 格式化时间差
 */
private fun formatTimeDiff(diffMs: Long): String {
    val diffSec = diffMs / 1000
    return when {
        diffSec < 60 -> "${diffSec}秒"
        diffSec < 3600 -> "${diffSec / 60}分钟"
        diffSec < 86400 -> "${diffSec / 3600}小时"
        else -> "${diffSec / 86400}天"
    }
}
