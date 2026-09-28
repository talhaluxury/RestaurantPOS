package com.talha.restaurantpos.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.talha.restaurantpos.presentation.components.GlassBackground
import com.talha.restaurantpos.presentation.components.GlassButton
import com.talha.restaurantpos.presentation.components.GlassButtonStyle
import com.talha.restaurantpos.presentation.components.TabletCentered
import com.talha.restaurantpos.presentation.theme.GlassTheme

@Composable
fun SplashScreen(onReady: () -> Unit) {
    LaunchedEffect(Unit) { onReady() }
    val colors = GlassTheme.colors
    GlassBackground {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, tint = colors.accent, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Talha POS", color = colors.textPrimary, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Restaurant Control Center", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(color = colors.accent, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit
) {
    val colors = GlassTheme.colors
    GlassBackground {
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            TabletCentered(modifier = Modifier.weight(1f)) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                    Text(
                        "Your Restaurant,\nSimplified.",
                        color = colors.textPrimary,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Create bills in seconds. Track your sales anywhere. Works even offline.",
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            TabletCentered {
                GlassButton(text = "GET STARTED", onClick = onRegister, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                GlassButton(text = "LOG IN", onClick = onLogin, style = GlassButtonStyle.SECONDARY, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
