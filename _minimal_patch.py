#!/usr/bin/env python3
"""Minimal patch: write only NEW components, then make small edits to ReportScreen"""
path = r'C:\Users\77497\.qclaw\workspace\AutoBookkeeper\app\src\main\java\com\autobookkeeper\ui\ReportScreen.kt'

with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Patch 1: Add new imports
old_import = 'import com.autobookkeeper.data.CategoryStat\nimport com.autobookkeeper.data.Transaction\nimport com.autobookkeeper.viewmodel.*'
new_import = 'import com.autobookkeeper.data.CategoryData\nimport com.autobookkeeper.data.CategoryStat\nimport com.autobookkeeper.data.Transaction\nimport com.autobookkeeper.viewmodel.*'
content = content.replace(old_import, new_import)

# Patch 2: Replace the ReportCategoryChartSection call block
old_block = '''                // ███ 分类分布（饼图 + 列表） ███
                if (incomeDistribution.categoryBreakdown.isNotEmpty()) {
                    val title = when (reportType) {
                        ReportType.EXPENSE -> "支出分布"
                        ReportType.INCOME -> "收入分布"
                    }
                    ReportCategoryChartSection(
                        categories = incomeDistribution.categoryBreakdown,
                        title = title,
                        totalAmount = incomeDistribution.totalIncome,
                        onCategoryClick = { viewModel.selectReportCategory(it) }
                    )
                }'''

new_block = '''                // ███ 分类分布（扇形图 + 图例 + 下钻） ███
                if (incomeDistribution.categoryBreakdown.isNotEmpty()) {
                    val title = when (reportType) {
                        ReportType.EXPENSE -> "支出分布"
                        ReportType.INCOME -> "收入分布"
                    }
                    if (selectedParentCategory != null) {
                        val parentName = selectedParentCategory!!
                        val subCats = allCats.filter { it.parentName == parentName && it.type == (if (reportType == ReportType.EXPENSE) "expense" else "income") }
                        val subStats = incomeDistribution.categoryBreakdown.filter { cat ->
                            subCats.any { it.name == cat.name }
                        }
                        if (subCategoryDrill != null) {
                            val subName = subCategoryDrill!!
                            CategoryMerchantCard(parentName, subName, periodSummary.startDate, periodSummary.endDate, trans, onBack = { subCategoryDrill = null })
                        } else {
                            SubCategoryPieCard(parentName, subStats, incomeDistribution.totalIncome, onBack = { selectedParentCategory = null }, onSubClick = { subCategoryDrill = it })
                        }
                    } else {
                        CategoryPieLegendCard(
                            categories = incomeDistribution.categoryBreakdown,
                            allCategories = allCats,
                            title = title,
                            totalAmount = incomeDistribution.totalIncome,
                            onCategoryClick = { catName ->
                                val hasSub = allCats.any { it.parentName == catName }
                                if (hasSub) selectedParentCategory = catName
                                else viewModel.selectReportCategory(catName)
                            }
                        )
                    }
                }'''

if old_block in content:
    content = content.replace(old_block, new_block)
    print('OK: block replaced')
else:
    print('FAIL: block not found')

# Patch 3: Add drilldown state variables after customPickerStep
old_state = '    var customPickerStep by remember { mutableStateOf(0) }\n    var customStart by remember { mutableStateOf<Long?>(null) }'
new_state = '''    var customPickerStep by remember { mutableStateOf(0) }
    var customStart by remember { mutableStateOf<Long?>(null) }

    // 下钻状态
    var selectedParentCategory by remember { mutableStateOf<String?>(null) }
    var subCategoryDrill by remember { mutableStateOf<String?>(null) }
    val allCats by viewModel.allCategories.collectAsState()'''

content = content.replace(old_state, new_state)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('Write done')
