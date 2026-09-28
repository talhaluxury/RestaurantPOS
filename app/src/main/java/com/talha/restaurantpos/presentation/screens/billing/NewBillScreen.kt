package com.talha.restaurantpos.presentation.screens.billing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.talha.restaurantpos.domain.model.Product
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.theme.GlassTheme
import com.talha.restaurantpos.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBillScreen(
    onBack: () -> Unit,
    onProceedToPayment: () -> Unit,
    viewModel: BillingViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val state by viewModel.uiState.collectAsState()

    var productForOptions by remember { mutableStateOf<Product?>(null) }
    var showOrderTypeSheet by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showCartSheet by remember { mutableStateOf(false) }
    var discountInput by remember { mutableStateOf("") }
    val cartSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    GlassBackground {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val isTablet = maxWidth >= 840.dp

            Column(Modifier.fillMaxSize()) {
                GlassTopBar(
                    title = "New Bill",
                    onBack = onBack,
                    actions = { OnlineStatusPill(state.isOnline) }
                )

                Column(Modifier.padding(horizontal = 20.dp)) {
                    GlassSearchBar(state.searchQuery, viewModel::setSearchQuery)
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            GlassChip("All", selected = state.selectedCategoryId == null, onClick = { viewModel.selectCategory(null) })
                        }
                        items(state.categories, key = { it.id }) { cat ->
                            GlassChip(cat.name, selected = state.selectedCategoryId == cat.id, onClick = { viewModel.selectCategory(cat.id) })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (isTablet) {
                    Row(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                        ProductGrid(state, viewModel, modifier = Modifier.weight(1.4f), onNeedsOptions = { productForOptions = it })
                        Spacer(Modifier.width(16.dp))
                        CartPanel(
                            state = state,
                            onIncrement = { viewModel.updateLineQuantity(it.lineId, it.quantity + 1) },
                            onDecrement = { viewModel.updateLineQuantity(it.lineId, it.quantity - 1) },
                            onRemove = { viewModel.removeLine(it.lineId) },
                            onEditDiscount = { discountInput = state.discount.toInt().toString(); showDiscountDialog = true },
                            onSaveOrder = { /* Persist as a held order — same as charge flow but skip payment */ },
                            onCharge = { showOrderTypeSheet = true },
                            onClear = viewModel::clearCart,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    ProductGrid(state, viewModel, modifier = Modifier.weight(1f).padding(horizontal = 20.dp), onNeedsOptions = { productForOptions = it })

                    if (state.cart.isNotEmpty()) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            elevated = true, accentBorder = true,
                            onClick = { showCartSheet = true }
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = colors.accent)
                                Spacer(Modifier.width(10.dp))
                                Text("${state.itemCount} items", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                Text(CurrencyFormatter.format(state.total, state.currency), color = colors.accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    productForOptions?.let { product ->
        if (product.variants.isEmpty() && product.addOns.isEmpty()) {
            // Single tap add — sheet not needed (spec §6).
            viewModel.quickAddToCart(product)
            productForOptions = null
        } else {
            ProductOptionsSheet(
                product = product,
                currency = state.currency,
                onDismiss = { productForOptions = null },
                onConfirm = { variant, addOns, qty ->
                    viewModel.addToCart(product, variant, addOns, qty)
                    productForOptions = null
                }
            )
        }
    }

    if (showCartSheet) {
        ModalBottomSheet(onDismissRequest = { showCartSheet = false }, sheetState = cartSheetState, containerColor = androidx.compose.ui.graphics.Color.Transparent) {
            CartPanel(
                state = state,
                onIncrement = { viewModel.updateLineQuantity(it.lineId, it.quantity + 1) },
                onDecrement = { viewModel.updateLineQuantity(it.lineId, it.quantity - 1) },
                onRemove = { viewModel.removeLine(it.lineId) },
                onEditDiscount = { discountInput = state.discount.toInt().toString(); showDiscountDialog = true },
                onSaveOrder = { showCartSheet = false },
                onCharge = { showCartSheet = false; showOrderTypeSheet = true },
                onClear = viewModel::clearCart,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }

    if (showOrderTypeSheet) {
        OrderTypeSheet(
            tables = state.tables,
            initialOrderType = state.orderType,
            initialTableId = state.selectedTableId,
            initialCustomer = state.customer,
            onDismiss = { showOrderTypeSheet = false },
            onConfirm = { type, tableId, customer ->
                viewModel.setOrderType(type)
                tableId?.let { viewModel.selectTable(it) }
                viewModel.setCustomer(customer)
                showOrderTypeSheet = false
                onProceedToPayment()
            }
        )
    }

    if (showDiscountDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDiscountDialog = false }) {
            GlassCard(elevated = true, accentBorder = true) {
                Column(Modifier.padding(24.dp).widthIn(min = 260.dp)) {
                    Text("Apply Discount", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    GlassTextField(discountInput, { discountInput = it.filter { c -> c.isDigit() } }, placeholder = "0", label = "Amount (${state.currency})")
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton("Cancel", { showDiscountDialog = false }, style = GlassButtonStyle.SECONDARY, modifier = Modifier.weight(1f))
                        GlassButton("Apply", {
                            viewModel.setDiscount(discountInput.toDoubleOrNull() ?: 0.0)
                            showDiscountDialog = false
                        }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductGrid(
    state: BillingUiState,
    viewModel: BillingViewModel,
    modifier: Modifier = Modifier,
    onNeedsOptions: (Product) -> Unit
) {
    if (state.filteredProducts.isEmpty()) {
        GlassEmptyState(
            icon = Icons.Default.RestaurantMenu,
            title = "No products found",
            message = "Try a different search or category.",
            modifier = modifier
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(state.filteredProducts, key = { it.id }) { product ->
            val qty = state.cart.filter { it.product.id == product.id }.sumOf { it.quantity }
            GlassProductCard(
                product = product,
                currency = state.currency,
                cartQuantity = qty,
                onClick = {
                    if (product.variants.isEmpty() && product.addOns.isEmpty()) {
                        viewModel.quickAddToCart(product)
                    } else {
                        onNeedsOptions(product)
                    }
                }
            )
        }
    }
}
