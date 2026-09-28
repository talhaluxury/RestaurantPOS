package com.talha.restaurantpos.presentation.screens.payment

import android.content.Context
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.talha.restaurantpos.domain.model.Order
import com.talha.restaurantpos.domain.model.Restaurant
import com.talha.restaurantpos.printer.PaperWidth
import com.talha.restaurantpos.printer.PrinterResult
import com.talha.restaurantpos.printer.ThermalPrinter
import com.talha.restaurantpos.receipt.ReceiptFormat
import com.talha.restaurantpos.receipt.ReceiptPdfGenerator
import com.talha.restaurantpos.util.PdfPrintDocumentAdapter
import com.talha.restaurantpos.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared by PaymentSuccessScreen and OrderDetailScreen so "Print" always tries the
 * paired Bluetooth thermal printer (set up once in Settings) first — that's what
 * actually produces a real receipt slip. Falls back to Android's generic Print dialog
 * (works with A4/network printers via a Print Service) only if no thermal printer is
 * connected, so the button never just silently does nothing.
 */
@HiltViewModel
class ReceiptPrinterViewModel @Inject constructor(
    private val thermalPrinter: ThermalPrinter,
    private val sessionManager: SessionManager
) : ViewModel() {

    fun printReceipt(
        context: Context,
        restaurant: Restaurant,
        order: Order,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (thermalPrinter.isConnected()) {
                val width = PaperWidth.valueOf(sessionManager.paperWidthFlow.first())
                when (val result = thermalPrinter.printReceipt(restaurant, order, width)) {
                    is PrinterResult.Success -> onResult("Slip sent to printer")
                    is PrinterResult.Failure -> onResult("Print failed: ${result.message}")
                }
            } else {
                // No thermal printer connected — fall back to the system print dialog
                // (works for A4 printers or a manufacturer print-service app).
                printViaSystemDialog(context, restaurant, order)
                onResult("No thermal printer connected — opened system print dialog. Pair one in Settings > Printer for direct slip printing.")
            }
        }
    }

    private fun printViaSystemDialog(context: Context, restaurant: Restaurant, order: Order) {
        val file = ReceiptPdfGenerator.generate(context, restaurant, order, ReceiptFormat.THERMAL_80MM)
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
        runCatching {
            printManager?.print(
                "Receipt ${order.orderNumber}",
                PdfPrintDocumentAdapter(file, "Receipt ${order.orderNumber}"),
                android.print.PrintAttributes.Builder().build()
            )
        }
    }
}
