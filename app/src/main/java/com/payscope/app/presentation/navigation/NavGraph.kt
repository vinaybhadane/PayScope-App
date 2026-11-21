package com.payscope.app.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
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
            fadeIn(animationSpec = tween(260)) + slideInHorizontally(
                initialOffsetX = { 80 },
                animationSpec = tween(260)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(180)) + slideOutHorizontally(
                targetOffsetX = { -80 },
                animationSpec = tween(180)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(260)) + slideInHorizontally(
                initialOffsetX = { -80 },
                animationSpec = tween(260)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(180)) + slideOutHorizontally(
                targetOffsetX = { 80 },
                animationSpec = tween(180)
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
    var currentTab by rememberSaveable { mutableStateOf(NavigationTab.HOME) }
    var secondaryScreen by rememberSaveable { mutableStateOf<SecondaryScreen?>(null) }
    var showAiAssistant by rememberSaveable { mutableStateOf(false) }
    val screenState = rememberSaveableStateHolder()

    BackHandler(enabled = secondaryScreen != null || currentTab != NavigationTab.HOME) {
        if (secondaryScreen != null) secondaryScreen = null
        else currentTab = NavigationTab.HOME
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = DeepBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            PayScopeFloatingBottomBar(
                currentTab = currentTab,
                onTabSelected = { selected ->
                    if (selected == NavigationTab.AI) showAiAssistant = true
                    else {
                        secondaryScreen = null
                        currentTab = selected
                    }
                }
            )
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentTab to secondaryScreen,
            contentKey = { (tab, secondary) -> secondary?.name ?: tab.name },
            transitionSpec = {
                (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 24 }) togetherWith
                    fadeOut(tween(120))
            },
            label = "mainNavigation",
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
        ) { (tab, secondary) ->
            screenState.SaveableStateProvider(secondary?.name ?: tab.name) {
                if (secondary != null) {
                    when (secondary) {
                        SecondaryScreen.ANALYTICS -> AnalyticsScreen(onNavigateBack = { secondaryScreen = null })
                        SecondaryScreen.SUBSCRIPTIONS -> SubscriptionsScreen(onNavigateBack = { secondaryScreen = null })
                        SecondaryScreen.SAVINGS_SIMULATOR -> SavingsScreen(onNavigateBack = { secondaryScreen = null })
                        SecondaryScreen.KNOWLEDGE_GRAPH -> RelationshipGraphScreen(onNavigateBack = { secondaryScreen = null })
                        SecondaryScreen.PROFILE -> ProfileScreen(onSignOut = onSignOut, onNavigateBack = { secondaryScreen = null })
                    }
                } else {
                    when (tab) {
                        NavigationTab.HOME, NavigationTab.AI -> DashboardScreen(
                            onNavigateToUpload = onNavigateToUpload,
                            onNavigateToAuth = onSignOut,
                            onNavigateToAnalytics = { secondaryScreen = SecondaryScreen.ANALYTICS },
                            onNavigateToSubscriptions = { secondaryScreen = SecondaryScreen.SUBSCRIPTIONS },
                            onNavigateToSavings = { secondaryScreen = SecondaryScreen.SAVINGS_SIMULATOR },
                            onNavigateToGraph = { secondaryScreen = SecondaryScreen.KNOWLEDGE_GRAPH },
                            onNavigateToProfile = { secondaryScreen = SecondaryScreen.PROFILE }
                        )
                        NavigationTab.SEARCH -> SearchScreen(onNavigateToSubscriptions = { secondaryScreen = SecondaryScreen.SUBSCRIPTIONS })
                        NavigationTab.ALERTS -> AlertsScreen(onNavigateToSubscriptions = { secondaryScreen = SecondaryScreen.SUBSCRIPTIONS })
                        NavigationTab.HISTORY -> HistoryScreen(
                            onNavigateToUpload = onNavigateToUpload,
                            onOpenAiAssistant = { showAiAssistant = true }
                        )
                    }
                }
            }
        }
    }

    if (showAiAssistant) AIAssistantDialog(onDismiss = { showAiAssistant = false })
}
