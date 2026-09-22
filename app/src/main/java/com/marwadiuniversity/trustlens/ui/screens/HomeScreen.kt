package com.marwadiuniversity.trustlens.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.marwadiuniversity.trustlens.ui.components.RecentScanCard
import com.marwadiuniversity.trustlens.ui.components.ScanOptionCard
import com.marwadiuniversity.trustlens.ui.components.SecurityScoreCard
import com.marwadiuniversity.trustlens.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val scrollState = rememberScrollState()
    val userName = viewModel.currentUser?.name?.substringBefore(" ") ?: "User"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting Block
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "Good morning, $userName",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Stay safe. Think before you click.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Security Score Card
        SecurityScoreCard(
            score = viewModel.currentUser?.securityScore ?: 86,
            statusText = "Status: Good",
            lastScanText = "Last full scan: 14 mins ago · Real-time shield active",
            onReverifyClick = { /* Reverify */ }
        )

        // Quick Input Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToScan() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Paste link, SMS, or UPI handle to verify...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onNavigateToScan,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(text = "Inspect", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Check Something Section (6 items)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Check Something",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Instant AI Analysis",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ScanOptionCard(
                    title = "Scan SMS",
                    subtitle = "Detect lottery fraud & bank impersonation",
                    icon = Icons.Default.ChatBubble,
                    accentColor = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToScan,
                    tag = "Phishing"
                )
                ScanOptionCard(
                    title = "Scan Link",
                    subtitle = "Analyze website URL & zero-day redirects",
                    icon = Icons.Default.Public,
                    accentColor = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = onNavigateToScan,
                    tag = "Zero-day"
                )
                ScanOptionCard(
                    title = "Scan QR Code",
                    subtitle = "Verify merchant VPA & UPI collect requests",
                    icon = Icons.Default.QrCodeScanner,
                    accentColor = Color(0xFF16A34A),
                    onClick = onNavigateToScan,
                    tag = "UPI Pay"
                )
                ScanOptionCard(
                    title = "Scan Image",
                    subtitle = "Detect fake screenshots & manipulated bills",
                    icon = Icons.Default.Image,
                    accentColor = Color(0xFFF59E0B),
                    onClick = onNavigateToScan,
                    tag = "Media"
                )
                ScanOptionCard(
                    title = "Analyze Email",
                    subtitle = "Check headers & suspicious attachments",
                    icon = Icons.Default.Email,
                    accentColor = Color(0xFFDC2626),
                    onClick = onNavigateToScan,
                    tag = "Inboxes"
                )
                ScanOptionCard(
                    title = "Check Payment Screenshot",
                    subtitle = "Verify authentic bank transfer proofs",
                    icon = Icons.Default.Receipt,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToScan,
                    tag = "Finance"
                )
            }
        }

        // Recent Scans Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Scans",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onNavigateToHistory) {
                    Text(text = "View All", color = MaterialTheme.colorScheme.secondary)
                }
            }

            viewModel.recentScans.take(3).forEach { scan ->
                RecentScanCard(scanItem = scan, onClick = onNavigateToHistory)
            }
        }
    }
}
