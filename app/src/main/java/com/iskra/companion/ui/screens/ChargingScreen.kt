package com.iskra.companion.ui.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iskra.companion.data.model.VehicleTelemetry
import com.iskra.companion.ui.theme.CardBorder
import com.iskra.companion.ui.theme.CockpitBg
import com.iskra.companion.ui.theme.EcoGreen
import com.iskra.companion.ui.theme.HyundaiBlue
import com.iskra.companion.ui.theme.PowerAmber
import com.iskra.companion.ui.theme.PowerRed
import com.iskra.companion.ui.theme.SurfaceCard
import com.iskra.companion.ui.theme.SurfaceCardElevated
import com.iskra.companion.ui.theme.TextMuted
import com.iskra.companion.ui.theme.TextPrimary
import com.iskra.companion.ui.theme.TextSecondary
import kotlin.math.abs

/**
 * Charge tab: one big power ring, time to 80%, a state-of-charge bar with an 80% marker,
 * and six quiet numbers.
 */
@Composable
fun ChargingScreen(
    telemetry: VehicleTelemetry,
    modifier: Modifier = Modifier
) {
    val live = telemetry.isConnected || telemetry.isDemoMode
    val isCharging = telemetry.charging.isCharging || telemetry.battery.powerKw > 0.5f
    val kw = if (telemetry.charging.isCharging) telemetry.charging.chargingPowerKw else abs(telemetry.battery.powerKw)
    val soc = if (live) telemetry.battery.socDisplay else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CHARGING", fontSize = 11.sp, letterSpacing = 5.sp, color = HyundaiBlue, fontWeight = FontWeight.Medium)
                Text(
                    text = when {
                        isCharging -> "800V DC fast charge"
                        live -> "Plugged in, standing by"
                        else -> "No charger connected"
                    },
                    fontSize = 15.sp,
                    color = TextSecondary
                )
            }
            val color = if (isCharging) EcoGreen else TextMuted
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SurfaceCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(50))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(8.dp))
                Text(if (isCharging) "CHARGING" else "IDLE", fontSize = 11.sp, color = color, letterSpacing = 1.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(18.dp))

        PowerRing(
            kw = if (isCharging) kw else 0f,
            charging = isCharging,
            etaMin = telemetry.charging.estMinutesTo80,
            modifier = Modifier.fillMaxWidth().height(300.dp)
        )

        Spacer(Modifier.height(8.dp))

        // SOC bar with an 80% marker
        val animatedSoc by animateFloatAsState((soc / 100f).coerceIn(0f, 1f), tween(140), label = "chargeSoc")
        val socColor = when {
            soc > 25f -> EcoGreen
            soc > 12f -> PowerAmber
            else -> PowerRed
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SurfaceCardElevated)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(animatedSoc)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(socColor)
                )
                // 80% target marker
                Box(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(8.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(Modifier.width(2.dp).height(8.dp).background(TextPrimary))
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = if (live) "${soc.toInt()}%" else "--%",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = if (live) socColor else TextMuted
            )
        }
        Text("Target 80%", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(26.dp))

        Row(Modifier.fillMaxWidth()) {
            ChargeStat("ENERGY ADDED", if (isCharging) String.format("%.1f", telemetry.charging.sessionKwhDelivered) else "0.0", "kWh", Modifier.weight(1f))
            ChargeStat("CURRENT", if (isCharging) String.format("%.0f", telemetry.charging.deliveredCurrentA) else "0", "A", Modifier.weight(1f))
            ChargeStat("PACK", if (live) "${telemetry.battery.voltage.toInt()}" else "--", "V", Modifier.weight(1f))
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            ChargeStat("BATTERY TEMP", if (live) "${telemetry.battery.avgTempC.toInt()}" else "--", "°C", Modifier.weight(1f))
            ChargeStat("STATION LIMIT", if (isCharging) "${telemetry.charging.evseMaxPowerKw.toInt()}" else "--", "kW", Modifier.weight(1f))
            ChargeStat("TO 100%", if (isCharging) "${telemetry.charging.estMinutesTo100}" else "--", "min", Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))

        val warm = telemetry.battery.avgTempC >= telemetry.preconditioning.targetTempC
        Text(
            text = when {
                !live -> ""
                telemetry.preconditioning.isActive -> "Battery warmer on  ·  ${String.format("%.1f", telemetry.preconditioning.heaterPowerKw)} kW"
                warm -> "Battery is warm enough for the fastest charge"
                else -> "Battery is cool, so charging may be slower"
            },
            fontSize = 13.sp,
            color = if (warm) EcoGreen else TextSecondary
        )
    }
}

@Composable
private fun PowerRing(kw: Float, charging: Boolean, etaMin: Int, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState((kw / 250f).coerceIn(0f, 1f), tween(120), label = "powerRing")
    val color = if (charging) EcoGreen else TextMuted

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 6.dp.toPx()
            val d = minOf(size.width, size.height) - stroke * 2 - 8.dp.toPx()
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            val arcSize = Size(d, d)
            drawArc(
                color = SurfaceCardElevated,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (animated > 0f) {
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = 270f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.0f", kw),
                fontSize = 88.sp,
                lineHeight = 88.sp,
                fontWeight = FontWeight.ExtraLight,
                color = if (charging) TextPrimary else TextMuted
            )
            Text("kW", fontSize = 18.sp, color = TextSecondary)
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (charging) "$etaMin min to 80%" else "Not charging",
                fontSize = 15.sp,
                color = if (charging) HyundaiBlue else TextMuted,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ChargeStat(label: String, value: String, unit: String, modifier: Modifier = Modifier, color: Color = TextPrimary) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, letterSpacing = 1.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.Light, color = color)
            Text(" $unit", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}
