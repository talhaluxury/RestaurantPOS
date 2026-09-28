package com.talha.restaurantpos.presentation.screens.billing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.talha.restaurantpos.domain.model.CartLine
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.theme.GlassTheme
import com.talha.restaurantpos.util.CurrencyFormatter

@Composable
fun CartPanel(
    state: BillingUiState,
    onIncrement: (CartLine) -> Unit,
    onDecrement: (CartLine) -> Unit,
    onRemove: (CartLine) -> Unit,
    onEditDiscount: () -> Unit,
    onSaveOrder: () -> Unit,
    onCharge: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = GlassTheme.colors
    GlassCard(modifier = modifier, elevated = true, accentBorder = true) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Current Bill", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${state.itemCount} items", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(10.dp))

            if (state.cart.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("Tap a product to add it here", color = colors.textTertiary, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
                    items(state.cart, key = { it.lineId }) { line ->
                        CartLineRow(line, state.currency, onIncrement, onDecrement, onRemove)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = colors.border)
            Spacer(Modifier.height(10.dp))

            TotalRow("Subtotal", CurrencyFormatter.format(state.subtotal, state.currency))
            TotalRow(
                "Discount", "-${CurrencyFormatter.format(state.discount, state.currency)}",
                onClick = onEditDiscount, accent = true
            )
            if (state.taxPercent > 0) TotalRow("Tax (${state.taxPercent}%)", CurrencyFormatter.format(state.tax, state.currency))
            if (state.serviceChargePercent > 0) TotalRow("Service (${state.serviceChargePercent}%)", CurrencyFormatter.format(state.serviceCharge, state.currency))

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TOTAL", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    CurrencyFormatter.format(state.total, state.currency),
                    color = colors.accent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton("CLEAR", onClear, style = GlassButtonStyle.SECONDARY, modifier = Modifier.weight(1f), enabled = state.cart.isNotEmpty())
                GlassButton("SAVE", onSaveOrder, style = GlassButtonStyle.SECONDARY, modifier = Modifier.weight(1f), enabled = state.cart.isNotEmpty())
            }
            Spacer(Modifier.height(10.dp))
            GlassButton("CHARGE", onCharge, modifier = Modifier.fillMaxWidth(), enabled = state.cart.isNotEmpty())
        }
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    currency: String,
    onIncrement: (CartLine) -> Unit,
    onDecrement: (CartLine) -> Unit,
    onRemove: (CartLine) -> Unit
) {
    val colors = GlassTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(line.product.name, color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            val extras = listOfNotNull(line.variant?.name).plus(line.addOns.map { it.name }).joinToString(", ")
            if (extras.isNotBlank()) {
                Text(extras, color = colors.textTertiary, style = MaterialTheme.typography.labelSmall)
            }
        }
        GlassIconButton(Icons.Default.Remove, "Decrease", { onDecrement(line) }, size = 28.dp)
        Text(line.quantity.toString(), color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 10.dp))
        GlassIconButton(Icons.Default.Add, "Increase", { onIncrement(line) }, size = 28.dp, accented = true)
        Spacer(Modifier.width(10.dp))
        Text(
            CurrencyFormatter.format(line.lineTotal, currency),
            color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.widthIn(min = 64.dp)
        )
    }
}

@Composable
private fun TotalRow(label: String, value: String, onClick: (() -> Unit)? = null, accent: Boolean = false) {
    val colors = GlassTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .then(
                if (onClick != null) Modifier.clickableSimple(onClick) else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text(value, color = if (accent) colors.accent else colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Modifier.clickableSimple(onClick: () -> Unit): Modifier {
    val interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
}
