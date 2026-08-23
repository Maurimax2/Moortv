package com.maurimax.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

internal val MobileColors = darkColorScheme(
    primary = Brand.Accent,
    onPrimary = Brand.TextPrimary,
    background = Brand.Ink,
    onBackground = Brand.TextPrimary,
    surface = Brand.Surface,
    onSurface = Brand.TextPrimary,
    surfaceVariant = Brand.SurfaceRaised,
    onSurfaceVariant = Brand.TextSecondary,
    outline = Brand.Outline,
)

@Composable
fun MaurimaxMobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MobileColors,
        content = content,
    )
}

/**
 * Applies the Material3 dark scheme without the rest of the mobile theme.
 *
 * Compose for TV has no text field — there is no `androidx.tv.material3.TextField`
 * — so TV forms must use Material3 components. Inside the TV theme those
 * components find no Material3 colour scheme and fall back to the default light
 * one, which renders a white form on a black screen. Wrapping them in this
 * fixes that.
 */
@Composable
fun MaurimaxFormColors(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MobileColors, content = content)
}
