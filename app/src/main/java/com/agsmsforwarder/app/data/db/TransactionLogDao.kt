package com.agsmsforwarder.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.agsmsforwarder.app.data.model.SmsDeliveryStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TransactionLogEntity): Long

    @Update
    suspend fun updateLog(log: TransactionLogEntity)

    @Query("UPDATE transaction_logs SET smsDeliveryStatus = :status WHERE id = :logId")
    suspend fun updateDeliveryStatus(logId: Long, status: SmsDeliveryStatus)

    @Query("SELECT * FROM transaction_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<TransactionLogEntity>>

    @Query("SELECT * FROM transaction_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: Long): TransactionLogEntity?

    @Query("DELETE FROM transaction_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM transaction_logs WHERE id NOT IN (SELECT id FROM transaction_logs ORDER BY timestamp DESC LIMIT 100)")
    suspend fun trimOldLogs()
}
