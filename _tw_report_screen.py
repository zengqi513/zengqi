# -*- coding: utf-8 -*-
import sys, subprocess

content = """package com.autobookkeeper.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autobookkeeper.data.*
import com.autobookkeeper.viewmodel.TransactionViewModel
import com.autobookkeeper.viewmodel.ReportPeriod
import com.autobookkeeper.viewmodel.ReportType
import com.autobookkeeper.viewmodel.ComparePeriodType
import com.autobookkeeper.viewmodel.CompareMode

private val BlueGray = Color(0xFF607D8B)
private val Teal500 = Color(0xFF009688)
private val Orange400 = Color(0xFFFF9800)
private val Pink400 = Color(0xFFE91E63)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    viewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val periodSummary by viewModel.periodSummary.collectAsState()
    val reportPeriod by viewModel.reportPeriod.collectAsState()
    val reportType by viewModel.reportType.collectAsState()
    val selectedReportCategory by viewModel.selectedReportCategory.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val comparisonResult by viewModel.comparisonResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("报表") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.forceRefresh() }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // 收支切换 + 周期选择 + 月份导航
            item { ReportTopBar(viewModel, reportPeriod, reportType, periodSummary) }

            // 汇总卡片
            item { SummaryCard(periodSummary, reportType) }

            // 分类饼图（支出/收入按当前 reportType 切换）
            item {
                CategoryPieSection(viewModel, reportType, periodSummary, allCategories, selectedReportCategory)
            }

            // 下钻子分类
            if (selectedReportCategory != null) {
                item {
                    SubCategorySection(viewModel, selectedReportCategory!!, reportType, periodSummary, allCategories)
                }
            }

            // 数据对比
            if (comparisonResult != null) {
                item { CompareCard(comparisonResult!!) }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ReportTopBar(
    viewModel: TransactionViewModel,
    period: ReportPeriod,
    reportType: ReportType,
    summary: com.autobookkeeper.viewmodel.PeriodSummary
) {
    val periodLabel = when (period) {
        ReportPeriod.WEEK -> "本周"
        ReportPeriod.MONTH -> summary.monthLabel.ifBlank { viewModel.currentMonth.collectAsState().value }
        ReportPeriod.YEAR -> "${summary.yearLabel}年"
        ReportPeriod.CUSTOM -> "自定义"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(
                    selected = reportType == ReportType.EXPENSE,
                    onClick = { viewModel.setReportType(ReportType.EXPENSE) },
                    label = { Text("支出") }
                )
                FilterChip(
                    selected = reportType == ReportType.INCOME,
                    onClick = { viewModel.setReportType(ReportType.INCOME) },
                    label = { Text("收入") }
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                FilterChip(selected = period == ReportPeriod.WEEK,
                    onClick = { viewModel.setReportPeriod(ReportPeriod.WEEK) },
                    label = { Text("周") })
                FilterChip(selected = period == ReportPeriod.MONTH,
                    onClick = { viewModel.setReportPeriod(ReportPeriod.MONTH) },
                    label = { Text("月") })
                FilterChip(selected = period == ReportPeriod.YEAR,
                    onClick = { viewModel.setReportPeriod(ReportPeriod.YEAR) },
                    label = { Text("年") })
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.prevPeriod() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "上一期")
                }
                Text(periodLabel, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.nextPeriod() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "下一期")
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    summary: com.autobookkeeper.viewmodel.PeriodSummary,
    reportType: ReportType
) {
    val mainAmount = if (reportType == ReportType.INCOME) summary.totalIncome else summary.totalExpense
    val compareAmount = if (reportType == ReportType.INCOME) summary.totalExpense else summary.totalIncome

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (reportType == ReportType.INCOME) "收入 ¥${String.format("%,.0f", mainAmount)}"
                else "支出 ¥${String.format("%,.0f", mainAmount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (reportType == ReportType.INCOME) Teal500 else Pink400
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${if (reportType == ReportType.INCOME) "支出" else "收入"} ¥${String.format("%,.0f", compareAmount)}",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF9E9E9E))
                Text("${summary.count}笔",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF9E9E9E))
            }
            val avg = if (summary.count > 0) mainAmount / summary.count else 0.0
            Text("日均 ¥${String.format("%,.0f", avg)}",
                style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E))
        }
    }
}

@Composable
private fun CategoryPieSection(
    viewModel: TransactionViewModel,
    reportType: ReportType,
    summary: com.autobookkeeper.viewmodel.PeriodSummary,
    allCategories: List<CategoryData>,
    selectedCategory: String?
) {
    // 用 LaunchedEffect 自动加载分类分布
    LaunchedEffect(reportType, summary) {
        viewModel.loadReportCategoryDistribution()
    }

    // 从 ViewModel 获取分类统计数据，这里用内部状态
    // 实际上 ViewModel 使用 transactionDao 获取，我们再在 UI 取
    val expenseStats = emptyList<CategoryStat>()
    val incomeStats = emptyList<CategoryStat>()

    if (reportType == ReportType.EXPENSE && expenseStats.isNotEmpty()) {
        CategoryPieLegendCard(
            categories = expenseStats,
            allCategories = allCategories,
            title = "支出分类分布",
            totalAmount = summary.totalExpense,
            onCategoryClick = { viewModel.selectReportCategory(it) }
        )
    }
    if (reportType == ReportType.INCOME && incomeStats.isNotEmpty()) {
        CategoryPieLegendCard(
            categories = incomeStats,
            allCategories = allCategories,
            title = "收入分类分布",
            totalAmount = summary.totalIncome,
            onCategoryClick = { viewModel.selectReportCategory(it) }
        )
    }
}

@Composable
private fun SubCategorySection(
    viewModel: TransactionViewModel,
    parentName: String,
    reportType: ReportType,
    summary: com.autobookkeeper.viewmodel.PeriodSummary,
    allCategories: List<CategoryData>
) {
    // 子分类数据需要在 ViewModel 中添加获取逻辑
    TextButton(onClick = { viewModel.clearSelectedReportCategory() }) {
        Text("< 返回分类总览")
    }
}

@Composable
private fun CompareCard(
    result: com.autobookkeeper.viewmodel.ComparisonResult
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, null,
                    tint = if (result.currentData.expense <= result.previousData.expense) Teal500 else Pink400)
                Spacer(Modifier.width(6.dp))
                Text("数据对比",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = BlueGray)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("本期", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E))
                    Text("¥${String.format("%,.0f", result.currentData.expense)}",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                        color = if (result.currentData.expense <= result.previousData.expense) Teal500 else Pink400)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("上期", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E))
                    Text("¥${String.format("%,.0f", result.previousData.expense)}",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            val diff = result.currentData.expense - result.previousData.expense
            val pct = if (result.previousData.expense > 0)
                (diff / result.previousData.expense * 100) else 0.0
            Text("${if (diff >= 0) "↑" else "↓"} ${String.format("%+.0f", diff)} (${String.format("%+.1f", pct)}%)",
                style = MaterialTheme.typography.labelSmall,
                color = if (diff <= 0) Teal500 else Pink400)
        }
    }
}
"""

file_path = r'C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\ui\ReportScreen.kt'
with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

result = subprocess.run(
    [sys.executable, r'D:\新建文件夹\QClaw\resources\openclaw\config\skills\qclaw-text-file\scripts\write_file.py',
     '--path', file_path, '--content-file', file_path],
    capture_output=True, text=True
)
print(result.stdout[-300:], result.stderr[-300:])
"""
