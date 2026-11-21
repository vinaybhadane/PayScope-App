package com.payscope.app.core.components

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.payscope.app.core.theme.DeepBackground
import com.payscope.app.core.theme.DarkDeepBackground
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payscope.app.core.theme.BorderSubtle
import com.payscope.app.core.theme.CardSurface
import com.payscope.app.core.theme.ElevatedCardSurface
import com.payscope.app.core.theme.PrimaryOrange
import com.payscope.app.core.theme.PrimaryOrangeHover
import com.payscope.app.core.theme.SecondaryCyan
import com.payscope.app.core.theme.TextMuted
import com.payscope.app.core.theme.TextPrimary
import com.payscope.app.core.theme.TextSecondary
import com.payscope.app.core.util.bounceClick

@Composable
fun PayScopeHeader(
    greeting: String = "Good morning",
    userName: String = "Your workspace",
    photoUrl: String? = null,
    unreadAlertsCount: Int = 0,
    onSearchClick: () -> Unit = {},
    onAlertsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo & Greeting (aligned on the left)
        Row(verticalAlignment = Alignment.CenterVertically) {
            PayScopeLogo(size = 40.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = greeting,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = userName,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Action Icons & Profile on top-right (no duplicate PayScope logo)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Profile / User Avatar placed in top-right corner
            UserAvatar(
                photoUrl = photoUrl,
                onClick = onProfileClick,
                size = 42.dp
            )
        }
    }
}

enum class NavigationTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    SEARCH("Search", Icons.Outlined.Search),
    AI("PayScope AI", Icons.Default.AutoAwesome),
    ALERTS("Alerts", Icons.Outlined.Notifications),
    HISTORY("History", Icons.Outlined.History)
}

@Composable
fun PayScopeFloatingBottomBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier,
    unreadAlertsCount: Int = 0
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier.fillMaxWidth()
            .background(DeepBackground)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .shadow(8.dp, shape, spotColor = Color.Black.copy(alpha = .08f))
                .clip(shape).background(CardSurface)
                .border(1.dp, BorderSubtle, shape)
                .selectableGroup().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationTab.entries.forEach { tab ->
                val selected = tab == currentTab
                val tint by animateColorAsState(
                    if (selected) PrimaryOrange else TextSecondary,
                    label = "navigationTint"
                )
                val highlight by animateColorAsState(
                    if (selected) PrimaryOrange.copy(alpha = .12f) else Color.Transparent,
                    label = "navigationHighlight"
                )
                if (tab == NavigationTab.AI) {
                    Box(
                        Modifier.weight(1f).heightIn(min = 64.dp)
                            .semantics { contentDescription = "Open PayScope AI assistant" }
                            .clip(RoundedCornerShape(18.dp))
                            .bounceClick(onClick = { onTabSelected(tab) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(17.dp))
                                .background(Brush.linearGradient(listOf(PrimaryOrangeHover, PrimaryOrange))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, tint = DarkDeepBackground, modifier = Modifier.size(24.dp))
                        }
                    }
                } else {
                    Column(
                        Modifier.weight(1f).heightIn(min = 64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .selectable(selected = selected, role = Role.Tab, onClick = { onTabSelected(tab) })
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            Modifier.width(44.dp).height(30.dp).clip(CircleShape).background(highlight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tab.icon, null, tint = tint, modifier = Modifier.size(22.dp))
                            if (tab == NavigationTab.ALERTS && unreadAlertsCount > 0) {
                                Box(Modifier.align(Alignment.TopEnd).padding(3.dp).size(6.dp).background(PrimaryOrange, CircleShape))
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(tab.label, color = if (selected) TextPrimary else TextSecondary,
                            fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            maxLines = 1)
                    }
                }
            }
        }
    }
}
