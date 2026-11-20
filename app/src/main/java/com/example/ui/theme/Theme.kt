package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.AppAppearanceMode
import com.example.data.AppThemeMode

@Immutable
data class TippaniColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val greenBadgeBg: Color,
    val isDark: Boolean,
    val isPureBlack: Boolean
) {
    val cardBorder: BorderStroke?
        get() = when {
            isPureBlack -> BorderStroke(1.dp, border)
            isDark -> BorderStroke(0.5.dp, border)
            else -> null
        }

    fun softIconBg(tint: Color): Color {
        return if (isDark) tint.copy(alpha = 0.18f) else tint.copy(alpha = 0.14f)
    }
}

private val DefaultLightColors = TippaniColors(
    background = CleanBackground,
    surface = CardBackground,
    surfaceVariant = SoftIconBgSlate,
    border = CardSubtleBorder,
    textPrimary = TextPrimary,
    textSecondary = TextSecondary,
    textMuted = TextMuted,
    accent = DarkNavActive,
    onAccent = Color.White,
    accentSoft = SoftIconBgSlate,
    greenBadgeBg = Color(0xFFECFDF5),
    isDark = false,
    isPureBlack = false
)

val LocalTippaniColors = staticCompositionLocalOf { DefaultLightColors }

object TippaniTheme {
    val colors: TippaniColors
        @Composable
        @ReadOnlyComposable
        get() = LocalTippaniColors.current
}

@Composable
fun TippaniTheme(
    appearanceMode: AppAppearanceMode = AppAppearanceMode.LIGHT,
    accentMode: AppThemeMode = AppThemeMode.SLATE,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (appearanceMode) {
        AppAppearanceMode.LIGHT -> false
        AppAppearanceMode.DARK -> true
        AppAppearanceMode.BLACK -> true
        AppAppearanceMode.SYSTEM -> systemDark
    }
    val isPureBlack = appearanceMode == AppAppearanceMode.BLACK

    val accentColor = Color(if (isDark) accentMode.darkColorHex else accentMode.colorHex)
    val onAccentColor = Color(if (isDark) accentMode.onDarkAccentHex else accentMode.onLightAccentHex)

    val tippaniColors = when {
        isPureBlack -> TippaniColors(
            background = Color(0xFF000000),
            surface = Color(0xFF0D0D10),
            surfaceVariant = Color(0xFF17171D),
            border = Color(0xFF23232C),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFFA1A1AA),
            textMuted = Color(0xFF71717A),
            accent = accentColor,
            onAccent = onAccentColor,
            accentSoft = accentColor.copy(alpha = 0.16f),
            greenBadgeBg = Color(0xFF052E1E),
            isDark = true,
            isPureBlack = true
        )
        isDark -> TippaniColors(
            background = Color(0xFF0F141D),
            surface = Color(0xFF18202C),
            surfaceVariant = Color(0xFF222D3D),
            border = Color(0xFF2A3649),
            textPrimary = Color(0xFFF1F5F9),
            textSecondary = Color(0xFF94A3B8),
            textMuted = Color(0xFF64748B),
            accent = accentColor,
            onAccent = onAccentColor,
            accentSoft = accentColor.copy(alpha = 0.16f),
            greenBadgeBg = Color(0xFF0A2E22),
            isDark = true,
            isPureBlack = false
        )
        else -> TippaniColors(
            background = CleanBackground,
            surface = CardBackground,
            surfaceVariant = SoftIconBgSlate,
            border = CardSubtleBorder,
            textPrimary = TextPrimary,
            textSecondary = TextSecondary,
            textMuted = TextMuted,
            accent = accentColor,
            onAccent = onAccentColor,
            accentSoft = accentColor.copy(alpha = 0.12f),
            greenBadgeBg = Color(0xFFECFDF5),
            isDark = false,
            isPureBlack = false
        )
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = tippaniColors.accent,
            onPrimary = tippaniColors.onAccent,
            background = tippaniColors.background,
            onBackground = tippaniColors.textPrimary,
            surface = tippaniColors.surface,
            onSurface = tippaniColors.textPrimary,
            surfaceVariant = tippaniColors.surfaceVariant,
            onSurfaceVariant = tippaniColors.textSecondary,
            surfaceContainer = tippaniColors.surface,
            surfaceContainerHigh = tippaniColors.surfaceVariant,
            surfaceContainerHighest = tippaniColors.surfaceVariant,
            outline = tippaniColors.border,
            secondary = tippaniColors.textSecondary,
            tertiary = AccentGreen
        )
    } else {
        lightColorScheme(
            primary = tippaniColors.accent,
            onPrimary = tippaniColors.onAccent,
            background = tippaniColors.background,
            onBackground = tippaniColors.textPrimary,
            surface = tippaniColors.surface,
            onSurface = tippaniColors.textPrimary,
            surfaceVariant = tippaniColors.surfaceVariant,
            onSurfaceVariant = tippaniColors.textSecondary,
            surfaceContainer = tippaniColors.surface,
            surfaceContainerHigh = tippaniColors.surfaceVariant,
            surfaceContainerHighest = tippaniColors.surfaceVariant,
            outline = tippaniColors.border,
            secondary = tippaniColors.textSecondary,
            tertiary = AccentGreen
        )
    }

    CompositionLocalProvider(LocalTippaniColors provides tippaniColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
