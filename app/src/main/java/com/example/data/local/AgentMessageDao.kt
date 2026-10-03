package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.AgentMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgentMessageDao {
    @Query("SELECT * FROM agent_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<AgentMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AgentMessageEntity): Long

    @Query("DELETE FROM agent_messages")
    suspend fun clearHistory()
}
