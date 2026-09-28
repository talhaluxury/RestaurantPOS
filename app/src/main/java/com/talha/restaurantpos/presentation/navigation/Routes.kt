package com.talha.restaurantpos.presentation.navigation

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val RESTAURANT_SETUP = "restaurant_setup"

    const val DASHBOARD = "dashboard"
    const val NEW_BILL = "new_bill"
    const val ORDERS = "orders"
    const val ORDER_DETAIL = "order_detail/{orderId}"
    const val MENU = "menu"
    const val ADD_EDIT_PRODUCT = "add_edit_product?productId={productId}"
    const val PAYMENT = "payment"
    const val PAYMENT_SUCCESS = "payment_success/{orderId}"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val STAFF = "staff"
    const val INVENTORY = "inventory"
    const val TABLES = "tables"
    const val KITCHEN = "kitchen"

    fun orderDetail(orderId: String) = "order_detail/$orderId"
    fun addEditProduct(productId: String? = null) = "add_edit_product?productId=${productId ?: ""}"
    fun paymentSuccess(orderId: String) = "payment_success/$orderId"
}
