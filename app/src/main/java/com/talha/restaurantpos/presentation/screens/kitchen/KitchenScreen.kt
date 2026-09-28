package com.talha.restaurantpos.presentation.screens.kitchen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.talha.restaurantpos.domain.model.Order
import com.talha.restaurantpos.domain.model.OrderStatus
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.screens.orders.OrdersViewModel
import com.talha.restaurantpos.presentation.theme.GlassTheme

@Composable
fun KitchenScreen(
    onBack: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val state by viewModel.uiState.collectAsState()
    val kitchenOrders = state.orders
        .filter { it.status == OrderStatus.NEW || it.status == OrderStatus.PREPARING || it.status == OrderStatus.READY }
        .sortedBy { it.createdAt }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = "Kitchen", subtitle = "${kitchenOrders.size} active orders", onBack = onBack)

            if (kitchenOrders.isEmpty()) {
                GlassEmptyState(Icons.Default.Restaurant, "All Caught Up", "New orders will appear here as they come in.")
            } else {
                LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(kitchenOrders, key = { it.id }) { order ->
                        KitchenOrderCard(order) { newStatus -> viewModel.updateStatus(order.id, newStatus) }
                    }
                }
            }
        }
    }
}

@Composable
private fun KitchenOrderCard(order: Order, onStatusChange: (OrderStatus) -> Unit) {
    val colors = GlassTheme.colors
    GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true, accentBorder = order.status == OrderStatus.READY) {
        Column(Modifier.padding(18.dp)) {
            Text("ORDER ${order.orderNumber}", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            order.tableLabel?.let { Text(it, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(10.dp))
            order.items.forEach { item ->
                Text("${item.quantity}x ${item.name}", color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    "PREPARING", { onStatusChange(OrderStatus.PREPARING) },
                    style = if (order.status == OrderStatus.PREPARING) GlassButtonStyle.PRIMARY else GlassButtonStyle.SECONDARY,
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    "READY", { onStatusChange(OrderStatus.READY) },
                    style = if (order.status == OrderStatus.READY) GlassButtonStyle.SUCCESS else GlassButtonStyle.SECONDARY,
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    "DONE", { onStatusChange(OrderStatus.COMPLETED) },
                    style = GlassButtonStyle.SECONDARY, modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
