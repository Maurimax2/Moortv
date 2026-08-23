package com.maurimax.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MobileColors = darkColorScheme(
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
