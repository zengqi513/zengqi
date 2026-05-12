package com.autobookkeeper.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.autobookkeeper.data.UserPreferences

// ===== 配色方案定义 =====

/** 暖绿 - 抹茶色系 */
val WarmGreenLight = lightColorScheme(
    primary = Color(0xFF2D8C6F), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9), onPrimaryContainer = Color(0xFF1B6B52),
    secondary = Color(0xFF2E7D32), secondaryContainer = Color(0xFFC8E6C9),
    tertiary = Color(0xFFD84315), tertiaryContainer = Color(0xFFFFCCBC),
    background = Color(0xFFF8F8F4), surface = Color.White,
    surfaceVariant = Color(0xFFEFEFEB), onBackground = Color(0xFF2D2D2D),
    onSurface = Color(0xFF2D2D2D), onSurfaceVariant = Color(0xFF8D8D8D),
    outline = Color(0xFFE8E8E3), outlineVariant = Color(0xFFE8E8E3),
    error = Color(0xFFD32F2F), errorContainer = Color(0xFFFFEBEE),
)
val WarmGreenDark = darkColorScheme(
    primary = Color(0xFF6FCF97), onPrimary = Color(0xFF003D26),
    primaryContainer = Color(0xFF005238), onPrimaryContainer = Color(0xFFE8F5E9),
    secondary = Color(0xFF81C784), secondaryContainer = Color(0xFF1B5E20),
    tertiary = Color(0xFFFF8A65), tertiaryContainer = Color(0xFFBF360C),
    background = Color(0xFF121212), surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF2D2D2D), onBackground = Color(0xFFE8E8E8),
    onSurface = Color(0xFFE8E8E8), onSurfaceVariant = Color(0xFFB0B0B0),
    outline = Color(0xFF3D3D3D), outlineVariant = Color(0xFF3D3D3D),
    error = Color(0xFFEF9A9A), errorContainer = Color(0xFFB71C1C),
)

/** 深林 - 深绿色系 */
val ForestGreenLight = lightColorScheme(
    primary = Color(0xFF1B5E20), onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9), onPrimaryContainer = Color(0xFF0D3B11),
    secondary = Color(0xFF33691E), secondaryContainer = Color(0xFFDCEDC8),
    tertiary = Color(0xFF795548), tertiaryContainer = Color(0xFFD7CCC8),
    background = Color(0xFFF1F8E9), surface = Color.White,
    surfaceVariant = Color(0xFFE8F5E9), onBackground = Color(0xFF1B1B1B),
    onSurface = Color(0xFF1B1B1B), onSurfaceVariant = Color(0xFF6D8B6D),
    outline = Color(0xFFC8E6C9), outlineVariant = Color(0xFFC8E6C9),
    error = Color(0xFFB71C1C), errorContainer = Color(0xFFFFCDD2),
)
val ForestGreenDark = darkColorScheme(
    primary = Color(0xFFA5D6A7), onPrimary = Color(0xFF0D3B11),
    primaryContainer = Color(0xFF0D3B11), onPrimaryContainer = Color(0xFFE8F5E9),
    secondary = Color(0xFFAED581), secondaryContainer = Color(0xFF33691E),
    tertiary = Color(0xFFBCAAA4), tertiaryContainer = Color(0xFF4E342E),
    background = Color(0xFF0D1B0E), surface = Color(0xFF1A2E1A),
    surfaceVariant = Color(0xFF2D4A2D), onBackground = Color(0xFFE0E0D0),
    onSurface = Color(0xFFE0E0D0), onSurfaceVariant = Color(0xFF9DB99D),
    outline = Color(0xFF2D4A2D), outlineVariant = Color(0xFF2D4A2D),
    error = Color(0xFFEF9A9A), errorContainer = Color(0xFF7F0000),
)

/** 靛蓝 - 蓝色系 */
val IndigoBlueLight = lightColorScheme(
    primary = Color(0xFF1565C0), onPrimary = Color.White,
    primaryContainer = Color(0xFFBBDEFB), onPrimaryContainer = Color(0xFF0D47A1),
    secondary = Color(0xFF4527A0), secondaryContainer = Color(0xFFD1C4E9),
    tertiary = Color(0xFF00838F), tertiaryContainer = Color(0xFFB2EBF2),
    background = Color(0xFFF5F7FA), surface = Color.White,
    surfaceVariant = Color(0xFFE8EAF6), onBackground = Color(0xFF1A1A2E),
    onSurface = Color(0xFF1A1A2E), onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFC5CAE9), outlineVariant = Color(0xFFC5CAE9),
    error = Color(0xFFD32F2F), errorContainer = Color(0xFFFFEBEE),
)
val IndigoBlueDark = darkColorScheme(
    primary = Color(0xFF90CAF9), onPrimary = Color(0xFF0D47A1),
    primaryContainer = Color(0xFF0D47A1), onPrimaryContainer = Color(0xFFE3F2FD),
    secondary = Color(0xFFB39DDB), secondaryContainer = Color(0xFF4527A0),
    tertiary = Color(0xFF80DEEA), tertiaryContainer = Color(0xFF006064),
    background = Color(0xFF0D111A), surface = Color(0xFF1A1F2E),
    surfaceVariant = Color(0xFF28344A), onBackground = Color(0xFFD0D8E0),
    onSurface = Color(0xFFD0D8E0), onSurfaceVariant = Color(0xFF9EA8B8),
    outline = Color(0xFF28344A), outlineVariant = Color(0xFF28344A),
    error = Color(0xFFEF9A9A), errorContainer = Color(0xFF7F0000),
)

/** 玫瑰金 - 棕粉色调 */
val RoseGoldLight = lightColorScheme(
    primary = Color(0xFFAD1457), onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE4EC), onPrimaryContainer = Color(0xFF880E4F),
    secondary = Color(0xFF6A1B9A), secondaryContainer = Color(0xFFE1BEE7),
    tertiary = Color(0xFFBF360C), tertiaryContainer = Color(0xFFFFCCBC),
    background = Color(0xFFFFF5F5), surface = Color.White,
    surfaceVariant = Color(0xFFFCE4EC), onBackground = Color(0xFF2D1B1B),
    onSurface = Color(0xFF2D1B1B), onSurfaceVariant = Color(0xFF9E6B6B),
    outline = Color(0xFFF8BBD0), outlineVariant = Color(0xFFF8BBD0),
    error = Color(0xFFC62828), errorContainer = Color(0xFFFFCDD2),
)
val RoseGoldDark = darkColorScheme(
    primary = Color(0xFFF48FB1), onPrimary = Color(0xFF880E4F),
    primaryContainer = Color(0xFF880E4F), onPrimaryContainer = Color(0xFFFCE4EC),
    secondary = Color(0xFFCE93D8), secondaryContainer = Color(0xFF4A148C),
    tertiary = Color(0xFFFF8A65), tertiaryContainer = Color(0xFFBF360C),
    background = Color(0xFF1A0D0D), surface = Color(0xFF2D1A1A),
    surfaceVariant = Color(0xFF4A2828), onBackground = Color(0xFFE0C8C8),
    onSurface = Color(0xFFE0C8C8), onSurfaceVariant = Color(0xFFB09090),
    outline = Color(0xFF4A2828), outlineVariant = Color(0xFF4A2828),
    error = Color(0xFFEF9A9A), errorContainer = Color(0xFF7F0000),
)

/** 莫兰迪 - 紫灰色系 */
val MorandiLight = lightColorScheme(
    primary = Color(0xFF7E57C2), onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7F6), onPrimaryContainer = Color(0xFF512DA8),
    secondary = Color(0xFF607D8B), secondaryContainer = Color(0xFFCFD8DC),
    tertiary = Color(0xFF8D6E63), tertiaryContainer = Color(0xFFEFEBE9),
    background = Color(0xFFF5F3F0), surface = Color(0xFFFAFAF8),
    surfaceVariant = Color(0xFFEEEDE8), onBackground = Color(0xFF3D3A3A),
    onSurface = Color(0xFF3D3A3A), onSurfaceVariant = Color(0xFF9A9793),
    outline = Color(0xFFD7D3CE), outlineVariant = Color(0xFFD7D3CE),
    error = Color(0xFFD32F2F), errorContainer = Color(0xFFFFEBEE),
)
val MorandiDark = darkColorScheme(
    primary = Color(0xFFB39DDB), onPrimary = Color(0xFF3E1D6E),
    primaryContainer = Color(0xFF512DA8), onPrimaryContainer = Color(0xFFEDE7F6),
    secondary = Color(0xFF90A4AE), secondaryContainer = Color(0xFF455A64),
    tertiary = Color(0xFFBCAAA4), tertiaryContainer = Color(0xFF5D4037),
    background = Color(0xFF1A1818), surface = Color(0xFF2A2825),
    surfaceVariant = Color(0xFF3D3A35), onBackground = Color(0xFFDDD8D0),
    onSurface = Color(0xFFDDD8D0), onSurfaceVariant = Color(0xFFAAA49B),
    outline = Color(0xFF3D3A35), outlineVariant = Color(0xFF3D3A35),
    error = Color(0xFFEF9A9A), errorContainer = Color(0xFF7F0000),
)

/** 粉白 - 粉色系 */
val PinkWhiteLight = lightColorScheme(
    primary = Color(0xFFEC407A), onPrimary = Color.White,
    primaryContainer = Color(0xFFFCE4EC), onPrimaryContainer = Color(0xFFAD1457),
    secondary = Color(0xFFAB47BC), secondaryContainer = Color(0xFFF3E5F5),
    tertiary = Color(0xFF6A1B9A), tertiaryContainer = Color(0xFFE1BEE7),
    background = Color(0xFFFEFAFA), surface = Color.White,
    surfaceVariant = Color(0xFFFDF2F6), onBackground = Color(0xFF2D1B1B),
    onSurface = Color(0xFF2D1B1B), onSurfaceVariant = Color(0xFF9E7A7A),
    outline = Color(0xFFF8E0E8), outlineVariant = Color(0xFFF8E0E8),
    error = Color(0xFFD32F2F), errorContainer = Color(0xFFFFEBEE),
)
val PinkWhiteDark = darkColorScheme(
    primary = Color(0xFFF48FB1), onPrimary = Color(0xFF4A0020),
    primaryContainer = Color(0xFF880E4F), onPrimaryContainer = Color(0xFFFCE4EC),
    secondary = Color(0xFFCE93D8), secondaryContainer = Color(0xFF4A148C),
    tertiary = Color(0xFFB39DDB), tertiaryContainer = Color(0xFF311B92),
    background = Color(0xFF1A0D12), surface = Color(0xFF2D1A22),
    surfaceVariant = Color(0xFF452A35), onBackground = Color(0xFFE0D0D4),
    onSurface = Color(0xFFE0D0D4), onSurfaceVariant = Color(0xFFB09EA5),
    outline = Color(0xFF452A35), outlineVariant = Color(0xFF452A35),
    error = Color(0xFFEF9A9A), errorContainer = Color(0xFF7F0000),
)

/** 天空蓝 - 蓝色调亮色系 */
val SkyBlueLight = lightColorScheme(
    primary = Color(0xFF0288D1), onPrimary = Color.White,
    primaryContainer = Color(0xFFB3E5FC), onPrimaryContainer = Color(0xFF01579B),
    secondary = Color(0xFF00ACC1), secondaryContainer = Color(0xFFB2EBF2),
    tertiary = Color(0xFF43A047), tertiaryContainer = Color(0xFFC8E6C9),
    background = Color(0xFFF0F8FF), surface = Color.White,
    surfaceVariant = Color(0xFFE1F5FE), onBackground = Color(0xFF1A2A3A),
    onSurface = Color(0xFF1A2A3A), onSurfaceVariant = Color(0xFF6B8A9E),
    outline = Color(0xFFB3E5FC), outlineVariant = Color(0xFFB3E5FC),
    error = Color(0xFFE53935), errorContainer = Color(0xFFFFEBEE),
)
val SkyBlueDark = darkColorScheme(
    primary = Color(0xFF4FC3F7), onPrimary = Color(0xFF00334D),
    primaryContainer = Color(0xFF01579B), onPrimaryContainer = Color(0xFFE1F5FE),
    secondary = Color(0xFF4DD0E1), secondaryContainer = Color(0xFF006064),
    tertiary = Color(0xFF81C784), tertiaryContainer = Color(0xFF1B5E20),
    background = Color(0xFF0D1A24), surface = Color(0xFF1A2D3A),
    surfaceVariant = Color(0xFF28485A), onBackground = Color(0xFFD0DDE8),
    onSurface = Color(0xFFD0DDE8), onSurfaceVariant = Color(0xFF9EB0C0),
    outline = Color(0xFF28485A), outlineVariant = Color(0xFF28485A),
    error = Color(0xFFEF9A9A), errorContainer = Color(0xFF7F0000),
)

/** 奶杏紫 - 杏色配浅紫 */
val CreamApricotLight = lightColorScheme(
    primary = Color(0xFF9C27B0), onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E5F5), onPrimaryContainer = Color(0xFF6A1B9A),
    secondary = Color(0xFFE91E63), secondaryContainer = Color(0xFFFCE4EC),
    tertiary = Color(0xFFFF8F00), tertiaryContainer = Color(0xFFFFF8E1),
    background = Color(0xFFFFFBF0), surface = Color(0xFFFFFFF5),
    surfaceVariant = Color(0xFFF5EDF5), onBackground = Color(0xFF2D1B2D),
    onSurface = Color(0xFF2D1B2D), onSurfaceVariant = Color(0xFF9E809E),
    outline = Color(0xFFE8DCE8), outlineVariant = Color(0xFFE8DCE8),
    error = Color(0xFFE53935), errorContainer = Color(0xFFFFEBEE),
)
val CreamApricotDark = darkColorScheme(
    primary = Color(0xFFCE93D8), onPrimary = Color(0xFF3E1D6E),
    primaryContainer = Color(0xFF6A1B9A), onPrimaryContainer = Color(0xFFF3E5F5),
    secondary = Color(0xFFF48FB1), secondaryContainer = Color(0xFF880E4F),
    tertiary = Color(0xFFFFB74D), tertiaryContainer = Color(0xFFE65100),
    background = Color(0xFF1A0D1A), surface = Color(0xFF2D1A2D),
    surfaceVariant = Color(0xFF452E45), onBackground = Color(0xFFE0D0E0),
    onSurface = Color(0xFFE0D0E0), onSurfaceVariant = Color(0xFFB09EB0),
    outline = Color(0xFF452E45), outlineVariant = Color(0xFF452E45),
    error = Color(0xFFEF9A9A), errorContainer = Color(0xFF7F0000),
)

/** 奶油薄荷 - 浅绿配奶白 */
val CreamMintLight = lightColorScheme(
    primary = Color(0xFF43A047), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9), onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF00BCD4), secondaryContainer = Color(0xFFB2EBF2),
    tertiary = Color(0xFFFF6F00), tertiaryContainer = Color(0xFFFFF3E0),
    background = Color(0xFFF5FFF5), surface = Color.White,
    surfaceVariant = Color(0xFFE8F5E9), onBackground = Color(0xFF1B2D1B),
    onSurface = Color(0xFF1B2D1B), onSurfaceVariant = Color(0xFF6B9E6B),
    outline = Color(0xFFC8E6C9), outlineVariant = Color(0xFFC8E6C9),
    error = Color(0xFFE53935), errorContainer = Color(0xFFFFEBEE),
)
val CreamMintDark = darkColorScheme(
    primary = Color(0xFF81C784), onPrimary = Color(0xFF003D0D),
    primaryContainer = Color(0xFF1B5E20), onPrimaryContainer = Color(0xFFE8F5E9),
    secondary = Color(0xFF4DD0E1), secondaryContainer = Color(0xFF006064),
    tertiary = Color(0xFFFFB74D), tertiaryContainer = Color(0xFFE65100),
    background = Color(0xFF0D1A0D), surface = Color(0xFF1A2D1A),
    surfaceVariant = Color(0xFF284A28), onBackground = Color(0xFFD0E0D0),
    onSurface = Color(0xFFD0E0D0), onSurfaceVariant = Color(0xFF9EB99E),
    outline = Color(0xFF284A28), outlineVariant = Color(0xFF284A28),
    error = Color(0xFFEF9A9A), errorContainer = Color(0xFF7F0000),
)

/** 蜜桃米白 - 粉桃配米白 */
val PeachCreamLight = lightColorScheme(
    primary = Color(0xFFE91E63), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF0F0), onPrimaryContainer = Color(0xFFAD1457),
    secondary = Color(0xFFFF9800), secondaryContainer = Color(0xFFFFF3E0),
    tertiary = Color(0xFF8BC34A), tertiaryContainer = Color(0xFFF1F8E9),
    background = Color(0xFFFFFDF5), surface = Color(0xFFFFFFFA),
    surfaceVariant = Color(0xFFFFF0ED), onBackground = Color(0xFF2D1B1B),
    onSurface = Color(0xFF2D1B1B), onSurfaceVariant = Color(0xFF9E7A7A),
    outline = Color(0xFFF8E0D8), outlineVariant = Color(0xFFF8E0D8),
    error = Color(0xFFD32F2F), errorContainer = Color(0xFFFFEBEE),
)
val PeachCreamDark = darkColorScheme(
    primary = Color(0xFFF48FB1), onPrimary = Color(0xFF4A0020),
    primaryContainer = Color(0xFF880E4F), onPrimaryContainer = Color(0xFFFFF0F0),
    secondary = Color(0xFFFFB74D), secondaryContainer = Color(0xFFE65100),
    tertiary = Color(0xFFAED581), tertiaryContainer = Color(0xFF33691E),
    background = Color(0xFF1A0D0D), surface = Color(0xFF2D1A1A),
    surfaceVariant = Color(0xFF452D28), onBackground = Color(0xFFE0D0C8),
    onSurface = Color(0xFFE0D0C8), onSurfaceVariant = Color(0xFFB09E96),
    outline = Color(0xFF452D28), outlineVariant = Color(0xFF452D28),
    error = Color(0xFFEF9A9A), errorContainer = Color(0xFF7F0000),
)

/** 香芋紫 - 紫色渐变 */
val LavenderLight = lightColorScheme(
    primary = Color(0xFF7B1FA2), onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E5F5), onPrimaryContainer = Color(0xFF4A148C),
    secondary = Color(0xFF00BCD4), secondaryContainer = Color(0xFFB2EBF2),
    tertiary = Color(0xFFE91E63), tertiaryContainer = Color(0xFFFCE4EC),
    background = Color(0xFFF8F4FF), surface = Color(0xFFFFFFFC),
    surfaceVariant = Color(0xFFF0E8F8), onBackground = Color(0xFF1D1B2D),
    onSurface = Color(0xFF1D1B2D), onSurfaceVariant = Color(0xFF7A80A0),
    outline = Color(0xFFE0D8F0), outlineVariant = Color(0xFFE0D8F0),
    error = Color(0xFFD32F2F), errorContainer = Color(0xFFFFEBEE),
)
val LavenderDark = darkColorScheme(
    primary = Color(0xFFCE93D8), onPrimary = Color(0xFF3E1D6E),
    primaryContainer = Color(0xFF4A148C), onPrimaryContainer = Color(0xFFF3E5F5),
    secondary = Color(0xFF4DD0E1), secondaryContainer = Color(0xFF006064),
    tertiary = Color(0xFFF48FB1), tertiaryContainer = Color(0xFF880E4F),
    background = Color(0xFF0D0D1A), surface = Color(0xFF1A1A2D),
    surfaceVariant = Color(0xFF2D284A), onBackground = Color(0xFFD0D0E0),
    onSurface = Color(0xFFD0D0E0), onSurfaceVariant = Color(0xFFA09EB8),
    outline = Color(0xFF2D284A), outlineVariant = Color(0xFF2D284A),
    error = Color(0xFFEF9A9A), errorContainer = Color(0xFF7F0000),
)

private fun selectLightPalette(paletteName: String) = when (paletteName) {
    "ForestGreen" -> ForestGreenLight
    "IndigoBlue" -> IndigoBlueLight
    "RoseGold" -> RoseGoldLight
    "Morandi" -> MorandiLight
    "PinkWhite" -> PinkWhiteLight
    "SkyBlue" -> SkyBlueLight
    "CreamApricot" -> CreamApricotLight
    "CreamMint" -> CreamMintLight
    "PeachCream" -> PeachCreamLight
    "Lavender" -> LavenderLight
    else -> WarmGreenLight
}

private fun selectDarkPalette(paletteName: String) = when (paletteName) {
    "ForestGreen" -> ForestGreenDark
    "IndigoBlue" -> IndigoBlueDark
    "RoseGold" -> RoseGoldDark
    "Morandi" -> MorandiDark
    "PinkWhite" -> PinkWhiteDark
    "SkyBlue" -> SkyBlueDark
    "CreamApricot" -> CreamApricotDark
    "CreamMint" -> CreamMintDark
    "PeachCream" -> PeachCreamDark
    "Lavender" -> LavenderDark
    else -> WarmGreenDark
}

/**
 * 统一的 App 主题入口（多配色支持）
 * @param darkTheme 是否深色模式
 * @param paletteName 配色名称（如 "WarmGreen", "ForestGreen" 等）
 * @param content 子组件
 */
@Composable
fun AutoBookkeeperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    paletteName: String = "WarmGreen",
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) selectDarkPalette(paletteName) else selectLightPalette(paletteName),
        typography = Typography(),
        content = content
    )
}

// ===== 辅助函数：获取当前主题的卡片背景色 =====
@Composable
fun cardBackgroundColor(): Color {
    return if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Color(0xFF2D2D2D)
    } else {
        Color(0xFFF5F5F0)
    }
}

private fun Color.luminance(): Float {
    val r = this.red
    val g = this.green
    val b = this.blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}
