package com.marwadiuniversity.trustlens.domain.engine

import com.marwadiuniversity.trustlens.domain.model.RiskIndicator
import com.marwadiuniversity.trustlens.domain.model.RiskLevel
import com.marwadiuniversity.trustlens.domain.model.RiskResult
import com.marwadiuniversity.trustlens.domain.model.ScanType
import com.marwadiuniversity.trustlens.domain.model.ThreatCategory
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
        private const val WEIGHT_AT_SYMBOL = 15
        private const val WEIGHT_USER_INFO = 25
        private const val WEIGHT_UNUSUAL_PORT = 10
        private const val WEIGHT_EXCESSIVE_LENGTH = 5
        private const val WEIGHT_EXCESSIVE_PARAMS = 5
        private const val WEIGHT_ENCODING = 15
        private const val WEIGHT_INTENT = 15

        private val SHORTENERS = listOf("bit.ly", "tinyurl.com", "t.co", "goo.gl", "is.gd", "ow.ly")
        private val SUSPICIOUS_KEYWORDS = listOf("login", "verify", "verification", "secure", "account", "update", "password", "wallet", "payment", "refund", "claim", "bonus", "prize", "winner", "urgent")
        private val CREDENTIAL_INTENT = listOf("login", "signin", "verify", "verification", "password", "otp", "pin", "account", "credential", "security")
        private val PAYMENT_INTENT = listOf("payment", "pay", "refund", "upi", "bank", "card", "invoice", "transaction", "claim")
        private val URGENCY_INTENT = listOf("urgent", "suspended", "blocked", "confirm", "immediately", "limited", "expire")

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
        if (trimmed.isEmpty()) {
            return RiskResult(
                score = 0,
                level = RiskLevel.LOW,
                scanType = ScanType.URL,
                input = urlInput,
                indicators = listOf(RiskIndicator("Empty Input", "No URL provided for analysis.", RiskLevel.LOW)),
                recommendation = getRecommendation(RiskLevel.LOW),
                threatCategory = ThreatCategory.UNKNOWN
            )
        }

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
                recommendation = getRecommendation(RiskLevel.HIGH),
                threatCategory = ThreatCategory.SUSPICIOUS_URL
            )
        }

        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: "http"
        val host = (uri.host ?: uri.path)?.lowercase(Locale.ROOT) ?: ""
        val fullUrl = uri.toString().lowercase(Locale.ROOT)
        val pathAndQuery = "${uri.path ?: ""}?${uri.query ?: ""}".lowercase(Locale.ROOT)

        var hasBrandMismatch = false
        var hasCredentialIntent = false
        var hasPaymentIntent = false
        var hasUrgencyIntent = false
        var hasStructuralAnomaly = false

        // Rule 1: HTTP vs HTTPS
        if (scheme == "http") {
            score += WEIGHT_HTTP
            indicators.add(RiskIndicator("HTTP Protocol Used", "The URL uses non-secure HTTP instead of encrypted HTTPS.", RiskLevel.LOW))
        }

        // Rule 2: Raw IP Address Host
        val ipRegex = Regex("""\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b""")
        if (ipRegex.containsMatchIn(host)) {
            score += WEIGHT_IP_HOST
            hasStructuralAnomaly = true
            indicators.add(RiskIndicator("Raw IP Address Host", "The domain is specified as a raw IP address, which is common in phishing.", RiskLevel.HIGH))
        }

        // Rule 3: URL Shortener
        if (SHORTENERS.any { host.contains(it) }) {
            score += WEIGHT_SHORTENER
            hasStructuralAnomaly = true
            indicators.add(RiskIndicator("URL Shortening Service", "Uses a link shortener that conceals the true destination domain.", RiskLevel.MEDIUM))
        }

        // Rule 4: Embedded '@' Symbol
        if (trimmed.contains("@") || uri.userInfo != null) {
            score += WEIGHT_AT_SYMBOL
            hasStructuralAnomaly = true
            indicators.add(RiskIndicator("Embedded '@' Symbol", "The URL contains user-info syntax that can obscure the actual destination.", RiskLevel.MEDIUM))
        }

        // Rule 5: Username/Password in Authority
        if (uri.userInfo != null && uri.userInfo.isNotEmpty()) {
            score += WEIGHT_USER_INFO
            hasCredentialIntent = true
            indicators.add(RiskIndicator("Embedded Credentials in Authority", "The URL specifies embedded user credentials which can be deceptive.", RiskLevel.HIGH))
        }

        // Rule 6: Unusual Port
        val port = uri.port
        if (port != -1 && port != 80 && port != 443 && port != 8080 && port != 8443) {
            score += WEIGHT_UNUSUAL_PORT
            hasStructuralAnomaly = true
            indicators.add(RiskIndicator("Unusual Port", "The URL specifies a non-standard port number ($port).", RiskLevel.LOW))
        }

        // Rule 7: Excessive URL Length
        if (trimmed.length > 100) {
            score += WEIGHT_EXCESSIVE_LENGTH
            indicators.add(RiskIndicator("Excessive URL Length", "The URL is unusually long (>100 characters), which can be used to hide suspicious parameters.", RiskLevel.LOW))
        }

        // Rule 8: Excessive Query Parameters
        val queryParams = uri.query?.split("&") ?: emptyList()
        if (queryParams.size > 5) {
            score += WEIGHT_EXCESSIVE_PARAMS
            indicators.add(RiskIndicator("Excessive Query Parameters", "The URL contains a high number of query parameters (>5).", RiskLevel.LOW))
        }

        // Rule 9: Percent-encoding / Obfuscation
        val encodedRegex = Regex("""%[0-9a-fA-F]{2}""")
        val encodedMatches = encodedRegex.findAll(trimmed).count()
        if (encodedMatches > 3 || trimmed.contains("%40") || trimmed.contains("%3D")) {
            score += WEIGHT_ENCODING
            hasStructuralAnomaly = true
            indicators.add(RiskIndicator("Suspicious URL Encoding", "The URL contains heavy or unusual percent-encoding which can obscure intent.", RiskLevel.MEDIUM))
        }

        // Rule 10: Path/Query Intent Analysis
        if (CREDENTIAL_INTENT.any { pathAndQuery.contains(it) }) {
            score += WEIGHT_INTENT
            hasCredentialIntent = true
            indicators.add(RiskIndicator("Credential-Related URL Intent", "The URL path or query contains login, verification, or credential terms.", RiskLevel.MEDIUM))
        }
        if (PAYMENT_INTENT.any { pathAndQuery.contains(it) }) {
            score += WEIGHT_INTENT
            hasPaymentIntent = true
            indicators.add(RiskIndicator("Payment-Related URL Intent", "The URL path or query contains payment, refund, or transaction terms.", RiskLevel.MEDIUM))
        }
        if (URGENCY_INTENT.any { pathAndQuery.contains(it) }) {
            score += WEIGHT_INTENT
            hasUrgencyIntent = true
            indicators.add(RiskIndicator("Urgency-Related URL Intent", "The URL path or query contains urgency or account restriction terms.", RiskLevel.MEDIUM))
        }

        // Rule 11: Suspicious Keywords in Full URL
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

        // Rule 12: Excessive Subdomains
        val domainParts = host.split(".")
        if (domainParts.size > 4) {
            score += WEIGHT_SUBDOMAINS
            hasStructuralAnomaly = true
            indicators.add(RiskIndicator("Excessive Subdomains", "The hostname contains an unusually high number of subdomains.", RiskLevel.MEDIUM))
        }

        // Rule 13: Suspicious Domain Patterns (e.g. multiple hyphens)
        if (host.count { it == '-' } > 2 || host.contains("--")) {
            score += WEIGHT_DOMAIN_PATTERN
            hasStructuralAnomaly = true
            indicators.add(RiskIndicator("Suspicious Domain Structure", "The domain contains excessive hyphens or unusual structuring.", RiskLevel.MEDIUM))
        }

        // Rule 14: Domain vs Brand Mismatch
        val combinedContext = "$contextText $fullUrl".lowercase(Locale.ROOT)
        for ((brand, officialDomains) in KNOWN_BRANDS) {
            if (combinedContext.contains(brand)) {
                val matchesOfficial = officialDomains.any { host.endsWith(it) }
                if (!matchesOfficial) {
                    score += WEIGHT_BRAND_MISMATCH
                    hasBrandMismatch = true
                    indicators.add(RiskIndicator("Potential Brand Impersonation", "Mentions '$brand' but the domain does not match official verified domains.", RiskLevel.HIGH))
                    break
                }
            }
        }

        val finalScore = score.coerceIn(0, 100)
        val level = riskLevelFromScore(finalScore)

        val category = when {
            hasBrandMismatch -> ThreatCategory.IMPERSONATION
            hasCredentialIntent -> ThreatCategory.CREDENTIAL_THEFT
            hasPaymentIntent -> ThreatCategory.PAYMENT_SCAM
            hasUrgencyIntent -> ThreatCategory.SOCIAL_ENGINEERING
            hasStructuralAnomaly || finalScore >= 30 -> ThreatCategory.SUSPICIOUS_URL
            else -> ThreatCategory.UNKNOWN
        }

        return RiskResult(
            score = finalScore,
            level = level,
            scanType = ScanType.URL,
            input = urlInput,
            indicators = indicators.ifEmpty {
                listOf(RiskIndicator("Clean URL", "No major risk indicators were detected in this URL.", RiskLevel.LOW))
            },
            recommendation = getRecommendation(level),
            threatCategory = category
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
