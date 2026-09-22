package com.marwadiuniversity.trustlens.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Home : Screen("home")
    object Scan : Screen("scan")
    object History : Screen("history")
    object Learn : Screen("learn")
    object Profile : Screen("profile")
    object RiskAnalysis : Screen("risk_analysis")
    object CameraScanner : Screen("camera_scanner")
    object ScanDetail : Screen("scan_detail/{scanId}") {
        fun createRoute(scanId: Long) = "scan_detail/$scanId"
    }
    object SecurityReport : Screen("security_report")
}
