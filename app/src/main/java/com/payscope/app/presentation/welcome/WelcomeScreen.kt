package com.payscope.app.presentation.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.payscope.app.core.components.*
import com.payscope.app.core.theme.*

@Composable
fun WelcomeScreen(onNavigateToAuth: () -> Unit, onNavigateToDemo: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.events.collect {
            when (it) {
                WelcomeEvent.NavigateToAuth -> onNavigateToAuth()
                WelcomeEvent.NavigateToDemoDashboard -> onNavigateToDemo()
            }
        }
    }
    Box(Modifier.fillMaxSize().background(DeepBackground)) {
        Canvas(Modifier.fillMaxSize()) {
            val orangeCenter = Offset(size.width, size.height * .28f)
            val cyanCenter = Offset(0f, size.height * .65f)
            drawCircle(Brush.radialGradient(listOf(GlowOrange, Color.Transparent), orangeCenter, size.width),
                radius = size.width, center = orangeCenter)
            drawCircle(Brush.radialGradient(listOf(GlowCyan, Color.Transparent), cyanCenter, size.width),
                radius = size.width, center = cyanCenter)
        }
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PayScopeLogo(size = 44.dp)
                    Text("PayScope", Modifier.padding(start = 10.dp), color = TextPrimary,
                        fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = (-.6).sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = SavingsTeal, modifier = Modifier.size(13.dp))
                    Text("No bank login", Modifier.padding(start = 5.dp), color = TextSecondary, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(40.dp))
            Text("YOUR MONEY. IN FOCUS.", color = SecondaryCyan, fontSize = 10.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(14.dp))
            Text("A clearer view of\nevery payment.", color = TextPrimary, fontSize = 39.sp,
                lineHeight = 45.sp, letterSpacing = (-1.5).sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(15.dp))
            Text("Turn your statements into a clear picture of subscriptions, upcoming bills, and opportunities to save.",
                color = TextSecondary, fontSize = 15.sp, lineHeight = 23.sp)
            Spacer(Modifier.height(26.dp))
            PayScopeGlassCard(Modifier.fillMaxWidth(), cornerRadius = 24.dp, borderColor = BorderHighlight) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(PrimaryOrange.copy(alpha = .12f)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Description, null, tint = PrimaryOrange)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("One statement. More clarity.", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("PDF / CSV / Excel / JSON", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom) {
                    listOf(.32f, .52f, .43f, .75f, .60f, .9f, .72f, 1f, .83f, .65f, .91f, .78f).forEachIndexed { i, h ->
                        Box(Modifier.weight(1f).height((h * 47).dp).clip(RoundedCornerShape(3.dp))
                            .background(if (i > 8) SecondaryCyan.copy(alpha = .7f) else PrimaryOrange.copy(alpha = .18f + i * .05f)))
                    }
                }
                Spacer(Modifier.height(17.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("IMPORT", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.sp)
                    Text("ANALYZE", color = TextMuted, fontSize = 9.sp, letterSpacing = 1.sp)
                    Text("TAKE CONTROL", color = SecondaryCyan, fontSize = 9.sp, letterSpacing = 1.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
            Benefit(Icons.Default.AutoAwesome, "Find recurring charges", "Identify patterns across your payment history.")
            Spacer(Modifier.height(16.dp))
            Benefit(Icons.Default.Insights, "Make informed decisions", "Review price changes and model your savings.")
            Spacer(Modifier.height(28.dp))
            PayScopePillButton(text = "Get started", onClick = { viewModel.onGetStartedClicked() },
                modifier = Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Filled.ArrowForward)
            Spacer(Modifier.height(10.dp))
            PayScopePillButton(text = "Explore sample workspace", onClick = { viewModel.onExploreDemoClicked() },
                modifier = Modifier.fillMaxWidth(), variant = ButtonVariant.SECONDARY, isLoading = state.isStartingDemo)
            state.errorMessage?.let {
                Text(it, color = DangerRose, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
            }
            Text("Your statement is sent to PayScope and Gemini for analysis. You control your data.",
                color = TextMuted, fontSize = 10.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = SecondaryCyan, modifier = Modifier.size(21.dp))
        Column(Modifier.padding(start = 13.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(description, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}
