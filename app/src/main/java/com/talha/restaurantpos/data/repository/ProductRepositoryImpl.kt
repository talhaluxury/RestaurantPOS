package com.talha.restaurantpos.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.talha.restaurantpos.data.remote.CloudinaryUploader
import com.talha.restaurantpos.data.local.Converters
import com.talha.restaurantpos.data.local.dao.CategoryDao
import com.talha.restaurantpos.data.local.dao.ProductDao
import com.talha.restaurantpos.data.local.entity.CategoryEntity
import com.talha.restaurantpos.data.local.entity.ProductEntity
import com.talha.restaurantpos.data.remote.FirestorePaths
import com.talha.restaurantpos.domain.model.Category
import com.talha.restaurantpos.domain.model.Product
import com.talha.restaurantpos.domain.model.ProductAddOn
import com.talha.restaurantpos.domain.model.ProductVariant
import com.talha.restaurantpos.domain.repository.ProductRepository
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
class ProductRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryUploader: CloudinaryUploader,
    private val categoryDao: CategoryDao,
    private val productDao: ProductDao,
    private val networkMonitor: NetworkMonitor
) : ProductRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val listenedRestaurants = mutableSetOf<String>()

    /** Starts a Firestore realtime listener (once per restaurant) that mirrors remote data into Room. */
    private fun ensureRemoteListener(restaurantId: String) {
        if (!listenedRestaurants.add(restaurantId)) return

        firestore.collection(FirestorePaths.categories(restaurantId))
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                syncScope.launch {
                    val entities = snap.documents.map { doc ->
                        CategoryEntity(
                            id = doc.id,
                            restaurantId = restaurantId,
                            name = doc.getString("name") ?: "",
                            sortOrder = (doc.getLong("sortOrder") ?: 0).toInt()
                        )
                    }
                    categoryDao.upsertAll(entities)
                }
            }

        firestore.collection(FirestorePaths.products(restaurantId))
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                syncScope.launch {
                    val entities = snap.documents.map { doc -> docToProductEntity(restaurantId, doc.id, doc.data ?: emptyMap()) }
                    productDao.upsertAll(entities)
                }
            }
    }

    private fun docToProductEntity(restaurantId: String, id: String, data: Map<String, Any?>): ProductEntity {
        @Suppress("UNCHECKED_CAST")
        val variants = (data["variants"] as? List<Map<String, Any?>>)?.map {
            ProductVariant(it["name"] as? String ?: "", (it["priceDelta"] as? Number)?.toDouble() ?: 0.0)
        } ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val addOns = (data["addOns"] as? List<Map<String, Any?>>)?.map {
            ProductAddOn(it["name"] as? String ?: "", (it["price"] as? Number)?.toDouble() ?: 0.0)
        } ?: emptyList()

        return ProductEntity(
            id = id,
            restaurantId = restaurantId,
            name = data["name"] as? String ?: "",
            description = data["description"] as? String ?: "",
            price = (data["price"] as? Number)?.toDouble() ?: 0.0,
            costPrice = (data["costPrice"] as? Number)?.toDouble() ?: 0.0,
            imageUrl = data["imageUrl"] as? String ?: "",
            categoryId = data["categoryId"] as? String ?: "",
            sku = data["sku"] as? String ?: "",
            available = data["available"] as? Boolean ?: true,
            featured = data["featured"] as? Boolean ?: false,
            taxPercent = (data["taxPercent"] as? Number)?.toDouble() ?: 0.0,
            variantsJson = Converters.variantsToJson(variants),
            addOnsJson = Converters.addOnsToJson(addOns)
        )
    }

    private fun ProductEntity.toDomain() = Product(
        id = id, name = name, description = description, price = price, costPrice = costPrice,
        imageUrl = imageUrl, categoryId = categoryId, sku = sku, available = available,
        featured = featured, taxPercent = taxPercent,
        variants = Converters.variantsFromJson(variantsJson),
        addOns = Converters.addOnsFromJson(addOnsJson)
    )

    private fun CategoryEntity.toDomain() = Category(id = id, name = name, sortOrder = sortOrder)

    override fun observeCategories(restaurantId: String): Flow<List<Category>> {
        ensureRemoteListener(restaurantId)
        return categoryDao.observeAll(restaurantId).map { list -> list.map { it.toDomain() } }
    }

    override fun observeProducts(restaurantId: String): Flow<List<Product>> {
        ensureRemoteListener(restaurantId)
        return productDao.observeAll(restaurantId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun upsertCategory(restaurantId: String, category: Category): Result<Unit> = runCatching {
        val id = category.id.ifBlank { UUID.randomUUID().toString() }
        categoryDao.upsert(CategoryEntity(id, restaurantId, category.name, category.sortOrder))
        if (networkMonitor.isOnlineNow()) {
            firestore.collection(FirestorePaths.categories(restaurantId)).document(id)
                .set(mapOf("name" to category.name, "sortOrder" to category.sortOrder)).await()
        }
    }

    override suspend fun deleteCategory(restaurantId: String, categoryId: String): Result<Unit> = runCatching {
        categoryDao.delete(categoryId)
        if (networkMonitor.isOnlineNow()) {
            firestore.collection(FirestorePaths.categories(restaurantId)).document(categoryId).delete().await()
        }
    }

    override suspend fun upsertProduct(restaurantId: String, product: Product): Result<Unit> = runCatching {
        val id = product.id.ifBlank { UUID.randomUUID().toString() }
        val entity = docToProductEntity(
            restaurantId, id, mapOf(
                "name" to product.name, "description" to product.description, "price" to product.price,
                "costPrice" to product.costPrice, "imageUrl" to product.imageUrl, "categoryId" to product.categoryId,
                "sku" to product.sku, "available" to product.available, "featured" to product.featured,
                "taxPercent" to product.taxPercent,
                "variants" to product.variants.map { mapOf("name" to it.name, "priceDelta" to it.priceDelta) },
                "addOns" to product.addOns.map { mapOf("name" to it.name, "price" to it.price) }
            )
        )
        productDao.upsert(entity)
        if (networkMonitor.isOnlineNow()) {
            firestore.collection(FirestorePaths.products(restaurantId)).document(id).set(
                mapOf(
                    "name" to product.name, "description" to product.description, "price" to product.price,
                    "costPrice" to product.costPrice, "imageUrl" to product.imageUrl, "categoryId" to product.categoryId,
                    "sku" to product.sku, "available" to product.available, "featured" to product.featured,
                    "taxPercent" to product.taxPercent,
                    "variants" to product.variants.map { mapOf("name" to it.name, "priceDelta" to it.priceDelta) },
                    "addOns" to product.addOns.map { mapOf("name" to it.name, "price" to it.price) }
                )
            ).await()
        }
    }

    override suspend fun deleteProduct(restaurantId: String, productId: String): Result<Unit> = runCatching {
        productDao.delete(productId)
        if (networkMonitor.isOnlineNow()) {
            firestore.collection(FirestorePaths.products(restaurantId)).document(productId).delete().await()
        }
    }

    override suspend fun uploadProductImage(restaurantId: String, productId: String, localUri: String): Result<String> {
        if (!networkMonitor.isOnlineNow()) {
            return Result.failure(java.io.IOException("No internet connection — connect and try again"))
        }
        return cloudinaryUploader.upload(
            localUri = localUri,
            folder = "restaurants/$restaurantId/products",
            publicId = productId
        )
    }
}
