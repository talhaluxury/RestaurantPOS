package com.talha.restaurantpos.presentation.screens.orders

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.talha.restaurantpos.domain.model.Order
import com.talha.restaurantpos.domain.model.OrderStatus
import com.talha.restaurantpos.domain.model.OrderType
import com.talha.restaurantpos.domain.model.Restaurant
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.screens.payment.ReceiptPrinterViewModel
import com.talha.restaurantpos.presentation.theme.GlassTheme
import com.talha.restaurantpos.receipt.ReceiptFormat
import com.talha.restaurantpos.receipt.ReceiptPdfGenerator
import com.talha.restaurantpos.util.CurrencyFormatter
import com.talha.restaurantpos.util.PdfPrintDocumentAdapter
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@Composable
fun OrderDetailScreen(
    orderId: String,
    restaurant: Restaurant,
    onBack: () -> Unit,
    onDuplicateToNewBill: (Order) -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val state by viewModel.uiState.collectAsState()
    val order = state.orders.find { it.id == orderId }
    val context = LocalContext.current
    val printerViewModel: ReceiptPrinterViewModel = hiltViewModel()
    var showRefundConfirm by remember { mutableStateOf(false) }

    // Delivery progress written by the Rider app (riderStatus on the same Firestore order doc).
    var riderStatus by remember { mutableStateOf<String?>(null) }
    DisposableEffect(orderId, restaurant.id) {
        if (restaurant.id.isBlank()) return@DisposableEffect onDispose { }
        val reg = FirebaseFirestore.getInstance().collection("restaurants").document(restaurant.id)
            .collection("orders").document(orderId)
            .addSnapshotListener { snap, _ -> riderStatus = snap?.getString("riderStatus") }
        onDispose { reg.remove() }
    }

    var locationPermissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        locationPermissionGranted = granted
    }
    var isSharingLocation by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Polls the device's current position every ~8s and writes it to the order doc while this
    // screen is open and sharing is on, so the customer's tracking page can show it moving live.
    // Sharing only runs while this screen stays open (no background service) — closing it, or the
    // app, stops updates until reopened and toggled on again.
    LaunchedEffect(isSharingLocation, order?.id) {
        val activeOrderId = order?.id ?: return@LaunchedEffect
        while (isSharingLocation) {
            if (locationPermissionGranted) {
                runCatching {
                    val loc = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
                    if (loc != null) viewModel.shareLiveLocation(activeOrderId, loc.latitude, loc.longitude)
                }
            }
            delay(8000)
        }
    }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = order?.orderNumber ?: "Order", onBack = onBack)

            if (order == null) {
                GlassEmptyState(icon = Icons.Default.Print, title = "Order not found", message = "It may still be syncing.")
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                    riderStatus?.let { rs ->
                        val label = when (rs) {
                            "ACCEPTED" -> "Rider Accepted"
                            "ARRIVED" -> "Rider Arrived at Restaurant"
                            "PICKED_UP" -> "Order Picked Up"
                            "ON_THE_WAY" -> "Rider On the Way"
                            "DELIVERED" -> "Delivered \u2713"
                            else -> rs
                        }
                        GlassCard(accentBorder = true, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Delivery: $label", color = colors.accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            order.items.forEach { item ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text("${item.quantity}x ${item.name}", color = colors.textPrimary, style = MaterialTheme.typography.bodyLarge)
                                        val extras = listOfNotNull(item.variantName).plus(item.addOnNames).joinToString(", ")
                                        if (extras.isNotBlank()) Text(extras, color = colors.textTertiary, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(CurrencyFormatter.format(item.lineTotal, restaurant.currency), color = colors.textSecondary, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            androidx.compose.material3.HorizontalDivider(color = colors.border)
                            Spacer(Modifier.height(10.dp))
                            row("Subtotal", CurrencyFormatter.format(order.subtotal, restaurant.currency))
                            if (order.discount > 0) row("Discount", "-" + CurrencyFormatter.format(order.discount, restaurant.currency))
                            if (order.tax > 0) row("Tax", CurrencyFormatter.format(order.tax, restaurant.currency))
                            if (order.serviceCharge > 0) row("Service", CurrencyFormatter.format(order.serviceCharge, restaurant.currency))
                            Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth()) {
                                Text("TOTAL", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(CurrencyFormatter.format(order.total, restaurant.currency), color = colors.accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            row("Type", order.orderType.name.replace("_", " "))
                            order.tableLabel?.let { row("Table", it) }
                            if (order.customer.name.isNotBlank()) row("Customer", order.customer.name)
                            if (order.customer.phone.isNotBlank()) row("Phone", order.customer.phone)
                            row("Payment", order.paymentMethod?.name ?: "-")
                            row("Cashier", order.cashierName)
                            row("Sync", if (order.syncStatus.name == "SYNCED") "Synced" else "Pending sync")
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Update Status", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.COMPLETED).forEach { s ->
                            GlassChip(s.name, selected = order.status == s, onClick = { viewModel.updateStatus(order.id, s) })
                        }
                    }

                    if (order.orderType == OrderType.DELIVERY && order.status in listOf(OrderStatus.PREPARING, OrderStatus.READY)) {
                        Spacer(Modifier.height(16.dp))
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Share Live Location", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "Lets the customer see this delivery moving on the tracking map",
                                            color = colors.textTertiary, style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Switch(
                                        checked = isSharingLocation,
                                        onCheckedChange = { turnOn ->
                                            if (turnOn && !locationPermissionGranted) {
                                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                            }
                                            isSharingLocation = turnOn
                                        },
                                        colors = SwitchDefaults.colors(checkedTrackColor = colors.accent)
                                    )
                                }
                                if (isSharingLocation && !locationPermissionGranted) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("Location permission needed to share position", color = colors.danger, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton("Share", {
                            val file = ReceiptPdfGenerator.generate(context, restaurant, order, ReceiptFormat.THERMAL_80MM)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "application/pdf"; putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching { context.startActivity(android.content.Intent.createChooser(intent, "Share Receipt")) }
                        }, style = GlassButtonStyle.SECONDARY, icon = Icons.Default.Share, modifier = Modifier.weight(1f))

                        GlassButton("Print", {
                            printerViewModel.printReceipt(context, restaurant, order) { message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }, style = GlassButtonStyle.SECONDARY, icon = Icons.Default.Print, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassButton("Duplicate Order", { onDuplicateToNewBill(order) }, style = GlassButtonStyle.SECONDARY, icon = Icons.Default.ContentCopy, modifier = Modifier.weight(1f))
                        GlassButton(
                            "Refund / Cancel", { showRefundConfirm = true }, style = GlassButtonStyle.DANGER,
                            modifier = Modifier.weight(1f), enabled = order.status != OrderStatus.CANCELLED
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showRefundConfirm && order != null) {
        GlassDialog(
            title = "Cancel this order?",
            message = "This marks the order as cancelled and excludes it from sales reports.",
            onDismiss = { showRefundConfirm = false },
            confirmText = "Cancel Order",
            dismissText = "Keep Order",
            isError = true,
            onConfirm = {
                viewModel.updateStatus(order.id, OrderStatus.CANCELLED)
                showRefundConfirm = false
            }
        )
    }
}

@Composable
private fun row(label: String, value: String) {
    val colors = GlassTheme.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}
