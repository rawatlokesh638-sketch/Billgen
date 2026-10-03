package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.AgentMessageEntity
import com.example.data.model.InvoiceEntity
import com.example.data.model.ProductEntity
import com.example.data.model.QuotationEntity
import com.example.data.model.ReceiptEntity
import com.example.data.model.SubscriptionRequest

@Database(
    entities = [
        InvoiceEntity::class,
        ProductEntity::class,
        ReceiptEntity::class,
        QuotationEntity::class,
        SubscriptionRequest::class,
        AgentMessageEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun productDao(): ProductDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun quotationDao(): QuotationDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun agentMessageDao(): AgentMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "billgen_ai_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
