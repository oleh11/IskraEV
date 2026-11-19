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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.HealthAndSafety
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
import com.iskra.companion.ui.components.CellVoltageHeatmap
import com.iskra.companion.ui.components.StatCard
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
 * Classic 2018 Battery Health & 192-Cell Matrix Diagnostic Screen.
 */
@Composable
fun BatteryCellsScreen(
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
                text = "HIGH VOLTAGE BATTERY SYSTEM",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = HyundaiBlue,
                letterSpacing = 0.5.sp
            )
            Text(
                text = if (isConnected) "BMS ONLINE" else "OFFLINE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isConnected) EcoGreen else TextMuted,
                fontFamily = FontFamily.Monospace
            )
        }

        // 192-Cell Voltage Heatmap Matrix (32 Modules)
        CellVoltageHeatmap(cellData = telemetry.cells)

        // Battery Health Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "State of Health",
                value = if (isConnected) String.format("%.1f", telemetry.battery.soh) else "--",
                unit = "%",
                subValue = if (isConnected) "Nominal 100%" else "Standby",
                icon = Icons.Default.HealthAndSafety,
                accentColor = EcoGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "Isolation Resist.",
                value = if (isConnected) "${telemetry.battery.isolationResistanceKohm}" else "--",
                unit = "kΩ",
                subValue = if (isConnected) "Safe > 500 kΩ" else "Standby",
                icon = Icons.Default.ElectricBolt,
                accentColor = HyundaiBlue,
                modifier = Modifier.weight(1f)
            )
        }

        // Energy Throughput Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "LIFETIME ENERGY THROUGHPUT",
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
                        Text(text = "Cumulative Charged", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = if (isConnected) "${String.format("%.1f", telemetry.battery.cumulativeEnergyChargedKwh)} kWh" else "-- kWh",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoGreen,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Cumulative Discharged", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = if (isConnected) "${String.format("%.1f", telemetry.battery.cumulativeEnergyDischargedKwh)} kWh" else "-- kWh",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HyundaiBlue,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
