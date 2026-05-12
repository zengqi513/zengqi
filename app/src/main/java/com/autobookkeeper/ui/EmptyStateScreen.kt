package com.autobookkeeper.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 空状态页面组件
 * 
 * @param isFirstUse 是否是首次使用（显示引导）
 * @param isSearchResult 是否是搜索结果为空
 * @param onAddClick 点击添加按钮
 * @param onVoiceClick 点击语音按钮
 * @param onEnableNotification 点击开启通知
 */
@Composable
fun EmptyStateScreen(
    isFirstUse: Boolean = false,
    isSearchResult: Boolean = false,
    onAddClick: () -> Unit = {},
    onVoiceClick: () -> Unit = {},
    onEnableNotification: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 插图区域
        EmptyStateIllustration(isFirstUse)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 标题
        Text(
            text = when {
                isSearchResult -> "没有找到匹配的记录"
                isFirstUse -> "开始你的第一笔记账"
                else -> "该时间段还没有记账"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 描述
        Text(
            text = when {
                isSearchResult -> "试试其他关键词或时间范围"
                isFirstUse -> "选择一种方式快速记录你的第一笔收支"
                else -> "点击下方按钮快速添加记录"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 操作按钮
        if (!isSearchResult) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 主要操作：手动添加
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("手动记账")
                }
                
                // 次要操作：语音记账
                OutlinedButton(
                    onClick = onVoiceClick,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("语音记账")
                }
                
                // 首次使用时显示通知引导
                if (isFirstUse) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "开启自动记账",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "自动识别微信、支付宝等支付通知",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = onEnableNotification) {
                                Text("开启")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 空状态插图
 */
@Composable
private fun EmptyStateIllustration(isFirstUse: Boolean) {
    val iconSize = 120.dp
    val colorScheme = MaterialTheme.colorScheme
    
    Box(
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center
    ) {
        // 背景圆形
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = MaterialTheme.shapes.extraLarge,
            color = colorScheme.primaryContainer.copy(alpha = 0.3f)
        ) {}
        
        // 图标
        Text(
            text = if (isFirstUse) "🎯" else "📝",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

/**
 * 简化版空状态（用于小空间）
 */
@Composable
fun EmptyStateCompact(
    message: String = "暂无数据",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📭",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
