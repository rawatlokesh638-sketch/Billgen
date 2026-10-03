package com.example.data.local

import androidx.room.*
import com.example.data.model.SubscriptionRequest
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscription_requests ORDER BY requestedAt DESC")
    fun getAllRequests(): Flow<List<SubscriptionRequest>>

    @Query("SELECT * FROM subscription_requests WHERE userId = :userId ORDER BY requestedAt DESC")
    fun getRequestsByUser(userId: String): Flow<List<SubscriptionRequest>>

    @Query("SELECT * FROM subscription_requests WHERE status = 'APPROVED' AND userId = :userId AND expiresAt > :now ORDER BY expiresAt DESC LIMIT 1")
    suspend fun getActiveApprovedSubscription(userId: String, now: Long = System.currentTimeMillis()): SubscriptionRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: SubscriptionRequest)

    @Query("UPDATE subscription_requests SET status = :status, adminNote = :note, approvedAt = :approvedAt, expiresAt = :expiresAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, note: String, approvedAt: Long, expiresAt: Long)

    @Query("DELETE FROM subscription_requests WHERE id = :id")
    suspend fun deleteById(id: String)
}
