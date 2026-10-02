package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "quotations")
data class QuotationEntity(
    @PrimaryKey
    val id: String = "quo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
    val quoteNo: String = "QUO-2026-0001",
    val date: String,
    val customerName: String,
    val customerPhone: String = "",
    val customerAddress: String = "",
    val businessName: String = "",
    val itemsJson: String = "[]",
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val gstRate: Double = 0.0,
    val total: Double = 0.0,
    val status: String = "Draft", // Draft, Sent, Accepted, Converted
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
