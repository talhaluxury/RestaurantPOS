package com.talha.restaurantpos.presentation.screens.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.talha.restaurantpos.domain.model.Product
import com.talha.restaurantpos.domain.model.ProductVariant
import com.talha.restaurantpos.domain.model.SelectedAddOn
import com.talha.restaurantpos.presentation.components.GlassButton
import com.talha.restaurantpos.presentation.components.GlassIconButton
import com.talha.restaurantpos.presentation.theme.GlassTheme
import com.talha.restaurantpos.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductOptionsSheet(
    product: Product,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (ProductVariant?, List<SelectedAddOn>, Int) -> Unit
) {
    val colors = GlassTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedVariant by remember { mutableStateOf(product.variants.firstOrNull()) }
    val selectedAddOns = remember { mutableStateListOf<SelectedAddOn>() }
    var quantity by remember { mutableIntStateOf(1) }

    val unitPrice = product.price + (selectedVariant?.priceDelta ?: 0.0) + selectedAddOns.sumOf { it.price }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.bg1,
        contentColor = colors.textPrimary
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 24.dp)) {
            Text(product.name, color = colors.textPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (product.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(product.description, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            }

            if (product.variants.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Size", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                product.variants.forEach { variant ->
                    OptionRow(
                        label = variant.name,
                        priceLabel = if (variant.priceDelta != 0.0) "+${CurrencyFormatter.format(variant.priceDelta, currency)}" else "",
                        selected = selectedVariant == variant,
                        onClick = { selectedVariant = variant }
                    )
                }
            }

            if (product.addOns.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Extras", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                product.addOns.forEach { addOn ->
                    val selected = selectedAddOns.any { it.name == addOn.name }
                    OptionRow(
                        label = "+ ${addOn.name}",
                        priceLabel = "+${CurrencyFormatter.format(addOn.price, currency)}",
                        selected = selected,
                        onClick = {
                            if (selected) selectedAddOns.removeAll { it.name == addOn.name }
                            else selectedAddOns.add(SelectedAddOn(addOn.name, addOn.price))
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Quantity", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GlassIconButton(Icons.Default.Remove, "Decrease", { if (quantity > 1) quantity-- }, size = 36.dp)
                    Text(quantity.toString(), color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 18.dp))
                    GlassIconButton(Icons.Default.Add, "Increase", { quantity++ }, size = 36.dp, accented = true)
                }
            }

            Spacer(Modifier.height(24.dp))
            GlassButton(
                text = "ADD TO BILL · ${CurrencyFormatter.format(unitPrice * quantity, currency)}",
                onClick = { onConfirm(selectedVariant, selectedAddOns.toList(), quantity) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OptionRow(label: String, priceLabel: String, selected: Boolean, onClick: () -> Unit) {
    val colors = GlassTheme.colors
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) colors.accent else androidx.compose.ui.graphics.Color.Transparent)
                .border(1.5.dp, if (selected) colors.accent else colors.border, CircleShape)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (priceLabel.isNotBlank()) {
            Text(priceLabel, color = colors.accent, style = MaterialTheme.typography.labelLarge)
        }
    }
}
