package com.maurimax.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Washes over artwork.
 *
 * Two rules hold in both themes. A hero fades to *this theme's* ground, so the
 * artwork and the page meet invisibly — fading to black on a light screen would
 * leave a bar across the middle. And copy laid over artwork always gets a dark
 * scrim, in light mode too: the artwork is whatever the panel sent, so light
 * text on a light poster is only legible if something darkens the poster first.
 */
object Scrims {

    /** Hero art fading into the page, ending exactly where the art ends. */
    @Composable
    @ReadOnlyComposable
    fun heroFade(): Brush {
        val fade = MaurimaxTheme.colors.heroFade
        return Brush.verticalGradient(
            0f to Color.Transparent,
            0.34f to fade.copy(alpha = 0.4f),
            Brand.HERO_ART_FRACTION to fade,
            1f to fade,
        )
    }

    /** TV side wash, so hero copy holds contrast beside the artwork. */
    @Composable
    @ReadOnlyComposable
    fun heroSide(): Brush {
        val fade = MaurimaxTheme.colors.heroFade
        return Brush.horizontalGradient(
            0f to fade,
            0.5f to fade.copy(alpha = 0.85f),
            1f to Color.Transparent,
        )
    }

    /**
     * Under text sitting directly on artwork. Dark in both themes, because the
     * artwork's brightness is not ours to predict.
     */
    val onArtwork: Brush = Brush.verticalGradient(
        0f to Color.Transparent,
        0.45f to Color(0x40000000),
        1f to Color(0xB3000000),
    )

    /** Violet bloom behind the sign-in lockup. */
    @Composable
    @ReadOnlyComposable
    fun signInGlow(): Brush = Brush.radialGradient(
        0f to Brand.Violet.copy(alpha = if (MaurimaxTheme.colors.isLight) 0.14f else 0.22f),
        1f to Color.Transparent,
    )
}
