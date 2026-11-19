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
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EvStation
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
import com.iskra.companion.ui.theme.CardBorder
import com.iskra.companion.ui.theme.CockpitBg
import com.iskra.companion.ui.theme.EcoGreen
import com.iskra.companion.ui.theme.HyundaiBlue
import com.iskra.companion.ui.theme.PowerAmber
import com.iskra.companion.ui.theme.SurfaceCard
import com.iskra.companion.ui.theme.SurfaceCardElevated
import com.iskra.companion.ui.theme.SurfaceCardSecondary
import com.iskra.companion.ui.theme.TextMuted
import com.iskra.companion.ui.theme.TextPrimary
import com.iskra.companion.ui.theme.TextSecondary
import kotlin.math.abs

/**
 * Classic 2018 800V DC Fast Charging Telemetry Screen.
 */
@Composable
fun ChargingScreen(
    telemetry: VehicleTelemetry,
    modifier: Modifier = Modifier
) {
    val isCharging = telemetry.charging.isCharging || telemetry.battery.powerKw > 0.5f
    val chargingKw = if (telemetry.charging.isCharging) telemetry.charging.chargingPowerKw else abs(telemetry.battery.powerKw)
    val isConnected = telemetry.isConnected || telemetry.isDemoMode

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBg)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Charging Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(SurfaceCard)
                .border(1.dp, CardBorder, RoundedCornerShape(26.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "CHARGING TELEMETRY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HyundaiBlue,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (isCharging) "800V DC Fast Charging (${String.format("%.1f", chargingKw)} kW)" else if (isConnected) "Port Connected (Standby)" else "EVSE Disconnected",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceCardElevated)
                    .border(1.dp, if (isCharging) EcoGreen.copy(alpha = 0.5f) else CardBorder, RoundedCornerShape(2.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isCharging) "CHARGING" else "STANDBY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCharging) EcoGreen else TextMuted,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // Primary 2x2 Telemetry Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Charging Power",
                value = if (isCharging) String.format("%.1f", chargingKw) else "0.0",
                unit = "kW",
                subValue = if (isCharging) "800V Architecture" else "Idle",
                icon = Icons.Default.EvStation,
                accentColor = EcoGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Current Draw",
                value = if (isCharging) String.format("%.1f", telemetry.charging.deliveredCurrentA) else "0.0",
                unit = "A",
                subValue = if (isCharging) "${telemetry.battery.voltage.toInt()} V Pack" else "0 V",
                icon = Icons.Default.ElectricBolt,
                accentColor = HyundaiBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Time to 80%",
                value = if (isCharging) "${telemetry.charging.estMinutesTo80}" else "--",
                unit = "min",
                subValue = if (isCharging) "Target 80% SOC" else "Standby",
                icon = Icons.Default.AvTimer,
                accentColor = HyundaiBlue,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Max Cell Temp",
                value = if (isConnected) String.format("%.1f", telemetry.battery.maxTempC) else "--",
                unit = "°C",
                subValue = if (isConnected) "Avg ${telemetry.battery.avgTempC.toInt()}°C" else "Standby",
                icon = Icons.Default.Thermostat,
                accentColor = PowerAmber,
                modifier = Modifier.weight(1f)
            )
        }

        // Charge Session Statistics
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SESSION DATA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                SessionRow("Energy Delivered", if (isCharging) "${String.format("%.2f", telemetry.charging.sessionKwhDelivered)} kWh" else "0.00 kWh")
                SessionRow("EVSE Pilot Signal", if (isCharging) "CCS Combo 1 (DC)" else "Disconnected")
                SessionRow("Battery Warmer (PTC)", if (telemetry.preconditioning.isActive) "Active (${String.format("%.1f", telemetry.preconditioning.heaterPowerKw)} kW)" else "Off")
            }
        }
    }
}

@Composable
private fun SessionRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontFamily = FontFamily.SansSerif)
    }
}
