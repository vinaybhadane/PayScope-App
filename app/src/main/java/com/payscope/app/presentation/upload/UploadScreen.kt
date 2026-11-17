package com.payscope.app.presentation.upload

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.payscope.app.core.components.*
import com.payscope.app.core.theme.*
import com.payscope.app.domain.model.UploadStage

@Composable
fun UploadScreen(onNavigateBack: () -> Unit, onNavigateToDashboard: () -> Unit,
    viewModel: UploadViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            viewModel.onFileSelected(uri)
        }
    }
    val selectFile = { picker.launch(arrayOf("application/pdf", "text/csv", "text/comma-separated-values",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/json")) }
    LaunchedEffect(state.completed) { if (state.completed) onNavigateToDashboard() }
    BackHandler(enabled = state.isProcessing) { /* Keep the import alive until its result is known. */ }

    Column(Modifier.fillMaxSize().background(DeepBackground).statusBarsPadding().navigationBarsPadding()
        .verticalScroll(rememberScrollState()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack, enabled = !state.isProcessing) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = if (state.isProcessing) TextMuted else TextPrimary)
            }
            Spacer(Modifier.weight(1f))
            PayScopeLogo(size = 48.dp)
        }
        Spacer(Modifier.height(28.dp))
        Text(if (state.isProcessing) "Bringing your statement into focus" else "Import your statement",
            color = TextPrimary, fontSize = 28.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(if (state.isProcessing) "We will open your dashboard once your transactions are saved and ready to display."
            else "Choose a file to save your transactions and analyze recurring payments with Gemini.",
            color = TextSecondary, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))

        if (state.isProcessing) {
            PayScopeGlassCard(Modifier.fillMaxWidth(), cornerRadius = 24.dp, borderColor = BorderHighlight) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(30.dp), color = PrimaryOrange, strokeWidth = 3.dp)
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(state.currentStage?.title ?: "Importing statement", color = TextPrimary,
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        state.selectedFile?.let { Text(it.name, color = TextMuted, fontSize = 11.sp, maxLines = 2) }
                    }
                }
                Text(when (state.currentStage) {
                    UploadStage.READING -> "Opening and checking the selected document."
                    UploadStage.PREPARING, UploadStage.COMPLETED -> "Verifying the saved import and loading your latest dashboard from the database."
                    else -> "Extracting transactions, saving your records, and reviewing payment patterns with Gemini."
                }, color = TextSecondary, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 18.dp))
                HorizontalDivider(Modifier.padding(vertical = 20.dp), color = BorderSubtle)
                val stage = when (state.currentStage) {
                    UploadStage.READING -> 0
                    UploadStage.PREPARING, UploadStage.COMPLETED -> 2
                    else -> 1
                }
                listOf("Read document", "Save and analyze transactions", "Load saved data into your dashboard").forEachIndexed { i, text ->
                    Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (i < stage) Icon(Icons.Default.CheckCircle, null, tint = SavingsTeal, modifier = Modifier.size(21.dp))
                        else if (i == stage) CircularProgressIndicator(Modifier.size(19.dp), color = SecondaryCyan, strokeWidth = 2.dp)
                        else Icon(Icons.Default.RadioButtonUnchecked, null, tint = TextMuted, modifier = Modifier.size(21.dp))
                        Text(text, color = if (i <= stage) TextPrimary else TextMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
            Text("Scanned PDFs can take a few minutes. Please keep this screen open.", color = TextMuted,
                fontSize = 11.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 20.dp))
        } else {
            PayScopeGlassCard(Modifier.fillMaxWidth(), cornerRadius = 24.dp, borderColor = BorderHighlight, onClick = selectFile) {
                Column(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.UploadFile, null, tint = PrimaryOrange, modifier = Modifier.size(42.dp))
                    Text("Select a statement", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 16.dp))
                    Text("PDF / CSV / Excel / JSON", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                    Text("Up to 10 MB", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            state.errorMessage?.let { message ->
                Text(message, color = WarningAmber, fontSize = 12.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 20.dp))
                if (state.selectedFile != null) PayScopePillButton(text = "Retry import", onClick = { viewModel.onRetryImport() },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
            }
            Spacer(Modifier.height(20.dp))
            PayScopePillButton(text = "Browse files", onClick = selectFile, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            PayScopePillButton(text = "Use sample statement", onClick = { viewModel.onUseSampleStatement() },
                variant = ButtonVariant.SECONDARY, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = onNavigateToDashboard) { Text("Skip for now", color = SecondaryCyan, fontSize = 12.sp) }
            Text("Your statement is sent to PayScope and Gemini for analysis. No bank login is required.",
                color = TextMuted, fontSize = 11.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
