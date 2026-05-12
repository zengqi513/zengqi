package com.autobookkeeper.ui

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.autobookkeeper.data.CategoryData
import com.autobookkeeper.data.Source
import com.autobookkeeper.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

// ─── 来源图标映射（仅缩略字符，便于区分） ───
// ─── 来源中文名 ───
private fun sourceShort(src: Source): String = src.label.take(1)

private val COMMON_SOURCES = listOf(
    Source.MANUAL,
    Source.WECHAT,
    Source.ALIPAY,
    Source.PDD,
    Source.JD,
    Source.DOUYIN,
    Source.MEITUAN
)

// ─── 来源分组（用于分组展示） ───
private val ALL_SOURCE_GROUPS = mapOf(
    "支付工具"  to listOf(Source.MANUAL, Source.WECHAT, Source.ALIPAY),
    "电商"     to listOf(Source.TAOBAO, Source.TAOBAO_FLASH, Source.PDD, Source.JD, Source.DOUYIN, Source.KUAISHOU, Source.MEITUAN, Source.DINGDONG, Source.PUPU),
    "银行"     to listOf(Source.BANK_ICBC, Source.BANK_CCB, Source.BANK_CMB, Source.BANK_BOCOM, Source.BANK_ABC, Source.BOC, Source.BANK_SPDB, Source.BANK_CITI, Source.BANK_CEB, Source.BANK_CMBC, Source.BANK_CITIC, Source.BANK_HXB, Source.BANK_PAB),
    "其他"     to listOf(Source.UNIONPAY, Source.DCEP)
)

// ─── 颜色调色盘（来源标签使用） ───
private val SOURCE_COLORS = listOf(
    Color(0xFF1E88E5), // 蓝
    Color(0xFF43A047), // 绿
    Color(0xFFE53935), // 红
    Color(0xFFFB8C00), // 橙
    Color(0xFF8E24AA), // 紫
    Color(0xFF00ACC1), // 青
    Color(0xFF6D4C41), // 棕
    Color(0xFF546E7A), // 灰蓝
    Color(0xFFF4511E), // 深橙
    Color(0xFF3949AB), // 靛
    Color(0xFFC0CA33), // 黄绿
    Color(0xFF7B1FA2), // 紫红
    Color(0xFF00897B), // 青绿
    Color(0xFF5C6BC0), // 浅靛
    Color(0xFF26A69A), // 青绿
    Color(0xFFD81B60), // 玫红
    Color(0xFF78909C), // 灰
)

private fun sourceColor(src: Source): Color {
    val idx = Source.entries.indexOf(src)
    return SOURCE_COLORS[idx % SOURCE_COLORS.size]
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditScreen(
    viewModel: TransactionViewModel,
    editId: Long? = null,
    onBack: () -> Unit,
    onSaveComplete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()

    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf<CategoryData?>(null) }
    var selectedParent by remember { mutableStateOf<CategoryData?>(null) }
    var selectedSource by remember { mutableStateOf(Source.MANUAL) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var isInitialized by remember { mutableStateOf(false) }

    // 来源展开状态
    var showAllSources by remember { mutableStateOf(false) }

    // 快速添加分类状态
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var addCategoryMode by remember { mutableStateOf("parent") }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("") }

    // 编辑模式加载数据
    LaunchedEffect(editId) {
        if (editId != null) {
            val txn = viewModel.getTransactionByIdSuspend(editId)
            txn?.let {
                amountText = kotlin.math.abs(it.amount).toString()
                note = it.note
                isExpense = it.amount < 0
                selectedSource = it.source
                selectedDate = it.date
                isInitialized = true
            }
        }
    }

    LaunchedEffect(isInitialized, editId, expenseCategories, incomeCategories) {
        if (editId != null && isInitialized && selectedCategory == null) {
            val txn = viewModel.getTransactionByIdSuspend(editId)
            txn?.let {
                val categories = if (it.amount < 0) expenseCategories else incomeCategories
                if (categories.isNotEmpty()) {
                    val category = categories.find { c -> c.name == it.categoryName }
                    if (category != null) {
                        selectedCategory = category
                        selectedParent = if (category.parentName != null)
                            categories.find { c -> c.name == category.parentName }
                        else category
                    } else {
                        val firstParent = categories.firstOrNull { c -> c.parentName == null }
                        if (firstParent != null) {
                            selectedParent = firstParent
                            selectedCategory = firstParent
                        }
                    }
                }
            }
        }
    }

    val allCategories = if (isExpense) expenseCategories else incomeCategories
    val parentCategories = allCategories.filter { it.parentName == null && !it.isHidden }.sortedBy { it.sortOrder }
    val subCategories = if (selectedParent != null)
        allCategories.filter { it.parentName == selectedParent!!.name }
    else emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editId == null) "添加记录" else "编辑记录", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val amount = amountText.toDoubleOrNull() ?: return@IconButton
                        if (amount <= 0 || selectedCategory == null) return@IconButton
                        val signedAmount = if (isExpense) -amount else amount
                        if (editId != null) {
                            kotlinx.coroutines.runBlocking {
                                val existing = viewModel.getTransactionByIdSuspend(editId)
                                existing?.let {
                                    viewModel.updateTransaction(it.copy(
                                        amount = signedAmount,
                                        categoryName = selectedCategory!!.name,
                                        categoryIcon = selectedCategory!!.icon,
                                        source = selectedSource,
                                        note = note,
                                        date = selectedDate
                                    ))
                                }
                            }
                        } else {
                            viewModel.addTransaction(
                                amount = signedAmount,
                                categoryName = selectedCategory!!.name,
                                categoryIcon = selectedCategory!!.icon,
                                source = selectedSource,
                                note = note,
                                date = selectedDate
                            )
                        }
                        onSaveComplete?.invoke()
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // █████ 收支类型切换 + 金额输入 █████
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                // 支出/收入 FilterChip
                FilterChip(
                    selected = isExpense,
                    onClick = { isExpense = true; selectedCategory = null; selectedParent = null },
                    label = { Text("支出", fontWeight = FontWeight.Bold) },
                    leadingIcon = { Text("📤", fontSize = MaterialTheme.typography.labelLarge.fontSize) }
                )
                Spacer(Modifier.width(12.dp))
                FilterChip(
                    selected = !isExpense,
                    onClick = { isExpense = false; selectedCategory = null; selectedParent = null },
                    label = { Text("收入", fontWeight = FontWeight.Bold) },
                    leadingIcon = { Text("📥", fontSize = MaterialTheme.typography.labelLarge.fontSize) }
                )

                Spacer(Modifier.width(16.dp))

                // 金额输入
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    prefix = { Text("¥", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    placeholder = { Text("0.00") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            // █████ 日期选择 █████
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    DatePickerDialog(
                        context,
                        { _: DatePicker, year: Int, month: Int, day: Int ->
                            selectedDate = Calendar.getInstance().apply { set(year, month, day) }.timeInMillis
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(dateFormatter.format(Date(selectedDate)))
            }

            // █████ 分类选择 █████
            Text("分类", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            // 一级分类
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                parentCategories.forEach { cat ->
                    val isSel = selectedParent?.name == cat.name
                    val bg = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    val tc = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    Surface(
                        modifier = Modifier.clickable {
                            selectedParent = cat
                            val hasSub = allCategories.any { it.parentName == cat.name }
                            selectedCategory = if (!hasSub) cat else null
                        },
                        shape = RoundedCornerShape(20.dp),
                        color = bg
                    ) {
                        Text(
                            "${cat.icon} ${cat.name}",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = tc
                        )
                    }
                }
            }

            // 二级分类
            if (subCategories.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    subCategories.forEach { cat ->
                        val isSel = selectedCategory?.name == cat.name
                        val bg = if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        val tc = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                        Surface(
                            modifier = Modifier.clickable { selectedCategory = cat },
                            shape = RoundedCornerShape(16.dp),
                            color = bg
                        ) {
                            Text(
                                "${cat.icon} ${cat.name}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = tc
                            )
                        }
                    }
                }
            }

            // 已选分类提示
            if (selectedCategory != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selectedCategory!!.icon} ${selectedCategory!!.name}",
                            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        if (selectedCategory!!.parentName != null) {
                            Spacer(Modifier.width(6.dp))
                            Text("（${selectedCategory!!.parentName}）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // █████ 来源选择（带颜色标签 + 分组折叠） █████
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("来源", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = { showAllSources = !showAllSources }
                ) {
                    Icon(
                        if (showAllSources) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (showAllSources) "收起" else "更多", style = MaterialTheme.typography.labelSmall)
                }
            }

            // === 常用来源 ===
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                COMMON_SOURCES.forEach { src ->
                    val isSel = selectedSource == src
                    Surface(
                        modifier = Modifier.clickable { selectedSource = src },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSel) sourceColor(src).copy(alpha = 0.85f) else sourceColor(src).copy(alpha = 0.12f),
                        border = if (isSel) null else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 圆点
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(sourceColor(src).copy(alpha = if (isSel) 1f else 0.5f))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                src.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else sourceColor(src)
                            )
                        }
                    }
                }
            }

            // === 展开：分组展示所有来源 ===
            if (showAllSources) {
                ALL_SOURCE_GROUPS.forEach { (groupName, sources) ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            groupName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            sources.forEach { src ->
                                val isSel = selectedSource == src
                                Surface(
                                    modifier = Modifier.clickable { selectedSource = src },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSel) sourceColor(src).copy(alpha = 0.85f) else sourceColor(src).copy(alpha = 0.08f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            src.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) Color.White else sourceColor(src).copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // █████ 备注 █████
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }

    // ── 快速添加分类对话框 ──
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text(if (addCategoryMode == "parent") "添加一级分类" else "添加子分类（${selectedParent?.name}）") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("分类名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCategoryIcon,
                        onValueChange = { newCategoryIcon = it },
                        label = { Text("图标（可选，留空自动匹配）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("如：🍜、🚗、🏠") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newCategoryName.isNotBlank()) {
                        val icon = if (newCategoryIcon.isNotBlank()) newCategoryIcon else "📌"
                        val type = if (isExpense) "expense" else "income"
                        val parentName = if (addCategoryMode == "sub") selectedParent?.name else null
                        viewModel.addCategory(newCategoryName, icon, type, parentName)
                        showAddCategoryDialog = false
                    }
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("取消") }
            }
        )
    }
}
