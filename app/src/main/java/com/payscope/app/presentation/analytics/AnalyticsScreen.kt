package com.payscope.app.presentation.analytics

import com.payscope.app.core.util.drawAmbientGlow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.payscope.app.core.components.AnimatedCurrencyText
import com.payscope.app.core.components.BrandLogoView
import com.payscope.app.core.components.CategoryDistributionView
import com.payscope.app.core.components.FinancialTrendChart
import com.payscope.app.core.components.PayScopeGlassCard
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
import com.payscope.app.core.theme.WarningAmber
import com.payscope.app.domain.model.DateRangeFilter
import com.payscope.app.domain.model.MerchantComparison
import com.payscope.app.presentation.graph.RelationshipGraphScreen

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton

@Composable
fun AnalyticsScreen(
    onNavigateBack: (() -> Unit)? = null,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.reload() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .drawBehind {
                drawAmbientGlow(
                    color = GlowCyan,
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.85f, size.height * 0.2f)
                )
                drawAmbientGlow(
                    color = GlowOrange,
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.15f, size.height * 0.65f)
                )
            }
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Header: Dedicated financial spending intelligence (Requirement 6: Graph moved to dedicated Home feature)
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
                    com.payscope.app.core.components.PayScopeLogo(size = 40.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spending Intelligence",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Statement spending trends & patterns",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SecondaryCyan, strokeWidth = 3.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    uiState.errorMessage?.let { message -> item {
                        com.payscope.app.core.components.StatusMessage(message) { viewModel.reload() }
                    } }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DateRangeFilter.entries.forEach { filter ->
                            val isSelected = filter == uiState.selectedFilter
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) PrimaryOrange else ElevatedCardSurface)
                                    .border(1.dp, if (isSelected) BorderHighlight else BorderSubtle, CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { viewModel.onFilterSelected(filter) }
                                    )
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = filter.label,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // 4 Top Metric Cards (2x2 Grid)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricCard(
                                title = "Observed spending",
                                amount = "₹${uiState.statement.totalDebits.toInt()}",
                                subtitle = "Imported INR debits",
                                accentColor = PrimaryOrange,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Monthly average",
                                amount = "₹${uiState.statement.monthlyAverage.toInt()}",
                                subtitle = "Across observed statement months",
                                accentColor = SecondaryCyan,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricCard(
                                title = "Confirmed recurring",
                                amount = "₹${uiState.summary.monthlyRecurringSpend.toInt()}",
                                subtitle = "${uiState.summary.activeSubscriptionsCount} confirmed subscriptions / month",
                                accentColor = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            MetricCard(
                                title = "Potential service costs",
                                amount = "₹${uiState.potentialMonthlyEstimate.toInt()}/mo",
                                subtitle = "${uiState.potentialCount} estimates; confirm in Subs",
                                accentColor = SavingsTeal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Spending Trend Chart Card
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        PayScopeGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 28.dp,
                            backgroundColor = CardSurface,
                            borderColor = BorderSubtle
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Spending by month",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = uiState.selectedFilter.label,
                                    color = SecondaryCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            FinancialTrendChart(
                                points = uiState.spendTrend,
                                height = 140.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Category Distribution Card
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        PayScopeGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 28.dp,
                            backgroundColor = CardSurface,
                            borderColor = BorderSubtle
                        ) {
                            Text(
                                text = "Category Allocation",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            CategoryDistributionView(categories = uiState.categories)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Top 5 Merchants Comparison Bar Chart
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        PayScopeGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 28.dp,
                            backgroundColor = CardSurface,
                            borderColor = BorderSubtle
                        ) {
                            Text(
                                text = "Top merchants in your statements",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val maxSpend = uiState.topMerchants.maxOfOrNull { it.annualSpend } ?: 1.0

                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                uiState.topMerchants.forEach { merchant ->
                                    MerchantSpendRow(merchant = merchant, maxSpend = maxSpend)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Monthly vs Annual Cost Comparison Card
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        PayScopeGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            backgroundColor = ElevatedCardSurface,
                            borderColor = BorderHighlight
                        ) {
                            Text(
                                text = "Spending and recurring payments",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Observed statement spending", color = TextMuted, fontSize = 12.sp)
                                    Text(text = "₹${uiState.statement.totalDebits.toInt()}", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Confirmed monthly recurring", color = TextMuted, fontSize = 12.sp)
                                    Text(text = "₹${uiState.summary.monthlyRecurringSpend.toInt()}", color = SecondaryCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = uiState.statement.spendingAnalysis.ifBlank { "Spending comes from saved statement records. Potential subscriptions need confirmation before they count as recurring payments." },
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun MetricCard(
    title: String,
    amount: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(CardSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(text = title, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = amount, color = accentColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = TextSecondary, fontSize = 10.sp)
        }
    }
}

@Composable
private fun MerchantSpendRow(merchant: MerchantComparison, maxSpend: Double) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(merchant) {
        progress.animateTo(
            targetValue = (merchant.annualSpend / maxSpend).toFloat(),
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandLogoView(
                    logoKey = merchant.logoKey,
                    merchantName = merchant.merchantName,
                    size = 32.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${merchant.rank}. ${merchant.merchantName}",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "₹${merchant.annualSpend.toInt()}",
                color = SecondaryCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(BorderSubtle)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.value)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(listOf(PrimaryOrange, SecondaryCyan))
                    )
            )
        }
    }
}
