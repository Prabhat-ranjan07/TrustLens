package com.marwadiuniversity.trustlens.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.marwadiuniversity.trustlens.domain.model.ScanType

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val scanType: String,
    val timestamp: Long,
    val riskScore: Int,
    val riskLevel: String,
    val inputSummary: String,
    val indicatorsJson: String,
    val recommendation: String,
    val confidence: Float,
    val aiSource: String
)

fun sanitizeInputForHistory(input: String, scanType: ScanType): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return "Empty Scan"
    return when (scanType) {
        ScanType.URL -> {
            if (trimmed.length > 50) trimmed.take(47) + "..." else trimmed
        }
        ScanType.MESSAGE -> {
            val lower = trimmed.lowercase()
            when {
                lower.contains("otp") || lower.contains("verification code") -> "Sensitive Verification Message"
                lower.contains("password") || lower.contains("pin") -> "Credential Request Message"
                lower.contains("bank") || lower.contains("account") -> "Financial Notification Message"
                else -> if (trimmed.length > 40) trimmed.take(37) + "..." else trimmed
            }
        }
    }
}
