package com.talha.restaurantpos.presentation.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.theme.GlassTheme

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val state by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = "Create Account", subtitle = "Start billing in minutes", onBack = onBack)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            ) {
            TabletCentered {
                GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        GlassTextField(email, { email = it }, label = "Email", placeholder = "you@restaurant.com", keyboardType = KeyboardType.Email, leadingIcon = Icons.Default.Email)
                        Spacer(Modifier.height(14.dp))
                        GlassTextField(password, { password = it }, label = "Password", placeholder = "At least 6 characters", isPassword = true, leadingIcon = Icons.Default.Lock)
                        Spacer(Modifier.height(14.dp))
                        GlassTextField(confirmPassword, { confirmPassword = it }, label = "Confirm Password", placeholder = "Re-enter password", isPassword = true, leadingIcon = Icons.Default.Lock)
                    }
                }
                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(state.error!!, color = colors.danger, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(20.dp))
                GlassButton(
                    text = "CREATE ACCOUNT",
                    onClick = { viewModel.register(email, password, confirmPassword, onRegistered) },
                    loading = state.loading,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val state by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = "Reset Password", onBack = onBack)
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TabletCentered {
                if (sent) {
                    Spacer(Modifier.height(40.dp))
                    Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = colors.accent, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Check your email", color = colors.textPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("We sent a password reset link to $email", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                } else {
                    GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Enter the email linked to your account and we'll send you a reset link.", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(14.dp))
                            GlassTextField(email, { email = it }, label = "Email", placeholder = "you@restaurant.com", keyboardType = KeyboardType.Email, leadingIcon = Icons.Default.Email)
                        }
                    }
                    if (state.error != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(state.error!!, color = colors.danger, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(20.dp))
                    GlassButton(
                        text = "SEND RESET LINK",
                        onClick = { viewModel.sendPasswordReset(email) { sent = true } },
                        loading = state.loading,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            }
        }
    }
}
