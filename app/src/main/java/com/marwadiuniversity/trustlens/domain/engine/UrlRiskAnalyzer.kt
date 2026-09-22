package com.marwadiuniversity.trustlens.domain.engine

import com.marwadiuniversity.trustlens.domain.model.RiskIndicator
import com.marwadiuniversity.trustlens.domain.model.RiskLevel
import com.marwadiuniversity.trustlens.domain.model.RiskResult
import com.marwadiuniversity.trustlens.domain.model.ScanType
import com.marwadiuniversity.trustlens.domain.model.riskLevelFromScore
import java.net.URI
import java.util.Locale

class UrlRiskAnalyzer {
    companion object {
        private const val WEIGHT_HTTP = 10
        private const val WEIGHT_IP_HOST = 25
        private const val WEIGHT_SHORTENER = 15
        private const val WEIGHT_KEYWORD = 5
        private const val MAX_KEYWORD_WEIGHT = 20
        private const val WEIGHT_SUBDOMAINS = 10
        private const val WEIGHT_DOMAIN_PATTERN = 10
        private const val WEIGHT_BRAND_MISMATCH = 25

        private val SHORTENERS = listOf("bit.ly", "tinyurl.com", "t.co", "goo.gl", "is.gd", "ow.ly")
        private val SUSPICIOUS_KEYWORDS = listOf("login", "verify", "verification", "secure", "account", "update", "password", "wallet", "payment", "refund", "claim", "bonus", "prize", "winner", "urgent")
        private val KNOWN_BRANDS = mapOf(
            "sbi" to listOf("sbi.co.in", "onlinesbi.sbi"),
            "hdfc" to listOf("hdfcbank.com"),
            "icici" to listOf("icicibank.com"),
            "paytm" to listOf("paytm.com"),
            "amazon" to listOf("amazon.in", "amazon.com"),
            "paypal" to listOf("paypal.com"),
            "google" to listOf("google.com", "google.co.in"),
            "netflix" to listOf("netflix.com")
        )
    }

    fun analyze(urlInput: String, contextText: String = ""): RiskResult {
        val trimmed = urlInput.trim()
        val indicators = mutableListOf<RiskIndicator>()
        var score = 0

        val uri = try {
            val formatted = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) "https://$trimmed" else trimmed
            URI(formatted)
        } catch (e: Exception) {
            indicators.add(RiskIndicator("Invalid URL Structure", "The provided text is not a valid URL format.", RiskLevel.HIGH))
            return RiskResult(
                score = 75,
                level = RiskLevel.HIGH,
                scanType = ScanType.URL,
                input = urlInput,
                indicators = indicators,
                recommendation = getRecommendation(RiskLevel.HIGH)
            )
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: "http"
        val host = (uri.host ?: uri.path)?.lowercase(Locale.ROOT) ?: ""
        val fullUrl = uri.toString().lowercase(Locale.ROOT)

        // Rule A: HTTP vs HTTPS
        if (scheme == "http") {
            score += WEIGHT_HTTP
            indicators.add(RiskIndicator("HTTP Protocol Used", "The URL uses non-secure HTTP instead of encrypted HTTPS.", RiskLevel.LOW))
        }

        // Rule B: Raw IP Address Host
        val ipRegex = Regex("""\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b""")
        if (ipRegex.containsMatchIn(host)) {
            score += WEIGHT_IP_HOST
            indicators.add(RiskIndicator("Raw IP Address Host", "The domain is specified as a raw IP address, which is common in phishing.", RiskLevel.HIGH))
        }

        // Rule C: URL Shortener
        if (SHORTENERS.any { host.contains(it) }) {
            score += WEIGHT_SHORTENER
            indicators.add(RiskIndicator("URL Shortening Service", "Uses a link shortener that conceals the true destination domain.", RiskLevel.MEDIUM))
        }

        // Rule D: Suspicious Keywords
        var keywordCount = 0
        for (kw in SUSPICIOUS_KEYWORDS) {
            if (fullUrl.contains(kw)) {
                keywordCount++
            }
        }
        if (keywordCount > 0) {
            val kwScore = minOf(keywordCount * WEIGHT_KEYWORD, MAX_KEYWORD_WEIGHT)
            score += kwScore
            indicators.add(RiskIndicator("Suspicious Keywords", "Contains sensitive security or urgency keywords ($keywordCount detected).", RiskLevel.MEDIUM))
        }

        // Rule E: Excessive Subdomains
        val domainParts = host.split(".")
        if (domainParts.size > 4) {
            score += WEIGHT_SUBDOMAINS
            indicators.add(RiskIndicator("Excessive Subdomains", "The hostname contains an unusually high number of subdomains.", RiskLevel.MEDIUM))
        }

        // Rule F: Suspicious Domain Patterns (e.g. multiple hyphens)
        if (host.count { it == '-' } > 2 || host.contains("--")) {
            score += WEIGHT_DOMAIN_PATTERN
            indicators.add(RiskIndicator("Suspicious Domain Structure", "The domain contains excessive hyphens or unusual structuring.", RiskLevel.MEDIUM))
        }

        // Rule G: Domain vs Brand Mismatch
        val combinedContext = "$contextText $fullUrl".lowercase(Locale.ROOT)
        for ((brand, officialDomains) in KNOWN_BRANDS) {
            if (combinedContext.contains(brand)) {
                val matchesOfficial = officialDomains.any { host.endsWith(it) }
                if (!matchesOfficial) {
                    score += WEIGHT_BRAND_MISMATCH
                    indicators.add(RiskIndicator("Possible Brand Mismatch", "Mentions '$brand' but the domain does not match official verified domains.", RiskLevel.HIGH))
                    break
                }
            }
        }

        val finalScore = score.coerceIn(0, 100)
        val level = riskLevelFromScore(finalScore)

        return RiskResult(
            score = finalScore,
            level = level,
            scanType = ScanType.URL,
            input = urlInput,
            indicators = indicators.ifEmpty {
                listOf(RiskIndicator("Clean URL", "No major risk indicators were detected in this URL.", RiskLevel.LOW))
            },
            recommendation = getRecommendation(level)
        )
    }

    private fun getRecommendation(level: RiskLevel): String {
        return when (level) {
            RiskLevel.LOW -> "No major risk indicators were detected. Still verify unexpected links before taking action."
            RiskLevel.MEDIUM -> "Be cautious. Verify the sender and website independently before clicking links or sharing information."
            RiskLevel.HIGH -> "Do not click links or share sensitive information. Verify the request through the organization's official website or app."
            RiskLevel.CRITICAL -> "Do not interact with this link. Do not share OTPs, passwords, PINs, or payment information. Verify through an official channel."
            else -> "Verify before clicking."
        }
    }
}
