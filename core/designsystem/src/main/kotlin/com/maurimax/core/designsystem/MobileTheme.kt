package com.maurimax.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun materialScheme(c: MaurimaxColors) = if (c.isLight) {
    lightColorScheme(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.identity,
        onPrimaryContainer = c.surface,
        secondary = c.identity,
        onSecondary = c.surface,
        background = c.ground,
        onBackground = c.textPrimary,
        surface = c.surface,
        onSurface = c.textPrimary,
        surfaceVariant = c.surfaceRaised,
        onSurfaceVariant = c.textSecondary,
        outline = c.outline,
        error = c.accentText,
        onError = c.onAccent,
    )
} else {
    darkColorScheme(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.identity,
        onPrimaryContainer = c.textPrimary,
        secondary = c.identity,
        onSecondary = c.textPrimary,
        background = c.ground,
        onBackground = c.textPrimary,
        surface = c.surface,
        onSurface = c.textPrimary,
        surfaceVariant = c.surfaceRaised,
        onSurfaceVariant = c.textSecondary,
        outline = c.outline,
        error = c.accentText,
        onError = c.onAccent,
    )
}

@Composable
fun MaurimaxMobileTheme(
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (dark) MaurimaxDarkColors else MaurimaxLightColors
    CompositionLocalProvider(LocalMaurimaxColors provides colors) {
        MaterialTheme(
            colorScheme = materialScheme(colors),
            typography = MaurimaxTypography,
            content = content,
        )
    }
}

/**
 * Material3 colours without the rest of the mobile theme.
 *
 * Compose for TV ships no text field, so TV forms must use Material3
 * components. Inside the TV theme those find no Material3 scheme and fall back
 * to the default light one — which is why the TV sign-in form needs this even
 * though the rest of the TV screen does not.
 */
@Composable
fun MaurimaxFormColors(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = materialScheme(MaurimaxTheme.colors),
        typography = MaurimaxTypography,
        content = content,
    )
}
