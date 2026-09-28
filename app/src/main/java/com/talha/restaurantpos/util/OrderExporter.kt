package com.talha.restaurantpos.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.talha.restaurantpos.domain.model.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exports all orders currently known to the app (i.e. everything in the local Room
 * database, which mirrors Firestore) to a CSV file in the phone's Downloads folder.
 *
 * This is a manual backup: even though every order is already safely stored in
 * Firestore (and re-downloads automatically into the app after a reinstall + login),
 * some restaurant owners want a plain file they can open in Excel or send to an
 * accountant, independent of the app or their Google/Firebase account.
 */
object OrderExporter {

    /** Returns the saved file's display name on success, or null on failure. */
    fun exportToCsv(context: Context, orders: List<Order>, currency: String): String? {
        if (orders.isEmpty()) return null

        val dateFmt = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val rowFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val fileName = "RestaurantPOS_Orders_${dateFmt.format(Date())}.csv"

        val header = "Order Number,Date,Customer Name,Customer Phone,Order Type,Status,Items,Subtotal,Discount,Tax,Service Charge,Total ($currency),Payment Method,Cashier\n"
        val body = buildString {
            orders.sortedByDescending { it.createdAt }.forEach { o ->
                val itemsSummary = o.items.joinToString("; ") { "${it.quantity}x ${it.name}" }
                append(csv(o.orderNumber)); append(',')
                append(csv(rowFmt.format(Date(o.createdAt)))); append(',')
                append(csv(o.customer.name)); append(',')
                append(csv(o.customer.phone)); append(',')
                append(csv(o.orderType.name)); append(',')
                append(csv(o.status.name)); append(',')
                append(csv(itemsSummary)); append(',')
                append(o.subtotal); append(',')
                append(o.discount); append(',')
                append(o.tax); append(',')
                append(o.serviceCharge); append(',')
                append(o.total); append(',')
                append(csv(o.paymentMethod?.name ?: "")); append(',')
                append(csv(o.cashierName))
                append('\n')
            }
        }
        val csvContent = header + body

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
                resolver.openOutputStream(uri)?.use { it.write(csvContent.toByteArray()) } ?: return null
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                downloadsDir.mkdirs()
                java.io.File(downloadsDir, fileName).writeText(csvContent)
            }
            fileName
        } catch (e: Exception) {
            null
        }
    }

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) "\"$escaped\"" else escaped
    }
}
