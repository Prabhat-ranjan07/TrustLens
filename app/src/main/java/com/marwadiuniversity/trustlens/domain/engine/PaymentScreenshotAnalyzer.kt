package com.marwadiuniversity.trustlens.domain.engine

import com.marwadiuniversity.trustlens.domain.model.RiskIndicator
import com.marwadiuniversity.trustlens.domain.model.RiskLevel
import com.marwadiuniversity.trustlens.domain.model.RiskResult
import com.marwadiuniversity.trustlens.domain.model.ScanType
import com.marwadiuniversity.trustlens.domain.model.ThreatCategory
import com.marwadiuniversity.trustlens.domain.model.riskLevelFromScore
import java.util.Locale

class PaymentScreenshotAnalyzer {
    companion object {
        private val PAYMENT_KEYWORDS = listOf("payment", "transaction", "paid", "amount", "upi", "recipient", "payee", "utr", "reference", "successful", "completed", "failed", "rs.", "inr")
        private val SUCCESS_TERMS = listOf("success", "successful", "paid", "completed", "complete")
        private val FAILURE_TERMS = listOf("failed", "declined", "cancelled", "canceled", "failure")
    }

    fun isPaymentScreenshot(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val matches = PAYMENT_KEYWORDS.count { lower.contains(it) }
        return matches >= 2
    }

    fun analyze(ocrText: String): RiskResult {
        val trimmed = ocrText.trim()
        if (trimmed.isEmpty()) {
            return RiskResult(
                score = 0,
                level = RiskLevel.LOW,
                scanType = ScanType.SCREENSHOT,
                input = ocrText,
                indicators = listOf(RiskIndicator("Empty Screenshot Text", "No text detected in the uploaded image.", RiskLevel.LOW)),
                recommendation = "Verify transaction details in your official payment app.",
                threatCategory = ThreatCategory.UNKNOWN
            )
        }

        val lower = trimmed.lowercase(Locale.ROOT)
        val indicators = mutableListOf<RiskIndicator>()
        var score = 0

        // 1. Status detection
        val hasSuccess = SUCCESS_TERMS.any { lower.contains(it) }
        val hasFailure = FAILURE_TERMS.any { lower.contains(it) }

        var hasConflictStatus = false
        if (hasSuccess && hasFailure) {
            score += 45
            hasConflictStatus = true
            indicators.add(RiskIndicator("Conflicting Payment Status", "The screenshot contains conflicting payment status claims (success and failure both detected).", RiskLevel.HIGH))
        }

        // 2. Amount extraction & conflict check
        val amountRegex = Regex("""(?:₹|inr|rs\.?)\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
        val amountMatches = amountRegex.findAll(trimmed).mapNotNull { match ->
            match.groupValues.getOrNull(1)?.replace(",", "")?.toDoubleOrNull()
        }.distinct().toList()

        var hasConflictAmount = false
        if (amountMatches.size > 1) {
            val maxAm = amountMatches.maxOrNull() ?: 0.0
            val minAm = amountMatches.minOrNull() ?: 0.0
            if (maxAm > minAm * 1.05) {
                score += 35
                hasConflictAmount = true
                indicators.add(RiskIndicator("Conflicting Payment Amounts", "Multiple conflicting payment amounts were detected in the screenshot text.", RiskLevel.HIGH))
            }
        }

        // 3. Transaction ID extraction
        val txnIdRegex = Regex("""(?:transaction\s*id|txn\s*id|transaction\s*ref|reference\s*(?:no|number)|utr(?:\s*no)?|order\s*id)[:\s]*([a-zA-Z0-9_-]{6,30})""", RegexOption.IGNORE_CASE)
        val hasTxnId = txnIdRegex.containsMatchIn(trimmed)

        // 4. Payee / UPI extraction
        val upiRegex = Regex("""[a-zA-Z0-9.\-_]+@[a-zA-Z0-9]+""")
        val hasUpi = upiRegex.containsMatchIn(trimmed)

        // 5. Success Claim with Missing Metadata
        var hasLimitedEvidence = false
        if (hasSuccess && !hasConflictStatus) {
            if (!hasTxnId && !hasUpi) {
                score += 20
                hasLimitedEvidence = true
                indicators.add(RiskIndicator("Limited Transaction Evidence", "Payment success is claimed, but standard transaction metadata (ID or UPI) is missing.", RiskLevel.MEDIUM))
            }
        }

        val finalScore = score.coerceIn(0, 100)
        val level = riskLevelFromScore(finalScore)

        val category = when {
            hasConflictStatus || hasConflictAmount -> ThreatCategory.PAYMENT_SCAM
            hasLimitedEvidence && finalScore >= 30 -> ThreatCategory.PAYMENT_SCAM
            else -> ThreatCategory.UNKNOWN
        }

        return RiskResult(
            score = finalScore,
            level = level,
            scanType = ScanType.SCREENSHOT,
            input = ocrText,
            indicators = indicators.ifEmpty {
                listOf(RiskIndicator("Payment Screenshot Verified", "Payment screenshot contains consistent metadata with no major risk indicators.", RiskLevel.LOW))
            },
            recommendation = getRecommendation(level),
            threatCategory = category
        )
    }

    private fun getRecommendation(level: RiskLevel): String {
        return when (level) {
            RiskLevel.LOW -> "Payment screenshot appears consistent. Verify transaction details in your official payment app."
            RiskLevel.MEDIUM -> "Exercise caution. Verify the transaction in your banking or payment application before relying on this screenshot."
            RiskLevel.HIGH -> "Inconsistencies detected in payment metadata. Verify the transaction directly in your payment account."
            RiskLevel.CRITICAL -> "Severe payment claim conflicts detected. Do not rely on this screenshot."
            else -> "Verify transaction in your official app."
        }
    }
}
