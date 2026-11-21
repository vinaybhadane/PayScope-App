package com.payscope.app.presentation.search

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.payscope.app.core.components.PayScopeGlassCard
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
import com.payscope.app.core.util.drawAmbientGlow
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SearchScreen(
    onNavigateToSubscriptions: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val inrFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }

    val filters = listOf("All", "Subscriptions", "Transactions")

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
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Input Header
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
                        tint = PrimaryOrange,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    BasicTextField(
                        value = state.query,
                        onValueChange = { viewModel.onQueryChanged(it) },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(PrimaryOrange),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (state.query.isEmpty()) {
                                Text(
                                    text = "Search subscriptions, merchants, amounts...",
                                    color = TextMuted,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (state.query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { viewModel.onQueryChanged("") }
                        )
                    }
                }
            }

            // Filter Chips
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.forEach { filter ->
                        val isSelected = state.selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) PrimaryOrange else ElevatedCardSurface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) PrimaryOrange else BorderSubtle,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.onFilterSelected(filter) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = filter,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryOrange)
                    }
                }
            } else {
                // Subscriptions Section
                if (state.filteredCommitments.isNotEmpty()) {
                    item {
                        Text(
                            text = "SUBSCRIPTIONS (${state.filteredCommitments.size})",
                            color = SecondaryCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    items(state.filteredCommitments, key = { "sub:${it.id}" }) { item ->
                        PayScopeGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onNavigateToSubscriptions
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(ElevatedCardSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Subscriptions,
                                        contentDescription = null,
                                        tint = SecondaryCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${item.category} • ${item.cadence} via ${item.paymentRail}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    text = inrFormatter.format(item.amount.toLong()),
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Transactions Section
                if (state.filteredTransactions.isNotEmpty()) {
                    item {
                        Text(
                            text = "TRANSACTIONS (${state.filteredTransactions.size})",
                            color = PrimaryOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(state.filteredTransactions, key = { "tx:${it.id}" }) { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardSurface)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (tx.direction == "credit") Icons.Default.SouthWest else Icons.Default.NorthEast,
                                contentDescription = null,
                                tint = if (tx.direction == "credit") SavingsTeal else PrimaryOrange,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.description,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${tx.date} • ${tx.status}",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }

                            Text(
                                text = (if (tx.direction == "credit") "+" else "-") + inrFormatter.format(tx.amount.toLong()),
                                color = if (tx.direction == "credit") SavingsTeal else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Empty State
                if (state.filteredCommitments.isEmpty() && state.filteredTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (state.query.isEmpty()) "Type to search your payments" else "No results found for '${state.query}'",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
