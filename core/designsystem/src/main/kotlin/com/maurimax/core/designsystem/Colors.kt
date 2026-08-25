package com.maurimax.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Fixed identity. These two hues are the logo and never change between themes —
 * only the roles built on top of them do.
 */
object Brand {
    val Violet = Color(0xFF4E0D83)
    val VioletDeep = Color(0xFF33075A)
    val VioletLit = Color(0xFF8B45D6)

    val Orange = Color(0xFFE86D31)
    val OrangeLit = Color(0xFFFF9152)
    val OrangeDeep = Color(0xFFB84E1A)

    /**
     * How much of a TV screen the hero artwork covers. Shared so the scrim's
     * final stop lands exactly where the artwork ends.
     */
    const val HERO_ART_FRACTION = 0.68f
}

/**
 * Every colour the product uses, resolved for one theme.
 *
 * Roles, not hues: a screen asks for `accent` rather than "orange", so the two
 * themes can use different orange values where contrast demands it. The same
 * #E86D31 that sits comfortably on near-black fails contrast as a fill behind
 * white text on paper, which is why light uses a deeper one.
 */
@Immutable
data class MaurimaxColors(
    val ground: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val outline: Color,

    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,

    /** Fills that mean "act here". Always paired with [onAccent]. */
    /**
     * The one thing on screen you are meant to press.
     *
     * White on near-black, black on paper — deliberately not the brand colour.
     * A play button has to be the most legible object on a page made of
     * artwork, and orange over a poster is neither as readable nor as calm.
     */
    val primaryFill: Color,
    val onPrimaryFill: Color,

    /** Beside it: readable, clearly secondary, never competing. */
    val secondaryFill: Color,

    val accent: Color,
    val onAccent: Color,
    /** The same intent as text or a hairline, where a fill would shout. */
    val accentText: Color,

    /** Identity violet, tuned so it reads on this theme's ground. */
    val identity: Color,

    /** The colour a hero gradient resolves to — always this theme's ground. */
    val heroFade: Color,
    val focusGlow: Color,

    val isLight: Boolean,
)

/**
 * Paper. Neutral rather than tinted: a catalogue screen is mostly artwork, and
 * a coloured ground fights every poster on it.
 */
val MaurimaxLightColors = MaurimaxColors(
    ground = Color(0xFFFAFAFB),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF0F0F2),
    outline = Color(0xFFE4E4E8),

    textPrimary = Color(0xFF0C0C0E),
    textSecondary = Color(0xFF57575F),
    textTertiary = Color(0xFF8A8A93),

    accent = Brand.OrangeDeep,
    onAccent = Color(0xFFFFFFFF),
    accentText = Color(0xFFA8420F),

    primaryFill = Color(0xFF101014),
    onPrimaryFill = Color(0xFFFFFFFF),
    secondaryFill = Color(0x140C0C0E),

    identity = Brand.Violet,

    heroFade = Color(0xFFFAFAFB),
    focusGlow = Color(0x33B84E1A),

    isLight = true,
)

/**
 * The product's real skin: near-black, and neutral.
 *
 * An earlier build tinted every surface toward the brand violet, which turned
 * the whole interface purple and made it read as a template rather than as a
 * place to watch something. Colour now belongs to the artwork. Violet appears
 * in the mark, orange marks the one thing you can act on, and everything else
 * is charcoal so the posters are the only lit thing on the screen.
 */
val MaurimaxDarkColors = MaurimaxColors(
    ground = Color(0xFF08080A),
    surface = Color(0xFF141417),
    surfaceRaised = Color(0xFF1E1E22),
    outline = Color(0xFF2A2A2F),

    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF9E9EA7),
    textTertiary = Color(0xFF66666E),

    accent = Brand.Orange,
    onAccent = Color(0xFF0B0B0D),
    accentText = Brand.OrangeLit,

    // The primary action is white on near-black, the way every premium
    // catalogue does it: nothing on a dark screen reads as "press this" faster,
    // and it leaves orange free to mean live, playing, or focused.
    primaryFill = Color(0xFFFFFFFF),
    onPrimaryFill = Color(0xFF0B0B0D),
    secondaryFill = Color(0x2EFFFFFF),

    identity = Brand.VioletLit,

    heroFade = Color(0xFF08080A),
    focusGlow = Color(0x4DF06A2A),

    isLight = false,
)

val LocalMaurimaxColors = staticCompositionLocalOf { MaurimaxLightColors }

/** Access point for themed colour, on phone and TV alike. */
object MaurimaxTheme {
    val colors: MaurimaxColors
        @androidx.compose.runtime.Composable
        @androidx.compose.runtime.ReadOnlyComposable
        get() = LocalMaurimaxColors.current
}
