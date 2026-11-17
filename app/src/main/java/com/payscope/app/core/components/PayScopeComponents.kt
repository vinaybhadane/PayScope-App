package com.payscope.app.core.components

import com.payscope.app.core.util.drawAmbientGlow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payscope.app.core.theme.BorderHighlight
import com.payscope.app.core.theme.BorderSubtle
import com.payscope.app.core.theme.CardSurface
import com.payscope.app.core.theme.DeepBackground
import com.payscope.app.core.theme.ElevatedCardSurface
import com.payscope.app.core.theme.GlowOrange
import com.payscope.app.core.theme.PrimaryOrange
import com.payscope.app.core.theme.PrimaryOrangeHover
import com.payscope.app.core.theme.SecondaryCyan
import com.payscope.app.core.theme.TextMuted
import com.payscope.app.core.theme.TextPrimary
import com.payscope.app.core.theme.TextSecondary
import com.payscope.app.core.util.bounceClick

@Composable
fun PayScopeGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    backgroundColor: Color = CardSurface,
    borderColor: Color = BorderSubtle,
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val cardModifier = modifier
        .then(
            if (glowColor != null) {
                Modifier.drawBehind {
                    drawAmbientGlow(
                        color = glowColor,
                        radius = size.width * 0.45f,
                        center = Offset(size.width * 0.5f, size.height * 0.4f)
                    )
                }
            } else Modifier
        )
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, borderColor, shape)
        .then(
            if (onClick != null) {
                Modifier.bounceClick(scaleDown = 0.98f, onClick = onClick)
            } else Modifier
        )
        .padding(20.dp)

    Column(
        modifier = cardModifier,
        content = content
    )
}

enum class ButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINE,
    GHOST,
    DANGER
}

@Composable
fun PayScopePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    height: Dp = 52.dp
) {
    val shape = CircleShape

    val (backgroundBrush, contentColor, borderStroke) = when (variant) {
        ButtonVariant.PRIMARY -> Triple(
            Brush.horizontalGradient(listOf(PrimaryOrange, PrimaryOrangeHover)),
            Color.White,
            null
        )
        ButtonVariant.SECONDARY -> Triple(
            Brush.horizontalGradient(listOf(ElevatedCardSurface, CardSurface)),
            TextPrimary,
            BorderSubtle
        )
        ButtonVariant.OUTLINE -> Triple(
            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
            SecondaryCyan,
            BorderSubtle
        )
        ButtonVariant.GHOST -> Triple(
            Brush.horizontalGradient(listOf(Color(0x15FFFFFF), Color(0x15FFFFFF))),
            TextSecondary,
            null
        )
        ButtonVariant.DANGER -> Triple(
            Brush.horizontalGradient(listOf(Color(0xFFF43F5E), Color(0xFFE11D48))),
            Color.White,
            null
        )
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(backgroundBrush)
            .then(
                if (borderStroke != null) Modifier.border(1.dp, borderStroke, shape) else Modifier
            )
            .bounceClick(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = contentColor,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = contentColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}
