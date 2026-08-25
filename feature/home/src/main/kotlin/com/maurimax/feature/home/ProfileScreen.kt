package com.maurimax.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maurimax.core.data.AppLocale
import com.maurimax.core.data.ThemeMode
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Spacing

/**
 * The account, and the handful of things that belong to it rather than to the
 * catalogue.
 *
 * Rows of plain text on the page, not a settings list in cards. There are six
 * things here; giving each one a container would make the screen look busier
 * than the catalogue it sits beside.
 */
@Composable
fun ProfileScreen(
    username: String,
    downloads: Int,
    language: String,
    onLanguageChange: (String) -> Unit,
    theme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    onSwitchAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .verticalScroll(rememberScrollState())
            .padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Spacing.xl,
                bottom = Spacing.xl,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.identity),
            ) {
                Text(
                    text = username.take(1).uppercase(),
                    color = colors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.profile_downloads, downloads),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                )
            }
        }

        Divider()
        SettingRow(
            label = stringResource(R.string.profile_switch_account),
            value = "",
            onClick = onSwitchAccount,
        )

        Divider()
        SettingRow(
            label = stringResource(R.string.profile_language),
            value = if (language == AppLocale.FRENCH) "Français" else "العربية",
            onClick = {
                onLanguageChange(
                    if (language == AppLocale.FRENCH) AppLocale.ARABIC else AppLocale.FRENCH,
                )
            },
        )

        Divider()
        SettingRow(
            label = stringResource(R.string.profile_appearance),
            value = stringResource(
                if (theme == ThemeMode.DARK) R.string.profile_theme_dark else R.string.profile_theme_light,
            ),
            onClick = {
                onThemeChange(if (theme == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK)
            },
        )

        Divider()
    }
}

@Composable
private fun SettingRow(label: String, value: String, onClick: () -> Unit) {
    val colors = MaurimaxTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (value.isNotBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .size(height = 1.dp, width = 1.dp)
            .background(MaurimaxTheme.colors.outline),
    )
}
