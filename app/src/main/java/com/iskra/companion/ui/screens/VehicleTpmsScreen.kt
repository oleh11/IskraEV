package com.iskra.companion.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iskra.companion.data.model.VehicleTelemetry
import com.iskra.companion.ui.components.StatCard
import com.iskra.companion.ui.components.TpmsCarVisualizer
import com.iskra.companion.ui.theme.CardBorder
import com.iskra.companion.ui.theme.CockpitBg
import com.iskra.companion.ui.theme.EcoGreen
import com.iskra.companion.ui.theme.HyundaiBlue
import com.iskra.companion.ui.theme.PowerAmber
import com.iskra.companion.ui.theme.SurfaceCard
import com.iskra.companion.ui.theme.SurfaceCardElevated
import com.iskra.companion.ui.theme.TextMuted
import com.iskra.companion.ui.theme.TextPrimary
import com.iskra.companion.ui.theme.TextSecondary

/**
 * Classic 2018 Vehicle Telemetry & TPMS Diagnostics Screen.
 */
@Composable
fun VehicleTpmsScreen(
    telemetry: VehicleTelemetry,
    modifier: Modifier = Modifier
) {
    val isConnected = telemetry.isConnected || telemetry.isDemoMode

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBg)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceCard)
                .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VEHICLE SYSTEMS & TPMS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = HyundaiBlue,
                letterSpacing = 0.5.sp
            )
            Text(
                text = if (isConnected) "ACTIVE" else "STANDBY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isConnected) EcoGreen else TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }

        // 4-Wheel TPMS Visualizer Card
        TpmsCarVisualizer(tpms = telemetry.tpms)

        // Drivetrain & Motor Thermals
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "DUAL MOTOR & INVERTER TELEMETRY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Front Motor RPM", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = if (isConnected) "${telemetry.drive.frontMotorRpm}" else "--",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HyundaiBlue,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text(text = "Rear Motor RPM", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = if (isConnected) "${telemetry.drive.rearMotorRpm}" else "--",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HyundaiBlue,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column {
                        Text(text = "Inverter Temp", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = if (isConnected) "${telemetry.drive.inverterTempC.toInt()}°C" else "--",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PowerAmber,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // HVAC & Climate Subsystems Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Cabin Climate",
                value = if (isConnected) String.format("%.1f", telemetry.climate.cabinTempC) else "--",
                unit = "°C",
                subValue = if (isConnected) "Ambient ${telemetry.climate.ambientTempC.toInt()}°C" else "Off",
                icon = Icons.Default.AcUnit,
                accentColor = HyundaiBlue,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "HVAC Power",
                value = if (isConnected) String.format("%.2f", telemetry.climate.hvacPowerDrawKw) else "--",
                unit = "kW",
                subValue = if (isConnected) "Heat Pump Active" else "Off",
                icon = Icons.Default.Air,
                accentColor = EcoGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
