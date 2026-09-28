package com.talha.restaurantpos.presentation.screens.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talha.restaurantpos.domain.model.*
import com.talha.restaurantpos.domain.repository.ProductRepository
import com.talha.restaurantpos.domain.repository.RestaurantRepository
import com.talha.restaurantpos.domain.repository.TableRepository
import com.talha.restaurantpos.util.NetworkMonitor
import com.talha.restaurantpos.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class BillingUiState(
    val restaurantId: String = "",
    val currency: String = "PKR",
    val taxPercent: Double = 0.0,
    val serviceChargePercent: Double = 0.0,
    val categories: List<Category> = emptyList(),
    val products: List<Product> = emptyList(),
    val tables: List<RestaurantTable> = emptyList(),
    val selectedCategoryId: String? = null, // null = "All"
    val searchQuery: String = "",
    val cart: List<CartLine> = emptyList(),
    val discount: Double = 0.0,
    val orderType: OrderType = OrderType.DINE_IN,
    val selectedTableId: String? = null,
    val customer: CustomerInfo = CustomerInfo(),
    val isOnline: Boolean = true,
    val cashierName: String = ""
) {
    val subtotal: Double get() = cart.sumOf { it.lineTotal }
    val tax: Double get() = subtotal * (taxPercent / 100.0)
    val serviceCharge: Double get() = subtotal * (serviceChargePercent / 100.0)
    val total: Double get() = (subtotal - discount + tax + serviceCharge).coerceAtLeast(0.0)
    val itemCount: Int get() = cart.sumOf { it.quantity }
    val filteredProducts: List<Product> get() = products.filter {
        (selectedCategoryId == null || it.categoryId == selectedCategoryId) &&
            (searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true))
    }
}

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val restaurantRepository: RestaurantRepository,
    private val tableRepository: TableRepository,
    private val sessionManager: SessionManager,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val restaurantId = sessionManager.currentRestaurantId() ?: return@launch
            _uiState.value = _uiState.value.copy(restaurantId = restaurantId)

            launch {
                restaurantRepository.observeRestaurant(restaurantId).collect { r ->
                    if (r != null) _uiState.value = _uiState.value.copy(
                        currency = r.currency, taxPercent = r.taxPercent, serviceChargePercent = r.serviceChargePercent
                    )
                }
            }
            launch {
                productRepository.observeCategories(restaurantId).collect { cats ->
                    _uiState.value = _uiState.value.copy(categories = cats)
                }
            }
            launch {
                productRepository.observeProducts(restaurantId).collect { prods ->
                    _uiState.value = _uiState.value.copy(products = prods.filter { it.available })
                }
            }
            launch {
                tableRepository.observeTables(restaurantId).collect { tables ->
                    _uiState.value = _uiState.value.copy(tables = tables)
                }
            }
            launch {
                networkMonitor.observe().collect { online -> _uiState.value = _uiState.value.copy(isOnline = online) }
            }
            launch {
                sessionManager.cashierNameFlow.collect { name -> _uiState.value = _uiState.value.copy(cashierName = name) }
            }
        }
    }

    fun selectCategory(categoryId: String?) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    /** Products with no variants/add-ons go straight to cart in a single tap (spec §6). */
    fun quickAddToCart(product: Product) {
        addToCart(product, variant = null, addOns = emptyList(), quantity = 1)
    }

    fun addToCart(product: Product, variant: ProductVariant?, addOns: List<SelectedAddOn>, quantity: Int, notes: String = "") {
        val newLine = CartLine(
            lineId = UUID.randomUUID().toString(),
            product = product, variant = variant, addOns = addOns, quantity = quantity, notes = notes
        )
        // Merge with an identical existing line (same product + variant + add-ons) rather than duplicating.
        val existingIndex = _uiState.value.cart.indexOfFirst {
            it.product.id == product.id && it.variant == variant && it.addOns == addOns && it.notes == notes
        }
        val updatedCart = if (existingIndex >= 0) {
            _uiState.value.cart.toMutableList().apply {
                this[existingIndex] = this[existingIndex].copy(quantity = this[existingIndex].quantity + quantity)
            }
        } else {
            _uiState.value.cart + newLine
        }
        _uiState.value = _uiState.value.copy(cart = updatedCart)
    }

    fun updateLineQuantity(lineId: String, quantity: Int) {
        if (quantity <= 0) {
            removeLine(lineId)
            return
        }
        _uiState.value = _uiState.value.copy(
            cart = _uiState.value.cart.map { if (it.lineId == lineId) it.copy(quantity = quantity) else it }
        )
    }

    fun removeLine(lineId: String) {
        _uiState.value = _uiState.value.copy(cart = _uiState.value.cart.filterNot { it.lineId == lineId })
    }

    fun setDiscount(amount: Double) {
        _uiState.value = _uiState.value.copy(discount = amount.coerceAtLeast(0.0))
    }

    fun setOrderType(type: OrderType) {
        _uiState.value = _uiState.value.copy(orderType = type, selectedTableId = if (type != OrderType.DINE_IN) null else _uiState.value.selectedTableId)
    }

    fun selectTable(tableId: String) {
        _uiState.value = _uiState.value.copy(selectedTableId = tableId)
    }

    fun setCustomer(customer: CustomerInfo) {
        _uiState.value = _uiState.value.copy(customer = customer)
    }

    fun clearCart() {
        _uiState.value = _uiState.value.copy(cart = emptyList(), discount = 0.0, customer = CustomerInfo())
    }

    /** Builds the pending Order to hand off to the Payment screen; order number/id assigned at placement time. */
    fun buildPendingOrder(): Order {
        val s = _uiState.value
        val table = s.tables.find { it.id == s.selectedTableId }
        return Order(
            items = s.cart.map { line ->
                OrderItemRecord(
                    productId = line.product.id, name = line.product.name,
                    variantName = line.variant?.name, addOnNames = line.addOns.map { it.name },
                    unitPrice = line.unitPrice, quantity = line.quantity, lineTotal = line.lineTotal
                )
            },
            orderType = s.orderType, tableId = table?.id, tableLabel = table?.label,
            customer = s.customer, subtotal = s.subtotal, discount = s.discount, tax = s.tax,
            serviceCharge = s.serviceCharge, total = s.total, cashierName = s.cashierName
        )
    }
}
