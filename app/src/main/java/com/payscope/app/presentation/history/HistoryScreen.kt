package com.payscope.app.presentation.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.payscope.app.core.components.ButtonVariant
import com.payscope.app.core.components.PayScopeGlassCard
import com.payscope.app.core.components.PayScopePillButton
import com.payscope.app.core.components.StatusMessage
import com.payscope.app.core.theme.BorderHighlight
import com.payscope.app.core.theme.BorderSubtle
import com.payscope.app.core.theme.CardSurface
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
import com.payscope.app.core.util.bounceClick
import com.payscope.app.core.util.drawAmbientGlow
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToUpload: () -> Unit = {},
    onOpenAiAssistant: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    val inrFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    val filteredStatements = state.statements.filter {
        state.searchQuery.isBlank() ||
                it.fileName.contains(state.searchQuery, ignoreCase = true) ||
                it.fileType.contains(state.searchQuery, ignoreCase = true) ||
                it.uploadedAt.contains(state.searchQuery, ignoreCase = true)
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
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.loadHistory(forceRefresh = true) },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header: "History" title (Import Statement removed per Addendum 03 - Req 6)
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "History",
                            color = TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.6).sp
                        )
                        Text(
                            text = "Imported statement records & summaries",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Search Bar with Filter Icon (Screenshot 2 reference)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        BasicTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = TextPrimary,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(PrimaryOrange),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (state.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search statements, banks & records...",
                                        color = TextMuted,
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        if (state.searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { viewModel.onSearchQueryChanged("") }
                            )
                        }
                    }
                }

                // Spend Insights -> Ask PayScope AI Highlight Card (Screenshot 2 reference)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(ElevatedCardSurface, CardSurface)
                                )
                            )
                            .border(1.dp, BorderHighlight, RoundedCornerShape(20.dp))
                            .bounceClick(onClick = onOpenAiAssistant)
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Spend Insights",
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = PrimaryOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Ask PayScope AI to analyze recurrence & find hidden charges",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryOrange.copy(alpha = 0.15f))
                                    .border(1.dp, PrimaryOrange.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = PrimaryOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Group Header: Month / Totals (Screenshot 2 reference)
                state.activity?.let { activity ->
                    if (activity.totals.isNotEmpty()) {
                        val totals = activity.totals.first()
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Statements Overview",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = "Total Out: ${inrFormatter.format(totals.debits.toLong())}",
                                    color = PrimaryOrange,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Error Message if present
                state.errorMessage?.let { msg ->
                    item {
                        StatusMessage(msg) { viewModel.loadHistory(forceRefresh = true) }
                    }
                }

                // Statements List (Expandable rows per Requirement 2.D & Screenshot 2)
                if (filteredStatements.isEmpty() && !state.isLoading) {
                    item {
                        PayScopeGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = SecondaryCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = if (state.searchQuery.isEmpty()) "No imported statements yet" else "No statements match '${state.searchQuery}'",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (state.searchQuery.isEmpty())
                                        "Imported statement records and transaction breakdowns will appear here."
                                    else
                                        "No records matched your search query. Try another search term.",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                } else {
                    items(filteredStatements, key = { it.id }) { stmt ->
                        val isExpanded = state.expandedStatementId == stmt.id

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(CardSurface)
                                .border(1.dp, if (isExpanded) BorderHighlight else BorderSubtle, RoundedCornerShape(18.dp))
                                .animateContentSize(animationSpec = tween(durationMillis = 150))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Statement Row (Screenshot 2 pattern)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onToggleExpandStatement(stmt.id) }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Statement Left Icon with angled arrow
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(ElevatedCardSurface),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NorthEast,
                                            contentDescription = null,
                                            tint = PrimaryOrange,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    // Statement Metadata
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stmt.fileName,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Imported on ${stmt.uploadedAt} • ${stmt.fileType}",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Right: record count & Expand Chevron
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${stmt.recordCount} records",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "Collapse summary" else "Expand summary",
                                            tint = SecondaryCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Expandable Statement Summary (Requirement 2.D) - Fast 150ms expand/contract
                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = fadeIn(animationSpec = tween(100)) + expandVertically(animationSpec = tween(150)),
                                    exit = fadeOut(animationSpec = tween(100)) + shrinkVertically(animationSpec = tween(150))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(ElevatedCardSurface.copy(alpha = 0.5f))
                                            .padding(horizontal = 16.dp, vertical = 14.dp)
                                    ) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(bottom = 12.dp),
                                            color = BorderSubtle
                                        )

                                        Text(
                                            text = "STATEMENT SUMMARY",
                                            color = TextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Summary Details Grid
                                        state.activity?.let { activity ->
                                            val totals = activity.totals.firstOrNull()

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Total Debits (Money Out)", color = TextMuted, fontSize = 11.sp)
                                                    Text(
                                                        text = totals?.let { inrFormatter.format(it.debits.toLong()) } ?: "₹0",
                                                        color = PrimaryOrange,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Total Credits (Money In)", color = TextMuted, fontSize = 11.sp)
                                                    Text(
                                                        text = totals?.let { inrFormatter.format(it.credits.toLong()) } ?: "₹0",
                                                        color = SavingsTeal,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Statement Period", color = TextMuted, fontSize = 11.sp)
                                                    val start = state.statementAnalytics?.periodStart
                                                    val end = state.statementAnalytics?.periodEnd
                                                    Text(
                                                        text = if (!start.isNullOrBlank())
                                                            "$start to $end"
                                                        else "Full Statement Window",
                                                        color = TextPrimary,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Ingestion Status", color = TextMuted, fontSize = 11.sp)
                                                    Text(
                                                        text = "Verified & Deduplicated",
                                                        color = SavingsTeal,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Text(
                                                text = "Tap to collapse",
                                                color = SecondaryCyan,
                                                fontSize = 11.sp,
                                                modifier = Modifier
                                                    .clickable { viewModel.onToggleExpandStatement(stmt.id) }
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
