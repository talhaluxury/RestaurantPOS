package com.talha.restaurantpos.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.talha.restaurantpos.presentation.theme.GlassTheme

/**
 * The one true background for every POS screen: a dark/light gradient base with soft blurred
 * "light blob" accents behind translucent glass content. Never render primary content directly
 * on this without a GlassCard/GlassSurface in between — see design system rule in the spec.
 */
@Composable
fun GlassBackground(
    content: @Composable () -> Unit
) {
    val colors = GlassTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(colors.bg0, colors.bg1))
            )
    ) {
        // Blurred light blobs — pure decoration, never interactive, kept subtle.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .size(280.dp)
                .blur(120.dp)
                .background(colors.blobCyan, CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-70).dp, y = 60.dp)
                .size(320.dp)
                .blur(140.dp)
                .background(colors.blobPurple, CircleShape)
        )
        content()
    }
}
