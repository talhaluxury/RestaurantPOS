package com.talha.restaurantpos.presentation.screens.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talha.restaurantpos.domain.model.Order
import com.talha.restaurantpos.domain.model.PaymentMethod
import com.talha.restaurantpos.domain.repository.OrderRepository
import com.talha.restaurantpos.util.NetworkMonitor
import com.talha.restaurantpos.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentUiState(
    val method: PaymentMethod = PaymentMethod.CASH,
    val amountReceivedText: String = "",
    val processing: Boolean = false,
    val completedOrder: Order? = null,
    val error: String? = null,
    val isOnline: Boolean = true
)

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.observe().collect { online -> _uiState.value = _uiState.value.copy(isOnline = online) }
        }
    }

    fun selectMethod(method: PaymentMethod) {
        _uiState.value = _uiState.value.copy(method = method, error = null)
    }

    fun setAmountReceived(text: String) {
        _uiState.value = _uiState.value.copy(amountReceivedText = text.filter { it.isDigit() || it == '.' })
    }

    fun completePayment(pendingOrder: Order, onSuccess: (Order) -> Unit) {
        val received = _uiState.value.amountReceivedText.toDoubleOrNull() ?: pendingOrder.total
        if (_uiState.value.method == PaymentMethod.CASH && received < pendingOrder.total) {
            _uiState.value = _uiState.value.copy(error = "Amount received is less than total due")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(processing = true, error = null)
            val restaurantId = sessionManager.currentRestaurantId()
            if (restaurantId == null) {
                _uiState.value = _uiState.value.copy(processing = false, error = "No active restaurant session")
                return@launch
            }
            val orderToPlace = pendingOrder.copy(
                paymentMethod = _uiState.value.method,
                amountReceived = received,
                changeDue = (received - pendingOrder.total).coerceAtLeast(0.0)
            )
            orderRepository.placeOrder(restaurantId, orderToPlace).fold(
                onSuccess = { placed ->
                    _uiState.value = _uiState.value.copy(processing = false, completedOrder = placed)
                    onSuccess(placed)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(processing = false, error = e.message ?: "Payment could not be completed")
                }
            )
        }
    }
}
