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
 * The default. Paper rather than pure white, warmed toward the brand violet so
 * it reads as chosen instead of unpainted.
 */
val MaurimaxLightColors = MaurimaxColors(
    ground = Color(0xFFFAF8FC),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFF1EBF8),
    outline = Color(0xFFE0D7EC),

    textPrimary = Color(0xFF17101F),
    textSecondary = Color(0xFF5C5268),
    textTertiary = Color(0xFF8C8397),

    // Deeper than the logo orange: #E86D31 behind white text on paper is under
    // 3:1, which fails as a button.
    accent = Brand.OrangeDeep,
    onAccent = Color(0xFFFFFFFF),
    accentText = Color(0xFFA9450F),

    identity = Brand.Violet,

    heroFade = Color(0xFFFAF8FC),
    focusGlow = Color(0x33B84E1A),

    isLight = true,
)

/** Near-black carrying a violet undertone, so surfaces feel part of one object. */
val MaurimaxDarkColors = MaurimaxColors(
    ground = Color(0xFF0A0610),
    surface = Color(0xFF140C1F),
    surfaceRaised = Color(0xFF1E1330),
    outline = Color(0xFF332347),

    textPrimary = Color(0xFFF7F4FA),
    textSecondary = Color(0xFFA9A0B8),
    textTertiary = Color(0xFF6E6580),

    accent = Brand.Orange,
    onAccent = Color(0xFFFFFFFF),
    accentText = Brand.OrangeLit,

    identity = Brand.VioletLit,

    heroFade = Color(0xFF0A0610),
    focusGlow = Color(0x4DE86D31),

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
