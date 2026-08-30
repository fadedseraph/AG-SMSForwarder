package com.agsmsforwarder.app.data.model

enum class SmsDeliveryStatus(val label: String) {
    PENDING("Pending"),
    SENT("Sent"),
    DELIVERED("Delivered"),
    FAILED_GENERIC("Failed: Error"),
    FAILED_NO_SERVICE("Failed: No Service"),
    FAILED_NULL_PDU("Failed: Null PDU"),
    FAILED_RADIO_OFF("Failed: Radio Off"),
    SKIPPED_NOT_TRANSACTION("Skipped: Non-Transaction"),
    SKIPPED_NO_RECIPIENT("Skipped: No Phone Number"),
    SIMULATED_TEST("Test Passed")
}
