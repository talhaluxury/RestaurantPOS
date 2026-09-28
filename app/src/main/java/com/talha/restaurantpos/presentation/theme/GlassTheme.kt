package com.talha.restaurantpos.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class GlassColors(
    val bg0: Color,
    val bg1: Color,
    val blobCyan: Color,
    val blobPurple: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val highlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentBright: Color,
    val warning: Color,
    val danger: Color,
    val success: Color,
    val isDark: Boolean
)

val LocalGlassColors = staticCompositionLocalOf {
    GlassColors(
        bg0 = DarkBg0, bg1 = DarkBg1, blobCyan = DarkBlobCyan, blobPurple = DarkBlobPurple,
        surface = DarkGlassSurface, surfaceElevated = DarkGlassSurfaceElevated, border = DarkGlassBorder,
        highlight = DarkGlassHighlight, textPrimary = DarkTextPrimary, textSecondary = DarkTextSecondary,
        textTertiary = DarkTextTertiary, accent = AccentCyan, accentBright = AccentCyanBright,
        warning = AccentAmber, danger = AccentRed, success = AccentGreen, isDark = true
    )
}

enum class GlassThemeMode { LIGHT, DARK, SYSTEM }

@Composable
fun RestaurantPosTheme(
    mode: GlassThemeMode = GlassThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (mode) {
        GlassThemeMode.LIGHT -> false
        GlassThemeMode.DARK -> true
        GlassThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val glassColors = if (isDark) {
        GlassColors(
            bg0 = DarkBg0, bg1 = DarkBg1, blobCyan = DarkBlobCyan, blobPurple = DarkBlobPurple,
            surface = DarkGlassSurface, surfaceElevated = DarkGlassSurfaceElevated, border = DarkGlassBorder,
            highlight = DarkGlassHighlight, textPrimary = DarkTextPrimary, textSecondary = DarkTextSecondary,
            textTertiary = DarkTextTertiary, accent = AccentCyan, accentBright = AccentCyanBright,
            warning = AccentAmber, danger = AccentRed, success = AccentGreen, isDark = true
        )
    } else {
        GlassColors(
            bg0 = LightBg0, bg1 = LightBg1, blobCyan = LightBlobCyan, blobPurple = LightBlobPurple,
            surface = LightGlassSurface, surfaceElevated = LightGlassSurfaceElevated, border = LightGlassBorder,
            highlight = LightGlassHighlight, textPrimary = LightTextPrimary, textSecondary = LightTextSecondary,
            textTertiary = LightTextTertiary, accent = AccentCyan, accentBright = AccentCyanBright,
            warning = AccentAmber, danger = AccentRed, success = AccentGreen, isDark = false
        )
    }

    val materialScheme = if (isDark) {
        darkColorScheme(
            primary = AccentCyan, onPrimary = Color.Black, background = glassColors.bg0,
            surface = glassColors.bg1, onBackground = glassColors.textPrimary, onSurface = glassColors.textPrimary,
            error = AccentRed
        )
    } else {
        lightColorScheme(
            primary = AccentCyan, onPrimary = Color.Black, background = glassColors.bg0,
            surface = glassColors.bg1, onBackground = glassColors.textPrimary, onSurface = glassColors.textPrimary,
            error = AccentRed
        )
    }

    CompositionLocalProvider(LocalGlassColors provides glassColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = GlassTypography,
            shapes = GlassShapes,
            content = content
        )
    }
}

object GlassTheme {
    val colors: GlassColors
        @Composable get() = LocalGlassColors.current
}
