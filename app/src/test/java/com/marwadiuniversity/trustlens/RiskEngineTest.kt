package com.marwadiuniversity.trustlens

import com.marwadiuniversity.trustlens.domain.engine.MessageRiskAnalyzer
import com.marwadiuniversity.trustlens.domain.engine.UpiRiskAnalyzer
import com.marwadiuniversity.trustlens.domain.engine.UrlRiskAnalyzer
import com.marwadiuniversity.trustlens.domain.model.RiskLevel
import com.marwadiuniversity.trustlens.domain.model.ScanType
import com.marwadiuniversity.trustlens.domain.model.ThreatCategory
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskEngineTest {
    private val urlAnalyzer = UrlRiskAnalyzer()
    private val messageAnalyzer = MessageRiskAnalyzer()
    private val upiAnalyzer = UpiRiskAnalyzer()

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
        assertTrue("Should be LOTTERY_SCAM", result.threatCategory == ThreatCategory.LOTTERY_SCAM)
    }

    @Test
    fun test6_safeOtpWarning() {
        val result = messageAnalyzer.analyze("Your OTP is 123456. Never share this OTP with anyone.")
        assertTrue("Safe OTP warning message should be LOW risk", result.score < 40)
        assertTrue("Safe OTP should not be CREDENTIAL_THEFT", result.threatCategory != ThreatCategory.CREDENTIAL_THEFT)
    }

    @Test
    fun test7_maliciousOtpRequest() {
        val result = messageAnalyzer.analyze("Send me your OTP to complete the refund.")
        assertTrue("Malicious OTP request should be HIGH or CRITICAL risk", result.score >= 50)
        assertTrue("Should be CREDENTIAL_THEFT", result.threatCategory == ThreatCategory.CREDENTIAL_THEFT)
    }

    @Test
    fun test8_validNormalUpiUri() {
        val result = upiAnalyzer.analyze("upi://pay?pa=merchant@upi&pn=Test%20Store&am=100&cu=INR")
        assertTrue("Valid normal UPI URI should be LOW risk", result.score < 30)
        assertTrue("Scan type should be UPI", result.scanType == ScanType.UPI)
    }

    @Test
    fun test9_upiUriWithAmount() {
        val result = upiAnalyzer.analyze("upi://pay?pa=store@upi&am=500&cu=INR")
        assertTrue("UPI with valid amount should be low risk", result.score < 30)
    }

    @Test
    fun test10_upiMissingPayee() {
        val result = upiAnalyzer.analyze("upi://pay?am=500&cu=INR")
        assertTrue("Missing payee should trigger high/critical risk", result.score >= 35)
        assertTrue("Should be QR_SCAM", result.threatCategory == ThreatCategory.QR_SCAM)
    }

    @Test
    fun test11_upiInvalidAddress() {
        val result = upiAnalyzer.analyze("upi://pay?pa=invalid")
        assertTrue("Invalid UPI address should trigger risk", result.score >= 35)
    }

    @Test
    fun test12_upiInvalidAmount() {
        val result = upiAnalyzer.analyze("upi://pay?pa=test@upi&am=abc&cu=INR")
        assertTrue("Invalid amount should trigger indicator", result.indicators.any { it.title.contains("Invalid Payment Amount", true) })
    }

    @Test
    fun test13_upiNegativeAmount() {
        val result = upiAnalyzer.analyze("upi://pay?pa=test@upi&am=-50&cu=INR")
        assertTrue("Negative amount should trigger indicator", result.indicators.any { it.title.contains("Invalid Payment Amount", true) })
    }

    @Test
    fun test14_upiMalformedUri() {
        val result = upiAnalyzer.analyze("upi://pay?%%%%")
        assertTrue("Malformed UPI URI should not crash", result.score >= 0)
    }

    @Test
    fun test15_upiEmptyInput() {
        val result = upiAnalyzer.analyze("")
        assertTrue("Empty UPI input should be LOW risk", result.score == 0)
    }

    @Test
    fun test16_textContainingUpiIsNotUpi() {
        val rawValue = "UPI payments are convenient."
        val lower = rawValue.lowercase()
        val isUpi = lower.startsWith("upi://pay") || lower.startsWith("upi:")
        assertTrue("Ordinary text containing UPI must not be classified as UPI", !isUpi)
    }

    @Test
    fun test17_caseInsensitiveUpiScheme() {
        val rawValue = "UPI://PAY?pa=test@upi&pn=Test"
        val lower = rawValue.lowercase()
        val isUpi = lower.startsWith("upi://pay") || lower.startsWith("upi:")
        assertTrue("Case-insensitive UPI scheme should be recognized", isUpi)
    }

    @Test
    fun test18_ordinaryMerchantUpiQr() {
        val result = upiAnalyzer.analyze("upi://pay?pa=shop@okaxis&pn=Local%20Merchant&am=250&cu=INR")
        assertTrue("Ordinary merchant UPI should remain LOW risk", result.score < 30)
    }
}
