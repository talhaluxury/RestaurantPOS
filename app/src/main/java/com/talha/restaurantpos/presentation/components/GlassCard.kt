package com.talha.restaurantpos.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.talha.restaurantpos.presentation.theme.GlassTheme

/**
 * The foundational translucent surface: soft rounded corners, a thin luminous border, a subtle
 * top highlight to fake glass depth, and an elevated variant for cards that should pop off the
 * base surface (e.g. the cart panel, the sales hero card).
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevated: Boolean = false,
    accentBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = GlassTheme.colors
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val fill = if (elevated) colors.surfaceElevated else colors.surface
    val borderColor = if (accentBorder) colors.accent.copy(alpha = 0.55f) else colors.border

    val topHighlightAlpha = (fill.alpha + 0.10f).coerceIn(0f, 1f)
    val base = modifier
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(fill.copy(alpha = topHighlightAlpha), fill)
            )
        )
        .border(1.dp, borderColor, shape)

    val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
    val finalModifier = if (onClick != null) {
        base.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    } else base

    Box(modifier = finalModifier) {
        content()
    }
}
