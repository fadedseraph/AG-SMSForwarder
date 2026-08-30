package com.agsmsforwarder.app.receiver

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import com.agsmsforwarder.app.data.db.AppDatabase
import com.agsmsforwarder.app.data.model.SmsDeliveryStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsDeliveryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val logId = intent.getLongExtra(EXTRA_LOG_ID, -1L)
        if (logId == -1L) return

        val pendingResult = goAsync()
        val db = AppDatabase.getInstance(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_SMS_SENT -> {
                        // In Android Telephony, Activity.RESULT_OK (-1) or 0 indicates success.
                        // Specific errors are positive integers (SmsManager.RESULT_ERROR_... > 0).
                        val status = when (resultCode) {
                            Activity.RESULT_OK, 0 -> SmsDeliveryStatus.SENT
                            SmsManager.RESULT_ERROR_NO_SERVICE -> SmsDeliveryStatus.FAILED_NO_SERVICE
                            SmsManager.RESULT_ERROR_RADIO_OFF -> SmsDeliveryStatus.FAILED_RADIO_OFF
                            SmsManager.RESULT_ERROR_NULL_PDU -> SmsDeliveryStatus.FAILED_NULL_PDU
                            else -> if (resultCode > 0) SmsDeliveryStatus.FAILED_GENERIC else SmsDeliveryStatus.SENT
                        }
                        Log.d(TAG, "SMS Sent Callback for Log #$logId: $status (Result Code: $resultCode)")
                        db.transactionLogDao().updateDeliveryStatus(logId, status)
                    }
                    ACTION_SMS_DELIVERED -> {
                        val status = when {
                            resultCode == Activity.RESULT_OK || resultCode == 0 -> SmsDeliveryStatus.DELIVERED
                            resultCode > 0 -> SmsDeliveryStatus.FAILED_GENERIC
                            else -> SmsDeliveryStatus.DELIVERED
                        }
                        Log.d(TAG, "SMS Delivered Callback for Log #$logId: $status (Result Code: $resultCode)")
                        db.transactionLogDao().updateDeliveryStatus(logId, status)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating delivery status for log #$logId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SmsDeliveryReceiver"
        const val ACTION_SMS_SENT = "com.agsmsforwarder.app.SMS_SENT"
        const val ACTION_SMS_DELIVERED = "com.agsmsforwarder.app.SMS_DELIVERED"
        const val EXTRA_LOG_ID = "extra_log_id"
        const val EXTRA_PART_INDEX = "extra_part_index"
        const val EXTRA_TOTAL_PARTS = "extra_total_parts"
    }
}
