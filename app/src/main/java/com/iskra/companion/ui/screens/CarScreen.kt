package com.iskra.companion.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import com.iskra.companion.ui.theme.SurfaceCardSecondary
import com.iskra.companion.ui.theme.TextInverse
import com.iskra.companion.ui.theme.TextMuted
import com.iskra.companion.ui.theme.TextPrimary
import com.iskra.companion.ui.theme.TextSecondary
import kotlin.math.abs

/**
 * The Car tab: a car-first hero screen. Big range up top, a top-down car that shows tyre
 * pressures at the wheels, the battery as a liquid-fill pack and motors on the axles.
 * Tap the tyres, the pack or a motor to open its detail page.
 */
@Composable
fun CarScreen(
    telemetry: VehicleTelemetry,
    onOpenPower: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenTyres: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val live = telemetry.isConnected || telemetry.isDemoMode
    val b = telemetry.battery

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 14.dp)
    ) {
        // Header: wordmark, model, connection pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ISKRA", fontSize = 11.sp, letterSpacing = 5.sp, color = HyundaiBlue, fontWeight = FontWeight.Medium)
                Text(telemetry.vehicleModel.displayName, fontSize = 15.sp, color = TextSecondary)
            }
            ConnectionPill(telemetry, onClick = onOpenSetup)
        }

        Spacer(Modifier.height(14.dp))

        // Hero range
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (live) "${b.estRangeMiles.toInt()}" else "--",
                fontSize = 80.sp,
                lineHeight = 80.sp,
                fontWeight = FontWeight.ExtraLight,
                color = TextPrimary
            )
            Text(
                text = " mi",
                fontSize = 22.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        Text(
            text = if (live) "${b.estRangeKm.toInt()} km  ·  ${String.format("%.1f", b.remainingEnergyKwh)} kWh available"
            else "Connect an OBD-II adapter, or start the demo in Setup",
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(Modifier.height(16.dp))
        SocBar(soc = if (live) b.socDisplay else 0f)

        Spacer(Modifier.height(6.dp))

        CarVisual(
            t = telemetry,
            live = live,
            onTyres = onOpenTyres,
            onBattery = onOpenBattery,
            onPower = onOpenPower
        )

        Text(
            text = "Tap the tyres, battery or motors for detail",
            fontSize = 12.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        // Live numbers
        val powerKw = b.powerKw
        val regen = powerKw > 0.5f
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Metric(
                label = if (regen) "REGEN" else "POWER",
                value = if (live) String.format("%.1f", abs(powerKw)) else "--",
                unit = "kW",
                color = if (regen) EcoGreen else TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Metric(
                label = "EFFICIENCY",
                value = if (live) String.format("%.1f", telemetry.efficiency.currentMiPerKwh) else "--",
                unit = "mi/kWh",
                modifier = Modifier.weight(1f)
            )
            Metric(
                label = "PACK TEMP",
                value = if (live) "${b.avgTempC.toInt()}" else "--",
                unit = "°C",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(14.dp))

        // Quick links to the detail pages
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LinkChip("Power & efficiency", onOpenPower)
            LinkChip("Battery cells", onOpenBattery)
            LinkChip("Tyres", onOpenTyres)
            LinkChip("Diagnostics", onOpenDiagnostics)
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ConnectionPill(t: VehicleTelemetry, onClick: () -> Unit) {
    val (label, color) = when {
        t.isConnected -> "ONLINE" to EcoGreen
        t.isDemoMode -> "DEMO" to HyundaiBlue
        else -> "CONNECT" to PowerAmber
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SurfaceCard)
            .border(1.dp, CardBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color, letterSpacing = 1.sp)
    }
}

@Composable
private fun SocBar(soc: Float) {
    val fraction = (soc / 100f).coerceIn(0f, 1f)
    val color = when {
        soc > 25f -> EcoGreen
        soc > 12f -> PowerAmber
        else -> PowerRed
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(SurfaceCardElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = if (soc > 0f) "${soc.toInt()}%" else "--%",
            fontSize = 20.sp,
            fontWeight = FontWeight.Light,
            color = if (soc > 0f) color else TextMuted
        )
    }
}

@Composable
private fun Metric(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, letterSpacing = 1.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Light, color = color)
            Text(" $unit", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 5.dp))
        }
    }
}

@Composable
private fun LinkChip(text: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(HyundaiBlue)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextInverse)
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextInverse, modifier = Modifier.size(18.dp))
    }
}

private fun tyreColor(psi: Float, recommended: Float): Color = when {
    psi <= 0f -> TextMuted
    psi < recommended - 6f -> PowerRed
    psi < recommended - 3f -> PowerAmber
    else -> EcoGreen
}

@Composable
private fun CarVisual(
    t: VehicleTelemetry,
    live: Boolean,
    onTyres: () -> Unit,
    onBattery: () -> Unit,
    onPower: () -> Unit
) {
    val carW = 150.dp
    val carH = 262.dp
    val topPad = 10.dp
    val wheelW = 13.dp
    val wheelH = 56.dp
    val rec = t.tpms.recommendedPsi

    val soc = if (live) (t.battery.socDisplay / 100f).coerceIn(0f, 1f) else 0f
    val socColor = when {
        soc > 0.25f -> EcoGreen
        soc > 0.12f -> PowerAmber
        else -> PowerRed
    }
    val powerKw = t.battery.powerKw
    val motorColor = when {
        !live -> TextMuted
        powerKw > 0.5f -> EcoGreen
        powerKw < -0.5f -> HyundaiBlue
        else -> TextSecondary
    }
    val tyres = listOf(
        Triple(t.tpms.frontLeftPsi, t.tpms.frontLeftTempC, true to true),
        Triple(t.tpms.frontRightPsi, t.tpms.frontRightTempC, false to true),
        Triple(t.tpms.rearLeftPsi, t.tpms.rearLeftTempC, true to false),
        Triple(t.tpms.rearRightPsi, t.tpms.rearRightTempC, false to false)
    )
    val noRipple = remember { MutableInteractionSource() }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(carH + topPad * 2)) {
        val carLeft = (maxWidth - carW) / 2
        val frontAxleY = topPad + carH * 0.16f
        val rearAxleY = topPad + carH * 0.84f

        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = carW.toPx()
            val ch = carH.toPx()
            val x0 = carLeft.toPx()
            val y0 = topPad.toPx()

            // Body
            val bodyRadius = CornerRadius(cw * 0.36f)
            drawRoundRect(
                color = SurfaceCardSecondary,
                topLeft = Offset(x0, y0),
                size = Size(cw, ch),
                cornerRadius = bodyRadius
            )
            drawRoundRect(
                color = Color(0xFF3A3A3E),
                topLeft = Offset(x0, y0),
                size = Size(cw, ch),
                cornerRadius = bodyRadius,
                style = Stroke(width = 1.5.dp.toPx())
            )
            // Headlights (front, gold) and tail lights (rear, red)
            drawRoundRect(HyundaiBlue, Offset(x0 + cw * 0.14f, y0 + ch * 0.012f), Size(cw * 0.26f, 3.dp.toPx()), CornerRadius(2.dp.toPx()))
            drawRoundRect(HyundaiBlue, Offset(x0 + cw * 0.60f, y0 + ch * 0.012f), Size(cw * 0.26f, 3.dp.toPx()), CornerRadius(2.dp.toPx()))
            drawRoundRect(PowerRed, Offset(x0 + cw * 0.14f, y0 + ch * 0.988f - 3.dp.toPx()), Size(cw * 0.26f, 3.dp.toPx()), CornerRadius(2.dp.toPx()))
            drawRoundRect(PowerRed, Offset(x0 + cw * 0.60f, y0 + ch * 0.988f - 3.dp.toPx()), Size(cw * 0.26f, 3.dp.toPx()), CornerRadius(2.dp.toPx()))

            // Windscreen and rear glass
            val glass = Path().apply {
                moveTo(x0 + cw * 0.24f, y0 + ch * 0.275f)
                lineTo(x0 + cw * 0.76f, y0 + ch * 0.275f)
                lineTo(x0 + cw * 0.70f, y0 + ch * 0.20f)
                lineTo(x0 + cw * 0.30f, y0 + ch * 0.20f)
                close()
            }
            drawPath(glass, color = SurfaceCardSecondary)
            val rear = Path().apply {
                moveTo(x0 + cw * 0.24f, y0 + ch * 0.745f)
                lineTo(x0 + cw * 0.76f, y0 + ch * 0.745f)
                lineTo(x0 + cw * 0.70f, y0 + ch * 0.815f)
                lineTo(x0 + cw * 0.30f, y0 + ch * 0.815f)
                close()
            }
            drawPath(rear, color = SurfaceCardSecondary)

            // Axles
            val frontY = y0 + ch * 0.16f
            val rearY = y0 + ch * 0.84f
            drawLine(CardBorder, Offset(x0 - 4.dp.toPx(), frontY), Offset(x0 + cw + 4.dp.toPx(), frontY), 1.5.dp.toPx())
            drawLine(CardBorder, Offset(x0 - 4.dp.toPx(), rearY), Offset(x0 + cw + 4.dp.toPx(), rearY), 1.5.dp.toPx())

            // Battery pack (liquid fill from the bottom)
            val px = x0 + cw * 0.17f
            val py = y0 + ch * 0.315f
            val pw = cw * 0.66f
            val ph = ch * 0.40f
            drawRoundRect(
                color = SurfaceCardElevated,
                topLeft = Offset(px, py),
                size = Size(pw, ph),
                cornerRadius = CornerRadius(18.dp.toPx())
            )
            if (soc > 0f) {
                val fillH = ph * soc
                drawRoundRect(
                    color = socColor.copy(alpha = 0.85f),
                    topLeft = Offset(px, py + ph - fillH),
                    size = Size(pw, fillH),
                    cornerRadius = CornerRadius(18.dp.toPx())
                )
            }
            // module lines
            for (i in intArrayOf(1, 5)) {
                val ly = py + ph * i / 6f
                drawLine(CockpitBg.copy(alpha = 0.25f), Offset(px + 10.dp.toPx(), ly), Offset(px + pw - 10.dp.toPx(), ly), 1.dp.toPx())
            }
            drawRoundRect(
                color = HyundaiBlue.copy(alpha = 0.55f),
                topLeft = Offset(px, py),
                size = Size(pw, ph),
                cornerRadius = CornerRadius(18.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // Wheels
            val ww = wheelW.toPx()
            val wh = wheelH.toPx()
            val leftX = x0 - ww / 2f
            val rightX = x0 + cw - ww / 2f
            fun wheel(x: Float, cy: Float, c: Color) = drawRoundRect(
                color = c,
                topLeft = Offset(x, cy - wh / 2f),
                size = Size(ww, wh),
                cornerRadius = CornerRadius(5.dp.toPx())
            )
            wheel(leftX, frontY, if (live) tyreColor(tyres[0].first, rec) else TextMuted)
            wheel(rightX, frontY, if (live) tyreColor(tyres[1].first, rec) else TextMuted)
            wheel(leftX, rearY, if (live) tyreColor(tyres[2].first, rec) else TextMuted)
            wheel(rightX, rearY, if (live) tyreColor(tyres[3].first, rec) else TextMuted)

            // Motors on the axles
            for (my in listOf(frontY, rearY)) {
                val c = Offset(x0 + cw / 2f, my)
                drawCircle(CockpitBg, radius = 12.dp.toPx(), center = c)
                drawCircle(motorColor, radius = 12.dp.toPx(), center = c, style = Stroke(width = 2.dp.toPx()))
                drawCircle(motorColor, radius = 3.5.dp.toPx(), center = c)
            }
        }

        // Tyre pressure labels at each wheel (tap to open tyres)
        val labelW = 64.dp
        tyres.forEachIndexed { index, tyre ->
            val isLeft = tyre.third.first
            val isFront = tyre.third.second
            val cy = if (isFront) frontAxleY else rearAxleY
            val x = if (isLeft) carLeft - wheelW / 2 - 8.dp - labelW else carLeft + carW + wheelW / 2 + 8.dp
            Column(
                horizontalAlignment = if (isLeft) Alignment.End else Alignment.Start,
                modifier = Modifier
                    .offset(x = x, y = cy - 24.dp)
                    .width(labelW)
                    .clickable(interactionSource = noRipple, indication = null, onClick = onTyres)
            ) {
                val psi = tyre.first
                Text(
                    text = if (live && psi > 0f) String.format("%.0f", psi) else "--",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = if (live) tyreColor(psi, rec) else TextMuted
                )
                Text(
                    text = if (live && psi > 0f) "PSI  ·  ${tyre.second.toInt()}°" else "PSI",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }

        // Battery readout inside the pack (tap to open cells)
        val packTop = topPad + carH * 0.315f
        val packH = carH * 0.40f
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .offset(x = carLeft + carW * 0.17f, y = packTop)
                .width(carW * 0.66f)
                .height(packH)
                .clickable(interactionSource = noRipple, indication = null, onClick = onBattery)
        ) {
            Text(
                text = if (live) "${t.battery.socDisplay.toInt()}%" else "--",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = if (soc > 0.5f) TextInverse else TextPrimary
            )
            Text(
                text = "HV BATTERY",
                fontSize = 9.sp,
                letterSpacing = 1.5.sp,
                color = if (soc > 0.5f) TextInverse.copy(alpha = 0.7f) else TextMuted
            )
        }

        // Motor tap targets
        for (my in listOf(frontAxleY, rearAxleY)) {
            Box(
                modifier = Modifier
                    .offset(x = carLeft + carW / 2 - 24.dp, y = my - 24.dp)
                    .size(48.dp)
                    .clickable(interactionSource = noRipple, indication = null, onClick = onPower)
            )
        }
    }
}
