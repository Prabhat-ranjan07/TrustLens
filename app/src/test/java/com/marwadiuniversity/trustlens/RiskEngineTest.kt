package com.marwadiuniversity.trustlens

import com.marwadiuniversity.trustlens.domain.engine.MessageRiskAnalyzer
import com.marwadiuniversity.trustlens.domain.engine.PaymentScreenshotAnalyzer
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
    private val screenshotAnalyzer = PaymentScreenshotAnalyzer()

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
    fun test9_normalPaymentSuccessScreenshot() {
        val text = "Payment Successful\nAmount ₹500\nTransaction ID ABC123\nPaid to Test Store\nUPI ID merchant@upi"
        val result = screenshotAnalyzer.analyze(text)
        assertTrue("Normal payment success screenshot should be LOW risk", result.score < 30)
        assertTrue("Scan type should be SCREENSHOT", result.scanType == ScanType.SCREENSHOT)
    }

    @Test
    fun test10_conflictingPaymentStatusScreenshot() {
        val text = "Payment Successful\nPayment Failed\nAmount ₹500\nTransaction ID ABC123"
        val result = screenshotAnalyzer.analyze(text)
        assertTrue("Conflicting payment status should trigger high risk", result.score >= 40)
        assertTrue("Should be PAYMENT_SCAM", result.threatCategory == ThreatCategory.PAYMENT_SCAM)
    }

    @Test
    fun test11_conflictingAmountsScreenshot() {
        val text = "Payment Successful\nAmount ₹500\nTotal Amount ₹5,000\nTransaction ID ABC123"
        val result = screenshotAnalyzer.analyze(text)
        assertTrue("Conflicting amounts should trigger high risk", result.score >= 35)
    }

    @Test
    fun test12_successClaimWithMissingMetadata() {
        val text = "Payment Successful"
        val result = screenshotAnalyzer.analyze(text)
        assertTrue("Success claim with missing metadata should have moderate indicator", result.indicators.any { it.title.contains("Limited Transaction Evidence", true) })
    }

    @Test
    fun test13_emptyScreenshotText() {
        val result = screenshotAnalyzer.analyze("")
        assertTrue("Empty screenshot text should be LOW risk", result.score == 0)
    }

    @Test
    fun test14_normalDocumentOcr() {
        val text = "Student Name: Rahul\nMarks: 85\nDate: 12/09/2026"
        val isPayment = screenshotAnalyzer.isPaymentScreenshot(text)
        assertTrue("Normal document should not be recognized as payment screenshot", !isPayment)
    }

    @Test
    fun test15_normalFinancialText() {
        val text = "The company reported revenue of INR 5 crore."
        val isPayment = screenshotAnalyzer.isPaymentScreenshot(text)
        assertTrue("Normal financial text should not be recognized as payment screenshot", !isPayment)
    }

    @Test
    fun test16_ocrTypoTolerance() {
        val text = "Paymnt Successfl\nAmount Rs 500\nTxn ID XYZ789"
        val result = screenshotAnalyzer.analyze(text)
        assertTrue("OCR typo tolerance test should not crash", result.score >= 0)
    }

    @Test
    fun test17_refundText() {
        val text = "Refund processed successfully\nAmount ₹1,200\nTxn REF123"
        val result = screenshotAnalyzer.analyze(text)
        assertTrue("Refund text should be handled safely", result.score < 40)
    }

    @Test
    fun test18_lowQualityIncompletePaymentText() {
        val text = "Paid ₹200 to merchant"
        val result = screenshotAnalyzer.analyze(text)
        assertTrue("Low quality payment text should be handled safely", result.score < 40)
    }
}
