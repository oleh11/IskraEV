package com.iskra.companion.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iskra.companion.data.model.TpmsData
import com.iskra.companion.ui.theme.CardBorder
import com.iskra.companion.ui.theme.CardBorderSubtle
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
 * Classic 2018 4-Wheel TPMS Visualizer with schematic layout.
 */
@Composable
fun TpmsCarVisualizer(
    tpms: TpmsData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TIRE PRESSURE & TEMPERATURE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Target Cold Pressure: ${tpms.recommendedPsi.toInt()} PSI",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                if (tpms.isWarningActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(2.dp))
                            .background(PowerRed.copy(alpha = 0.15f))
                            .border(1.dp, PowerRed.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LOW PRESSURE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = PowerRed,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-Wheel Grid schematic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Wheels Column (Front Left & Rear Left)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TireMetricBox(
                        position = "FRONT LEFT",
                        psi = tpms.frontLeftPsi,
                        tempC = tpms.frontLeftTempC,
                        isWarning = tpms.frontLeftPsi < 30f
                    )
                    TireMetricBox(
                        position = "REAR LEFT",
                        psi = tpms.rearLeftPsi,
                        tempC = tpms.rearLeftTempC,
                        isWarning = tpms.rearLeftPsi < 30f
                    )
                }

                // Center Chassis Graphic
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 130.dp)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val cx = w / 2f

                        // Chassis Outline
                        drawRoundRect(
                            color = CardBorder,
                            topLeft = Offset(cx - 22.dp.toPx(), 8.dp.toPx()),
                            size = Size(44.dp.toPx(), h - 16.dp.toPx()),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx())
                        )

                        // Axles
                        drawLine(
                            color = CardBorder,
                            start = Offset(6.dp.toPx(), 28.dp.toPx()),
                            end = Offset(w - 6.dp.toPx(), 28.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawLine(
                            color = CardBorder,
                            start = Offset(6.dp.toPx(), h - 28.dp.toPx()),
                            end = Offset(w - 6.dp.toPx(), h - 28.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx()
                        )

                        // 4 Wheels
                        val wheelW = 8.dp.toPx()
                        val wheelH = 18.dp.toPx()

                        // FL & FR
                        drawRoundRect(
                            color = if (tpms.frontLeftPsi < 30f) PowerRed else EcoGreen,
                            topLeft = Offset(2.dp.toPx(), 19.dp.toPx()),
                            size = Size(wheelW, wheelH),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                        drawRoundRect(
                            color = if (tpms.frontRightPsi < 30f) PowerRed else EcoGreen,
                            topLeft = Offset(w - 2.dp.toPx() - wheelW, 19.dp.toPx()),
                            size = Size(wheelW, wheelH),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )

                        // RL & RR
                        drawRoundRect(
                            color = if (tpms.rearLeftPsi < 30f) PowerRed else EcoGreen,
                            topLeft = Offset(2.dp.toPx(), h - 19.dp.toPx() - wheelH),
                            size = Size(wheelW, wheelH),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                        drawRoundRect(
                            color = if (tpms.rearRightPsi < 30f) PowerRed else EcoGreen,
                            topLeft = Offset(w - 2.dp.toPx() - wheelW, h - 19.dp.toPx() - wheelH),
                            size = Size(wheelW, wheelH),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }

                // Right Wheels Column (Front Right & Rear Right)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TireMetricBox(
                        position = "FRONT RIGHT",
                        psi = tpms.frontRightPsi,
                        tempC = tpms.frontRightTempC,
                        isWarning = tpms.frontRightPsi < 30f
                    )
                    TireMetricBox(
                        position = "REAR RIGHT",
                        psi = tpms.rearRightPsi,
                        tempC = tpms.rearRightTempC,
                        isWarning = tpms.rearRightPsi < 30f
                    )
                }
            }
        }
    }
}

@Composable
private fun TireMetricBox(
    position: String,
    psi: Float,
    tempC: Float,
    isWarning: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(2.dp))
            .background(SurfaceCardSecondary)
            .border(
                1.dp,
                if (isWarning) PowerRed else CardBorderSubtle,
                RoundedCornerShape(2.dp)
            )
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = position,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
            Text(
                text = if (psi > 0f) "${String.format("%.1f", psi)} PSI" else "-- PSI",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isWarning) PowerRed else TextPrimary,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = if (tempC > 0f) "${tempC.toInt()}°C" else "--°C",
                fontSize = 10.sp,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
