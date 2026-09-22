package com.marwadiuniversity.trustlens

import com.marwadiuniversity.trustlens.domain.engine.MessageRiskAnalyzer
import com.marwadiuniversity.trustlens.domain.engine.UrlRiskAnalyzer
import com.marwadiuniversity.trustlens.domain.model.RiskLevel
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
}
