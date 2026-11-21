package com.payscope.app.presentation.savings

import com.payscope.app.core.util.drawAmbientGlow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.payscope.app.core.components.BrandLogoView
import com.payscope.app.core.components.ButtonVariant
import com.payscope.app.core.components.PayScopeGlassCard
import com.payscope.app.core.components.PayScopePillButton
import com.payscope.app.core.theme.BorderHighlight
import com.payscope.app.core.theme.BorderSubtle
import com.payscope.app.core.theme.CardSurface
import com.payscope.app.core.theme.DangerRose
import com.payscope.app.core.theme.DeepBackground
import com.payscope.app.core.theme.ElevatedCardSurface
import com.payscope.app.core.theme.GlowOrange
import com.payscope.app.core.theme.GlowTeal
import com.payscope.app.core.theme.PrimaryOrange
import com.payscope.app.core.theme.SavingsTeal
import com.payscope.app.core.theme.SecondaryCyan
import com.payscope.app.core.theme.TextMuted
import com.payscope.app.core.theme.TextPrimary
import com.payscope.app.core.theme.TextSecondary
import com.payscope.app.core.theme.WarningAmber
import com.payscope.app.domain.model.CommitmentItem
import com.payscope.app.domain.model.SavedSavingsPlan

@Composable
fun SavingsScreen(
    onNavigateBack: (() -> Unit)? = null,
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.loadData() }
    var newPlanName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .drawBehind {
                drawAmbientGlow(
                    color = GlowTeal,
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
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SavingsTeal, strokeWidth = 3.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                uiState.errorMessage?.let { message -> item {
                    com.payscope.app.core.components.StatusMessage(message) { viewModel.loadData() }
                } }
                // Header
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                com.payscope.app.core.components.PayScopeLogo(size = 42.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column {
                                Text(
                                    text = "Live Savings Calculator",
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Interactive What-If budget cut simulator",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                        if (uiState.includesEstimates) Text(
                            "Includes potential subscriptions. Annual amounts assume the suggested billing cycle; confirm it in Subs. This is a planning estimate, not a verified future bill.",
                            color = WarningAmber, fontSize = 12.sp)
                        if (uiState.analysis.isNotBlank()) Text(uiState.analysis,
                            color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        if (uiState.commitments.isEmpty()) Text(
                            "No recurring service identified yet. Import more billing history or review potential services in Subs.",
                            color = TextSecondary, fontSize = 12.sp)
                    }
                }
                // Interactive Live Math Bar Hero Card
                item {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        PayScopeGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 28.dp,
                            backgroundColor = CardSurface,
                            borderColor = BorderSubtle,
                            glowColor = GlowTeal
                        ) {
                            Text(
                                text = "Annual what-if estimate",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // 3-way Math Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Baseline estimate", color = TextMuted, fontSize = 11.sp)
                                    Text(
                                        text = "₹${uiState.calculation.baselineAnnualCost.toInt()}",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(text = "—", color = TextMuted, fontSize = 16.sp)

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Save (${uiState.calculation.selectedCount})",
                                        color = SavingsTeal,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "₹${uiState.calculation.selectedAnnualSavings.toInt()}",
                                        color = SavingsTeal,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(text = "═", color = TextMuted, fontSize = 16.sp)

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "After selection", color = SecondaryCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = "₹${uiState.calculation.projectedAnnualCost.toInt()}",
                                        color = SecondaryCyan,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Reduction Pill Badge
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ElevatedCardSurface)
                                    .border(1.dp, SavingsTeal.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = SavingsTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Projected Annual Reduction",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = "↓ ${uiState.calculation.percentageReduction.toInt()}% Less Cost",
                                    color = SavingsTeal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Subscriptions You Can Cancel Checklist
                item {
                    Text(
                        text = "Services to include in your scenario",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                items(uiState.commitments, key = { it.id }) { commitment ->
                    val isSelected = commitment.id in uiState.selectedIds
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                        SavingsOpportunityCard(
                            commitment = commitment,
                            isSelected = isSelected,
                            onToggle = { viewModel.onToggleCommitment(commitment.id) }
                        )
                    }
                }

                // Save Plan Section
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        PayScopeGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 24.dp,
                            backgroundColor = CardSurface,
                            borderColor = BorderSubtle
                        ) {
                            Text(
                                text = "Save This Budget Cut Plan",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Persist this simulation to compare against future statements.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newPlanName,
                                    onValueChange = { newPlanName = it },
                                    placeholder = { Text("e.g. 2027 Budget Cuts", color = TextMuted, fontSize = 13.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary,
                                        focusedBorderColor = PrimaryOrange,
                                        unfocusedBorderColor = BorderSubtle
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                PayScopePillButton(
                                    text = "Save",
                                    icon = Icons.Default.Bookmark,
                                    variant = ButtonVariant.PRIMARY,
                                    isLoading = uiState.isSavingPlan,
                                    onClick = {
                                        if (newPlanName.isNotBlank()) {
                                            viewModel.onSavePlan(newPlanName)
                                            newPlanName = ""
                                        }
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Saved Plans List
                if (uiState.savedPlans.isNotEmpty()) {
                    item {
                        Text(
                            text = "Saved Optimization Plans",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }

                    items(uiState.savedPlans, key = { it.id }) { plan ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                            SavedPlanRow(
                                plan = plan,
                                onLoad = { viewModel.onLoadSavedPlan(plan) },
                                onDelete = { viewModel.onDeletePlan(plan.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsOpportunityCard(
    commitment: CommitmentItem,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val animatedBorder by animateColorAsState(
        targetValue = if (isSelected) SavingsTeal else BorderSubtle,
        label = "opportunityBorder"
    )

    PayScopeGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        backgroundColor = if (isSelected) CardSurface else CardSurface.copy(alpha = 0.7f),
        borderColor = animatedBorder,
        onClick = onToggle
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) SavingsTeal else ElevatedCardSurface)
                        .border(1.dp, if (isSelected) SavingsTeal else BorderSubtle, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                BrandLogoView(
                    logoKey = commitment.logoKey,
                    merchantName = commitment.merchantName,
                    size = 40.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = commitment.name,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    ReasonBadge(commitment = commitment)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Save ₹${commitment.annualizedAmount.toInt()}/yr",
                    color = SavingsTeal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "₹${commitment.normalizedMonthlyAmount.toInt()}/mo",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ReasonBadge(commitment: CommitmentItem) {
    val (label, color) = when {
        commitment.hasPriceHike -> Pair("Price Went Up", WarningAmber)
        commitment.status.equals("Needs Review", ignoreCase = true) -> Pair("Review Suggested", PrimaryOrange)
        commitment.status.equals("Potential", ignoreCase = true) -> Pair("Potential - estimate", SecondaryCyan)
        else -> Pair("Active commitment", TextSecondary)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SavedPlanRow(
    plan: SavedSavingsPlan,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onLoad)
            .background(ElevatedCardSurface)
            .border(1.dp, BorderSubtle, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = plan.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${plan.selectedCommitmentIds.size} cuts • Created ${plan.createdAt}",
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Save ₹${plan.annualSavings.toInt()}/yr",
                color = SavingsTeal,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
