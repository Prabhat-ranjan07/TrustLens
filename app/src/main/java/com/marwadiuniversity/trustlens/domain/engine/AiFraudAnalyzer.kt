package com.marwadiuniversity.trustlens.domain.engine

import com.marwadiuniversity.trustlens.domain.model.AiAnalysisRequest
import com.marwadiuniversity.trustlens.domain.model.AiAnalysisResult

interface AiFraudAnalyzer {
    suspend fun analyze(request: AiAnalysisRequest): Result<AiAnalysisResult>
}
