package com.marwadiuniversity.trustlens.domain.engine

import com.marwadiuniversity.trustlens.domain.model.RiskResult
import com.marwadiuniversity.trustlens.domain.model.ScanType

class RiskDetectionEngine {
    private val urlAnalyzer = UrlRiskAnalyzer()
    private val messageAnalyzer = MessageRiskAnalyzer()

    fun analyze(input: String, scanType: ScanType): RiskResult {
        return when (scanType) {
            ScanType.URL -> urlAnalyzer.analyze(input)
            ScanType.MESSAGE -> messageAnalyzer.analyze(input)
        }
    }
}
