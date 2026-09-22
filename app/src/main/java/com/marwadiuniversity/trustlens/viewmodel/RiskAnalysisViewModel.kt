package com.marwadiuniversity.trustlens.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marwadiuniversity.trustlens.data.local.room.ScanDatabase
import com.marwadiuniversity.trustlens.data.local.room.ScanHistoryEntity
import com.marwadiuniversity.trustlens.data.local.room.sanitizeInputForHistory
import com.marwadiuniversity.trustlens.data.repository.HttpAiFraudAnalyzer
import com.marwadiuniversity.trustlens.data.repository.ScanHistoryRepository
import com.marwadiuniversity.trustlens.data.repository.ScanRepository
import com.marwadiuniversity.trustlens.domain.engine.AiFraudAnalyzer
import com.marwadiuniversity.trustlens.domain.engine.CombinedRiskCalculator
import com.marwadiuniversity.trustlens.domain.model.AiAnalysisRequest
import com.marwadiuniversity.trustlens.domain.model.AiAnalysisResult
import com.marwadiuniversity.trustlens.domain.model.RiskResult
import com.marwadiuniversity.trustlens.domain.model.ScanType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface RiskAnalysisUiState {
    object Idle : RiskAnalysisUiState
    object Analyzing : RiskAnalysisUiState
    data class Success(val result: RiskResult, val aiResult: AiAnalysisResult?) : RiskAnalysisUiState
    data class Error(val message: String) : RiskAnalysisUiState
}

class RiskAnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private val scanRepository = ScanRepository()
    private val aiAnalyzer: AiFraudAnalyzer = HttpAiFraudAnalyzer()
    private val combinedRiskCalculator = CombinedRiskCalculator()
    private val historyRepository: ScanHistoryRepository by lazy {
        ScanHistoryRepository(ScanDatabase.getDatabase(application).scanHistoryDao())
    }

    var uiState by mutableStateOf<RiskAnalysisUiState>(RiskAnalysisUiState.Idle)
        private set

    var inputQuery by mutableStateOf("")
        private set

    var isAiEnabled by mutableStateOf(false)
        private set

    var aiStatusMessage by mutableStateOf("AI Analysis Available (Optional)")
        private set

    fun toggleAiConsent(enabled: Boolean) {
        isAiEnabled = enabled
        aiStatusMessage = if (enabled) "AI Analysis enabled (gemini-3.6-flash)" else "AI Analysis disabled"
    }

    fun updateInput(input: String) {
        if (input.length <= 1000) {
            inputQuery = input
        }
    }

    fun analyzeInput(input: String, scanType: ScanType) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            uiState = RiskAnalysisUiState.Error("Input cannot be empty.")
            return
        }

        viewModelScope.launch {
            uiState = RiskAnalysisUiState.Analyzing
            aiStatusMessage = "Running local heuristic engine..."
            delay(300L)

            val localResult = try {
                scanRepository.scanInput(trimmed, scanType)
            } catch (e: Exception) {
                uiState = RiskAnalysisUiState.Error(e.localizedMessage ?: "Analysis failed")
                return@launch
            }

            var aiResult: AiAnalysisResult? = null

            if (isAiEnabled) {
                aiStatusMessage = "Querying secure backend & Gemini model..."
                val request = AiAnalysisRequest(
                    contentType = scanType.name,
                    content = trimmed,
                    localRiskScore = localResult.score,
                    localRiskLevel = localResult.level.name,
                    localIndicators = localResult.indicators.map { it.title }
                )

                val aiResponse = aiAnalyzer.analyze(request)
                aiResult = aiResponse.getOrNull()

                if (aiResult != null && aiResult.aiSource == "gemini") {
                    aiStatusMessage = "Real Gemini AI analysis active."
                } else {
                    aiStatusMessage = "AI analysis unavailable — showing local analysis."
                }
            } else {
                aiStatusMessage = "AI analysis unavailable — showing local analysis."
            }

            val finalResult = combinedRiskCalculator.combine(localResult, aiResult)

            try {
                val sanitizedSummary = sanitizeInputForHistory(trimmed, scanType)
                val source = aiResult?.aiSource ?: if (isAiEnabled) "fallback" else "local"
                val conf = aiResult?.confidence ?: 0f
                val entity = ScanHistoryEntity(
                    scanType = scanType.name,
                    timestamp = System.currentTimeMillis(),
                    riskScore = finalResult.score,
                    riskLevel = finalResult.level.name,
                    inputSummary = sanitizedSummary,
                    indicatorsJson = finalResult.indicators.joinToString(" | ") { it.title },
                    recommendation = finalResult.recommendation,
                    confidence = conf,
                    aiSource = source
                )
                historyRepository.insertScan(entity)
            } catch (e: Exception) {
                // Ignore persistence failure
            }

            uiState = RiskAnalysisUiState.Success(finalResult, aiResult)
        }
    }

    fun resetState() {
        uiState = RiskAnalysisUiState.Idle
        inputQuery = ""
        aiResult = null
    }

    var aiResult by mutableStateOf<AiAnalysisResult?>(null)
        private set
}
