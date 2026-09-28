package com.talha.restaurantpos.domain.model

enum class OrderType { DINE_IN, TAKEAWAY, DELIVERY }

enum class OrderStatus { NEW, PREPARING, READY, COMPLETED, CANCELLED }

enum class PaymentMethod { CASH, CARD, EASYPAISA, JAZZCASH, BANK, OTHER }

enum class StaffRole { OWNER, MANAGER, CASHIER, KITCHEN }

enum class TableStatus { AVAILABLE, OCCUPIED, RESERVED }

enum class SyncStatus { PENDING, SYNCED, FAILED }

data class Restaurant(
    val id: String = "",
    val name: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val currency: String = "PKR",
    val logoUrl: String = "",
    val receiptFooter: String = "Thank You! Visit Again",
    val taxPercent: Double = 0.0,
    val serviceChargePercent: Double = 0.0
)

data class Category(
    val id: String = "",
    val name: String = "",
    val sortOrder: Int = 0
)

data class ProductVariant(
    val name: String = "",
    val priceDelta: Double = 0.0
)

data class ProductAddOn(
    val name: String = "",
    val price: Double = 0.0
)

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val costPrice: Double = 0.0,
    val imageUrl: String = "",
    val categoryId: String = "",
    val sku: String = "",
    val available: Boolean = true,
    val featured: Boolean = false,
    val taxPercent: Double = 0.0,
    val variants: List<ProductVariant> = emptyList(),
    val addOns: List<ProductAddOn> = emptyList()
)

data class SelectedAddOn(val name: String, val price: Double)

data class CartLine(
    val lineId: String,
    val product: Product,
    val variant: ProductVariant? = null,
    val addOns: List<SelectedAddOn> = emptyList(),
    val quantity: Int = 1,
    val notes: String = ""
) {
    val unitPrice: Double get() = product.price + (variant?.priceDelta ?: 0.0) + addOns.sumOf { it.price }
    val lineTotal: Double get() = unitPrice * quantity
}

data class OrderItemRecord(
    val productId: String,
    val name: String,
    val variantName: String? = null,
    val addOnNames: List<String> = emptyList(),
    val unitPrice: Double,
    val quantity: Int,
    val lineTotal: Double
)

data class RestaurantTable(
    val id: String = "",
    val label: String = "",
    val status: TableStatus = TableStatus.AVAILABLE
)

data class CustomerInfo(
    val name: String = "",
    val phone: String = "",
    val address: String = ""
)

data class Order(
    val id: String = "",
    val orderNumber: String = "",
    val items: List<OrderItemRecord> = emptyList(),
    val orderType: OrderType = OrderType.DINE_IN,
    val tableId: String? = null,
    val tableLabel: String? = null,
    val customer: CustomerInfo = CustomerInfo(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val serviceCharge: Double = 0.0,
    val total: Double = 0.0,
    val paymentMethod: PaymentMethod? = null,
    val amountReceived: Double = 0.0,
    val changeDue: Double = 0.0,
    val status: OrderStatus = OrderStatus.NEW,
    val cashierName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING
)

data class InventoryItem(
    val id: String = "",
    val name: String = "",
    val currentStock: Double = 0.0,
    val lowStockThreshold: Double = 0.0,
    val unit: String = ""
) {
    val isLowStock: Boolean get() = currentStock <= lowStockThreshold
}

data class StaffMember(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val role: StaffRole = StaffRole.CASHIER,
    val pin: String = ""
)

data class SalesSummary(
    val totalRevenue: Double = 0.0,
    val orderCount: Int = 0,
    val averageOrderValue: Double = 0.0,
    val itemsSold: Int = 0,
    val totalDiscount: Double = 0.0,
    val totalTax: Double = 0.0,
    val paymentBreakdown: Map<PaymentMethod, Double> = emptyMap()
)
