package com.talha.restaurantpos.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.talha.restaurantpos.presentation.theme.GlassTheme

@Composable
fun GlassDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmText: String = "OK",
    onConfirm: () -> Unit = onDismiss,
    dismissText: String? = null,
    onDismissAction: (() -> Unit)? = null,
    isError: Boolean = false
) {
    val colors = GlassTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        GlassCard(elevated = true, accentBorder = !isError) {
            Column(Modifier.padding(24.dp).widthIn(min = 260.dp)) {
                Text(
                    title,
                    color = if (isError) colors.danger else colors.textPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Text(message, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (dismissText != null) {
                        GlassButton(
                            text = dismissText,
                            onClick = { (onDismissAction ?: onDismiss)() },
                            style = GlassButtonStyle.SECONDARY,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    GlassButton(
                        text = confirmText,
                        onClick = onConfirm,
                        style = if (isError) GlassButtonStyle.DANGER else GlassButtonStyle.PRIMARY,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
