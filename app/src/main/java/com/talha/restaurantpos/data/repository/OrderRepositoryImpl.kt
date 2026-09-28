package com.talha.restaurantpos.data.repository

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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.talha.restaurantpos.MainActivity
import com.talha.restaurantpos.data.local.Converters
import com.talha.restaurantpos.data.local.dao.OrderDao
import com.talha.restaurantpos.data.local.entity.OrderEntity
import com.talha.restaurantpos.data.remote.FirestorePaths
import com.talha.restaurantpos.domain.model.*
import com.talha.restaurantpos.domain.repository.OrderRepository
import com.talha.restaurantpos.util.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val orderDao: OrderDao,
    private val networkMonitor: NetworkMonitor,
    @ApplicationContext private val context: Context
) : OrderRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listened = mutableSetOf<String>()
    private val primedListeners = mutableSetOf<String>()

    companion object {
        private const val NEW_ORDER_CHANNEL_ID = "new_orders"
    }

    // ---------- mapping ----------

    private fun Order.toEntity(restaurantId: String): OrderEntity = OrderEntity(
        id = id, restaurantId = restaurantId, orderNumber = orderNumber,
        itemsJson = Converters.orderItemsToJson(items),
        orderType = orderType.name, tableId = tableId, tableLabel = tableLabel,
        customerName = customer.name, customerPhone = customer.phone, customerAddress = customer.address,
        subtotal = subtotal, discount = discount, tax = tax, serviceCharge = serviceCharge, total = total,
        paymentMethod = paymentMethod?.name, amountReceived = amountReceived, changeDue = changeDue,
        status = status.name, cashierName = cashierName, createdAt = createdAt, syncStatus = syncStatus.name
    )

    private fun OrderEntity.toDomain(): Order = Order(
        id = id, orderNumber = orderNumber, items = Converters.orderItemsFromJson(itemsJson),
        orderType = OrderType.valueOf(orderType), tableId = tableId, tableLabel = tableLabel,
        customer = CustomerInfo(customerName, customerPhone, customerAddress),
        subtotal = subtotal, discount = discount, tax = tax, serviceCharge = serviceCharge, total = total,
        paymentMethod = paymentMethod?.let { PaymentMethod.valueOf(it) }, amountReceived = amountReceived,
        changeDue = changeDue, status = OrderStatus.valueOf(status), cashierName = cashierName,
        createdAt = createdAt, syncStatus = SyncStatus.valueOf(syncStatus)
    )

    private fun orderToFirestoreMap(order: Order): Map<String, Any?> = mapOf(
        "orderNumber" to order.orderNumber,
        "items" to order.items.map {
            mapOf(
                "productId" to it.productId, "name" to it.name, "variantName" to it.variantName,
                "addOnNames" to it.addOnNames, "unitPrice" to it.unitPrice,
                "quantity" to it.quantity, "lineTotal" to it.lineTotal
            )
        },
        "orderType" to order.orderType.name,
        "tableId" to order.tableId, "tableLabel" to order.tableLabel,
        "customerName" to order.customer.name, "customerPhone" to order.customer.phone, "customerAddress" to order.customer.address,
        "subtotal" to order.subtotal, "discount" to order.discount, "tax" to order.tax,
        "serviceCharge" to order.serviceCharge, "total" to order.total,
        "paymentMethod" to order.paymentMethod?.name, "amountReceived" to order.amountReceived, "changeDue" to order.changeDue,
        "status" to order.status.name, "cashierName" to order.cashierName, "createdAt" to order.createdAt
    )

    // ---------- listener (remote -> local mirror, for cross-device visibility) ----------

    private fun ensureListener(restaurantId: String) {
        if (!listened.add(restaurantId)) return
        firestore.collection(FirestorePaths.orders(restaurantId))
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val wasPrimed = primedListeners.contains(restaurantId)
                primedListeners.add(restaurantId)
                syncScope.launch {
                    snap.documentChanges.forEach { change ->
                        val d = change.document
                        val existing = orderDao.getPendingSync(restaurantId).find { it.id == d.id }
                        // Don't clobber a locally-pending unsynced edit with a stale remote read.
                        if (existing == null) {
                            val order = firestoreDocToOrder(d.id, d.data)
                            orderDao.upsert(order.toEntity(restaurantId).copy(syncStatus = SyncStatus.SYNCED.name))
                        }
                        // Only alert for orders that truly just arrived (e.g. from the customer
                        // website) — the first snapshot on app start delivers every existing order
                        // as an "ADDED" change too, so that initial batch must never notify.
                        if (wasPrimed && change.type == DocumentChange.Type.ADDED) {
                            val status = d.getString("status")
                            if (status == OrderStatus.NEW.name) {
                                notifyNewOrder(
                                    orderId = d.id,
                                    orderNumber = d.getString("orderNumber") ?: "",
                                    customerName = d.getString("customerName") ?: ""
                                )
                            }
                        }
                    }
                }
            }
    }

    private fun notifyNewOrder(orderId: String, orderNumber: String, customerName: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NEW_ORDER_CHANNEL_ID, "New Orders", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a new order comes in (including from the website)"
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
            }
            nm.createNotificationChannel(channel)
        }
        val title = if (orderNumber.isNotBlank()) "New order $orderNumber" else "New order received"
        val text = if (customerName.isNotBlank()) "From $customerName — tap to view" else "Tap to view details"
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("orderId", orderId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, orderId.hashCode(), openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, NEW_ORDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (canPost) {
            runCatching { nm.notify(orderId.hashCode(), notification) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun firestoreDocToOrder(id: String, data: Map<String, Any?>): Order {
        val itemsRaw = data["items"] as? List<Map<String, Any?>> ?: emptyList()
        val items = itemsRaw.map {
            OrderItemRecord(
                productId = it["productId"] as? String ?: "",
                name = it["name"] as? String ?: "",
                variantName = it["variantName"] as? String,
                addOnNames = (it["addOnNames"] as? List<String>) ?: emptyList(),
                unitPrice = (it["unitPrice"] as? Number)?.toDouble() ?: 0.0,
                quantity = (it["quantity"] as? Number)?.toInt() ?: 1,
                lineTotal = (it["lineTotal"] as? Number)?.toDouble() ?: 0.0
            )
        }
        return Order(
            id = id, orderNumber = data["orderNumber"] as? String ?: "",
            items = items, orderType = OrderType.valueOf(data["orderType"] as? String ?: "DINE_IN"),
            tableId = data["tableId"] as? String, tableLabel = data["tableLabel"] as? String,
            customer = CustomerInfo(
                data["customerName"] as? String ?: "", data["customerPhone"] as? String ?: "", data["customerAddress"] as? String ?: ""
            ),
            subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
            discount = (data["discount"] as? Number)?.toDouble() ?: 0.0,
            tax = (data["tax"] as? Number)?.toDouble() ?: 0.0,
            serviceCharge = (data["serviceCharge"] as? Number)?.toDouble() ?: 0.0,
            total = (data["total"] as? Number)?.toDouble() ?: 0.0,
            paymentMethod = (data["paymentMethod"] as? String)?.let { PaymentMethod.valueOf(it) },
            amountReceived = (data["amountReceived"] as? Number)?.toDouble() ?: 0.0,
            changeDue = (data["changeDue"] as? Number)?.toDouble() ?: 0.0,
            status = OrderStatus.valueOf(data["status"] as? String ?: "NEW"),
            cashierName = data["cashierName"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            syncStatus = SyncStatus.SYNCED
        )
    }

    // ---------- public API ----------

    override fun observeOrders(restaurantId: String): Flow<List<Order>> {
        ensureListener(restaurantId)
        return orderDao.observeAll(restaurantId).map { list -> list.map { it.toDomain() } }
    }

    override fun observeActiveOrders(restaurantId: String): Flow<List<Order>> {
        ensureListener(restaurantId)
        return orderDao.observeActive(restaurantId).map { list -> list.map { it.toDomain() } }
    }

    /**
     * Reliable, gap-free order numbering:
     * - Online: atomically increments restaurants/{id}/settings/config.orderCounter in a Firestore
     *   transaction and returns "#000001"-style number. Safe across multiple concurrent devices.
     * - Offline: returns a temporary "OFFLINE-<uuid8>" placeholder. The real sequential number is
     *   assigned transactionally the moment the order syncs (see syncPendingOrders), so two devices
     *   billing offline at once can never collide on the same final order number.
     */
    override suspend fun nextOrderNumber(restaurantId: String): String {
        if (!networkMonitor.isOnlineNow()) {
            return "OFFLINE-${UUID.randomUUID().toString().take(8).uppercase()}"
        }
        return allocateRealOrderNumber(restaurantId)
    }

    private suspend fun allocateRealOrderNumber(restaurantId: String): String {
        val counterRef = firestore.document(FirestorePaths.settings(restaurantId))
        val next = firestore.runTransaction { txn ->
            val snap = txn.get(counterRef)
            val current = snap.getLong("orderCounter") ?: 0L
            val newValue = current + 1
            txn.set(counterRef, mapOf("orderCounter" to newValue), com.google.firebase.firestore.SetOptions.merge())
            newValue
        }.await()
        return "#" + next.toString().padStart(6, '0')
    }

    override suspend fun placeOrder(restaurantId: String, order: Order): Result<Order> = runCatching {
        val id = order.id.ifBlank { UUID.randomUUID().toString() }
        var finalOrder = order.copy(id = id)

        // Always persist locally first so billing never blocks on network.
        if (finalOrder.orderNumber.isBlank()) {
            finalOrder = finalOrder.copy(orderNumber = nextOrderNumber(restaurantId))
        }
        val isOffline = finalOrder.orderNumber.startsWith("OFFLINE-")
        finalOrder = finalOrder.copy(syncStatus = if (isOffline) SyncStatus.PENDING else SyncStatus.PENDING)
        orderDao.upsert(finalOrder.toEntity(restaurantId))

        // Best-effort immediate push if we have connectivity and a real order number.
        if (!isOffline && networkMonitor.isOnlineNow()) {
            runCatching {
                firestore.collection(FirestorePaths.orders(restaurantId)).document(id)
                    .set(orderToFirestoreMap(finalOrder)).await()
                orderDao.updateSyncStatus(id, SyncStatus.SYNCED.name)
                finalOrder = finalOrder.copy(syncStatus = SyncStatus.SYNCED)
            }
        }
        finalOrder
    }

    override suspend fun updateOrderStatus(restaurantId: String, orderId: String, status: OrderStatus): Result<Unit> = runCatching {
        orderDao.updateStatus(orderId, status.name)
        if (networkMonitor.isOnlineNow()) {
            runCatching {
                firestore.collection(FirestorePaths.orders(restaurantId)).document(orderId)
                    .update("status", status.name).await()
            }
        }
    }

    /**
     * Called by the WorkManager sync worker (and opportunistically when connectivity returns).
     * For every locally-pending order: if it still carries a placeholder OFFLINE- number, allocate
     * its real transactional number now, then push the full order document, then mark SYNCED.
     */
    override suspend fun syncPendingOrders(restaurantId: String): Result<Int> = runCatching {
        if (!networkMonitor.isOnlineNow()) return@runCatching 0
        val pending = orderDao.getPendingSync(restaurantId)
        var syncedCount = 0
        for (entity in pending) {
            runCatching {
                var order = entity.toDomain()
                if (order.orderNumber.startsWith("OFFLINE-")) {
                    order = order.copy(orderNumber = allocateRealOrderNumber(restaurantId))
                }
                firestore.collection(FirestorePaths.orders(restaurantId)).document(order.id)
                    .set(orderToFirestoreMap(order)).await()
                orderDao.upsert(order.toEntity(restaurantId).copy(syncStatus = SyncStatus.SYNCED.name))
                syncedCount++
            }
        }
        syncedCount
    }

    override suspend fun updateLiveLocation(restaurantId: String, orderId: String, lat: Double, lng: Double): Result<Unit> = runCatching {
        if (!networkMonitor.isOnlineNow()) return@runCatching
        firestore.collection(FirestorePaths.orders(restaurantId)).document(orderId)
            .update(
                "liveLocation", mapOf(
                    "lat" to lat, "lng" to lng, "updatedAt" to System.currentTimeMillis()
                )
            ).await()
    }

    override suspend fun getSalesSummary(restaurantId: String, fromEpochMs: Long, toEpochMs: Long): SalesSummary {
        val orders = orderDao.getInRange(restaurantId, fromEpochMs, toEpochMs)
            .map { it.toDomain() }
            .filter { it.status != OrderStatus.CANCELLED }

        if (orders.isEmpty()) return SalesSummary()

        val totalRevenue = orders.sumOf { it.total }
        val itemsSold = orders.sumOf { o -> o.items.sumOf { it.quantity } }
        val totalDiscount = orders.sumOf { it.discount }
        val totalTax = orders.sumOf { it.tax }
        val paymentBreakdown = orders
            .filter { it.paymentMethod != null }
            .groupBy { it.paymentMethod!! }
            .mapValues { entry -> entry.value.sumOf { it.total } }

        return SalesSummary(
            totalRevenue = totalRevenue,
            orderCount = orders.size,
            averageOrderValue = if (orders.isNotEmpty()) totalRevenue / orders.size else 0.0,
            itemsSold = itemsSold,
            totalDiscount = totalDiscount,
            totalTax = totalTax,
            paymentBreakdown = paymentBreakdown
        )
    }
}
