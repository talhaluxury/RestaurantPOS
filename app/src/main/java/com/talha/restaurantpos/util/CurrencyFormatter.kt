package com.talha.restaurantpos.util

import java.text.DecimalFormat

object CurrencyFormatter {
    private val format = DecimalFormat("#,##0")
    private val formatDecimal = DecimalFormat("#,##0.00")

    /** e.g. format(1700.0, "PKR") -> "Rs 1,700" (PKR uses "Rs" symbol by convention) */
    fun format(amount: Double, currencyCode: String): String {
        val symbol = symbolFor(currencyCode)
        val isWhole = amount == Math.floor(amount)
        val number = if (isWhole) format.format(amount) else formatDecimal.format(amount)
        return "$symbol $number"
    }

    private fun symbolFor(code: String): String = when (code.uppercase()) {
        "PKR" -> "Rs"
        "USD" -> "$"
        "AED" -> "AED"
        "SAR" -> "SAR"
        "GBP" -> "£"
        "EUR" -> "€"
        "INR" -> "₹"
        else -> code.uppercase()
    }
}
