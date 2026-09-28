package com.talha.restaurantpos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.talha.restaurantpos.data.local.dao.StaffDao
import com.talha.restaurantpos.data.local.entity.StaffEntity
import com.talha.restaurantpos.data.remote.FirestorePaths
import com.talha.restaurantpos.domain.model.StaffMember
import com.talha.restaurantpos.domain.model.StaffRole
import com.talha.restaurantpos.domain.repository.StaffRepository
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
class StaffRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val staffDao: StaffDao,
    private val networkMonitor: NetworkMonitor
) : StaffRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listened = mutableSetOf<String>()

    private fun ensureListener(restaurantId: String) {
        if (!listened.add(restaurantId)) return
        firestore.collection(FirestorePaths.staff(restaurantId)).addSnapshotListener { snap, _ ->
            if (snap == null) return@addSnapshotListener
            syncScope.launch {
                val entities = snap.documents.map {
                    StaffEntity(
                        id = it.id, restaurantId = restaurantId,
                        name = it.getString("name") ?: "",
                        phone = it.getString("phone") ?: "",
                        role = it.getString("role") ?: StaffRole.CASHIER.name,
                        pin = it.getString("pin") ?: ""
                    )
                }
                staffDao.upsertAll(entities)
            }
        }
    }

    override fun observeStaff(restaurantId: String): Flow<List<StaffMember>> {
        ensureListener(restaurantId)
        return staffDao.observeAll(restaurantId).map { list ->
            list.map { StaffMember(it.id, it.name, it.phone, StaffRole.valueOf(it.role), it.pin) }
        }
    }

    override suspend fun upsertStaff(restaurantId: String, staff: StaffMember): Result<Unit> = runCatching {
        val id = staff.id.ifBlank { UUID.randomUUID().toString() }
        staffDao.upsert(StaffEntity(id, restaurantId, staff.name, staff.phone, staff.role.name, staff.pin))
        if (networkMonitor.isOnlineNow()) {
            firestore.collection(FirestorePaths.staff(restaurantId)).document(id).set(
                mapOf("name" to staff.name, "phone" to staff.phone, "role" to staff.role.name, "pin" to staff.pin)
            ).await()
        }
    }

    override suspend fun deleteStaff(restaurantId: String, staffId: String): Result<Unit> = runCatching {
        staffDao.delete(staffId)
        if (networkMonitor.isOnlineNow()) {
            firestore.collection(FirestorePaths.staff(restaurantId)).document(staffId).delete().await()
        }
    }
}
