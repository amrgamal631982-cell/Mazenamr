package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class WirdExtendedColors(
    val goldAccent: Color,
    val goldContainer: Color,
    val onGoldContainer: Color,
    val subtleBorder: Color,
    val mushafPaper: Color,
    val mushafInk: Color,
    val verifiedColor: Color,
    val verifiedBg: Color,
    val warningColor: Color,
    val warningBg: Color
)

val LocalWirdColors = staticCompositionLocalOf {
    WirdExtendedColors(
        goldAccent = SoftGold,
        goldContainer = SoftGoldLight,
        onGoldContainer = OnSoftGold,
        subtleBorder = SubtleBorderLight,
        mushafPaper = WarmIvory,
        mushafInk = WarmInk,
        verifiedColor = VerifiedGreen,
        verifiedBg = VerifiedGreenBg,
        warningColor = WarningAmber,
        warningBg = WarningAmberBg
    )
}

private val LightColorScheme = lightColorScheme(
    primary = DeepEmerald,
    onPrimary = Color.White,
    primaryContainer = DeepEmeraldContainer,
    onPrimaryContainer = OnDeepEmeraldContainer,
    secondary = CalmOlive,
    onSecondary = Color.White,
    secondaryContainer = CalmOliveContainer,
    onSecondaryContainer = OnCalmOliveContainer,
    tertiary = SoftGold,
    onTertiary = OnSoftGold,
    tertiaryContainer = SoftGoldLight,
    onTertiaryContainer = OnSoftGold,
    background = WarmIvory,
    onBackground = WarmInk,
    surface = WarmIvoryCard,
    onSurface = WarmInk,
    surfaceVariant = WarmIvorySurface,
    onSurfaceVariant = WarmInkMuted,
    outline = SubtleBorderLight,
    error = ErrorCrimson,
    errorContainer = ErrorCrimsonBg
)

private val DarkColorScheme = darkColorScheme(
    primary = NightEmeraldPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightEmeraldContainer,
    onPrimaryContainer = Color(0xFFD2F7E7),
    secondary = NightOliveSecondary,
    onSecondary = Color(0xFF0E2918),
    secondaryContainer = NightOliveContainer,
    onSecondaryContainer = Color(0xFFD4EEDB),
    tertiary = NightGold,
    onTertiary = Color(0xFF281E04),
    tertiaryContainer = NightGoldContainer,
    onTertiaryContainer = Color(0xFFF9E9B9),
    background = NightBg,
    onBackground = NightParchment,
    surface = NightSurface,
    onSurface = NightParchment,
    surfaceVariant = NightSurfaceElevated,
    onSurfaceVariant = NightParchmentMuted,
    outline = SubtleBorderDark,
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF5F1410)
)

private val WirdShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun WirdTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val extendedColors = if (isDark) {
        WirdExtendedColors(
            goldAccent = NightGold,
            goldContainer = NightGoldContainer,
            onGoldContainer = Color(0xFFF9E9B9),
            subtleBorder = SubtleBorderDark,
            mushafPaper = NightBg,
            mushafInk = NightParchment,
            verifiedColor = Color(0xFF7BE0AD),
            verifiedBg = Color(0xFF133A27),
            warningColor = Color(0xFFFFCA62),
            warningBg = Color(0xFF3E2D06)
        )
    } else {
        WirdExtendedColors(
            goldAccent = SoftGold,
            goldContainer = SoftGoldLight,
            onGoldContainer = OnSoftGold,
            subtleBorder = SubtleBorderLight,
            mushafPaper = WarmIvory,
            mushafInk = WarmInk,
            verifiedColor = VerifiedGreen,
            verifiedBg = VerifiedGreenBg,
            warningColor = WarningAmber,
            warningBg = WarningAmberBg
        )
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalWirdColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WirdTypography,
            shapes = WirdShapes,
            content = content
        )
    }
}
