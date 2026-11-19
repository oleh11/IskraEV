package com.iskra.companion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iskra.companion.data.model.EfficiencyData
import com.iskra.companion.ui.theme.CardBorder
import com.iskra.companion.ui.theme.EcoGreen
import com.iskra.companion.ui.theme.HyundaiBlue
import com.iskra.companion.ui.theme.PowerAmber
import com.iskra.companion.ui.theme.SurfaceCard
import com.iskra.companion.ui.theme.SurfaceCardElevated
import com.iskra.companion.ui.theme.TextMuted
import com.iskra.companion.ui.theme.TextPrimary
import com.iskra.companion.ui.theme.TextSecondary

/**
 * Classic 2018 Range & 15-Minute Rolling Efficiency Graph Tile.
 */
@Composable
fun RangeEfficiencyTile(
    estRangeMiles: Float,
    estRangeKm: Float,
    efficiency: EfficiencyData,
    isMilesUnit: Boolean = true,
    modifier: Modifier = Modifier
) {
    val displayRange = if (isMilesUnit) "${estRangeMiles.toInt()} mi" else "${estRangeKm.toInt()} km"
    val displayEfficiency = if (isMilesUnit) "${String.format("%.1f", efficiency.currentMiPerKwh)} mi/kWh" else "${String.format("%.1f", efficiency.currentKmPerKwh)} km/kWh"
    val displayAvg = if (isMilesUnit) "${String.format("%.1f", efficiency.rollingAverageMiPerKwh)} avg" else "${String.format("%.1f", efficiency.rollingAverageMiPerKwh * 1.609f)} avg"
    val isBetter = efficiency.isBetterThanAverage
    val trendColor = if (isBetter) EcoGreen else PowerAmber

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Title & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = HyundaiBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RANGE & EFFICIENCY (15-MIN)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceCardElevated)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(trendColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = displayEfficiency,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = trendColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Numbers Row: Steady Range & 15-min Average
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = displayRange,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "STEADY RANGE PROJECTION",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = displayAvg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "15-MIN BASELINE",
                        fontSize = 9.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Technical 15-Minute Rolling Efficiency Graph Canvas
            val history = efficiency.trendHistory
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceCardElevated)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    if (history.size < 2) return@Canvas

                    val maxEff = (history.maxOrNull() ?: 5.0f).coerceAtLeast(5.0f)
                    val minEff = (history.minOrNull() ?: 0.0f).coerceAtMost(0.0f)
                    val effRange = (maxEff - minEff).coerceAtLeast(1.0f)

                    // 1. Grid Background Lines
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val y = h * (i.toFloat() / gridSteps.toFloat())
                        drawLine(
                            color = CardBorder,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 0.8.dp.toPx()
                        )
                    }

                    // 2. Dashed 15-Minute Average Line
                    val avg = efficiency.rollingAverageMiPerKwh
                    val avgFraction = ((avg - minEff) / effRange).coerceIn(0f, 1f)
                    val avgY = h - (avgFraction * h)
                    drawLine(
                        color = TextMuted,
                        start = Offset(0f, avgY),
                        end = Offset(w, avgY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )

                    // 3. Efficiency History Line
                    val path = Path()
                    val dx = w / (history.size - 1).coerceAtLeast(1).toFloat()

                    history.forEachIndexed { index, value ->
                        val x = index.toFloat() * dx
                        val fraction = ((value - minEff) / effRange).coerceIn(0f, 1f)
                        val y = h - (fraction * h)
                        if (index == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = path,
                        color = trendColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // 4. Latest sample point
                    val lastX = (history.size - 1).toFloat() * dx
                    val lastVal = history.last()
                    val lastFraction = ((lastVal - minEff) / effRange).coerceIn(0f, 1f)
                    val lastY = h - (lastFraction * h)
                    drawCircle(color = trendColor, radius = 3.5.dp.toPx(), center = Offset(lastX, lastY))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Time Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("-15 min", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                Text("-7.5 min", fontSize = 9.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                Text("Now", fontSize = 9.sp, color = trendColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}
