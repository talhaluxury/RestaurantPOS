package com.talha.restaurantpos.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.talha.restaurantpos.data.remote.FirestorePaths
import com.talha.restaurantpos.domain.model.Restaurant
import com.talha.restaurantpos.domain.repository.RestaurantRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestaurantRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) : RestaurantRepository {

    private fun toMap(r: Restaurant): Map<String, Any?> = mapOf(
        "id" to r.id,
        "name" to r.name,
        "ownerName" to r.ownerName,
        "phone" to r.phone,
        "address" to r.address,
        "currency" to r.currency,
        "logoUrl" to r.logoUrl,
        "receiptFooter" to r.receiptFooter,
        "taxPercent" to r.taxPercent,
        "serviceChargePercent" to r.serviceChargePercent
    )

    private fun fromSnapshot(id: String, data: Map<String, Any?>?): Restaurant {
        if (data == null) return Restaurant(id = id)
        return Restaurant(
            id = id,
            name = data["name"] as? String ?: "",
            ownerName = data["ownerName"] as? String ?: "",
            phone = data["phone"] as? String ?: "",
            address = data["address"] as? String ?: "",
            currency = data["currency"] as? String ?: "PKR",
            logoUrl = data["logoUrl"] as? String ?: "",
            receiptFooter = data["receiptFooter"] as? String ?: "Thank You! Visit Again",
            taxPercent = (data["taxPercent"] as? Number)?.toDouble() ?: 0.0,
            serviceChargePercent = (data["serviceChargePercent"] as? Number)?.toDouble() ?: 0.0
        )
    }

    override suspend fun createRestaurant(restaurant: Restaurant): Result<String> = runCatching {
        val uid = firebaseAuth.currentUser?.uid ?: error("Not authenticated")
        val docRef = firestore.collection(FirestorePaths.RESTAURANTS).document()
        val toSave = restaurant.copy(id = docRef.id)
        docRef.set(toMap(toSave)).await()

        // Register owner as staff + link user -> restaurant for security-rule lookups
        firestore.collection(FirestorePaths.staff(docRef.id)).document(uid)
            .set(mapOf("name" to restaurant.ownerName, "role" to "OWNER", "phone" to restaurant.phone))
            .await()
        firestore.collection(FirestorePaths.USER_RESTAURANTS).document(uid)
            .set(mapOf("restaurantId" to docRef.id))
            .await()

        docRef.id
    }

    override suspend fun getRestaurant(restaurantId: String): Result<Restaurant> = runCatching {
        val snap = firestore.document(FirestorePaths.restaurant(restaurantId)).get().await()
        fromSnapshot(restaurantId, snap.data)
    }

    override fun observeRestaurant(restaurantId: String): Flow<Restaurant?> = callbackFlow {
        val reg = firestore.document(FirestorePaths.restaurant(restaurantId))
            .addSnapshotListener { snap, _ ->
                trySend(if (snap != null && snap.exists()) fromSnapshot(restaurantId, snap.data) else null)
            }
        awaitClose { reg.remove() }
    }

    override suspend fun updateRestaurant(restaurant: Restaurant): Result<Unit> = runCatching {
        firestore.document(FirestorePaths.restaurant(restaurant.id))
            .set(toMap(restaurant)).await()
    }

    override suspend fun getMyRestaurantId(): String? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        return runCatching {
            val snap = firestore.collection(FirestorePaths.USER_RESTAURANTS).document(uid).get().await()
            snap.getString("restaurantId")
        }.getOrNull()
    }
}
