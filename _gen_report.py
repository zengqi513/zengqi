#!/usr/bin/env python3
"""Write clean ReportScreen.kt - no full rewrite, just targeted component updates"""
path = r'C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\ui\ReportScreen.kt'

code = '''package com.autobookkeeper.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autobookkeeper.data.CategoryData
import com.autobookkeeper.data.CategoryStat
import com.autobookkeeper.data.Transaction
import com.autobookkeeper.viewmodel.CompareMode
import com.autobookkeeper.viewmodel.ComparePeriodType
import com.autobookkeeper.viewmodel.ComparisonResult
import com.autobookkeeper.viewmodel.DailyPoint
import com.autobookkeeper.viewmodel.IncomeDistribution
import com.autobookkeeper.viewmodel.PeriodSummary
import com.autobookkeeper.viewmodel.ReportPeriod
import com.autobookkeeper.viewmodel.ReportType
import com.autobookkeeper.viewmodel.TransactionViewModel
import com.autobookkeeper.viewmodel.YearSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 报表页面 — 优化版（参考设计稿）
 *  1. 扇形图 + 右侧图例
 *  2. 三级下钻：一级分类 → 子分类 → 商家
 *  3. 数据对比优化
 *  4. 趋势图网格线
 *  5. 日历热力图
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val periodSummary by viewModel.periodSummary.collectAsState()
    val yearSummary by viewModel.yearSummary.collectAsState()
    val reportPeriod by viewModel.reportPeriod.collectAsState()
    val incomeDistribution by viewModel.incomeDistribution.collectAsState()
    val comparisonResult by viewModel.comparisonResult.collectAsState()
    val selectedCategory by viewModel.selectedReportCategory.collectAsState()
    val reportType by viewModel.reportType.collectAsState()
    val rawTransactions by viewModel.transactions.collectAsState(listOf())
    val allCats by viewModel.allCategories.collectAsState()
    val iconMap by viewModel.categoryIconMap.collectAsState()

    var customPickerStep by remember { mutableStateOf(0) }
    var customStart by remember { mutableStateOf<Long?>(null) }

    // 下钻链路: null=一级饼图, Pair(parent,null)=子分类, Pair(parent,sub)=商家明细
    var drillDown by remember { mutableStateOf<Pair<String, String?>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("报表", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.forceRefresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 收支切换 Tab ──
            ReportTypeTabs(currentType = reportType, onTypeChange = { viewModel.setReportType(it) })

            // ── 周期导航 ──
            PeriodNavBar(
                currentPeriod = reportPeriod,
                periodLabel = periodSummary.periodLabel,
                onPeriodChange = { viewModel.setReportPeriod(it) },
                onPrev = { viewModel.prevPeriod() },
                onNext = { viewModel.nextPeriod() },
                onCustomRange = { customPickerStep = 1 }
            )

            // ── 汇总卡片 ──
            SummaryHighlightCard(
                reportType = reportType,
                summary = periodSummary,
                transactions = rawTransactions,
                iconMap = iconMap
            )

            // ── 日均 ──
            if (periodSummary.dailyAvg.avgDailyExpense > 0 || periodSummary.dailyAvg.avgDailyIncome > 0) {
                DailyCard(dailyAvg = periodSummary.dailyAvg, reportType = reportType)
            }

            // ── 收支走势 ──
            if (periodSummary.dailyPoints.isNotEmpty()) {
                TrendChartCard(dailyPoints = periodSummary.dailyPoints)
            }

            // ── 日历热力图 ──
            if (periodSummary.dailyPoints.isNotEmpty() && reportType == ReportType.EXPENSE) {
                CalendarHeatmapCard(dailyPoints = periodSummary.dailyPoints)
            }

            // ── 分类分布（扇形图 + 图例 + 下钻） ──
            if (incomeDistribution.categoryBreakdown.isNotEmpty()) {
                val dd = drillDown

                if (dd == null) {
                    CategoryRingCard(
                        title = if (reportType == ReportType.EXPENSE) "支出分类" else "收入分类",
                        categories = incomeDistribution.categoryBreakdown,
                        totalAmount = incomeDistribution.totalIncome,
                        allCats = allCats,
                        onCategoryClick = { name ->
                            val hasSub = allCats.any { it.parentName == name }
                            if (hasSub) drillDown = name to null
                            else viewModel.selectReportCategory(name)
                        }
                    )
                } else {
                    val (parentName, subName) = dd
                    val subCats = allCats.filter {
                        it.parentName == parentName && it.type == (if (reportType == ReportType.EXPENSE) "expense" else "income")
                    }
                    val matched = incomeDistribution.categoryBreakdown.filter { c -> subCats.any { it.name == c.name } }

                    if (subName == null) {
                        SubDetailCard(
                            parentName = parentName,
                            subStats = matched,
                            totalAmount = incomeDistribution.totalIncome,
                            onBack = { drillDown = null },
                            onSubClick = { drillDown = Pair(parentName, it) }
                        )
                    } else {
                        MerchantDetailCard(
                            parentName = parentName,
                            subName = subName,
                            timeStart = periodSummary.startDate,
                            timeEnd = periodSummary.endDate,
                            allTxns = rawTransactions,
                            onBack = { drillDown = Pair(parentName, null) }
                        )
                    }
                }
            }

            // ── 选中分类 → 交易明细 ──
            selectedCategory?.let { cat ->
                TransactionDetailCard(
                    categoryName = cat,
                    startDate = periodSummary.startDate,
                    endDate = periodSummary.endDate,
                    transactions = rawTransactions,
                    onClose = { viewModel.clearSelectedReportCategory() }
                )
            }

            // ── 数据对比 ──
            ComparisonBlock(
                viewModel = viewModel,
                comparisonResult = comparisonResult,
                reportType = reportType
            )

            // ── 年度汇总 ──
            if (reportPeriod == ReportPeriod.YEAR && yearSummary != null) {
                YearSummaryCard(summary = yearSummary!!)
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    // ── 日期对话框（不变） ──
    if (customPickerStep == 1) {
        AlertDialog(
            onDismissRequest = { customPickerStep = 0; customStart = null },
            title = { Text("选择日期范围") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        val (s, e) = getReportWeekRange(); viewModel.setCustomDateRange(s, e); customPickerStep = 0
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.DateRange, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("本周")
                    }
                    Button(onClick = {
                        viewModel.setReportPeriod(ReportPeriod.MONTH); customPickerStep = 0
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CalendarMonth, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("本月")
                    }
                    OutlinedButton(onClick = { customPickerStep = 2 }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.EditCalendar, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("自定义")
                    }
                }
            }, confirmButton = {}, dismissButton = { TextButton(onClick = { customPickerStep = 0 }) { Text("取消") } }
        )
    }
    if (customPickerStep == 2) {
        val s = rememberDatePickerState()
        DatePickerDialog(onDismissRequest = { customPickerStep = 1; customStart = null },
            confirmButton = { TextButton(onClick = {
                val sel = s.selectedDateMillis ?: return@TextButton; customStart = sel; customPickerStep = 3
            }) { Text("下一步") } },
            dismissButton = { TextButton(onClick = { customPickerStep = 1; customStart = null }) { Text("返回") } }
        ) { DatePicker(state = s) }
    }
    if (customPickerStep == 3 && customStart != null) {
        val start = customStart!!
        val e = rememberDatePickerState(initialSelectedDateMillis = start + 86_400_000L)
        DatePickerDialog(onDismissRequest = { customPickerStep = 2 },
            confirmButton = { TextButton(onClick = {
                val sel = e.selectedDateMillis ?: return@TextButton
                if (sel >= start) { viewModel.setCustomDateRange(start, sel); customPickerStep = 0; customStart = null }
            }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { customPickerStep = 2 }) { Text("返回") } }
        ) { DatePicker(state = e) }
    }
}

// ════════════════════════════════════════════════════════════════
//  收支类型 Tabs
// ════════════════════════════════════════════════════════════════
@Composable
private fun ReportTypeTabs(currentType: ReportType, onTypeChange: (ReportType) -> Unit) {
    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
        Row(modifier = Modifier.padding(5.dp)) {
            listOf(ReportType.EXPENSE to "支出", ReportType.INCOME to "收入").forEach { (type, label) ->
                val sel = currentType == type
                Surface(
                    modifier = Modifier.weight(1f).clickable { onTypeChange(type) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (sel) MaterialTheme.colorScheme.primary else Color.Transparent,
                    tonalElevation = if (sel) 4.dp else 0.dp
                ) {
                    Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (type == ReportType.INCOME) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            null, Modifier.size(18.dp),
                            tint = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(label, MaterialTheme.typography.labelLarge,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  周期导航栏
// ════════════════════════════════════════════════════════════════
@Composable
private fun PeriodNavBar(
    currentPeriod: ReportPeriod, periodLabel: String,
    onPeriodChange: (ReportPeriod) -> Unit,
    onPrev: () -> Unit, onNext: () -> Unit, onCustomRange: () -> Unit
) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "上一期", modifier = Modifier.size(22.dp))
                }
                Text(periodLabel, MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "下一期", modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                listOf(ReportPeriod.WEEK to "周", ReportPeriod.MONTH to "月", ReportPeriod.YEAR to "年", ReportPeriod.CUSTOM to "自定")
                    .forEach { (p, l) ->
                        val sel = currentPeriod == p
                        Surface(
                            modifier = Modifier.weight(1f).clickable { if (p == ReportPeriod.CUSTOM) onCustomRange() else onPeriodChange(p) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            tonalElevation = if (sel) 4.dp else 0.dp
                        ) {
                            Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                                Text(l, MaterialTheme.typography.labelLarge,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  汇总高亮卡片
// ════════════════════════════════════════════════════════════════
@Composable
private fun SummaryHighlightCard(
    reportType: ReportType, summary: PeriodSummary,
    transactions: List<Transaction>, iconMap: Map<String, String>
) {
    val incC = Color(0xFF10B981); val expC = Color(0xFFEF4444)
    val mainVal = if (reportType == ReportType.INCOME) summary.income else summary.expense
    val primaryC = if (reportType == ReportType.INCOME) incC else expC
    val net = summary.income - summary.expense
    val days = ((summary.endDate - summary.startDate) / 86_400_000L + 1).coerceAtLeast(1)
    val avg = mainVal / days

    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                Column {
                    Text(if (reportType == ReportType.INCOME) "本期收入" else "本期支出",
                        MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("%.2f".format(mainVal), MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, color = primaryC)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("结余", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("%.2f".format(net), MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = if (net >= 0) incC else expC)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.2f".format(summary.income), MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = incC)
                    Text("收入", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.2f".format(summary.expense), MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = expC)
                    Text("支出", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${summary.transactionCount}", MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("笔数", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.2f".format(avg), MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("日均", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  日均小卡片
// ════════════════════════════════════════════════════════════════
@Composable
private fun DailyCard(dailyAvg: com.autobookkeeper.viewmodel.DailyAverageStats, reportType: ReportType) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%.2f".format(dailyAvg.avgDailyIncome),
                    MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981)
                )
                Text("日均收入", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "%.2f".format(dailyAvg.avgDailyExpense),
                    MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444)
                )
                Text("日均支出", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${dailyAvg.totalDays}", MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("统计天数", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  收支走势 (折线图)
// ════════════════════════════════════════════════════════════════
@Composable
private fun TrendChartCard(dailyPoints: List<DailyPoint>) {
    val incC = Color(0xFF10B981); val expC = Color(0xFFEF4444)
    val dateFmt = remember { SimpleDateFormat("M/d", Locale.getDefault()) }

    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("收支走势", MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(incC, RoundedCornerShape(2.dp))); Spacer(Modifier.width(4.dp)); Text("收入", MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(expC, RoundedCornerShape(2.dp))); Spacer(Modifier.width(4.dp)); Text("支出", MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                val w = size.width; val h = size.height; val pad = 44f
                val mx = (dailyPoints.flatMap { listOf(it.income, it.expense) }.maxOrNull() ?: 1.0) * 1.15
                val uw = w - pad * 2; val uh = h - pad * 2; val n = dailyPoints.size.coerceAtLeast(2)
                val sep = uw / (n - 1).coerceAtLeast(1)

                // 网格线
                (0..3).forEach { i ->
                    val y = pad + uh * (1 - i / 3f)
                    drawLine(Color(0x1A000000), Offset(pad, y), Offset(pad + uw, y), 0.8f)
                }
                // 收入线
                val ip = Path()
                dailyPoints.forEachIndexed { i, pt ->
                    val x = pad + i * sep; val y = pad + (1 - (pt.income / mx).toFloat()) * uh
                    if (i == 0) ip.moveTo(x, y) else ip.lineTo(x, y)
                }
                drawPath(ip, incC, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
                // 支出线
                val ep = Path()
                dailyPoints.forEachIndexed { i, pt ->
                    val x = pad + i * sep; val y = pad + (1 - (pt.expense / mx).toFloat()) * uh
                    if (i == 0) ep.moveTo(x, y) else ep.lineTo(x, y)
                }
                drawPath(ep, expC, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
                // X轴标签 (每7个)
                val lp = android.graphics.Paint().apply { color = 0xFF999999; textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER }
                dailyPoints.forEachIndexed { i, pt ->
                    if (i % 7 == 0 || i == n - 1) {
                        val label = dateFmt.format(Date(pt.date))
                        drawContext.canvas.nativeCanvas.drawText(label, pad + i * sep, h - 6f, lp)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  日历热力图
// ════════════════════════════════════════════════════════════════
@Composable
private fun CalendarHeatmapCard(dailyPoints: List<DailyPoint>) {
    val dateFmt = remember { SimpleDateFormat("d", Locale.getDefault()) }

    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("消费日历", MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("低", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.size(12.dp).background(Color(0xFFFEE2E2), RoundedCornerShape(2.dp)))
                    Box(Modifier.size(12.dp).background(Color(0xFFFECACA), RoundedCornerShape(2.dp)))
                    Box(Modifier.size(12.dp).background(Color(0xFFF87171), RoundedCornerShape(2.dp)))
                    Box(Modifier.size(12.dp).background(Color(0xFFEF4444), RoundedCornerShape(2.dp)))
                    Text("高", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            val maxVal = dailyPoints.maxOfOrNull { it.expense } ?: 1.0
            val chunked = dailyPoints.chunked(7).take(5)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                chunked.forEach { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        week.forEach { day ->
                            val intensity = (day.expense / maxVal.coerceAtLeast(0.01)).coerceIn(0.0, 1.0)
                            val c = when {
                                intensity < 0.01 -> Color(0xFFF5F5F5)
                                intensity < 0.25 -> Color(0xFFFEE2E2)
                                intensity < 0.50 -> Color(0xFFFECACA)
                                intensity < 0.75 -> Color(0xFFF87171)
                                else -> Color(0xFFEF4444)
                            }
                            Box(Modifier.size(38.dp).background(c, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                                Text(dateFmt.format(Date(day.date)), MaterialTheme.typography.labelSmall,
                                    color = if (intensity > 0.6) Color.White else Color(0xFF666666))
                            }
                        }
                        repeat(7 - week.size) { Box(Modifier.size(38.dp)) }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  扇形图 + 右侧图例
// ════════════════════════════════════════════════════════════════
@Composable
private fun CategoryRingCard(
    title: String, categories: List<CategoryStat>, totalAmount: Double,
    allCats: List<CategoryData>, onCategoryClick: (String) -> Unit
) {
    val colors = listOf(
        Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF10B981),
        Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899),
        Color(0xFF06B6D4), Color(0xFF84CC16), Color(0xFFF97316),
        Color(0xFF14B8A6), Color(0xFF6366F1)
    )
    val total = categories.sumOf { it.total }.coerceAtLeast(1.0)
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(title, MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("%.2f".format(total), MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // 左侧扇形
                Box(Modifier.size(160.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        val cx = size.width / 2; val cy = size.height / 2
                        val or = minOf(cx, cy) * 0.85f
                        var sa = -90f
                        categories.forEachIndexed { i, cat ->
                            val sw = (cat.total / total * 360).toFloat().coerceAtLeast(0.3f)
                            drawArc(colors[i % colors.size], sa, sw, true,
                                Offset(cx - or, cy - or), Size(or * 2, or * 2))
                            sa += sw
                        }
                        // 中间小圆 + 总额
                        drawCircle(Color.White, or * 0.48f, Offset(cx, cy))
                        val tp = android.graphics.Paint().apply { color = android.graphics.Color.BLACK; textSize = 30f; isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER }
                        drawContext.canvas.nativeCanvas.drawText("%.0f".format(total), cx, cy + 6f, tp)
                        val lp2 = android.graphics.Paint().apply { color = 0xFF999999; textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER }
                        drawContext.canvas.nativeCanvas.drawText("总额", cx, cy + 28f, lp2)
                    }
                }
                Spacer(Modifier.width(14.dp))
                // 右侧图例
                Column(Modifier.weight(1f).heightIn(max = 160.dp), Arrangement.spacedBy(2.dp)) {
                    categories.take(9).forEachIndexed { i, cat ->
                        val pct = (cat.total / total * 100).toInt()
                        val hasSub = allCats.any { it.parentName == cat.name }
                        Row(Modifier.fillMaxWidth().clickable { onCategoryClick(cat.name) }.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(colors[i % colors.size], CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(cat.name, MaterialTheme.typography.labelSmall, modifier = Modifier.width(40.dp),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.width(4.dp))
                            Text("${pct}%", MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold, color = colors[i % colors.size])
                            if (hasSub) { Spacer(Modifier.width(2.dp)); Text("›", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                    if (categories.size > 9) Text("+${categories.size - 9} 项", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  子分类明细
// ════════════════════════════════════════════════════════════════
@Composable
private fun SubDetailCard(
    parentName: String, subStats: List<CategoryStat>, totalAmount: Double,
    onBack: () -> Unit, onSubClick: (String) -> Unit
) {
    val colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4), Color(0xFF84CC16), Color(0xFFF97316), Color(0xFF6366F1))
    val total = subStats.sumOf { it.total }.coerceAtLeast(1.0)
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", modifier = Modifier.size(20.dp)) }
                Text("$parentName 明细", MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("%.2f".format(totalAmount), MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            if (subStats.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { Text("暂无子分类数据", MaterialTheme.typography.bodyMedium) }
            } else {
                val mx = subStats.maxOfOrNull { it.total } ?: 1.0
                subStats.forEachIndexed { i, s ->
                    val pct = s.total / total * 100
                    Column(Modifier.fillMaxWidth().clickable { onSubClick(s.name) }.padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(10.dp).background(colors[i % colors.size], CircleShape)); Spacer(Modifier.width(6.dp)); Text(s.name, MaterialTheme.typography.bodySmall)
                            }
                            Row { Text("%.0f".format(s.total), MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium); Spacer(Modifier.width(6.dp)); Text("%.0f%%".format(pct), MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))) {
                            Box(Modifier.fillMaxWidth((s.total / mx).toFloat().coerceIn(0f, 1f)).height(8.dp).background(colors[i % colors.size], RoundedCornerShape(4.dp)))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text("点击分类查看商家明细 ›", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  商家明细 (按备注分组)
// ════════════════════════════════════════════════════════════════
@Composable
private fun MerchantDetailCard(
    parentName: String, subName: String, timeStart: Long, timeEnd: Long,
    allTxns: List<Transaction>, onBack: () -> Unit
) {
    val filtered = allTxns.filter { it.categoryName == subName && it.date in timeStart..timeEnd }
    val grouped = filtered.groupBy { t -> t.note.ifBlank { "(未备注)" } }
        .mapValues { (_, list) -> list.sumOf { kotlin.math.abs(it.amount) } }
        .entries.sortedByDescending { it.value }
    val total = grouped.sumOf { it.value }
    val colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4))

    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", modifier = Modifier.size(20.dp)) }
                Column {
                    Text("$parentName · $subName", MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("商家/备注明细", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("共 ${filtered.size} 笔 · 合计 %.2f".format(total), MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))

            if (grouped.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) { Text("暂无数据", MaterialTheme.typography.bodyMedium) }
            } else {
                grouped.take(15).forEachIndexed { i, (name, amount) ->
                    val pct = if (total > 0) amount / total * 100 else 0.0
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(colors[i % colors.size], CircleShape)); Spacer(Modifier.width(8.dp))
                            Column {
                                Text(name, MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("%.0f%%".format(pct), MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("%.2f".format(amount), MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(2.dp))
                }
                if (grouped.size > 15) Text("+${grouped.size - 15} 项...", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  交易明细列表
// ════════════════════════════════════════════════════════════════
@Composable
private fun TransactionDetailCard(
    categoryName: String, startDate: Long, endDate: Long,
    transactions: List<Transaction>, onClose: () -> Unit
) {
    val filtered = transactions.filter { it.categoryName == categoryName && it.date in startDate..endDate }.sortedByDescending { it.date }
    val total = filtered.sumOf { kotlin.math.abs(it.amount) }

    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("$categoryName · ${filtered.size}笔 %.2f".format(total), MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Text("暂无记录", MaterialTheme.typography.bodyMedium)
            } else {
                filtered.take(15).forEach { txn ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(txn.note.ifBlank { txn.categoryName }, MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(txn.date)), MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("%.2f".format(txn.amount), MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (txn.amount > 0) Color(0xFF10B981) else Color(0xFFEF4444))
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
                if (filtered.size > 15) Text("+${filtered.size - 15} 笔", MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  数据对比模块
// ════════════════════════════════════════════════════════════════
@Composable
private fun ComparisonBlock(
    viewModel: TransactionViewModel, comparisonResult: ComparisonResult?, reportType: ReportType
) {
    var ct by remember { mutableStateOf(ComparePeriodType.WEEK) }
    var cm by remember { mutableStateOf(CompareMode.SEQUENTIAL) }
    LaunchedEffect(ct, cm) { viewModel.triggerCompare(ct, cm) }

    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("数据对比", MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(6.dp)) {
                ComparePeriodType.values().forEach { t ->
                    val lb = when (t) { ComparePeriodType.WEEK -> "按周"; ComparePeriodType.MONTH -> "按月"; ComparePeriodType.QUARTER -> "按季"; ComparePeriodType.YEAR -> "按年" }
                    FilterChip(selected = ct == t, onClick = { ct = t }, label = { Text(lb, MaterialTheme.typography.labelSmall) }, modifier = Modifier.height(30.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = cm == CompareMode.SEQUENTIAL, onClick = { cm = CompareMode.SEQUENTIAL }, label = { Text("环比", MaterialTheme.typography.labelSmall) }, modifier = Modifier.height(30.dp))
                FilterChip(selected = cm == CompareMode.YOY, onClick = { cm = CompareMode.YOY }, label = { Text("同比", MaterialTheme.typography.labelSmall) }, modifier = Modifier.height(30.dp))
            }
        }
    }

    comparisonResult?.let { res ->
        ComparisonResultCard(res = res, reportType = reportType)
        if (res.currentPeriods.isNotEmpty() && res.previousPeriods.isNotEmpty()) {
            ComparisonBarCard(result = res, reportType = reportType)
        }
    }
}

@Composable
private fun ComparisonResultCard(res: ComparisonResult, reportType: ReportType) {
    val incC = Color(0xFF10B981); val expC = Color(0xFFEF4444)
    val rate = if (reportType == ReportType.INCOME) res.incomeChangeRate else res.expenseChangeRate
    val cur = if (reportType == ReportType.INCOME) res.currentData.income else res.currentData.expense
    val prev = if (reportType == ReportType.INCOME) res.previousData.income else res.previousData.expense

    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(res.currentLabel, MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(res.previousLabel, MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.Bottom) {
                Column {
                    Text("%.2f".format(cur), MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val up = rate >= 0
                        Icon(if (up) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown, null, Modifier.size(16.dp), tint = if (up) incC else expC)
                        Spacer(Modifier.width(2.dp))
                        Text("%.1f%%".format(rate), MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (up) incC else expC)
                    }
                }
                Text("%.2f".format(prev), MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
            }
        }
    }
}

@Composable
private fun ComparisonBarCard(result: ComparisonResult, reportType: ReportType) {
    val curC = Color(0xFF3B82F6); val prevC = Color(0xFF93C5FD)
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(if (reportType == ReportType.INCOME) "收入对比" else "支出对比", MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).background(curC, RoundedCornerShape(2.dp))); Spacer(Modifier.width(4.dp)); Text("本期", MaterialTheme.typography.labelSmall) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).background(prevC, RoundedCornerShape(2.dp))); Spacer(Modifier.width(4.dp)); Text("上期", MaterialTheme.typography.labelSmall) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                val w = size.width; val h = size.height; val pad = 46f
                val uw = w - pad * 2; val uh = h - pad * 2
                val curVals = result.currentPeriods.map { if (reportType == ReportType.INCOME) it.income else it.expense }
                val prevVals = result.previousPeriods.map { if (reportType == ReportType.INCOME) it.income else it.expense }
                val mx = ((curVals + prevVals).maxOrNull() ?: 1.0) * 1.15
                val n = curVals.size.coerceAtLeast(1)
                val slot = uw / n; val bw = slot * 0.32f; val gap = slot * 0.18f

                curVals.forEachIndexed { i, cv ->
                    val pv = prevVals.getOrElse(i) { 0.0 }
                    val bx = pad + i * slot + (slot - bw * 2 - gap) / 2
                    val ph = (pv / mx * uh).toFloat().coerceAtLeast(0f)
                    val ch = (cv / mx * uh).toFloat().coerceAtLeast(0f)
                    drawRoundRect(prevC, Offset(bx, pad + uh - ph), Size(bw, ph), CornerRadius(4f, 4f))
                    drawRoundRect(curC, Offset(bx + bw + gap, pad + uh - ch), Size(bw, ch), CornerRadius(4f, 4f))
                    val label = result.currentPeriods.getOrElse(i) { result.currentData }.label
                    val lp = android.graphics.Paint().apply { color = 0xFF888888; textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER }
                    drawContext.canvas.nativeCanvas.drawText(label.take(4).toString(), bx + bw + gap / 2, h - 6f, lp)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  年度汇总
// ════════════════════════════════════════════════════════════════
@Composable
private fun YearSummaryCard(summary: YearSummary) {
    val incC = Color(0xFF10B981); val expC = Color(0xFFEF4444)
    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("${summary.year}年度汇总", MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.2f".format(summary.totalIncome), MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = incC)
                    Text("总收入", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.2f".format(summary.totalExpense), MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = expC)
                    Text("总支出", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("%.2f".format(summary.netSavings), MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (summary.netSavings >= 0) incC else expC)
                    Text("结余", MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(10.dp))
            Canvas(Modifier.fillMaxWidth().height(100.dp)) {
                val months = summary.monthlyStats.filter { it.income > 0 || it.expense > 0 }
                if (months.isEmpty()) return@Canvas
                val mx = months.maxOfOrNull { maxOf(it.income, it.expense) } ?: 1.0
                val bw = (size.width - 40f) / months.size - 8f
                months.forEachIndexed { i, s ->
                    val x = 20f + i * ((size.width - 40f) / months.size)
                    val ih = (s.income / mx * size.height * 0.35f).toFloat()
                    val eh = (s.expense / mx * size.height * 0.35f).toFloat()
                    drawRoundRect(incC, Offset(x, size.height * 0.1f), Size(bw, ih), CornerRadius(4f, 4f))
                    drawRoundRect(expC, Offset(x, size.height * 0.1f + ih + 2f), Size(bw, eh), CornerRadius(4f, 4f))
                    drawContext.canvas.nativeCanvas.drawText("${s.month}月", x + bw / 2 - 12, size.height - 4f,
                        android.graphics.Paint().apply { color = 0xFF888888; textSize = 18f })
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  工具
// ════════════════════════════════════════════════════════════════
private fun getReportWeekRange(): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis(); set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) }
    val s = normalizeDayStart(cal.timeInMillis)
    cal.add(Calendar.DAY_OF_YEAR, 6)
    return s to normalizeDayEnd(cal.timeInMillis)
}

private fun normalizeDayStart(ts: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = ts; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    return cal.timeInMillis
}

private fun normalizeDayEnd(ts: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = ts; set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }
    return cal.timeInMillis
}
'''

with open(path, 'w', encoding='utf-8') as f:
    f.write(code)
print('OK')
