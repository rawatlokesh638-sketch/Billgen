package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.QuotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuotationDao {
    @Query("SELECT * FROM quotations ORDER BY createdAt DESC")
    fun getAllQuotations(): Flow<List<QuotationEntity>>

    @Query("SELECT * FROM quotations WHERE id = :id LIMIT 1")
    suspend fun getQuotationById(id: String): QuotationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotation(quotation: QuotationEntity)

    @Update
    suspend fun updateQuotation(quotation: QuotationEntity)

    @Query("UPDATE quotations SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM quotations WHERE id = :id")
    suspend fun deleteQuotationById(id: String)
}
