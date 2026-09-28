package com.talha.restaurantpos.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "pos_session")

enum class ThemePref { LIGHT, DARK, SYSTEM }

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val RESTAURANT_ID = stringPreferencesKey("restaurant_id")
        val CASHIER_NAME = stringPreferencesKey("cashier_name")
        val STAFF_ROLE = stringPreferencesKey("staff_role")
        val THEME = stringPreferencesKey("theme_pref")
        val PRINTER_ADDRESS = stringPreferencesKey("printer_address")
        val PAPER_WIDTH = stringPreferencesKey("paper_width")
    }

    val restaurantIdFlow: Flow<String?> =
        context.dataStore.data.map { it[Keys.RESTAURANT_ID] }

    val cashierNameFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.CASHIER_NAME] ?: "Cashier" }

    val staffRoleFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.STAFF_ROLE] ?: "OWNER" }

    val themeFlow: Flow<ThemePref> =
        context.dataStore.data.map {
            ThemePref.valueOf(it[Keys.THEME] ?: ThemePref.SYSTEM.name)
        }

    // Remembers the last-connected thermal printer so the app can silently reconnect to it
    // on every launch, instead of the user having to open Settings and tap Connect every time.
    val printerAddressFlow: Flow<String?> =
        context.dataStore.data.map { it[Keys.PRINTER_ADDRESS] }

    val paperWidthFlow: Flow<String> =
        context.dataStore.data.map { it[Keys.PAPER_WIDTH] ?: "MM_80" }

    suspend fun setPrinterAddress(address: String) {
        context.dataStore.edit { it[Keys.PRINTER_ADDRESS] = address }
    }

    suspend fun setPaperWidth(width: String) {
        context.dataStore.edit { it[Keys.PAPER_WIDTH] = width }
    }

    suspend fun currentRestaurantId(): String? = restaurantIdFlow.first()

    suspend fun setRestaurantId(id: String) {
        context.dataStore.edit { it[Keys.RESTAURANT_ID] = id }
        subscribeToOrderNotifications(id)
    }

    // Subscribes this device to push notifications for this restaurant's new orders.
    // Topic-based, so no per-device token needs to be stored anywhere — the Cloud
    // Function just publishes to "restaurant_<id>_orders" and every staff device
    // subscribed to it (every device that has ever logged into this restaurant) gets it.
    // Safe to call repeatedly (e.g. on every app start) — subscribing twice is a no-op.
    fun subscribeToOrderNotifications(restaurantId: String) {
        if (restaurantId.isBlank()) return
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic("restaurant_${restaurantId}_orders") }
    }

    suspend fun setCashierSession(name: String, role: String) {
        context.dataStore.edit {
            it[Keys.CASHIER_NAME] = name
            it[Keys.STAFF_ROLE] = role
        }
    }

    suspend fun setTheme(pref: ThemePref) {
        context.dataStore.edit { it[Keys.THEME] = pref.name }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
