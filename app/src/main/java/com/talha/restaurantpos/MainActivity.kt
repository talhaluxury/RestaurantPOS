package com.talha.restaurantpos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.talha.restaurantpos.presentation.navigation.PosNavGraph
import com.talha.restaurantpos.presentation.theme.GlassThemeMode
import com.talha.restaurantpos.presentation.theme.RestaurantPosTheme
import com.talha.restaurantpos.util.NetworkMonitor
import com.talha.restaurantpos.util.SessionManager
import com.talha.restaurantpos.util.SyncWorker
import com.talha.restaurantpos.util.ThemePref
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themePref by sessionManager.themeFlow.collectAsState(initial = ThemePref.SYSTEM)
            val mode = when (themePref) {
                ThemePref.LIGHT -> GlassThemeMode.LIGHT
                ThemePref.DARK -> GlassThemeMode.DARK
                ThemePref.SYSTEM -> GlassThemeMode.SYSTEM
            }

            // Android 13+ (API 33+) blocks all notifications — including new-order alerts —
            // unless this runtime permission is explicitly granted. Ask for it once, right
            // on launch, so the app can actually show the "new order" notification at all.
            val notifPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* no-op: if denied, notifications simply won't show; nothing else to do here */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            // Whenever connectivity returns, flush any bills that were created while offline
            // (spec §19: "Do NOT prevent billing because internet is unavailable").
            LaunchedEffect(Unit) {
                networkMonitor.observe().collect { online ->
                    if (online) {
                        val request = OneTimeWorkRequestBuilder<SyncWorker>()
                            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                            .build()
                        WorkManager.getInstance(applicationContext)
                            .enqueueUniqueWork("sync_pending_orders", ExistingWorkPolicy.REPLACE, request)
                    }
                }
            }

            RestaurantPosTheme(mode = mode) {
                PosNavGraph()
            }
        }
    }
}
