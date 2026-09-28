package com.talha.restaurantpos.presentation.screens.menu

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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

@Composable
fun MenuManagementScreen(
    onBack: () -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (String) -> Unit,
    viewModel: MenuViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val state by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("") }
    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var productPendingDelete by remember { mutableStateOf<Product?>(null) }

    val filtered = state.products.filter { it.name.contains(query, ignoreCase = true) }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(
                title = "Menu Management",
                subtitle = "${state.products.size} products · ${state.categories.size} categories",
                onBack = onBack,
                actions = { GlassIconButton(Icons.Default.Add, "Add Product", onAddProduct, accented = true) }
            )

            Column(Modifier.padding(horizontal = 20.dp)) {
                GlassSearchBar(query, { query = it }, placeholder = "Search products")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip("+ Category", selected = false, onClick = { showAddCategory = true })
                }
            }

            Spacer(Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                GlassEmptyState(
                    icon = Icons.Default.RestaurantMenu,
                    title = "No Menu Items",
                    message = "Add your first product to start taking orders.",
                    actionText = "ADD PRODUCT",
                    onAction = onAddProduct
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { product ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), onClick = { onEditProduct(product.id) }) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.name, color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(2.dp))
                                    Text(CurrencyFormatter.format(product.price, state.currency), color = colors.accent, style = MaterialTheme.typography.labelLarge)
                                }
                                Switch(
                                    checked = product.available,
                                    onCheckedChange = { viewModel.toggleAvailability(product) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = colors.accent)
                                )
                                Spacer(Modifier.width(4.dp))
                                GlassIconButton(Icons.Default.Edit, "Edit", { onEditProduct(product.id) }, size = 36.dp)
                                Spacer(Modifier.width(4.dp))
                                GlassIconButton(Icons.Default.Delete, "Delete", { productPendingDelete = product }, size = 36.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCategory) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddCategory = false }) {
            GlassCard(elevated = true, accentBorder = true) {
                Column(Modifier.padding(24.dp).widthIn(min = 260.dp)) {
                    Text("Add Category", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    GlassTextField(newCategoryName, { newCategoryName = it }, placeholder = "e.g. Burgers")
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton("Cancel", { showAddCategory = false }, style = GlassButtonStyle.SECONDARY, modifier = Modifier.weight(1f))
                        GlassButton(
                            "Add",
                            {
                                viewModel.addCategory(newCategoryName)
                                newCategoryName = ""
                                showAddCategory = false
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    productPendingDelete?.let { product ->
        GlassDialog(
            title = "Delete ${product.name}?",
            message = "This will remove the product from your menu. This cannot be undone.",
            onDismiss = { productPendingDelete = null },
            confirmText = "Delete",
            dismissText = "Cancel",
            isError = true,
            onConfirm = {
                viewModel.deleteProduct(product.id)
                productPendingDelete = null
            }
        )
    }
}
