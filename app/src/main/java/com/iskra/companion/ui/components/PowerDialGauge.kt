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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iskra.companion.data.model.Gear
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Classic 2018 Automotive Analog-Digital Power & Regen Instrument Dial.
 * Clean, high-contrast, tactile dial gauge with graduated tick marks and needle.
 */
@Composable
fun PowerDialGauge(
    powerKw: Float,
    gear: Gear,
    speedKmh: Float,
    modifier: Modifier = Modifier,
    size: Dp = 175.dp
) {
    val animatedPower by animateFloatAsState(
        targetValue = powerKw,
        animationSpec = tween(durationMillis = 150),
        label = "powerDialAnimation"
    )

    val isRegen = animatedPower > 0.5f
    val isDraw = animatedPower < -0.5f
    val absPower = abs(animatedPower)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(SurfaceCard)
            .border(1.5.dp, CardBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.toPx()
            val h = size.toPx()
            val center = Offset(w / 2f, h / 2f)
            val trackWidth = 8.dp.toPx()
            val radius = (w - trackWidth - 24.dp.toPx()) / 2f

            // Total sweep from 135° (bottom left) to 405° (bottom right) -> 270° total sweep
            val startAngle = 135f
            val totalSweep = 270f
            // Zero power position sits at top center (270°)
            val zeroAngle = 270f
            val regenMaxAngle = 135f   // 135° to 270° is Regen (135° sweep)
            val powerMaxAngle = 405f   // 270° to 405° is Power Draw (135° sweep)

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

            // 2. Active Power / Regen Fill Arc
            if (isRegen) {
                // Regen sweeps counter-clockwise from 270° back towards 135°
                val regenFraction = (absPower / 60.0f).coerceIn(0f, 1f)
                val sweep = -(regenFraction * 135f)
                drawArc(
                    color = EcoGreen,
                    startAngle = zeroAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = trackWidth, cap = StrokeCap.Round)
                )
            } else if (isDraw) {
                // Power sweeps clockwise from 270° towards 405°
                val drawFraction = (absPower / 200.0f).coerceIn(0f, 1f)
                val sweep = drawFraction * 135f
                val color = if (absPower > 150f) PowerRed else if (absPower > 80f) PowerAmber else HyundaiBlue
                drawArc(
                    color = color,
                    startAngle = zeroAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = trackWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Dial Tick Marks (Every 15 degrees)
            for (angleDeg in 135..405 step 15) {
                val isMajor = angleDeg % 45 == 0 || angleDeg == 270
                val tickLen = if (isMajor) 7.dp.toPx() else 4.dp.toPx()
                val tickColor = if (angleDeg == 270) TextPrimary else if (angleDeg < 270) EcoGreen.copy(alpha = 0.6f) else TextMuted

                val rad = (angleDeg * PI / 180.0).toFloat()
                val innerR = radius - (trackWidth / 2f) - tickLen
                val outerR = radius - (trackWidth / 2f) - 1.dp.toPx()

                val p1 = Offset(center.x + innerR * cos(rad), center.y + innerR * sin(rad))
                val p2 = Offset(center.x + outerR * cos(rad), center.y + outerR * sin(rad))

                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // 4. Dial Needle
            val needleAngleDeg = when {
                isRegen -> zeroAngle - ((absPower / 60f).coerceIn(0f, 1f) * 135f)
                isDraw -> zeroAngle + ((absPower / 200f).coerceIn(0f, 1f) * 135f)
                else -> zeroAngle
            }
            val needleRad = (needleAngleDeg * PI / 180.0).toFloat()
            val needleLen = radius - 10.dp.toPx()
            val needleEnd = Offset(center.x + needleLen * cos(needleRad), center.y + needleLen * sin(needleRad))
            val needleColor = if (isRegen) EcoGreen else if (absPower > 100f) PowerAmber else TextPrimary

            drawLine(
                color = needleColor,
                start = center,
                end = needleEnd,
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Needle Center Pivot Cap
            drawCircle(color = SurfaceCardElevated, radius = 7.dp.toPx(), center = center)
            drawCircle(color = needleColor, radius = 3.dp.toPx(), center = center)
        }

        // Center Digital Numeric Display & Gear
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 18.dp)
        ) {
            // Power Value
            Text(
                text = String.format("%.1f", absPower),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRegen) EcoGreen else if (absPower > 80f) PowerAmber else TextPrimary,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-0.5).sp
            )

            // Unit Label
            Text(
                text = if (isRegen) "REGEN kW" else "PWR kW",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isRegen) EcoGreen else TextSecondary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Classic 2018 Gear Selector Badge [ P | R | N | D ]
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SurfaceCardElevated)
                    .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = gear.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (gear == Gear.DRIVE) HyundaiBlue else TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
