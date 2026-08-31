package com.agsmsforwarder.app.ai

import com.agsmsforwarder.app.data.model.FormattedTransaction
import java.util.regex.Pattern

object RegexFallbackExtractor {

    // Regex for matching currency amounts (e.g. $45.20, 45.20 USD, $1,250.00, $1,317.32, Rs. 500, €45.00, £30.00)
    private val AMOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:\$|USD\s*|CAD\s*|AUD\s*|EUR\s*|€|GBP\s*|£|INR\s*|₹)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{2})?|[0-9]+(?:\.[0-9]{2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{2})?|[0-9]+(?:\.[0-9]{2})?)\s*(?:USD|CAD|AUD|EUR|GBP|INR)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:amount|for|spent|paid|purchase of|debit of|charged|balance is|balance of)\s*(?:of)?\s*\$?([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{2})?|[0-9]+(?:\.[0-9]{2})?)""", Pattern.CASE_INSENSITIVE)
    )

    // Regex patterns for detecting merchant name after keywords like "at", "to", "from"
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("""(?:at|to|from)\s+([A-Za-z0-9\s\.\*\#\-\_&']{2,40}?)(?=\s+(?:on|with|using|available|card|ending|via|ref|bal|\.|\$|\n|$))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""purchase\s+(?:of\s+\$[0-9\.,]+\s+)?at\s+([A-Za-z0-9\s\.\*\#\-\_&']{2,40})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""charged\s+\$[0-9\.,]+\s+at\s+([A-Za-z0-9\s\.\*\#\-\_&']{2,40})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""paid\s+to\s+([A-Za-z0-9\s\.\*\#\-\_&']{2,40})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""merchant:\s*([A-Za-z0-9\s\.\*\#\-\_&']{2,40})""", Pattern.CASE_INSENSITIVE)
    )

    // Keywords indicating non-transaction alerts that should be skipped
    private val NON_TRANSACTION_KEYWORDS = listOf(
        "security code",
        "verification code",
        "otp",
        "one-time password",
        "temporary code",
        "login detected",
        "password reset",
        "statement available",
        "special offer",
        "reward points balance",
        "scheduled maintenance"
    )

    fun extract(
        packageName: String,
        notificationTitle: String,
        notificationText: String
    ): FormattedTransaction {
        // Strip card suffix (e.g. "ending in 4102") to avoid greedy false positive matches before amount/merchant
        val sanitizedText = notificationText.replace(Regex("""ending in \d+ charged""", RegexOption.IGNORE_CASE), "charged")
        val combinedText = "$notificationTitle $sanitizedText".trim()

        // 1. Check if non-transaction
        val lower = combinedText.lowercase()
        for (keyword in NON_TRANSACTION_KEYWORDS) {
            if (lower.contains(keyword)) {
                return FormattedTransaction.SKIP
            }
        }

        // 2. Extract Bank Name
        val bank = resolveBankName(packageName, notificationTitle, combinedText)

        // 3. Extract Amount
        var amount: String? = null
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(combinedText)
            if (matcher.find()) {
                val matched = matcher.group(1)?.trim()
                if (!matched.isNullOrBlank()) {
                    amount = matched
                    break
                }
            }
        }

        if (amount == null) {
            // Cannot find transaction amount -> Skip
            return FormattedTransaction.SKIP
        }

        val hasPurchaseIntent = lower.contains("spent") ||
                lower.contains("purchase") ||
                lower.contains("charged") ||
                lower.contains("debit") ||
                lower.contains("paid") ||
                lower.contains("authorized")

        // 4. Pure balance update alert (e.g. Chime morning money update)
        val isPureBalanceUpdate = !hasPurchaseIntent && (
                lower.contains("balance is") ||
                lower.contains("money update") ||
                lower.contains("checking balance") ||
                lower.contains("account balance")
        )

        if (isPureBalanceUpdate) {
            val formattedMsg = "$bank: Balance is $$amount"
            return FormattedTransaction(
                bank = bank,
                amount = amount,
                merchant = "Account Balance",
                isTransaction = true,
                formattedMessage = formattedMsg,
                isAiGenerated = false,
                latencyMs = 1L
            )
        }

        // 5. Spending transaction: Extract Merchant & optional balance suffix
        var merchant: String? = null
        for (pattern in MERCHANT_PATTERNS) {
            val matcher = pattern.matcher(combinedText)
            if (matcher.find()) {
                val candidate = matcher.group(1)?.trim()
                if (!candidate.isNullOrBlank() && candidate.length > 1 && !candidate.contains("charged", ignoreCase = true)) {
                    merchant = cleanMerchantName(candidate)
                    break
                }
            }
        }

        val balanceRegex = Regex("""(?:account\s+balance|checking\s+balance|new\s+balance|balance|bal)(?:\s+is)?\s*:?\s*\$?([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{2})?|[0-9]+(?:\.[0-9]{2})?)""", RegexOption.IGNORE_CASE)
        val balanceMatch = balanceRegex.find(combinedText)
        val detectedBalance = balanceMatch?.groupValues?.getOrNull(1)
        val balanceSuffix = if (detectedBalance != null && detectedBalance != amount) ". Balance: \$$detectedBalance" else ""

        val cleanMerchant = merchant ?: "Merchant"
        val formattedMsg = "$bank: \$$amount spent at $cleanMerchant$balanceSuffix"

        return FormattedTransaction(
            bank = bank,
            amount = amount,
            merchant = cleanMerchant,
            isTransaction = true,
            formattedMessage = formattedMsg,
            isAiGenerated = false,
            latencyMs = 1L
        )
    }

    private fun resolveBankName(packageName: String, title: String, combinedText: String): String {
        val packageLower = packageName.lowercase()
        val combinedLower = combinedText.lowercase()
        return when {
            packageLower.contains("chime") || combinedLower.contains("chime") -> "Chime"
            packageLower.contains("chase") || combinedLower.contains("chase") -> "Chase"
            packageLower.contains("bofa") || combinedLower.contains("bank of america") -> "Bank of America"
            packageLower.contains("wellsfargo") || combinedLower.contains("wells fargo") -> "Wells Fargo"
            packageLower.contains("citi") || combinedLower.contains("citibank") -> "Citi"
            packageLower.contains("capitalone") || combinedLower.contains("capital one") -> "Capital One"
            packageLower.contains("americanexpress") || combinedLower.contains("amex") -> "Amex"
            packageLower.contains("discover") || combinedLower.contains("discover") -> "Discover"
            packageLower.contains("usbank") || combinedLower.contains("u.s. bank") -> "U.S. Bank"
            packageLower.contains("pnc") || combinedLower.contains("pnc bank") -> "PNC"
            packageLower.contains("revolut") || combinedLower.contains("revolut") -> "Revolut"
            packageLower.contains("monzo") || combinedLower.contains("monzo") -> "Monzo"
            packageLower.contains("venmo") || combinedLower.contains("venmo") -> "Venmo"
            packageLower.contains("paypal") || combinedLower.contains("paypal") -> "PayPal"
            title.isNotBlank() && title.length <= 20 -> title.removeSuffix("Mobile").removeSuffix("App").trim()
            else -> "Bank"
        }
    }

    private fun cleanMerchantName(raw: String): String {
        var clean = raw.trim()
        clean = clean.replace(Regex("""[\.\,\:\;]+$"""), "")
        clean = clean.replace(Regex("""^[\.\,\:\;]+"""), "")

        // Title case if ALL CAPS
        if (clean.length > 2 && clean == clean.uppercase()) {
            clean = clean.lowercase().split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }

        return clean
    }
}
