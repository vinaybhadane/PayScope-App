package com.payscope.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.first
import androidx.activity.enableEdgeToEdge
import com.payscope.app.core.theme.PayScopeTheme
import com.payscope.app.presentation.navigation.PayScopeNavGraph
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var authRepository: com.payscope.app.data.repository.AuthRepository
    @Inject lateinit var dataStoreManager: com.payscope.app.data.local.DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val initialTheme = kotlinx.coroutines.runBlocking {
            dataStoreManager.ensureDefaultLightTheme()
            dataStoreManager.themeModeFlow.first()
        }
        setContent {
            val systemDark = isSystemInDarkTheme()
            val themeMode by dataStoreManager.themeModeFlow.collectAsState(initial = initialTheme)
            val darkTheme = when (themeMode) {
                "dark" -> true
                "system" -> systemDark
                else -> false // Light theme is default
            }

            PayScopeTheme(darkTheme = darkTheme) {
                var initialRoute by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    initialRoute = if (authRepository.currentSessionFlow.first() != null) "main" else "welcome"
                }
                initialRoute?.let { PayScopeNavGraph(startDestination = it) }
            }
        }
    }
}
