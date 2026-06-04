package com.alarm.meluko.ui.theme

import androidx.compose.ui.graphics.Color

// ── Meluko · Seed: #FF8C00 (Sunrise Amber) ───────────────────────────────────
//
// All values derived from the M3 HCT tonal palette algorithm.
// Seed hue ≈ 35° (amber-orange), high chroma.
// Primary tones follow the M3 spec: T40 for light primary, T80 for dark primary.
// Secondary uses same hue, reduced chroma (muted caramel).
// Tertiary uses hue + 60° shift (→ golden yellow-green).

// ── Light scheme ─────────────────────────────────────────────────────────────
val md_light_primary              = Color(0xFF8C4300)  // T40 — dark amber, strong on cream
val md_light_onPrimary            = Color(0xFFFFFFFF)
val md_light_primaryContainer     = Color(0xFFFFDBCA)  // T90 — soft peach
val md_light_onPrimaryContainer   = Color(0xFF2E1400)  // T10

val md_light_secondary            = Color(0xFF745A4F)  // same hue, low chroma — caramel
val md_light_onSecondary          = Color(0xFFFFFFFF)
val md_light_secondaryContainer   = Color(0xFFFFDBD1)  // warm blush
val md_light_onSecondaryContainer = Color(0xFF2A1610)

val md_light_tertiary             = Color(0xFF5C6023)  // hue + 60° → golden olive
val md_light_onTertiary           = Color(0xFFFFFFFF)
val md_light_tertiaryContainer    = Color(0xFFE2E59C)  // warm yellow-green tint
val md_light_onTertiaryContainer  = Color(0xFF1A1D00)

val md_light_error                = Color(0xFFBA1A1A)
val md_light_onError              = Color(0xFFFFFFFF)
val md_light_errorContainer       = Color(0xFFFFDAD6)
val md_light_onErrorContainer     = Color(0xFF410002)

val md_light_background           = Color(0xFFFFF8F5)  // warm cream — not cold white
val md_light_onBackground         = Color(0xFF201B18)
val md_light_surface              = Color(0xFFFFF8F5)
val md_light_onSurface            = Color(0xFF201B18)
val md_light_surfaceVariant       = Color(0xFFF5DED6)  // warm blush surface
val md_light_onSurfaceVariant     = Color(0xFF53433D)
val md_light_surfaceContainer     = Color(0xFFFAEDE6)
val md_light_surfaceContainerHigh = Color(0xFFF4E7DF)
val md_light_outline              = Color(0xFF85736D)
val md_light_outlineVariant       = Color(0xFFD8C2BC)
val md_light_inverseSurface       = Color(0xFF362F2C)
val md_light_inverseOnSurface     = Color(0xFFFBEEE8)
val md_light_inversePrimary       = Color(0xFFFFB68C)  // T80

// ── Dark scheme ───────────────────────────────────────────────────────────────
val md_dark_primary               = Color(0xFFFFB68C)  // T80 — warm amber glow
val md_dark_onPrimary             = Color(0xFF4E2000)
val md_dark_primaryContainer      = Color(0xFF6F3000)  // T30 — rich burnt amber
val md_dark_onPrimaryContainer    = Color(0xFFFFDBCA)

val md_dark_secondary             = Color(0xFFE5BDB2)
val md_dark_onSecondary           = Color(0xFF432B22)
val md_dark_secondaryContainer    = Color(0xFF5B4238)
val md_dark_onSecondaryContainer  = Color(0xFFFFDBD1)

val md_dark_tertiary              = Color(0xFFC6C983)  // warm olive glow
val md_dark_onTertiary            = Color(0xFF2E3100)
val md_dark_tertiaryContainer     = Color(0xFF454800)
val md_dark_onTertiaryContainer   = Color(0xFFE2E59C)

val md_dark_error                 = Color(0xFFFFB4AB)
val md_dark_onError               = Color(0xFF690005)
val md_dark_errorContainer        = Color(0xFF93000A)
val md_dark_onErrorContainer      = Color(0xFFFFDAD6)

val md_dark_background            = Color(0xFF201B18)  // warm dark — not cold black
val md_dark_onBackground          = Color(0xFFEDE0DA)
val md_dark_surface               = Color(0xFF201B18)
val md_dark_onSurface             = Color(0xFFEDE0DA)
val md_dark_surfaceVariant        = Color(0xFF53433D)
val md_dark_onSurfaceVariant      = Color(0xFFD8C2BC)
val md_dark_surfaceContainer      = Color(0xFF2A2320)
val md_dark_surfaceContainerHigh  = Color(0xFF352D2A)
val md_dark_outline               = Color(0xFFA08D87)
val md_dark_outlineVariant        = Color(0xFF53433D)
val md_dark_inverseSurface        = Color(0xFFEDE0DA)
val md_dark_inverseOnSurface      = Color(0xFF362F2C)
val md_dark_inversePrimary        = Color(0xFF8C4300)