package com.marwadiuniversity.trustlens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marwadiuniversity.trustlens.domain.model.RiskLevel

@Composable
fun RiskBadge(
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val (text, color, containerColor) = when (riskLevel) {
        RiskLevel.Safe, RiskLevel.LOW -> Triple("Safe", Color(0xFF16A34A), Color(0xFFDCFCE7))
        RiskLevel.Suspicious, RiskLevel.MEDIUM -> Triple("Suspicious", Color(0xFFD97706), Color(0xFFFEF3C7))
        RiskLevel.HighRisk, RiskLevel.HIGH -> Triple("High Risk", Color(0xFFDC2626), Color(0xFFFFDAD6))
        RiskLevel.CRITICAL -> Triple("Critical Risk", Color(0xFF990000), Color(0xFFFFCDD2))
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
