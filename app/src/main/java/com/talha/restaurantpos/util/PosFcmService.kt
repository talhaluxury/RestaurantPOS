package com.talha.restaurantpos.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.talha.restaurantpos.MainActivity

/**
 * Receives push notifications sent by the "sendNewOrderPush" Cloud Function whenever a new
 * order is created (e.g. from the website). Unlike the in-app Firestore listener in
 * OrderRepositoryImpl (which only fires while the app process is alive), this works even
 * when the app has been killed, as long as the device has internet and Google Play services.
 *
 * The app subscribes to the topic "restaurant_<restaurantId>_orders" — see SessionManager
 * and AppSessionViewModel — so this service only ever receives orders for its own restaurant.
 */
class PosFcmService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "new_orders"
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title
            ?: message.data["title"]
            ?: "New order received"
        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Tap to view details"
        val orderId = message.data["orderId"]
        showNotification(title, body, orderId)
    }

    // A new FCM token doesn't need to be sent anywhere here — we notify by topic
    // (restaurant_<id>_orders), not by individual device token, so nothing to sync.
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    private fun showNotification(title: String, body: String, orderId: String?) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "New Orders", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a new order comes in (including from the website)"
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
            }
            nm.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            orderId?.let { putExtra("orderId", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, orderId?.hashCode() ?: 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (canPost) {
            runCatching { nm.notify(orderId?.hashCode() ?: System.currentTimeMillis().toInt(), notification) }
        }
    }
}
