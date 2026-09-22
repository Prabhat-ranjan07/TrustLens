package com.marwadiuniversity.trustlens.domain.engine

import com.marwadiuniversity.trustlens.domain.model.RiskIndicator
import com.marwadiuniversity.trustlens.domain.model.RiskLevel
import com.marwadiuniversity.trustlens.domain.model.RiskResult
import com.marwadiuniversity.trustlens.domain.model.ScanType
import com.marwadiuniversity.trustlens.domain.model.ThreatCategory
import com.marwadiuniversity.trustlens.domain.model.riskLevelFromScore
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Locale

class UpiRiskAnalyzer {
    fun analyze(upiUriInput: String): RiskResult {
        val trimmed = upiUriInput.trim()
        if (trimmed.isEmpty()) {
            return RiskResult(
                score = 0,
                level = RiskLevel.LOW,
                scanType = ScanType.UPI,
                input = upiUriInput,
                indicators = listOf(RiskIndicator("Empty UPI URI", "No UPI payment URI provided.", RiskLevel.LOW)),
                recommendation = "Verify payment details before transferring funds.",
                threatCategory = ThreatCategory.UNKNOWN
            )
        }

        val indicators = mutableListOf<RiskIndicator>()
        var score = 0

        val queryParams = try {
            val uri = URI(trimmed)
            val query = uri.query ?: ""
            query.split("&").associate { param ->
                val parts = param.split("=", limit = 2)
                val key = parts.getOrNull(0)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) } ?: ""
                val value = parts.getOrNull(1)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) } ?: ""
                key to value
            }
        } catch (e: Exception) {
            try {
                val qIndex = trimmed.indexOf('?')
                if (qIndex != -1 && qIndex < trimmed.length - 1) {
                    val query = trimmed.substring(qIndex + 1)
                    query.split("&").associate { param ->
                        val parts = param.split("=", limit = 2)
                        val key = parts.getOrNull(0) ?: ""
                        val value = parts.getOrNull(1) ?: ""
                        key to value
                    }
                } else {
                    emptyMap()
                }
            } catch (ex: Exception) {
                emptyMap()
            }
        }

        if (!trimmed.lowercase(Locale.ROOT).startsWith("upi://pay")) {
            score += 25
            indicators.add(RiskIndicator("Malformed UPI Payment URI", "The payment URI does not start with the standard 'upi://pay' scheme.", RiskLevel.MEDIUM))
        }

        val pa = queryParams["pa"]?.trim() ?: ""
        val am = queryParams["am"]?.trim()
        val cu = queryParams["cu"]?.trim()?.uppercase(Locale.ROOT)

        var hasInvalidPa = false
        var hasInvalidAm = false
        var hasMissingPa = false

        if (pa.isEmpty()) {
            score += 40
            hasMissingPa = true
            indicators.add(RiskIndicator("Missing UPI Payee Address", "The payment URI does not specify a payee UPI ID ('pa').", RiskLevel.HIGH))
        } else {
            val parts = pa.split("@")
            if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                score += 35
                hasInvalidPa = true
                indicators.add(RiskIndicator("Malformed UPI Payee Address", "The payee UPI ID format appears invalid or malformed.", RiskLevel.HIGH))
            }
        }

        if (am != null) {
            val amountVal = am.toDoubleOrNull()
            if (amountVal == null) {
                score += 30
                hasInvalidAm = true
                indicators.add(RiskIndicator("Invalid Payment Amount", "The payment amount specified is not a valid number.", RiskLevel.MEDIUM))
            } else if (amountVal <= 0) {
                score += 30
                hasInvalidAm = true
                indicators.add(RiskIndicator("Invalid Payment Amount", "The payment amount must be greater than zero.", RiskLevel.MEDIUM))
            }
        }

        if (am != null && cu != null && cu != "INR") {
            score += 20
            indicators.add(RiskIndicator("Unexpected Currency", "The payment currency is '$cu' instead of expected standard INR.", RiskLevel.MEDIUM))
        }

        val finalScore = score.coerceIn(0, 100)
        val level = riskLevelFromScore(finalScore)

        val category = when {
            hasMissingPa || hasInvalidPa || hasInvalidAm -> ThreatCategory.QR_SCAM
            finalScore >= 50 -> ThreatCategory.PAYMENT_SCAM
            else -> ThreatCategory.UNKNOWN
        }

        return RiskResult(
            score = finalScore,
            level = level,
            scanType = ScanType.UPI,
            input = upiUriInput,
            indicators = indicators.ifEmpty {
                listOf(RiskIndicator("Valid UPI Payment URI", "The UPI payment URI structure is well-formed.", RiskLevel.LOW))
            },
            recommendation = getRecommendation(level),
            threatCategory = category
        )
    }

    private fun getRecommendation(level: RiskLevel): String {
        return when (level) {
            RiskLevel.LOW -> "UPI URI structure is valid. Verify merchant name and amount before paying."
            RiskLevel.MEDIUM -> "Exercise caution with this payment QR. Verify the recipient identity independently."
            RiskLevel.HIGH -> "Suspicious payment parameters detected. Do not complete payment unless fully verified."
            RiskLevel.CRITICAL -> "Invalid or potentially fraudulent UPI payment code. Do not transfer funds."
            else -> "Verify before making payments."
        }
    }
}
