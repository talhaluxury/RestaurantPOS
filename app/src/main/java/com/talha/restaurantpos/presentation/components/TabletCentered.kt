package com.talha.restaurantpos.presentation.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wraps form-style content (login, register, add/edit product, settings forms) so it stops
 * stretching edge-to-edge once the screen is wider than a phone. Below [breakpoint] this is a
 * plain full-width Column (phones, unchanged behavior); at or above it, content is capped at
 * [formMaxWidth] and centered, which is what makes these screens look intentional on tablets
 * instead of a phone layout awkwardly blown up.
 */
@Composable
fun TabletCentered(
    modifier: Modifier = Modifier,
    formMaxWidth: Dp = 480.dp,
    breakpoint: Dp = 600.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val isWide = maxWidth >= breakpoint
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(if (isWide) Modifier.widthIn(max = formMaxWidth) else Modifier.fillMaxWidth()),
            content = content
        )
    }
}
