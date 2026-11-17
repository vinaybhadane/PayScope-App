package com.payscope.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payscope.app.core.theme.BorderSubtle
import com.payscope.app.core.theme.TextPrimary

@Composable
fun BrandLogoView(
    logoKey: String,
    merchantName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val key = logoKey.lowercase()
    val (backgroundBrush, symbolText, symbolColor) = when {
        key.contains("netflix") -> Triple(
            Brush.linearGradient(listOf(Color(0xFF141414), Color(0xFF000000))),
            "N",
            Color(0xFFE50914)
        )
        key.contains("spotify") -> Triple(
            Brush.linearGradient(listOf(Color(0xFF1DB954), Color(0xFF107A35))),
            "S",
            Color(0xFFFFFFFF)
        )
        key.contains("amazon") || key.contains("prime") -> Triple(
            Brush.linearGradient(listOf(Color(0xFF00A8E1), Color(0xFF005696))),
            "P",
            Color(0xFFFFFFFF)
        )
        key.contains("openai") || key.contains("chatgpt") -> Triple(
            Brush.linearGradient(listOf(Color(0xFF10A37F), Color(0xFF0A634D))),
            "AI",
            Color(0xFFFFFFFF)
        )
        key.contains("youtube") -> Triple(
            Brush.linearGradient(listOf(Color(0xFFFF0000), Color(0xFFB30000))),
            "▶",
            Color(0xFFFFFFFF)
        )
        key.contains("claude") -> Triple(
            Brush.linearGradient(listOf(Color(0xFFCC785C), Color(0xFF94452C))),
            "C",
            Color(0xFFFFFFFF)
        )
        key.contains("devstack") || key.contains("cloud") -> Triple(
            Brush.linearGradient(listOf(Color(0xFF00E0E0), Color(0xFF008B8B))),
            "☁",
            Color(0xFF040711)
        )
        key.contains("zenith") || key.contains("gym") || key.contains("fit") -> Triple(
            Brush.linearGradient(listOf(Color(0xFFFF5500), Color(0xFFC73E00))),
            "⚡",
            Color(0xFFFFFFFF)
        )
        key.contains("google") -> Triple(
            Brush.linearGradient(listOf(Color(0xFF4285F4), Color(0xFF34A853))),
            "G",
            Color(0xFFFFFFFF)
        )
        else -> {
            val initial = merchantName.trim().firstOrNull()?.uppercase() ?: "P"
            Triple(
                Brush.linearGradient(listOf(Color(0xFF1C273C), Color(0xFF121B2C))),
                initial,
                Color(0xFF00E0E0)
            )
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(backgroundBrush)
            .border(1.dp, BorderSubtle, RoundedCornerShape(size * 0.32f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbolText,
            color = symbolColor,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp
        )
    }
}
