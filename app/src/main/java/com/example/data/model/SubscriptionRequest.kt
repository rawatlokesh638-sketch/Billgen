package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription_requests")
data class SubscriptionRequest(
    @PrimaryKey val id: String = "sub_${System.currentTimeMillis()}",
    val userId: String,
    val userEmail: String,
    val planType: String, // "PRO", "PRO_PLUS", "PREMIUM"
    val billingCycle: String, // "MONTHLY", "YEARLY"
    val amount: Double,
    val utrNumber: String,
    val paymentPhone: String = "9050884894",
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val adminNote: String = "",
    val requestedAt: Long = System.currentTimeMillis(),
    val approvedAt: Long = 0L,
    val expiresAt: Long = 0L
)
