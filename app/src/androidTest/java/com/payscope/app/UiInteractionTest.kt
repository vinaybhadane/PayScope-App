package com.payscope.app

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.payscope.app.core.components.*
import com.payscope.app.core.theme.PayScopeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun buttonTapRunsOnce() {
        var taps = 0
        compose.setContent { PayScopeTheme { PayScopePillButton("Import", { taps++ }, Modifier.testTag("button").width(180.dp)) } }
        compose.onNodeWithTag("button").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, taps) }
    }

    @Test fun disabledButtonCannotRunAnAction() {
        var taps = 0
        compose.setContent { PayScopeTheme { PayScopePillButton("Import", { taps++ }, Modifier.testTag("button").width(180.dp), enabled = false) } }
        compose.onNodeWithTag("button").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, taps) }
    }

    @Test fun loadingButtonPreventsDuplicateSubmissions() {
        var taps = 0
        compose.setContent { PayScopeTheme { PayScopePillButton("Import", { taps++ }, Modifier.testTag("button").width(180.dp), isLoading = true) } }
        compose.onNodeWithTag("button").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, taps) }
    }

    @Test fun cancelledPressDoesNotClickAndNextTapWorks() {
        var taps = 0
        compose.setContent { PayScopeTheme { PayScopePillButton("Import", { taps++ }, Modifier.testTag("button").width(180.dp)) } }
        compose.onNodeWithTag("button").performTouchInput {
            swipe(center, Offset(width * 1.6f, center.y), 200)
        }
        compose.runOnIdle { assertEquals(0, taps) }
        compose.onNodeWithTag("button").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, taps) }
    }

    @Test fun dockExposesSelectionAndAssistantDoesNotSwitchTabs() {
        var assistantOpens = 0
        compose.setContent {
            var tab by remember { mutableStateOf(NavigationTab.HOME) }
            PayScopeTheme {
                PayScopeFloatingBottomBar(tab, {
                    if (it == NavigationTab.AI) assistantOpens++ else tab = it
                })
            }
        }
        compose.onNodeWithText("Home").assertIsSelected()
        compose.onNodeWithText("Search").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Open PayScope AI assistant").performClick()
        compose.onNodeWithText("Search").assertIsSelected()
        compose.runOnIdle { assertEquals(1, assistantOpens) }
    }

    @Test fun largeTextButtonGrowsToFitLabel() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                PayScopeTheme(darkTheme = true) {
                    PayScopePillButton("Continue with your account", {}, Modifier.testTag("button").width(220.dp))
                }
            }
        }
        compose.onNodeWithText("Continue with your account").assertIsDisplayed()
        compose.onNodeWithTag("button").assertHeightIsAtLeast(60.dp)
    }
}
