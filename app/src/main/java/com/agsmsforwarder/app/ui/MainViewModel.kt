package com.agsmsforwarder.app.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.agsmsforwarder.app.SmsForwarderApp
import com.agsmsforwarder.app.ai.ModelLoadState
import com.agsmsforwarder.app.ai.TransactionAiFormatter
import com.agsmsforwarder.app.data.db.AppDatabase
import com.agsmsforwarder.app.data.db.TransactionLogEntity
import com.agsmsforwarder.app.data.model.FormattedTransaction
import com.agsmsforwarder.app.data.model.SmsDeliveryStatus
import com.agsmsforwarder.app.data.preferences.AppPreferences
import com.agsmsforwarder.app.data.preferences.AppPreferencesRepository
import com.agsmsforwarder.app.service.SmsDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SmsForwarderApp
    private val preferencesRepo = app.preferencesRepository
    private val aiFormatter = app.aiFormatter
    private val database = app.database
    private val smsDispatcher = SmsDispatcher(application)

    val preferences: StateFlow<AppPreferences> = preferencesRepo.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppPreferences()
        )

    val logs: StateFlow<List<TransactionLogEntity>> = database.transactionLogDao()
        .getRecentLogs(100)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val modelLoadState: StateFlow<ModelLoadState> = aiFormatter.modelState
    val lastLatencyMs: StateFlow<Long> = aiFormatter.lastInferenceLatencyMs

    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    private val _testResult = MutableStateFlow<FormattedTransaction?>(null)
    val testResult: StateFlow<FormattedTransaction?> = _testResult.asStateFlow()

    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setServiceEnabled(enabled)
        }
    }

    fun setDestinationPhoneNumber(number: String) {
        viewModelScope.launch {
            preferencesRepo.setDestinationPhoneNumber(number)
        }
    }

    fun setUseAiFormatting(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setUseAiFormatting(enabled)
        }
    }

    fun setCustomSmsPrefix(prefix: String) {
        viewModelScope.launch {
            preferencesRepo.setCustomSmsPrefix(prefix)
        }
    }

    fun togglePackage(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.togglePackage(packageName, enabled)
        }
    }

    fun updateAiParameters(temperature: Float, topK: Int, maxTokens: Int, customPrompt: String) {
        viewModelScope.launch {
            preferencesRepo.updateAiParameters(temperature, topK, maxTokens, customPrompt)
            val currentPrefs = preferences.value
            if (currentPrefs.modelFilePath.isNotBlank()) {
                aiFormatter.initializeModel(
                    modelPath = currentPrefs.modelFilePath,
                    temperature = temperature,
                    topK = topK,
                    maxTokens = maxTokens
                )
            }
        }
    }

    /**
     * Imports a model file from a SAF Uri or path into internal storage.
     */
    fun importModelFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val contentResolver = context.contentResolver

                // Extract filename
                val fileName = uri.lastPathSegment?.substringAfterLast("/")?.replace(":", "_")
                    ?: "custom_model.task"
                val targetFile = File(context.filesDir, fileName)

                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val fullPath = targetFile.absolutePath
                preferencesRepo.setModelFilePath(fullPath)

                val prefs = preferences.value
                aiFormatter.initializeModel(
                    modelPath = fullPath,
                    temperature = prefs.aiTemperature,
                    topK = prefs.aiTopK,
                    maxTokens = prefs.aiMaxTokens
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadModelFromDirectPath(path: String) {
        viewModelScope.launch {
            preferencesRepo.setModelFilePath(path)
            val prefs = preferences.value
            aiFormatter.initializeModel(
                modelPath = path,
                temperature = prefs.aiTemperature,
                topK = prefs.aiTopK,
                maxTokens = prefs.aiMaxTokens
            )
        }
    }

    fun runTestInference(title: String, text: String) {
        viewModelScope.launch {
            _isTesting.value = true
            try {
                val prefs = preferences.value
                val result = if (prefs.useAiFormatting) {
                    aiFormatter.formatNotification(
                        packageName = "com.test.bank",
                        notificationTitle = title,
                        notificationText = text,
                        systemPromptOverride = prefs.customSystemPrompt
                    )
                } else {
                    val rawSummary = "$title: $text".trim()
                    FormattedTransaction(
                        bank = title,
                        amount = "",
                        merchant = "",
                        isTransaction = true,
                        formattedMessage = rawSummary,
                        isAiGenerated = false,
                        latencyMs = 0L
                    )
                }
                _testResult.value = result
            } finally {
                _isTesting.value = false
            }
        }
    }

    fun sendTestSms(message: String) {
        viewModelScope.launch {
            val prefs = preferences.value
            if (prefs.destinationPhoneNumber.isBlank()) return@launch

            val log = TransactionLogEntity(
                packageName = "com.test.bank",
                appName = "Test Bank",
                rawNotificationTitle = "Live Test",
                rawNotificationText = message,
                parsedResult = message,
                isAiParsed = _testResult.value?.isAiGenerated ?: false,
                latencyMs = _testResult.value?.latencyMs ?: 0L,
                smsRecipient = prefs.destinationPhoneNumber,
                smsDeliveryStatus = SmsDeliveryStatus.PENDING
            )
            val logId = database.transactionLogDao().insertLog(log)

            smsDispatcher.dispatchSms(
                logId = logId,
                destinationNumbers = prefs.destinationPhoneNumber,
                messagePrefix = prefs.customSmsPrefix,
                formattedContent = message
            )
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            database.transactionLogDao().clearAllLogs()
        }
    }

    companion object {
        fun isNotificationServiceEnabled(context: Context): Boolean {
            val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            return enabledListeners.contains(context.packageName)
        }

        fun isIgnoringBatteryOptimizations(context: Context): Boolean {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        }
    }
}
