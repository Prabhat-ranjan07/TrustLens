package com.marwadiuniversity.trustlens.domain.model

enum class ThreatCategory {
    PHISHING,
    PAYMENT_SCAM,
    FAKE_JOB,
    FAKE_INVESTMENT,
    LOTTERY_SCAM,
    IMPERSONATION,
    CREDENTIAL_THEFT,
    SUSPICIOUS_URL,
    QR_SCAM,
    SOCIAL_ENGINEERING,
    UNKNOWN
}

fun formatThreatCategory(category: ThreatCategory): String {
    return category.name.split("_").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}
