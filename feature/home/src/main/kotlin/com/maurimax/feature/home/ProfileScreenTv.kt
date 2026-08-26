@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.maurimax.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.maurimax.core.data.AppLocale
import com.maurimax.core.data.Download
import com.maurimax.core.data.DownloadState
import com.maurimax.core.data.ThemeMode
import com.maurimax.core.designsystem.Artwork
import com.maurimax.core.designsystem.ArtworkKind
import com.maurimax.core.designsystem.Corners
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.MediaItem

/**
 * The account, on a television.
 *
 * The same three things the phone's account page carries — how long the
 * subscription has left, what is saved on the box, and the two settings worth
 * changing — laid out for a remote instead of a thumb: one column, everything
 * focusable, nothing behind a menu.
 */
@Composable
fun ProfileScreenTv(
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
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(
            start = Spacing.tvOverscan,
            end = Spacing.tvOverscan,
            top = Spacing.xl,
            bottom = Spacing.xl,
        ),
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground),
    ) {
        item(key = "identity") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.padding(bottom = Spacing.lg),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.identity),
                ) {
                    Text(
                        text = username.take(1).uppercase(),
                        color = colors.textPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Column {
                    Text(
                        text = username,
                        color = colors.textPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // The number the customer came here for. Anything under a
                    // week is said in the warning colour, because on a prepaid
                    // line that is the difference between renewing and not.
                    Text(
                        text = when {
                            daysRemaining == null -> status
                            daysRemaining <= 0 -> stringResource(R.string.profile_expired)
                            else -> stringResource(R.string.profile_days_left, daysRemaining)
                        },
                        color = if (daysRemaining != null && daysRemaining <= 7) {
                            colors.accentText
                        } else {
                            colors.textSecondary
                        },
                        fontSize = 17.sp,
                    )
                }
            }
        }

        item(key = "language") {
            TvSettingRow(
                label = stringResource(R.string.profile_language),
                value = if (language == AppLocale.FRENCH) "Français" else "العربية",
                focusRequester = first,
                onClick = {
                    onLanguageChange(
                        if (language == AppLocale.FRENCH) AppLocale.ARABIC else AppLocale.FRENCH,
                    )
                },
            )
        }

        item(key = "appearance") {
            TvSettingRow(
                label = stringResource(R.string.profile_appearance),
                value = stringResource(
                    if (theme == ThemeMode.DARK) {
                        R.string.profile_theme_dark
                    } else {
                        R.string.profile_theme_light
                    },
                ),
                onClick = {
                    onThemeChange(if (theme == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK)
                },
            )
        }

        item(key = "switch") {
            TvSettingRow(
                label = stringResource(R.string.profile_switch_account),
                value = "",
                onClick = onSwitchAccount,
            )
        }

        item(key = "downloads-title") {
            Text(
                text = stringResource(R.string.row_downloads),
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = Spacing.xl, bottom = Spacing.sm),
            )
        }

        if (downloads.isEmpty()) {
            item(key = "downloads-empty") {
                Text(
                    text = stringResource(R.string.profile_no_downloads),
                    color = colors.textTertiary,
                    fontSize = 16.sp,
                )
            }
        } else {
            items(downloads, key = { it.item.id }) { download ->
                TvDownloadRow(
                    download = download,
                    onClick = { onDownloadClick(download.item) },
                    onRemove = { onRemoveDownload(download.item) },
                )
            }
        }
    }
}

@Composable
private fun TvSettingRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
) {
    val colors = MaurimaxTheme.colors
    Card(
        onClick = onClick,
        shape = CardDefaults.shape(RoundedCornerShape(Corners.control)),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = colors.surfaceRaised,
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                BorderStroke(2.dp, colors.accent),
                shape = RoundedCornerShape(Corners.control),
            ),
        ),
        scale = CardDefaults.scale(focusedScale = 1.02f),
        modifier = Modifier
            .fillMaxWidth()
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            Text(text = label, color = colors.textPrimary, fontSize = 18.sp)
            Box(modifier = Modifier.weight(1f))
            Text(text = value, color = colors.textSecondary, fontSize = 17.sp)
        }
    }
}

/**
 * One kept title.
 *
 * Pressing it plays; the remove control is a second focus stop beside it, so
 * neither action can be taken by accident on a remote where everything is one
 * OK away.
 */
@Composable
private fun TvDownloadRow(download: Download, onClick: () -> Unit, onRemove: () -> Unit) {
    val colors = MaurimaxTheme.colors
    val done = download.state == DownloadState.DONE

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Card(
            onClick = { if (done) onClick() },
            shape = CardDefaults.shape(RoundedCornerShape(Corners.control)),
            colors = CardDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = colors.surfaceRaised,
            ),
            border = CardDefaults.border(
                focusedBorder = Border(
                    BorderStroke(2.dp, colors.accent),
                    shape = RoundedCornerShape(Corners.control),
                ),
            ),
            scale = CardDefaults.scale(focusedScale = 1.02f),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.padding(Spacing.sm),
            ) {
                Artwork(
                    url = download.item.artworkUrl,
                    title = download.item.title,
                    kind = ArtworkKind.POSTER,
                    modifier = Modifier
                        .width(96.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(6.dp)),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.item.title,
                        color = colors.textPrimary,
                        fontSize = 18.sp,
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
                        color = if (download.state == DownloadState.FAILED) {
                            colors.accentText
                        } else {
                            colors.textSecondary
                        },
                        fontSize = 15.sp,
                    )
                }
            }
        }

        Card(
            onClick = onRemove,
            shape = CardDefaults.shape(RoundedCornerShape(Corners.control)),
            colors = CardDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = colors.surfaceRaised,
            ),
            border = CardDefaults.border(
                focusedBorder = Border(
                    BorderStroke(2.dp, colors.accent),
                    shape = RoundedCornerShape(Corners.control),
                ),
            ),
            scale = CardDefaults.scale(focusedScale = 1.04f),
        ) {
            Text(
                text = stringResource(R.string.accounts_remove_short),
                color = colors.textSecondary,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            )
        }
    }
}
