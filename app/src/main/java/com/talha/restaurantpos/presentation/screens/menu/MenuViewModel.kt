package com.talha.restaurantpos.presentation.screens.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talha.restaurantpos.domain.model.Category
import com.talha.restaurantpos.domain.model.Product
import com.talha.restaurantpos.domain.repository.ProductRepository
import com.talha.restaurantpos.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuUiState(
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val currency: String = "PKR",
    val restaurantId: String = "",
    val saving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val restaurantId = sessionManager.currentRestaurantId() ?: return@launch
            _uiState.value = _uiState.value.copy(restaurantId = restaurantId)
            launch {
                productRepository.observeCategories(restaurantId).collect { cats ->
                    _uiState.value = _uiState.value.copy(categories = cats)
                }
            }
            launch {
                productRepository.observeProducts(restaurantId).collect { prods ->
                    _uiState.value = _uiState.value.copy(products = prods)
                }
            }
        }
    }

    fun addCategory(name: String) {
        val restaurantId = _uiState.value.restaurantId
        if (restaurantId.isBlank() || name.isBlank()) return
        viewModelScope.launch {
            productRepository.upsertCategory(restaurantId, Category(name = name, sortOrder = _uiState.value.categories.size))
        }
    }

    fun saveProduct(product: Product, localImageUri: String?, onSaved: () -> Unit) {
        val restaurantId = _uiState.value.restaurantId
        if (restaurantId.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "No restaurant session found — please log out and log back in")
            return
        }
        if (product.name.isBlank() || product.price <= 0.0) {
            _uiState.value = _uiState.value.copy(error = "Product name and a valid price are required")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, error = null)
            val productId = product.id.ifBlank { java.util.UUID.randomUUID().toString() }

            var finalImageUrl = product.imageUrl
            if (localImageUri != null) {
                val uploadResult = productRepository.uploadProductImage(restaurantId, productId, localImageUri)
                val uploadedUrl = uploadResult.getOrElse { e ->
                    _uiState.value = _uiState.value.copy(saving = false, error = "Image upload failed: ${e.message}")
                    return@launch
                }
                finalImageUrl = uploadedUrl
            }

            val finalProduct = product.copy(id = productId, imageUrl = finalImageUrl)
            productRepository.upsertProduct(restaurantId, finalProduct).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(saving = false); onSaved() },
                onFailure = { e -> _uiState.value = _uiState.value.copy(saving = false, error = e.message) }
            )
        }
    }

    fun deleteProduct(productId: String) {
        val restaurantId = _uiState.value.restaurantId
        if (restaurantId.isBlank()) return
        viewModelScope.launch { productRepository.deleteProduct(restaurantId, productId) }
    }

    fun toggleAvailability(product: Product) {
        val restaurantId = _uiState.value.restaurantId
        if (restaurantId.isBlank()) return
        viewModelScope.launch { productRepository.upsertProduct(restaurantId, product.copy(available = !product.available)) }
    }
}
