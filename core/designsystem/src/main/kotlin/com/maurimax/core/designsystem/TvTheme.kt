package com.maurimax.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import androidx.tv.material3.lightColorScheme

@Composable
fun MaurimaxTvTheme(
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val c = if (dark) MaurimaxDarkColors else MaurimaxLightColors
    val scheme = if (c.isLight) {
        lightColorScheme(
            primary = c.accent,
            onPrimary = c.onAccent,
            secondary = c.identity,
            onSecondary = c.surface,
            background = c.ground,
            onBackground = c.textPrimary,
            surface = c.surface,
            onSurface = c.textPrimary,
            surfaceVariant = c.surfaceRaised,
            onSurfaceVariant = c.textSecondary,
            border = c.outline,
        )
    } else {
        darkColorScheme(
            primary = c.accent,
            onPrimary = c.onAccent,
            secondary = c.identity,
            onSecondary = c.textPrimary,
            background = c.ground,
            onBackground = c.textPrimary,
            surface = c.surface,
            onSurface = c.textPrimary,
            surfaceVariant = c.surfaceRaised,
            onSurfaceVariant = c.textSecondary,
            border = c.outline,
        )
    }

    CompositionLocalProvider(LocalMaurimaxColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
