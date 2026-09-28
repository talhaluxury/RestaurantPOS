package com.talha.restaurantpos.presentation.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talha.restaurantpos.domain.model.PaymentMethod
import com.talha.restaurantpos.domain.model.SalesSummary
import com.talha.restaurantpos.domain.repository.OrderRepository
import com.talha.restaurantpos.domain.repository.RestaurantRepository
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.theme.GlassTheme
import com.talha.restaurantpos.util.CurrencyFormatter
import com.talha.restaurantpos.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class ReportRange(val label: String) { TODAY("Today"), YESTERDAY("Yesterday"), THIS_WEEK("This Week"), THIS_MONTH("This Month") }

data class ReportsUiState(
    val summary: SalesSummary = SalesSummary(),
    val currency: String = "PKR",
    val restaurantId: String = ""
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val restaurantRepository: RestaurantRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()
    private var restaurantId: String = ""

    init {
        viewModelScope.launch {
            restaurantId = sessionManager.currentRestaurantId() ?: return@launch
            _uiState.value = _uiState.value.copy(restaurantId = restaurantId)
            restaurantRepository.observeRestaurant(restaurantId).collect { r ->
                if (r != null) _uiState.value = _uiState.value.copy(currency = r.currency)
            }
        }
        loadRange(ReportRange.TODAY)
    }

    fun loadRange(range: ReportRange) {
        viewModelScope.launch {
            if (restaurantId.isBlank()) restaurantId = sessionManager.currentRestaurantId() ?: return@launch
            val (from, to) = rangeMillis(range)
            val summary = orderRepository.getSalesSummary(restaurantId, from, to)
            _uiState.value = _uiState.value.copy(summary = summary)
        }
    }

    private fun rangeMillis(range: ReportRange): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        return when (range) {
            ReportRange.TODAY -> todayStart to (todayStart + 86_400_000L)
            ReportRange.YESTERDAY -> (todayStart - 86_400_000L) to todayStart
            ReportRange.THIS_WEEK -> {
                val weekCal = cal.clone() as Calendar
                weekCal.set(Calendar.DAY_OF_WEEK, weekCal.firstDayOfWeek)
                weekCal.timeInMillis to (todayStart + 86_400_000L)
            }
            ReportRange.THIS_MONTH -> {
                val monthCal = cal.clone() as Calendar
                monthCal.set(Calendar.DAY_OF_MONTH, 1)
                monthCal.timeInMillis to (todayStart + 86_400_000L)
            }
        }
    }
}

@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val state by viewModel.uiState.collectAsState()
    var selectedRange by remember { mutableStateOf(ReportRange.TODAY) }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = "Reports", subtitle = "Sales analytics", onBack = onBack)

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportRange.entries.forEach { range ->
                        GlassChip(range.label, selected = range == selectedRange, onClick = {
                            selectedRange = range
                            viewModel.loadRange(range)
                        })
                    }
                }

                Spacer(Modifier.height(16.dp))
                GlassHeroStatCard(
                    title = "Total Revenue",
                    valueText = CurrencyFormatter.format(state.summary.totalRevenue, state.currency),
                    deltaText = "${state.summary.orderCount} orders",
                    positive = true
                )

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassStatCard("Avg Order Value", CurrencyFormatter.format(state.summary.averageOrderValue, state.currency), modifier = Modifier.weight(1f))
                    GlassStatCard("Items Sold", state.summary.itemsSold.toString(), modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassStatCard("Discounts Given", CurrencyFormatter.format(state.summary.totalDiscount, state.currency), modifier = Modifier.weight(1f))
                    GlassStatCard("Tax Collected", CurrencyFormatter.format(state.summary.totalTax, state.currency), modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))
                Text("Payment Breakdown", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (state.summary.paymentBreakdown.isEmpty()) {
                    Text("No payments recorded for this period.", color = colors.textTertiary, style = MaterialTheme.typography.bodyMedium)
                } else {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            PaymentMethod.entries.forEach { method ->
                                val amount = state.summary.paymentBreakdown[method] ?: 0.0
                                if (amount > 0) {
                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                        Text(method.name, color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        Text(CurrencyFormatter.format(amount, state.currency), color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
