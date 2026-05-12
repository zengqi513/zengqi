package com.autobookkeeper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autobookkeeper.data.*
import com.autobookkeeper.viewmodel.AnalysisViewModel
import com.autobookkeeper.viewmodel.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel = viewModel(),
    onBackClick: (() -> Unit)? = null
) {
    val analysisData by viewModel.monthlyAnalysisData.collectAsState()
    val aiResult by viewModel.aiAnalysisResult.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentYear by viewModel.currentYear.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(chatMessages.size) {
        if (chatMessages.isNotEmpty()) listState.animateScrollToItem(chatMessages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 月份导航（顶部，同色，无返回按钮）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 48.dp, end = 8.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "AI 分析",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "上月")
                }
                Text(
                    "${currentYear}年${currentMonth}月",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "下月")
                }
            }
        }

        if (isLoading && analysisData == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (analysisData != null) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // AI 总结卡片
                item {
                    AISummaryCard(aiResult?.summary ?: "正在分析中...")
                }

                // 关键指标
                item {
                    KeyMetricsRow(analysisData!!)
                }

                // AI 洞察
                aiResult?.insights?.takeIf { it.isNotEmpty() }?.let { insights ->
                    item {
                        InsightCard(insights)
                    }
                }

                // 建议
                aiResult?.suggestions?.takeIf { it.isNotEmpty() }?.let { suggestions ->
                    item {
                        SuggestionSection(suggestions)
                    }
                }

                // 异常提醒
                analysisData!!.anomalies.takeIf { it.isNotEmpty() }?.let { anomalies ->
                    item {
                        AnomalySection(anomalies)
                    }
                }

                // 积极亮点
                aiResult?.positiveHighlights?.takeIf { it.isNotEmpty() }?.let { positives ->
                    item {
                        PositiveHighlightsCard(positives)
                    }
                }

                // 聊天区域
                item {
                    Text(
                        "智能问答",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(chatMessages) { msg ->
                    ChatBubble(msg)
                }

                // 快捷问题
                if (chatMessages.isEmpty()) {
                    item {
                        QuickQuestionsRow { query ->
                            viewModel.processQuery(query)
                        }
                    }
                }
            }

            // 输入框
            ChatInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.processQuery(inputText)
                        inputText = ""
                    }
                }
            )
        }
    }
}

@Composable
fun AISummaryCard(summary: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("AI 总结", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun KeyMetricsRow(data: MonthlyAnalysisData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val mod = Modifier.weight(1f)
        MetricCard(
            title = "支出",
            value = "${String.format("%.0f", data.totalExpense)}",
            unit = "元",
            icon = Icons.Default.TrendingDown,
            color = MaterialTheme.colorScheme.error,
            modifier = mod
        )
        MetricCard(
            title = "收入",
            value = "${String.format("%.0f", data.totalIncome)}",
            unit = "元",
            icon = Icons.Default.TrendingUp,
            color = MaterialTheme.colorScheme.primary,
            modifier = mod
        )
        MetricCard(
            title = "结余",
            value = "${String.format("%.0f", data.totalIncome - data.totalExpense)}",
            unit = "元",
            icon = Icons.Default.AccountBalance,
            color = if (data.totalIncome >= data.totalExpense) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
            modifier = mod
        )
    }
}

@Composable
fun MetricCard(title: String, value: String, unit: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun InsightCard(insights: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("关键洞察", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            insights.forEach { insight ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("• ", color = MaterialTheme.colorScheme.primary)
                    Text(insight, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun SuggestionSection(suggestions: List<SuggestionItem>) {
    Column {
        Text("💡 建议", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        suggestions.forEach { s ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(s.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(s.description, style = MaterialTheme.typography.bodySmall)
                    s.potentialSavings?.let {
                        Text("预计节省 ${String.format("%.0f", it)} 元",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun AnomalySection(anomalies: List<AnomalyItem>) {
    Column {
        Text("⚠️ 提醒", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        anomalies.forEach { a ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (a.severity) {
                        AnomalySeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                        AnomalySeverity.WARNING -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (a.severity) {
                            AnomalySeverity.CRITICAL -> Icons.Default.Error
                            AnomalySeverity.WARNING -> Icons.Default.Warning
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when (a.severity) {
                            AnomalySeverity.CRITICAL -> MaterialTheme.colorScheme.error
                            AnomalySeverity.WARNING -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(a.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun PositiveHighlightsCard(positives: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("做得好的地方", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            positives.forEach { p ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("✓ ", color = MaterialTheme.colorScheme.primary)
                    Text(p, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            )
        ) {
            Text(
                msg.content,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun QuickQuestionsRow(onClick: (String) -> Unit) {
    Column {
        Text("试试这样问：", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickQuestionChip("花了多少？", onClick)
            QuickQuestionChip("收入呢？", onClick)
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickQuestionChip("和上月比？", onClick)
            QuickQuestionChip("有什么建议？", onClick)
        }
    }
}

@Composable
fun QuickQuestionChip(text: String, onClick: (String) -> Unit) {
    AssistChip(
        onClick = { onClick(text) },
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        modifier = Modifier.height(32.dp)
    )
}

@Composable
fun ChatInputBar(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入问题...", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() })
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "发送")
            }
        }
    }
}
