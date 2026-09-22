package com.marwadiuniversity.trustlens.domain.engine

import com.marwadiuniversity.trustlens.domain.model.RiskIndicator
import com.marwadiuniversity.trustlens.domain.model.RiskLevel
import com.marwadiuniversity.trustlens.domain.model.RiskResult
import com.marwadiuniversity.trustlens.domain.model.ScanType
import com.marwadiuniversity.trustlens.domain.model.ThreatCategory
import com.marwadiuniversity.trustlens.domain.model.riskLevelFromScore
import java.util.Locale

class MessageRiskAnalyzer {
    private val urlRiskAnalyzer = UrlRiskAnalyzer()

    companion object {
        private const val WEIGHT_URGENCY = 15
        private const val WEIGHT_THREAT = 25
        private const val WEIGHT_OTP_REQUEST = 40
        private const val WEIGHT_CREDENTIAL = 40
        private const val WEIGHT_FINANCIAL = 25
        private const val WEIGHT_PRIZE = 25
        private const val WEIGHT_JOB_FEE = 35
        private const val WEIGHT_INVESTMENT_GUARANTEE = 35

        private val URGENCY_TERMS = listOf("immediately", "urgent", "act now", "within 24 hours", "last warning", "today only", "expires today")
        private val THREAT_TERMS = listOf("account will be suspended", "legal action", "police complaint", "account blocked", "suspend", "block", "arrest", "service termination")
        private val OTP_TERMS = listOf("otp", "one time password", "verification code")
        private val CREDENTIAL_TERMS = listOf("password", "pin", "upi pin", "cvv", "card number", "login credentials", "credentials")
        private val FINANCIAL_TERMS = listOf("send money", "transfer", "payment", "upi", "bank account", "refund", "refund fee", "processing fee", "deposit", "pay", "fee")
        private val PRIZE_TERMS = listOf("winner", "lottery", "prize", "reward", "cashback", "free gift", "claim now", "won")
        private val JOB_TERMS = listOf("job", "hiring", "work from home", "salary", "employment", "recruitment")
        private val JOB_FEE_TERMS = listOf("registration fee", "training fee", "security deposit", "application fee", "pay to secure")
        private val INVESTMENT_TERMS = listOf("crypto", "invest", "trading", "stocks", "forex")
        private val INVESTMENT_GUARANTEE_TERMS = listOf("guaranteed return", "guaranteed profit", "double money", "risk-free", "10x", "zero risk")
        private val AUTHORITY_TERMS = listOf("bank", "sbi", "hdfc", "icici", "police", "tax authority", "government", "support team", "security team", "official")
        private val ACTION_VERBS = listOf("share", "send", "provide", "tell", "enter", "submit", "confirm", "forward", "transfer", "pay")
    }

    private fun hasSafetyWarning(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("never share") ||
               lower.contains("do not share") ||
               lower.contains("don't share") ||
               lower.contains("never pay") ||
               lower.contains("do not pay") ||
               lower.contains("market risk") ||
               lower.contains("not guaranteed") ||
               lower.contains("never give") ||
               lower.contains("successful")
    }

    fun analyze(message: String): RiskResult {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) {
            return RiskResult(
                score = 0,
                level = RiskLevel.LOW,
                scanType = ScanType.MESSAGE,
                input = message,
                indicators = listOf(RiskIndicator("Empty Message", "No message text provided for analysis.", RiskLevel.LOW)),
                recommendation = getRecommendation(RiskLevel.LOW),
                threatCategory = ThreatCategory.UNKNOWN
            )
        }

        val text = trimmed.lowercase(Locale.ROOT)
        val indicators = mutableListOf<RiskIndicator>()
        var score = 0

        val isSafeNotice = hasSafetyWarning(text)

        var hasUrgency = false
        var hasThreat = false
        var hasCredentialTheft = false
        var hasPaymentScam = false
        var hasFakeJob = false
        var hasFakeInvestment = false
        var hasLotteryScam = false
        var hasImpersonation = false
        var hasSocialEngineering = false

        // A. Urgency
        if (!isSafeNotice && URGENCY_TERMS.any { text.contains(it) }) {
            score += WEIGHT_URGENCY
            hasUrgency = true
            indicators.add(RiskIndicator("Urgent Language", "The message conveys artificial urgency to pressure immediate action.", RiskLevel.MEDIUM))
        }

        // B. Threatening Language
        if (!isSafeNotice && THREAT_TERMS.any { text.contains(it) }) {
            score += WEIGHT_THREAT
            hasThreat = true
            indicators.add(RiskIndicator("Threatening Language", "Contains threats of account suspension, blocking, or legal action.", RiskLevel.HIGH))
        }

        // C. Credential Theft (OTP / Passwords + Action Verbs)
        val hasOtpTerm = OTP_TERMS.any { text.contains(it) }
        val hasCredTerm = CREDENTIAL_TERMS.any { text.contains(it) }
        val hasActionVerb = ACTION_VERBS.any { text.contains(it) }

        if (!isSafeNotice && (hasOtpTerm || hasCredTerm) && hasActionVerb) {
            score += WEIGHT_CREDENTIAL
            hasCredentialTheft = true
            val title = if (hasOtpTerm) "Requests OTP" else "Requests Passwords or PINs"
            indicators.add(RiskIndicator(title, "Asks you to share or disclose sensitive verification codes or credentials.", RiskLevel.CRITICAL))
        }

        // D. Payment Scam
        val hasFinancialTerm = FINANCIAL_TERMS.any { text.contains(it) }
        val requestsMoney = hasFinancialTerm && (text.contains("pay") || text.contains("transfer") || text.contains("fee") || text.contains("deposit"))
        if (!isSafeNotice && requestsMoney && !text.contains("successful")) {
            score += WEIGHT_FINANCIAL
            hasPaymentScam = true
            indicators.add(RiskIndicator("Financial Payment Request", "Mentions money transfers, processing fees, or payment demands.", RiskLevel.HIGH))
        }

        // E. Fake Job Scam
        val hasJobTerm = JOB_TERMS.any { text.contains(it) }
        val hasJobFeeTerm = JOB_FEE_TERMS.any { text.contains(it) }
        if (!isSafeNotice && hasJobTerm && hasJobFeeTerm) {
            score += WEIGHT_JOB_FEE
            hasFakeJob = true
            indicators.add(RiskIndicator("Job Registration Fee Scam", "Combines a job offer with a demand for registration, training, or deposit fees.", RiskLevel.HIGH))
        }

        // F. Fake Investment Scam
        val hasInvestmentTerm = INVESTMENT_TERMS.any { text.contains(it) }
        val hasInvestmentGuarantee = INVESTMENT_GUARANTEE_TERMS.any { text.contains(it) }
        if (!isSafeNotice && hasInvestmentTerm && hasInvestmentGuarantee) {
            score += WEIGHT_INVESTMENT_GUARANTEE
            hasFakeInvestment = true
            indicators.add(RiskIndicator("Guaranteed Investment Scam", "Promises high or risk-free investment returns without market disclosures.", RiskLevel.HIGH))
        }

        // G. Lottery / Prize Scam
        val hasPrizeTerm = PRIZE_TERMS.any { text.contains(it) }
        if (!isSafeNotice && hasPrizeTerm && (text.contains("fee") || text.contains("tax") || text.contains("processing") || text.contains("claim"))) {
            score += WEIGHT_PRIZE
            hasLotteryScam = true
            indicators.add(RiskIndicator("Advance-Fee Lottery Scam", "Claims you won a prize or lottery but requires an advance payment or fee to claim.", RiskLevel.HIGH))
        }

        // H. Impersonation
        val hasAuthority = AUTHORITY_TERMS.any { text.contains(it) }
        if (!isSafeNotice && hasAuthority && (hasCredentialTheft || hasPaymentScam || hasThreat)) {
            score += 20
            hasImpersonation = true
            indicators.add(RiskIndicator("Suspicious Authority Impersonation", "Appears to claim authority from an official institution while making demands or threats.", RiskLevel.HIGH))
        }

        // I. Social Engineering (Urgency + Threat)
        if (!isSafeNotice && hasUrgency && hasThreat) {
            score += 20
            hasSocialEngineering = true
            indicators.add(RiskIndicator("Social Engineering Manipulation", "Combines severe threats with artificial urgency to force compliance.", RiskLevel.HIGH))
        }

        // J. Extract and Analyze URLs in Message
        val urlRegex = Regex("""https?://[^\s]+|www\.[^\s]+|[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}[^\s]*""")
        val foundUrls = urlRegex.findAll(message).map { it.value }.toList()
        for (url in foundUrls) {
            val urlResult = urlRiskAnalyzer.analyze(url, message)
            score += (urlResult.score / 2)
            indicators.addAll(urlResult.indicators)
            if (hasUrgency || hasThreat) {
                score += 15 // context boost for URL combined with urgency/threat
            }
        }

        val finalScore = score.coerceIn(0, 100)
        val level = riskLevelFromScore(finalScore)

        val category = when {
            hasCredentialTheft -> ThreatCategory.CREDENTIAL_THEFT
            hasPaymentScam -> ThreatCategory.PAYMENT_SCAM
            hasFakeJob -> ThreatCategory.FAKE_JOB
            hasFakeInvestment -> ThreatCategory.FAKE_INVESTMENT
            hasLotteryScam -> ThreatCategory.LOTTERY_SCAM
            hasImpersonation -> ThreatCategory.IMPERSONATION
            hasSocialEngineering -> ThreatCategory.SOCIAL_ENGINEERING
            finalScore >= 50 -> ThreatCategory.PHISHING
            else -> ThreatCategory.UNKNOWN
        }

        return RiskResult(
            score = finalScore,
            level = level,
            scanType = ScanType.MESSAGE,
            input = message,
            indicators = indicators.ifEmpty {
                listOf(RiskIndicator("Clean Message", "No suspicious phishing or fraud patterns detected in this message.", RiskLevel.LOW))
            },
            recommendation = getRecommendation(level),
            threatCategory = category
        )
    }

    private fun getRecommendation(level: RiskLevel): String {
        return when (level) {
            RiskLevel.LOW -> "No major risk indicators were detected. Still verify unexpected messages before taking action."
            RiskLevel.MEDIUM -> "Be cautious. Verify the sender and information independently before clicking links or sharing information."
            RiskLevel.HIGH -> "Do not click links or share sensitive information. Verify the request through the organization's official website or app."
            RiskLevel.CRITICAL -> "Do not interact with this message or link. Do not share OTPs, passwords, PINs, or payment information. Verify through an official channel."
            else -> "Verify before taking action."
        }
    }
}
