package com.agsmsforwarder.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.agsmsforwarder.app.data.model.SmsDeliveryStatus

@Entity(tableName = "transaction_logs")
data class TransactionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val rawNotificationTitle: String,
    val rawNotificationText: String,
    val parsedResult: String,
    val isAiParsed: Boolean,
    val latencyMs: Long,
    val smsRecipient: String,
    val smsDeliveryStatus: SmsDeliveryStatus
)
