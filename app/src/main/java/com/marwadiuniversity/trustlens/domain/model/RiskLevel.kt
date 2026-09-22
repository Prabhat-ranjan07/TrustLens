package com.marwadiuniversity.trustlens.domain.model

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    Safe,
    Suspicious,
    HighRisk
}

fun riskLevelFromScore(score: Int): RiskLevel {
    return when (score) {
        in 0..29 -> RiskLevel.LOW
        in 30..59 -> RiskLevel.MEDIUM
        in 60..79 -> RiskLevel.HIGH
        else -> RiskLevel.CRITICAL
    }
}
