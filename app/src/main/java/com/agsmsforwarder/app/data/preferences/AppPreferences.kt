package com.agsmsforwarder.app.data.preferences

data class AppPreferences(
    val isServiceEnabled: Boolean = true,
    val enabledPackages: Set<String> = setOf(
        "com.chase.sig.android",
        "com.infonow.bofa",
        "com.wf.wellsfargomobile"
    ),
    val destinationPhoneNumber: String = "",
    val useAiFormatting: Boolean = true,
    val customSmsPrefix: String = "[ALERT]",
    val modelFilePath: String = "",
    val aiTemperature: Float = 0.2f,
    val aiTopK: Int = 40,
    val aiMaxTokens: Int = 128,
    val customSystemPrompt: String = DEFAULT_SYSTEM_PROMPT
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "You are a transaction parser. Extract the bank, amount, and clean merchant name from the notification text. " +
            "Output ONLY in this format: '[Bank]: $[Amount] spent at [Merchant]'. If not a transaction, output 'SKIP'."
    }
}
