package com.maurimax.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maurimax.core.data.AppLocale
import com.maurimax.core.data.ThemeMode
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Spacing

/**
 * Arabic / French switch.
 *
 * The app defaults to Arabic whatever the device says, so a French customer
 * would otherwise have no way back. Each option is written in its own language:
 * someone who cannot read the current one can still recognise theirs.
 */
@Composable
fun LanguageSwitch(
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = modifier) {
        Chip("العربية", current == AppLocale.ARABIC, fontSize) { onSelect(AppLocale.ARABIC) }
        Chip("Français", current == AppLocale.FRENCH, fontSize) { onSelect(AppLocale.FRENCH) }
    }
}

/** Light / dark switch. Light is the default. */
@Composable
fun ThemeSwitch(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = modifier) {
        Chip(stringResource(R.string.theme_light), current == ThemeMode.LIGHT, fontSize) {
            onSelect(ThemeMode.LIGHT)
        }
        Chip(stringResource(R.string.theme_dark), current == ThemeMode.DARK, fontSize) {
            onSelect(ThemeMode.DARK)
        }
    }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    fontSize: TextUnit,
    onClick: () -> Unit,
) {
    val colors = MaurimaxTheme.colors
    val shape = RoundedCornerShape(50)
    BasicText(
        text = label,
        style = TextStyle(
            color = if (selected) colors.textPrimary else colors.textTertiary,
            fontSize = fontSize,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (selected) colors.surfaceRaised else colors.ground, shape)
            .border(1.dp, if (selected) colors.accent else colors.outline, shape)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    )
}
