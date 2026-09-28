package com.talha.restaurantpos.data.local.dao

import androidx.room.*
import com.talha.restaurantpos.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE restaurantId = :restaurantId ORDER BY sortOrder ASC")
    fun observeAll(restaurantId: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE restaurantId = :restaurantId ORDER BY name ASC")
    fun observeAll(restaurantId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE restaurantId = :restaurantId AND (name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%')")
    fun search(restaurantId: String, query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface TableDao {
    @Query("SELECT * FROM tables WHERE restaurantId = :restaurantId ORDER BY label ASC")
    fun observeAll(restaurantId: String): Flow<List<TableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(table: TableEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tables: List<TableEntity>)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory WHERE restaurantId = :restaurantId ORDER BY name ASC")
    fun observeAll(restaurantId: String): Flow<List<InventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: InventoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<InventoryEntity>)
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff WHERE restaurantId = :restaurantId ORDER BY name ASC")
    fun observeAll(restaurantId: String): Flow<List<StaffEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(staff: StaffEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(staff: List<StaffEntity>)

    @Query("DELETE FROM staff WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE restaurantId = :restaurantId ORDER BY createdAt DESC")
    fun observeAll(restaurantId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE restaurantId = :restaurantId AND status IN ('NEW','PREPARING','READY') ORDER BY createdAt ASC")
    fun observeActive(restaurantId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE restaurantId = :restaurantId AND syncStatus = 'PENDING'")
    suspend fun getPendingSync(restaurantId: String): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE restaurantId = :restaurantId AND createdAt BETWEEN :from AND :to")
    suspend fun getInRange(restaurantId: String, from: Long, to: Long): List<OrderEntity>

    @Query("SELECT COUNT(*) FROM orders WHERE restaurantId = :restaurantId")
    suspend fun countAll(restaurantId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(orders: List<OrderEntity>)

    @Query("UPDATE orders SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE orders SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}
