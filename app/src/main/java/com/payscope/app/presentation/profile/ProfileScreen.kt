package com.payscope.app.presentation.profile

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.payscope.app.core.components.ButtonVariant
import com.payscope.app.core.components.PayScopeGlassCard
import com.payscope.app.core.components.PayScopeLogo
import com.payscope.app.core.components.PayScopePillButton
import com.payscope.app.core.components.StatusMessage
import com.payscope.app.core.components.UserAvatar
import com.payscope.app.core.theme.BorderSubtle
import com.payscope.app.core.theme.CardSurface
import com.payscope.app.core.theme.DangerRose
import com.payscope.app.core.theme.DeepBackground
import com.payscope.app.core.theme.ElevatedCardSurface
import com.payscope.app.core.theme.GlowCyan
import com.payscope.app.core.theme.GlowOrange
import com.payscope.app.core.theme.PrimaryOrange
import com.payscope.app.core.theme.SavingsTeal
import com.payscope.app.core.theme.SecondaryCyan
import com.payscope.app.core.theme.TextMuted
import com.payscope.app.core.theme.TextPrimary
import com.payscope.app.core.theme.TextSecondary
import com.payscope.app.core.util.FinancialAuditPdfGenerator
import com.payscope.app.core.util.drawAmbientGlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileEvent.NavigateToAuth -> onSignOut()
                is ProfileEvent.ShowMessage -> Toast.makeText(context, event.text, Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .drawBehind {
                drawAmbientGlow(
                    color = GlowCyan,
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.85f, size.height * 0.15f)
                )
                drawAmbientGlow(
                    color = GlowOrange,
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.15f, size.height * 0.7f)
                )
            }
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            uiState.errorMessage?.let { message ->
                item {
                    StatusMessage(message) { viewModel.loadProfile() }
                }
            }

            // Screen Header with Back Action if accessible as subpage
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        PayScopeLogo(size = 40.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Profile & Settings",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Preferences, Appearance & Financial Audit",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // User Info Card
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    PayScopeGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                photoUrl = uiState.userPhotoUrl,
                                size = 56.dp
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.userName,
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = uiState.email,
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (uiState.isGuest) ElevatedCardSurface else SavingsTeal.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (uiState.isGuest) "Guest Demo Mode" else "Authenticated Workspace",
                                        color = if (uiState.isGuest) SecondaryCyan else SavingsTeal,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Grid
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ElevatedCardSurface)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Statements", color = TextMuted, fontSize = 11.sp)
                                Text(text = "${uiState.statementsCount}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Subscriptions", color = TextMuted, fontSize = 11.sp)
                                Text(text = "${uiState.activeSubscriptionsCount}", color = PrimaryOrange, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Currency", color = TextMuted, fontSize = 11.sp)
                                Text(text = "INR (₹)", color = SecondaryCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Appearance & Theme Section (Requirement 2.F)
            item {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    PayScopeGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp
                    ) {
                        Text(
                            text = "Appearance & Theme",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Choose light or dark mode across all screens",
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(ElevatedCardSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Dark Mode Option
                            val isDark = uiState.themeMode == "dark"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) CardSurface else Color.Transparent)
                                    .border(
                                        width = if (isDark) 1.dp else 0.dp,
                                        color = if (isDark) PrimaryOrange else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.onSetThemeMode("dark") },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DarkMode,
                                        contentDescription = null,
                                        tint = if (isDark) PrimaryOrange else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Dark",
                                        color = if (isDark) TextPrimary else TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = if (isDark) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }

                            // Light Mode Option
                            val isLight = uiState.themeMode == "light"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isLight) CardSurface else Color.Transparent)
                                    .border(
                                        width = if (isLight) 1.dp else 0.dp,
                                        color = if (isLight) PrimaryOrange else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.onSetThemeMode("light") },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = if (isLight) PrimaryOrange else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Light",
                                        color = if (isLight) TextPrimary else TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = if (isLight) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }

                            // System Default Option
                            val isSystem = uiState.themeMode == "system"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSystem) CardSurface else Color.Transparent)
                                    .border(
                                        width = if (isSystem) 1.dp else 0.dp,
                                        color = if (isSystem) PrimaryOrange else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.onSetThemeMode("system") },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SettingsBrightness,
                                        contentDescription = null,
                                        tint = if (isSystem) PrimaryOrange else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Auto",
                                        color = if (isSystem) TextPrimary else TextMuted,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSystem) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Financial Audit & Actions
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PayScopePillButton(
                        text = if (uiState.isExporting) "Generating PDF..." else "Financial Audit Export",
                        icon = Icons.Default.Download,
                        variant = ButtonVariant.SECONDARY,
                        isLoading = uiState.isExporting,
                        onClick = { viewModel.onExportData() },
                        modifier = Modifier.fillMaxWidth()
                    )

                    PayScopePillButton(
                        text = "Delete Workspace Data",
                        icon = Icons.Default.Delete,
                        variant = ButtonVariant.DANGER,
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    PayScopePillButton(
                        text = "Sign Out",
                        variant = ButtonVariant.GHOST,
                        onClick = { viewModel.onSignOut() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // PDF Generation Success Dialog
        uiState.exportPdfFile?.let { pdfFile ->
            BasicAlertDialog(onDismissRequest = { viewModel.onDismissExport() }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(CardSurface)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(26.dp))
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SavingsTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = SavingsTeal,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Financial Audit Report (PDF)",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "A formatted PDF report has been generated with your real verified statement audit data.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // File Info Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ElevatedCardSurface)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pdfFile.name,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${pdfFile.length() / 1024} KB • PDF Document",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Open PDF button
                            PayScopePillButton(
                                text = "Open PDF",
                                icon = Icons.Default.OpenInNew,
                                variant = ButtonVariant.PRIMARY,
                                onClick = {
                                    FinancialAuditPdfGenerator.openPdf(context, pdfFile)
                                    viewModel.onDismissExport()
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // Share button
                            PayScopePillButton(
                                text = "Share",
                                icon = Icons.Default.Share,
                                variant = ButtonVariant.SECONDARY,
                                onClick = {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            pdfFile
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Financial Audit PDF"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not share PDF.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Dismiss",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clickable { viewModel.onDismissExport() }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmation) {
            BasicAlertDialog(onDismissRequest = { showDeleteConfirmation = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(CardSurface)
                        .border(1.dp, DangerRose.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = DangerRose,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Delete All Imported Data?",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This will permanently purge all statement transactions and detected commitments from your isolated tenant workspace.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            PayScopePillButton(
                                text = "Cancel",
                                variant = ButtonVariant.SECONDARY,
                                onClick = { showDeleteConfirmation = false },
                                modifier = Modifier.weight(1f)
                            )
                            PayScopePillButton(
                                text = "Delete",
                                variant = ButtonVariant.DANGER,
                                onClick = {
                                    showDeleteConfirmation = false
                                    viewModel.onDeleteWorkspaceData()
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
