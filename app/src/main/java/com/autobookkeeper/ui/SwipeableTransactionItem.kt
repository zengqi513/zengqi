package com.autobookkeeper.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.FractionalThreshold
import androidx.wear.compose.material.rememberSwipeableState
import androidx.wear.compose.material.swipeable
import com.autobookkeeper.data.Transaction
import com.autobookkeeper.data.Source
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * 支持滑动删除/编辑的交易项组件
 * 
 * @param transaction 交易数据
 * @param categoryIconMap 分类图标映射
 * @param isSelected 是否选中（管理模式）
 * @param isManageMode 是否处于管理模式
 * @param onClick 点击回调
 * @param onEdit 编辑回调
 * @param onDelete 删除回调
 * @param enabled 是否启用滑动（管理模式时禁用）
 */
@OptIn(ExperimentalWearMaterialApi::class)
@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    categoryIconMap: Map<String, String>,
    isSelected: Boolean,
    isManageMode: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    enabled: Boolean = true
) {
    val swipeableState = rememberSwipeableState(
        initialValue = 0,
        confirmStateChange = { newState ->
            when (newState) {
                -1 -> { onDelete(); true }
                1 -> { onEdit(); true }
                else -> true
            }
        }
    )
    
    // 动画恢复到原位
    LaunchedEffect(swipeableState.currentValue) {
        if (swipeableState.currentValue != 0) {
            kotlinx.coroutines.delay(150)
            swipeableState.animateTo(0)
        }
    }
    
    val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val isExpense = transaction.amount < 0
    val amountColor = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val liveIcon = categoryIconMap[transaction.categoryName] ?: transaction.categoryIcon
    
    // 滑动阈值 - 调整触发距离
    val maxSwipe = with(LocalDensity.current) { 100.dp.toPx() }
    val anchors = mapOf(
        -maxSwipe to -1,  // 左滑删除
        0f to 0,          // 原位
        maxSwipe to 1     // 右滑编辑
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        // 背景层（滑动时显示）- 带背景色和图标
        if (!isManageMode && enabled) {
            val swipeOffset = swipeableState.offset.value
            val swipeProgress = (swipeOffset / maxSwipe).coerceIn(-1f, 1f)
            
            // 左滑删除背景（红色）
            if (swipeOffset < 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(
                                alpha = kotlin.math.abs(swipeProgress) * 0.8f
                            )
                        ),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        modifier = Modifier.padding(end = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "删除",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // 右滑编辑背景（蓝色/主色）
            if (swipeOffset > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(
                                alpha = swipeProgress * 0.8f
                            )
                        ),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.padding(start = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "编辑",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "编辑",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // 前景卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!isManageMode && enabled) {
                        Modifier
                            .offset {
                                IntOffset(
                                    swipeableState.offset.value.roundToInt(),
                                    0
                                )
                            }
                            .swipeable(
                                state = swipeableState,
                                anchors = anchors,
                                thresholds = { _, _ -> FractionalThreshold(0.25f) },
                                orientation = Orientation.Horizontal
                            )
                    } else Modifier
                )
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 管理模式复选框
                if (isManageMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                // 分类图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) { 
                    CategoryIcon(icon = liveIcon, size = 28) 
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 中间信息
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            transaction.categoryName, 
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (transaction.source == Source.MANUAL) 
                                MaterialTheme.colorScheme.primaryContainer
                            else 
                                MaterialTheme.colorScheme.secondaryContainer
                        ) { 
                            Text(
                                " ${transaction.source.label} ", 
                                style = MaterialTheme.typography.labelSmall
                            ) 
                        }
                    }
                    if (transaction.note.isNotBlank()) {
                        Text(
                            transaction.note.take(20), 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, 
                            maxLines = 1
                        )
                    }
                }
                
                // 右侧金额和时间
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (isExpense) "-¥%.2f".format(kotlin.math.abs(transaction.amount))
                        else "+¥%.2f".format(transaction.amount),
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold, 
                        color = amountColor
                    )
                    Text(
                        dateFormat.format(Date(transaction.date)), 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
