package com.agsmsforwarder.app.data.model

data class AppPreset(
    val id: String,
    val name: String,
    val packageName: String,
    val isDefaultSelected: Boolean = false,
    val category: String = "Major Bank"
) {
    companion object {
        val DEFAULT_PRESETS = listOf(
            AppPreset("chase", "Chase Mobile", "com.chase.sig.android", true, "Major US Bank"),
            AppPreset("bofa", "Bank of America", "com.infonow.bofa", true, "Major US Bank"),
            AppPreset("wellsfargo", "Wells Fargo Mobile", "com.wf.wellsfargomobile", true, "Major US Bank"),
            AppPreset("citi", "Citi Mobile", "com.citi.citimobile", false, "Major US Bank"),
            AppPreset("capitalone", "Capital One Mobile", "com.konylabs.capitalone", false, "Credit Cards"),
            AppPreset("amex", "American Express", "com.americanexpress.android.acctsvcs.us", false, "Credit Cards"),
            AppPreset("discover", "Discover Mobile", "com.discoverfinancial.mobile", false, "Credit Cards"),
            AppPreset("usbank", "U.S. Bank", "com.usbank.mobilebanking", false, "Major US Bank"),
            AppPreset("pnc", "PNC Mobile", "com.pnc.ecommerce.mobile.android", false, "Major US Bank"),
            AppPreset("revolut", "Revolut", "com.revolut.revolut", false, "Fintech & Global"),
            AppPreset("monzo", "Monzo Bank", "co.uk.monzo", false, "Fintech & Global"),
            AppPreset("wise", "Wise", "com.transferwise.android", false, "Fintech & Global"),
            AppPreset("venmo", "Venmo", "com.venmo", false, "P2P Payment"),
            AppPreset("paypal", "PayPal", "com.paypal.android.p2pmobile", false, "P2P Payment")
        )
    }
}
