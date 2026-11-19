package com.iskra.companion.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iskra.companion.data.model.DtcItem
import com.iskra.companion.data.model.DtcSeverity
import com.iskra.companion.data.model.VehicleTelemetry
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
import com.iskra.companion.util.AppLogger
import com.iskra.companion.util.LogEntry
import com.iskra.companion.util.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Classic 2018 Multi-ECU Bus Scanner, DTC Diagnostics, CSV Exporter, and Live Terminal Logger.
 */
@Composable
fun DiagnosticsScreen(
    telemetry: VehicleTelemetry,
    onExportCsv: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTagFilter by remember { mutableStateOf<String?>(null) }
    val logs by AppLogger.logs.collectAsStateWithLifecycle()

    // Read stored crash log from SharedPreferences
    val prefs = remember { context.getSharedPreferences("iskra_crash_store", Context.MODE_PRIVATE) }
    var lastCrashTrace by remember { mutableStateOf(prefs.getString("last_crash_trace", null)) }
    var lastCrashMsg by remember { mutableStateOf(prefs.getString("last_crash_message", null)) }
    val lastCrashTime by remember { mutableStateOf(prefs.getLong("last_crash_time", 0L)) }

    val filteredLogs = remember(logs, selectedTagFilter) {
        if (selectedTagFilter == null) logs
        else logs.filter { it.tag.contains(selectedTagFilter!!, ignoreCase = true) }
    }

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
                .clip(RoundedCornerShape(26.dp))
                .background(SurfaceCard)
                .border(1.dp, CardBorder, RoundedCornerShape(26.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SYSTEM DIAGNOSTICS & DTC",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = HyundaiBlue,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "CAN BUS LOGGER",
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif
            )
        }

        // On-Device Saved Crash Report Box (if present)
        if (lastCrashTrace != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PowerRed.copy(alpha = 0.8f)))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = PowerRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(
                                text = "RECENT CRASH CAPTURED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PowerRed
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Crash Log",
                                tint = HyundaiBlue,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val crashText = "ISKRA EV CRASH REPORT\nTime: ${Date(lastCrashTime)}\nDevice: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})\nMessage: $lastCrashMsg\n\n$lastCrashTrace"
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Iskra Crash Log", crashText))
                                        Toast.makeText(context, "Crash log copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    }
                            )
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Crash Log",
                                tint = TextMuted,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable {
                                        prefs.edit().clear().apply()
                                        lastCrashTrace = null
                                        lastCrashMsg = null
                                        Toast.makeText(context, "Crash log cleared", Toast.LENGTH_SHORT).show()
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${Date(lastCrashTime)}: $lastCrashMsg",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(85.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceCardSecondary)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = lastCrashTrace ?: "",
                            fontSize = 9.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }

        // Live Diagnostic Terminal Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
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
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = HyundaiBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = "LIVE PROTOCOL TERMINAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Logs",
                            tint = HyundaiBlue,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    val text = AppLogger.exportFormattedLogs()
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Iskra Logs", text))
                                    Toast.makeText(context, "Copied ${logs.size} log lines to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                        )
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Logs",
                            tint = TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    AppLogger.clear()
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tag Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(null to "ALL", "BLUETOOTH" to "BT", "ELM327" to "ELM", "CAN-RX" to "CAN").forEach { (tag, label) ->
                        val isSelected = selectedTagFilter == tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) HyundaiBlue else SurfaceCardSecondary)
                                .clickable { selectedTagFilter = tag }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) TextPrimary else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Log Output Terminal Window
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(CockpitBg)
                        .border(1.dp, CardBorderSubtle, RoundedCornerShape(2.dp))
                        .padding(6.dp)
                ) {
                    if (filteredLogs.isEmpty()) {
                        Text(
                            text = "Console ready. Connect adapter to view handshake and CAN stream...",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontFamily = FontFamily.SansSerif
                        )
                    } else {
                        val listState = rememberLazyListState()
                        LaunchedEffect(filteredLogs.size) {
                            listState.animateScrollToItem(filteredLogs.size - 1)
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(filteredLogs) { entry ->
                                LogLineView(entry = entry)
                            }
                        }
                    }
                }
            }
        }

        // Active Diagnostic Trouble Codes (DTC)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
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
                        text = "DIAGNOSTIC TROUBLE CODES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )

                    val dtcCount = telemetry.diagnostics.activeDtcs.size
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (dtcCount == 0) EcoGreen.copy(alpha = 0.15f) else PowerRed.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (dtcCount == 0) "NO FAULTS" else "$dtcCount DTC FOUND",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dtcCount == 0) EcoGreen else PowerRed,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (telemetry.diagnostics.activeDtcs.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(2.dp))
                            .background(SurfaceCardSecondary)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EcoGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Column {
                            Text(
                                text = "All Powertrain & BMS Systems Normal",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "No active DTCs detected across CAN 11-bit ECUs",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    telemetry.diagnostics.activeDtcs.forEach { dtc ->
                        DtcItemRow(dtc = dtc)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogLineView(entry: LogEntry) {
    val timeStr = remember(entry.timestamp) {
        SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(entry.timestamp))
    }
    val levelColor = when (entry.level) {
        LogLevel.DEBUG -> TextMuted
        LogLevel.INFO -> EcoGreen
        LogLevel.WARN -> PowerAmber
        LogLevel.ERROR -> PowerRed
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$timeStr ",
            fontSize = 9.sp,
            color = TextMuted,
            fontFamily = FontFamily.SansSerif
        )
        Text(
            text = "[${entry.tag}] ",
            fontSize = 9.sp,
            color = HyundaiBlue,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = entry.message,
            fontSize = 9.sp,
            color = levelColor,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
private fun DtcItemRow(dtc: DtcItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(SurfaceCardSecondary)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(2.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${dtc.code} - ${dtc.module}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PowerRed,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = dtc.description,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}
