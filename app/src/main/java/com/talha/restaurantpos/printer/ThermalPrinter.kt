package com.talha.restaurantpos.printer

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.talha.restaurantpos.domain.model.Order
import com.talha.restaurantpos.domain.model.Restaurant
import com.talha.restaurantpos.util.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class PaperWidth(val charsPerLine: Int) { MM_58(32), MM_80(48) }

data class PairedPrinter(val name: String, val address: String)

sealed class PrinterResult {
    object Success : PrinterResult()
    data class Failure(val message: String) : PrinterResult()
}

/**
 * ESC/POS printing over classic Bluetooth SPP (RFCOMM) — the near-universal protocol for
 * budget/mid-range thermal receipt printers (Sunmi, Goojprt, Rongta, generic "58mm/80mm
 * Bluetooth printer" units sold on Daraz/Alibaba). This talks to the printer directly at the
 * byte level, independent of Android's Print framework/PrintDocumentAdapter path used for
 * proper AirPrint-style/registered print-service printers (see PdfPrintDocumentAdapter).
 *
 * Architecture note (spec §12): new printer protocols (network ESC/POS over TCP:9100, USB,
 * or a vendor SDK) can be added later as additional implementations of [ThermalPrinter]
 * without touching any billing/receipt code — callers only depend on this interface.
 */
interface ThermalPrinter {
    suspend fun listPairedPrinters(): List<PairedPrinter>
    suspend fun connect(address: String): PrinterResult
    suspend fun disconnect()
    suspend fun isConnected(): Boolean
    suspend fun testPrint(): PrinterResult
    suspend fun printReceipt(restaurant: Restaurant, order: Order, paperWidth: PaperWidth): PrinterResult
}

@Singleton
class BluetoothEscPosPrinter @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : ThermalPrinter {

    // Standard Serial Port Profile UUID used by virtually all classic-Bluetooth ESC/POS printers.
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    private val adapter: BluetoothAdapter?
        get() = BluetoothAdapter.getDefaultAdapter()

    @SuppressLint("MissingPermission")
    override suspend fun listPairedPrinters(): List<PairedPrinter> = withContext(Dispatchers.IO) {
        val bonded = adapter?.bondedDevices ?: return@withContext emptyList()
        bonded.map { PairedPrinter(it.name ?: it.address, it.address) }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String): PrinterResult = withContext(Dispatchers.IO) {
        runCatching {
            disconnect()
            val device: BluetoothDevice = adapter?.getRemoteDevice(address) ?: error("Bluetooth not available")
            val newSocket = device.createRfcommSocketToServiceRecord(sppUuid)
            adapter?.cancelDiscovery()
            newSocket.connect()
            socket = newSocket
            outputStream = newSocket.outputStream
            PrinterResult.Success
        }.getOrElse { PrinterResult.Failure(it.message ?: "Could not connect to printer") }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        runCatching { outputStream?.close(); socket?.close() }
        outputStream = null
        socket = null
        Unit
    }

    override suspend fun isConnected(): Boolean = socket?.isConnected == true

    override suspend fun testPrint(): PrinterResult = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext PrinterResult.Failure("Printer not connected.")
        runCatching {
            out.write(EscPos.init())
            out.write(EscPos.centerAlign())
            out.write(EscPos.bold(true))
            out.write(EscPos.text("Talha POS\n"))
            out.write(EscPos.bold(false))
            out.write(EscPos.text("Test Print OK\n"))
            out.write(EscPos.feedAndCut())
            out.flush()
            PrinterResult.Success
        }.getOrElse { PrinterResult.Failure(it.message ?: "Test print failed") }
    }

    override suspend fun printReceipt(restaurant: Restaurant, order: Order, paperWidth: PaperWidth): PrinterResult =
        withContext(Dispatchers.IO) {
            val out = outputStream ?: return@withContext PrinterResult.Failure("Printer not connected.")
            runCatching {
                val width = paperWidth.charsPerLine
                out.write(EscPos.init())
                out.write(EscPos.centerAlign())
                out.write(EscPos.bold(true))
                out.write(EscPos.text("${restaurant.name}\n"))
                out.write(EscPos.bold(false))
                if (restaurant.address.isNotBlank()) out.write(EscPos.text("${restaurant.address}\n"))
                if (restaurant.phone.isNotBlank()) out.write(EscPos.text("${restaurant.phone}\n"))
                out.write(EscPos.text(EscPos.divider(width)))
                out.write(EscPos.leftAlign())
                out.write(EscPos.text("Order ${order.orderNumber}\n"))
                out.write(EscPos.text("Cashier: ${order.cashierName}\n"))
                order.tableLabel?.let { out.write(EscPos.text("Table: $it\n")) }
                out.write(EscPos.text(EscPos.divider(width)))
                order.items.forEach { item ->
                    val name = if (item.variantName != null) "${item.name} (${item.variantName})" else item.name
                    out.write(EscPos.text(EscPos.twoColumn(name, CurrencyFormatter.format(item.lineTotal, restaurant.currency), width)))
                    out.write(EscPos.text("  x${item.quantity}\n"))
                }
                out.write(EscPos.text(EscPos.divider(width)))
                out.write(EscPos.text(EscPos.twoColumn("Subtotal", CurrencyFormatter.format(order.subtotal, restaurant.currency), width)))
                if (order.discount > 0) out.write(EscPos.text(EscPos.twoColumn("Discount", "-" + CurrencyFormatter.format(order.discount, restaurant.currency), width)))
                if (order.tax > 0) out.write(EscPos.text(EscPos.twoColumn("Tax", CurrencyFormatter.format(order.tax, restaurant.currency), width)))
                out.write(EscPos.bold(true))
                out.write(EscPos.text(EscPos.twoColumn("TOTAL", CurrencyFormatter.format(order.total, restaurant.currency), width)))
                out.write(EscPos.bold(false))
                out.write(EscPos.text(EscPos.divider(width)))
                out.write(EscPos.centerAlign())
                out.write(EscPos.text("${restaurant.receiptFooter}\n"))
                out.write(EscPos.feedAndCut())
                out.flush()
                PrinterResult.Success
            }.getOrElse { PrinterResult.Failure(it.message ?: "Print failed") }
        }
}

/** Minimal ESC/POS command builder — covers exactly what receipt printing needs. */
private object EscPos {
    fun init(): ByteArray = byteArrayOf(0x1B, 0x40)
    fun centerAlign(): ByteArray = byteArrayOf(0x1B, 0x61, 0x01)
    fun leftAlign(): ByteArray = byteArrayOf(0x1B, 0x61, 0x00)
    fun bold(on: Boolean): ByteArray = byteArrayOf(0x1B, 0x45, if (on) 0x01 else 0x00)
    fun text(s: String): ByteArray = s.toByteArray(Charsets.UTF_8)
    fun feedAndCut(): ByteArray = byteArrayOf(0x0A, 0x0A, 0x0A, 0x1D, 0x56, 0x00)
    fun divider(width: Int): String = "-".repeat(width) + "\n"
    fun twoColumn(left: String, right: String, width: Int): String {
        val space = (width - left.length - right.length).coerceAtLeast(1)
        return left + " ".repeat(space) + right + "\n"
    }
}
