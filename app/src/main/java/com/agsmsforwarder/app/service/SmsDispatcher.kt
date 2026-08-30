package com.agsmsforwarder.app.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.agsmsforwarder.app.data.db.AppDatabase
import com.agsmsforwarder.app.data.model.SmsDeliveryStatus
import com.agsmsforwarder.app.receiver.SmsDeliveryReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmsDispatcher(private val context: Context) {

    private val db = AppDatabase.getInstance(context)

    suspend fun dispatchSms(
        logId: Long,
        destinationNumbers: String,
        messagePrefix: String,
        formattedContent: String
    ): SmsDeliveryStatus = withContext(Dispatchers.IO) {
        val rawNumbers = destinationNumbers
            .split(",", ";", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (rawNumbers.isEmpty()) {
            Log.w(TAG, "No valid destination phone number configured.")
            db.transactionLogDao().updateDeliveryStatus(logId, SmsDeliveryStatus.SKIPPED_NO_RECIPIENT)
            return@withContext SmsDeliveryStatus.SKIPPED_NO_RECIPIENT
        }

        val fullText = if (messagePrefix.isNotBlank()) {
            "$messagePrefix $formattedContent"
        } else {
            formattedContent
        }.trim()

        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        var overallStatus = SmsDeliveryStatus.PENDING

        for (phoneNumber in rawNumbers) {
            try {
                val parts = smsManager.divideMessage(fullText)
                val sentIntents = ArrayList<PendingIntent>()
                val deliveredIntents = ArrayList<PendingIntent>()

                for (i in parts.indices) {
                    // PendingIntent for Sent Status
                    val sentIntent = Intent(context, SmsDeliveryReceiver::class.java).apply {
                        action = SmsDeliveryReceiver.ACTION_SMS_SENT
                        putExtra(SmsDeliveryReceiver.EXTRA_LOG_ID, logId)
                        putExtra(SmsDeliveryReceiver.EXTRA_PART_INDEX, i)
                        putExtra(SmsDeliveryReceiver.EXTRA_TOTAL_PARTS, parts.size)
                    }
                    val sentPending = PendingIntent.getBroadcast(
                        context,
                        (logId * 100 + i).toInt(),
                        sentIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    sentIntents.add(sentPending)

                    // PendingIntent for Delivery Status
                    val deliveredIntent = Intent(context, SmsDeliveryReceiver::class.java).apply {
                        action = SmsDeliveryReceiver.ACTION_SMS_DELIVERED
                        putExtra(SmsDeliveryReceiver.EXTRA_LOG_ID, logId)
                        putExtra(SmsDeliveryReceiver.EXTRA_PART_INDEX, i)
                    }
                    val deliveredPending = PendingIntent.getBroadcast(
                        context,
                        (logId * 100 + i + 50).toInt(),
                        deliveredIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    deliveredIntents.add(deliveredPending)
                }

                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    sentIntents,
                    deliveredIntents
                )

                Log.i(TAG, "Dispatched SMS multipart (${parts.size} parts) to $phoneNumber for log #$logId")
                overallStatus = SmsDeliveryStatus.PENDING
            } catch (e: SecurityException) {
                Log.e(TAG, "Missing SEND_SMS permission when attempting to forward alert", e)
                overallStatus = SmsDeliveryStatus.FAILED_GENERIC
                db.transactionLogDao().updateDeliveryStatus(logId, overallStatus)
            } catch (e: Exception) {
                Log.e(TAG, "Exception during SMS dispatch to $phoneNumber", e)
                overallStatus = SmsDeliveryStatus.FAILED_GENERIC
                db.transactionLogDao().updateDeliveryStatus(logId, overallStatus)
            }
        }

        overallStatus
    }

    companion object {
        private const val TAG = "SmsDispatcher"
    }
}
