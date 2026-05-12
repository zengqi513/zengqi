package com.autobookkeeper.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autobookkeeper.data.CategoryData
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.autobookkeeper.speech.XunfeiSpeechRecognizer
import com.autobookkeeper.data.Source
import com.autobookkeeper.data.Transaction
import com.autobookkeeper.ui.SwipeableTransactionItem
import com.autobookkeeper.viewmodel.HomePeriodMode
import com.autobookkeeper.viewmodel.MonthlySummary
import com.autobookkeeper.viewmodel.TransactionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Quickreply
import androidx.compose.material.icons.filled.SearchOff
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    viewModel: TransactionViewModel,
    userPreferences: com.autobookkeeper.data.UserPreferences,
    onAddClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    onDataClick: () -> Unit,
    onCategoryClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onBudgetClick: () -> Unit = {},
    onAiClick: () -> Unit = {},
    onVoiceRecordClick: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    @Suppress("UNUSED_VARIABLE") val context = LocalContext.current
    val transactions by viewModel.transactions.collectAsState()
    val summary by viewModel.searchSummary.collectAsState()
    val homePeriodMode by viewModel.homePeriodMode.collectAsState()
    val homePeriodLabel by viewModel.homePeriodLabel.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

    // 搜索
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    // 批量选择
    val selectedIds by viewModel.selectedIds.collectAsState()
    val isManageMode by viewModel.isManageMode.collectAsState()

    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var showDeleteSecondConfirm by remember { mutableStateOf<Long?>(null) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }
    var showBatchDeleteSecondConfirm by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var showCategoryBatchDialog by remember { mutableStateOf(false) }
    var showDateBatchDialog by remember { mutableStateOf(false) }
    var batchEditDate by remember { mutableStateOf(System.currentTimeMillis()) }

    // 快速记账状态
    var showQuickEntryDialog by remember { mutableStateOf(false) }
    var quickEntryText by remember { mutableStateOf("") }
    var isExpenseForQuick by remember { mutableStateOf(true) }
    val expenseCategories by viewModel.expenseCategories.collectAsState()
    val incomeCategories by viewModel.incomeCategories.collectAsState()

    // 实时分类图标映射(修改分类图标后立即同步)
    val categoryIconMap by viewModel.categoryIconMap.collectAsState()

    // AI 悬浮按钮状态
    val aiFloatingVisible by userPreferences.aiFloatingVisible.collectAsState(initial = true)
    val aiFloatingX by userPreferences.aiFloatingX.collectAsState(initial = 0.85f)
    val aiFloatingY by userPreferences.aiFloatingY.collectAsState(initial = 0.15f)
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // AI 按钮位置(像素值)
    var aiButtonOffsetX by remember { mutableStateOf(aiFloatingX * screenWidth.value) }
    var aiButtonOffsetY by remember { mutableStateOf(aiFloatingY * screenHeight.value) }

    // 同步 DataStore 位置变化
    LaunchedEffect(aiFloatingX, aiFloatingY) {
        aiButtonOffsetX = aiFloatingX * screenWidth.value
        aiButtonOffsetY = aiFloatingY * screenHeight.value
    }

    val displayList = if (showSearchBar && searchQuery.isNotBlank()) searchResults else transactions

    // 搜索弹窗
    if (showSearchBar) {
        SearchDialog(
            searchQuery = searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            searchResults = searchResults,
            searchStats = viewModel.searchStats.collectAsState().value,
            searchStartDate = viewModel.searchStartDate.collectAsState().value,
            searchEndDate = viewModel.searchEndDate.collectAsState().value,
            onDismiss = {
                showSearchBar = false
                viewModel.clearSearch()
                viewModel.clearSearchDateRange()
            },
            onTransactionClick = { txn ->
                onEditClick(txn.id)
            },
            onDateRangeChange = { start, end ->
                viewModel.setSearchDateRange(start, end)
            },
            categoryIconMap = categoryIconMap
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ),
                navigationIcon = {
                    if (isManageMode) {
                        IconButton(onClick = { viewModel.exitManageMode() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                title = {},
                actions = {
                    if (isManageMode) {
                        // 管理模式:不显示操作图标
                    } else {
                        // 6 个操作按钮均匀分布,左右对齐
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.forceRefresh() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "刷新")
                            }
                            IconButton(onClick = { showQuickEntryDialog = true }) {
                                Icon(Icons.Default.Quickreply, contentDescription = "快速记账")
                            }
                            IconButton(onClick = { showSearchBar = true }) {
                                Icon(Icons.Default.Search, contentDescription = "搜索")
                            }
                            IconButton(onClick = { onCategoryClick() }) {
                                Icon(Icons.Default.Category, contentDescription = "分类管理")
                            }
                            IconButton(onClick = { viewModel.enterManageMode() }) {
                                Icon(Icons.Default.Checklist, contentDescription = "管理交易")
                            }
                            IconButton(onClick = { showDarkModeDialog = true }) {
                                Icon(Icons.Default.DarkMode, contentDescription = "深色模式")
                            }
                        }
                    }
                }
            )
        },

        bottomBar = {
            if (isManageMode) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧:全选
                        TextButton(onClick = {
                            val allIds = displayList.map { it.id }
                            if (selectedIds.size == allIds.size) viewModel.clearSelection()
                            else viewModel.selectAll(allIds)
                        }) {
                            Icon(
                                if (selectedIds.size == displayList.size && displayList.isNotEmpty()) Icons.Default.Deselect
                                else Icons.Default.SelectAll,
                                contentDescription = "全选",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (selectedIds.size == displayList.size && displayList.isNotEmpty()) "取消全选" else "全选",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        // 右侧:修改分类 + 删除 + 取消
                        Row {
                            // 批量修改分类
                            TextButton(
                                onClick = { showCategoryBatchDialog = true },
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "修改分类",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "分类",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.secondary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                            // 批量修改日期
                            TextButton(
                                onClick = {
                                    batchEditDate = System.currentTimeMillis()
                                    showDateBatchDialog = true
                                },
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = "修改日期",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "日期",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                            TextButton(
                                onClick = { showBatchDeleteDialog = true },
                                enabled = selectedIds.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "删除",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selectedIds.isNotEmpty()) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                            TextButton(onClick = { viewModel.exitManage() }) {
                                Icon(Icons.Default.Close, contentDescription = "取消", modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("取消", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            } else {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "透视") },
                        label = { Text("透视") },
                        selected = false,
                        onClick = onReportClick
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "预算") },
                        label = { Text("预算") },
                        selected = false,
                        onClick = onBudgetClick
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Storage, contentDescription = "数据") },
                        label = { Text("数据") },
                        selected = false,
                        onClick = onDataClick
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
                        label = { Text("设置") },
                        selected = false,
                        onClick = onSettingsClick
                    )
                }
            }
        },
        floatingActionButton = {
            if (!isManageMode) {
                Column(horizontalAlignment = Alignment.End) {
                    // AI 悬浮按钮
                    if (aiFloatingVisible) {
                        var offsetX by remember { mutableFloatStateOf(0f) }
                        var offsetY by remember { mutableFloatStateOf(0f) }
                        val density = LocalDensity.current
                        Box(
                            modifier = Modifier
                                .offset(x = offsetX.dp, y = offsetY.dp)
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                    )
                                )
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        offsetX += dragAmount.x / density.density
                                        offsetY += dragAmount.y / density.density
                                    }
                                }
                                .clickable { onAiClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            AiIcon(size = 32.dp, contentColor = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    // 记账按钮:点击添加,长按语音记账
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                )
                            )
                            .combinedClickable(
                                onClick = { onAddClick() },
                                onLongClick = { showQuickEntryDialog = true }                       ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "记账(长按语音)",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
        ) { padding ->
        // 使用 Box 包裹,让 AI 按钮可以悬浮在最上层
        Box(modifier = Modifier.fillMaxSize()) {
            // ===== 主内容区域 =====
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // 顶部渐变装饰
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                )
                // 主内容继续...
            // █████ 日期周期导航栏 █████
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ← 上周期
                IconButton(modifier = Modifier.padding(start = 4.dp), onClick = { viewModel.homePrevPeriod() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上周期")
                }

                // 中间:周期标签,点击弹出日期范围选择器
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDateRangePicker = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val label = when (homePeriodMode) {
                            HomePeriodMode.CUSTOM -> homePeriodLabel
                            HomePeriodMode.MONTH -> "$currentMonth"
                        }
                        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "选择日期范围",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        if (homePeriodMode == HomePeriodMode.CUSTOM) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "回到整月",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { viewModel.clearHomePeriod() }
                            )
                        }
                    }
                }

                // → 下周期
                IconButton(modifier = Modifier.padding(end = 4.dp), onClick = { viewModel.homeNextPeriod() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下周期")
                }
            }

            // 搜索结果提示
            if (showSearchBar && searchQuery.isNotBlank()) {
                Text(
                    "找到 ${searchResults.size} 条结果",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // 汇总卡片
            SummaryCard(summary = summary)

            // 交易列表
            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("搜索中...", style = MaterialTheme.typography.bodyLarge)
                }
            } else if (displayList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📝", style = MaterialTheme.typography.displayLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (showSearchBar) "没有找到匹配的记录" else "该时间段还没有记账",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayList, key = { it.id }) { txn ->
                        SwipeableTransactionItem(
                            transaction = txn,
                            categoryIconMap = categoryIconMap,
                            isSelected = txn.id in selectedIds,
                            isManageMode = isManageMode,
                            onClick = {
                                if (isManageMode) viewModel.toggleSelect(txn.id)
                                else onEditClick(txn.id)
                            },
                            onEdit = { onEditClick(txn.id) },
                            onDelete = { showDeleteDialog = txn.id }
                        )
                    }
                }
            }
        }


        } // Box 结束
    } // Scaffold content 结束

    // ── 单条删除第一次确认 ──
    showDeleteDialog?.let { txnId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除记录") },
            text = { Text("确定要删除这条记录吗?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = null
                    showDeleteSecondConfirm = txnId
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("取消") }
            }
        )
    }

    // ── 单条删除第二次确认 ──
    showDeleteSecondConfirm?.let { txnId ->
        AlertDialog(
            onDismissRequest = { showDeleteSecondConfirm = null },
            title = { Text("再次确认") },
            text = { Text("此操作不可恢复,确定删除?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(txnId)
                    Unit
                    showDeleteSecondConfirm = null
                }) { Text("确认删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSecondConfirm = null }) { Text("取消") }
            }
        )
    }



    // ── 批量删除第一次确认 ──
    if (showBatchDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text("批量删除") },
            text = { Text("确定删除选中的 ${selectedIds.size} 条记录?") },
            confirmButton = {
                TextButton(onClick = {
                    showBatchDeleteDialog = false
                    showBatchDeleteSecondConfirm = true
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    // ── 批量删除第二次确认 ──
    if (showBatchDeleteSecondConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteSecondConfirm = false },
            title = { Text("再次确认") },
            text = { Text("此操作不可恢复,确定删除 ${selectedIds.size} 条记录?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSelected()
                    Unit
                    showBatchDeleteSecondConfirm = false
                }) { Text("确认删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteSecondConfirm = false }) { Text("取消") }
            }
        )
    }

    // ══ 批量修改分类对话框 ══
    if (showCategoryBatchDialog) {
        val allCats = expenseCategories
        var searchCatQuery by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCategoryBatchDialog = false },
            title = { Text("批量修改分类(${selectedIds.size}条)") },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp)) {
                    OutlinedTextField(
                        value = searchCatQuery,
                        onValueChange = { searchCatQuery = it },
                        placeholder = { Text("搜索分类...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    Spacer(Modifier.height(8.dp))
                    val filtered = if (searchCatQuery.isBlank()) allCats
                    else allCats.filter {
                        it.name.contains(searchCatQuery, ignoreCase = true) ||
                        (it.parentName?.contains(searchCatQuery, ignoreCase = true) == true)
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(filtered, key = { it.id }) { cat ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    viewModel.batchEditCategory(cat.name, cat.icon)
                                    showCategoryBatchDialog = false
                                },
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CategoryIcon(icon = cat.icon)
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                                        if (cat.parentName != null) {
                                            Text(
                                                cat.parentName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryBatchDialog = false }) { Text("取消") }
            }
        )
    }

    // ══ 批量修改日期对话框（滚动式年/月/日选择） ══
    if (showDateBatchDialog) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = batchEditDate
        var selYear by remember(batchEditDate) { mutableStateOf(cal.get(Calendar.YEAR)) }
        var selMonth by remember(batchEditDate) { mutableStateOf(cal.get(Calendar.MONTH)) }
        var selDay by remember(batchEditDate) { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }

        AlertDialog(
            onDismissRequest = { showDateBatchDialog = false },
            title = { Text("批量修改日期(${selectedIds.size}条)") },
            text = {
                DatePickerContent(
                    year = selYear, month = selMonth, day = selDay,
                    onYearChange = { selYear = it },
                    onMonthChange = { selMonth = it },
                    onDayChange = { selDay = it }
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = { showDateBatchDialog = false }) { Text("取消") }
                    TextButton(onClick = {
                        val ms = Calendar.getInstance().apply { set(selYear, selMonth, selDay, 0, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                        viewModel.batchEditDate(ms)
                        showDateBatchDialog = false
                    }) { Text("确定") }
                }
            }
        )
    }

    // 快速记账对话框
    if (showQuickEntryDialog) {
        QuickEntryDialog(
            text = quickEntryText,
            onTextChange = { quickEntryText = it },
            onDismiss = { showQuickEntryDialog = false; quickEntryText = "" },
            onConfirm = { text ->
                showQuickEntryDialog = false
                quickEntryText = ""
                val parsed = parseQuickEntry(text,
                    if (isExpenseForQuick) expenseCategories else incomeCategories,
                    isExpenseForQuick)
                if (parsed != null) {
                    val (amount, cat, note) = parsed
                    viewModel.addTransaction(
                        amount = amount,
                        categoryName = (cat?.name ?: if (isExpenseForQuick) "其他" else "其他"),
                        categoryIcon = (cat?.icon ?: "📌"),
                        source = Source.MANUAL,
                        note = note,
                        date = System.currentTimeMillis()
                    )
                    viewModel.forceRefresh()
                }
            },
            categories = if (isExpenseForQuick) expenseCategories else incomeCategories,
            isExpense = isExpenseForQuick,
            onToggleType = { isExpenseForQuick = !isExpenseForQuick }
        )
    }

    // ── 深色模式弹窗 ──
    if (showDarkModeDialog) {
        val followSystem by userPreferences.followSystem.collectAsState(initial = true)
        val currentDarkMode by userPreferences.darkMode.collectAsState(initial = false)
        var selectedFollowSystem by remember { mutableStateOf(followSystem) }
        var selectedDarkMode by remember { mutableStateOf(currentDarkMode) }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = { showDarkModeDialog = false },
            title = { Text("深色模式") },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFollowSystem,
                            onClick = { selectedFollowSystem = true },
                            label = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "跟随系统",
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        FilterChip(
                            selected = !selectedFollowSystem && !selectedDarkMode,
                            onClick = {
                                selectedFollowSystem = false
                                selectedDarkMode = false
                            },
                            label = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("浅色", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                        FilterChip(
                            selected = !selectedFollowSystem && selectedDarkMode,
                            onClick = {
                                selectedFollowSystem = false
                                selectedDarkMode = true
                            },
                            label = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("深色", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selectedFollowSystem) {
                        scope.launch { userPreferences.setFollowSystem(true) }
                    } else {
                        scope.launch { userPreferences.setDarkMode(selectedDarkMode) }
                    }
                    showDarkModeDialog = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDarkModeDialog = false }) { Text("取消") }
            }
        )
    }

    // █████ 日期范围选择对话框(快捷周期 + 自定义起止日期) █████
    if (showDateRangePicker) {
        DateRangePickerDialog(
            onDismiss = { showDateRangePicker = false },
            onSelectWeek = {
                val (start, end) = getWeekRangeForDate(System.currentTimeMillis())
                viewModel.setHomeDateRange(start, end)
                showDateRangePicker = false
            },
            onSelectMonth = {
                viewModel.clearHomePeriod()
                showDateRangePicker = false
            },
            onSelectRange = { start, end ->
                viewModel.setHomeDateRange(start, end)
                showDateRangePicker = false
            }
        )
    }
}

/** 获取某天所在周(周一~周日)的起止毫秒 */
private fun getWeekRangeForDate(dateMillis: Long): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val start = normalizeDayStart(cal.timeInMillis)
    cal.add(Calendar.DAY_OF_YEAR, 6)
    val end = normalizeDayEnd(cal.timeInMillis)
    return start to end
}

private fun normalizeDayStart(ts: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun normalizeDayEnd(ts: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = ts }
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 999)
    return cal.timeInMillis
}

// ─── 日期范围选择对话框(快捷周期 + 自定义) ───
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onSelectWeek: () -> Unit,
    onSelectMonth: () -> Unit,
    onSelectRange: (start: Long, end: Long) -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    if (showCustomPicker) {
        // ── 自定义起止日期:滚动式年/月/日选择，同报表风格 ──
        val cal = Calendar.getInstance()
        var startYear by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
        var startMonth by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
        var startDay by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
        var endYear by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
        var endMonth by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
        var endDay by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
        var selectedTab by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = { showCustomPicker = false },
            title = { Text("选择日期范围") },
            text = {
                Column {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("开始日期") })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("结束日期") })
                    }
                    Spacer(Modifier.height(16.dp))

                    if (selectedTab == 0) {
                        DatePickerContent(
                            year = startYear, month = startMonth, day = startDay,
                            onYearChange = { startYear = it },
                            onMonthChange = { startMonth = it },
                            onDayChange = { startDay = it }
                        )
                    } else {
                        DatePickerContent(
                            year = endYear, month = endMonth, day = endDay,
                            onYearChange = { endYear = it },
                            onMonthChange = { endMonth = it },
                            onDayChange = { endDay = it }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val sc = Calendar.getInstance().apply { set(startYear, startMonth, startDay, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                    val ec = Calendar.getInstance().apply { set(endYear, endMonth, endDay, 23, 59, 59); set(Calendar.MILLISECOND, 999) }
                    onSelectRange(sc.timeInMillis, ec.timeInMillis)
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPicker = false }) { Text("取消") }
            }
        )
    } else {
        // ── 快捷选择页 ──
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("选择日期范围") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 本周
                    Button(
                        onClick = onSelectWeek,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("本周")
                    }
                    // 本月
                    Button(
                        onClick = onSelectMonth,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("本月")
                    }
                    // 自定义
                    OutlinedButton(
                        onClick = { showCustomPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("自定义起止日期")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SummaryCard(summary: MonthlySummary) {
    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error
    val dailyExpense = if (summary.dailyAvg.avgDailyExpense > 0) "¥%.2f/天".format(summary.dailyAvg.avgDailyExpense) else "-"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📥 收入", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    Text("¥%.2f".format(summary.income),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = incomeColor)
                }
                Box(Modifier.width(1.dp).height(48.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📤 支出", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    Text("¥%.2f".format(summary.expense),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold, color = expenseColor)
                }
                Box(Modifier.width(1.dp).height(48.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊 笔数", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    Text("${summary.transactionCount}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("日均消费 $dailyExpense", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun TransactionItem(
    transaction: Transaction,
    categoryIconMap: Map<String, String>,
    isSelected: Boolean,
    isManageMode: Boolean,
    onClick: () -> Unit,
    /* onDelete: () -> Unit */ // unused
) {
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val isExpense = transaction.amount < 0
    val amountColor = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    // 实时查询最新分类图标,优先使用 categoryIconMap 中的最新配置
    val liveIcon = categoryIconMap[transaction.categoryName] ?: transaction.categoryIcon

    Box(modifier = Modifier.fillMaxWidth()) {
        // Card 完全不动,和正常模式一模一样
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) { CategoryIcon(icon = liveIcon, size = 28) }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(transaction.categoryName, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (transaction.source == Source.MANUAL) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.secondaryContainer
                        ) { Text(" ${transaction.source.label} ", style = MaterialTheme.typography.labelSmall) }
                    }
                    if (transaction.note.isNotBlank()) {
                        Text(transaction.note.take(20), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (isExpense) "-¥%.2f".format(kotlin.math.abs(transaction.amount))
                        else "+¥%.2f".format(transaction.amount),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = amountColor
                    )
                    Text(dateFormat.format(Date(transaction.date)), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // 始终显示 Edit 按钮,不改动内部控件
                IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        // 复选框悬浮在卡片上,用 offset 微调位置
        if (isManageMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-20).dp)
                    .size(36.dp)
            )
        }
    }
}


// ===== 快速记账对话框 =====
@Composable
private fun QuickEntryDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    categories: List<CategoryData>,
    isExpense: Boolean,
    onToggleType: () -> Unit
) {
    var showPreview by remember { mutableStateOf(false) }
    var previewAmount by remember { mutableStateOf(0.0) }
    var previewCategory by remember { mutableStateOf<CategoryData?>(null) }
    var previewNote by remember { mutableStateOf("") }
    val quickCtx = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    val quickRecognizer = remember { XunfeiSpeechRecognizer(quickCtx) }
    // val quickScope = rememberCoroutineScope() // unused
    var volumeLevel by remember { mutableStateOf(0) }

    LaunchedEffect(isListening) {
        if (!isListening) return@LaunchedEffect
        launch { quickRecognizer.partialResult.collect { partial -> if (partial.isNotEmpty()) onTextChange(partial) } }
        launch {
            quickRecognizer.recognitionResult.collect { result ->
                if (!result.isNullOrEmpty()) {
                    onTextChange(result)
                    isListening = false; isPaused = false; quickRecognizer.stopListening()
                    delay(300)
                    val parsed = parseQuickEntry(result, categories, isExpense)
                    if (parsed != null) { previewAmount = parsed.first; previewCategory = parsed.second; previewNote = parsed.third; showPreview = true }
                    Toast.makeText(quickCtx, "识别完成", Toast.LENGTH_SHORT).show()
                }
            }
        }
        launch { quickRecognizer.recognitionError.collect { err -> if (err != null) { isListening = false; isPaused = false; Toast.makeText(quickCtx, err, Toast.LENGTH_LONG).show() } } }
        launch { quickRecognizer.volumeLevel.collect { vol -> volumeLevel = vol } }
        launch { delay(30000); if (isListening) { isListening = false; isPaused = false; quickRecognizer.stopListening(); Toast.makeText(quickCtx, "语音识别超时", Toast.LENGTH_SHORT).show() } }
    }

    fun doStartVoice() {
        if (!XunfeiSpeechRecognizer.isSdkAvailable()) {
            Toast.makeText(quickCtx, XunfeiSpeechRecognizer.getInitError() ?: "讯飞SDK未初始化", Toast.LENGTH_LONG).show()
            return
        }
        isListening = true; isPaused = false
        quickRecognizer.startListening(dialect = "普通话", retries = 2, continuous = false)
        val errSnapshot = quickRecognizer.recognitionError.value
        if (errSnapshot != null) { isListening = false; Toast.makeText(quickCtx, errSnapshot, Toast.LENGTH_LONG).show(); return }
        Toast.makeText(quickCtx, "请说话...", Toast.LENGTH_SHORT).show()
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (ContextCompat.checkSelfPermission(quickCtx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) doStartVoice()
        } else {
            Toast.makeText(quickCtx, "需要麦克风权限才能语音记账", Toast.LENGTH_LONG).show()
        }
    }

    fun togglePause() {
        if (isPaused) { quickRecognizer.resumeListening(); isPaused = false }
        else { quickRecognizer.pauseListening(); isPaused = true }
    }

    fun stopVoice() { quickRecognizer.stopListening(); isListening = false; isPaused = false; Toast.makeText(quickCtx, "已停止", Toast.LENGTH_SHORT).show() }
    fun cancelVoice() { quickRecognizer.cancelListening(); isListening = false; isPaused = false; Toast.makeText(quickCtx, "已取消", Toast.LENGTH_SHORT).show() }

    fun startQuickVoice() {
        if (isListening) { stopVoice(); return }
        if (ContextCompat.checkSelfPermission(quickCtx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO); return
        }
        doStartVoice()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("快速记账", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(selected = isExpense, onClick = { if (!isExpense) onToggleType() }, label = { Text("支出", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.height(28.dp))
                    FilterChip(selected = !isExpense, onClick = { if (isExpense) onToggleType() }, label = { Text("收入", style = MaterialTheme.typography.labelSmall) }, modifier = Modifier.height(28.dp))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        onTextChange(it)
                        val parsed = parseQuickEntry(it, categories, isExpense)
                        if (parsed != null) { previewAmount = parsed.first; previewCategory = parsed.second; previewNote = parsed.third; showPreview = true }
                        else { showPreview = false }
                    },
                    placeholder = { Text(if (isExpense) "例: 早餐15元、滴滴打车20、淘宝衣服99" else "例: 工资5000、红包100、退款50") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (isListening) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { togglePause() }) {
                                    Icon(imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (isPaused) "恢复" else "暂停",
                                        tint = if (isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { stopVoice() }) {
                                    Icon(imageVector = Icons.Default.Stop, contentDescription = "停止", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        } else {
                            IconButton(onClick = { startQuickVoice() }) {
                                Icon(imageVector = Icons.Default.Mic, contentDescription = "语音输入", tint = LocalContentColor.current)
                            }
                        }
                    }
                )

                // 语音录制状态条
                AnimatedVisibility(visible = isListening) {
                    Surface(color = if (isPaused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            // 音波动画
                            if (!isPaused) {
                                Row(modifier = Modifier.fillMaxWidth().height(32.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                                    val nowMillis = System.currentTimeMillis()
                                    for (i in 0 until 8) {
//                                         val volFactor = if (volumeLevel > 0) 1f else 0.3f // unused
                                        val barH = (6 + volumeLevel * 3) * (0.5f + 0.5f * kotlin.math.sin((nowMillis / 200.0 + i * 0.8).toFloat()).coerceIn(0.3f, 1.0f))
                                        Box(modifier = Modifier.width(4.dp).height((barH).dp.coerceIn(4.dp, 32.dp)).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(text = if (isPaused) "已暂停，点击▶继续录音" else "正在聆听...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPaused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                TextButton(onClick = { cancelVoice() }) { Text("取消", style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }
                }

                if (showPreview) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("解析预览", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isExpense) "-¥%.2f".format(kotlin.math.abs(previewAmount)) else "+¥%.2f".format(previewAmount),
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                    color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                previewCategory?.let { cat -> Text("${cat.icon} ${cat.name}", style = MaterialTheme.typography.bodySmall) }
                                if (previewNote.isNotBlank()) { Spacer(Modifier.width(8.dp)); Text(previewNote.take(16), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
                Text("点击麦克风语音记账，支持暂停/恢复录制", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }, enabled = text.isNotBlank()) { Text("确认记账") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
private data class ParseResult(
    val amount: Double,
    val category: CategoryData?,
    val note: String
)

/**
 * 解析自然语言文本为记账信息
 * 支持格式:
 *   - "早餐15元" → 金额=15, 分类=餐饮→早餐, 备注=早餐
 *   - "滴滴打车20块" → 金额=20, 分类=交通→打车, 备注=滴滴打车
 *   - "工资5000" → 金额=5000, 分类=工资, 备注=工资
 *   - "淘宝买衣服99" → 金额=99, 分类=购物, 备注=淘宝买衣服
 *   - "15" → 金额=15, 分类=null(需用户选择)
 */
private fun parseQuickEntry(
    raw: String,
    categories: List<CategoryData>,
    isExpense: Boolean
): Triple<Double, CategoryData?, String>? {
    if (raw.isBlank()) return null

    val text = raw.trim()

    // 1. 提取金额 - 支持多种格式: 15元, 20块, 15.5, 15.00
    val amountPattern = Regex("""(\d+(?:\.\d{1,2})?)\s*(?:元|块|毛|分|块钱)?""")
    val amountMatch = amountPattern.find(text)
    val amount = amountMatch?.groupValues?.get(1)?.toDoubleOrNull() ?: return null

    if (amount <= 0) return null

    val signedAmount = if (isExpense) -amount else amount

    // 2. 提取备注(移除金额部分)
    val note = text.replace(amountPattern, "").trim()
        .replace(Regex("""[元块钱毛分]{1,2}$"""), "")
        .trim()

    // 3. 智能分类匹配
    val matchedCategory = findBestCategory(note, categories)

    return Triple(signedAmount, matchedCategory, note)
}

/**
 * 根据自然语言文本自动匹配最佳分类
 * 覆盖衣食住行吃喝玩乐等生活场景,核心原则:
 *   1. 优先匹配双字/多字关键词,避免单字误匹配
 *   2. 层级清晰:大类(父分类)→ 细分(子分类)
 *   3. 无匹配时不做危险回退(不取第一个分类)
 */
private fun findBestCategory(text: String, categories: List<CategoryData>): CategoryData? {
    if (text.isBlank() || categories.isEmpty()) return null

    val lower = text.lowercase()

    // ─── 场景化关键词规则 ───
    // Pair<关键词列表, Pair<父分类名, 子分类名?>>
    data class Rule(val keywords: List<String>, val parent: String, val sub: String? = null)

    val rules = listOf(
        // ═══ 食 ═══
        // 一日三餐
        Rule(listOf("早餐", "早饭", "包子", "豆浆", "油条", "粥", "肠粉", "烧卖", "饺子", "馄饨", "煎饼", "灌饼", "茶叶蛋", "蒸饺"), "餐饮", "早餐"),
        Rule(listOf("午餐", "午饭", "中饭", "快餐", "套餐", "食堂", "盒饭", "便当", "桶饭"), "餐饮", "午餐"),
        Rule(listOf("晚餐", "晚饭", "晚饭"), "餐饮", "晚餐"),
        // 主食/面食
        Rule(listOf("面条", "米粉", "米线", "河粉", "螺蛳粉", "酸辣粉", "凉皮", "热干面", "炸酱面", "拌面"), "餐饮", "早/午/晚"),
        Rule(listOf("米饭", "炒饭", "盖饭", "拌饭", "煲仔饭", "盖浇饭", "卤肉饭"), "餐饮", "午餐"),
        // 餐饮品牌
        Rule(listOf("汉堡", "薯条", "炸鸡", "披萨", "肯德基", "麦当劳", "必胜客", "华莱士", "德克士", "塔斯汀"), "餐饮", "午餐"),
        Rule(listOf("火锅", "烧烤", "烤肉", "麻辣烫", "串串", "麻辣香锅", "冒菜", "关东煮"), "餐饮", "晚餐"),
        Rule(listOf("日料", "寿司", "刺身", "拉面", "乌冬", "居酒屋", "鳗鱼饭"), "餐饮", "午餐"),
        Rule(listOf("外卖", "美团外卖", "饿了么", "食", "餐厅", "饭店", "美食", "小吃"), "餐饮"),
        // 饮品
        Rule(listOf("咖啡", "瑞幸", "星巴克", "库迪", "奶茶", "喜茶", "蜜雪", "茶百道", "古茗", "沪上"), "餐饮", "零食"),
        Rule(listOf("饮料", "可乐", "雪碧", "果汁", "汽水", "矿泉水", "红牛", "东鹏"), "餐饮", "零食"),
        // 零食果品
        Rule(listOf("水果", "香蕉", "苹果", "葡萄", "草莓", "西瓜", "橙子", "橘子", "芒果", "梨", "桃", "榴莲", "荔枝", "龙眼", "哈密瓜", "提子"), "餐饮", "水果"),
        Rule(listOf("零食", "面包", "蛋糕", "饼干", "甜品", "冰淇淋", "薯片", "巧克力", "膨化"), "餐饮", "零食"),
        // 超市/生鲜(购物子分类)
        Rule(listOf("买菜", "生鲜", "朴朴", "叮咚", "每日优鲜"), "购物", "超市"),
        Rule(listOf("超市", "便利店", "盒马", "山姆", "沃尔玛", "家乐福", "大润发", "永旺"), "购物", "超市"),

        // ═══ 衣 ═══
        Rule(listOf("衣服", "上衣", "裤子", "裙子", "外套", "衬衫", "T恤", "T恤衫", "卫衣", "毛衣", "羽绒服", "大衣", "睡衣"), "购物"),
        Rule(listOf("鞋", "运动鞋", "皮鞋", "靴子", "凉鞋", "拖鞋", "球鞋"), "购物"),
        Rule(listOf("包包", "书包", "背包", "挎包", "钱包"), "购物"),
        Rule(listOf("帽子", "袜子", "围巾", "手套", "腰带"), "购物"),
        // 服饰电商
        Rule(listOf("买衣服", "买鞋", "淘宝", "天猫", "唯品会", "得物"), "购物"),
        Rule(listOf("京东"), "购物"),
        Rule(listOf("拼多多", "PDD"), "购物"),

        // ═══ 住 ═══
        Rule(listOf("房租", "租金", "押金"), "房屋水电", "房租"),
        Rule(listOf("水费", "电费", "燃气", "煤气"), "房屋水电", "水电"),
        Rule(listOf("物业", "物业费"), "房屋水电"),
        Rule(listOf("暖气", "取暖"), "房屋水电"),
        Rule(listOf("宽带", "光纤"), "通信", "宽带"),
        Rule(listOf("话费", "流量", "手机费", "5G套餐"), "通信", "充值"),

        // ═══ 行 ═══
        // 日常通勤
        Rule(listOf("地铁", "公交", "公交卡", "通勤", "巴士"), "交通", "地铁"),
        Rule(listOf("打车", "滴滴", "快车", "出租车", "网约车", "高德打车", "花小猪"), "交通", "打车"),
        // 跨城出行
        Rule(listOf("高铁", "火车", "动车", "机票", "飞机", "特价机票"), "交通", "长途"),
        Rule(listOf("长途", "大巴", "客运", "顺风车"), "交通", "长途"),
        // 自驾
        Rule(listOf("加油", "加油站", "汽油", "柴油"), "交通", "加油"),
        Rule(listOf("充电", "充电桩"), "交通", "加油"),
        Rule(listOf("停车", "停车场", "停车费"), "交通", "停车"),
        Rule(listOf("ETC", "高速", "过路费"), "交通"),

        // ═══ 玩 ═══
        Rule(listOf("电影", "影院", "电影票", "演出", "演唱会", "话剧", "音乐节"), "消费", "娱乐"),
        Rule(listOf("KTV", "唱歌"), "消费", "娱乐"),
        Rule(listOf("游戏", "Steam", "点卡", "手游", "皮肤"), "消费", "娱乐"),
        Rule(listOf("旅游", "旅行", "酒店", "民宿", "门票", "景区", "度假"), "消费", "娱乐"),
        Rule(listOf("健身", "运动", "瑜伽", "游泳", "跑步"), "消费"),
        Rule(listOf("学习", "教育", "书籍", "文具", "培训", "课程", "考研"), "消费"),
        Rule(listOf("会员", "VIP", "视频会员", "音乐会员", "订阅"), "消费"),

        // ═══ 日用百货 ═══
        Rule(listOf("纸巾", "洗衣", "牙刷", "牙膏", "洗发", "沐浴", "洗面奶", "肥皂", "洗衣液", "柔顺剂", "垃圾袋", "厨房纸"), "日用"),

        // ═══ 美妆美容 ═══
        Rule(listOf("化妆品", "护肤品", "面膜", "口红", "粉底", "眼影", "卸妆", "防晒", "爽肤水", "乳液", "精华"), "美容"),
        Rule(listOf("美发", "理发", "剪发", "染发"), "美容"),

        // ═══ 数码电子 ═══
        Rule(listOf("手机", "电脑", "平板", "笔记本", "耳机", "充电器", "数据线", "充电宝", "移动硬盘", "U盘", "内存卡"), "数码"),
        Rule(listOf("鼠标", "键盘", "屏幕", "显示器", "路由器", "摄像头"), "数码"),

        // ═══ 医疗健康 ═══
        Rule(listOf("医院", "看病", "挂号", "体检", "医生", "门诊"), "医疗"),
        Rule(listOf("药店", "买药", "处方药", "感冒药", "消炎药", "创可贴", "口罩"), "医疗"),

        // ═══ 话费通信 ═══
        Rule(listOf("话费", "流量", "充值"), "通信", "充值"),
        Rule(listOf("联通", "移动", "电信"), "通信", "充值"),

        // ═══ 转账/资金 ═══
        Rule(listOf("转账", "转给", "转出"), "转账"),
        Rule(listOf("工资", "奖金", "补贴", "收入", "薪水"), "工资"),
        Rule(listOf("红包", "收款", "收到钱"), "红包"),
        Rule(listOf("退款", "退费", "退回", "退货"), "退款"),
    )

    // 按规则匹配(顺序即优先级)
    for (rule in rules) {
        // 只匹配包含完整关键字的情况,不匹配单字片段
        if (rule.keywords.any { kw -> lower.contains(kw, ignoreCase = true) }) {
            val parentName = rule.parent
            val subName = rule.sub

            // 优先精确匹配:子分类
            if (subName != null) {
                val exact = categories.find { it.name == subName && it.parentName == parentName }
                if (exact != null) return exact
            }

            // 匹配父分类
            val parent = categories.find { it.name == parentName && it.parentName.isNullOrEmpty() }
            if (parent != null) return parent

            // 回退:父分类下的第一个非隐藏子分类
            val anySub = categories.find { it.parentName == parentName && !it.isHidden }
            if (anySub != null) return anySub
        }
    }
    return null
}


/**
 * 全屏搜索弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchDialog(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<Transaction>,
    searchStats: com.autobookkeeper.viewmodel.SearchStats,
    searchStartDate: Long?,
    searchEndDate: Long?,
    onDismiss: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onDateRangeChange: (Long?, Long?) -> Unit,
    categoryIconMap: Map<String, String>
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            SearchDialogContent(
                searchQuery = searchQuery,
                onQueryChange = onQueryChange,
                searchResults = searchResults,
                searchStats = searchStats,
                searchStartDate = searchStartDate,
                searchEndDate = searchEndDate,
                onDismiss = onDismiss,
                onTransactionClick = onTransactionClick,
                onDateRangeChange = onDateRangeChange,
                categoryIconMap = categoryIconMap
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchDialogContent(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<Transaction>,
    searchStats: com.autobookkeeper.viewmodel.SearchStats,
    searchStartDate: Long?,
    searchEndDate: Long?,
    onDismiss: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onDateRangeChange: (Long?, Long?) -> Unit,
    categoryIconMap: Map<String, String>
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        placeholder = { Text("搜索分类/备注/金额...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    )
                },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (searchQuery.isBlank()) {
                // 空搜索状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "输入关键词搜索交易",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else if (searchResults.isEmpty()) {
                // 无结果状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "未找到相关交易",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                // 搜索结果列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 日期筛选栏
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = if (searchStartDate != null && searchEndDate != null) {
                                            "${dateFormat.format(Date(searchStartDate))} 至 ${dateFormat.format(Date(searchEndDate))}"
                                        } else {
                                            "全部时间"
                                        },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                TextButton(onClick = { showDatePicker = true }) {
                                    Text(if (searchStartDate != null) "修改" else "筛选")
                                }
                            }
                        }
                    }

                    // 统计卡片
                    item {
                        SearchStatsCard(stats = searchStats)
                    }

                    item {
                        Text(
                            text = "找到 ${searchResults.size} 条结果",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                        )
                    }

                    items(searchResults) { transaction ->
                        SearchResultItem(
                            transaction = transaction,
                            onClick = { onTransactionClick(transaction) },
                            icon = categoryIconMap[transaction.categoryName] ?: transaction.categoryIcon
                        )
                    }
                }
            }
        }
    }

    // 日期选择器
    if (showDatePicker) {
        SearchDateRangePicker(
            initialStart = searchStartDate,
            initialEnd = searchEndDate,
            onConfirm = { start, end ->
                onDateRangeChange(start, end)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * 搜索结果统计卡片
 */
@Composable
private fun SearchStatsCard(stats: com.autobookkeeper.viewmodel.SearchStats) {
    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 收入
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "📥 收入",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "¥%.2f".format(stats.totalIncome),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = incomeColor
                    )
                    Text(
                        "${stats.incomeCount} 笔",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    Modifier
                        .width(1.dp)
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )

                // 支出
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "📤 支出",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "¥%.2f".format(stats.totalExpense),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = expenseColor
                    )
                    Text(
                        "${stats.expenseCount} 笔",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    Modifier
                        .width(1.dp)
                        .height(56.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                )

                // 结余
                val net = stats.totalIncome - stats.totalExpense
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "💰 结余",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "¥%.2f".format(net),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (net >= 0) incomeColor else expenseColor
                    )
                    Text(
                        "${stats.incomeCount + stats.expenseCount} 笔",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 搜索日期范围选择器 — 滚动式年/月/日选择，同报表风格
 */
@Composable
private fun SearchDateRangePicker(
    initialStart: Long?,
    initialEnd: Long?,
    onConfirm: (Long?, Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val cal = Calendar.getInstance()
    var startYear by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
    var startMonth by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
    var startDay by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var endYear by remember { mutableStateOf(cal.get(Calendar.YEAR)) }
    var endMonth by remember { mutableStateOf(cal.get(Calendar.MONTH)) }
    var endDay by remember { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }
    var selectedTab by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期范围") },
        text = {
            Column {
                // 快捷选项
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = false,
                        onClick = { onConfirm(null, null) },
                        label = { Text("全部") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val c = Calendar.getInstance()
                            c.set(Calendar.DAY_OF_MONTH, 1)
                            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                            val s = c.timeInMillis
                            c.add(Calendar.MONTH, 1); c.add(Calendar.MILLISECOND, -1)
                            onConfirm(s, c.timeInMillis)
                        },
                        label = { Text("本月") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val c = Calendar.getInstance()
                            c.add(Calendar.MONTH, -1)
                            c.set(Calendar.DAY_OF_MONTH, 1)
                            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                            val s = c.timeInMillis
                            c.add(Calendar.MONTH, 1); c.add(Calendar.MILLISECOND, -1)
                            onConfirm(s, c.timeInMillis)
                        },
                        label = { Text("上月") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("开始日期") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("结束日期") })
                }
                Spacer(Modifier.height(16.dp))

                if (selectedTab == 0) {
                    DatePickerContent(
                        year = startYear, month = startMonth, day = startDay,
                        onYearChange = { startYear = it },
                        onMonthChange = { startMonth = it },
                        onDayChange = { startDay = it }
                    )
                } else {
                    DatePickerContent(
                        year = endYear, month = endMonth, day = endDay,
                        onYearChange = { endYear = it },
                        onMonthChange = { endMonth = it },
                        onDayChange = { endDay = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val sc = Calendar.getInstance().apply { set(startYear, startMonth, startDay, 0, 0, 0); set(Calendar.MILLISECOND, 0) }
                val ec = Calendar.getInstance().apply { set(endYear, endMonth, endDay, 23, 59, 59); set(Calendar.MILLISECOND, 999) }
                onConfirm(sc.timeInMillis, ec.timeInMillis)
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultItem(
    transaction: Transaction,
    onClick: () -> Unit,
    icon: String
) {
    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }
    val amountColor = if (transaction.amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 22.sp)
            }

            Spacer(Modifier.width(12.dp))

            // 中间信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (transaction.note.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(Date(transaction.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // 金额
            Text(
                text = "${if (transaction.amount < 0) "-" else "+"}¥%.2f".format(kotlin.math.abs(transaction.amount)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════
//  滚动式日期选择器（年/月/日 — 同报表风格）
// ══════════════════════════════════════════════════════════════════════
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
        NumberPicker(label = "年", values = years, selectedValue = year, onValueChange = onYearChange)
        NumberPicker(label = "月", values = months, selectedValue = month + 1, onValueChange = { onMonthChange(it - 1) })
        NumberPicker(label = "日", values = days, selectedValue = day, onValueChange = onDayChange)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
            )

            LazyColumn(
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                items(values) { value ->
                    val isSelected = value == selectedValue
                    Box(
                        modifier = Modifier.fillMaxWidth().height(40.dp).clickable { onValueChange(value) },
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
//  单日期选择对话框（无开始/结束 tab，仅年/月/日滚动，确定直接返回）
// ══════════════════════════════════════════════════════════════════════
@Composable
private fun SingleDatePickerDialog(
    initialDateMillis: Long,
    title: String,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
    var selYear by remember(initialDateMillis) { mutableStateOf(cal.get(Calendar.YEAR)) }
    var selMonth by remember(initialDateMillis) { mutableStateOf(cal.get(Calendar.MONTH)) }
    var selDay by remember(initialDateMillis) { mutableStateOf(cal.get(Calendar.DAY_OF_MONTH)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            DatePickerContent(
                year = selYear, month = selMonth, day = selDay,
                onYearChange = { selYear = it },
                onMonthChange = { selMonth = it },
                onDayChange = { selDay = it }
            )
        },
        confirmButton = {
            Row {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(onClick = {
                    val ms = Calendar.getInstance().apply {
                        set(selYear, selMonth, selDay, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    onConfirm(ms)
                }) { Text("确定") }
            }
        }
    )
}
