package com.agsmsforwarder.app.data.preferences

data class AppPreferences(
    val isServiceEnabled: Boolean = true,
    val enabledPackages: Set<String> = setOf(
        "com.chase.sig.android",
        "com.infonow.bofa",
        "com.wf.wellsfargomobile",
        "com.onedebit.chime"
    ),
    val destinationPhoneNumber: String = "",
    val useAiFormatting: Boolean = true,
    val customSmsPrefix: String = "[ALERT]",
    val modelFilePath: String = "",
    val aiTemperature: Float = 0.2f,
    val aiTopK: Int = 40,
    val aiMaxTokens: Int = 1024,
    val customSystemPrompt: String = DEFAULT_SYSTEM_PROMPT
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "You are a financial notification alert parser. Extract the bank name, amount, and merchant. " +
            "For purchases, output: '[Bank]: $[Amount] spent at [Merchant]'. " +
            "For balance updates, output: '[Bank]: Balance is $[Amount]'. " +
            "If not a transaction or balance update, output 'SKIP'."
    }
}
