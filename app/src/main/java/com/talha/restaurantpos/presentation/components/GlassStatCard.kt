package com.talha.restaurantpos.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.talha.restaurantpos.presentation.theme.GlassTheme

@Composable
fun GlassStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    deltaText: String? = null,
    positive: Boolean = true
) {
    val colors = GlassTheme.colors
    GlassCard(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label.uppercase(), color = colors.textTertiary, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(6.dp))
            Text(value, color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (deltaText != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    deltaText,
                    color = if (positive) colors.success else colors.danger,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/** The large hero "Today's Sales" card on the dashboard. */
@Composable
fun GlassHeroStatCard(
    title: String,
    valueText: String,
    deltaText: String,
    positive: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    GlassCard(modifier = modifier.fillMaxWidth(), elevated = true, accentBorder = true) {
        Column(Modifier.padding(24.dp)) {
            Text(title.uppercase(), color = colors.textTertiary, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(10.dp))
            Text(valueText, color = colors.textPrimary, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                deltaText,
                color = if (positive) colors.success else colors.danger,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
