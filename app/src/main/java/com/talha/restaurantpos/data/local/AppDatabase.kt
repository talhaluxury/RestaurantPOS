package com.talha.restaurantpos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.talha.restaurantpos.data.local.dao.*
import com.talha.restaurantpos.data.local.entity.*

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        TableEntity::class,
        InventoryEntity::class,
        StaffEntity::class,
        OrderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun tableDao(): TableDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun staffDao(): StaffDao
    abstract fun orderDao(): OrderDao

    companion object {
        const val DB_NAME = "restaurant_pos.db"
    }
}
