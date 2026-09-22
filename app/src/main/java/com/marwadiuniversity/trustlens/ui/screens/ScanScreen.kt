package com.marwadiuniversity.trustlens.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.marwadiuniversity.trustlens.domain.engine.PaymentScreenshotAnalyzer
import com.marwadiuniversity.trustlens.domain.model.ScanType
import com.marwadiuniversity.trustlens.ui.components.ScanOptionCard
import com.marwadiuniversity.trustlens.viewmodel.MainViewModel
import com.marwadiuniversity.trustlens.viewmodel.RiskAnalysisViewModel

@Composable
fun ScanScreen(
    viewModel: MainViewModel,
    riskViewModel: RiskAnalysisViewModel,
    onNavigateToCameraScanner: () -> Unit,
    onNavigateToResult: () -> Unit
) {
    var queryInput by remember { mutableStateOf("") }
    var ocrStatusMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val extractedText = visionText.text
                        if (extractedText.isNotBlank()) {
                            val screenshotAnalyzer = PaymentScreenshotAnalyzer()
                            val scanType = if (screenshotAnalyzer.isPaymentScreenshot(extractedText)) {
                                ScanType.SCREENSHOT
                            } else {
                                ScanType.MESSAGE
                            }
                            riskViewModel.analyzeInput(extractedText, scanType)
                            onNavigateToResult()
                        } else {
                            ocrStatusMessage = "No readable text was detected in this image."
                        }
                    }
                    .addOnFailureListener {
                        ocrStatusMessage = "Failed to process image for OCR."
                    }
            } catch (e: Exception) {
                ocrStatusMessage = "Error loading selected image."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Universal Scan Hub",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Scan QR codes with camera or upload a payment screenshot / invoice.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Camera QR Scanner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCameraScanner() }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Scan QR Code (CameraX)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Real-time ML Kit barcode inspection", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }

        // Gallery OCR Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { galleryLauncher.launch("image/*") }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF16A34A).copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color(0xFF16A34A))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Upload Screenshot (OCR)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Extract text from invoice or image locally", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
            }
        }

        if (!ocrStatusMessage.isNullOrEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Text(
                    text = ocrStatusMessage ?: "",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Quick Paste Input Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = queryInput,
                    onValueChange = { queryInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste link, SMS, message or UPI ID...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Button(
                    onClick = {
                        val type = if (queryInput.contains("http://") || queryInput.contains("https://") || queryInput.contains("www.")) ScanType.URL else ScanType.MESSAGE
                        riskViewModel.analyzeInput(queryInput.ifBlank { "https://secure-bank-login.xyz" }, type)
                        onNavigateToResult()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "Run Local Fraud Engine", fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "Inspection Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        ScanOptionCard(
            title = "SMS & Messages",
            subtitle = "Detect lottery fraud, bank impersonation & fake couriers",
            icon = Icons.Default.ChatBubble,
            accentColor = MaterialTheme.colorScheme.secondary,
            onClick = {
                riskViewModel.analyzeInput("Your account will be suspended. Verify immediately using this link.", ScanType.MESSAGE)
                onNavigateToResult()
            },
            tag = "Phishing"
        )
        ScanOptionCard(
            title = "Web Links & URLs",
            subtitle = "Analyze website links for zero-day redirects & spoofing",
            icon = Icons.Default.Public,
            accentColor = MaterialTheme.colorScheme.secondaryContainer,
            onClick = {
                riskViewModel.analyzeInput("http://192.168.1.10/login", ScanType.URL)
                onNavigateToResult()
            },
            tag = "Zero-day"
        )
    }
}
