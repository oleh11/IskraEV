package com.iskra.companion.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Iskra "Nordic" palette: Polestar-style calm near-black surfaces, hairline borders,
// a single warm gold accent, with Diia-style soft squircle cards and pill buttons.
val CockpitBg = Color(0xFF0B0B0C)             // Near-black canvas
val SurfaceCard = Color(0xFF141415)           // Flat card surface
val SurfaceCardSecondary = Color(0xFF1B1B1D)  // Raised surface
val SurfaceCardElevated = Color(0xFF242426)   // Track / chip surface
val CardBorder = Color(0xFF2A2A2D)            // Hairline border
val CardBorderSubtle = Color(0xFF1F1F22)

// Accents (names kept for compatibility, values re-tuned)
val HyundaiBlue = Color(0xFFD9A441)           // Swedish gold (primary accent)
val IoniqCyan = Color(0xFFA7C4BF)             // Pale mineral teal (secondary)
val IoniqPixel = Color(0xFFB8B8BD)            // Neutral light grey
val EcoGreen = Color(0xFF86D6A0)              // Regen / OK (soft mint)
val EcoGreenSubtle = Color(0xFF1E3A2A)
val PowerAmber = Color(0xFFF08C4A)            // Warm orange for load / heating
val PowerRed = Color(0xFFEF6B6B)              // Warning / critical

// Typography
val TextPrimary = Color(0xFFF5F5F6)
val TextSecondary = Color(0xFFA0A0A6)
val TextMuted = Color(0xFF6E6E74)
val TextInverse = Color(0xFF0B0B0C)

// Battery cell thermal spectrum
val CellCool = Color(0xFF5B9BD5)
val CellNominal = Color(0xFF3F8F5B)
val CellWarm = Color(0xFFC9772A)
val CellHot = Color(0xFFC94A4A)

/** Flat gauge backdrop (Polestar keeps surfaces calm: no glow). */
val GaugeGlowBrush: Brush = Brush.verticalGradient(
    colors = listOf(Color(0xFF151516), Color(0xFF111112))
)

/** Flat hero-card surface. */
val CardSheenBrush: Brush = Brush.verticalGradient(
    colors = listOf(Color(0xFF161617), Color(0xFF131314))
)
