package com.example.util

import com.example.data.model.LineItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CalculationResult(
    val subtotal: Double,
    val discount: Double,
    val shipping: Double,
    val roundoff: Double,
    val taxableAmount: Double,
    val gstRate: Double,
    val gstType: String,
    val gstMode: String,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val totalGst: Double,
    val total: Double,
    val amountPaid: Double,
    val pendingAmount: Double,
    val status: String
)

object FormatUtils {
    fun formatMoney(amount: Double, currency: String = "INR"): String {
        val symbol = when (currency.uppercase()) {
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> "$currency "
        }
        val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
        return symbol + formatter.format(amount)
    }

    fun formatDate(timestampOrDateString: String?): String {
        if (timestampOrDateString.isNullOrBlank()) {
            return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        }
        return try {
            if (timestampOrDateString.contains("-")) {
                val parts = timestampOrDateString.split("-")
                if (parts.size == 3) {
                    "${parts[2]}/${parts[1]}/${parts[0]}"
                } else timestampOrDateString
            } else timestampOrDateString
        } catch (e: Exception) {
            timestampOrDateString
        }
    }

    fun currentDateFormatted(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun generateNextInvoiceNumber(prefix: String, sequenceNumber: Int): String {
        val cleanPrefix = prefix.ifBlank { "INV" }.uppercase()
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        val formattedSeq = String.format(Locale.US, "%04d", sequenceNumber.coerceAtLeast(1))
        return "$cleanPrefix-$year-$formattedSeq"
    }

    fun calculateInvoice(
        items: List<LineItem>,
        discount: Double,
        shipping: Double,
        roundoff: Double,
        gstRate: Double,
        gstType: String,
        gstMode: String,
        amountPaid: Double,
        dueDate: String = ""
    ): CalculationResult {
        val subtotal = items.sumOf { it.total }
        val discountBase = (subtotal - discount).coerceAtLeast(0.0)

        var taxable = discountBase
        var totalGst = 0.0

        if (gstRate > 0 && gstType != "none") {
            if (gstMode == "inclusive" || gstMode == "reverse") {
                taxable = discountBase / (1 + gstRate / 100.0)
                totalGst = discountBase - taxable
            } else {
                taxable = discountBase
                totalGst = taxable * (gstRate / 100.0)
            }
        }

        val cgst = if (gstType == "intra") totalGst / 2.0 else 0.0
        val sgst = if (gstType == "intra") totalGst / 2.0 else 0.0
        val igst = if (gstType == "inter") totalGst else 0.0

        val grossBeforeShipping = if (gstMode == "inclusive" || gstMode == "reverse") {
            discountBase
        } else {
            discountBase + totalGst
        }

        val finalTotal = (grossBeforeShipping + shipping + roundoff).coerceAtLeast(0.0)
        val pending = (finalTotal - amountPaid).coerceAtLeast(0.0)

        val status = when {
            amountPaid >= finalTotal && finalTotal > 0.0 -> "PAID"
            amountPaid > 0.0 && amountPaid < finalTotal -> "PARTIALLY PAID"
            dueDate.isNotBlank() && isOverdue(dueDate) -> "OVERDUE"
            else -> "UNPAID"
        }

        return CalculationResult(
            subtotal = subtotal,
            discount = discount,
            shipping = shipping,
            roundoff = roundoff,
            taxableAmount = taxable,
            gstRate = gstRate,
            gstType = gstType,
            gstMode = gstMode,
            cgst = cgst,
            sgst = sgst,
            igst = igst,
            totalGst = totalGst,
            total = finalTotal,
            amountPaid = amountPaid,
            pendingAmount = pending,
            status = status
        )
    }

    private fun isOverdue(dueDateStr: String): Boolean {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val due = sdf.parse(dueDateStr) ?: return false
            Date().after(due)
        } catch (e: Exception) {
            false
        }
    }
}
