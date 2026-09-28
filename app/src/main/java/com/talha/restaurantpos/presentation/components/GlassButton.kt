package com.talha.restaurantpos.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.talha.restaurantpos.presentation.theme.GlassTheme

enum class GlassButtonStyle { PRIMARY, SECONDARY, DANGER, SUCCESS }

/**
 * Primary call-to-action button. PRIMARY renders as a solid cyan-glow pill (used for CHARGE,
 * ADD TO BILL, COMPLETE PAYMENT); SECONDARY is a translucent glass pill (CLEAR, cancel actions);
 * DANGER/SUCCESS tint the fill for destructive or confirmatory actions.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GlassButtonStyle = GlassButtonStyle.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null
) {
    val colors = GlassTheme.colors
    val shape = RoundedCornerShape(18.dp)

    val (fillBrush, textColor, borderColor) = when (style) {
        GlassButtonStyle.PRIMARY -> Triple(
            Brush.horizontalGradient(listOf(colors.accent, colors.accentBright)),
            Color.Black,
            Color.Transparent
        )
        GlassButtonStyle.SECONDARY -> Triple(
            Brush.horizontalGradient(listOf(colors.surfaceElevated, colors.surfaceElevated)),
            colors.textPrimary,
            colors.border
        )
        GlassButtonStyle.DANGER -> Triple(
            Brush.horizontalGradient(listOf(colors.danger.copy(alpha = 0.85f), colors.danger)),
            Color.White,
            Color.Transparent
        )
        GlassButtonStyle.SUCCESS -> Triple(
            Brush.horizontalGradient(listOf(colors.success.copy(alpha = 0.85f), colors.success)),
            Color.Black,
            Color.Transparent
        )
    }

    val interactionSource = remember { MutableInteractionSource() }
    val alpha = if (enabled) 1f else 0.45f

    Box(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(fillBrush, shape)
            .then(if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && !loading,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = textColor)
                Spacer(Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = textColor.copy(alpha = alpha), style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    accented: Boolean = false
) {
    val colors = GlassTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (accented) colors.accent.copy(alpha = 0.18f) else colors.surface)
            .border(1.dp, if (accented) colors.accent.copy(alpha = 0.5f) else colors.border, CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (accented) colors.accent else colors.textPrimary,
            modifier = Modifier.size(size * 0.45f)
        )
    }
}
