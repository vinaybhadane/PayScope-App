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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var authRepository: com.payscope.app.data.repository.AuthRepository
    @Inject lateinit var dataStoreManager: com.payscope.app.data.local.DataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        var readyToDraw = false
        val rootView = findViewById<android.view.View>(android.R.id.content)
        val preDraw = object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (readyToDraw) rootView.viewTreeObserver.removeOnPreDrawListener(this)
                return readyToDraw
            }
        }
        rootView.viewTreeObserver.addOnPreDrawListener(preDraw)
        lifecycleScope.launch {
            dataStoreManager.ensureDefaultLightTheme()
            val initialTheme = dataStoreManager.themeModeFlow.first()
            val initialRoute = if (authRepository.currentSessionFlow.first() != null) "main" else "welcome"
            setContent {
                val systemDark = isSystemInDarkTheme()
                val themeMode by dataStoreManager.themeModeFlow.collectAsState(initial = initialTheme)
                val darkTheme = when (themeMode) {
                    "dark" -> true
                    "system" -> systemDark
                    else -> false
                }
                PayScopeTheme(darkTheme = darkTheme) {
                    PayScopeNavGraph(startDestination = initialRoute)
                }
            }
            readyToDraw = true
        }
    }
}
