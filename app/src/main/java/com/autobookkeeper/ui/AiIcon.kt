package com.autobookkeeper.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 高级AI图标组件 - 大字体版本
 */
@Composable
fun AiIcon(
    size: Dp = 32.dp,
    contentColor: Color = Color.White
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasSize = this.size.minDimension
            val center = canvasSize / 2
            val radius = canvasSize * 0.42f
            
            // 绘制六边形边框
            val hexPath = Path().apply {
                for (i in 0..5) {
                    val angle = Math.PI / 3 * i - Math.PI / 2
                    val x = center + (radius * kotlin.math.cos(angle)).toFloat()
                    val y = center + (radius * kotlin.math.sin(angle)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            
            drawPath(
                path = hexPath,
                color = contentColor.copy(alpha = 0.25f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = canvasSize * 0.04f)
            )
            
            // 中心节点
            drawCircle(
                color = contentColor,
                radius = canvasSize * 0.08f,
                center = Offset(center, center)
            )
            
            // 四个方向连接点
            val directions = listOf(
                Offset(center * 0.5f, center * 0.5f),
                Offset(center * 1.5f, center * 0.5f),
                Offset(center * 0.5f, center * 1.5f),
                Offset(center * 1.5f, center * 1.5f)
            )
            
            directions.forEach { pos ->
                drawCircle(
                    color = contentColor.copy(alpha = 0.5f),
                    radius = canvasSize * 0.05f,
                    center = pos
                )
                drawLine(
                    color = contentColor.copy(alpha = 0.3f),
                    start = Offset(center, center),
                    end = pos,
                    strokeWidth = canvasSize * 0.02f
                )
            }
            
            // 顶部信号点
            drawCircle(
                color = contentColor.copy(alpha = 0.7f),
                radius = canvasSize * 0.03f,
                center = Offset(center, canvasSize * 0.18f)
            )
        }
        
        // AI文字 - 增大字体
        Text(
            text = "AI",
            color = contentColor,
            fontSize = (size.value * 0.5f).sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (size.value * 0.02f).sp
        )
    }
}

/**
 * 简化版AI图标（用于小尺寸场景如设置页）
 */
@Composable
fun AiIconSmall(
    size: Dp = 24.dp,
    contentColor: Color = Color.White
) {
    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasSize = this.size.minDimension
            val center = canvasSize / 2
            
            // 简单圆环
            drawCircle(
                color = contentColor.copy(alpha = 0.15f),
                radius = canvasSize * 0.38f,
                center = Offset(center, center),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = canvasSize * 0.03f)
            )
            
            // 中心点
            drawCircle(
                color = contentColor.copy(alpha = 0.4f),
                radius = canvasSize * 0.06f,
                center = Offset(center, center)
            )
        }
        
        // 文字 - 增大字体
        Text(
            text = "AI",
            color = contentColor,
            fontSize = (size.value * 0.5f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}