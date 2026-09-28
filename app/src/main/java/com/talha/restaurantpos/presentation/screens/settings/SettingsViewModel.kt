package com.talha.restaurantpos.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talha.restaurantpos.domain.model.Restaurant
import com.talha.restaurantpos.domain.repository.AuthRepository
import com.talha.restaurantpos.domain.repository.RestaurantRepository
import com.talha.restaurantpos.printer.PairedPrinter
import com.talha.restaurantpos.printer.PaperWidth
import com.talha.restaurantpos.printer.PrinterResult
import com.talha.restaurantpos.printer.ThermalPrinter
import com.talha.restaurantpos.util.SessionManager
import com.talha.restaurantpos.util.ThemePref
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val restaurant: Restaurant = Restaurant(),
    val restaurantId: String = "",
    val theme: ThemePref = ThemePref.SYSTEM,
    val pairedPrinters: List<PairedPrinter> = emptyList(),
    val connectedPrinterAddress: String? = null,
    val paperWidth: PaperWidth = PaperWidth.MM_80,
    val printerMessage: String? = null,
    val saving: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val restaurantRepository: RestaurantRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val thermalPrinter: ThermalPrinter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val restaurantId = sessionManager.currentRestaurantId() ?: return@launch
            _uiState.value = _uiState.value.copy(restaurantId = restaurantId)
            launch {
                restaurantRepository.observeRestaurant(restaurantId).collect { r ->
                    if (r != null) _uiState.value = _uiState.value.copy(restaurant = r)
                }
            }
            launch {
                sessionManager.themeFlow.collect { t -> _uiState.value = _uiState.value.copy(theme = t) }
            }
            launch {
                sessionManager.paperWidthFlow.collect { w ->
                    _uiState.value = _uiState.value.copy(paperWidth = PaperWidth.valueOf(w))
                }
            }
            // Silently reconnect to the last-used printer on launch, so staff don't have to
            // re-pair every time the app is reopened.
            sessionManager.printerAddressFlow.first()?.let { address ->
                if (address.isNotBlank()) connectPrinter(address, silent = true)
            }
        }
    }

    fun saveRestaurant(restaurant: Restaurant) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true)
            restaurantRepository.updateRestaurant(restaurant)
            _uiState.value = _uiState.value.copy(saving = false)
        }
    }

    fun setTheme(theme: ThemePref) {
        viewModelScope.launch { sessionManager.setTheme(theme) }
    }

    fun refreshPairedPrinters() {
        viewModelScope.launch {
            val list = thermalPrinter.listPairedPrinters()
            _uiState.value = _uiState.value.copy(pairedPrinters = list)
        }
    }

    fun connectPrinter(address: String, silent: Boolean = false) {
        viewModelScope.launch {
            when (val result = thermalPrinter.connect(address)) {
                is PrinterResult.Success -> {
                    sessionManager.setPrinterAddress(address)
                    _uiState.value = _uiState.value.copy(
                        connectedPrinterAddress = address,
                        printerMessage = if (silent) null else "Printer connected"
                    )
                }
                is PrinterResult.Failure -> {
                    if (!silent) _uiState.value = _uiState.value.copy(printerMessage = result.message)
                }
            }
        }
    }

    fun testPrint() {
        viewModelScope.launch {
            val result = thermalPrinter.testPrint()
            _uiState.value = _uiState.value.copy(
                printerMessage = when (result) {
                    is PrinterResult.Success -> "Test print sent"
                    is PrinterResult.Failure -> result.message
                }
            )
        }
    }

    fun setPaperWidth(width: PaperWidth) {
        _uiState.value = _uiState.value.copy(paperWidth = width)
        viewModelScope.launch { sessionManager.setPaperWidth(width.name) }
    }

    fun logout(onLoggedOut: () -> Unit) {
        authRepository.logout()
        viewModelScope.launch { sessionManager.clear() }
        onLoggedOut()
    }
}
