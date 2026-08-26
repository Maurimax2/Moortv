package com.maurimax.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.maurimax.core.data.Download
import com.maurimax.core.data.DownloadState
import com.maurimax.core.data.ThemeMode
import com.maurimax.core.designsystem.Artwork
import com.maurimax.core.designsystem.ArtworkKind
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.MediaItem

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
    daysRemaining: Int?,
    status: String,
    downloads: List<Download>,
    onDownloadClick: (MediaItem) -> Unit,
    onRemoveDownload: (MediaItem) -> Unit,
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
                // The one number a customer on a prepaid line actually wants,
                // and the reason they open this page at all.
                Text(
                    text = when {
                        daysRemaining == null -> status
                        daysRemaining <= 0 -> stringResource(R.string.profile_expired)
                        else -> stringResource(R.string.profile_days_left, daysRemaining)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (daysRemaining != null && daysRemaining <= 7) {
                        colors.accentText
                    } else {
                        colors.textSecondary
                    },
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

        Text(
            text = stringResource(R.string.row_downloads),
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = Spacing.xl, bottom = Spacing.sm),
        )

        if (downloads.isEmpty()) {
            Text(
                text = stringResource(R.string.profile_no_downloads),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textTertiary,
            )
        } else {
            downloads.forEach { download ->
                DownloadRow(
                    download = download,
                    onClick = { onDownloadClick(download.item) },
                    onRemove = { onRemoveDownload(download.item) },
                )
            }
        }
    }
}

/**
 * One kept title.
 *
 * Here rather than only on each film's own page, because a customer looking
 * for what they downloaded is looking for a list of it — hunting for the films
 * one at a time to find out which ones finished is not a list.
 */
@Composable
private fun DownloadRow(download: Download, onClick: () -> Unit, onRemove: () -> Unit) {
    val colors = MaurimaxTheme.colors
    val done = download.state == DownloadState.DONE

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = done, onClick = onClick)
            .padding(vertical = Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .size(width = 64.dp, height = 40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.surface),
        ) {
            Artwork(
                url = download.item.artworkUrl,
                title = download.item.title,
                kind = ArtworkKind.POSTER,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = download.item.title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when (download.state) {
                    DownloadState.DONE -> stringResource(R.string.action_downloaded)
                    DownloadState.FAILED -> stringResource(R.string.action_download_failed)
                    else -> stringResource(
                        R.string.action_downloading,
                        (download.progress * 100).toInt(),
                    )
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (download.state == DownloadState.FAILED) {
                    colors.accentText
                } else {
                    colors.textSecondary
                },
            )
        }

        Text(
            text = stringResource(R.string.accounts_remove_short),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textTertiary,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onRemove)
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        )
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
