package com.iskra.companion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.iskra.companion.data.model.VehicleTelemetry
import com.iskra.companion.ui.components.BatterySocRing
import com.iskra.companion.ui.components.PowerDialGauge
import com.iskra.companion.ui.components.PreconditioningTile
import com.iskra.companion.ui.components.RangeEfficiencyTile
import com.iskra.companion.ui.components.StatCard
import com.iskra.companion.ui.components.TelemetryLineChart
import com.iskra.companion.ui.theme.CardBorder
import com.iskra.companion.ui.theme.CockpitBg
import com.iskra.companion.ui.theme.EcoGreen
import com.iskra.companion.ui.theme.HyundaiBlue
import com.iskra.companion.ui.theme.IoniqCyan
import com.iskra.companion.ui.theme.PowerAmber
import com.iskra.companion.ui.theme.PowerRed
import com.iskra.companion.ui.theme.SurfaceCard
import com.iskra.companion.ui.theme.SurfaceCardElevated
import com.iskra.companion.ui.theme.TextMuted
import com.iskra.companion.ui.theme.TextPrimary
import com.iskra.companion.ui.theme.TextSecondary
import kotlin.math.abs

/**
 * Main Cockpit Driving Dashboard Screen.
 * Authentic 2018 Material Dark Automotive Interface.
 */
@Composable
fun DashboardScreen(
    telemetry: VehicleTelemetry,
    powerHistory: List<Pair<Long, Float>>,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBg)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Status Header
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
            Column {
                Text(
                    text = telemetry.vehicleModel.displayName.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HyundaiBlue,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (telemetry.isConnected) "CAN BUS CONNECTED (500 kbps)" else if (telemetry.isDemoMode) "DEMO SIMULATOR ACTIVE" else "NO ADAPTER CONNECTED",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            val statusColor = when {
                telemetry.isConnected -> EcoGreen
                telemetry.isDemoMode -> HyundaiBlue
                else -> PowerAmber
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(SurfaceCardElevated)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (telemetry.isConnected) "ONLINE" else if (telemetry.isDemoMode) "SIM" else "OFFLINE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Connection Alert Banner (if disconnected)
        if (!telemetry.isConnected && !telemetry.isDemoMode) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSettings() },
                shape = RoundedCornerShape(4.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(HyundaiBlue.copy(alpha = 0.6f)))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            tint = HyundaiBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Column {
                            Text(
                                text = "OBD-II Adapter Disconnected",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Tap to pair any OBD-II dongle (Veepeak, vLinker, OBDLink, etc.)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onNavigateToSettings,
                        colors = ButtonDefaults.buttonColors(containerColor = HyundaiBlue, contentColor = TextPrimary),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Connect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Twin Instrument Cluster Gauges (Power & Battery SOC)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PowerDialGauge(
                powerKw = telemetry.battery.powerKw,
                gear = telemetry.drive.gear,
                speedKmh = telemetry.drive.speedKmh,
                size = 165.dp
            )

            BatterySocRing(
                battery = telemetry.battery,
                size = 165.dp
            )
        }

        // Range & 15-Minute Efficiency Tile (gburlingame/ioniq-app Model)
        RangeEfficiencyTile(
            estRangeMiles = telemetry.battery.estRangeMiles,
            estRangeKm = telemetry.battery.estRangeKm,
            efficiency = telemetry.efficiency,
            isMilesUnit = true
        )

        // Preconditioning Readiness & Battery Warmer ETA Tile
        PreconditioningTile(
            preconditioning = telemetry.preconditioning,
            battery = telemetry.battery
        )

        // Live Power Dynamic Flow Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "POWER FLOW TELEMETRY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (telemetry.isConnected || telemetry.isDemoMode) "${String.format("%.1f", abs(telemetry.battery.powerKw))} kW" else "0.0 kW",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (telemetry.battery.powerKw > 0) EcoGreen else HyundaiBlue,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TelemetryLineChart(
                    dataPoints = powerHistory,
                    lineColor = if (telemetry.battery.powerKw > 0) EcoGreen else HyundaiBlue
                )
            }
        }

        // Primary Telemetry 2x2 Grid (Pack Voltage, Battery Temp, Motor RPM, Aux 12V Battery)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Pack Voltage",
                value = if (telemetry.isConnected || telemetry.isDemoMode) String.format("%.1f", telemetry.battery.voltage) else "--",
                unit = "V",
                subValue = if (telemetry.isConnected || telemetry.isDemoMode) "Nominal 720V" else "Standby",
                icon = Icons.Default.ElectricBolt,
                accentColor = HyundaiBlue,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Battery Temp",
                value = if (telemetry.isConnected || telemetry.isDemoMode) String.format("%.1f", telemetry.battery.avgTempC) else "--",
                unit = "°C",
                subValue = if (telemetry.isConnected || telemetry.isDemoMode) "Min ${telemetry.battery.minTempC.toInt()}° • Max ${telemetry.battery.maxTempC.toInt()}°" else "Standby",
                icon = Icons.Default.Thermostat,
                accentColor = PowerAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "Motor RPM",
                value = if (telemetry.isConnected || telemetry.isDemoMode) "${telemetry.drive.frontMotorRpm}" else "--",
                unit = "rpm",
                subValue = if (telemetry.isConnected || telemetry.isDemoMode) "Motor ${telemetry.drive.motorTempC.toInt()}°C" else "Standby",
                icon = Icons.Default.Speed,
                accentColor = HyundaiBlue,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Aux 12V Battery",
                value = if (telemetry.isConnected || telemetry.isDemoMode) String.format("%.2f", telemetry.auxBattery.voltage) else "--",
                unit = "V",
                subValue = if (telemetry.isConnected || telemetry.isDemoMode) "SOC ${telemetry.auxBattery.soc.toInt()}% • LDC Active" else "Standby",
                icon = Icons.Default.ElectricBolt,
                accentColor = EcoGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // Subsystem Energy Breakdown Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ENERGY CONSUMPTION BREAKDOWN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                PowerRow(
                    label = "Drivetrain (Front & Rear Motors)",
                    kw = if (telemetry.isConnected || telemetry.isDemoMode) telemetry.drive.motorPowerDrawKw else 0f,
                    color = HyundaiBlue
                )
                PowerRow(
                    label = "Climate Control (HVAC / Heat Pump)",
                    kw = if (telemetry.isConnected || telemetry.isDemoMode) telemetry.climate.hvacPowerDrawKw else 0f,
                    color = IoniqCyan
                )
                PowerRow(
                    label = "High-Voltage Battery Heater",
                    kw = if (telemetry.isConnected || telemetry.isDemoMode) telemetry.preconditioning.heaterPowerKw else 0f,
                    color = PowerAmber
                )
                PowerRow(
                    label = "Auxiliary Electronics (12V LDC)",
                    kw = if (telemetry.isConnected || telemetry.isDemoMode) 0.35f else 0f,
                    color = EcoGreen
                )
            }
        }
    }
}

@Composable
fun PowerRow(
    label: String,
    kw: Float,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
            Text(
                text = "  $label",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Text(
            text = "${String.format("%.2f", kw)} kW",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            fontFamily = FontFamily.Monospace
        )
    }
}
