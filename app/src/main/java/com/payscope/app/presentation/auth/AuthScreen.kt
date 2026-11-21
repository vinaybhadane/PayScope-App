package com.payscope.app.presentation.auth

import com.payscope.app.core.util.drawAmbientGlow
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.payscope.app.core.components.ButtonVariant
import com.payscope.app.core.components.PayScopeGlassCard
import com.payscope.app.core.components.PayScopePillButton
import com.payscope.app.core.theme.BorderHighlight
import com.payscope.app.core.theme.BorderSubtle
import com.payscope.app.core.theme.CardSurface
import com.payscope.app.core.theme.DangerRose
import com.payscope.app.core.theme.DeepBackground
import com.payscope.app.core.theme.ElevatedCardSurface
import com.payscope.app.core.theme.GlowCyan
import com.payscope.app.core.theme.GlowOrange
import com.payscope.app.core.theme.LocalIsDarkTheme
import com.payscope.app.core.theme.PrimaryOrange
import com.payscope.app.core.theme.SavingsTeal
import com.payscope.app.core.theme.SecondaryCyan
import com.payscope.app.core.theme.TextMuted
import com.payscope.app.core.theme.TextPrimary
import com.payscope.app.core.theme.TextSecondary
import com.payscope.app.core.theme.WarningAmber

@Composable
fun AuthScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUpload: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleSignInResult(result.data)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.NavigateToUpload -> onNavigateToUpload()
                AuthEvent.NavigateToDashboard -> onNavigateToDashboard()
                is AuthEvent.ShowError -> {}
            }
        }
    }

    val isDark = LocalIsDarkTheme.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .drawBehind {
                val glowAlpha = if (isDark) 1f else 0.35f
                drawAmbientGlow(
                    color = GlowCyan.copy(alpha = GlowCyan.alpha * glowAlpha),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.2f, size.height * 0.25f)
                )
                drawAmbientGlow(
                    color = GlowOrange.copy(alpha = GlowOrange.alpha * glowAlpha),
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.8f, size.height * 0.75f)
                )
            }
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Back Button and Accessible Theme Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ElevatedCardSurface)
                        .border(1.dp, BorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleTheme(isDark) },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(ElevatedCardSurface)
                        .border(1.dp, BorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDark) "Switch to Light theme" else "Switch to Dark theme",
                        tint = if (isDark) PrimaryOrange else SecondaryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            com.payscope.app.core.components.PayScopeLogo(size = 84.dp)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome to PayScope",
                color = TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your financial clarity starts here.",
                color = TextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Central Glass Card
            PayScopeGlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp,
                backgroundColor = CardSurface,
                borderColor = BorderSubtle
            ) {
                if (uiState.errorMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DangerRose.copy(alpha = 0.15f))
                            .border(1.dp, DangerRose.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = DangerRose,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = uiState.errorMessage!!,
                            color = DangerRose,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Continue with Google Button
                PayScopePillButton(
                    text = "Continue with Google",
                    variant = ButtonVariant.SECONDARY,
                    isLoading = uiState.isLoadingGoogle,
                    onClick = {
                        viewModel.startGoogleSignIn(context) { intent ->
                            googleSignInLauncher.launch(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Try Demo Dashboard Button
                PayScopePillButton(
                    text = "Try Demo Dashboard",
                    icon = Icons.Default.AutoAwesome,
                    variant = ButtonVariant.PRIMARY,
                    isLoading = uiState.isLoadingGuest,
                    onClick = { viewModel.onDemoDashboardClicked() },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Privacy assurance note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = SavingsTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your financial data stays private.\nNo banking credentials required.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
