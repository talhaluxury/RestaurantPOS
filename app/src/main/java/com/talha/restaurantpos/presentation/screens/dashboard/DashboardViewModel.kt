package com.talha.restaurantpos.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talha.restaurantpos.domain.model.Restaurant
import com.talha.restaurantpos.domain.model.SalesSummary
import com.talha.restaurantpos.domain.repository.OrderRepository
import com.talha.restaurantpos.domain.repository.RestaurantRepository
import com.talha.restaurantpos.util.NetworkMonitor
import com.talha.restaurantpos.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val restaurant: Restaurant = Restaurant(),
    val todaySales: SalesSummary = SalesSummary(),
    val yesterdaySales: SalesSummary = SalesSummary(),
    val isOnline: Boolean = true,
    val cashierName: String = "",
    val loading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val restaurantRepository: RestaurantRepository,
    private val orderRepository: OrderRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val restaurantId = sessionManager.currentRestaurantId() ?: return@launch

            launch {
                restaurantRepository.observeRestaurant(restaurantId).collect { r ->
                    if (r != null) _uiState.value = _uiState.value.copy(restaurant = r, loading = false)
                }
            }
            launch {
                networkMonitor.observe().collect { online ->
                    _uiState.value = _uiState.value.copy(isOnline = online)
                }
            }
            launch {
                sessionManager.cashierNameFlow.collect { name ->
                    _uiState.value = _uiState.value.copy(cashierName = name)
                }
            }
            // Orders flow drives sales recompute whenever anything changes locally (offline-safe).
            launch {
                orderRepository.observeOrders(restaurantId).collect {
                    refreshSales(restaurantId)
                }
            }
        }
    }

    private suspend fun refreshSales(restaurantId: String) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000L
        val yesterdayStart = todayStart - 24 * 60 * 60 * 1000L

        val today = orderRepository.getSalesSummary(restaurantId, todayStart, todayEnd)
        val yesterday = orderRepository.getSalesSummary(restaurantId, yesterdayStart, todayStart)
        _uiState.value = _uiState.value.copy(todaySales = today, yesterdaySales = yesterday)
    }
}

fun percentDelta(today: Double, yesterday: Double): Pair<String, Boolean> {
    if (yesterday <= 0.0) return if (today > 0) "+100%" to true else "0%" to true
    val pct = ((today - yesterday) / yesterday) * 100
    val sign = if (pct >= 0) "+" else ""
    return "$sign${"%.1f".format(pct)}%" to (pct >= 0)
}
