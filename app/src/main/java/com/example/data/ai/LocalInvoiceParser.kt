package com.example.data.ai

import com.example.data.model.LineItem
import java.util.regex.Pattern

data class ParsedInvoiceData(
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val items: List<LineItem> = emptyList(),
    val discount: Double = 0.0,
    val shipping: Double = 0.0,
    val gstRate: Double = 0.0,
    val paymentMethod: String = "UPI",
    val paymentStatus: String = "UNPAID",
    val amountPaid: Double = 0.0,
    val notes: String = ""
) {
    fun isValid(): Boolean {
        return items.any { it.name.trim().isNotBlank() && it.price > 0.0 }
    }
}

data class CalculationDiscrepancy(
    val expected: Double,
    val actual: Double,
    val diff: Double,
    val isMatch: Boolean
)

object LocalInvoiceParser {
    fun parseNaturalOrder(text: String): ParsedInvoiceData {
        if (text.isBlank()) return ParsedInvoiceData()

        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var customerName = ""
        var customerPhone = ""
        var customerAddress = ""
        var discount = 0.0
        var shipping = 0.0
        var paymentMethod = "UPI"
        var paymentStatus = "UNPAID"
        var amountPaid = 0.0
        val items = mutableListOf<LineItem>()

        val phonePattern = Pattern.compile("(?:\\+?91[- ]?)?[6-9]\\d{9}")
        val phoneMatcher = phonePattern.matcher(text)
        if (phoneMatcher.find()) {
            customerPhone = phoneMatcher.group()
        }

        val namePattern = Pattern.compile("(?:customer|name|for|bhai|bill to|bill for)\\s*[:=]?\\s*([A-Za-z][A-Za-z\\s]{2,25})", Pattern.CASE_INSENSITIVE)
        val nameMatcher = namePattern.matcher(text)
        if (nameMatcher.find()) {
            customerName = nameMatcher.group(1)?.trim() ?: ""
        } else if (lines.isNotEmpty() && !lines[0].contains(Regex("\\d")) && lines[0].length in 3..25) {
            customerName = lines[0]
        }

        val discPattern = Pattern.compile("discount\\s*[:=]?\\s*(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE)
        val discMatcher = discPattern.matcher(text)
        if (discMatcher.find()) {
            discount = discMatcher.group(1)?.toDoubleOrNull() ?: 0.0
        }

        val shipPattern = Pattern.compile("(?:shipping|delivery)\\s*[:=]?\\s*(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE)
        val shipMatcher = shipPattern.matcher(text)
        if (shipMatcher.find()) {
            shipping = shipMatcher.group(1)?.toDoubleOrNull() ?: 0.0
        }

        if (text.contains(Regex("paid|payment done|successful|received", RegexOption.IGNORE_CASE))) {
            paymentStatus = "PAID"
            val paidPattern = Pattern.compile("(?:paid|payment done|received)\\s*(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE)
            val paidMatcher = paidPattern.matcher(text)
            if (paidMatcher.find()) {
                amountPaid = paidMatcher.group(1)?.toDoubleOrNull() ?: 0.0
            }
        }
        if (text.contains(Regex("cash", RegexOption.IGNORE_CASE))) paymentMethod = "Cash"
        else if (text.contains(Regex("card", RegexOption.IGNORE_CASE))) paymentMethod = "Card"
        else if (text.contains(Regex("bank|transfer|neft|rtgs", RegexOption.IGNORE_CASE))) paymentMethod = "Bank Transfer"
        else paymentMethod = "UPI"

        for (line in lines) {
            if (line.equals(customerName, ignoreCase = true)) continue
            if (line.startsWith("discount", ignoreCase = true)) continue
            if (line.startsWith("shipping", ignoreCase = true)) continue
            if (line.startsWith("phone", ignoreCase = true)) continue

            val pA = Pattern.compile("^(\\d+)\\s*(?:x|×|pcs)?\\s+([A-Za-z0-9\\s\\-\\.,]+?)(?:\\s+(?:at|@|₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?))\\s*$", Pattern.CASE_INSENSITIVE)
            val mA = pA.matcher(line)
            if (mA.matches()) {
                val qty = mA.group(1)?.toDoubleOrNull() ?: 1.0
                val name = mA.group(2)?.trim() ?: ""
                val price = mA.group(3)?.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && price > 0) {
                    items.add(LineItem(name = name, qty = qty, price = price))
                    continue
                }
            }

            val pB = Pattern.compile("^([A-Za-z0-9\\s\\-\\.,]+?)\\s+(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?)\\s*(?:x|×)\\s*(\\d+)\\s*$", Pattern.CASE_INSENSITIVE)
            val mB = pB.matcher(line)
            if (mB.matches()) {
                val name = mB.group(1)?.trim() ?: ""
                val price = mB.group(2)?.toDoubleOrNull() ?: 0.0
                val qty = mB.group(3)?.toDoubleOrNull() ?: 1.0
                if (name.isNotBlank() && price > 0) {
                    items.add(LineItem(name = name, qty = qty, price = price))
                    continue
                }
            }

            val pC = Pattern.compile("^([A-Za-z0-9\\s\\-\\.,]+?)\\s+(?:₹|rs\\.?|inr)\\s*(\\d+(?:\\.\\d+)?)\\s*$", Pattern.CASE_INSENSITIVE)
            val mC = pC.matcher(line)
            if (mC.matches()) {
                val name = mC.group(1)?.trim() ?: ""
                val price = mC.group(2)?.toDoubleOrNull() ?: 0.0
                if (name.isNotBlank() && price > 0) {
                    items.add(LineItem(name = name, qty = 1.0, price = price))
                    continue
                }
            }
        }

        val finalItems = if (items.isNotEmpty()) items else listOf(LineItem(name = "Item 1", qty = 1.0, price = 0.0))
        val subtotal = finalItems.sumOf { it.total }
        if (amountPaid == 0.0 && paymentStatus == "PAID") {
            amountPaid = (subtotal - discount + shipping).coerceAtLeast(0.0)
        }

        return ParsedInvoiceData(
            customerName = customerName.ifBlank { "Customer" },
            customerPhone = customerPhone,
            customerAddress = customerAddress,
            items = finalItems,
            discount = discount,
            shipping = shipping,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            amountPaid = amountPaid
        )
    }

    fun checkCalculationDiscrepancy(text: String): CalculationDiscrepancy? {
        val qtyMatch = Pattern.compile("(?:qty|quantity)\\s*[:=]?\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE).matcher(text)
        val priceMatch = Pattern.compile("(?:price|rate|unit price)\\s*[:=]?\\s*(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE).matcher(text)
        val totalMatch = Pattern.compile("(?:total|entered amount|amount)\\s*[:=]?\\s*(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE).matcher(text)

        if (qtyMatch.find() && priceMatch.find() && totalMatch.find()) {
            val q = qtyMatch.group(1)?.toDoubleOrNull() ?: return null
            val p = priceMatch.group(1)?.toDoubleOrNull() ?: return null
            val actual = totalMatch.group(1)?.toDoubleOrNull() ?: return null
            val expected = q * p
            val diff = actual - expected
            return CalculationDiscrepancy(
                expected = expected,
                actual = actual,
                diff = diff,
                isMatch = Math.abs(diff) < 0.01
            )
        }
        return null
    }
}
