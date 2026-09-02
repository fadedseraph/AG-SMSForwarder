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

                val effectiveMaxTokens = maxOf(maxTokens, 1024)
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

            val parsed = parseLlmResponse(rawOutput, combinedInput, packageName, notificationTitle, latency)
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
     * Strict Gemma turn prompt with comprehensive few-shot demonstrations for purchases, spending with balance, and pure balance updates.
     */
    private fun buildFewShotPrompt(systemPrompt: String, input: String): String {
        return buildString {
            append("<start_of_turn>user\n")
            append("You are a financial notification alert parser.\n")
            append("Rule 1: If the alert is a purchase/charge, output: '<Bank>: $<Amount> spent at <Merchant>'\n")
            append("Rule 2: If the alert is a deposit or money received, output: '<Bank>: $<Amount> received from <Sender>'\n")
            append("Rule 3: If card digits are mentioned, append 'on card <Digits>'.\n")
            append("Rule 4: If an updated account balance is included in the purchase alert, append '. Balance: $<Balance>'.\n")
            append("Rule 5: If the alert is purely an account balance update (no purchase), output: '<Bank>: Balance is $<Amount>'\n")
            append("Rule 6: If the alert is a verification code, OTP, security alert, login, promo, or has NO money amount, output ONLY: SKIP\n\n")
            append("Alert: Chase Mobile: Debit ending in 4102 charged \$42.50 at Trader Joe's.<end_of_turn>\n")
            append("<start_of_turn>model\n")
            append("Chase: \$42.50 spent at Trader Joe's on card 4102<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append("Alert: You spent \$56.14: Your new Chime account balance is \$1,317.32 after your purchase at Walmart.<end_of_turn>\n")
            append("<start_of_turn>model\n")
            append("Chime: \$56.14 spent at Walmart. Balance: \$1,317.32<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append("Alert: Direct deposit of \$1,000.00 from Employer XYZ is now available.<end_of_turn>\n")
            append("<start_of_turn>model\n")
            append("Chime: \$1,000.00 received from Employer XYZ<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append("Alert: Chime: Here's your morning money update: your checking balance is \$245.80.<end_of_turn>\n")
            append("<start_of_turn>model\n")
            append("Chime: Balance is \$245.80<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append("Alert: Wells Fargo: Your one-time verification code is 849201 for login.<end_of_turn>\n")
            append("<start_of_turn>model\n")
            append("SKIP<end_of_turn>\n")
            append("<start_of_turn>user\n")
            append("Alert: $input<end_of_turn>\n")
            append("<start_of_turn>model\n")
        }
    }

    /**
     * Parses the LLM response with robust anti-hallucination validation and comma-aware currency regexes.
     */
    private fun parseLlmResponse(
        rawResponse: String,
        rawInput: String,
        packageName: String,
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
                inputLower.contains("temporary") ||
                (inputLower.contains("cash back") && !rawInput.contains("$"))

        val hasAmountInInput = rawInput.contains("$") || Regex("""\b[0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{2})?\b""").containsMatchIn(rawInput)

        if (isNonTransactionPattern && !hasAmountInInput) {
            Log.d(TAG, "Input matched non-transaction pattern without amount. Outputting SKIP.")
            return FormattedTransaction.SKIP.copy(isAiGenerated = true, latencyMs = latencyMs)
        }

        // Robust currency regex supporting commas (e.g. $1,317.32 or $56.14)
        val balanceRegex = Regex("""(?:account\s+balance|checking\s+balance|new\s+balance|balance|bal)(?:\s+is)?\s*:?\s*\$?([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{2})?|[0-9]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE)
        val balanceMatchInInput = balanceRegex.find(rawInput)

        val hasPurchaseIntent = rawInput.contains("spent", ignoreCase = true) ||
                rawInput.contains("purchase", ignoreCase = true) ||
                rawInput.contains("charged", ignoreCase = true) ||
                rawInput.contains("debit", ignoreCase = true) ||
                rawInput.contains("paid", ignoreCase = true) ||
                rawInput.contains("authorized", ignoreCase = true)

        val hasDepositIntent = rawInput.contains("deposit", ignoreCase = true) ||
                rawInput.contains("received", ignoreCase = true) ||
                rawInput.contains("refund", ignoreCase = true) ||
                rawInput.contains("sent you", ignoreCase = true) ||
                rawInput.contains("transfer from", ignoreCase = true) ||
                rawInput.contains("hit your account", ignoreCase = true)

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

            // Currency match with comma support: $1,317.32 or $56.14
            val amountRegex = Regex("""\$([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{2})?|[0-9]+(?:\.[0-9]{2})?)""")
            val amountMatch = amountRegex.find(candidate) ?: amountRegex.find(rawInput)

            if (amountMatch != null) {
                val amt = amountMatch.groupValues[1]

                // Anti-Hallucination Guard: Ensure the extracted amount actually exists in the raw input notification!
                if (!rawInput.contains(amt) && !rawInput.contains(amt.replace(",", ""))) {
                    Log.w(TAG, "Rejecting hallucinated amount $$amt not present in input: '$rawInput'")
                    continue
                }

                val bankCandidate = candidate.substringBefore(":").trim()
                val finalBank = resolveBankName(packageName, defaultBankName, rawInput, bankCandidate)
                val afterColon = candidate.substringAfter(":", "").trim().ifBlank { candidate }

                // Check if this is purely a balance update
                val isPureBalanceUpdate = !hasPurchaseIntent && (
                        candidate.contains("balance is", ignoreCase = true) ||
                        candidate.contains("balance update", ignoreCase = true) ||
                        rawInput.contains("money update", ignoreCase = true) ||
                        rawInput.contains("balance is", ignoreCase = true)
                )

                if (isPureBalanceUpdate) {
                    val finalAmt = balanceMatchInInput?.groupValues?.getOrNull(1) ?: amt
                    val formatted = "$finalBank: Balance is \$$finalAmt"
                    return FormattedTransaction(
                        bank = finalBank,
                        amount = finalAmt,
                        merchant = "Account Balance",
                        isTransaction = true,
                        formattedMessage = formatted,
                        isAiGenerated = true,
                        latencyMs = latencyMs
                    )
                }

                // Otherwise, spending transaction
                val balanceCandidateMatch = balanceRegex.find(candidate)
                val detectedBalance = balanceCandidateMatch?.groupValues?.getOrNull(1)
                    ?: balanceMatchInInput?.groupValues?.getOrNull(1)

                val merchantCandidate = when {
                    afterColon.contains("received from ", ignoreCase = true) -> afterColon.substringAfter("received from ")
                    afterColon.contains("spent at ", ignoreCase = true) -> afterColon.substringAfter("spent at ")
                    afterColon.contains("purchase at ", ignoreCase = true) -> afterColon.substringAfter("purchase at ")
                    afterColon.contains("at ", ignoreCase = true) -> afterColon.substringAfter("at ")
                    afterColon.contains("to ", ignoreCase = true) -> afterColon.substringAfter("to ")
                    rawInput.contains("purchase at ", ignoreCase = true) -> rawInput.substringAfter("purchase at ")
                    rawInput.contains("at ", ignoreCase = true) -> rawInput.substringAfter("at ")
                    else -> "Merchant"
                }
                    .substringBefore(" on card")
                    .substringBefore(". Balance")
                    .substringBefore(" Balance:")
                    .substringBefore(" after your purchase")
                    .trimEnd('.', ';', ',', '!')

                val cardSuffix = if (afterColon.contains("on card", ignoreCase = true)) {
                    " on card " + afterColon.substringAfter("on card").substringBefore(". Balance").substringBefore(" Balance:").trim().take(10)
                } else if (rawInput.contains("ending in ", ignoreCase = true)) {
                    val digits = Regex("""ending in (\d{2,6})""", RegexOption.IGNORE_CASE).find(rawInput)?.groupValues?.getOrNull(1)
                    if (digits != null) " on card $digits" else ""
                } else {
                    ""
                }

                val balanceSuffix = if (detectedBalance != null && detectedBalance != amt && (rawInput.contains(detectedBalance) || rawInput.contains(detectedBalance.replace(",", "")))) {
                    ". Balance: \$$detectedBalance"
                } else {
                    ""
                }

                val finalMerchant = cleanMerchant(merchantCandidate)
                val formatted = if (hasDepositIntent) {
                    "$finalBank: \$$amt received from $finalMerchant$balanceSuffix".trim()
                } else {
                    "$finalBank: \$$amt spent at $finalMerchant$cardSuffix$balanceSuffix".trim()
                }

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

    private fun resolveBankName(packageName: String, defaultBankName: String, rawInput: String, candidateBank: String?): String {
        val inputLower = rawInput.lowercase()
        val packageLower = packageName.lowercase()
        return when {
            packageLower.contains("chime") || inputLower.contains("chime") -> "Chime"
            packageLower.contains("chase") || inputLower.contains("chase") -> "Chase"
            packageLower.contains("bofa") || inputLower.contains("bank of america") -> "Bank of America"
            packageLower.contains("wellsfargo") || inputLower.contains("wells fargo") -> "Wells Fargo"
            packageLower.contains("citi") || inputLower.contains("citibank") -> "Citi"
            packageLower.contains("capitalone") || inputLower.contains("capital one") -> "Capital One"
            packageLower.contains("americanexpress") || inputLower.contains("amex") -> "Amex"
            packageLower.contains("discover") || inputLower.contains("discover") -> "Discover"
            packageLower.contains("usbank") || inputLower.contains("u.s. bank") -> "U.S. Bank"
            packageLower.contains("pnc") || inputLower.contains("pnc bank") -> "PNC"
            packageLower.contains("revolut") || inputLower.contains("revolut") -> "Revolut"
            packageLower.contains("monzo") || inputLower.contains("monzo") -> "Monzo"
            packageLower.contains("venmo") || inputLower.contains("venmo") -> "Venmo"
            packageLower.contains("paypal") || inputLower.contains("paypal") -> "PayPal"
            !candidateBank.isNullOrBlank() && candidateBank.length in 2..30 &&
                    !candidateBank.contains("Amount", ignoreCase = true) &&
                    !candidateBank.contains("Bank", ignoreCase = true) &&
                    !candidateBank.contains("Balance", ignoreCase = true) -> candidateBank
            defaultBankName.isNotBlank() && defaultBankName.length <= 25 -> defaultBankName.removeSuffix("Mobile").removeSuffix("App").trim()
            else -> "Bank Alert"
        }
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
