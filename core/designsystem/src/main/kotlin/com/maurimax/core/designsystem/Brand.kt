package com.maurimax.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * MAURIMAX brand tokens. Form-factor agnostic on purpose: the mobile theme and
 * the TV theme both build on exactly these values, so the two apps can never
 * drift apart visually.
 */
object Brand {
    val Ink = Color(0xFF07070B)
    val Surface = Color(0xFF121218)
    val SurfaceRaised = Color(0xFF1C1C25)
    val Outline = Color(0xFF2E2E3A)

    val Accent = Color(0xFFE23B2E)
    val AccentBright = Color(0xFFFF5F4E)

    val TextPrimary = Color(0xFFF5F5F7)
    val TextSecondary = Color(0xFFA0A0AE)
}

/** Spacing scale. TV needs more breathing room than a phone, hence two values. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 40.dp

    /** Safe-area inset for TV: broadcast overscan eats the outer 5% of the panel. */
    val tvOverscan = 48.dp
}
