package com.marwadiuniversity.trustlens.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marwadiuniversity.trustlens.domain.model.RiskLevel
import com.marwadiuniversity.trustlens.domain.model.ThreatCategory
import com.marwadiuniversity.trustlens.viewmodel.MainViewModel
import com.marwadiuniversity.trustlens.viewmodel.RiskAnalysisUiState
import com.marwadiuniversity.trustlens.viewmodel.RiskAnalysisViewModel

@Composable
fun RiskAnalysisScreen(
    viewModel: MainViewModel,
    riskViewModel: RiskAnalysisViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState = riskViewModel.uiState
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header & Back
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Deep Inspection Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // AI Consent Toggle Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Analyze with AI Assistant",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Send content securely to AI backend for contextual threat assessment.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = riskViewModel.isAiEnabled,
                        onCheckedChange = { riskViewModel.toggleAiConsent(it) }
                    )
                }
                Text(
                    text = riskViewModel.aiStatusMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        when (uiState) {
            is RiskAnalysisUiState.Idle, is RiskAnalysisUiState.Analyzing -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = riskViewModel.aiStatusMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is RiskAnalysisUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Analysis Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(text = uiState.message, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
            is RiskAnalysisUiState.Success -> {
                val result = uiState.result
                val aiResult = uiState.aiResult

                // Hero Result Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (result.level) {
                            RiskLevel.LOW, RiskLevel.Safe -> MaterialTheme.colorScheme.surfaceContainerLowest
                            RiskLevel.MEDIUM, RiskLevel.Suspicious -> Color(0xFFFEF3C7)
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = when (result.level) {
                                    RiskLevel.LOW, RiskLevel.Safe -> Color(0xFF16A34A)
                                    RiskLevel.MEDIUM -> Color(0xFFD97706)
                                    else -> MaterialTheme.colorScheme.error
                                }
                            ) {
                                Text(
                                    text = "${result.level} RISK · ${result.scanType}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Source Label
                            val sourceLabel = when (aiResult?.aiSource) {
                                "gemini" -> "AI-assisted analysis"
                                "fallback" -> "Local analysis — AI unavailable"
                                else -> "Local analysis"
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = sourceLabel,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${result.score}/100",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (result.score < 30) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Risk Score",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Threat Category Badge
                                val categoryText = if (result.threatCategory == ThreatCategory.UNKNOWN) {
                                    "Unknown / No specific threat category"
                                } else {
                                    formatThreatCategory(result.threatCategory.name)
                                }
                                Text(
                                    text = "Threat: $categoryText",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val confidenceStr = if (aiResult != null && aiResult.confidence > 0f) {
                                    "AI confidence: ${(aiResult.confidence * 100).toInt()}%"
                                } else {
                                    "Confidence: Not provided"
                                }
                                Text(
                                    text = confidenceStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (aiResult != null && aiResult.summary.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(text = "AI Summary:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                    Text(text = aiResult.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // What should you do? (Recommendation)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "What should you do?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = result.recommendation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Analyzed Input Display (Privacy safe summary)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Analyzed Target (${result.scanType})",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            Text(
                                text = result.input,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Risk Indicators Breakdown ("Why this was flagged" or "Analysis summary")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val sectionTitle = if (result.indicators.isEmpty() || (result.indicators.size == 1 && result.indicators[0].title.contains("Clean", true))) {
                            "Analysis summary"
                        } else {
                            "Why this was flagged"
                        }
                        Text(
                            text = sectionTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (result.indicators.isEmpty()) {
                            Text(
                                text = "No significant suspicious indicators were detected by the current analysis.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            result.indicators.forEach { ind ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ind.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = ind.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Certainty Boundary Note
                Text(
                    text = "TrustLens identifies risk indicators. It does not guarantee that an item is fraudulent or safe.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

private fun formatThreatCategory(categoryStr: String): String {
    return categoryStr.split("_").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}
