package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey
    val id: String = "rec_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
    val receiptNo: String = "REC-0001",
    val customerName: String,
    val invoiceNo: String = "",
    val amount: Double,
    val method: String = "UPI",
    val date: String,
    val reference: String = "",
    val businessName: String = "BillGen AI",
    val notes: String = "Payment received with thanks.",
    val createdAt: Long = System.currentTimeMillis()
)
