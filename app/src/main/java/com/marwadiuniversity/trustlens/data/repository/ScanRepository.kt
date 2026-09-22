package com.marwadiuniversity.trustlens.data.repository

import com.marwadiuniversity.trustlens.domain.engine.RiskDetectionEngine
import com.marwadiuniversity.trustlens.domain.model.RiskResult
import com.marwadiuniversity.trustlens.domain.model.ScanType

class ScanRepository {
    private val engine = RiskDetectionEngine()

    suspend fun scanInput(input: String, scanType: ScanType): RiskResult {
        return engine.analyze(input, scanType)
    }
}
