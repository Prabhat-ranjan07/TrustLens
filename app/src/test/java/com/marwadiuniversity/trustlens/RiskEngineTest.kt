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
    fun test8_bankImpersonation() {
        val result = messageAnalyzer.analyze("This is SBI security team. Send your OTP immediately.")
        assertTrue("Bank impersonation OTP request should be high risk", result.score >= 50)
    }

    @Test
    fun test9_refundScam() {
        val result = messageAnalyzer.analyze("Pay ₹500 processing fee to receive your refund.")
        assertTrue("Refund scam should be PAYMENT_SCAM", result.threatCategory == ThreatCategory.PAYMENT_SCAM)
    }

    @Test
    fun test10_safeJobAd() {
        val result = messageAnalyzer.analyze("Our company is hiring software engineers.")
        assertTrue("Safe job ad should be LOW risk", result.score < 30)
    }

    @Test
    fun test11_fakeJobFee() {
        val result = messageAnalyzer.analyze("Congratulations, you have been selected. Pay ₹999 registration fee to confirm your job.")
        assertTrue("Fake job fee scam should be FAKE_JOB", result.threatCategory == ThreatCategory.FAKE_JOB)
    }

    @Test
    fun test12_safeInvestment() {
        val result = messageAnalyzer.analyze("Investments involve market risk and returns are not guaranteed.")
        assertTrue("Safe investment disclosure should be LOW risk", result.score < 30)
    }

    @Test
    fun test13_fakeInvestmentScam() {
        val result = messageAnalyzer.analyze("Invest ₹5,000 and get guaranteed ₹50,000 in 7 days.")
        assertTrue("Guaranteed return investment scam should be FAKE_INVESTMENT", result.threatCategory == ThreatCategory.FAKE_INVESTMENT)
    }

    @Test
    fun test14_emptyMessage() {
        val result = messageAnalyzer.analyze("")
        assertTrue("Empty message should be LOW risk", result.score == 0)
    }

    @Test
    fun test15_successfulPaymentNotification() {
        val result = messageAnalyzer.analyze("Your payment of ₹500 was successful.")
        assertTrue("Successful payment notification should be LOW risk", result.score < 30)
    }

    @Test
    fun test16_safeRecruitmentFeeWarning() {
        val result = messageAnalyzer.analyze("Never pay anyone a recruitment fee.")
        assertTrue("Safe recruitment fee warning should be LOW risk", result.score < 30)
    }

    @Test
    fun test17_urlAndUrgencyMessage() {
        val result = messageAnalyzer.analyze("Your account is suspended. Verify immediately: http://192.168.1.1/login")
        assertTrue("Urgent URL message should be high risk", result.score >= 40)
    }

    @Test
    fun test18_veryLongMessage() {
        val longMsg = "Please note that " + "word ".repeat(200)
        val result = messageAnalyzer.analyze(longMsg)
        assertTrue("Long message should be handled without crash", result.score >= 0)
    }
}
