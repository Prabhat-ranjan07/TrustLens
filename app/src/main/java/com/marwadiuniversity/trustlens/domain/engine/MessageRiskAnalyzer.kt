package com.marwadiuniversity.trustlens.domain.engine

import com.marwadiuniversity.trustlens.domain.model.RiskIndicator
import com.marwadiuniversity.trustlens.domain.model.RiskLevel
import com.marwadiuniversity.trustlens.domain.model.RiskResult
import com.marwadiuniversity.trustlens.domain.model.ScanType
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

        private val URGENCY_TERMS = listOf("immediately", "urgent", "act now", "within 24 hours", "last warning", "today only")
        private val THREAT_TERMS = listOf("account will be suspended", "legal action", "police complaint", "account blocked", "suspend", "block")
        private val OTP_TERMS = listOf("otp", "one time password", "verification code")
        private val CREDENTIAL_TERMS = listOf("password", "pin", "upi pin", "cvv", "card number", "login credentials", "credentials")
        private val FINANCIAL_TERMS = listOf("send money", "transfer", "payment", "upi", "bank account", "refund", "refund fee", "processing fee", "deposit", "investment", "pay", "fee")
        private val PRIZE_TERMS = listOf("winner", "lottery", "prize", "reward", "cashback", "free gift", "claim now", "won")
        private val SHARE_ACTION_TERMS = listOf("share", "send", "give", "disclose", "tell", "forward", "enter")
    }

    fun analyze(message: String): RiskResult {
        val text = message.lowercase(Locale.ROOT)
        val indicators = mutableListOf<RiskIndicator>()
        var score = 0

        // A. Urgency
        if (URGENCY_TERMS.any { text.contains(it) }) {
            score += WEIGHT_URGENCY
            indicators.add(RiskIndicator("Urgent Language", "The message conveys artificial urgency to pressure immediate action.", RiskLevel.MEDIUM))
        }

        // B. Threatening Language
        if (THREAT_TERMS.any { text.contains(it) }) {
            score += WEIGHT_THREAT
            indicators.add(RiskIndicator("Threatening Language", "Contains threats of account suspension, blocking, or legal action.", RiskLevel.HIGH))
        }

        // C. OTP Request (Context-Aware: Only flag if asking to share/disclose without negative warning)
        val hasOtpTerm = OTP_TERMS.any { text.contains(it) }
        val isWarning = text.contains("never share") || text.contains("do not share") || text.contains("don't share")
        val asksToShare = !isWarning && SHARE_ACTION_TERMS.any { action ->
            text.contains(action)
        } && hasOtpTerm

        if (hasOtpTerm && asksToShare) {
            score += WEIGHT_OTP_REQUEST
            indicators.add(RiskIndicator("Requests OTP", "Asks you to share or disclose a one-time verification password (OTP). Never share OTPs.", RiskLevel.CRITICAL))
        }

        // D. Password / Credential Request
        if (CREDENTIAL_TERMS.any { text.contains(it) }) {
            score += WEIGHT_CREDENTIAL
            indicators.add(RiskIndicator("Requests Passwords or PINs", "Asks for sensitive credentials, PINs, or card CVVs.", RiskLevel.CRITICAL))
        }

        // E. Financial Request
        if (FINANCIAL_TERMS.any { text.contains(it) }) {
            score += WEIGHT_FINANCIAL
            indicators.add(RiskIndicator("Financial Request", "Mentions money transfers, processing fees, or payment demands.", RiskLevel.HIGH))
        }

        // F. Prize / Reward Scam Patterns
        if (PRIZE_TERMS.any { text.contains(it) }) {
            score += WEIGHT_PRIZE
            indicators.add(RiskIndicator("Prize or Reward Language", "Claims you won a lottery or prize, often a hook for advance fee fraud.", RiskLevel.MEDIUM))
        }

        // G. Extract and Analyze URLs in Message
        val urlRegex = Regex("""https?://[^\s]+|www\.[^\s]+|[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}[^\s]*""")
        val foundUrls = urlRegex.findAll(message).map { it.value }.toList()
        for (url in foundUrls) {
            val urlResult = urlRiskAnalyzer.analyze(url, message)
            score += (urlResult.score / 2)
            indicators.addAll(urlResult.indicators)
        }

        val finalScore = score.coerceIn(0, 100)
        val level = riskLevelFromScore(finalScore)

        return RiskResult(
            score = finalScore,
            level = level,
            scanType = ScanType.MESSAGE,
            input = message,
            indicators = indicators.ifEmpty {
                listOf(RiskIndicator("Clean Message", "No suspicious phishing or fraud patterns detected in this message.", RiskLevel.LOW))
            },
            recommendation = getRecommendation(level)
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
