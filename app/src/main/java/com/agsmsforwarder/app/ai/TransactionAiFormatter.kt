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

            val parsed = parseLlmResponse(rawOutput, combinedInput, notificationTitle, latency)
            if (parsed != null) {
                return@withContext parsed
            } else {
                Log.w(TAG, "AI output could not be parsed into transaction. Falling back to Regex.")
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
     * Strict Gemma turn prompt with support for purchases, balance updates (e.g. Chime), and OTP skips.
     */
    private fun buildFewShotPrompt(systemPrompt: String, input: String): String {
        return buildString {
            append("<start_of_turn>user\n")
            append("You are a financial notification alert parser.\n")
            append("Rule 1: If the alert is a purchase/charge/debit, output ONLY in this format: '<Bank>: $<Amount> spent at <Merchant>'\n")
            append("Rule 2: If card digits are mentioned, append 'on card <Digits>'.\n")
            append("Rule 3: If the alert is an account balance update (e.g. money update, balance is), output: '<Bank>: Balance is $<Amount>'\n")
            append("Rule 4: If the alert is a verification code, OTP, security alert, login notification, or has NO money amount, you MUST output ONLY the single word: SKIP\n\n")
            append("Alert: Chase Mobile: Debit ending in 4102 charged \$42.50 at Trader Joe's.<end_of_turn>\n")
            append("<start_of_turn>model\n")
            append("Chase: \$42.50 spent at Trader Joe's on card 4102<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append("Alert: Chime: Here's your morning money update: your checking balance is \$245.80.<end_of_turn>\n")
            append("<start_of_turn>model\n")
            append("Chime: Balance is \$245.80<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append("Alert: Bank of America: Alert: \$120.00 debit card purchase at SHELL OIL 5744.<end_of_turn>\n")
            append("<start_of_turn>model\n")
            append("Bank of America: \$120.00 spent at Shell Oil on card 5744<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append("Alert: Wells Fargo: Your one-time verification code is 849201 for login.<end_of_turn>\n")
            append("<start_of_turn>model\n")
            append("SKIP<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append("Alert: American Express: Purchase authorized: \$89.99 at AMAZON.COM on card 4091.<end_of_turn>\n")
            append("<start_of_turn>model\n")
            append("American Express: \$89.99 spent at Amazon.com on card 4091<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append("Alert: $input<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }
    }

    /**
     * Parses the LLM response with anti-hallucination validation against the original input.
     */
    private fun parseLlmResponse(
        rawResponse: String,
        rawInput: String,
        defaultBankName: String,
        latencyMs: Long
    ): FormattedTransaction? {
        val cleanRaw = rawResponse.trim()
            .replace("<start_of_turn>model", "")
            .replace("<start_of_turn>", "")
            .replace("<end_of_turn>", "")
            .trim()

        if (cleanRaw.isBlank()) {
            return null
        }

        val lines = cleanRaw.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Explicit SKIP output
        if (lines.any { it.equals("SKIP", ignoreCase = true) || it.startsWith("SKIP", ignoreCase = true) }) {
            return FormattedTransaction.SKIP.copy(isAiGenerated = true, latencyMs = latencyMs)
        }

        // 2. Anti-Hallucination Check for Non-Transaction / OTP alerts without money amount
        val inputLower = rawInput.lowercase()
        val isNonTransactionPattern = inputLower.contains("code") ||
                inputLower.contains("otp") ||
                inputLower.contains("verification") ||
                inputLower.contains("login") ||
                inputLower.contains("password") ||
                inputLower.contains("security") ||
                inputLower.contains("temporary")

        val hasAmountInInput = rawInput.contains("$") || Regex("""\b[0-9]{1,5}\.[0-9]{2}\b""").containsMatchIn(rawInput)

        if (isNonTransactionPattern && !hasAmountInInput) {
            Log.d(TAG, "Input matched non-transaction pattern without amount. Outputting SKIP.")
            return FormattedTransaction.SKIP.copy(isAiGenerated = true, latencyMs = latencyMs)
        }

        // 3. Scan candidate lines for extracted transaction details
        for (line in lines) {
            if (line.startsWith("Sure,", ignoreCase = true) ||
                line.startsWith("Here is", ignoreCase = true) ||
                line.startsWith("Here's", ignoreCase = true) ||
                line.startsWith("Extracted:", ignoreCase = true) ||
                line.startsWith("Note:", ignoreCase = true)
            ) {
                continue
            }

            var candidate = line
                .replace("Output:", "", ignoreCase = true)
                .replace("`", "")
                .replace("[Bank]", defaultBankName.removeSuffix("Mobile").trim(), ignoreCase = true)
                .replace("[Merchant]", "", ignoreCase = true)
                .replace("[", "")
                .replace("]", "")
                .trim()

            // Check if candidate contains a currency amount
            val amountMatch = Regex("""\$([0-9]+(?:\.[0-9]{2})?)""").find(candidate)
            if (amountMatch != null) {
                val amt = amountMatch.groupValues[1]

                // Anti-Hallucination Guard: Ensure the extracted amount actually exists in the raw input notification!
                if (!rawInput.contains(amt)) {
                    Log.w(TAG, "Rejecting hallucinated amount $$amt not present in input: '$rawInput'")
                    continue
                }

                val bankCandidate = candidate.substringBefore(":").trim()
                val finalBank = if (bankCandidate.length in 2..30 &&
                    !bankCandidate.contains("Amount", ignoreCase = true) &&
                    !bankCandidate.contains("Bank", ignoreCase = true) &&
                    !bankCandidate.contains("Balance", ignoreCase = true)
                ) {
                    bankCandidate
                } else if (rawInput.contains("chime", ignoreCase = true)) {
                    "Chime"
                } else {
                    defaultBankName.removeSuffix("Mobile").removeSuffix("App").trim().ifBlank { "Bank Alert" }
                }

                val afterColon = candidate.substringAfter(":", "").trim().ifBlank { candidate }

                // Check if this is a balance update (e.g. Chime morning money update)
                val isBalanceUpdate = candidate.contains("balance is", ignoreCase = true) ||
                        candidate.contains("balance update", ignoreCase = true) ||
                        candidate.contains("balance:", ignoreCase = true) ||
                        rawInput.contains("money update", ignoreCase = true) ||
                        rawInput.contains("balance is", ignoreCase = true)

                if (isBalanceUpdate) {
                    val formatted = "$finalBank: Balance is \$$amt"
                    return FormattedTransaction(
                        bank = finalBank,
                        amount = amt,
                        merchant = "Account Balance",
                        isTransaction = true,
                        formattedMessage = formatted,
                        isAiGenerated = true,
                        latencyMs = latencyMs
                    )
                }

                val merchantCandidate = when {
                    afterColon.contains("spent at ", ignoreCase = true) -> afterColon.substringAfter("spent at ")
                    afterColon.contains("at ", ignoreCase = true) -> afterColon.substringAfter("at ")
                    afterColon.contains("to ", ignoreCase = true) -> afterColon.substringAfter("to ")
                    else -> "Merchant"
                }.substringBefore(" on card").trimEnd('.', ';', ',')

                val cardSuffix = if (afterColon.contains("on card", ignoreCase = true)) {
                    " on card " + afterColon.substringAfter("on card").trim().take(10)
                } else if (rawInput.contains("ending in ", ignoreCase = true)) {
                    val digits = Regex("""ending in (\d{2,6})""", RegexOption.IGNORE_CASE).find(rawInput)?.groupValues?.getOrNull(1)
                    if (digits != null) " on card $digits" else ""
                } else {
                    ""
                }

                val finalMerchant = cleanMerchant(merchantCandidate)
                val formatted = "$finalBank: \$$amt spent at $finalMerchant$cardSuffix".trim()

                return FormattedTransaction(
                    bank = finalBank,
                    amount = amt,
                    merchant = finalMerchant,
                    isTransaction = true,
                    formattedMessage = formatted,
                    isAiGenerated = true,
                    latencyMs = latencyMs
                )
            }
        }

        // If no amount in input, return SKIP
        if (!hasAmountInInput) {
            return FormattedTransaction.SKIP.copy(isAiGenerated = true, latencyMs = latencyMs)
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
