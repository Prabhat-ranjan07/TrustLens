package com.marwadiuniversity.trustlens.domain.model

data class ScanItem(
    val id: String,
    val title: String,
    val type: String, // SMS, URL, QR, Image, Email, Payment
    val riskLevel: RiskLevel, // Safe, Suspicious, HighRisk
    val timestamp: String,
    val description: String
)

data class Article(
    val id: String,
    val title: String,
    val category: String,
    val readTime: String,
    val description: String
)

data class ScanOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconName: String,
    val accentColorHex: String
)
