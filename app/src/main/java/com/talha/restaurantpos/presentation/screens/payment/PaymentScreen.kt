package com.talha.restaurantpos.presentation.screens.payment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.talha.restaurantpos.domain.model.Order
import com.talha.restaurantpos.domain.model.PaymentMethod
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.theme.GlassTheme
import com.talha.restaurantpos.util.CurrencyFormatter

private val paymentMethods = listOf(
    PaymentMethod.CASH to "Cash",
    PaymentMethod.CARD to "Card",
    PaymentMethod.EASYPAISA to "EasyPaisa",
    PaymentMethod.JAZZCASH to "JazzCash",
    PaymentMethod.BANK to "Bank",
    PaymentMethod.OTHER to "Other"
)

@Composable
fun PaymentScreen(
    pendingOrder: Order,
    currency: String,
    onBack: () -> Unit,
    onPaymentComplete: (Order) -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val state by viewModel.uiState.collectAsState()
    val received = state.amountReceivedText.toDoubleOrNull() ?: 0.0
    val change = (received - pendingOrder.total).coerceAtLeast(0.0)

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = "Payment", onBack = onBack, actions = { OnlineStatusPill(state.isOnline) })

            Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
                GlassCard(elevated = true, accentBorder = true, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL DUE", color = colors.textTertiary, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            CurrencyFormatter.format(pendingOrder.total, currency),
                            color = colors.textPrimary, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text("Payment Method", color = colors.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.heightIn(max = 140.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(paymentMethods) { (method, label) ->
                        GlassChip(label, selected = state.method == method, onClick = { viewModel.selectMethod(method) }, modifier = Modifier.fillMaxWidth())
                    }
                }

                if (state.method == PaymentMethod.CASH) {
                    Spacer(Modifier.height(20.dp))
                    GlassTextField(
                        value = state.amountReceivedText, onValueChange = viewModel::setAmountReceived,
                        label = "Amount Received", placeholder = CurrencyFormatter.format(pendingOrder.total, currency),
                        keyboardType = KeyboardType.Decimal
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Text("Change", color = colors.textSecondary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(CurrencyFormatter.format(change, currency), color = colors.success, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }

                if (state.error != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(state.error!!, color = colors.danger, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(24.dp))
            }

            Column(Modifier.padding(20.dp)) {
                GlassButton(
                    text = "COMPLETE PAYMENT",
                    loading = state.processing,
                    onClick = { viewModel.completePayment(pendingOrder, onPaymentComplete) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
