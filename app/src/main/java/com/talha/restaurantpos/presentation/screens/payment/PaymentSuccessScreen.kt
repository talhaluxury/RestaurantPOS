package com.talha.restaurantpos.presentation.screens.payment

import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import com.talha.restaurantpos.domain.model.Order
import com.talha.restaurantpos.domain.model.Restaurant
import com.talha.restaurantpos.presentation.components.GlassButton
import com.talha.restaurantpos.presentation.components.GlassButtonStyle
import com.talha.restaurantpos.presentation.components.GlassCard
import com.talha.restaurantpos.presentation.components.GlassChip
import com.talha.restaurantpos.presentation.components.GlassBackground
import com.talha.restaurantpos.presentation.theme.GlassTheme
import com.talha.restaurantpos.receipt.ReceiptFormat
import com.talha.restaurantpos.receipt.ReceiptPdfGenerator
import com.talha.restaurantpos.util.CurrencyFormatter

@Composable
fun PaymentSuccessScreen(
    order: Order,
    restaurant: Restaurant,
    onDone: () -> Unit,
    onNewBill: () -> Unit
) {
    val colors = GlassTheme.colors
    val context = LocalContext.current
    var format by remember { mutableStateOf(ReceiptFormat.THERMAL_80MM) }
    val printerViewModel: ReceiptPrinterViewModel = hiltViewModel()

    val scale = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    fun sharePdf(action: String) {
        val file = ReceiptPdfGenerator.generate(context, restaurant, order, format)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (action == "whatsapp") `package` = "com.whatsapp"
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Share Receipt")) }
    }

    fun printPdf() {
        printerViewModel.printReceipt(context, restaurant, order) { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    GlassBackground {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Box(
                Modifier.size(90.dp).scale(scale.value).clip(CircleShape).background(colors.success.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = colors.success, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Payment Completed", color = colors.textPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Order ${order.orderNumber}", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(24.dp))
            GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true) {
                Column(Modifier.padding(18.dp)) {
                    order.items.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text("${item.quantity}x ${item.name}", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(CurrencyFormatter.format(item.lineTotal, restaurant.currency), color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("TOTAL", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(CurrencyFormatter.format(order.total, restaurant.currency), color = colors.accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassChip("A4", format == ReceiptFormat.A4, { format = ReceiptFormat.A4 })
                GlassChip("80mm", format == ReceiptFormat.THERMAL_80MM, { format = ReceiptFormat.THERMAL_80MM })
                GlassChip("58mm", format == ReceiptFormat.THERMAL_58MM, { format = ReceiptFormat.THERMAL_58MM })
            }

            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton("Share", { sharePdf("share") }, style = GlassButtonStyle.SECONDARY, icon = Icons.Default.Share, modifier = Modifier.weight(1f))
                GlassButton("Print", { printPdf() }, style = GlassButtonStyle.SECONDARY, icon = Icons.Default.Print, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            GlassButton("NEW BILL", onNewBill, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            GlassButton("Done", onDone, style = GlassButtonStyle.SECONDARY, modifier = Modifier.fillMaxWidth())
        }
    }
}
