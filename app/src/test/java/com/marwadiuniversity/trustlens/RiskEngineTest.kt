package com.marwadiuniversity.trustlens

import com.marwadiuniversity.trustlens.domain.engine.MessageRiskAnalyzer
import com.marwadiuniversity.trustlens.domain.engine.UrlRiskAnalyzer
import com.marwadiuniversity.trustlens.domain.model.RiskLevel
import com.marwadiuniversity.trustlens.domain.model.ThreatCategory
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskEngineTest {
    private val urlAnalyzer = UrlRiskAnalyzer()
    private val messageAnalyzer = MessageRiskAnalyzer()

    @Test
    fun test1_googleUrl() {
        val result = urlAnalyzer.analyze("https://www.google.com")
        assertTrue("Google URL should be LOW risk", result.level == RiskLevel.LOW || result.score < 30)
    }

    @Test
    fun test2_ipAddressLoginUrl() {
        val result = urlAnalyzer.analyze("http://192.168.1.10/login")
        assertTrue("IP address login URL should have increased risk", result.score >= 35)
    }

    @Test
    fun test3_urlShortener() {
        val result = urlAnalyzer.analyze("https://bit.ly/example")
        assertTrue("URL shortener should trigger indicator", result.indicators.any { it.title.contains("URL Shortening", true) })
    }

    @Test
    fun test4_accountBlockedMessage() {
        val result = messageAnalyzer.analyze("Your account will be blocked today. Verify immediately using this link.")
        assertTrue("Account block warning should be at least MEDIUM/HIGH risk", result.score >= 25)
    }

    @Test
    fun test5_lotteryPrizeFee() {
        val result = messageAnalyzer.analyze("Congratulations! You won ₹50,000. Pay ₹500 processing fee to claim your prize.")
        assertTrue("Lottery fee scam should be HIGH or CRITICAL risk", result.score >= 50)
    }

    @Test
    fun test6_safeOtpWarning() {
        val result = messageAnalyzer.analyze("Your OTP is 123456. Never share this OTP with anyone.")
        assertTrue("Safe OTP warning message should be LOW risk", result.score < 40)
    }

    @Test
    fun test7_maliciousOtpRequest() {
        val result = messageAnalyzer.analyze("Send me your OTP to complete the refund.")
        assertTrue("Malicious OTP request should be HIGH or CRITICAL risk", result.score >= 50)
    }

    @Test
    fun test8_atSymbolUrl() {
        val result = urlAnalyzer.analyze("https://trusted.example@malicious.example")
        assertTrue("At symbol in URL should trigger indicator", result.indicators.any { it.title.contains("Embedded '@'", true) })
    }

    @Test
    fun test9_userInfoUrl() {
        val result = urlAnalyzer.analyze("https://user:pass@example.com/login")
        assertTrue("User info in authority should trigger indicator", result.indicators.any { it.title.contains("Embedded Credentials", true) })
    }

    @Test
    fun test10_unusualPortUrl() {
        val result = urlAnalyzer.analyze("https://example.com:8080/path")
        assertTrue("Unusual port should trigger indicator", result.indicators.any { it.title.contains("Unusual Port", true) })
    }

    @Test
    fun test11_excessiveLengthUrl() {
        val longUrl = "https://example.com/" + "a".repeat(100)
        val result = urlAnalyzer.analyze(longUrl)
        assertTrue("Excessive length should trigger indicator", result.indicators.any { it.title.contains("Excessive URL Length", true) })
    }

    @Test
    fun test12_excessiveParamsUrl() {
        val result = urlAnalyzer.analyze("https://example.com?p1=1&p2=2&p3=3&p4=4&p5=5&p6=6")
        assertTrue("Excessive query params should trigger indicator", result.indicators.any { it.title.contains("Excessive Query Parameters", true) })
    }

    @Test
    fun test13_percentEncodedUrl() {
        val result = urlAnalyzer.analyze("https://example.com/%40secure%2Flogin")
        assertTrue("Suspicious encoding should trigger indicator", result.indicators.any { it.title.contains("Suspicious URL Encoding", true) })
    }

    @Test
    fun test14_credentialIntentUrl() {
        val result = urlAnalyzer.analyze("https://example.com/login/verify/password")
        assertTrue("Credential intent should categorize as CREDENTIAL_THEFT", result.threatCategory == ThreatCategory.CREDENTIAL_THEFT)
    }

    @Test
    fun test15_paymentIntentUrl() {
        val result = urlAnalyzer.analyze("https://example.com/refund/upi/payment")
        assertTrue("Payment intent should categorize as PAYMENT_SCAM", result.threatCategory == ThreatCategory.PAYMENT_SCAM)
    }

    @Test
    fun test16_malformedUrl() {
        val result = urlAnalyzer.analyze("invalid://[url]")
        assertTrue("Malformed URL should be handled safely", result.score >= 0)
    }

    @Test
    fun test17_emptyInputUrl() {
        val result = urlAnalyzer.analyze("")
        assertTrue("Empty input URL should be LOW risk", result.score == 0)
    }

    @Test
    fun test18_normalQueryUrlFalsePositiveCheck() {
        val result = urlAnalyzer.analyze("https://www.google.com/search?q=android&hl=en&safe=active")
        assertTrue("Normal Google search URL should not be flagged as high risk", result.score < 40)
    }

    @Test
    fun test19_normalLongUrlFalsePositiveCheck() {
        val result = urlAnalyzer.analyze("https://en.wikipedia.org/wiki/Android_(operating_system)")
        assertTrue("Normal Wikipedia URL should not be flagged as high risk", result.score < 40)
    }
}
