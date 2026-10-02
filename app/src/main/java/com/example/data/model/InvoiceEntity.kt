package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "invoices")
data class InvoiceEntity(
    @PrimaryKey
    val id: String = "inv_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
    val invoiceNo: String = "INV-2026-0001",
    val date: String = "",
    val dueDate: String = "",
    val orderId: String = "",
    
    // Customer
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    
    // Business Profile
    val businessName: String = "BillGen AI Shop",
    val businessTagline: String = "Quality • Trust • Service",
    val businessAddress: String = "New Delhi, India",
    val businessPhone: String = "",
    val businessEmail: String = "",
    val businessWebsite: String = "",
    val businessGstin: String = "",
    val upiId: String = "",
    val bankDetails: String = "",
    val logoUri: String = "",
    
    // Items stored as JSON string
    val itemsJson: String = "[]",
    
    // Calculations
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val shipping: Double = 0.0,
    val roundoff: Double = 0.0,
    val gstRate: Double = 0.0,
    val gstType: String = "intra", // intra, inter, none
    val gstMode: String = "exclusive", // exclusive, inclusive, reverse
    val taxableAmount: Double = 0.0,
    val cgst: Double = 0.0,
    val sgst: Double = 0.0,
    val igst: Double = 0.0,
    val totalGst: Double = 0.0,
    val total: Double = 0.0,
    val amountPaid: Double = 0.0,
    
    // Status & Payment
    val paymentStatus: String = "UNPAID",
    val paymentMethod: String = "UPI",
    val notes: String = "Thank you for your business!",
    val terms: String = "Goods once sold cannot be returned unless defective.",
    
    // Design & Template
    val templateName: String = "modern",
    val themeColor: String = "orange",
    val currency: String = "INR",
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val pendingAmount: Double
        get() = (total - amountPaid).coerceAtLeast(0.0)

    val effectiveStatus: String
        get() {
            if (amountPaid >= total && total > 0.0) return "PAID"
            if (amountPaid > 0.0 && amountPaid < total) return "PARTIALLY PAID"
            if (paymentStatus == "OVERDUE") return "OVERDUE"
            return "UNPAID"
        }
}
