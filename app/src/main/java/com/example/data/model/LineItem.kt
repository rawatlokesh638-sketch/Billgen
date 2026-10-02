package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LineItem(
    val name: String = "",
    val hsn: String = "",
    val qty: Double = 1.0,
    val price: Double = 0.0,
    val catalogId: String? = null
) {
    val total: Double
        get() = qty * price
}
