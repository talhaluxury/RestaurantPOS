package com.talha.restaurantpos.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talha.restaurantpos.domain.repository.AuthRepository
import com.talha.restaurantpos.domain.repository.RestaurantRepository
import com.talha.restaurantpos.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val loading: Boolean = false,
    val error: String? = null
)

/** Result of checking session on app start: where should navigation land? */
sealed class SessionDestination {
    object Splash : SessionDestination()
    object Welcome : SessionDestination()
    object NeedsRestaurantSetup : SessionDestination()
    object Dashboard : SessionDestination()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val restaurantRepository: RestaurantRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _destination = MutableStateFlow<SessionDestination>(SessionDestination.Splash)
    val destination: StateFlow<SessionDestination> = _destination.asStateFlow()

    fun checkSession() {
        viewModelScope.launch {
            if (!authRepository.isLoggedIn()) {
                _destination.value = SessionDestination.Welcome
                return@launch
            }
            val restaurantId = restaurantRepository.getMyRestaurantId()
            if (!restaurantId.isNullOrBlank()) sessionManager.setRestaurantId(restaurantId)
            _destination.value = if (restaurantId.isNullOrBlank()) {
                SessionDestination.NeedsRestaurantSetup
            } else {
                SessionDestination.Dashboard
            }
        }
    }

    fun login(email: String, password: String, onResult: (SessionDestination) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(error = "Enter email and password")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            authRepository.loginWithEmail(email.trim(), password).fold(
                onSuccess = {
                    val restaurantId = restaurantRepository.getMyRestaurantId()
                    if (!restaurantId.isNullOrBlank()) sessionManager.setRestaurantId(restaurantId)
                    _uiState.value = AuthUiState()
                    onResult(if (restaurantId.isNullOrBlank()) SessionDestination.NeedsRestaurantSetup else SessionDestination.Dashboard)
                },
                onFailure = { e -> _uiState.value = AuthUiState(error = e.message ?: "Login failed") }
            )
        }
    }

    fun register(email: String, password: String, confirmPassword: String, onResult: () -> Unit) {
        if (email.isBlank() || password.length < 6) {
            _uiState.value = AuthUiState(error = "Password must be at least 6 characters")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState(error = "Passwords do not match")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            authRepository.registerWithEmail(email.trim(), password).fold(
                onSuccess = {
                    _uiState.value = AuthUiState()
                    onResult()
                },
                onFailure = { e -> _uiState.value = AuthUiState(error = e.message ?: "Registration failed") }
            )
        }
    }

    fun loginWithGoogle(idToken: String, onResult: (SessionDestination) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            authRepository.loginWithGoogleIdToken(idToken).fold(
                onSuccess = {
                    val restaurantId = restaurantRepository.getMyRestaurantId()
                    if (!restaurantId.isNullOrBlank()) sessionManager.setRestaurantId(restaurantId)
                    _uiState.value = AuthUiState()
                    onResult(if (restaurantId.isNullOrBlank()) SessionDestination.NeedsRestaurantSetup else SessionDestination.Dashboard)
                },
                onFailure = { e -> _uiState.value = AuthUiState(error = e.message ?: "Google sign-in failed") }
            )
        }
    }

    fun sendPasswordReset(email: String, onSent: () -> Unit) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState(error = "Enter your email")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(loading = true)
            authRepository.sendPasswordReset(email.trim()).fold(
                onSuccess = { _uiState.value = AuthUiState(); onSent() },
                onFailure = { e -> _uiState.value = AuthUiState(error = e.message ?: "Could not send reset email") }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
