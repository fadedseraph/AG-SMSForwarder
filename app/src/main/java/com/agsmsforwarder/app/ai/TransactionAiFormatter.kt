package com.agsmsforwarder.app.ai

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.agsmsforwarder.app.data.model.FormattedTransaction
import com.agsmsforwarder.app.data.preferences.AppPreferences
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

sealed class ModelLoadState {
    object Uninitialized : ModelLoadState()
    object Loading : ModelLoadState()
    data class Loaded(val modelPath: String, val loadDurationMs: Long) : ModelLoadState()
    data class Error(val message: String) : ModelLoadState()
}

class TransactionAiFormatter private constructor(private val context: Context) {

    private val mutex = Mutex()
    private var llmInference: LlmInference? = null
    private var currentLoadedPath: String? = null

    private val _modelState = MutableStateFlow<ModelLoadState>(ModelLoadState.Uninitialized)
    val modelState: StateFlow<ModelLoadState> = _modelState.asStateFlow()

    private val _lastInferenceLatencyMs = MutableStateFlow(0L)
    val lastInferenceLatencyMs: StateFlow<Long> = _lastInferenceLatencyMs.asStateFlow()

    /**
     * Initializes or reloads the MediaPipe LlmInference model.
     */
    suspend fun initializeModel(
        modelPath: String,
        temperature: Float = 0.2f,
        topK: Int = 40,
        maxTokens: Int = 128
    ): Result<Unit> = withContext(Dispatchers.Default) {
        mutex.withLock {
            if (modelPath.isBlank()) {
                releaseModelInternal()
                _modelState.value = ModelLoadState.Uninitialized
                return@withContext Result.failure(IllegalArgumentException("Model path is empty"))
            }

            val file = File(modelPath)
            if (!file.exists() || !file.canRead()) {
                val err = "Model file does not exist or is not readable: $modelPath"
                Log.w(TAG, err)
                releaseModelInternal()
                _modelState.value = ModelLoadState.Error(err)
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            _modelState.value = ModelLoadState.Loading
            val startTime = SystemClock.elapsedRealtime()

            try {
                releaseModelInternal()
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(file.absolutePath)
                    .setMaxTokens(maxTokens)
                    .setTopK(topK)
                    .setTemperature(temperature)
                    .build()

                val inference = LlmInference.createFromOptions(context, options)
                llmInference = inference
                currentLoadedPath = modelPath
                val loadDuration = SystemClock.elapsedRealtime() - startTime
                _modelState.value = ModelLoadState.Loaded(modelPath, loadDuration)
                Log.i(TAG, "MediaPipe LLM loaded successfully from $modelPath in ${loadDuration}ms")
                Result.success(Unit)
            } catch (e: Throwable) {
                val errMsg = "Failed to initialize MediaPipe LLM: ${e.localizedMessage}"
                Log.e(TAG, errMsg, e)
                releaseModelInternal()
                _modelState.value = ModelLoadState.Error(errMsg)
                Result.failure(e)
            }
        }
    }

    /**
     * Formats notification using on-device LLM with fallback to Regex.
     */
    suspend fun formatNotification(
        packageName: String,
        notificationTitle: String,
        notificationText: String,
        systemPromptOverride: String? = null
    ): FormattedTransaction = withContext(Dispatchers.Default) {
        val combinedInput = if (notificationTitle.isNotBlank()) {
            "$notificationTitle: $notificationText"
        } else {
            notificationText
        }.trim()

        val inferenceInstance = mutex.withLock { llmInference }

        if (inferenceInstance == null) {
            Log.d(TAG, "LLM not initialized or unavailable. Using Regex fallback.")
            return@withContext RegexFallbackExtractor.extract(packageName, notificationTitle, notificationText)
        }

        val startTime = SystemClock.elapsedRealtime()
        try {
            val systemPrompt = systemPromptOverride?.ifBlank { null } ?: AppPreferences.DEFAULT_SYSTEM_PROMPT
            val prompt = buildFewShotPrompt(systemPrompt, combinedInput)

            val rawOutput = mutex.withLock {
                inferenceInstance.generateResponse(prompt)
            }

            val latency = SystemClock.elapsedRealtime() - startTime
            _lastInferenceLatencyMs.value = latency
            Log.d(TAG, "AI Inference raw output (in ${latency}ms): '$rawOutput'")

            val parsed = parseLlmResponse(rawOutput, latency)
            if (parsed != null) {
                return@withContext parsed
            } else {
                Log.w(TAG, "AI output did not match expected structure. Falling back to Regex.")
                val fallback = RegexFallbackExtractor.extract(packageName, notificationTitle, notificationText)
                return@withContext fallback.copy(latencyMs = latency)
            }
        } catch (e: Throwable) {
            val latency = SystemClock.elapsedRealtime() - startTime
            Log.e(TAG, "AI Inference threw exception after ${latency}ms. Falling back to Regex.", e)
            val fallback = RegexFallbackExtractor.extract(packageName, notificationTitle, notificationText)
            return@withContext fallback.copy(latencyMs = latency)
        }
    }

    private fun buildFewShotPrompt(systemPrompt: String, input: String): String {
        return buildString {
            append(systemPrompt)
            append("\n\n")
            append("Input: Chase: You spent \$45.20 at TRADER JOE'S #123 on 08/29.\n")
            append("Output: Chase: \$45.20 spent at Trader Joe's\n\n")
            append("Input: Bank of America: Alert: \$120.00 debit card purchase at SHELL OIL 5744.\n")
            append("Output: Bank of America: \$120.00 spent at Shell Oil\n\n")
            append("Input: Wells Fargo: Your one-time verification code is 849201.\n")
            append("Output: SKIP\n\n")
            append("Input: ").append(input).append("\n")
            append("Output:")
        }
    }

    private fun parseLlmResponse(rawResponse: String, latencyMs: Long): FormattedTransaction? {
        val clean = rawResponse.trim()
            .lines().firstOrNull()?.trim() ?: ""

        if (clean.isBlank() || clean.equals("SKIP", ignoreCase = true)) {
            return FormattedTransaction.SKIP.copy(latencyMs = latencyMs)
        }

        // Expected format: "[Bank]: $[Amount] spent at [Merchant]"
        val regex = Regex("""^([^:]+):\s*\$([0-9]+(?:\.[0-9]{2})?)\s+spent\s+at\s+(.+)$""", RegexOption.IGNORE_CASE)
        val match = regex.find(clean)
        return if (match != null) {
            val (bank, amount, merchant) = match.destructured
            FormattedTransaction(
                bank = bank.trim(),
                amount = amount.trim(),
                merchant = merchant.trim(),
                isTransaction = true,
                formattedMessage = clean,
                isAiGenerated = true,
                latencyMs = latencyMs
            )
        } else if (clean.contains("$") && (clean.contains("spent at", ignoreCase = true) || clean.contains("at", ignoreCase = true))) {
            // Generous parsing for slight deviations
            FormattedTransaction(
                bank = clean.substringBefore(":").trim(),
                amount = "",
                merchant = "",
                isTransaction = true,
                formattedMessage = clean,
                isAiGenerated = true,
                latencyMs = latencyMs
            )
        } else {
            null
        }
    }

    private fun releaseModelInternal() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing LlmInference instance", e)
        } finally {
            llmInference = null
            currentLoadedPath = null
        }
    }

    suspend fun close() = withContext(Dispatchers.Default) {
        mutex.withLock {
            releaseModelInternal()
            _modelState.value = ModelLoadState.Uninitialized
        }
    }

    companion object {
        private const val TAG = "TransactionAiFormatter"

        @Volatile
        private var INSTANCE: TransactionAiFormatter? = null

        fun getInstance(context: Context): TransactionAiFormatter {
            return INSTANCE ?: synchronized(this) {
                val instance = TransactionAiFormatter(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
