package com.payscope.app.presentation.dashboard

import com.payscope.app.core.util.drawAmbientGlow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.payscope.app.core.components.*
import com.payscope.app.core.theme.*
import java.text.NumberFormat
import java.util.Locale

private fun money(amount: Double): String = com.payscope.app.core.util.formatMoney(amount)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToUpload: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToSavings: () -> Unit = {},
    onNavigateToGraph: () -> Unit = {},
    onNavigateToProfile: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showAllTransactions by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
        viewModel.events.collect { when (it) {
            DashboardEvent.NavigateToUpload -> onNavigateToUpload()
            DashboardEvent.NavigateToAuth -> onNavigateToAuth()
        } }
    }

    val isDark = LocalIsDarkTheme.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .drawBehind {
                val glowAlpha = if (isDark) 1f else 0.35f
                drawAmbientGlow(GlowOrange.copy(alpha = GlowOrange.alpha * glowAlpha), size.width, Offset(size.width, 0f))
                drawAmbientGlow(GlowCyan.copy(alpha = GlowCyan.alpha * glowAlpha), size.width * .8f, Offset(0f, size.height * .6f))
            }
            .statusBarsPadding()
    ) {
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.hasLoaded,
            onRefresh = { viewModel.loadDashboardData(forceRefresh = true) },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Header: Profile Avatar at top-left & Personalized Welcome Heading (Addendum 03 - Req 2)
                item {
                    val welcomeHeading = remember(state.userName, state.isGuestDemo) {
                        val rawName = state.userName.trim()
                        val isGeneric = rawName.isEmpty() ||
                                rawName.equals("Your workspace", ignoreCase = true) ||
                                rawName.equals("PayScope User", ignoreCase = true) ||
                                rawName.equals("null", ignoreCase = true) ||
                                rawName.equals("undefined", ignoreCase = true)
                        if (!state.isGuestDemo && !isGeneric) {
                            "Welcome back, $rawName"
                        } else {
                            "Welcome back"
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(
                            photoUrl = state.userPhotoUrl,
                            onClick = onNavigateToProfile,
                            size = 46.dp
                        )

                        Spacer(Modifier.width(14.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (state.isGuestDemo) "Sample workspace" else "YOUR MONEY, IN FOCUS",
                                color = TextMuted,
                                fontSize = 12.sp,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = welcomeHeading,
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.4).sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (state.isLoading && !state.hasLoaded) {
                    item { ScreenLoading("Loading your workspace...") }
                } else {
                // 2. Monthly Recurring Spend Card (Theme-specific: Darker Cyan in Light, Orange in Dark, Sharper Edges)
                item {
                    val cardBg = if (isDark) Color(0xFF1B140E) else Color(0xFFDDF1F8)
                    val cardBorder = if (isDark) PrimaryOrange.copy(alpha = 0.20f) else Color(0xFF0284C7).copy(alpha = 0.18f)
                    val cardGlow = if (isDark) GlowOrange else GlowCyan
                    val accentColor = if (isDark) PrimaryOrange else Color(0xFF0369A1)

                    PayScopeGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 14.dp,
                        backgroundColor = cardBg,
                        borderColor = cardBorder,
                        glowColor = cardGlow
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly recurring spend",
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${state.summary.activeSubscriptionsCount} active",
                                    color = accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = money(state.summary.monthlyRecurringSpend),
                            color = TextPrimary,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1.2).sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Text(
                            text = if (state.isGuestDemo) "Sample recurring commitments" else "INR commitments",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        HorizontalDivider(Modifier.padding(vertical = 16.dp), color = accentColor.copy(alpha = 0.20f))

                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text("Annualized cost", color = TextMuted, fontSize = 12.sp)
                                Text(
                                    text = money(state.summary.annualizedSpend),
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text("Potential savings / yr", color = TextMuted, fontSize = 12.sp)
                                Text(
                                    text = money(state.summary.potentialSavingsAnnual),
                                    color = accentColor,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val actions = listOf(
                            Triple(Icons.Default.Analytics, "Analysis", onNavigateToAnalytics),
                            Triple(Icons.Default.Subscriptions, "Subscriptions", onNavigateToSubscriptions),
                            Triple(Icons.Default.Savings, "Savings Simulator", onNavigateToSavings),
                            Triple(Icons.Default.Hub, "Graph Analysis", onNavigateToGraph)
                        )
                        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
                        val columns = when {
                            maxWidth < 330.dp || fontScale > 1.6f -> 1
                            maxWidth < 600.dp -> 2
                            else -> 4
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            actions.chunked(columns).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    row.forEach { (icon, label, click) ->
                                        QuickActionItem(icon, label, click, Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                state.errorMessage?.let { message ->
                    item {
                        StatusMessage(message) { viewModel.loadDashboardData(forceRefresh = true) }
                    }
                }

                if (state.activity.analysisStatus == "failed") item {
                    StatusMessage(state.activity.analysisError ?: "Your transactions are saved. Gemini analysis needs a retry.") { viewModel.retryAnalysis() }
                }

                if (state.activity.transactionCount > 0) item {
                    PayScopeGlassCard(Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                        Text("Your cash flow", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("${state.activity.transactionCount} saved transactions / ${state.activity.statementCount} statements", color = TextSecondary,
                            fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp, bottom = 18.dp))
                        state.activity.totals.forEachIndexed { index, totals ->
                            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 14.dp), color = BorderSubtle)
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text("Money out / ${totals.currency}", color = TextMuted, fontSize = 12.sp)
                                    Text(statementMoney(totals.currency, totals.debits), color = TextPrimary, fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Money in / ${totals.currency}", color = TextMuted, fontSize = 12.sp)
                                    Text(statementMoney(totals.currency, totals.credits), color = SavingsTeal, fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                        }
                    }
                }

                if (!state.hasUploadedStatements) item {
                    PayScopeGlassCard(Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                        Icon(Icons.Default.Description, null, tint = SecondaryCyan, modifier = Modifier.size(30.dp))
                        Text(if (state.hasUploadedStatements) "No recurring payments yet" else "Your financial picture starts here",
                            color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
                        Text("Import a statement with multiple billing cycles. Gemini reviews the payment evidence before subscriptions appear here.",
                            color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 10.dp, bottom = 20.dp))
                        PayScopePillButton(text = "Import statement", onClick = { viewModel.onUploadStatementClicked() }, modifier = Modifier.fillMaxWidth())
                        if (state.hasUploadedStatements) TextButton(onClick = { viewModel.retryAnalysis() }) { Text("Retry Gemini analysis", color = SecondaryCyan) }
                    }
                } else {
                    // Import Statement Action (Connections removed from Overview per Requirement 2.A)
                    item {
                        PayScopePillButton(
                            text = "Import Statement",
                            onClick = { viewModel.onUploadStatementClicked() },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.Add,
                            variant = ButtonVariant.PRIMARY
                        )
                    }

                    if (state.commitments.isEmpty() && state.activity.analysisStatus == "completed") item {
                        Text("Your statement is saved. No recurring subscriptions were confirmed in this history. Your transactions are shown below; additional billing cycles can help establish recurrence.",
                            color = TextSecondary, fontSize = 12.sp, lineHeight = 19.sp)
                    }

                    if (state.activity.transactions.isNotEmpty()) {
                        item {
                            SectionTitle("Imported transactions", if (showAllTransactions) "Show fewer" else "Show more") { showAllTransactions = !showAllTransactions }
                            Text(if (showAllTransactions && state.activity.transactionCount > state.activity.transactions.size)
                                "Latest ${state.activity.transactions.size} of ${state.activity.transactionCount} saved records"
                                else "Read directly from your saved statement records", color = TextMuted, fontSize = 12.sp)
                        }
                        items(if (showAllTransactions) state.activity.transactions else state.activity.transactions.take(5), key = { "transaction:${it.id}" }) { tx ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardSurface).border(1.dp, BorderSubtle, RoundedCornerShape(18.dp)).padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (tx.direction == "credit") Icons.Default.SouthWest else Icons.Default.NorthEast, null,
                                    tint = if (tx.direction == "credit") SavingsTeal else PrimaryOrange, modifier = Modifier.size(20.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(tx.description, color = TextPrimary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${tx.date} / ${tx.status}", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                                Text((if (tx.direction == "credit") "+" else "-") + statementMoney(tx.currency, tx.amount),
                                    color = if (tx.direction == "credit") SavingsTeal else TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(max = 132.dp))
                            }
                        }
                    }

                    if (state.priceHikes.isNotEmpty()) item {
                        PayScopeGlassCard(Modifier.fillMaxWidth(), cornerRadius = 20.dp, borderColor = WarningAmber.copy(alpha = .3f)) {
                            Text("Price changes to review", color = WarningAmber, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            state.priceHikes.take(3).forEach { hike ->
                                Text("${hike.merchantName}: ${money(hike.oldAmount)} to ${money(hike.newAmount)}", color = TextSecondary,
                                    fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }

                    if (state.spendTrend.isNotEmpty()) item {
                        PayScopeGlassCard(Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                            SectionTitle("Spending history", "View analysis", onNavigateToAnalytics)
                            Text("Recorded recurring payments", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
                            FinancialTrendChart(points = state.spendTrend.map { it.copy(label = it.label.substringBefore(' ')) })
                        }
                    }

                    if (state.categories.isNotEmpty()) item {
                        PayScopeGlassCard(Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                            Text("Where your money goes", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(18.dp))
                            CategoryDistributionView(categories = state.categories)
                        }
                    }

                    if (state.commitments.isNotEmpty()) item { SectionTitle("Your subscriptions", "See all", onNavigateToSubscriptions) }

                    items(state.commitments.take(5), key = { it.id }) { item ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardSurface)
                            .clickable(onClick = onNavigateToSubscriptions).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            BrandLogoView(item.logoKey, item.merchantName, size = 38.dp)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(item.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${item.category} / ${item.cadence}", color = TextMuted, fontSize = 12.sp)
                            }
                            Text(if (item.currency == "INR") money(item.amount) else "${item.currency} ${item.amount}", color = TextPrimary, fontSize = 14.sp)
                        }
                    }

                    if (!state.isGuestDemo) item {
                        TextButton(onClick = { viewModel.retryAnalysis() }) { Text("Reanalyze saved records with Gemini", color = SecondaryCyan, fontSize = 12.sp) }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp))
                .background(PrimaryOrange.copy(alpha = .10f))
                .border(1.dp, PrimaryOrange.copy(alpha = .12f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = PrimaryOrange, modifier = Modifier.size(21.dp))
        }
        Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            lineHeight = 18.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SectionTitle(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(action, color = SecondaryCyan, fontSize = 12.sp) }
    }
}

private fun statementMoney(currency: String, amount: Double): String = com.payscope.app.core.util.formatMoney(amount, currency)
