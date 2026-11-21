package com.payscope.app.presentation.subscriptions

import com.payscope.app.core.util.formatMoney
import com.payscope.app.core.util.drawAmbientGlow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.TextButton
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.payscope.app.core.theme.GlowCyan
import com.payscope.app.core.theme.GlowOrange
import com.payscope.app.core.theme.PrimaryOrange
import com.payscope.app.core.theme.SavingsTeal
import com.payscope.app.core.theme.SecondaryCyan
import com.payscope.app.core.theme.TextMuted
import com.payscope.app.core.theme.TextPrimary
import com.payscope.app.core.theme.TextSecondary
import com.payscope.app.core.theme.WarningAmber
import com.payscope.app.domain.model.CommitmentItem
import com.payscope.app.presentation.cancellations.CancellationAssistantDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    onNavigateBack: (() -> Unit)? = null,
    viewModel: SubscriptionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.loadSubscriptions() }

    var editDialogCommitment by remember { mutableStateOf<CommitmentItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBackground)
            .drawBehind {
                drawAmbientGlow(
                    color = GlowOrange,
                    radius = size.width * 0.5f,
                    center = Offset(size.width * 0.9f, size.height * 0.15f)
                )
                drawAmbientGlow(
                    color = GlowCyan,
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.1f, size.height * 0.7f)
                )
            }
            .statusBarsPadding()
    ) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryOrange, strokeWidth = 3.dp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                uiState.errorMessage?.let { message -> item {
                    com.payscope.app.core.components.StatusMessage(message) { viewModel.loadSubscriptions() }
                } }
                // Header
                item {
                    com.payscope.app.core.components.ScreenHeading(
                        "Subscriptions", "${uiState.filteredCommitments.size} services to review",
                        Modifier.padding(horizontal = 20.dp), onBack = onNavigateBack
                    )
                }

                // Search Bar
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardSurface)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = { viewModel.onSearchQueryChanged(it) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = TextStyle(color = TextPrimary, fontSize = 14.sp),
                                cursorBrush = SolidColor(PrimaryOrange),
                                decorationBox = { innerTextField ->
                                    if (uiState.searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search by service or merchant...",
                                            color = TextMuted,
                                            fontSize = 14.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Status Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusFilter.entries.forEach { filter ->
                            val isSelected = filter == uiState.statusFilter
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) PrimaryOrange else ElevatedCardSurface)
                                    .border(1.dp, if (isSelected) BorderHighlight else BorderSubtle, CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { viewModel.onStatusFilterSelected(filter) }
                                    )
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = filter.label,
                                    color = if (isSelected) com.payscope.app.core.theme.DarkDeepBackground else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Subscriptions List
                if (uiState.filteredCommitments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No subscriptions matched your filters.",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(uiState.filteredCommitments, key = { it.id }) { commitment ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                            SubscriptionDirectoryCard(
                                commitment = commitment,
                                onClick = { viewModel.onCommitmentSelected(commitment) },
                                onConfirm = { viewModel.onConfirmCommitment(commitment.id) },
                                onDismiss = { viewModel.onDismissCommitment(commitment.id) },
                                onStartCancellation = { viewModel.startCancellation(commitment) }
                            )
                        }
                    }
                }
            }
        }

        // Selected Commitment Details Dialog
        if (uiState.selectedCommitment != null) {
            val commitment = uiState.selectedCommitment!!
            SubscriptionDetailsDialog(
                commitment = commitment,
                historicalDebits = uiState.selectedHistoricalDebits,
                onDismiss = { viewModel.onDismissDetails() },
                onEdit = {
                    val target = commitment
                    viewModel.onDismissDetails()
                    editDialogCommitment = target
                },
                onStartCancellation = {
                    val target = commitment
                    viewModel.onDismissDetails()
                    viewModel.startCancellation(target)
                }
            )
        }

        // Cancellation Assistant Dialog
        uiState.cancellationPlaybook?.let { playbook ->
            CancellationAssistantDialog(playbook, onDismiss = { viewModel.dismissCancellation() },
                onConfirmCancelled = { viewModel.reportCancellation() })
        }

        // Edit Subscription Dialog
        if (editDialogCommitment != null) {
            val c = editDialogCommitment!!
            EditSubscriptionDialog(
                commitment = c,
                onDismiss = { editDialogCommitment = null },
                onSave = { name, category, cadence, amount ->
                    viewModel.onUpdateCommitment(c.id, name, category, cadence, amount)
                    editDialogCommitment = null
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubscriptionDirectoryCard(
    commitment: CommitmentItem,
    onClick: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onStartCancellation: () -> Unit
) {
    PayScopeGlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        backgroundColor = CardSurface,
        borderColor = if (commitment.hasPriceHike) BorderHighlight else BorderSubtle,
        onClick = onClick
    ) {
        // Header Row: Brand Logo, Merchant Title, Category + Amount & Cadence
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandLogoView(
                    logoKey = commitment.logoKey,
                    merchantName = commitment.merchantName,
                    size = 44.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = commitment.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = commitment.category,
                        color = TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatMoney(commitment.amount, commitment.currency),
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = commitment.cadence,
                    color = SecondaryCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                if (commitment.status == "Confirmed") "Evidence reviewed" else "Limited billing history",
                color = SavingsTeal, fontSize = 11.sp,
                modifier = Modifier.background(SavingsTeal.copy(alpha = .10f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
            if (commitment.hasPriceHike) Text(
                "${commitment.priceHikePercentage?.toInt() ?: 0}% price increase",
                color = WarningAmber, fontSize = 11.sp,
                modifier = Modifier.background(WarningAmber.copy(alpha = .10f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            )
        }
        Text(
            if (commitment.nextBillingDate.isBlank()) "Billing cycle unconfirmed" else "Estimated renewal ${commitment.nextBillingDate}",
            color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)
        )
        Spacer(Modifier.height(6.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(commitment.status, color = if (commitment.status == "Confirmed") SavingsTeal else TextSecondary,
                fontSize = 12.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 15.dp, horizontal = 2.dp))
            TextButton(onClick = onStartCancellation) { Text("Cancel guide", color = DangerRose, fontSize = 12.sp) }
            if (commitment.status != "Confirmed") {
                TextButton(onClick = onConfirm) { Text("Confirm", color = SavingsTeal, fontSize = 12.sp) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionDetailsDialog(
    commitment: CommitmentItem,
    historicalDebits: List<com.payscope.app.domain.model.HistoricalDebit>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onStartCancellation: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(CardSurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(28.dp))
                .padding(22.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with logo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        BrandLogoView(
                            logoKey = commitment.logoKey,
                            merchantName = commitment.merchantName,
                            size = 48.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = commitment.name,
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${commitment.category} • ${commitment.cadence}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ElevatedCardSurface)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cost Overview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ElevatedCardSurface)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Per Billing", color = TextMuted, fontSize = 11.sp)
                        Text(text = formatMoney(commitment.amount, commitment.currency), color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Monthly Run-Rate", color = TextMuted, fontSize = 11.sp)
                        Text(text = formatMoney(commitment.normalizedMonthlyAmount, commitment.currency), color = SecondaryCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Annual Total", color = TextMuted, fontSize = 11.sp)
                        Text(text = formatMoney((commitment.normalizedMonthlyAmount * 12), commitment.currency), color = PrimaryOrange, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detection Reasoning
                Text(
                    text = "Recurrence Evidence",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = commitment.reasonExplanation,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Supporting Debits History
                Text(
                    text = "Supporting Debits History",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    historicalDebits.forEach { debit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ElevatedCardSurface)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = debit.date, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(text = debit.descriptor, color = TextMuted, fontSize = 10.sp, maxLines = 1)
                            }
                            Text(text = formatMoney(debit.amount, commitment.currency), color = PrimaryOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PayScopePillButton(
                        text = "Edit",
                        icon = Icons.Default.Edit,
                        variant = ButtonVariant.SECONDARY,
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    )
                    PayScopePillButton(
                        text = "Cancel Guide",
                        icon = Icons.Default.Cancel,
                        variant = ButtonVariant.DANGER,
                        onClick = onStartCancellation,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSubscriptionDialog(
    commitment: CommitmentItem,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, cadence: String, amount: Double) -> Unit
) {
    var name by remember { mutableStateOf(commitment.name) }
    var category by remember { mutableStateOf(commitment.category) }
    var cadence by remember { mutableStateOf(commitment.cadence) }
    var amountText by remember { mutableStateOf(commitment.amount.toString()) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(CardSurface)
                .border(1.dp, BorderSubtle, RoundedCornerShape(26.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Edit Subscription",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subscription Name", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryOrange,
                        unfocusedBorderColor = BorderSubtle
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                PayScopePillButton(
                    text = "Save Changes",
                    variant = ButtonVariant.PRIMARY,
                    onClick = {
                        val parsed = amountText.toDoubleOrNull() ?: commitment.amount
                        onSave(name, category, cadence, parsed)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
