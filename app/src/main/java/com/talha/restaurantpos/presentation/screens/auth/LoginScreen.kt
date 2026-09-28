package com.talha.restaurantpos.presentation.screens.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.talha.restaurantpos.presentation.components.*
import com.talha.restaurantpos.presentation.theme.GlassTheme

@Composable
fun LoginScreen(
    onLoggedIn: (SessionDestination) -> Unit,
    onGoToRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val colors = GlassTheme.colors
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val forgotInteraction = remember { MutableInteractionSource() }
    val registerInteraction = remember { MutableInteractionSource() }

    // Web client ID comes from google-services.json once real Firebase credentials are added
    // (generated as R.string.default_web_client_id by the google-services Gradle plugin).
    val webClientIdRes = remember {
        context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    }
    val googleClient: GoogleSignInClient? = remember(webClientIdRes) {
        if (webClientIdRes == 0) null else {
            val opts = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(webClientIdRes))
                .requestEmail()
                .build()
            GoogleSignIn.getClient(context, opts)
        }
    }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) viewModel.loginWithGoogle(idToken, onLoggedIn)
        } catch (_: ApiException) {
            // Silently ignore user-cancelled sign-in; a real failure surfaces via uiState.error on next attempt.
        }
    }

    GlassBackground {
        Column(Modifier.fillMaxSize()) {
            GlassTopBar(title = "Welcome Back", subtitle = "Log in to continue billing", onBack = onBack)

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
            TabletCentered {
                GlassCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        GlassTextField(
                            value = email, onValueChange = { email = it }, label = "Email",
                            placeholder = "you@restaurant.com", keyboardType = KeyboardType.Email, leadingIcon = Icons.Default.Email
                        )
                        Spacer(Modifier.height(14.dp))
                        GlassTextField(
                            value = password, onValueChange = { password = it }, label = "Password",
                            placeholder = "••••••••", isPassword = true, leadingIcon = Icons.Default.Lock
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Forgot password?",
                            color = colors.accent,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable(interactionSource = forgotInteraction, indication = null, onClick = onForgotPassword)
                        )
                    }
                }

                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(state.error!!, color = colors.danger, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(20.dp))
                GlassButton(
                    text = "LOG IN",
                    onClick = { viewModel.login(email, password, onLoggedIn) },
                    loading = state.loading,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                GlassButton(
                    text = "Continue with Google",
                    onClick = {
                        if (googleClient != null) {
                            googleLauncher.launch(googleClient.signInIntent)
                        }
                        // No Firebase project connected yet — button stays inert until
                        // google-services.json is replaced with real credentials (see file comment).
                    },
                    style = GlassButtonStyle.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
            }
            }

            Row(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Don't have an account? ", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Create one",
                    color = colors.accent,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable(interactionSource = registerInteraction, indication = null, onClick = onGoToRegister)
                )
            }
        }
    }
}
