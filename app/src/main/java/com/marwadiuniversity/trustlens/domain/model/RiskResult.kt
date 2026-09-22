package com.marwadiuniversity.trustlens.domain.model

data class RiskResult(
    val score: Int,
    val level: RiskLevel,
    val scanType: ScanType,
    val input: String,
    val indicators: List<RiskIndicator>,
    val recommendation: String
)
