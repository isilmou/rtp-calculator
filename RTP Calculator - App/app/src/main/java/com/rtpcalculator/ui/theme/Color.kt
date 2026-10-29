package com.rtpcalculator.ui.theme

import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────────────────────
//   Color tokens — sportive-moderne dark palette (validated charte).
//
//   Two background levels (screen + cards), yellow odds accent, neutral CTAs,
//   grayscale format pills, TRJ gradient (4 stops). Components are refactored
//   in the next step; this file is the canonical source for the new palette.
// ──────────────────────────────────────────────────────────────────────────────

// --- Backgrounds (two depth levels; fields merge with the screen background). ---
val AppBackground   = Color(0xFF0A0A0A)   // app-wide background, near-black
val CardBg          = Color(0xFF141414)   // section cards (event, issues, history)
val FieldBackground = Color(0xFF0A0A0A)   // input fields (= AppBackground)
// Slight lift above CardBg to delimit each issue's column as its own block — used so the 3
// odds blocks read as distinct islands, especially in the expanded ">3 outcomes" layout
// where ROW N's bookmaker would otherwise sit next to ROW N+1's issue label on the same bg.
val OutcomeBlockBg  = Color(0xFF1E1E1E)

// Material3 surface consumed by Compose defaults / dialogs. Aligned to the new
// palette so DatePickerDialog / TimePickerDialog etc. look right by default.
val DarkSurface = Color(0xFF141414)

// --- Yellow accent — odds value (yellow on dark cards). ---
val OddsYellow = Color(0xFFFFD600)
// Sky blue, two roles: placeholder of the EMPTY odds fields ("Cote"), so the user spots at once the
// fields to fill in to get an RTP, and background of the TRJ pill when the bet is a SUREBET
// (RTP > 100 %). Same value as --odds-ph in the website's site.css.
val OddsPlaceholder = Color(0xFF4FC3F7)

// --- TRJ gradient — feeds rtpGradientColor (mapping unchanged) + history side bar. ---
val RtpRed    = Color(0xFFE53935)
val RtpOrange = Color(0xFFFB8C00)
val RtpYellow = Color(0xFFFDD835)
val RtpGreen  = Color(0xFF43A047)

// --- Text — 3 levels of hierarchy. ---
val TextPrimary   = Color(0xFFFFFFFF)   // event name, dominant values
val TextSecondary = Color(0xFFAAAAAA)   // metadata, labels
val TextTertiary  = Color(0xFF666666)   // micro-info (bookmaker under odds, "no date")

// --- Buttons / actions. ---
val CtaSurface   = Color(0xFFFAFAFA)    // CREATE / SAVE button background (off-white)
val CtaOnSurface = Color(0xFF0A0A0A)    // CTA button text (near-black)
val ActionIcon   = Color(0xFFBBBBBB)    // Duplicate / Share icons (light gray)
val DangerRed    = Color(0xFFEF5350)    // trash icon, error tint

// --- Format pills EU/UK/US — 3 grayscale shades (no more blue/cyan/violet). ---
val FormatActiveSurface   = Color(0xFFE8E8E8)
val FormatActiveText      = Color(0xFF0A0A0A)
val FormatInactiveSurface = Color(0xFF1F1F1F)
val FormatInactiveText    = Color(0xFF888888)

// --- Subtle separators / dividers. ---
val DividerSubtle = Color(0xFF1F1F1F)

// --- Focus indication (text-field cursor + focused border). ---
val FieldFocus = Color(0xFFFFFFFF)

// --- Subtle white overlay (~30% alpha) for fine dividers / focus-style borders. ---
val OverlayWhite30 = Color(0x4DFFFFFF)
