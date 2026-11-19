package com.iskra.companion.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Iskra "Cyber Dashboard" palette: deep navy-black glass surfaces with neon cyan / teal sparks.
val CockpitBg = Color(0xFF05080F)             // Near-black navy canvas
val SurfaceCard = Color(0xFF0C1524)           // Glass card surface
val SurfaceCardSecondary = Color(0xFF111E33)  // Raised surface
val SurfaceCardElevated = Color(0xFF182843)   // Track / chip surface
val CardBorder = Color(0xFF1C3050)            // Cool hairline border
val CardBorderSubtle = Color(0xFF142238)

// Accents (names kept for compatibility, values re-tuned)
val HyundaiBlue = Color(0xFF22D3EE)           // Spark cyan (primary accent)
val IoniqCyan = Color(0xFF2DD4BF)             // Teal
val IoniqPixel = Color(0xFF60A5FA)            // Soft blue
val EcoGreen = Color(0xFF34D399)              // Regen / OK
val EcoGreenSubtle = Color(0xFF064E3B)
val PowerAmber = Color(0xFFFBBF24)            // Power draw / heating
val PowerRed = Color(0xFFFB7185)              // Warning / critical

// Typography
val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)
val TextInverse = Color(0xFF05080F)

// Battery cell thermal spectrum
val CellCool = Color(0xFF0EA5E9)
val CellNominal = Color(0xFF16A34A)
val CellWarm = Color(0xFFD97706)
val CellHot = Color(0xFFDC2626)

/** Subtle glow gradient used behind the round gauges. */
val GaugeGlowBrush: Brush = Brush.radialGradient(
    colors = listOf(Color(0xFF12233F), Color(0xFF0A1322))
)

/** Soft vertical sheen for hero cards. */
val CardSheenBrush: Brush = Brush.verticalGradient(
    colors = listOf(Color(0xFF0F1B30), Color(0xFF0A1220))
)
