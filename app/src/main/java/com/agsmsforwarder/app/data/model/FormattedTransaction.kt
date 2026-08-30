package com.agsmsforwarder.app.data.model

data class FormattedTransaction(
    val bank: String,
    val amount: String,
    val merchant: String,
    val isTransaction: Boolean = true,
    val formattedMessage: String,
    val isAiGenerated: Boolean = false,
    val latencyMs: Long = 0L
) {
    companion object {
        val SKIP = FormattedTransaction(
            bank = "",
            amount = "",
            merchant = "",
            isTransaction = false,
            formattedMessage = "SKIP",
            isAiGenerated = false,
            latencyMs = 0L
        )
    }
}
