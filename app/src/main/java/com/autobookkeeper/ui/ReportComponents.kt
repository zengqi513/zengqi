package com.autobookkeeper.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.autobookkeeper.data.CategoryData
import com.autobookkeeper.data.CategoryStat
import com.autobookkeeper.data.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ════════════════════════════════════════════════════════════════
//  扇形图 + 右侧图例（替代旧 ReportCategoryChartSection）
// ════════════════════════════════════════════════════════════════
@Composable
fun CategoryPieLegendCard(
    categories: List<CategoryStat>,
    allCategories: List<CategoryData>,
    title: String,
    totalAmount: Double,
    onCategoryClick: (String) -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "%.2f".format(totalAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧扇形
                Box(modifier = Modifier.size(160.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val or = kotlin.math.min(cx, cy) * 0.85f
                        var startAngle = -90f

                        categories.forEachIndexed { i, cat ->
                            val sweep = ((cat.total / total * 360.0).toFloat()).coerceAtLeast(0.3f)
                            drawArc(
                                color = colors[i % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = Offset(cx - or, cy - or),
                                size = Size(or * 2f, or * 2f)
                            )
                            startAngle += sweep
                        }

                        // 中间小圆 + 总额文字
                        drawCircle(Color.White, or * 0.48f, Offset(cx, cy))
                        val totalPaint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK.toInt()
                            textSize = 30f
                            isFakeBoldText = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            "%.0f".format(total), cx, cy + 6f, totalPaint
                        )
                        val labelPaint = android.graphics.Paint().apply {
                            color = 0xFF999999.toInt().toInt()
                            textSize = 20f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        drawContext.canvas.nativeCanvas.drawText("总金额", cx, cy + 28f, labelPaint)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // 右侧图例
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    categories.take(9).forEachIndexed { i, cat ->
                        val pct = (cat.total / total * 100).toInt()
                        val hasSub = allCategories.any { it.parentName == cat.name }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryClick(cat.name) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(colors[i % colors.size], CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.width(42.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${pct}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors[i % colors.size]
                            )
                            if (hasSub) {
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "›",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (categories.size > 9) {
                        Text(
                            text = "+${categories.size - 9} 项",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  子分类明细（水平条形图 + 下钻到商家）
// ════════════════════════════════════════════════════════════════
@Composable
fun SubCategoryPieCard(
    parentName: String,
    subStats: List<CategoryStat>,
    totalAmount: Double,
    onBack: () -> Unit,
    onSubClick: (String) -> Unit
) {
    val colors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4),
        Color(0xFF84CC16), Color(0xFFF97316), Color(0xFF6366F1)
    )
    val total = subStats.sumOf { it.total }.coerceAtLeast(1.0)

    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "$parentName 明细",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "%.2f".format(totalAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (subStats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无子分类数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxVal = subStats.maxOfOrNull { it.total } ?: 1.0
                subStats.forEachIndexed { i, s ->
                    val pct = s.total / total * 100
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSubClick(s.name) }
                            .padding(vertical = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(colors[i % colors.size], CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${s.icon} ${s.name}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "%.0f".format(s.total),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "%.0f%%".format(pct),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((s.total / maxVal).toFloat().coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .background(colors[i % colors.size], RoundedCornerShape(4.dp))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "点击分类查看商家明细 ›",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════
//  商家明细（按 note/备注 分组统计）
// ════════════════════════════════════════════════════════════════
@Composable
fun CategoryMerchantCard(
    parentName: String,
    subName: String,
    timeStart: Long,
    timeEnd: Long,
    allTransactions: List<Transaction>,
    onBack: () -> Unit
) {
    val filtered = allTransactions
        .filter { it.categoryName == subName && it.date in timeStart..timeEnd }
    val grouped = filtered
        .groupBy { t -> t.note.ifBlank { "(未备注)" } }
        .mapValues { (_, list) -> list.sumOf { kotlin.math.abs(it.amount) } }
        .entries
        .sortedByDescending { it.value }
    val total = grouped.sumOf { it.value }

    val colors = listOf(
        Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B),
        Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF06B6D4)
    )

    Card(shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "$parentName · $subName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "商家/备注明细",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "共 ${filtered.size} 笔 · 合计 %.2f".format(total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (grouped.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                grouped.take(15).forEachIndexed { i, (name, amount) ->
                    val pct = if (total > 0) amount / total * 100 else 0.0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(colors[i % colors.size], CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "%.0f%%".format(pct),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = "%.2f".format(amount),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
                if (grouped.size > 15) {
                    Text(
                        text = "+${grouped.size - 15} 项...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
