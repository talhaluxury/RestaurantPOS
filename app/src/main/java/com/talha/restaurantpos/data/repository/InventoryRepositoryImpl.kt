package com.talha.restaurantpos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.talha.restaurantpos.data.local.dao.InventoryDao
import com.talha.restaurantpos.data.local.entity.InventoryEntity
import com.talha.restaurantpos.data.remote.FirestorePaths
import com.talha.restaurantpos.domain.model.InventoryItem
import com.talha.restaurantpos.domain.repository.InventoryRepository
import com.talha.restaurantpos.util.NetworkMonitor
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
class InventoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val inventoryDao: InventoryDao,
    private val networkMonitor: NetworkMonitor
) : InventoryRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listened = mutableSetOf<String>()

    private fun ensureListener(restaurantId: String) {
        if (!listened.add(restaurantId)) return
        firestore.collection(FirestorePaths.inventory(restaurantId)).addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            syncScope.launch {
                val entities = snap.documents.map {
                    InventoryEntity(
                        id = it.id, restaurantId = restaurantId,
                        name = it.getString("name") ?: "",
                        currentStock = it.getDouble("currentStock") ?: 0.0,
                        lowStockThreshold = it.getDouble("lowStockThreshold") ?: 0.0,
                        unit = it.getString("unit") ?: ""
                    )
                }
                inventoryDao.upsertAll(entities)
            }
        }
    }

    override fun observeInventory(restaurantId: String): Flow<List<InventoryItem>> {
        ensureListener(restaurantId)
        return inventoryDao.observeAll(restaurantId).map { list ->
            list.map { InventoryItem(it.id, it.name, it.currentStock, it.lowStockThreshold, it.unit) }
        }
    }

    override suspend fun upsertItem(restaurantId: String, item: InventoryItem): Result<Unit> = runCatching {
        val id = item.id.ifBlank { UUID.randomUUID().toString() }
        inventoryDao.upsert(InventoryEntity(id, restaurantId, item.name, item.currentStock, item.lowStockThreshold, item.unit))
        if (networkMonitor.isOnlineNow()) {
            firestore.collection(FirestorePaths.inventory(restaurantId)).document(id).set(
                mapOf("name" to item.name, "currentStock" to item.currentStock, "lowStockThreshold" to item.lowStockThreshold, "unit" to item.unit)
            ).await()
        }
    }
}
