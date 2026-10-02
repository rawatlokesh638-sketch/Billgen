package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val id: String = "prod_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}",
    val name: String,
    val price: Double,
    val stock: Int = 0,
    val hsn: String = "",
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isOutOfStock: Boolean get() = stock <= 0
    val isLowStock: Boolean get() = stock in 1..5
}
