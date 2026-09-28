package com.talha.restaurantpos.presentation.screens.menu

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.talha.restaurantpos.domain.model.Product
import com.talha.restaurantpos.domain.model.ProductAddOn
import com.talha.restaurantpos.domain.model.ProductVariant
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.theme.GlassTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AddEditProductScreen(
    productId: String?,
    onBack: () -> Unit,
    viewModel: MenuViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val state by viewModel.uiState.collectAsState()
    val existing = remember(productId, state.products) { state.products.find { it.id == productId } }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var description by remember(existing) { mutableStateOf(existing?.description ?: "") }
    var price by remember(existing) { mutableStateOf(existing?.price?.toString() ?: "") }
    var costPrice by remember(existing) { mutableStateOf(existing?.costPrice?.toString() ?: "") }
    var sku by remember(existing) { mutableStateOf(existing?.sku ?: "") }
    var taxPercent by remember(existing) { mutableStateOf(existing?.taxPercent?.toString() ?: "0") }
    var categoryId by remember(existing) { mutableStateOf(existing?.categoryId ?: state.categories.firstOrNull()?.id ?: "") }
    var available by remember(existing) { mutableStateOf(existing?.available ?: true) }
    var featured by remember(existing) { mutableStateOf(existing?.featured ?: false) }
    var imageUrl by remember(existing) { mutableStateOf(existing?.imageUrl ?: "") }
    var localImageUri by remember { mutableStateOf<Uri?>(null) }

    var variants by remember(existing) { mutableStateOf(existing?.variants ?: emptyList()) }
    var addOns by remember(existing) { mutableStateOf(existing?.addOns ?: emptyList()) }
    var newVariantName by remember { mutableStateOf("") }
    var newVariantDelta by remember { mutableStateOf("") }
    var newAddOnName by remember { mutableStateOf("") }
    var newAddOnPrice by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var imagePickError by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        val temp = File.createTempFile("picked_", ".jpg", context.cacheDir)
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            temp.outputStream().use { output -> input.copyTo(output) }
                        } ?: error("Could not read selected image")
                        Uri.fromFile(temp)
                    }
                }.onSuccess { stableUri ->
                    localImageUri = stableUri
                    imagePickError = null
                }.onFailure {
                    imagePickError = "Could not load that image — please try picking it again"
                }
            }
        }
    }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = if (existing != null) "Edit Product" else "Add Product", onBack = onBack)

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            TabletCentered {
                // Image picker
                GlassCard(modifier = Modifier.fillMaxWidth().height(140.dp), onClick = { imagePicker.launch("image/*") }) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val displayUri = localImageUri?.toString() ?: imageUrl
                        if (displayUri.isNotBlank()) {
                            AsyncImage(model = displayUri, contentDescription = "Product image", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = colors.textTertiary, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(6.dp))
                                Text("Tap to add photo", color = colors.textTertiary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                if (imagePickError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(imagePickError!!, color = colors.danger, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(16.dp))
                GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        GlassTextField(name, { name = it }, label = "Product Name", placeholder = "Zinger Burger")
                        Spacer(Modifier.height(12.dp))
                        GlassTextField(description, { description = it }, label = "Description", placeholder = "Crispy fried chicken burger")
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassTextField(price, { price = it }, label = "Price", placeholder = "550", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                            GlassTextField(costPrice, { costPrice = it }, label = "Cost Price", placeholder = "300", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassTextField(sku, { sku = it }, label = "SKU", placeholder = "BRG-001", modifier = Modifier.weight(1f))
                            GlassTextField(taxPercent, { taxPercent = it }, label = "Tax %", placeholder = "0", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                        }

                        if (state.categories.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            Text("Category", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(state.categories) { cat ->
                                    GlassChip(cat.name, selected = cat.id == categoryId, onClick = { categoryId = cat.id })
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Available", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(checked = available, onCheckedChange = { available = it }, colors = SwitchDefaults.colors(checkedTrackColor = colors.accent))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Featured", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(checked = featured, onCheckedChange = { featured = it }, colors = SwitchDefaults.colors(checkedTrackColor = colors.accent))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Variants (e.g. Regular / Large)", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                variants.forEach { v ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${v.name}  (+${v.priceDelta.toInt()})", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        GlassIconButton(Icons.Default.Close, "Remove", { variants = variants - v }, size = 28.dp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassTextField(newVariantName, { newVariantName = it }, placeholder = "Large", modifier = Modifier.weight(1f))
                    GlassTextField(newVariantDelta, { newVariantDelta = it }, placeholder = "+100", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                    GlassIconButton(Icons.Default.Add, "Add variant", {
                        val delta = newVariantDelta.toDoubleOrNull() ?: 0.0
                        if (newVariantName.isNotBlank()) {
                            variants = variants + ProductVariant(newVariantName, delta)
                            newVariantName = ""; newVariantDelta = ""
                        }
                    })
                }

                Spacer(Modifier.height(16.dp))
                Text("Add-ons (e.g. Extra Cheese)", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                addOns.forEach { a ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${a.name}  (+${a.price.toInt()})", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        GlassIconButton(Icons.Default.Close, "Remove", { addOns = addOns - a }, size = 28.dp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlassTextField(newAddOnName, { newAddOnName = it }, placeholder = "Extra Cheese", modifier = Modifier.weight(1f))
                    GlassTextField(newAddOnPrice, { newAddOnPrice = it }, placeholder = "50", keyboardType = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                    GlassIconButton(Icons.Default.Add, "Add add-on", {
                        val p = newAddOnPrice.toDoubleOrNull() ?: 0.0
                        if (newAddOnName.isNotBlank()) {
                            addOns = addOns + ProductAddOn(newAddOnName, p)
                            newAddOnName = ""; newAddOnPrice = ""
                        }
                    })
                }

                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(state.error!!, color = colors.danger, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(20.dp))
                GlassButton(
                    text = when {
                        state.saving && localImageUri != null -> "UPLOADING IMAGE…"
                        existing != null -> "SAVE CHANGES"
                        else -> "ADD PRODUCT"
                    },
                    loading = state.saving,
                    onClick = {
                        val product = Product(
                            id = existing?.id ?: "",
                            name = name, description = description,
                            price = price.toDoubleOrNull() ?: 0.0,
                            costPrice = costPrice.toDoubleOrNull() ?: 0.0,
                            imageUrl = imageUrl,
                            categoryId = categoryId, sku = sku, available = available, featured = featured,
                            taxPercent = taxPercent.toDoubleOrNull() ?: 0.0,
                            variants = variants, addOns = addOns
                        )
                        viewModel.saveProduct(product, localImageUri?.toString(), onBack)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
            }
        }
    }
}
