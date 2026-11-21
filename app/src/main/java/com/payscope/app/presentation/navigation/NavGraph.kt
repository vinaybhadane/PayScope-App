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
import com.payscope.app.presentation.alerts.AlertsScreen
import com.payscope.app.presentation.analytics.AnalyticsScreen
import com.payscope.app.presentation.assistant.AIAssistantDialog
import com.payscope.app.presentation.auth.AuthScreen
import com.payscope.app.presentation.dashboard.DashboardScreen
import com.payscope.app.presentation.graph.RelationshipGraphScreen
import com.payscope.app.presentation.history.HistoryScreen
import com.payscope.app.presentation.profile.ProfileScreen
import com.payscope.app.presentation.savings.SavingsScreen
import com.payscope.app.presentation.search.SearchScreen
import com.payscope.app.presentation.subscriptions.SubscriptionsScreen
import com.payscope.app.presentation.upload.UploadScreen
import com.payscope.app.presentation.welcome.WelcomeScreen

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object Auth : Screen("auth")
    data object Upload : Screen("upload")
    data object Main : Screen("main")
}

enum class SecondaryScreen {
    ANALYTICS,
    SUBSCRIPTIONS,
    SAVINGS_SIMULATOR,
    KNOWLEDGE_GRAPH,
    PROFILE
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
    var secondaryScreen by remember { mutableStateOf<SecondaryScreen?>(null) }
    var showAiAssistant by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
    ) {
        if (secondaryScreen != null) {
            when (secondaryScreen) {
                SecondaryScreen.ANALYTICS -> {
                    AnalyticsScreen(
                        onNavigateBack = { secondaryScreen = null }
                    )
                }
                SecondaryScreen.SUBSCRIPTIONS -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        SubscriptionsScreen()
                    }
                }
                SecondaryScreen.SAVINGS_SIMULATOR -> {
                    SavingsScreen(
                        onNavigateBack = { secondaryScreen = null }
                    )
                }
                SecondaryScreen.KNOWLEDGE_GRAPH -> {
                    RelationshipGraphScreen(
                        onNavigateBack = { secondaryScreen = null }
                    )
                }
                SecondaryScreen.PROFILE -> {
                    ProfileScreen(
                        onSignOut = onSignOut,
                        onNavigateBack = { secondaryScreen = null }
                    )
                }
                null -> Unit
            }
        } else {
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
                            onNavigateToAnalytics = { secondaryScreen = SecondaryScreen.ANALYTICS },
                            onNavigateToSubscriptions = { secondaryScreen = SecondaryScreen.SUBSCRIPTIONS },
                            onNavigateToSavings = { secondaryScreen = SecondaryScreen.SAVINGS_SIMULATOR },
                            onNavigateToGraph = { secondaryScreen = SecondaryScreen.KNOWLEDGE_GRAPH },
                            onNavigateToProfile = { secondaryScreen = SecondaryScreen.PROFILE }
                        )
                    }
                    NavigationTab.SEARCH -> {
                        SearchScreen(
                            onNavigateToSubscriptions = { secondaryScreen = SecondaryScreen.SUBSCRIPTIONS }
                        )
                    }
                    NavigationTab.AI -> {
                        // Center prominent AI action: triggers assistant and stays on previous or Home
                        DashboardScreen(
                            onNavigateToUpload = onNavigateToUpload,
                            onNavigateToAuth = onSignOut,
                            onNavigateToAnalytics = { secondaryScreen = SecondaryScreen.ANALYTICS },
                            onNavigateToSubscriptions = { secondaryScreen = SecondaryScreen.SUBSCRIPTIONS },
                            onNavigateToSavings = { secondaryScreen = SecondaryScreen.SAVINGS_SIMULATOR },
                            onNavigateToGraph = { secondaryScreen = SecondaryScreen.KNOWLEDGE_GRAPH },
                            onNavigateToProfile = { secondaryScreen = SecondaryScreen.PROFILE }
                        )
                    }
                    NavigationTab.ALERTS -> {
                        AlertsScreen(
                            onNavigateToSubscriptions = { secondaryScreen = SecondaryScreen.SUBSCRIPTIONS }
                        )
                    }
                    NavigationTab.HISTORY -> {
                        HistoryScreen(
                            onNavigateToUpload = onNavigateToUpload,
                            onOpenAiAssistant = { showAiAssistant = true }
                        )
                    }
                }
            }
        }

        // Floating Bottom Navigation Bar (5 destinations per Requirement 2.B & Screenshot 1)
        PayScopeFloatingBottomBar(
            currentTab = currentTab,
            onTabSelected = { selected ->
                if (selected == NavigationTab.AI) {
                    showAiAssistant = true
                } else {
                    secondaryScreen = null
                    currentTab = selected
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Global AI Assistant Dialog (triggered from center bottom nav action or Spend Insights)
        if (showAiAssistant) {
            AIAssistantDialog(
                onDismiss = { showAiAssistant = false }
            )
        }
    }
}
