package com.marwadiuniversity.trustlens.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.marwadiuniversity.trustlens.ui.components.PrimaryTopBar
import com.marwadiuniversity.trustlens.ui.screens.*
import com.marwadiuniversity.trustlens.viewmodel.MainViewModel
import com.marwadiuniversity.trustlens.viewmodel.RiskAnalysisViewModel

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun NavGraph(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val riskAnalysisViewModel: RiskAnalysisViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem("Home", Screen.Home.route, Icons.Default.Home),
        BottomNavItem("Scan", Screen.Scan.route, Icons.Default.Search),
        BottomNavItem("History", Screen.History.route, Icons.Default.History),
        BottomNavItem("Learn", Screen.Learn.route, Icons.Default.MenuBook),
        BottomNavItem("Profile", Screen.Profile.route, Icons.Default.Person)
    )

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }
    val showTopBar = showBottomBar

    Scaffold(
        topBar = {
            if (showTopBar) {
                PrimaryTopBar(
                    isDarkTheme = viewModel.isDarkTheme,
                    onToggleTheme = { viewModel.toggleTheme() },
                    onProfileClick = { navController.navigate(Screen.Profile.route) }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = selected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    viewModel = viewModel,
                    onNavigateToOnboarding = {
                        navController.navigate(Screen.Onboarding.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    viewModel = viewModel,
                    onFinishOnboarding = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToSignup = {
                        navController.navigate(Screen.Signup.route)
                    }
                )
            }
            composable(Screen.Signup.route) {
                SignupScreen(
                    viewModel = viewModel,
                    onSignupSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) }
                )
            }
            composable(Screen.Scan.route) {
                ScanScreen(
                    viewModel = viewModel,
                    riskViewModel = riskAnalysisViewModel,
                    onNavigateToCameraScanner = { navController.navigate(Screen.CameraScanner.route) },
                    onNavigateToResult = { navController.navigate(Screen.RiskAnalysis.route) }
                )
            }
            composable(Screen.CameraScanner.route) {
                CameraScannerScreen(
                    riskViewModel = riskAnalysisViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToResult = {
                        navController.navigate(Screen.RiskAnalysis.route) {
                            popUpTo(Screen.Scan.route)
                        }
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    onNavigateToDetail = { scanId ->
                        navController.navigate(Screen.ScanDetail.createRoute(scanId))
                    },
                    onNavigateToReport = {
                        navController.navigate(Screen.SecurityReport.route)
                    }
                )
            }
            composable(
                route = Screen.ScanDetail.route,
                arguments = listOf(navArgument("scanId") { type = NavType.LongType })
            ) { backStackEntry ->
                val scanId = backStackEntry.arguments?.getLong("scanId") ?: 0L
                ScanDetailScreen(
                    scanId = scanId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SecurityReport.route) {
                SecurityReportScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Learn.route) {
                LearnScreen(viewModel = viewModel)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.RiskAnalysis.route) {
                RiskAnalysisScreen(
                    viewModel = viewModel,
                    riskViewModel = riskAnalysisViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
