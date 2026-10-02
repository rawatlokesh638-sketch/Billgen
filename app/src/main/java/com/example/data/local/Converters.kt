package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.LineItem
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val lineItemListType = Types.newParameterizedType(List::class.java, LineItem::class.java)
    private val lineItemAdapter = moshi.adapter<List<LineItem>>(lineItemListType)

    @TypeConverter
    fun fromLineItemList(items: List<LineItem>?): String {
        if (items.isNullOrEmpty()) return "[]"
        return lineItemAdapter.toJson(items)
    }

    @TypeConverter
    fun toLineItemList(json: String?): List<LineItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            lineItemAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
