package com.maurimax.core.designsystem

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val TvColors = darkColorScheme(
    primary = Brand.Orange,
    onPrimary = Color_White,
    primaryContainer = Brand.Violet,
    onPrimaryContainer = Brand.TextPrimary,
    secondary = Brand.VioletLit,
    onSecondary = Brand.TextPrimary,
    background = Brand.Ink,
    onBackground = Brand.TextPrimary,
    surface = Brand.Surface,
    onSurface = Brand.TextPrimary,
    surfaceVariant = Brand.SurfaceRaised,
    onSurfaceVariant = Brand.TextSecondary,
    border = Brand.Outline,
)

@Composable
fun MaurimaxTvTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TvColors, content = content)
}
