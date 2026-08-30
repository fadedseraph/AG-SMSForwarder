package com.agsmsforwarder.app.ai

import com.agsmsforwarder.app.data.model.FormattedTransaction
import java.util.regex.Pattern

object RegexFallbackExtractor {

    // Regex for matching currency amounts (e.g. $45.20, 45.20 USD, $1,250.00, Rs. 500, €45.00, £30.00)
    private val AMOUNT_PATTERNS = listOf(
        Pattern.compile("""(?:\$|USD\s*|CAD\s*|AUD\s*|EUR\s*|€|GBP\s*|£|INR\s*|₹)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{2})?)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{2})?)\s*(?:USD|CAD|AUD|EUR|GBP|INR)""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""(?:amount|for|spent|paid|purchase of|debit of|charged)\s*(?:of)?\s*\$?([0-9]+(?:\.[0-9]{2})?)""", Pattern.CASE_INSENSITIVE)
    )

    // Regex patterns for detecting merchant name after keywords like "at", "to", "from"
    private val MERCHANT_PATTERNS = listOf(
        Pattern.compile("""(?:at|to|from)\s+([A-Za-z0-9\s\.\*\#\-\_&']{2,40}?)(?=\s+(?:on|with|using|available|card|ending|via|ref|bal|\.|\$|\n|$))""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""purchase\s+(?:of\s+\$[0-9\.]+\s+)?at\s+([A-Za-z0-9\s\.\*\#\-\_&']{2,40})""", Pattern.CASE_INSENSITIVE),
        Pattern.compile("""charged\s+\$[0-9\.]+\s+at\s+([A-Za-z0-9\s\.\*\#\-\_&']{2,40})""", Pattern.CASE_INSENSITIVE),
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
        "marketing",
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
                val matched = matcher.group(1)?.replace(",", "")?.trim()
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

        // 4. Extract Merchant Name
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

        val cleanMerchant = merchant ?: "Merchant"
        val formattedMsg = "$bank: $$amount spent at $cleanMerchant"

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
        return when {
            packageName.contains("chase", ignoreCase = true) || combinedText.contains("chase", ignoreCase = true) -> "Chase"
            packageName.contains("bofa", ignoreCase = true) || combinedText.contains("bank of america", ignoreCase = true) -> "Bank of America"
            packageName.contains("wellsfargo", ignoreCase = true) || combinedText.contains("wells fargo", ignoreCase = true) -> "Wells Fargo"
            packageName.contains("citi", ignoreCase = true) || combinedText.contains("citibank", ignoreCase = true) -> "Citi"
            packageName.contains("capitalone", ignoreCase = true) || combinedText.contains("capital one", ignoreCase = true) -> "Capital One"
            packageName.contains("americanexpress", ignoreCase = true) || combinedText.contains("amex", ignoreCase = true) -> "Amex"
            packageName.contains("discover", ignoreCase = true) || combinedText.contains("discover", ignoreCase = true) -> "Discover"
            packageName.contains("usbank", ignoreCase = true) || combinedText.contains("u.s. bank", ignoreCase = true) -> "U.S. Bank"
            packageName.contains("pnc", ignoreCase = true) || combinedText.contains("pnc bank", ignoreCase = true) -> "PNC"
            packageName.contains("revolut", ignoreCase = true) || combinedText.contains("revolut", ignoreCase = true) -> "Revolut"
            packageName.contains("monzo", ignoreCase = true) || combinedText.contains("monzo", ignoreCase = true) -> "Monzo"
            packageName.contains("venmo", ignoreCase = true) || combinedText.contains("venmo", ignoreCase = true) -> "Venmo"
            packageName.contains("paypal", ignoreCase = true) || combinedText.contains("paypal", ignoreCase = true) -> "PayPal"
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
        return clean.trim()
    }
}
