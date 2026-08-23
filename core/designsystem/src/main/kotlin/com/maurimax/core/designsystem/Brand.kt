package com.maurimax.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * MAURIMAX brand tokens. Form-factor agnostic on purpose: the mobile theme and
 * the TV theme both build on exactly these values, so the two apps can never
 * drift apart visually.
 *
 * The palette is near-black rather than grey, with a single warm accent. On an
 * OLED TV a true black ground makes artwork the only lit thing on screen, which
 * is what makes a catalogue feel expensive rather than busy.
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
    val TextTertiary = Color(0xFF6C6C7A)

    /** Sits under hero artwork so overlaid text keeps contrast at any brightness. */
    val HeroScrim = Brush.verticalGradient(
        0f to Color.Transparent,
        0.45f to Color(0x66000000),
        1f to Ink,
    )

    /** Left-edge scrim for TV, where hero text sits beside the art, not under it. */
    val HeroScrimHorizontal = Brush.horizontalGradient(
        0f to Ink,
        0.55f to Color(0xCC07070B),
        1f to Color.Transparent,
    )

    /** Applied to every tile so bright artwork never fights the ground. */
    val TileScrim = Brush.verticalGradient(
        0f to Color.Transparent,
        0.6f to Color(0x00000000),
        1f to Color(0xCC000000),
    )
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

/** Corner radii. Tight rather than pill-shaped — key art should look like a poster. */
object Corners {
    val tile = 6.dp
    val card = 10.dp
}
