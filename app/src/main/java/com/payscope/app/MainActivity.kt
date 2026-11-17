package com.payscope.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.first
import androidx.activity.enableEdgeToEdge
import com.payscope.app.core.theme.PayScopeTheme
import com.payscope.app.presentation.navigation.PayScopeNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @javax.inject.Inject lateinit var authRepository: com.payscope.app.data.repository.AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PayScopeTheme {
                var initialRoute by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    initialRoute = if (authRepository.currentSessionFlow.first() != null) "main" else "welcome"
                }
                initialRoute?.let { PayScopeNavGraph(startDestination = it) }
            }
        }
    }
}
