package com.payscope.app.core.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalIsDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    primaryContainer = DarkElevatedCardSurface,
    onPrimaryContainer = DarkTextPrimary,
    secondary = DarkSecondaryCyan,
    onSecondary = DarkDeepBackground,
    secondaryContainer = DarkElevatedCardSurface,
    onSecondaryContainer = DarkSecondaryCyan,
    tertiary = SavingsTeal,
    onTertiary = DarkDeepBackground,
    background = DarkDeepBackground,
    onBackground = DarkTextPrimary,
    surface = DarkCardSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkElevatedCardSurface,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorderSubtle,
    outlineVariant = DarkBorderSubtle,
    error = DangerRose,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryOrange,
    onPrimary = Color.White,
    primaryContainer = LightElevatedCardSurface,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightSecondaryCyan,
    onSecondary = Color.White,
    secondaryContainer = LightElevatedCardSurface,
    onSecondaryContainer = LightSecondaryCyan,
    tertiary = SavingsTeal,
    onTertiary = Color.White,
    background = LightDeepBackground,
    onBackground = LightTextPrimary,
    surface = LightCardSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightElevatedCardSurface,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorderSubtle,
    outlineVariant = LightBorderSubtle,
    error = DangerRose,
    onError = Color.White
)

@Composable
fun PayScopeTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val bgColor = if (darkTheme) DarkDeepBackground else LightDeepBackground
            window.statusBarColor = bgColor.toArgb()
            window.navigationBarColor = bgColor.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
