package com.agsmsforwarder.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.agsmsforwarder.app.data.model.SmsDeliveryStatus

class Converters {
    @TypeConverter
    fun fromSmsDeliveryStatus(status: SmsDeliveryStatus): String = status.name

    @TypeConverter
    fun toSmsDeliveryStatus(value: String): SmsDeliveryStatus = try {
        SmsDeliveryStatus.valueOf(value)
    } catch (e: Exception) {
        SmsDeliveryStatus.PENDING
    }
}

@Database(entities = [TransactionLogEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionLogDao(): TransactionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agsmsforwarder.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
