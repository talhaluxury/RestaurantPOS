package com.talha.restaurantpos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Note: variants/add-ons/order-items are stored as pre-serialized JSON String columns
// (see data.local.Converters) and mapped manually in the repository layer — no Room
// @TypeConverter registration needed since the entity fields are already plain Strings.

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val restaurantId: String,
    val name: String,
    val sortOrder: Int
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val restaurantId: String,
    val name: String,
    val description: String,
    val price: Double,
    val costPrice: Double,
    val imageUrl: String,
    val categoryId: String,
    val sku: String,
    val available: Boolean,
    val featured: Boolean,
    val taxPercent: Double,
    val variantsJson: String,
    val addOnsJson: String
)

@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey val id: String,
    val restaurantId: String,
    val label: String,
    val status: String
)

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey val id: String,
    val restaurantId: String,
    val name: String,
    val currentStock: Double,
    val lowStockThreshold: Double,
    val unit: String
)

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey val id: String,
    val restaurantId: String,
    val name: String,
    val phone: String,
    val role: String,
    val pin: String
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val restaurantId: String,
    val orderNumber: String,
    val itemsJson: String,
    val orderType: String,
    val tableId: String?,
    val tableLabel: String?,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val subtotal: Double,
    val discount: Double,
    val tax: Double,
    val serviceCharge: Double,
    val total: Double,
    val paymentMethod: String?,
    val amountReceived: Double,
    val changeDue: Double,
    val status: String,
    val cashierName: String,
    val createdAt: Long,
    val syncStatus: String
)
