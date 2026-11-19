package com.iskra.companion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iskra.companion.data.model.CellData
import com.iskra.companion.ui.theme.CardBorder
import com.iskra.companion.ui.theme.CardBorderSubtle
import com.iskra.companion.ui.theme.CellCool
import com.iskra.companion.ui.theme.CellHot
import com.iskra.companion.ui.theme.CellNominal
import com.iskra.companion.ui.theme.CellWarm
import com.iskra.companion.ui.theme.EcoGreen
import com.iskra.companion.ui.theme.HyundaiBlue
import com.iskra.companion.ui.theme.SurfaceCard
import com.iskra.companion.ui.theme.SurfaceCardElevated
import com.iskra.companion.ui.theme.SurfaceCardSecondary
import com.iskra.companion.ui.theme.TextMuted
import com.iskra.companion.ui.theme.TextPrimary
import com.iskra.companion.ui.theme.TextSecondary

/**
 * Classic 2018 Diagnostic Battery Cell Voltage Matrix (192 Cells).
 * High-density automotive diagnostic grid with cell balance inspection.
 */
@Composable
fun CellVoltageHeatmap(
    cellData: CellData,
    modifier: Modifier = Modifier
) {
    var selectedCellIndex by remember { mutableStateOf<Int?>(null) }
    val hasCells = cellData.cellVoltages.isNotEmpty()
    val totalCells = if (hasCells) cellData.cellVoltages.size else 192

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "192-CELL VOLTAGE MATRIX",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = if (hasCells) "32 Modules • 6 Cells per Module" else "Standby (Awaiting OBD Telemetry)",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                val deltaColor = when {
                    !hasCells -> TextMuted
                    cellData.deltaVoltageMv < 20f -> EcoGreen
                    cellData.deltaVoltageMv < 40f -> CellWarm
                    else -> CellHot
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(SurfaceCardElevated)
                        .border(1.dp, deltaColor.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (hasCells) "Δ ${cellData.deltaVoltageMv.toInt()} mV" else "Δ -- mV",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = deltaColor,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val avgVoltage = if (hasCells) cellData.cellVoltages.average().toFloat() else 0f

            // Diagnostic Summary Strip (Min, Max, Avg, Delta)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(2.dp))
                    .background(SurfaceCardElevated)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min: #${cellData.minCellIndex + 1} (${String.format("%.3f", cellData.minCellVoltage)}V)",
                    fontSize = 10.sp,
                    color = HyundaiBlue,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Avg: ${String.format("%.3f", avgVoltage)}V",
                    fontSize = 10.sp,
                    color = TextPrimary,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Max: #${cellData.maxCellIndex + 1} (${String.format("%.3f", cellData.maxCellVoltage)}V)",
                    fontSize = 10.sp,
                    color = CellWarm,
                    fontFamily = FontFamily.SansSerif
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // High-Density 16-Column Cell Matrix Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(16),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(totalCells) { index ->
                    val voltage = if (hasCells && index < cellData.cellVoltages.size) cellData.cellVoltages[index] else 0f
                    val isSelected = selectedCellIndex == index
                    val isMin = hasCells && index == cellData.minCellIndex
                    val isMax = hasCells && index == cellData.maxCellIndex

                    val cellColor = when {
                        !hasCells -> SurfaceCardElevated
                        isMin -> HyundaiBlue
                        isMax -> CellWarm
                        voltage >= avgVoltage -> CellNominal
                        else -> CellNominal.copy(alpha = 0.8f)
                    }

                    Box(
                        modifier = Modifier
                            .size(17.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .background(cellColor)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) TextPrimary else CardBorderSubtle,
                                shape = RoundedCornerShape(1.dp)
                            )
                            .clickable { selectedCellIndex = if (isSelected) null else index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 6.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!hasCells) TextMuted else Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            // Cell Detail Inspector Banner (if clicked)
            if (selectedCellIndex != null) {
                val idx = selectedCellIndex!!
                val voltage = if (hasCells && idx < cellData.cellVoltages.size) cellData.cellVoltages[idx] else 0f
                val moduleNum = (idx / 6) + 1
                val cellInModule = (idx % 6) + 1

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(2.dp))
                        .background(SurfaceCardSecondary)
                        .border(1.dp, CardBorder, RoundedCornerShape(2.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CELL #${idx + 1} (MODULE $moduleNum • PIN $cellInModule)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${String.format("%.3f", voltage)} V",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoGreen,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            }
        }
    }
}
