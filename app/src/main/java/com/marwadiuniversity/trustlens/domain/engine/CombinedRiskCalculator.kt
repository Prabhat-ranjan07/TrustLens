package com.marwadiuniversity.trustlens.domain.engine

import com.marwadiuniversity.trustlens.domain.model.AiAnalysisResult
import com.marwadiuniversity.trustlens.domain.model.AiRiskLevel
import com.marwadiuniversity.trustlens.domain.model.RiskIndicator
import com.marwadiuniversity.trustlens.domain.model.RiskLevel
import com.marwadiuniversity.trustlens.domain.model.RiskResult
import com.marwadiuniversity.trustlens.domain.model.ThreatCategory
import com.marwadiuniversity.trustlens.domain.model.riskLevelFromScore

class CombinedRiskCalculator {
    fun combine(local: RiskResult, ai: AiAnalysisResult?): RiskResult {
        if (ai == null) return local

        // Conservative combination: 70% local deterministic score + 30% AI contextual score
        val combinedScore = ((local.score * 0.7) + (ai.riskScore * 0.3)).toInt().coerceIn(0, 100)
        val combinedLevel = riskLevelFromScore(combinedScore)

        val combinedIndicators = mutableListOf<RiskIndicator>()
        combinedIndicators.addAll(local.indicators)

        // Add AI indicators if not redundant
        for (aiInd in ai.indicators) {
            if (combinedIndicators.none { it.title.equals(aiInd.title, ignoreCase = true) }) {
                val sev = when (ai.riskLevel) {
                    AiRiskLevel.CRITICAL -> RiskLevel.CRITICAL
                    AiRiskLevel.HIGH -> RiskLevel.HIGH
                    AiRiskLevel.MEDIUM -> RiskLevel.MEDIUM
                    else -> RiskLevel.LOW
                }
                combinedIndicators.add(RiskIndicator(aiInd.title, aiInd.description, sev))
            }
        }

        // Disagreement detection
        val localIsLow = local.score < 40
        val aiIsHigh = ai.riskScore >= 70
        if (localIsLow && aiIsHigh) {
            combinedIndicators.add(
                RiskIndicator(
                    "Analysis Note: Disagreement",
                    "Local heuristic score is low, but AI context indicates potential advanced phishing or social engineering.",
                    RiskLevel.MEDIUM
                )
            )
        }

        val finalRecommendation = if (ai.recommendation.isNotBlank() && ai.confidence >= 0.5f) {
            ai.recommendation
        } else {
            local.recommendation
        }

        val finalCategory = if (ai.confidence >= 0.5f && ai.threatCategory != ThreatCategory.UNKNOWN) {
            ai.threatCategory
        } else {
            local.threatCategory
        }

        return RiskResult(
            score = combinedScore,
            level = combinedLevel,
            scanType = local.scanType,
            input = local.input,
            indicators = combinedIndicators,
            recommendation = finalRecommendation,
            threatCategory = finalCategory
        )
    }
}
