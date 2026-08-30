package com.agsmsforwarder.app.receiver

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.util.Log
import com.agsmsforwarder.app.service.BankNotificationService

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "Received broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    NotificationListenerService.requestRebind(
                        ComponentName(context, BankNotificationService::class.java)
                    )
                    Log.i(TAG, "Successfully requested rebind for BankNotificationService on boot")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to request rebind on boot", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }
}
