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
        maxTokens: Int = 512
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

                // MediaPipe maxTokens specifies TOTAL sequence length (prompt tokens + output tokens).
                // Must be at least 512 to prevent native C++ buffer asserts / SIGABRT crashes.
                val effectiveMaxTokens = maxOf(maxTokens, 512)
                val effectiveTopK = if (topK in 1..100) topK else 40
                val effectiveTemperature = if (temperature in 0f..2f) temperature else 0.2f

                Log.i(TAG, "Configuring LlmInference with model=$modelPath, maxTokens=$effectiveMaxTokens, topK=$effectiveTopK, temp=$effectiveTemperature")

                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(file.absolutePath)
                    .setMaxTokens(effectiveMaxTokens)
                    .setTopK(effectiveTopK)
                    .setTemperature(effectiveTemperature)
                    .build()

                val inference = LlmInference.createFromOptions(context, options)
                llmInference = inference
                currentLoadedPath = modelPath
                val loadDuration = SystemClock.elapsedRealtime() - startTime
                _modelState.value = ModelLoadState.Loaded(modelPath, loadDuration)
                Log.i(TAG, "MediaPipe LLM loaded successfully from $modelPath in ${loadDuration}ms")
                Result.success(Unit)
            } catch (e: Throwable) {
                val errMsg = "Failed to initialize MediaPipe LLM: ${e.localizedMessage ?: e.javaClass.simpleName}"
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

            Log.d(TAG, "Submitting prompt to MediaPipe LlmInference:\n$prompt")

            val rawOutput = mutex.withLock {
                inferenceInstance.generateResponse(prompt)
            }

            val latency = SystemClock.elapsedRealtime() - startTime
            _lastInferenceLatencyMs.value = latency
            Log.d(TAG, "AI Inference raw output (in ${latency}ms): '$rawOutput'")

            val parsed = parseLlmResponse(rawOutput, notificationTitle, latency)
            if (parsed != null) {
                return@withContext parsed
            } else {
                Log.w(TAG, "AI output could not be formatted. Falling back to Regex.")
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

    /**
     * Uses strict Gemma multi-turn few-shot formatting to eliminate conversational filler.
     */
    private fun buildFewShotPrompt(systemPrompt: String, input: String): String {
        return buildString {
            append("<start_of_turn>user\n")
            append("Instruction: $systemPrompt\n\n")
            append("Alert: Chase Mobile: Debit ending in 4102 charged \$42.50 at Trader Joe's.<end_of_turn>\n")
            append("<start_of_turn>model\n")
            append("Chase: \$42.50 spent at Trader Joe's<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append("Alert: Bank of America: Alert: \$120.00 debit card purchase at SHELL OIL 5744.<end_of_turn>\n")
            append("<start_of_turn>model\n")
            append("Bank of America: \$120.00 spent at Shell Oil<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append("Alert: Wells Fargo: Your OTP code is 849201 for login.<end_of_turn>\n")
            append("<start_of_turn>model\n")
            append("SKIP<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append("Alert: $input<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }
    }

    /**
     * Parses the LLM output robustly, extracting transaction details even if the model
     * adds conversational preamble or formatting tags.
     */
    private fun parseLlmResponse(rawResponse: String, defaultBankName: String, latencyMs: Long): FormattedTransaction? {
        val cleanRaw = rawResponse.trim()
            .replace("<start_of_turn>model", "")
            .replace("<start_of_turn>", "")
            .replace("<end_of_turn>", "")
            .trim()

        if (cleanRaw.isBlank()) {
            return null
        }

        val lines = cleanRaw.lines().map { it.trim() }.filter { it.isNotBlank() }

        // Check for immediate SKIP
        if (lines.any { it.equals("SKIP", ignoreCase = true) || it.startsWith("SKIP", ignoreCase = true) }) {
            return FormattedTransaction.SKIP.copy(isAiGenerated = true, latencyMs = latencyMs)
        }

        // Find candidate line that contains transaction details
        for (line in lines) {
            // Ignore conversational preambles
            if (line.startsWith("Sure,", ignoreCase = true) ||
                line.startsWith("Here is", ignoreCase = true) ||
                line.startsWith("Here's", ignoreCase = true) ||
                line.startsWith("Extracted:", ignoreCase = true) ||
                line.startsWith("Output:", ignoreCase = true) && !line.contains("$")
            ) {
                continue
            }

            var candidate = line
                .replace("Output:", "", ignoreCase = true)
                .replace("`", "")
                .trim()

            // Resolve placeholder brackets like "[Bank]" or "[Chase Mobile]"
            val resolvedBank = if (candidate.contains("[Bank]", ignoreCase = true)) {
                val cleanBank = defaultBankName.removeSuffix("Mobile").removeSuffix("App").trim().ifBlank { "Bank Alert" }
                candidate = candidate.replace("[Bank]", cleanBank, ignoreCase = true)
                cleanBank
            } else {
                candidate.substringBefore(":").replace("[", "").replace("]", "").trim()
            }

            candidate = candidate.replace("[", "").replace("]", "")

            // Regex 1: "Bank: $42.50 spent at Trader Joe's"
            val standardMatch = Regex("""^([^:]+):\s*\$([0-9]+(?:\.[0-9]{2})?)\s+spent\s+at\s+(.+)$""", RegexOption.IGNORE_CASE).find(candidate)
            if (standardMatch != null) {
                val (b, amt, merch) = standardMatch.destructured
                return FormattedTransaction(
                    bank = b.trim(),
                    amount = amt.trim(),
                    merchant = cleanMerchant(merch.trim()),
                    isTransaction = true,
                    formattedMessage = "${b.trim()}: \$${amt.trim()} spent at ${cleanMerchant(merch.trim())}",
                    isAiGenerated = true,
                    latencyMs = latencyMs
                )
            }

            // Regex 2: Flexible extraction if line contains currency amount ($XX.XX)
            val amountMatch = Regex("""\$([0-9]+(?:\.[0-9]{2})?)""").find(candidate)
            if (amountMatch != null) {
                val amt = amountMatch.groupValues[1]
                val merchantPart = when {
                    candidate.contains("spent at ", ignoreCase = true) -> candidate.substringAfter("spent at ")
                    candidate.contains("at ", ignoreCase = true) -> candidate.substringAfter("at ")
                    candidate.contains("to ", ignoreCase = true) -> candidate.substringAfter("to ")
                    else -> "Merchant"
                }.substringBefore(" on card").substringBefore(".").trim()

                val finalBank = if (resolvedBank.length in 2..30) resolvedBank else defaultBankName.ifBlank { "Bank Alert" }
                val finalMerchant = cleanMerchant(merchantPart)

                return FormattedTransaction(
                    bank = finalBank,
                    amount = amt,
                    merchant = finalMerchant,
                    isTransaction = true,
                    formattedMessage = "$finalBank: \$$amt spent at $finalMerchant",
                    isAiGenerated = true,
                    latencyMs = latencyMs
                )
            }
        }

        return null
    }

    private fun cleanMerchant(raw: String): String {
        return raw.trim()
            .removePrefix("at ")
            .removePrefix("AT ")
            .trimEnd('.', ';', ',')
    }

    private fun releaseModelInternal() {
        try {
            llmInference?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing previous LlmInference instance: ${e.message}")
        } finally {
            llmInference = null
            currentLoadedPath = null
        }
    }

    suspend fun release() {
        mutex.withLock {
            releaseModelInternal()
            _modelState.value = ModelLoadState.Uninitialized
        }
    }

    companion object {
        private const val TAG = "TransactionAiFormatter"

        @Volatile
        private var instance: TransactionAiFormatter? = null

        fun getInstance(context: Context): TransactionAiFormatter {
            return instance ?: synchronized(this) {
                instance ?: TransactionAiFormatter(context.applicationContext).also { instance = it }
            }
        }
    }
}
