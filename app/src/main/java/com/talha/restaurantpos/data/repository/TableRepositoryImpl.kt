package com.talha.restaurantpos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.talha.restaurantpos.data.local.dao.TableDao
import com.talha.restaurantpos.data.local.entity.TableEntity
import com.talha.restaurantpos.data.remote.FirestorePaths
import com.talha.restaurantpos.domain.model.RestaurantTable
import com.talha.restaurantpos.domain.model.TableStatus
import com.talha.restaurantpos.domain.repository.TableRepository
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
class TableRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val tableDao: TableDao,
    private val networkMonitor: NetworkMonitor
) : TableRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listened = mutableSetOf<String>()

    private fun ensureListener(restaurantId: String) {
        if (!listened.add(restaurantId)) return
        firestore.collection(FirestorePaths.tables(restaurantId)).addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            syncScope.launch {
                val entities = snap.documents.map {
                    TableEntity(
                        id = it.id, restaurantId = restaurantId,
                        label = it.getString("label") ?: "",
                        status = it.getString("status") ?: TableStatus.AVAILABLE.name
                    )
                }
                tableDao.upsertAll(entities)
            }
        }
    }

    override fun observeTables(restaurantId: String): Flow<List<RestaurantTable>> {
        ensureListener(restaurantId)
        return tableDao.observeAll(restaurantId).map { list ->
            list.map { RestaurantTable(it.id, it.label, TableStatus.valueOf(it.status)) }
        }
    }

    override suspend fun upsertTable(restaurantId: String, table: RestaurantTable): Result<Unit> = runCatching {
        val id = table.id.ifBlank { UUID.randomUUID().toString() }
        tableDao.upsert(TableEntity(id, restaurantId, table.label, table.status.name))
        if (networkMonitor.isOnlineNow()) {
            firestore.collection(FirestorePaths.tables(restaurantId)).document(id)
                .set(mapOf("label" to table.label, "status" to table.status.name)).await()
        }
    }

    override suspend fun setTableStatus(restaurantId: String, tableId: String, status: TableStatus): Result<Unit> = runCatching {
        if (networkMonitor.isOnlineNow()) {
            firestore.collection(FirestorePaths.tables(restaurantId)).document(tableId)
                .update("status", status.name).await()
        }
    }
}
