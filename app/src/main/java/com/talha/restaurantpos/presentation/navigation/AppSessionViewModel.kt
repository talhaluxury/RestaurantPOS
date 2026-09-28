package com.talha.restaurantpos.presentation.navigation

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talha.restaurantpos.domain.model.Restaurant
import com.talha.restaurantpos.domain.repository.RestaurantRepository
import com.talha.restaurantpos.util.OrderListenerService
import com.talha.restaurantpos.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppSessionViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _restaurant = MutableStateFlow(Restaurant())
    val restaurant: StateFlow<Restaurant> = _restaurant.asStateFlow()

    init {
        viewModelScope.launch {
            val restaurantId = sessionManager.currentRestaurantId() ?: return@launch
            sessionManager.subscribeToOrderNotifications(restaurantId)
            startOrderListenerService()
            restaurantRepository.observeRestaurant(restaurantId).collect { r ->
                if (r != null) _restaurant.value = r
            }
        }
    }

    // Free (no Blaze plan needed) fallback that keeps a background service alive so
    // new-order notifications keep firing even if the app was swiped from Recents.
    private fun startOrderListenerService() {
        val intent = Intent(context, OrderListenerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }
}
