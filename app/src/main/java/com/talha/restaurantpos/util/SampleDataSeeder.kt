package com.talha.restaurantpos.util

import com.talha.restaurantpos.domain.model.Category
import com.talha.restaurantpos.domain.model.Product
import com.talha.restaurantpos.domain.model.ProductAddOn
import com.talha.restaurantpos.domain.model.ProductVariant
import com.talha.restaurantpos.domain.model.RestaurantTable
import com.talha.restaurantpos.domain.repository.ProductRepository
import com.talha.restaurantpos.domain.repository.TableRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds a brand-new restaurant with a sample menu (spec §34) so a fresh install can be tested
 * immediately without manual data entry. Only ever called from debug builds
 * (BuildConfig.SEED_SAMPLE_DATA) right after RestaurantSetupScreen creates a restaurant.
 */
@Singleton
class SampleDataSeeder @Inject constructor(
    private val productRepository: ProductRepository,
    private val tableRepository: TableRepository
) {
    suspend fun seed(restaurantId: String) {
        // Stable, predictable ids (rather than the usual random UUID) so the products below can
        // reference the right category immediately without waiting on a round trip.
        fun cat(name: String, order: Int) = Category(id = name.lowercase(), name = name, sortOrder = order)
        val burgers = cat("Burgers", 0)
        val pizza = cat("Pizza", 1)
        val fries = cat("Fries", 2)
        val drinks = cat("Drinks", 3)
        val deals = cat("Deals", 4)
        listOf(burgers, pizza, fries, drinks, deals).forEach { productRepository.upsertCategory(restaurantId, it) }

        val sizeVariants = listOf(ProductVariant("Regular", 0.0), ProductVariant("Large", 100.0))
        val burgerAddOns = listOf(ProductAddOn("Cheese", 50.0), ProductAddOn("Extra Sauce", 30.0), ProductAddOn("Jalapeno", 20.0))

        val products = listOf(
            Product(name = "Zinger Burger", price = 550.0, costPrice = 300.0, categoryId = "burgers", sku = "BRG-001", addOns = burgerAddOns),
            Product(name = "Chicken Burger", price = 450.0, costPrice = 250.0, categoryId = "burgers", sku = "BRG-002", addOns = burgerAddOns),
            Product(name = "Beef Burger", price = 600.0, costPrice = 340.0, categoryId = "burgers", sku = "BRG-003", addOns = burgerAddOns),
            Product(name = "Cheese Burger", price = 500.0, costPrice = 280.0, categoryId = "burgers", sku = "BRG-004", addOns = burgerAddOns),
            Product(name = "Margherita Pizza", price = 900.0, costPrice = 500.0, categoryId = "pizza", sku = "PIZ-001", variants = sizeVariants),
            Product(name = "Fries", price = 250.0, costPrice = 100.0, categoryId = "fries", sku = "FRY-001", variants = sizeVariants),
            Product(name = "Loaded Fries", price = 380.0, costPrice = 180.0, categoryId = "fries", sku = "FRY-002"),
            Product(name = "Coke", price = 150.0, costPrice = 70.0, categoryId = "drinks", sku = "DRK-001"),
            Product(name = "Pepsi", price = 150.0, costPrice = 70.0, categoryId = "drinks", sku = "DRK-002"),
            Product(name = "Water", price = 80.0, costPrice = 30.0, categoryId = "drinks", sku = "DRK-003"),
            Product(name = "Zinger Deal", price = 950.0, costPrice = 550.0, categoryId = "deals", sku = "DL-001", featured = true),
            Product(name = "Family Deal", price = 2400.0, costPrice = 1400.0, categoryId = "deals", sku = "DL-002", featured = true)
        )
        products.forEach { productRepository.upsertProduct(restaurantId, it) }

        (1..8).forEach { n ->
            tableRepository.upsertTable(restaurantId, RestaurantTable(label = "Table %02d".format(n)))
        }
    }
}
