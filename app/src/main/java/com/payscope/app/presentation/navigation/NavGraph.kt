package com.payscope.app.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.payscope.app.core.theme.GlowCyan
import com.payscope.app.core.theme.PrimaryOrange
import com.payscope.app.core.theme.SecondaryCyan
import com.payscope.app.presentation.assistant.AIAssistantDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.payscope.app.core.components.NavigationTab
import com.payscope.app.core.components.PayScopeFloatingBottomBar
import com.payscope.app.core.theme.DeepBackground
import com.payscope.app.presentation.auth.AuthScreen
import com.payscope.app.presentation.dashboard.DashboardScreen
import com.payscope.app.presentation.analytics.AnalyticsScreen
import com.payscope.app.presentation.profile.ProfileScreen
import com.payscope.app.presentation.savings.SavingsScreen
import com.payscope.app.presentation.subscriptions.SubscriptionsScreen
import com.payscope.app.presentation.upload.UploadScreen
import com.payscope.app.presentation.welcome.WelcomeScreen

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object Auth : Screen("auth")
    data object Upload : Screen("upload")
    data object Main : Screen("main")
}

@Composable
fun PayScopeNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Welcome.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(350)) + slideInHorizontally(
                initialOffsetX = { 80 },
                animationSpec = tween(350)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(250)) + slideOutHorizontally(
                targetOffsetX = { -80 },
                animationSpec = tween(250)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(350)) + slideInHorizontally(
                initialOffsetX = { -80 },
                animationSpec = tween(350)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(250)) + slideOutHorizontally(
                targetOffsetX = { 80 },
                animationSpec = tween(250)
            )
        }
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route)
                },
                onNavigateToDemo = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToUpload = {
                    navController.navigate(Screen.Upload.route)
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Upload.route) {
            UploadScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(navController.graph.id) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Main.route) {
            MainContainerScreen(
                onNavigateToUpload = {
                    navController.navigate(Screen.Upload.route)
                },
                onSignOut = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun MainContainerScreen(
    onNavigateToUpload: () -> Unit,
    onSignOut: () -> Unit
) {
    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var analyticsGraph by remember { mutableStateOf(false) }
    var showAiAssistant by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
    ) {
        // Tab Content with Animated Transition
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "tabTransition",
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                NavigationTab.HOME -> {
                    DashboardScreen(
                        onNavigateToUpload = onNavigateToUpload,
                        onNavigateToAuth = onSignOut,
                        onNavigateToAnalytics = { analyticsGraph = false; currentTab = NavigationTab.ANALYTICS },
                        onNavigateToGraph = { analyticsGraph = true; currentTab = NavigationTab.ANALYTICS },
                        onNavigateToProfile = { currentTab = NavigationTab.PROFILE },
                        onNavigateToSubscriptions = { currentTab = NavigationTab.SUBSCRIPTIONS }
                    )
                }
                NavigationTab.ANALYTICS -> {
                    AnalyticsScreen(initialTab = if (analyticsGraph) 1 else 0)
                }
                NavigationTab.SUBSCRIPTIONS -> {
                    SubscriptionsScreen()
                }
                NavigationTab.SAVINGS -> {
                    SavingsScreen()
                }
                NavigationTab.PROFILE -> {
                    ProfileScreen(onSignOut = onSignOut)
                }
            }
        }

        // Floating AI Advisor Symbol (present on every page)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 86.dp)
                .size(54.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = GlowCyan.copy(alpha = 0.5f)
                )
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1E293B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            SecondaryCyan,
                            PrimaryOrange
                        )
                    ),
                    shape = CircleShape
                )
                .clickable { showAiAssistant = true },
            contentAlignment = Alignment.Center
        ) {
            com.payscope.app.core.components.PayScopeLogo(size = 44.dp)
        }

        // Floating Bottom Navigation Bar
        PayScopeFloatingBottomBar(
            currentTab = currentTab,
            onTabSelected = { currentTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Global AI Assistant Dialog (accessible from any screen)
        if (showAiAssistant) {
            AIAssistantDialog(
                onDismiss = { showAiAssistant = false }
            )
        }
    }
}
