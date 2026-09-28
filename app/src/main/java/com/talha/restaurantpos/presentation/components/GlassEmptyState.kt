package com.talha.restaurantpos.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.talha.restaurantpos.presentation.theme.GlassTheme

@Composable
fun GlassEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionText: String? = null,
    modifier: Modifier = Modifier,
    onAction: (() -> Unit)? = null
) {
    val colors = GlassTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            GlassButton(text = actionText, onClick = onAction)
        }
    }
}
