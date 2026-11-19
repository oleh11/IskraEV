package com.iskra.companion.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iskra.companion.data.model.BatteryData
import com.iskra.companion.ui.theme.CardBorder
import com.iskra.companion.ui.theme.EcoGreen
import com.iskra.companion.ui.theme.HyundaiBlue
import com.iskra.companion.ui.theme.PowerAmber
import com.iskra.companion.ui.theme.PowerRed
import com.iskra.companion.ui.theme.SurfaceCard
import com.iskra.companion.ui.theme.SurfaceCardElevated
import com.iskra.companion.ui.theme.TextMuted
import com.iskra.companion.ui.theme.TextPrimary
import com.iskra.companion.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Classic 2018 High-Voltage Battery Level Gauge.
 * Clean, tactile, high-contrast circular battery meter with capacity & voltage readouts.
 */
@Composable
fun BatterySocRing(
    battery: BatteryData,
    modifier: Modifier = Modifier,
    size: Dp = 175.dp
) {
    val animatedSoc by animateFloatAsState(
        targetValue = battery.socDisplay,
        animationSpec = tween(durationMillis = 200),
        label = "socAnimation"
    )

    val isConnected = battery.voltage > 0f

    val socColor = when {
        battery.socDisplay > 25f -> EcoGreen
        battery.socDisplay > 12f -> PowerAmber
        else -> PowerRed
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(com.iskra.companion.ui.theme.GaugeGlowBrush)
            .border(1.5.dp, CardBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.toPx()
            val h = size.toPx()
            val center = Offset(w / 2f, h / 2f)
            val trackWidth = 8.dp.toPx()
            val radius = (w - trackWidth - 24.dp.toPx()) / 2f

            val startAngle = 135f
            val totalSweep = 270f

            // 1. Background Arc Track
            drawArc(
                color = SurfaceCardElevated,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = trackWidth, cap = StrokeCap.Round)
            )

            // 2. Active SOC Arc
            if (isConnected && animatedSoc > 0f) {
                val sweep = (animatedSoc / 100f).coerceIn(0f, 1f) * totalSweep
                // Neon glow under the active arc
                drawArc(
                    color = socColor.copy(alpha = 0.22f),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = trackWidth * 2.6f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = socColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = trackWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Dial Ticks (Every 10% from 0 to 100)
            for (step in 0..10) {
                val fraction = step / 10f
                val angleDeg = startAngle + (fraction * totalSweep)
                val isMajor = step % 5 == 0
                val tickLen = if (isMajor) 7.dp.toPx() else 4.dp.toPx()

                val rad = (angleDeg * PI / 180.0).toFloat()
                val innerR = radius - (trackWidth / 2f) - tickLen
                val outerR = radius - (trackWidth / 2f) - 1.dp.toPx()

                val p1 = Offset(center.x + innerR * cos(rad), center.y + innerR * sin(rad))
                val p2 = Offset(center.x + outerR * cos(rad), center.y + outerR * sin(rad))

                drawLine(
                    color = if (isMajor) TextSecondary else TextMuted,
                    start = p1,
                    end = p2,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                )
            }
        }

        // Center Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                text = if (isConnected) "${battery.socDisplay.toInt()}%" else "--%",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (isConnected) socColor else TextMuted,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = "STATE OF CHARGE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isConnected) "${String.format("%.1f", battery.remainingEnergyKwh)} kWh" else "0.0 kWh",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontFamily = FontFamily.Monospace
            )

            Text(
                text = if (isConnected) "${battery.voltage.toInt()} V" else "-- V",
                fontSize = 10.sp,
                color = TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
