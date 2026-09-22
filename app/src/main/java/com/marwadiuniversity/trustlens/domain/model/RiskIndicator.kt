package com.marwadiuniversity.trustlens.domain.model

data class RiskIndicator(
    val title: String,
    val description: String,
    val severity: RiskLevel
)
