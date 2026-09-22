package com.marwadiuniversity.trustlens.domain.model

enum class AiRiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
    UNKNOWN
}

data class AiIndicator(
    val title: String,
    val description: String
)

data class AiAnalysisRequest(
    val contentType: String,
    val content: String,
    val localRiskScore: Int,
    val localRiskLevel: String,
    val localIndicators: List<String>
)

data class AiAnalysisResult(
    val riskLevel: AiRiskLevel,
    val riskScore: Int,
    val summary: String,
    val indicators: List<AiIndicator>,
    val recommendation: String,
    val confidence: Float,
    val aiSource: String = "fallback"
)
