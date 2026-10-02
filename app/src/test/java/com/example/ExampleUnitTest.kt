package com.example

import com.example.data.ai.LocalInvoiceParser
import com.example.data.model.LineItem
import com.example.util.FormatUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testInvoiceCalculations() {
        val items = listOf(
            LineItem(name = "T-Shirt", qty = 2.0, price = 500.0),
            LineItem(name = "Cap", qty = 1.0, price = 200.0)
        )
        // Subtotal = 1200
        val result = FormatUtils.calculateInvoice(
            items = items,
            discount = 100.0, // 1100
            shipping = 50.0,
            roundoff = 0.0,
            gstRate = 18.0,
            gstType = "intra",
            gstMode = "exclusive",
            amountPaid = 0.0
        )

        assertEquals(1200.0, result.subtotal, 0.01)
        assertEquals(1100.0, result.taxableAmount, 0.01)
        assertEquals(198.0, result.totalGst, 0.01) // 18% of 1100
        assertEquals(99.0, result.cgst, 0.01)
        assertEquals(99.0, result.sgst, 0.01)
        assertEquals(1348.0, result.total, 0.01) // 1100 + 198 + 50
        assertEquals("UNPAID", result.status)
    }

    @Test
    fun testNaturalOrderParser() {
        val message = "Amit Sharma, 2 T-shirt 599, 1 Jeans 999, discount 100, paid by UPI"
        val parsed = LocalInvoiceParser.parseNaturalOrder(message)

        assertEquals("Amit Sharma", parsed.customerName)
        assertEquals(2, parsed.items.size)
        assertEquals(100.0, parsed.discount, 0.01)
        assertEquals("PAID", parsed.paymentStatus)
    }

    @Test
    fun testDiscrepancyChecker() {
        val text = "Quantity = 3, Price = 500, Total = 1200"
        val discrepancy = LocalInvoiceParser.checkCalculationDiscrepancy(text)

        assertNotNull(discrepancy)
        assertEquals(1500.0, discrepancy!!.expected, 0.01)
        assertEquals(1200.0, discrepancy.actual, 0.01)
        assertFalse(discrepancy.isMatch)
    }
}
