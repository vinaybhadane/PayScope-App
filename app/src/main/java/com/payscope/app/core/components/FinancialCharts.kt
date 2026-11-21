package com.payscope.app.core.components

import com.payscope.app.core.util.formatMoney
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payscope.app.core.theme.BorderSubtle
import com.payscope.app.core.theme.PrimaryOrange
import com.payscope.app.core.theme.SavingsTeal
import com.payscope.app.core.theme.SecondaryCyan
import com.payscope.app.core.theme.TextMuted
import com.payscope.app.core.theme.TextPrimary
import com.payscope.app.core.theme.TextSecondary
import com.payscope.app.domain.model.CategorySpend
import com.payscope.app.domain.model.SpendTrendPoint
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AnimatedCurrencyText(
    amount: Double,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹",
    suffix: String = "",
    fontSize: Int = 28,
    fontWeight: FontWeight = FontWeight.Bold,
    textColor: Color = Color.Unspecified
) {
    val actualTextColor = if (textColor != Color.Unspecified) textColor else TextPrimary
    val animatedAmount by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "currencyCounter"
    )

    val formatter = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            maximumFractionDigits = 0
        }
    }

    val formatted = formatter.format(animatedAmount.toLong())

    Text(
        text = "$currencySymbol $formatted$suffix",
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        color = actualTextColor,
        letterSpacing = (-0.5).sp,
        modifier = modifier
    )
}

@Composable
fun FinancialTrendChart(
    points: List<SpendTrendPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp
) {
    if (points.isEmpty()) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    val maxAmount = points.maxOfOrNull { it.amount }?.takeIf { it > 0 } ?: 1.0

    val secondaryCyanColor = SecondaryCyan
    val primaryOrangeColor = PrimaryOrange
    val borderSubtleColor = BorderSubtle

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val width = size.width
            val canvasHeight = size.height
            val barWidth = ((width / points.size) * 0.45f).coerceAtMost(28.dp.toPx())
            val spacing = width / points.size

            points.forEachIndexed { index, point ->
                val x = index * spacing + spacing / 2
                val normalizedHeight = (point.amount / maxAmount).toFloat() * (canvasHeight - 20f) * progress.value
                val y = canvasHeight - normalizedHeight

                val barBrush = if (point.isProjected) {
                    Brush.verticalGradient(
                        listOf(secondaryCyanColor.copy(alpha = 0.9f), secondaryCyanColor.copy(alpha = 0.2f)),
                        startY = y,
                        endY = canvasHeight
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(primaryOrangeColor.copy(alpha = 0.95f), primaryOrangeColor.copy(alpha = 0.25f)),
                        startY = y,
                        endY = canvasHeight
                    )
                }

                // Draw background track
                drawRoundRect(
                    color = borderSubtleColor.copy(alpha = 0.35f),
                    topLeft = Offset(x - barWidth / 2, 0f),
                    size = Size(barWidth, canvasHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Draw animated bar
                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(x - barWidth / 2, y),
                    size = Size(barWidth, normalizedHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Month labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = if (point.isProjected) SecondaryCyan else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun CategoryDistributionView(
    categories: List<CategorySpend>,
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return

    Column(modifier = modifier) {
        // Multi-segment progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(BorderSubtle)
        ) {
            categories.forEach { cat ->
                val weight = (cat.percentage.toFloat() / 100f).coerceAtLeast(0.01f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(weight)
                        .height(10.dp)
                        .background(Color(cat.colorHex))
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Category items
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { cat ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(cat.colorHex))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cat.category,
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = "${formatMoney(cat.amount)} (${cat.percentage}%)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
