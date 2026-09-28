package com.talha.restaurantpos.presentation.screens.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.widget.Toast
import com.talha.restaurantpos.domain.model.Order
import com.talha.restaurantpos.domain.model.OrderStatus
import com.talha.restaurantpos.domain.repository.OrderRepository
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.theme.GlassTheme
import com.talha.restaurantpos.util.CurrencyFormatter
import com.talha.restaurantpos.util.OrderExporter
import com.talha.restaurantpos.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val currency: String = "PKR",
    val restaurantId: String = ""
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val restaurantId = sessionManager.currentRestaurantId() ?: return@launch
            _uiState.value = _uiState.value.copy(restaurantId = restaurantId)
            orderRepository.observeOrders(restaurantId).collect { orders ->
                _uiState.value = _uiState.value.copy(orders = orders)
            }
        }
    }

    fun updateStatus(orderId: String, status: OrderStatus) {
        val restaurantId = _uiState.value.restaurantId
        if (restaurantId.isBlank()) return
        viewModelScope.launch { orderRepository.updateOrderStatus(restaurantId, orderId, status) }
    }

    fun shareLiveLocation(orderId: String, lat: Double, lng: Double) {
        val restaurantId = _uiState.value.restaurantId
        if (restaurantId.isBlank()) return
        viewModelScope.launch { orderRepository.updateLiveLocation(restaurantId, orderId, lat, lng) }
    }
}

private enum class OrderTab(val label: String) { ALL("All"), ACTIVE("Active"), COMPLETED("Completed"), CANCELLED("Cancelled") }

@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(OrderTab.ALL) }
    val context = LocalContext.current

    val filtered = when (selectedTab) {
        OrderTab.ALL -> state.orders
        OrderTab.ACTIVE -> state.orders.filter { it.status in listOf(OrderStatus.NEW, OrderStatus.PREPARING, OrderStatus.READY) }
        OrderTab.COMPLETED -> state.orders.filter { it.status == OrderStatus.COMPLETED }
        OrderTab.CANCELLED -> state.orders.filter { it.status == OrderStatus.CANCELLED }
    }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(
                title = "Orders",
                subtitle = "${state.orders.size} total",
                onBack = onBack,
                actions = {
                    GlassIconButton(
                        icon = Icons.Default.Download,
                        contentDescription = "Download all orders as CSV",
                        onClick = {
                            val fileName = OrderExporter.exportToCsv(context, state.orders, state.currency)
                            val message = if (fileName != null) "Saved to Downloads: $fileName" else "No orders to export"
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        },
                        size = 40.dp
                    )
                }
            )

            Row(
                Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OrderTab.entries.forEach { tab ->
                    GlassChip(tab.label, selected = tab == selectedTab, onClick = { selectedTab = tab })
                }
            }
            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                GlassEmptyState(
                    icon = Icons.Default.Receipt,
                    title = "No Orders",
                    message = "Your completed orders will appear here."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { order ->
                        OrderRow(order, state.currency, onClick = { onOpenOrder(order.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: Order, currency: String, onClick: () -> Unit) {
    val colors = GlassTheme.colors
    val dateFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(order.orderNumber, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    StatusPill(order.status)
                }
                Spacer(Modifier.height(2.dp))
                Text("${order.items.sumOf { it.quantity }} items · ${dateFmt.format(Date(order.createdAt))}", color = colors.textTertiary, style = MaterialTheme.typography.labelSmall)
                if (order.customer.name.isNotBlank()) {
                    Text(order.customer.name, color = colors.textSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(CurrencyFormatter.format(order.total, currency), color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (order.syncStatus.name == "PENDING") {
                    Text("Pending sync", color = colors.warning, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: OrderStatus) {
    val colors = GlassTheme.colors
    val (bg, label) = when (status) {
        OrderStatus.NEW -> colors.accent to "New"
        OrderStatus.PREPARING -> colors.warning to "Preparing"
        OrderStatus.READY -> colors.success to "Ready"
        OrderStatus.COMPLETED -> colors.textTertiary to "Completed"
        OrderStatus.CANCELLED -> colors.danger to "Cancelled"
    }
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .background(bg.copy(alpha = 0.18f), androidx.compose.foundation.shape.RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(label, color = bg, style = MaterialTheme.typography.labelSmall)
    }
}

