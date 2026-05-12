package com.autobookkeeper.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobookkeeper.data.CategoryData
import com.autobookkeeper.data.CategoryStat
import com.autobookkeeper.data.Transaction
import com.autobookkeeper.viewmodel.*
import com.autobookkeeper.ui.CategoryPieLegendCard
import com.autobookkeeper.ui.SubCategoryPieCard
import com.autobookkeeper.ui.CategoryMerchantCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val BlueGray = Color(0xFF607D8B)  // Kept for reference, use colorScheme in Composable
private val chartColors = listOf(
    Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF10B981),
    Color(0xFFF59E0B), Color(0xFF8B5CF6), Color(0xFFEC4899),
    Color(0xFF06B6D4), Color(0xFF84CC16), Color(0xFFF97316),
    Color(0xFF14B8A6), Color(0xFF6366F1)
)
// 使用MaterialTheme.colorScheme.primary表示上涨/收入
// 使用MaterialTheme.colorScheme.error表示下跌/支出

// ══════════════════════════════════════════════════════════════════════
//  主页面
// ══════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("收支统计", "智能分析")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("透视") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                        }
                    },
                    actions = {
                        if (selectedTab == 0) {
                            IconButton(onClick = { viewModel.forceRefresh() }) {
                                Icon(Icons.Default.Refresh, "刷新")
                            }
                        }
                    }
                )
                // Tab 栏
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (selectedTab == 0) {
            ReportContent(viewModel, padding)
        } else {
            Box(modifier = Modifier.padding(padding)) {
                AnalysisScreen(onBackClick = onBack)
            }
        }
    }
}

@Composable
private fun ReportContent(
    viewModel: TransactionViewModel,
    padding: PaddingValues
) {
    val periodSummary by viewModel.periodSummary.collectAsState()
    val reportPeriod by viewModel.reportPeriod.collectAsState()
    val reportType by viewModel.reportType.collectAsState()
    val selectedCategory by viewModel.selectedReportCategory.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val incomeDist by viewModel.incomeDistribution.collectAsState()
    val comparisonResult by viewModel.comparisonResult.collectAsState()
    val yearSummary by viewModel.yearSummary.collectAsState()
    val allTransactions by viewModel.transactions.collectAsState()
//     val customStartDate by viewModel.customStartDate.collectAsState() // unused
//     val customEndDate by viewModel.customEndDate.collectAsState() // unused

    var showDatePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 顶部操作面板
        item { ReportTopPanel(viewModel, reportPeriod, reportType, periodSummary) }

        // ─── 汇总卡片 ───
        item { SummaryCard(periodSummary, reportType, comparisonResult, reportPeriod) }

        // ─── 日趋势折线图 ───
        if (periodSummary.dailyPoints.isNotEmpty()) {
            item { DailyTrendCard(periodSummary.dailyPoints, reportType) }
        }

        // ─── 年分布柱状图 (年视图) ───
        if (reportPeriod == ReportPeriod.YEAR && yearSummary != null) {
            item { YearDistributionCard(yearSummary!!, reportType) }
        }

        // ─── 数据对比卡片 ───
        if (comparisonResult != null) {
            item { CompareResultCard(comparisonResult!!) }
        }

        // ─── 分类分布 ───
        val stats = if (reportType == ReportType.INCOME) incomeDist.categoryBreakdown else periodSummary.categoryBreakdown
        if (stats.isNotEmpty() && selectedCategory == null) {
            item {
                CategoryPieLegendCard(
                    categories = stats,
                    allCategories = allCategories,
                    title = if (reportType == ReportType.INCOME) "收入分布" else "支出分布",
                    totalAmount = if (reportType == ReportType.INCOME) periodSummary.income else periodSummary.expense,
                    onCategoryClick = { viewModel.selectReportCategory(it) }
                )
            }
        }

        // ─── 子分类下钻 ───
        if (selectedCategory != null) {
            val selCat = selectedCategory!!
            if (!selCat.contains(".")) {
                val subCatData = allCategories.filter { it.parentName == selCat }
                val subCatNames = subCatData.map { it.name }.toSet()
                val subStats = stats.filter { it.name in subCatNames }
                item {
                    SubCategoryPieCard(
                        parentName = selCat,
                        subStats = subStats,
                        totalAmount = if (reportType == ReportType.INCOME) periodSummary.income else periodSummary.expense,
                        onBack = { viewModel.clearSelectedReportCategory() },
                        onSubClick = { subName ->
                            viewModel.selectReportCategory("$selCat.$subName")
                        }
                    )
                }
            } else {
                // 商家明细
                val parts = selCat.split(".")
                if (parts.size == 2) {
                    item {
                        CategoryMerchantCard(
                            parentName = parts[0],
                            subName = parts[1],
                            timeStart = periodSummary.startDate,
                            timeEnd = periodSummary.endDate,
                            allTransactions = allTransactions,
                            onBack = { viewModel.clearSelectedReportCategory() }
                        )
                    }
                }
            }
        }

        // ─── 排行榜 ───
        if (stats.isNotEmpty()) {
            item { RankingCard(stats, reportType, allCategories) }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    // ─── 自定义日期选择弹窗 ───
    if (showDatePicker) {
        CustomDateRangeDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                viewModel.setCustomDateRange(start, end)
                showDatePicker = false
            }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════
//  顶部操作面板 — 一体化背景图层
// ══════════════════════════════════════════════════════════════════════
@Composable
private fun ReportTopPanel(
    viewModel: TransactionViewModel,
    period: ReportPeriod,
    reportType: ReportType,
    periodSummary: PeriodSummary
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val isIncome = reportType == ReportType.INCOME
    val mainColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val bgColor = if (isIncome) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            // ═══ 收支切换（卡片样式）═══
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            ) {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(
                        selected = !isIncome,
                        onClick = { viewModel.setReportType(ReportType.EXPENSE) },
                        label = { Text("支出", fontWeight = if (!isIncome) FontWeight.Bold else FontWeight.Normal) }
                    )
                    FilterChip(
                        selected = isIncome,
                        onClick = { viewModel.setReportType(ReportType.INCOME) },
                        label = { Text("收入", fontWeight = if (isIncome) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // ═══ 日期显示 ═══
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.prevPeriod() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上一期", tint = mainColor)
                    }
                    Text(
                        text = periodLabel(period, periodSummary),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = mainColor,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    IconButton(onClick = { viewModel.nextPeriod() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下一期", tint = mainColor)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // ═══ 周期切换（卡片样式）═══
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        ReportPeriod.WEEK to "周报",
                        ReportPeriod.MONTH to "月报",
                        ReportPeriod.YEAR to "年报",
                        ReportPeriod.CUSTOM to "自定义"
                    ).forEach { (p, l) ->
                        val sel = period == p
                        TextButton(
                            onClick = {
                                if (p == ReportPeriod.CUSTOM) {
                                    showDatePicker = true
                                } else {
                                    viewModel.setReportPeriod(p)
                                }
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (sel) mainColor else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                l,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        CustomDateRangeDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                viewModel.setCustomDateRange(start, end)
                showDatePicker = false
            }
        )
    }
}

private fun periodLabel(period: ReportPeriod, summary: PeriodSummary): String {
    val sdf = SimpleDateFormat("M月d日", Locale.CHINA)
    return when (period) {
        ReportPeriod.WEEK -> {
            if (summary.startDate > 0) {
                "${sdf.format(Date(summary.startDate))}-${sdf.format(Date(summary.endDate))}"
            } else {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val start = sdf.format(Date(cal.timeInMillis))
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                val end = sdf.format(Date(cal.timeInMillis))
                "${start}-${end}"
            }
        }
        ReportPeriod.MONTH -> summary.periodLabel.ifBlank {
            val cal = Calendar.getInstance()
            val sdf2 = SimpleDateFormat("yyyy年M月", Locale.CHINA)
            sdf2.format(Date(cal.timeInMillis))
        }
        ReportPeriod.YEAR -> summary.periodLabel.ifBlank {
            "${Calendar.getInstance().get(Calendar.YEAR)}年度"
        }
        ReportPeriod.CUSTOM -> summary.periodLabel.ifBlank {
            if (summary.startDate > 0) {
                "${sdf.format(Date(summary.startDate))} - ${sdf.format(Date(summary.endDate))}"
            } else {
                "自定义期间"
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  自定义日期弹窗
// ══════════════════════════════════════════════════════════════════════
@Composable
private fun CustomDateRangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val cal = Calendar.getInstance()
    val currentYear = cal.get(Calendar.YEAR)
    
    // 开始日期状态
    var startYear by remember { mutableStateOf(currentYear) }
    var startMonth by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
    var startDay by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    
    // 结束日期状态
    var endYear by remember { mutableStateOf(currentYear) }
    var endMonth by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
    var endDay by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    
    // 当前选中的标签页：0=开始日期, 1=结束日期
    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期范围") },
        text = {
            Column {
                // 标签页切换
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("开始日期") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("结束日期") }
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // 日期选择器
                if (selectedTab == 0) {
                    DatePickerContent(
                        year = startYear,
                        month = startMonth,
                        day = startDay,
                        onYearChange = { startYear = it },
                        onMonthChange = { startMonth = it },
                        onDayChange = { startDay = it }
                    )
                } else {
                    DatePickerContent(
                        year = endYear,
                        month = endMonth,
                        day = endDay,
                        onYearChange = { endYear = it },
                        onMonthChange = { endMonth = it },
                        onDayChange = { endDay = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                try {
                    val sc = Calendar.getInstance().apply {
                        set(startYear, startMonth, startDay, 0, 0, 0)
                    }
                    val ec = Calendar.getInstance().apply {
                        set(endYear, endMonth, endDay, 23, 59, 59)
                    }
                    onConfirm(sc.timeInMillis, ec.timeInMillis)
                } catch (_: Exception) { }
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun DatePickerContent(
    year: Int,
    month: Int,
    day: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit
) {
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear - 10..currentYear + 1).toList()
    val months = (1..12).toList()
    val daysInMonth = Calendar.getInstance().apply { set(year, month, 1) }.let { cal ->
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.get(Calendar.DAY_OF_MONTH)
    }
    val days = (1..daysInMonth).toList()

    Row(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 年份选择
        NumberPicker(
            label = "年",
            values = years,
            selectedValue = year,
            onValueChange = onYearChange
        )
        
        // 月份选择
        NumberPicker(
            label = "月",
            values = months,
            selectedValue = month + 1,
            onValueChange = { onMonthChange(it - 1) }
        )
        
        // 日期选择
        NumberPicker(
            label = "日",
            values = days,
            selectedValue = day,
            onValueChange = onDayChange
        )
    }
}

@Composable
private fun NumberPicker(
    label: String,
    values: List<Int>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit
) {
    val listState = rememberLazyListState()
//     val coroutineScope = rememberCoroutineScope() // unused
    
    // 滚动到选中位置
    LaunchedEffect(selectedValue) {
        val index = values.indexOf(selectedValue)
        if (index >= 0) {
            listState.scrollToItem(index.coerceIn(0, values.size - 1))
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // 选中指示器背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(8.dp)
                    )
            )
            
            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                items(values) { value ->
                    val isSelected = value == selectedValue
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clickable { onValueChange(value) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = value.toString(),
                            style = if (isSelected) {
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            } else {
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  汇总卡片 — 带背景图层
// ══════════════════════════════════════════════════════════════════════
@Composable
private fun SummaryCard(
    summary: PeriodSummary,
    reportType: ReportType,
    comparisonResult: ComparisonResult?,
    reportPeriod: ReportPeriod = ReportPeriod.MONTH
) {
    val isIncome = reportType == ReportType.INCOME
    val mainAmount = if (isIncome) summary.income else summary.expense
    val mainColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val bgColor = if (isIncome) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
    val txnCount = if (isIncome) summary.incomeTransactionCount else summary.expenseTransactionCount

    // 结余
    val netAmount = summary.income - summary.expense

    // 变化
    val changeAmt: Double
    val changeLabel: String
    if (comparisonResult != null) {
        val cur = comparisonResult.currentData
        val prev = comparisonResult.previousData
        if (isIncome) {
            changeAmt = cur.income - prev.income
            changeLabel = when (reportPeriod) {
                ReportPeriod.WEEK -> "上周"
                ReportPeriod.MONTH -> "上月"
                ReportPeriod.YEAR -> "去年"
                ReportPeriod.CUSTOM -> "上期"
            }
        } else {
            changeAmt = cur.expense - prev.expense
            changeLabel = when (reportPeriod) {
                ReportPeriod.WEEK -> "上周"
                ReportPeriod.MONTH -> "上月"
                ReportPeriod.YEAR -> "去年"
                ReportPeriod.CUSTOM -> "上期"
            }
        }
    } else {
        changeAmt = 0.0
        changeLabel = ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // 金额行 + 结余气泡
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (isIncome) "本期收入" else "本期支出",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "¥${String.format("%,.0f", mainAmount)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = mainColor
                    )
                }
                // 结余气泡
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("结余",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "¥${String.format("%,.0f", netAmount)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (netAmount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // 笔数、日均、变化（半透明底块）
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("笔数", "${txnCount}笔")
                    StatItem(
                        "日均${if (isIncome) "收入" else "支出"}",
                        "¥${String.format("%,.0f", if (isIncome) summary.dailyAvg.avgDailyIncome else summary.dailyAvg.avgDailyExpense)}"
                    )
                    if (changeLabel.isNotBlank()) {
                        val isUp = changeAmt > 0
                        StatItem(
                            "较${changeLabel}",
                            "${if (isUp) "↑" else "↓"} ¥${String.format("%,.0f", kotlin.math.abs(changeAmt))}",
                            valueColor = if (isUp) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor)
    }
}

// ══════════════════════════════════════════════════════════════════════
//  日趋势折线图 — 加强版（渐变填充 + 粗线 + 大圆点）
// ══════════════════════════════════════════════════════════════════════
@Composable
private fun DailyTrendCard(
    dailyPoints: List<DailyPoint>,
    reportType: ReportType
) {
    val isIncome = reportType == ReportType.INCOME
    val lineColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val bgColor = if (isIncome) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    val maxVal = dailyPoints.maxOf {
        if (isIncome) it.income else it.expense
    }.coerceAtLeast(1.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("日趋势",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.height(8.dp))

            val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val padBottom = 8f
                    val padTop = 8f
                    val drawH = h - padBottom - padTop
                    val count = dailyPoints.size.coerceAtLeast(2)
                    val stepX = if (count > 1) w / (count - 1).toFloat() else w

                    // 背景网格
                    for (i in 0..3) {
                        val y = padTop + drawH * (1f - i / 3f)
                        drawLine(outlineVariantColor.copy(alpha = 0.4f), Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    }

                    // 计算坐标
                    val points = dailyPoints.mapIndexed { i, dp ->
                        val value = if (isIncome) dp.income else dp.expense
                        val x = i * stepX
                        val y = padTop + drawH * (1f - (value / maxVal).toFloat())
                        Offset(x, y)
                    }

                    // 渐变填充（线下区域）
                    if (points.size >= 2) {
                        val path = Path().apply {
                            moveTo(points.first().x, padTop + drawH)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, padTop + drawH)
                            close()
                        }
                        drawPath(
                            path,
                            brush = Brush.verticalGradient(
                                colors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.05f)),
                                startY = padTop,
                                endY = padTop + drawH
                            )
                        )
                    }

                    // 粗折线
                    if (points.size >= 2) {
                        for (i in 1 until points.size) {
                            drawLine(lineColor, points[i - 1], points[i], strokeWidth = 4f, cap = StrokeCap.Round)
                        }
                    }

                    // 大圆点
                    points.forEach { pt ->
                        drawCircle(lineColor, 7f, pt)
                        drawCircle(Color.White, 4f, pt)
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            val sdf = SimpleDateFormat("dd日", Locale.CHINA)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(sdf.format(Date(dailyPoints.first().date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(sdf.format(Date(dailyPoints.last().date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  年分布柱状图 — 带背景图层
// ══════════════════════════════════════════════════════════════════════
@Composable
private fun YearDistributionCard(
    yearSummary: YearSummary,
    reportType: ReportType
) {
    val isIncome = reportType == ReportType.INCOME
    val barColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val bgColor = if (isIncome) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
    val maxVal = yearSummary.monthlyStats.maxOfOrNull {
        if (isIncome) it.income else it.expense
    }?.coerceAtLeast(1.0) ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("${yearSummary.year}年 · 月度${if (isIncome) "收入" else "支出"}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
            Text("全年总计 ¥${String.format("%,.0f", if (isIncome) yearSummary.totalIncome else yearSummary.totalExpense)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            yearSummary.monthlyStats.forEach { ms ->
                val value = if (isIncome) ms.income else ms.expense
                val frac = (value / maxVal).toFloat().coerceIn(0f, 1f)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${ms.month}月",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(30.dp))
                    Box(modifier = Modifier.weight(1f).height(12.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))) {
                        Box(modifier = Modifier.fillMaxWidth(frac).fillMaxHeight()
                            .background(barColor, RoundedCornerShape(4.dp)))
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("¥${String.format("%,.0f", value)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(72.dp),
                        textAlign = TextAlign.End)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  数据对比卡片
// ══════════════════════════════════════════════════════════════════════
@Composable
private fun CompareResultCard(result: ComparisonResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("数据对比",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary)
                Text(if (result.compareMode == CompareMode.SEQUENTIAL) "环比" else "同比",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary)
            }
            Spacer(Modifier.height(12.dp))

            // 本期 vs 上期
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("本期（${result.currentLabel}）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("支出 ¥${String.format("%,.0f", result.currentData.expense)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary)
                    Text("收入 ¥${String.format("%,.0f", result.currentData.income)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("上期（${result.previousLabel}）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("支出 ¥${String.format("%,.0f", result.previousData.expense)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold)
                    Text("收入 ¥${String.format("%,.0f", result.previousData.income)}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(6.dp))

            // 支出变化
            val expDiff = result.currentData.expense - result.previousData.expense
            val expPct = if (result.previousData.expense > 0) (expDiff / result.previousData.expense * 100) else 0.0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("支出：", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${if (expDiff >= 0) "↑" else "↓"} ¥${String.format("%,.0f", kotlin.math.abs(expDiff))} (${String.format("%+.1f", expPct)}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (expDiff <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            // 收入变化
            val incDiff = result.currentData.income - result.previousData.income
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("收入：", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${if (incDiff >= 0) "↑" else "↓"} ¥${String.format("%,.0f", kotlin.math.abs(incDiff))}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (incDiff >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  排行榜：支出排行 or 收入排行（根据 reportType 切换）
// ══════════════════════════════════════════════════════════════════════
@Composable
private fun RankingCard(
    stats: List<CategoryStat>,
    reportType: ReportType,
    allCategories: List<CategoryData>
) {
    val isIncome = reportType == ReportType.INCOME
    val topN = stats.sortedByDescending { it.total }.take(10)
    // 使用 categories 表中最新的分类图标
    fun resolveIcon(name: String): String {
        return allCategories.find { it.name == name }?.icon ?: name
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isIncome) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (reportType == ReportType.EXPENSE) "支出排行" else "收入排行",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BlueGray
            )
            Spacer(Modifier.height(8.dp))

            topN.forEachIndexed { i, stat ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 排名序号
                    Text(
                        "${i + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (i < 3) chartColors[i] else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(20.dp)
                    )
                    // 分类名
                    CategoryIcon(
                        icon = resolveIcon(stat.name),
                        size = 20
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stat.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    // 金额
                    Text(
                        "¥${String.format("%,.0f", stat.total)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (reportType == ReportType.EXPENSE) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                }
                if (i < topN.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}


