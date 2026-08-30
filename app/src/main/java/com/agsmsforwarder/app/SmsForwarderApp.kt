package com.agsmsforwarder.app

import android.app.Application
import android.util.Log
import com.agsmsforwarder.app.ai.TransactionAiFormatter
import com.agsmsforwarder.app.data.db.AppDatabase
import com.agsmsforwarder.app.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsForwarderApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: AppPreferencesRepository
        private set

    lateinit var aiFormatter: TransactionAiFormatter
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        preferencesRepository = AppPreferencesRepository.getInstance(this)
        aiFormatter = TransactionAiFormatter.getInstance(this)

        // Preload configured AI model in background if path exists
        applicationScope.launch {
            try {
                val prefs = preferencesRepository.preferencesFlow.first()
                if (prefs.modelFilePath.isNotBlank() && prefs.useAiFormatting) {
                    Log.i(TAG, "Attempting to auto-load AI model from ${prefs.modelFilePath}")
                    aiFormatter.initializeModel(
                        modelPath = prefs.modelFilePath,
                        temperature = prefs.aiTemperature,
                        topK = prefs.aiTopK,
                        maxTokens = prefs.aiMaxTokens
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Initial model auto-load failed: ${e.message}")
            }
        }
    }

    companion object {
        private const val TAG = "SmsForwarderApp"
        lateinit var instance: SmsForwarderApp
            private set
    }
}
