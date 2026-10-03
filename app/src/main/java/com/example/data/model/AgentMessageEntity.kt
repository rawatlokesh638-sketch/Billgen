package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "agent_messages")
data class AgentMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "user" or "agent"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actionType: String? = null, // "INVOICE_CREATED", "UDHAAR_ADDED", "PRODUCT_ADDED", null
    val actionData: String? = null
)
