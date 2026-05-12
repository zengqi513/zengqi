import sys
sys.path.insert(0, r'D:\新建文件夹\QClaw\resources\openclaw\config\skills\qclaw-text-file\scripts')
from write_file import main as write_main

content = '''package com.autobookkeeper.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobookkeeper.data.Budget
import com.autobookkeeper.data.BudgetStatus
import com.autobookkeeper.data.ManualDebt
import com.autobookkeeper.viewmodel.BudgetViewModel
import com.autobookkeeper.viewmodel.TransactionViewModel
import com.autobookkeeper.ui.CategoryIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgetViewModel: BudgetViewModel,
    transactionViewModel: TransactionViewModel,
    onBack: () -> Unit
) {
    val budgets by budgetViewModel.budgetsOfMonth.collectAsState()
    val totalBudgetStatus by budgetViewModel.totalBudgetStatus.collectAsState()
    val budgetStatuses by budgetViewModel.budgetStatuses.collectAsState()
    val currentMonth by budgetViewModel.currentMonth.collectAsState()
    val showEditDialog by budgetViewModel.showEditDialog.collectAsState()
    val editingBudget by budgetViewModel.editingBudget.collectAsState()
    val categories by budgetViewModel.allCategories.collectAsState()
    val expenseCategories = categories.filter { it.type == "expense" }
    val manualDebts by budgetViewModel.manualDebts.collectAsState()
    val categoryIconMap by transactionViewModel.categoryIconMap.collectAsState()
    val showDebtDialog by budgetViewModel.showDebtDialog.collectAsState()
    val debtDialogState by budgetViewModel.debtDialogState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddBudgetDialog by remember { mutableStateOf(false) }

    // 获取主题颜色
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("预算管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { budgetViewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, "刷新")
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
            // 月份导航
            MonthNavBar(currentMonth = currentMonth,
                onPrev = { budgetViewModel.navigatePrevMonth() },
                onNext = { budgetViewModel.navigateNextMonth() })

            Spacer(Modifier.height(12.dp))

            // 总预算卡片（可点击编辑）
            TotalBudgetCard(
                totalBudgetStatus = totalBudgetStatus,
                onAddEdit = {
                    if (totalBudgetStatus != null) {
                        val existing = budgets.find { it.categoryName == null }
                        if (existing != null) budgetViewModel.showEditBudget(existing)
                    } else {
                        budgetViewModel.showAddTotalBudget()
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            // 个人负债账单
            DebtSection(
                debts = manualDebts,
                onAdd = { budgetViewModel.showAddDebtDialog() },
                onEdit = { budgetViewModel.showEditDebtDialog(it) },
                onDelete = { budgetViewModel.deleteDebt(it) }
            )

            Spacer(Modifier.height(16.dp))

            // 分项生活预算标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("分项预算", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = colorScheme.primary)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showAddBudgetDialog = true }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加", fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))

            // 有预算的分类（只显示 enabled 的预算）
            val enabledCategoryBudgets = budgets.filter { it.categoryName != null }
            if (enabledCategoryBudgets.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant)
                ) {
                    Text("暂无分项预算，点击右上角「添加」",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center)
                }
            } else {
                enabledCategoryBudgets.forEach { budget ->
                    val status = budgetStatuses.find { it.categoryName == budget.categoryName }
                    CategoryBudgetCard(
                        budget = budget, status = status,
                        icon = categoryIconMap[budget.categoryName] ?: "📋",
                        onEdit = { budgetViewModel.showEditBudget(budget) },
                        onDelete = { budgetViewModel.deleteBudget(budget) },
                        onToggleEnabled = { budgetViewModel.toggleBudgetEnabled(budget) })
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // 预算编辑/新增弹窗
    if (showEditDialog && editingBudget != null) {
        BudgetEditDialog(
            budget = editingBudget!!,
            onDismiss = { budgetViewModel.dismissEditDialog() },
            onSave = { amount -> budgetViewModel.saveBudget(amount, editingBudget!!.categoryName) }
        )
    }

    // 快速添加预算弹窗（含分类选择）
    if (showAddBudgetDialog) {
        AddBudgetDialog(
            expenseCategories = expenseCategories,
            categoryIconMap = categoryIconMap,
            onDismiss = { showAddBudgetDialog = false },
            onAdd = { catName, amount ->
                budgetViewModel.saveBudget(amount, catName)
                showAddBudgetDialog = false
            }
        )
    }

    // 负债编辑弹窗
    if (showDebtDialog) {
        DebtEditDialog(
            existing = debtDialogState,
            onDismiss = { budgetViewModel.dismissDebtDialog() },
            onSave = { name, amount, monthly, rate, notes ->
                budgetViewModel.saveDebt(name, amount, monthly, rate, notes)
            }
        )
    }
}

// 月份导航
@Composable
private fun MonthNavBar(currentMonth: String, onPrev: () -> Unit, onNext: () -> Unit) {
    val parts = currentMonth.split("-")
    val label = if (parts.size == 2) "${parts[0]}年${parts[1].toInt()}月" else currentMonth
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "上月")
        }
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "下月")
        }
    }
}

// 总预算卡片（可点击编辑）
@Composable
private fun TotalBudgetCard(totalBudgetStatus: BudgetStatus?, onAddEdit: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onAddEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalanceWallet, null, tint = colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("月度总预算", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Edit, null, tint = colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text("编辑", fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val amount = if (totalBudgetStatus != null) totalBudgetStatus.budgetAmount else 0.0
                Text("预算额度", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
                Text("¥${String.format("%,.0f", amount)}",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = if (amount > 0) colorScheme.primary else colorScheme.onSurfaceVariant)
            }
            if (totalBudgetStatus != null) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("已支出", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                    Text("¥${String.format("%,.0f", totalBudgetStatus.spentAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (totalBudgetStatus.isOverBudget) colorScheme.error else colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                val pct = totalBudgetStatus.percentage.coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (pct > 0.85f) colorScheme.error else if (pct > 0.6f) colorScheme.tertiary else colorScheme.primary,
                    trackColor = colorScheme.primaryContainer
                )
            }
        }
    }
}

// 分项预算卡片
@Composable
private fun CategoryBudgetCard(
    budget: Budget,
    status: BudgetStatus?,
    icon: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    val spent = status?.spentAmount ?: 0.0
    val pct = status?.percentage?.coerceIn(0f, 1f) ?: 0f
    val isOver = status?.isOverBudget == true
    val colorScheme = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!budget.enabled) colorScheme.surfaceVariant else colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (budget.enabled) 1.dp else 0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon.isNotEmpty()) {
                    CategoryIcon(icon = icon, size = 20)
                } else {
                    Text("📋", fontSize = 16.sp)
                }
                Spacer(Modifier.width(6.dp))
                Text(budget.categoryName ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text("¥${String.format("%,.0f", budget.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) colorScheme.error else colorScheme.primary)
            }
            if (budget.enabled) {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("已用 ¥${String.format("%,.0f", spent)}",
                        style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                    Text("剩余 ¥${String.format("%,.0f", (budget.amount - spent).coerceAtLeast(0.0))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOver) colorScheme.error else colorScheme.primary)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { pct },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = if (isOver) colorScheme.error else if (pct > 0.75f) colorScheme.tertiary else colorScheme.primary,
                    trackColor = colorScheme.primaryContainer
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onToggleEnabled) {
                    Text(if (budget.enabled) "隐藏" else "显示", fontSize = 12.sp)
                }
                TextButton(onClick = onEdit) {
                    Text("编辑", fontSize = 12.sp)
                }
                TextButton(onClick = onDelete) {
                    Text("删除", color = colorScheme.error, fontSize = 12.sp)
                }
            }
        }
    }
}

// 添加预算对话框（含搜索+层级+图标+金额）
@Composable
private fun AddBudgetDialog(
    expenseCategories: List<com.autobookkeeper.data.CategoryData>,
    categoryIconMap: Map<String, String>,
    onDismiss: () -> Unit,
    onAdd: (categoryName: String, amount: Double) -> Unit
) {
    val catIconMap = remember(expenseCategories) {
        expenseCategories.associate { it.name to it.icon }
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var amountText by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(0) }
    val colorScheme = MaterialTheme.colorScheme

    val parentCategories = expenseCategories.filter { it.parentName == null }
    val subCategories = expenseCategories.filter { it.parentName != null }

    val filtered = remember(expenseCategories, searchQuery) {
        if (searchQuery.isBlank()) {
            expenseCategories
        } else {
            val q = searchQuery.trim().lowercase()
            expenseCategories.filter { cat ->
                cat.name.lowercase().contains(q) ||
                (cat.parentName?.lowercase()?.contains(q) == true)
            }
        }
    }

    val selectedCat = expenseCategories.find { it.name == selectedCategory }

    if (step == 0) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("添加预算分类") },
            text = {
                Column(modifier = Modifier.heightIn(max = 480.dp).fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜索分类名称...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))

                    val displayList = if (searchQuery.isNotBlank()) filtered else expenseCategories

                    if (displayList.isEmpty()) {
                        Text("未匹配到分类",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
                            textAlign = TextAlign.Center)
                    } else {
                        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                            if (searchQuery.isBlank()) {
                                parentCategories.forEach { parent ->
                                    val parentSubs = subCategories.filter { it.parentName == parent.name }
                                    item {
                                        ParentBudgetCategoryItem(
                                            category = parent,
                                            iconMap = catIconMap,
                                            isSelected = selectedCategory == parent.name,
                                            onClick = { selectedCategory = parent.name },
                                            subCount = parentSubs.size
                                        )
                                    }
                                    parentSubs.forEach { sub ->
                                        item {
                                            SubBudgetCategoryItem(
                                                parentName = parent.name,
                                                category = sub,
                                                iconMap = catIconMap,
                                                isSelected = selectedCategory == sub.name,
                                                onClick = { selectedCategory = sub.name }
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(displayList) { cat ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedCategory = cat.name }
                                            .background(
                                                if (selectedCategory == cat.name) colorScheme.primaryContainer
                                                else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CategoryIcon(icon = catIconMap[cat.name] ?: cat.icon, size = 20)
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                                            if (cat.parentName != null) {
                                                Text(cat.parentName, style = MaterialTheme.typography.labelSmall,
                                                    color = colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        if (selectedCategory == cat.name) {
                                            Icon(Icons.Default.Check, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { if (selectedCategory != null) step = 1 },
                    enabled = selectedCategory != null
                ) { Text("下一步") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("设置预算金额") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryIcon(icon = catIconMap[selectedCategory] ?: "📋", size = 26)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            if (selectedCat?.parentName != null) {
                                Text(selectedCat.parentName, style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant)
                            }
                            Text(selectedCategory ?: "", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("预算金额") },
                        prefix = { Text("¥") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount >= 0 && selectedCategory != null) {
                        onAdd(selectedCategory!!, amount)
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { step = 0 }) { Text("上一步") }
            }
        )
    }
}

// 添加预算：父分类行
@Composable
private fun ParentBudgetCategoryItem(
    category: com.autobookkeeper.data.CategoryData,
    iconMap: Map<String, String>,
    isSelected: Boolean,
    onClick: () -> Unit,
    subCount: Int
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(
                if (isSelected) colorScheme.primaryContainer else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(icon = iconMap[category.name] ?: category.icon, size = 22)
        Spacer(Modifier.width(10.dp))
        Text(category.name, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold)
        if (subCount > 0) {
            Spacer(Modifier.width(6.dp))
            Text("(${subCount})", style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        if (isSelected) {
            Icon(Icons.Default.Check, null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

// 添加预算：子分类行
@Composable
private fun SubBudgetCategoryItem(
    parentName: String,
    category: com.autobookkeeper.data.CategoryData,
    iconMap: Map<String, String>,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 28.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
            .background(
                if (isSelected) colorScheme.primaryContainer else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIcon(icon = iconMap[category.name] ?: category.icon, size = 18)
        Spacer(Modifier.width(8.dp))
        Text(category.name, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium)
        Spacer(Modifier.weight(1f))
        if (isSelected) {
            Icon(Icons.Default.Check, null, tint = colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}

// 负债管理板块
@Composable
private fun DebtSection(
    debts: List<ManualDebt>,
    onAdd: () -> Unit,
    onEdit: (ManualDebt) -> Unit,
    onDelete: (ManualDebt) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, null, tint = colorScheme.tertiary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("个人负债账单", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = colorScheme.primary)
                Spacer(Modifier.weight(1f))
                if (debts.isNotEmpty()) {
                    Text("共${debts.size}笔", style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(12.dp))
            if (debts.isEmpty()) {
                Text("暂无负债记录，点击下方添加",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp))
            } else {
                val totalAmount = debts.sumOf { it.totalAmount }
                val totalMonthly = debts.sumOf { it.monthlyPayment }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("负债总额", style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant)
                    Text("¥${String.format("%,.0f", totalAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold, color = colorScheme.error)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("月还款", style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant)
                    Text("¥${String.format("%,.0f", totalMonthly)}/月",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.tertiary)
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                debts.forEach { debt ->
                    DebtItemRow(debt = debt, onEdit = { onEdit(debt) }, onDelete = { onDelete(debt) })
                    Spacer(Modifier.height(6.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("添加负债")
            }
        }
    }
}

@Composable
private fun DebtItemRow(debt: ManualDebt, onEdit: () -> Unit, onDelete: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(debt.name, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
                Text("总额 ¥${String.format("%,.0f", debt.totalAmount)}  月还 ¥${String.format("%,.0f", debt.monthlyPayment)}",
                    style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                if (debt.interestRate > 0) {
                    Text("利率 ${(debt.interestRate * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall, color = colorScheme.tertiary)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, "删除", tint = colorScheme.outline, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// 预算编辑弹窗
@Composable
private fun BudgetEditDialog(
    budget: Budget,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(
        if (budget.amount > 0) "%.0f".format(budget.amount) else ""
    ) }
    val title = if (budget.categoryName == null) "设置月度总预算" else "设置「${budget.categoryName}」预算"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("预算金额") },
                prefix = { Text("¥") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull()
                if (amount != null && amount >= 0) {
                    onSave(amount)
                }
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// 负债编辑弹窗
@Composable
private fun DebtEditDialog(
    existing: ManualDebt?,
    onDismiss: () -> Unit,
    onSave: (name: String, amount: Double, monthly: Double, rate: Float, notes: String) -> Unit
) {
    var nameText by remember { mutableStateOf(existing?.name ?: "") }
    var amountText by remember { mutableStateOf(
        if (existing != null) "%.0f".format(existing.totalAmount) else ""
    ) }
    var monthlyText by remember { mutableStateOf(
        if (existing != null) "%.0f".format(existing.monthlyPayment) else ""
    ) }
    var rateText by remember { mutableStateOf(
        if (existing != null) "${(existing.interestRate * 100).toInt()}" else ""
    ) }
    var notesText by remember { mutableStateOf(existing?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "编辑负债" else "添加负债") },
        text = {
            Column {
                OutlinedTextField(value = nameText, onValueChange = { nameText = it },
                    label = { Text("负债名称") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("如 花呗、信用卡分期") })
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it },
                        label = { Text("总金额") }, prefix = { Text("¥") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = monthlyText, onValueChange = { monthlyText = it },
                        label = { Text("月还") }, prefix = { Text("¥") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = rateText, onValueChange = { rateText = it },
                    label = { Text("年利率（%）") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("选填，如 18") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = notesText, onValueChange = { notesText = it },
                    label = { Text("备注") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val name = nameText.trim()
                val amount = amountText.toDoubleOrNull() ?: return@TextButton
                val monthly = monthlyText.toDoubleOrNull() ?: 0.0
                val rate = (rateText.toFloatOrNull() ?: 0f) / 100f
                if (name.isNotEmpty() && amount > 0) {
                    onSave(name, amount, monthly, rate, notesText.trim())
                }
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
'''

# Write the file
import sys
sys.argv = ['write_file.py', '--path', r'E:\AutoBookkeeper\app\src\main\java\com\autobookkeeper\ui\BudgetScreen.kt', '--content', content, '--platform', 'windows']
write_main()
