package com.talha.restaurantpos.data.local

import org.json.JSONArray
import org.json.JSONObject
import com.talha.restaurantpos.domain.model.ProductVariant
import com.talha.restaurantpos.domain.model.ProductAddOn
import com.talha.restaurantpos.domain.model.OrderItemRecord

/**
 * Lightweight JSON (de)serialization without extra dependencies (org.json ships with Android).
 * Used to store list fields (variants, add-ons, order items) as TEXT columns in Room.
 */
object Converters {

    fun variantsToJson(list: List<ProductVariant>): String {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("name", it.name)
                put("priceDelta", it.priceDelta)
            })
        }
        return arr.toString()
    }

    fun variantsFromJson(json: String): List<ProductVariant> {
        if (json.isBlank()) return emptyList()
        val arr = JSONArray(json)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            ProductVariant(o.getString("name"), o.getDouble("priceDelta"))
        }
    }

    fun addOnsToJson(list: List<ProductAddOn>): String {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("name", it.name)
                put("price", it.price)
            })
        }
        return arr.toString()
    }

    fun addOnsFromJson(json: String): List<ProductAddOn> {
        if (json.isBlank()) return emptyList()
        val arr = JSONArray(json)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            ProductAddOn(o.getString("name"), o.getDouble("price"))
        }
    }

    fun orderItemsToJson(list: List<OrderItemRecord>): String {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("productId", item.productId)
                put("name", item.name)
                put("variantName", item.variantName ?: JSONObject.NULL)
                put("addOnNames", JSONArray(item.addOnNames))
                put("unitPrice", item.unitPrice)
                put("quantity", item.quantity)
                put("lineTotal", item.lineTotal)
            })
        }
        return arr.toString()
    }

    fun orderItemsFromJson(json: String): List<OrderItemRecord> {
        if (json.isBlank()) return emptyList()
        val arr = JSONArray(json)
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            val addOns = mutableListOf<String>()
            val addOnsArr = o.optJSONArray("addOnNames")
            if (addOnsArr != null) {
                for (i in 0 until addOnsArr.length()) addOns.add(addOnsArr.getString(i))
            }
            OrderItemRecord(
                productId = o.getString("productId"),
                name = o.getString("name"),
                variantName = if (o.isNull("variantName")) null else o.getString("variantName"),
                addOnNames = addOns,
                unitPrice = o.getDouble("unitPrice"),
                quantity = o.getInt("quantity"),
                lineTotal = o.getDouble("lineTotal")
            )
        }
    }
}
