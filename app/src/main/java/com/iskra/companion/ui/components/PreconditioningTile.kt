package com.iskra.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iskra.companion.data.model.BatteryData
import com.iskra.companion.data.model.PreconditioningData
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
 * Classic 2018 Battery Preconditioning Readiness Diagnostic Panel.
 * Clean, structured status bar with target temperature countdown and PTC heater state.
 */
@Composable
fun PreconditioningTile(
    preconditioning: PreconditioningData,
    battery: BatteryData,
    modifier: Modifier = Modifier
) {
    val isReady = battery.minTempC >= 21.0f
    val isWarming = preconditioning.isActive || (battery.minTempC < 21.0f && battery.powerKw < -10f)
    val statusColor = if (isWarming) PowerAmber else if (isReady) EcoGreen else HyundaiBlue
    val statusText = if (isWarming) "WARMING ACTIVE" else if (isReady) "READY (≥21°C)" else "STANDBY"

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PRECONDITIONING / THERMAL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(SurfaceCardElevated)
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Primary Readout: ETA Countdown or Ready Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isWarming && !isReady) {
                            Icon(
                                imageVector = Icons.Default.HourglassTop,
                                contentDescription = null,
                                tint = PowerAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = if (isReady) "235+ kW Peak Ready" else if (isWarming) "${preconditioning.timeToTargetMin} min to 21°C" else "Target: 21°C (70°F)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReady) EcoGreen else if (isWarming) PowerAmber else TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = if (isReady) "Optimal 800V charging curve unlocked" else "Optimal temperature for DC Fast Charging",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                if (preconditioning.heaterPowerKw > 0.1f) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format("%.1f", preconditioning.heaterPowerKw)} kW",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PowerAmber,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "PTC HEATER",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Temperature Matrix Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(3.dp))
                    .background(SurfaceCardElevated)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min: ${battery.minTempC.toInt()}°C",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Max: ${battery.maxTempC.toInt()}°C",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Inlet: ${preconditioning.coolantInletTempC.toInt()}°C",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Target: 21°C",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
