package com.maurimax.core.designsystem

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * MAURIMAX brand tokens, taken from the mark itself: the violet and the orange
 * are sampled straight out of the logo artwork.
 *
 * The two hues carry different jobs and never swap. Violet is identity — it
 * tints the ground, the surfaces and anything structural. Orange is intent —
 * focus, actions, live state, progress. Because only one of them ever means
 * "this is what you are about to do", the eye never has to choose between two
 * competing highlights, which is what separates a storefront from a dashboard.
 */
object Brand {

    // ---- sampled from the logo -------------------------------------------
    val Violet = Color(0xFF4E0D83)
    val Orange = Color(0xFFE86D31)

    /** Lifted variants: the source hues are too dark to read as text on ink. */
    val VioletLit = Color(0xFF8B45D6)
    val OrangeLit = Color(0xFFFF9152)

    // ---- ground ----------------------------------------------------------
    /**
     * Near-black carrying a violet undertone rather than neutral grey. On an
     * OLED panel it still reads as black, but every surface above it feels part
     * of the same object instead of artwork floating on a system background.
     */
    val Ink = Color(0xFF0A0610)
    val Surface = Color(0xFF140C1F)
    val SurfaceRaised = Color(0xFF1E1330)
    val Outline = Color(0xFF332347)

    // ---- type ------------------------------------------------------------
    val TextPrimary = Color(0xFFF7F4FA)
    val TextSecondary = Color(0xFFA9A0B8)
    val TextTertiary = Color(0xFF6E6580)

    // ---- washes ----------------------------------------------------------
    /** Under hero artwork, so overlaid copy holds contrast at any brightness. */
    val HeroScrim = Brush.verticalGradient(
        0f to Color.Transparent,
        0.42f to Color(0x730A0610),
        1f to Ink,
    )

    /**
     * Side scrim for TV, where hero copy sits beside the art rather than under
     * it. Declared start-to-end so it mirrors correctly in Arabic.
     */
    val HeroScrimSide = Brush.horizontalGradient(
        0f to Ink,
        0.5f to Color(0xD90A0610),
        1f to Color.Transparent,
    )

    /** Violet bloom behind the sign-in mark. */
    val SignInGlow = Brush.radialGradient(
        0f to Color(0x384E0D83),
        1f to Color.Transparent,
    )

    /** Focus ring bloom. Orange, so focus never competes with brand violet. */
    val FocusGlow = Color(0x4DE86D31)
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

/** Corner radii. Tight rather than pill-shaped — key art should read as a poster. */
object Corners {
    val tile = 8.dp
    val card = 12.dp
    val control = 12.dp
}
