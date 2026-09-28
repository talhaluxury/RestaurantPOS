package com.talha.restaurantpos.presentation.screens.setup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talha.restaurantpos.BuildConfig
import com.talha.restaurantpos.domain.model.Restaurant
import com.talha.restaurantpos.domain.repository.RestaurantRepository
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.theme.GlassTheme
import com.talha.restaurantpos.util.SampleDataSeeder
import com.talha.restaurantpos.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(val loading: Boolean = false, val error: String? = null)

@HiltViewModel
class RestaurantSetupViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val sessionManager: SessionManager,
    private val sampleDataSeeder: SampleDataSeeder
) : ViewModel() {
    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun createRestaurant(restaurant: Restaurant, onCreated: () -> Unit) {
        if (restaurant.name.isBlank() || restaurant.ownerName.isBlank()) {
            _uiState.value = SetupUiState(error = "Restaurant name and owner name are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = SetupUiState(loading = true)
            restaurantRepository.createRestaurant(restaurant).fold(
                onSuccess = { id ->
                    sessionManager.setRestaurantId(id)
                    sessionManager.setCashierSession(restaurant.ownerName, "OWNER")
                    if (BuildConfig.SEED_SAMPLE_DATA) {
                        runCatching { sampleDataSeeder.seed(id) }
                    }
                    _uiState.value = SetupUiState()
                    onCreated()
                },
                onFailure = { e -> _uiState.value = SetupUiState(error = e.message ?: "Could not create restaurant") }
            )
        }
    }
}

private val currencies = listOf("PKR", "USD", "AED", "SAR", "GBP", "EUR")

@Composable
fun RestaurantSetupScreen(
    onDone: () -> Unit,
    viewModel: RestaurantSetupViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val state by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var currencyIndex by remember { mutableStateOf(0) }
    var footer by remember { mutableStateOf("Thank You! Visit Again") }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = "Create Your Restaurant", subtitle = "This takes less than a minute")
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            ) {
                GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        GlassTextField(name, { name = it }, label = "Restaurant Name", placeholder = "Talha Restaurant", leadingIcon = Icons.Default.Store)
                        Spacer(Modifier.height(14.dp))
                        GlassTextField(ownerName, { ownerName = it }, label = "Owner Name", placeholder = "Ubaid Talha")
                        Spacer(Modifier.height(14.dp))
                        GlassTextField(phone, { phone = it }, label = "Phone", placeholder = "03xx-xxxxxxx", keyboardType = KeyboardType.Phone)
                        Spacer(Modifier.height(14.dp))
                        GlassTextField(address, { address = it }, label = "Address", placeholder = "Street, City")
                        Spacer(Modifier.height(14.dp))
                        Text("Currency", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            currencies.take(3).forEachIndexed { i, c ->
                                GlassChip(c, selected = currencies.indexOf(c) == currencyIndex, onClick = { currencyIndex = currencies.indexOf(c) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            currencies.drop(3).forEachIndexed { i, c ->
                                GlassChip(c, selected = currencies.indexOf(c) == currencyIndex, onClick = { currencyIndex = currencies.indexOf(c) })
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        GlassTextField(footer, { footer = it }, label = "Receipt Footer Message", placeholder = "Thank You! Visit Again")
                    }
                }
                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(state.error!!, color = colors.danger, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(20.dp))
                GlassButton(
                    text = "CREATE RESTAURANT",
                    onClick = {
                        viewModel.createRestaurant(
                            Restaurant(
                                name = name, ownerName = ownerName, phone = phone, address = address,
                                currency = currencies[currencyIndex], receiptFooter = footer
                            ),
                            onDone
                        )
                    },
                    loading = state.loading,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
