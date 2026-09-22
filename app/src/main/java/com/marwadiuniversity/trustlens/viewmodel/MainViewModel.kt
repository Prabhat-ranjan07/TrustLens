package com.marwadiuniversity.trustlens.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marwadiuniversity.trustlens.data.local.PreferencesManager
import com.marwadiuniversity.trustlens.data.repository.FirebaseUserRepository
import com.marwadiuniversity.trustlens.data.repository.UserRepository
import com.marwadiuniversity.trustlens.domain.model.Article
import com.marwadiuniversity.trustlens.domain.model.RiskLevel
import com.marwadiuniversity.trustlens.domain.model.ScanItem
import com.marwadiuniversity.trustlens.domain.model.UserProfile
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsManager = PreferencesManager(application)
    private val userRepository: UserRepository = FirebaseUserRepository(application)

    var isDarkTheme by mutableStateOf(prefsManager.isDarkTheme())
        private set

    var searchQuery by mutableStateOf("")
        private set

    var selectedHistoryFilter by mutableStateOf("All")
        private set

    var currentUser by mutableStateOf<UserProfile?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var currentScanResult by mutableStateOf<ScanItem?>(
        ScanItem("1", "https://secure-bank-login.xyz", "URL", RiskLevel.HighRisk, "14 mins ago", "Phishing domain mimicking banking portal")
    )
        private set

    val recentScans = mutableListOf(
        ScanItem("1", "https://secure-bank-login.xyz", "URL", RiskLevel.HighRisk, "14 mins ago", "Phishing domain mimicking banking portal"),
        ScanItem("2", "SMS: Urgent KYC Update...", "SMS", RiskLevel.HighRisk, "2 hours ago", "Impersonation fraud attempt"),
        ScanItem("3", "UPI: scammer@paytm", "QR", RiskLevel.Suspicious, "Yesterday", "Flagged virtual payment address"),
        ScanItem("4", "https://play.google.com/store", "URL", RiskLevel.Safe, "2 days ago", "Verified secure official domain"),
        ScanItem("5", "Invoice_May2026.pdf", "Image", RiskLevel.Safe, "3 days ago", "Clean document with zero threats")
    )

    val articles = listOf(
        Article("1", "How to Spot Sophisticated Phishing SMS", "Phishing", "4 min read", "Learn the key giveaways of fraudulent banking texts and courier scams."),
        Article("2", "UPI Collect Request Fraud Prevention", "UPI Scams", "3 min read", "Never approve incoming collect requests from unknown VPA handles."),
        Article("3", "QR Code Tampering at Retail Outlets", "QR Scams", "5 min read", "How cybercriminals overlay malicious QR stickers on legitimate merchant stands."),
        Article("4", "Fake Job Offers & Advance Fee Scams", "Job Scams", "4 min read", "Spotting fake remote work offers requiring training deposits."),
        Article("5", "Cryptocurrency Investment Traps", "Investment Scams", "6 min read", "Recognizing high-yield guaranteed return Ponzi schemes."),
        Article("6", "Lottery & Lucky Draw Impersonation", "Lottery Scams", "3 min read", "Avoiding claims that you won international sweepstakes."),
        Article("7", "OTP Sharing & SIM Swap Attacks", "OTP Scams", "4 min read", "Protecting your 6-digit verification codes from social engineering."),
        Article("8", "Fake Banking Customer Care Numbers", "Fake Customer Care", "3 min read", "Finding official helpline numbers through verified directories.")
    )

    init {
        viewModelScope.launch {
            isLoading = true
            currentUser = userRepository.getCurrentUser()
            isLoading = false
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        if (isLoading) return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = userRepository.signIn(email, pass)
            isLoading = false
            result.fold(
                onSuccess = { user ->
                    currentUser = user
                    prefsManager.setLoggedIn(true)
                    onSuccess()
                },
                onFailure = { error ->
                    errorMessage = error.localizedMessage ?: "Login failed"
                }
            )
        }
    }

    fun signUp(name: String, email: String, pass: String, onSuccess: () -> Unit) {
        if (isLoading) return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = userRepository.signUp(name, email, pass)
            isLoading = false
            result.fold(
                onSuccess = { user ->
                    currentUser = user
                    prefsManager.setLoggedIn(true)
                    onSuccess()
                },
                onFailure = { error ->
                    errorMessage = error.localizedMessage ?: "Sign up failed"
                }
            )
        }
    }

    fun resetPassword(email: String, onComplete: () -> Unit) {
        if (isLoading) return
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            successMessage = null
            val result = userRepository.resetPassword(email)
            isLoading = false
            result.fold(
                onSuccess = {
                    successMessage = "Password reset email sent. Check your inbox."
                    onComplete()
                },
                onFailure = { error ->
                    errorMessage = error.localizedMessage ?: "Failed to send reset email"
                }
            )
        }
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }

    fun logout() {
        userRepository.signOut()
        currentUser = null
        prefsManager.setLoggedIn(false)
    }

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        prefsManager.setDarkTheme(isDarkTheme)
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun setHistoryFilter(filter: String) {
        selectedHistoryFilter = filter
    }

    fun runScan(query: String, onComplete: (ScanItem) -> Unit) {
        val trimmed = query.ifBlank { "https://example-secure-domain.com" }
        val isRisky = trimmed.contains("xyz") || trimmed.contains("login") || trimmed.contains("update") || trimmed.contains("win")
        val risk = if (isRisky) RiskLevel.HighRisk else RiskLevel.Safe
        val desc = if (isRisky) "Potential phishing signature detected." else "No threats identified. Secure SSL."
        val item = ScanItem(
            id = (recentScans.size + 1).toString(),
            title = trimmed,
            type = "URL",
            riskLevel = risk,
            timestamp = "Just now",
            description = desc
        )
        currentScanResult = item
        recentScans.add(0, item)
        onComplete(item)
    }

    fun isOnboardingCompleted(): Boolean {
        return prefsManager.isOnboardingCompleted()
    }

    fun completeOnboarding() {
        prefsManager.setOnboardingCompleted(true)
    }
}
