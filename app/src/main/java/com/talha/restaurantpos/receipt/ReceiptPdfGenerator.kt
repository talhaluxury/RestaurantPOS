package com.talha.restaurantpos.receipt

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.talha.restaurantpos.domain.model.Order
import com.talha.restaurantpos.domain.model.Restaurant
import com.talha.restaurantpos.util.CurrencyFormatter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReceiptFormat(val widthPt: Int, val label: String) {
    A4(595, "A4"),
    THERMAL_80MM(227, "80mm"), // 80mm at 72dpi ≈ 227pt printable width
    THERMAL_58MM(164, "58mm")
}

/**
 * Renders a receipt directly to a PDF using android.graphics.pdf.PdfDocument — no external
 * dependency needed. Thermal formats produce a tall, narrow single-page receipt; A4 produces a
 * standard full-page invoice layout.
 */
object ReceiptPdfGenerator {

    fun generate(context: Context, restaurant: Restaurant, order: Order, format: ReceiptFormat): File {
        return when (format) {
            ReceiptFormat.A4 -> generateA4(context, restaurant, order)
            else -> generateThermal(context, restaurant, order, format)
        }
    }

    private fun outputFile(context: Context, order: Order): File {
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        return File(dir, "receipt_${order.orderNumber.replace("#", "").replace("-", "")}.pdf")
    }

    private fun generateThermal(context: Context, restaurant: Restaurant, order: Order, format: ReceiptFormat): File {
        val width = format.widthPt
        val lineHeight = 16
        val estimatedLines = 14 + order.items.size * 2 + 8
        val height = (estimatedLines * lineHeight) + 80

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = 20f
        val margin = 10f
        val centerPaint = textPaint(10f, Paint.Align.CENTER, bold = true)
        val normalPaint = textPaint(9f, Paint.Align.LEFT)
        val rightPaint = textPaint(9f, Paint.Align.RIGHT)
        val smallCenter = textPaint(8f, Paint.Align.CENTER)
        val centerX = width / 2f

        fun line(text: String, paint: Paint, x: Float = centerX) {
            canvas.drawText(text, x, y, paint)
            y += lineHeight
        }
        fun divider() {
            canvas.drawLine(margin, y, width - margin, y, Paint().apply { strokeWidth = 1f; color = android.graphics.Color.BLACK })
            y += lineHeight * 0.8f
        }

        line(restaurant.name.ifBlank { "Restaurant" }, textPaint(12f, Paint.Align.CENTER, bold = true))
        if (restaurant.address.isNotBlank()) line(restaurant.address, smallCenter)
        if (restaurant.phone.isNotBlank()) line(restaurant.phone, smallCenter)
        y += 4f
        divider()

        canvas.drawText("Order ${order.orderNumber}", margin, y, normalPaint); y += lineHeight
        canvas.drawText(dateFormat.format(Date(order.createdAt)), margin, y, normalPaint); y += lineHeight
        canvas.drawText("Cashier: ${order.cashierName}", margin, y, normalPaint); y += lineHeight
        if (order.tableLabel != null) { canvas.drawText("Table: ${order.tableLabel}", margin, y, normalPaint); y += lineHeight }
        y += 4f
        divider()

        order.items.forEach { item ->
            val name = if (item.variantName != null) "${item.name} (${item.variantName})" else item.name
            canvas.drawText(name, margin, y, normalPaint)
            canvas.drawText(CurrencyFormatter.format(item.lineTotal, restaurant.currency), width - margin, y, rightPaint)
            y += lineHeight
            canvas.drawText("  x${item.quantity} @ ${CurrencyFormatter.format(item.unitPrice, restaurant.currency)}", margin, y, smallCenter.apply { textAlign = Paint.Align.LEFT })
            y += lineHeight
        }
        y += 4f
        divider()

        fun totalRow(label: String, value: String) {
            canvas.drawText(label, margin, y, normalPaint)
            canvas.drawText(value, width - margin, y, rightPaint)
            y += lineHeight
        }
        totalRow("Subtotal", CurrencyFormatter.format(order.subtotal, restaurant.currency))
        if (order.discount > 0) totalRow("Discount", "-" + CurrencyFormatter.format(order.discount, restaurant.currency))
        if (order.tax > 0) totalRow("Tax", CurrencyFormatter.format(order.tax, restaurant.currency))
        if (order.serviceCharge > 0) totalRow("Service", CurrencyFormatter.format(order.serviceCharge, restaurant.currency))
        divider()
        totalRow("TOTAL", CurrencyFormatter.format(order.total, restaurant.currency))
        divider()
        totalRow("Payment", order.paymentMethod?.name ?: "-")
        if (order.paymentMethod?.name == "CASH") {
            totalRow("Received", CurrencyFormatter.format(order.amountReceived, restaurant.currency))
            totalRow("Change", CurrencyFormatter.format(order.changeDue, restaurant.currency))
        }
        y += 8f
        line(restaurant.receiptFooter.ifBlank { "Thank You! Visit Again" }, smallCenter)

        document.finishPage(page)
        val file = outputFile(context, order)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun generateA4(context: Context, restaurant: Restaurant, order: Order): File {
        val width = 595
        val height = 842
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val margin = 48f
        var y = 70f

        canvas.drawText(restaurant.name.ifBlank { "Restaurant" }, margin, y, textPaint(22f, Paint.Align.LEFT, bold = true))
        y += 22f
        if (restaurant.address.isNotBlank()) { canvas.drawText(restaurant.address, margin, y, textPaint(11f, Paint.Align.LEFT)); y += 16f }
        if (restaurant.phone.isNotBlank()) { canvas.drawText(restaurant.phone, margin, y, textPaint(11f, Paint.Align.LEFT)); y += 16f }

        canvas.drawText("Invoice ${order.orderNumber}", width - margin, 70f, textPaint(14f, Paint.Align.RIGHT, bold = true))
        canvas.drawText(dateFormat.format(Date(order.createdAt)), width - margin, 88f, textPaint(11f, Paint.Align.RIGHT))

        y += 24f
        canvas.drawLine(margin, y, width - margin, y, Paint().apply { strokeWidth = 1f })
        y += 24f

        canvas.drawText("Item", margin, y, textPaint(11f, Paint.Align.LEFT, bold = true))
        canvas.drawText("Qty", width - 220f, y, textPaint(11f, Paint.Align.LEFT, bold = true))
        canvas.drawText("Price", width - 150f, y, textPaint(11f, Paint.Align.LEFT, bold = true))
        canvas.drawText("Total", width - margin, y, textPaint(11f, Paint.Align.RIGHT, bold = true))
        y += 18f

        order.items.forEach { item ->
            val name = if (item.variantName != null) "${item.name} (${item.variantName})" else item.name
            canvas.drawText(name, margin, y, textPaint(11f, Paint.Align.LEFT))
            canvas.drawText(item.quantity.toString(), width - 220f, y, textPaint(11f, Paint.Align.LEFT))
            canvas.drawText(CurrencyFormatter.format(item.unitPrice, restaurant.currency), width - 150f, y, textPaint(11f, Paint.Align.LEFT))
            canvas.drawText(CurrencyFormatter.format(item.lineTotal, restaurant.currency), width - margin, y, textPaint(11f, Paint.Align.RIGHT))
            y += 20f
        }

        y += 8f
        canvas.drawLine(margin, y, width - margin, y, Paint().apply { strokeWidth = 1f })
        y += 24f

        fun totalRow(label: String, value: String, bold: Boolean = false) {
            canvas.drawText(label, width - 220f, y, textPaint(12f, Paint.Align.LEFT, bold))
            canvas.drawText(value, width - margin, y, textPaint(12f, Paint.Align.RIGHT, bold))
            y += 20f
        }
        totalRow("Subtotal", CurrencyFormatter.format(order.subtotal, restaurant.currency))
        if (order.discount > 0) totalRow("Discount", "-" + CurrencyFormatter.format(order.discount, restaurant.currency))
        if (order.tax > 0) totalRow("Tax", CurrencyFormatter.format(order.tax, restaurant.currency))
        if (order.serviceCharge > 0) totalRow("Service Charge", CurrencyFormatter.format(order.serviceCharge, restaurant.currency))
        totalRow("Grand Total", CurrencyFormatter.format(order.total, restaurant.currency), bold = true)
        totalRow("Payment Method", order.paymentMethod?.name ?: "-")

        y += 24f
        canvas.drawText(restaurant.receiptFooter.ifBlank { "Thank You! Visit Again" }, margin, y, textPaint(11f, Paint.Align.LEFT))

        document.finishPage(page)
        val file = outputFile(context, order)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun textPaint(size: Float, align: Paint.Align, bold: Boolean = false): Paint = Paint().apply {
        textSize = size
        textAlign = align
        isAntiAlias = true
        typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
}
