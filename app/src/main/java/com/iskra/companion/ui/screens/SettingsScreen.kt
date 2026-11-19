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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iskra.companion.data.model.ConnectionState
import com.iskra.companion.data.model.DiscoveredBluetoothDevice
import com.iskra.companion.data.model.SimulationScenario
import com.iskra.companion.data.model.VehicleModel
import com.iskra.companion.ui.theme.CardBorder
import com.iskra.companion.ui.theme.CardBorderSubtle
import com.iskra.companion.ui.theme.CockpitBg
import com.iskra.companion.ui.theme.EcoGreen
import com.iskra.companion.ui.theme.HyundaiBlue
import com.iskra.companion.ui.theme.PowerAmber
import com.iskra.companion.ui.theme.PowerRed
import com.iskra.companion.ui.theme.SurfaceCard
import com.iskra.companion.ui.theme.SurfaceCardElevated
import com.iskra.companion.ui.theme.SurfaceCardSecondary
import com.iskra.companion.ui.theme.TextMuted
import com.iskra.companion.ui.theme.TextPrimary
import com.iskra.companion.ui.theme.TextSecondary

/**
 * Classic 2018 Material Design Settings & Adapter Pairing Screen.
 * Functional, high-utility preferences with RSSI device rankings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    connectionState: ConnectionState,
    discoveredDevices: List<DiscoveredBluetoothDevice>,
    isDemoMode: Boolean,
    selectedVehicleModel: VehicleModel,
    onStartScan: () -> Unit,
    onConnectDevice: (DiscoveredBluetoothDevice) -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onSelectScenario: (SimulationScenario) -> Unit,
    onSelectVehicleModel: (VehicleModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var isVehicleDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBg)
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // OBD-II Adapter Section Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = HyundaiBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "OBD-II ADAPTER CONNECTION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                    }

                    val statusColor = when (connectionState) {
                        is ConnectionState.Connected -> EcoGreen
                        is ConnectionState.Connecting, is ConnectionState.Scanning -> HyundaiBlue
                        is ConnectionState.Error -> PowerRed
                        else -> PowerAmber
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceCardElevated)
                            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (connectionState) {
                                is ConnectionState.Connected -> "CONNECTED"
                                is ConnectionState.Connecting -> "CONNECTING"
                                is ConnectionState.Scanning -> "SCANNING"
                                is ConnectionState.Error -> "ERROR"
                                else -> "DISCONNECTED"
                            },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onStartScan,
                    colors = ButtonDefaults.buttonColors(containerColor = HyundaiBlue, contentColor = TextPrimary),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Scan for Adapters (Ranked by Signal)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                if (discoveredDevices.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "DISCOVERED DEVICES (BEST SIGNAL FIRST)",
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    discoveredDevices.forEachIndexed { index, device ->
                        val isBestSignal = index == 0
                        val signalPercent = ((device.rssi + 100) * 2).coerceIn(10, 100)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { onConnectDevice(device) },
                            shape = RoundedCornerShape(2.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceCardSecondary),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(
                                    if (isBestSignal) HyundaiBlue.copy(alpha = 0.8f) else CardBorderSubtle
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = device.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (device.isVeepeak) HyundaiBlue else TextPrimary
                                        )
                                        if (isBestSignal) {
                                            Spacer(modifier = Modifier.size(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(EcoGreen.copy(alpha = 0.15f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("BEST SIGNAL", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = EcoGreen)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.SignalCellularAlt,
                                            contentDescription = null,
                                            tint = if (isBestSignal) EcoGreen else TextMuted,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text(
                                            text = "${device.address} • $signalPercent% (${device.rssi} dBm)",
                                            fontSize = 9.sp,
                                            color = if (isBestSignal) EcoGreen else TextMuted,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onConnectDevice(device) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isBestSignal) HyundaiBlue else SurfaceCardElevated,
                                        contentColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(2.dp)
                                ) {
                                    Text("Pair", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Vehicle Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = HyundaiBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "VEHICLE PROFILE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = isVehicleDropdownExpanded,
                    onExpandedChange = { isVehicleDropdownExpanded = !isVehicleDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedVehicleModel.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isVehicleDropdownExpanded) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SurfaceCardSecondary,
                            unfocusedContainerColor = SurfaceCardSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedIndicatorColor = HyundaiBlue,
                            unfocusedIndicatorColor = CardBorderSubtle
                        ),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = isVehicleDropdownExpanded,
                        onDismissRequest = { isVehicleDropdownExpanded = false },
                        modifier = Modifier.background(SurfaceCardSecondary)
                    ) {
                        VehicleModel.entries.forEach { model ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(model.displayName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                        Text("${model.packCapacityKwh} kWh • ${model.cellCount} Cells", fontSize = 10.sp, color = TextSecondary)
                                    }
                                },
                                onClick = {
                                    onSelectVehicleModel(model)
                                    isVehicleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Demo Simulator Card
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = HyundaiBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "DEMO SIMULATOR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Switch(
                        checked = isDemoMode,
                        onCheckedChange = onToggleDemoMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TextPrimary,
                            checkedTrackColor = HyundaiBlue,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = SurfaceCardSecondary
                        )
                    )
                }

                if (isDemoMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "SIMULATION PRESETS", fontSize = 9.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    SimulationScenario.entries.forEach { scenario ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(2.dp))
                                .background(SurfaceCardSecondary)
                                .clickable { onSelectScenario(scenario) }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(scenario.displayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(scenario.description, fontSize = 9.sp, color = TextSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }

        // Android Auto Setup Guide Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = HyundaiBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "ANDROID AUTO SETUP",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .background(SurfaceCardSecondary)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("1. Open Android Auto settings on your phone.", fontSize = 10.sp, color = TextPrimary)
                    Text("2. Tap 'Version' 10 times consecutively to enable Developer Mode.", fontSize = 10.sp, color = TextPrimary)
                    Text("3. Top right 3 dots ➔ Developer settings ➔ Enable 'Unknown sources'.", fontSize = 10.sp, color = HyundaiBlue, fontWeight = FontWeight.SemiBold)
                    Text("4. Plug in to car USB ➔ 'Iskra Auto' will appear on head unit.", fontSize = 10.sp, color = EcoGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
