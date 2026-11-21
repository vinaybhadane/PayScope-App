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

private fun money(amount: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    .apply { maximumFractionDigits = 0 }.format(amount)

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
    var showAllTransactions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
        viewModel.events.collect { when (it) {
            DashboardEvent.NavigateToUpload -> onNavigateToUpload()
            DashboardEvent.NavigateToAuth -> onNavigateToAuth()
        } }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .drawBehind {
                drawAmbientGlow(GlowOrange, size.width, Offset(size.width, 0f))
                drawAmbientGlow(GlowCyan, size.width * .8f, Offset(0f, size.height * .6f))
            }
            .statusBarsPadding()
    ) {
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.loadDashboardData(forceRefresh = true) },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header: Profile Avatar at top-left, No PayScope logo in Home header (Addendum 02 - Req 2 & 5)
                item {
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
                                fontSize = 9.sp,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Overview",
                                color = TextPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.8).sp
                            )
                        }
                    }
                }

                // Four Icon-led Quick-Access Options (Addendum 02 - Req 3 & PhonePe layout reference)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(CardSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(22.dp))
                            .padding(horizontal = 8.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        QuickActionItem(
                            icon = Icons.Default.Analytics,
                            label = "Analysis",
                            tint = SecondaryCyan,
                            onClick = onNavigateToAnalytics,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionItem(
                            icon = Icons.Default.Subscriptions,
                            label = "Subscriptions",
                            tint = PrimaryOrange,
                            onClick = onNavigateToSubscriptions,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionItem(
                            icon = Icons.Default.Savings,
                            label = "Savings Simulator",
                            tint = SavingsTeal,
                            onClick = onNavigateToSavings,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionItem(
                            icon = Icons.Default.Hub,
                            label = "Graph Analysis",
                            tint = SecondaryCyan,
                            onClick = onNavigateToGraph,
                            modifier = Modifier.weight(1f)
                        )
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
                    PayScopeGlassCard(Modifier.fillMaxWidth(), cornerRadius = 24.dp, borderColor = BorderHighlight) {
                        Text("Statement activity", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("${state.activity.transactionCount} saved transactions / ${state.activity.statementCount} statements", color = TextSecondary,
                            fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp, bottom = 18.dp))
                        state.activity.totals.forEachIndexed { index, totals ->
                            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 14.dp), color = BorderSubtle)
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text("Money out / ${totals.currency}", color = TextMuted, fontSize = 11.sp)
                                    Text(statementMoney(totals.currency, totals.debits), color = TextPrimary, fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Money in / ${totals.currency}", color = TextMuted, fontSize = 11.sp)
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
                            color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 10.dp, bottom = 20.dp))
                        PayScopePillButton(text = "Import statement", onClick = { viewModel.onUploadStatementClicked() }, modifier = Modifier.fillMaxWidth())
                        if (state.hasUploadedStatements) TextButton(onClick = { viewModel.retryAnalysis() }) { Text("Retry Gemini analysis", color = SecondaryCyan) }
                    }
                } else {
                    if (state.commitments.isNotEmpty()) item {
                        PayScopeGlassCard(Modifier.fillMaxWidth(), cornerRadius = 26.dp, borderColor = BorderHighlight) {
                            Text("Monthly recurring spend", color = TextSecondary, fontSize = 12.sp)
                            Text(money(state.summary.monthlyRecurringSpend), color = TextPrimary, fontSize = 38.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = (-1.3).sp, modifier = Modifier.padding(top = 8.dp))
                            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${state.summary.activeSubscriptionsCount} active", color = SecondaryCyan, fontSize = 11.sp,
                                    modifier = Modifier.background(SecondaryCyan.copy(alpha = .09f), CircleShape).padding(horizontal = 10.dp, vertical = 5.dp))
                                Text("INR commitments", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 5.dp))
                            }
                            HorizontalDivider(Modifier.padding(vertical = 20.dp), color = BorderSubtle)
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text("Annualized cost", color = TextMuted, fontSize = 11.sp)
                                    Text(money(state.summary.annualizedSpend), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 5.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Savings scenarios / yr", color = TextMuted, fontSize = 11.sp)
                                    Text(money(state.summary.potentialSavingsAnnual), color = SavingsTeal, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 5.dp))
                                }
                            }
                        }
                    }

                    // Import Statement Action (Connections removed from Overview per Requirement 2.A)
                    item {
                        PayScopePillButton(
                            text = "Import Statement",
                            onClick = { viewModel.onUploadStatementClicked() },
                            modifier = Modifier.fillMaxWidth(),
                            icon = Icons.Default.Add,
                            variant = ButtonVariant.SECONDARY
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
                                else "Read directly from your saved statement records", color = TextMuted, fontSize = 10.sp)
                        }
                        items(if (showAllTransactions) state.activity.transactions else state.activity.transactions.take(5), key = { "transaction:${it.id}" }) { tx ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardSurface).padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (tx.direction == "credit") Icons.Default.SouthWest else Icons.Default.NorthEast, null,
                                    tint = if (tx.direction == "credit") SavingsTeal else PrimaryOrange, modifier = Modifier.size(20.dp))
                                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                    Text(tx.description, color = TextPrimary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${tx.date} / ${tx.status}", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                                }
                                Text((if (tx.direction == "credit") "+" else "-") + statementMoney(tx.currency, tx.amount),
                                    color = if (tx.direction == "credit") SavingsTeal else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                            Text("Recorded recurring payments", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))
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

                    if (state.upcoming.isNotEmpty()) item {
                        PayScopeGlassCard(Modifier.fillMaxWidth(), cornerRadius = 24.dp) {
                            Text("Coming up", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("Estimated from your payment history", color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                            state.upcoming.take(4).forEach { bill ->
                                Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    BrandLogoView(bill.logoKey, bill.merchantName, size = 34.dp)
                                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                        Text(bill.merchantName, color = TextPrimary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(bill.dueDate, color = TextMuted, fontSize = 10.sp)
                                    }
                                    Text(money(bill.amount), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    if (state.commitments.isNotEmpty()) item { SectionTitle("Your subscriptions", "See all", onNavigateToSubscriptions) }

                    items(state.commitments.take(5), key = { it.id }) { item ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardSurface)
                            .clickable(onClick = onNavigateToSubscriptions).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            BrandLogoView(item.logoKey, item.merchantName, size = 38.dp)
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                Text(item.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${item.category} / ${item.cadence}", color = TextMuted, fontSize = 10.sp)
                            }
                            Text(if (item.currency == "INR") money(item.amount) else "${item.currency} ${item.amount}", color = TextPrimary, fontSize = 14.sp)
                        }
                    }

                    if (!state.isGuestDemo) item {
                        TextButton(onClick = { viewModel.retryAnalysis() }) { Text("Reanalyze saved records with Gemini", color = SecondaryCyan, fontSize = 11.sp) }
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
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(ElevatedCardSurface)
                .border(1.dp, BorderSubtle, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            lineHeight = 13.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SectionTitle(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(action, color = SecondaryCyan, fontSize = 10.sp, modifier = Modifier.clickable(onClick = onClick).padding(vertical = 8.dp))
    }
}

private fun statementMoney(currency: String, amount: Double): String = if (currency == "INR") money(amount)
    else currency + " " + NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 2 }.format(amount)
