package com.talha.restaurantpos.presentation.screens.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.talha.restaurantpos.domain.model.CustomerInfo
import com.talha.restaurantpos.domain.model.OrderType
import com.talha.restaurantpos.domain.model.RestaurantTable
import com.talha.restaurantpos.domain.model.TableStatus
import com.talha.restaurantpos.presentation.components.GlassButton
import com.talha.restaurantpos.presentation.components.GlassSegmentedControl
import com.talha.restaurantpos.presentation.components.GlassTextField
import com.talha.restaurantpos.presentation.theme.GlassTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTypeSheet(
    tables: List<RestaurantTable>,
    initialOrderType: OrderType,
    initialTableId: String?,
    initialCustomer: CustomerInfo,
    onDismiss: () -> Unit,
    onConfirm: (OrderType, String?, CustomerInfo) -> Unit
) {
    val colors = GlassTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var orderType by remember { mutableStateOf(initialOrderType) }
    var tableId by remember { mutableStateOf(initialTableId) }
    var name by remember { mutableStateOf(initialCustomer.name) }
    var phone by remember { mutableStateOf(initialCustomer.phone) }
    var address by remember { mutableStateOf(initialCustomer.address) }

    val isDelivery = orderType == OrderType.DELIVERY
    val canConfirm = orderType != OrderType.DINE_IN || tableId != null

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = colors.bg1, contentColor = colors.textPrimary) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Order Type", color = colors.textPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            GlassSegmentedControl(
                options = listOf("DINE IN", "TAKEAWAY", "DELIVERY"),
                selectedIndex = OrderType.entries.indexOf(orderType),
                onSelect = { orderType = OrderType.entries[it] }
            )

            if (orderType == OrderType.DINE_IN) {
                Spacer(Modifier.height(20.dp))
                Text("Select Table", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                if (tables.isEmpty()) {
                    Text("No tables configured yet. Add tables from Settings.", color = colors.textTertiary, style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.heightIn(max = 220.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tables) { table ->
                            val selected = table.id == tableId
                            val enabled = table.status != TableStatus.OCCUPIED || selected
                            TableChip(table, selected, enabled) { if (enabled) tableId = table.id }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                if (isDelivery) "Customer Info (required)" else "Customer Info (optional)",
                color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            GlassTextField(name, { name = it }, placeholder = "Customer name")
            Spacer(Modifier.height(10.dp))
            GlassTextField(phone, { phone = it }, placeholder = "Phone number", keyboardType = KeyboardType.Phone)
            if (isDelivery) {
                Spacer(Modifier.height(10.dp))
                GlassTextField(address, { address = it }, placeholder = "Delivery address")
            }

            Spacer(Modifier.height(24.dp))
            GlassButton(
                text = "CONFIRM",
                enabled = canConfirm && (!isDelivery || (name.isNotBlank() && phone.isNotBlank() && address.isNotBlank())),
                onClick = { onConfirm(orderType, tableId, CustomerInfo(name, phone, address)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun TableChip(table: RestaurantTable, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = GlassTheme.colors
    val bg = when {
        selected -> colors.accent
        table.status == TableStatus.OCCUPIED -> colors.danger.copy(alpha = 0.15f)
        table.status == TableStatus.RESERVED -> colors.warning.copy(alpha = 0.15f)
        else -> colors.surface
    }
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(14.dp))
            .then(
                if (enabled) Modifier.androidxClickable(interactionSource, onClick) else Modifier
            )
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(table.label, color = if (selected) androidx.compose.ui.graphics.Color.Black else colors.textPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(
            table.status.name.lowercase().replaceFirstChar { it.uppercase() },
            color = if (selected) androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.7f) else colors.textTertiary,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun Modifier.androidxClickable(interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource, onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
