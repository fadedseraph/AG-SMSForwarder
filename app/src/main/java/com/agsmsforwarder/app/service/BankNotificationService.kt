package com.agsmsforwarder.app.service

import android.app.Notification
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.agsmsforwarder.app.ai.TransactionAiFormatter
import com.agsmsforwarder.app.data.db.AppDatabase
import com.agsmsforwarder.app.data.db.TransactionLogEntity
import com.agsmsforwarder.app.data.model.FormattedTransaction
import com.agsmsforwarder.app.data.model.SmsDeliveryStatus
import com.agsmsforwarder.app.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class BankNotificationService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var preferencesRepo: AppPreferencesRepository
    private lateinit var aiFormatter: TransactionAiFormatter
    private lateinit var smsDispatcher: SmsDispatcher
    private lateinit var database: AppDatabase

    // In-memory 60-second sliding cache for deduplication
    private val deduplicationCache = ConcurrentHashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        preferencesRepo = AppPreferencesRepository.getInstance(applicationContext)
        aiFormatter = TransactionAiFormatter.getInstance(applicationContext)
        smsDispatcher = SmsDispatcher(applicationContext)
        database = AppDatabase.getInstance(applicationContext)
        Log.i(TAG, "BankNotificationService initialized")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.i(TAG, "BankNotificationService destroyed")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Notification listener connected successfully")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification listener disconnected! Attempting immediate rebind...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, BankNotificationService::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        val notification = sbn.notification ?: return

        // Skip ongoing/foreground service notifications
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) {
            return
        }

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""

        if (title.isBlank() && text.isBlank()) {
            return
        }

        serviceScope.launch {
            processIncomingNotification(pkg, title, text)
        }
    }

    private suspend fun processIncomingNotification(
        packageName: String,
        title: String,
        text: String
    ) {
        val prefs = preferencesRepo.preferencesFlow.first()
        if (!prefs.isServiceEnabled) {
            Log.d(TAG, "Service is disabled in preferences. Skipping notification from $packageName.")
            return
        }

        if (!prefs.enabledPackages.contains(packageName)) {
            return
        }

        // 60-second sliding deduplication check
        val dedupeKey = "$packageName|$title|$text"
        val currentTime = System.currentTimeMillis()
        cleanDeduplicationCache(currentTime)

        val lastSeen = deduplicationCache[dedupeKey]
        if (lastSeen != null && (currentTime - lastSeen) < DEDUPLICATION_WINDOW_MS) {
            Log.d(TAG, "Duplicate notification detected within 60s from $packageName. Skipping.")
            return
        }
        deduplicationCache[dedupeKey] = currentTime

        val appName = getApplicationLabel(packageName)

        // Process with AI / Regex Formatter
        val formattedTx: FormattedTransaction = if (prefs.useAiFormatting) {
            aiFormatter.formatNotification(
                packageName = packageName,
                notificationTitle = title,
                notificationText = text,
                systemPromptOverride = prefs.customSystemPrompt
            )
        } else {
            // Raw or simple regex formatting without LLM
            val rawSummary = "$title: $text".trim()
            FormattedTransaction(
                bank = appName,
                amount = "",
                merchant = "",
                isTransaction = true,
                formattedMessage = rawSummary,
                isAiGenerated = false,
                latencyMs = 0L
            )
        }

        // Handle SKIP
        if (!formattedTx.isTransaction || formattedTx.formattedMessage.equals("SKIP", ignoreCase = true)) {
            Log.d(TAG, "Notification was classified as non-transaction / SKIP. Persisting log.")
            val logEntity = TransactionLogEntity(
                packageName = packageName,
                appName = appName,
                rawNotificationTitle = title,
                rawNotificationText = text,
                parsedResult = "SKIP",
                isAiParsed = formattedTx.isAiGenerated,
                latencyMs = formattedTx.latencyMs,
                smsRecipient = prefs.destinationPhoneNumber,
                smsDeliveryStatus = SmsDeliveryStatus.SKIPPED_NOT_TRANSACTION
            )
            database.transactionLogDao().insertLog(logEntity)
            return
        }

        // Save transaction to DB with PENDING status
        val initialLog = TransactionLogEntity(
            packageName = packageName,
            appName = appName,
            rawNotificationTitle = title,
            rawNotificationText = text,
            parsedResult = formattedTx.formattedMessage,
            isAiParsed = formattedTx.isAiGenerated,
            latencyMs = formattedTx.latencyMs,
            smsRecipient = prefs.destinationPhoneNumber,
            smsDeliveryStatus = SmsDeliveryStatus.PENDING
        )
        val logId = database.transactionLogDao().insertLog(initialLog)

        // Dispatch SMS
        if (prefs.destinationPhoneNumber.isNotBlank()) {
            smsDispatcher.dispatchSms(
                logId = logId,
                destinationNumbers = prefs.destinationPhoneNumber,
                messagePrefix = prefs.customSmsPrefix,
                formattedContent = formattedTx.formattedMessage
            )
        } else {
            Log.w(TAG, "No destination phone number configured. Marking as SKIPPED_NO_RECIPIENT.")
            database.transactionLogDao().updateDeliveryStatus(logId, SmsDeliveryStatus.SKIPPED_NO_RECIPIENT)
        }

        // Trim old logs to maintain database size
        database.transactionLogDao().trimOldLogs()
    }

    private fun cleanDeduplicationCache(currentTime: Long) {
        val iterator = deduplicationCache.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value > DEDUPLICATION_WINDOW_MS) {
                iterator.remove()
            }
        }
    }

    private fun getApplicationLabel(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    companion object {
        private const val TAG = "BankNotifService"
        private const val DEDUPLICATION_WINDOW_MS = 60_000L // 60 seconds
    }
}
