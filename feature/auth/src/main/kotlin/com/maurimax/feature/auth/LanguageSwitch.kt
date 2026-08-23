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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maurimax.core.data.AppLocale
import com.maurimax.core.designsystem.Brand
import com.maurimax.core.designsystem.Spacing

/**
 * Arabic / French switch, shown on sign-in.
 *
 * The app defaults to Arabic regardless of device language, so a French
 * customer would otherwise have no way back. Each option is written in its own
 * language — a customer who cannot read the current one can still find theirs.
 */
@Composable
fun LanguageSwitch(
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier,
    ) {
        LanguageChip("العربية", AppLocale.ARABIC == current, fontSize) { onSelect(AppLocale.ARABIC) }
        LanguageChip("Français", AppLocale.FRENCH == current, fontSize) { onSelect(AppLocale.FRENCH) }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    selected: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    BasicText(
        text = label,
        style = TextStyle(
            color = if (selected) Brand.TextPrimary else Brand.TextTertiary,
            fontSize = fontSize,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        ),
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (selected) Brand.SurfaceRaised else Brand.Ink, shape)
            .border(1.dp, if (selected) Brand.Orange else Brand.Outline, shape)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    )
}
