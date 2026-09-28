package com.talha.restaurantpos.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.talha.restaurantpos.domain.repository.OrderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * A free (no Cloud Functions / no Blaze plan) alternative to server-side push.
 *
 * Runs as a foreground service, which Android will not kill just for running in the
 * background — this keeps the app *process* alive, which in turn keeps the Firestore
 * order listener in OrderRepositoryImpl connected, so notifyNewOrder() keeps firing for
 * new orders (from the app or the website) as long as this service is running, even if
 * the user has swiped the app away from Recents.
 *
 * It is still an Android process, so it is not 100% guaranteed by the OS — aggressive
 * battery optimization on some phones (Xiaomi/Oppo/Vivo/etc.) can still kill it. For best
 * reliability, ask staff to disable battery optimization for this app in phone Settings.
 * It stops only if the user force-stops the app from Android Settings, or reboots the
 * phone without reopening the app.
 */
@AndroidEntryPoint
class OrderListenerService : Service() {

    @Inject lateinit var orderRepository: OrderRepository
    @Inject lateinit var sessionManager: SessionManager

    private var job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        const val CHANNEL_ID = "order_listener_status"
        const val NOTIFICATION_ID = 9001
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildStatusNotification())

        scope.launch {
            val restaurantId = sessionManager.currentRestaurantId() ?: return@launch
            // Collecting keeps this coroutine (and the underlying Firestore listener it
            // starts as a side effect) alive for as long as this service runs.
            orderRepository.observeActiveOrders(restaurantId).collect { }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: if Android kills this service under memory pressure, it will try
        // to recreate it shortly after (with a null intent), which re-attaches the listener.
        return START_STICKY
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, "Order Listener Status", NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Keeps the app listening for new orders in the background"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildStatusNotification() =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("RestaurantPOS")
            .setContentText("Listening for new orders…")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
}
