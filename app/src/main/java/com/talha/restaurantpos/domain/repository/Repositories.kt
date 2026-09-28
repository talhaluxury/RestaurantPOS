package com.talha.restaurantpos.domain.repository

import com.talha.restaurantpos.domain.model.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserId: String?
    suspend fun loginWithEmail(email: String, password: String): Result<Unit>
    suspend fun registerWithEmail(email: String, password: String): Result<Unit>
    suspend fun loginWithGoogleIdToken(idToken: String): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun logout()
    fun isLoggedIn(): Boolean
}

interface RestaurantRepository {
    suspend fun createRestaurant(restaurant: Restaurant): Result<String>
    suspend fun getRestaurant(restaurantId: String): Result<Restaurant>
    fun observeRestaurant(restaurantId: String): Flow<Restaurant?>
    suspend fun updateRestaurant(restaurant: Restaurant): Result<Unit>
    suspend fun getMyRestaurantId(): String?
}

interface ProductRepository {
    fun observeCategories(restaurantId: String): Flow<List<Category>>
    fun observeProducts(restaurantId: String): Flow<List<Product>>
    suspend fun upsertCategory(restaurantId: String, category: Category): Result<Unit>
    suspend fun deleteCategory(restaurantId: String, categoryId: String): Result<Unit>
    suspend fun upsertProduct(restaurantId: String, product: Product): Result<Unit>
    suspend fun deleteProduct(restaurantId: String, productId: String): Result<Unit>
    suspend fun uploadProductImage(restaurantId: String, productId: String, localUri: String): Result<String>
}

interface TableRepository {
    fun observeTables(restaurantId: String): Flow<List<RestaurantTable>>
    suspend fun upsertTable(restaurantId: String, table: RestaurantTable): Result<Unit>
    suspend fun setTableStatus(restaurantId: String, tableId: String, status: TableStatus): Result<Unit>
}

interface OrderRepository {
    fun observeOrders(restaurantId: String): Flow<List<Order>>
    fun observeActiveOrders(restaurantId: String): Flow<List<Order>>
    suspend fun placeOrder(restaurantId: String, order: Order): Result<Order>
    suspend fun updateOrderStatus(restaurantId: String, orderId: String, status: OrderStatus): Result<Unit>
    suspend fun nextOrderNumber(restaurantId: String): String
    suspend fun syncPendingOrders(restaurantId: String): Result<Int>
    suspend fun getSalesSummary(restaurantId: String, fromEpochMs: Long, toEpochMs: Long): SalesSummary
    /** Writes the delivery rider's current position onto the order doc so the customer-facing
     *  website can show it moving live on a map. No-op locally — this is online-only, best-effort. */
    suspend fun updateLiveLocation(restaurantId: String, orderId: String, lat: Double, lng: Double): Result<Unit>
}

interface InventoryRepository {
    fun observeInventory(restaurantId: String): Flow<List<InventoryItem>>
    suspend fun upsertItem(restaurantId: String, item: InventoryItem): Result<Unit>
}

interface StaffRepository {
    fun observeStaff(restaurantId: String): Flow<List<StaffMember>>
    suspend fun upsertStaff(restaurantId: String, staff: StaffMember): Result<Unit>
    suspend fun deleteStaff(restaurantId: String, staffId: String): Result<Unit>
}
