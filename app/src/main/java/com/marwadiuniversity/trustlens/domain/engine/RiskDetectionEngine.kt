package com.marwadiuniversity.trustlens.domain.engine

import com.marwadiuniversity.trustlens.domain.model.RiskResult
import com.marwadiuniversity.trustlens.domain.model.ScanType

class RiskDetectionEngine {
    private val urlAnalyzer = UrlRiskAnalyzer()
    private val messageAnalyzer = MessageRiskAnalyzer()
    private val upiAnalyzer = UpiRiskAnalyzer()
    private val screenshotAnalyzer = PaymentScreenshotAnalyzer()

    fun analyze(input: String, scanType: ScanType): RiskResult {
        return when (scanType) {
            ScanType.URL -> urlAnalyzer.analyze(input)
            ScanType.MESSAGE -> messageAnalyzer.analyze(input)
            ScanType.UPI -> upiAnalyzer.analyze(input)
            ScanType.SCREENSHOT -> screenshotAnalyzer.analyze(input)
        }
    }
}
