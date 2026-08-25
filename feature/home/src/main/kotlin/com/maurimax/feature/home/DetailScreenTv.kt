@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.maurimax.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Text
import com.maurimax.core.data.Download
import com.maurimax.core.data.DownloadState
import com.maurimax.core.designsystem.Artwork
import com.maurimax.core.data.Download
import com.maurimax.core.data.DownloadState
import com.maurimax.core.designsystem.ArtworkKind
import com.maurimax.core.designsystem.Brand
import com.maurimax.core.designsystem.Corners
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Scrims
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.MediaItem
import kotlinx.coroutines.delay

/**
 * A title's own page on TV.
 *
 * Copy on the left, artwork carrying the right — the classic ten-foot split.
 * It exists because a remote has no pointer: the customer needs somewhere the
 * D-pad lands on Play immediately, rather than a page they have to hunt across.
 */
@Composable
fun DetailScreenTv(
    item: MediaItem,
    onPlay: (MediaItem) -> Unit,
    isFavourite: Boolean = false,
    onToggleFavourite: () -> Unit = {},
    download: Download? = null,
    onDownload: () -> Unit = {},
    onRemoveDownload: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    var favourite by remember(item.id) { mutableStateOf(isFavourite) }
    val playButton = remember(item.id) { FocusRequester() }
    val kind = stringResource(item.kind.labelRes)
    val score = item.rating.trim().toDoubleOrNull()

    // Play is where the remote should already be. Anything else makes the
    // customer travel across the page to do the only thing they came for.
    LaunchedEffect(item.id) { runCatching { playButton.requestFocus() } }

    // The system downloader reports progress only when asked, so while
    // something is arriving this page asks — and stops the moment it lands.
    val inFlight = download != null &&
        download.state != DownloadState.DONE &&
        download.state != DownloadState.FAILED
    LaunchedEffect(inFlight) {
        while (inFlight) {
            delay(1_500)
            onRefresh()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground),
    ) {
        Artwork(
            url = item.artworkUrl,
            title = "",
            kind = if (item.isLive) ArtworkKind.CHANNEL_LOGO else ArtworkKind.POSTER,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(Brand.HERO_ART_FRACTION)
                .align(Alignment.TopCenter),
        )
        Box(modifier = Modifier.fillMaxSize().background(Scrims.heroSide()))
        Box(modifier = Modifier.fillMaxSize().background(Scrims.heroFade()))

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(580.dp)
                .padding(horizontal = Spacing.tvOverscan),
        ) {
            Text(
                text = item.title,
                color = colors.textPrimary,
                fontSize = 40.sp,
                lineHeight = 46.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TvPill(text = kind)
                if (score != null && score > 0.0) TvPill(text = "★ ${item.rating.trim()}", accent = true)
                if (item.year > 0) TvPill(text = item.year.toString())
                if (item.durationMinutes > 0) TvPill(text = "${item.durationMinutes}′")
            }

            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    color = colors.textSecondary,
                    fontSize = 16.sp,
                    lineHeight = 25.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.isPlayable) {
                    TvAction(
                        label = stringResource(
                            // Half-watched titles say so, so the customer knows
                            // they are picking up rather than starting over.
                            if (item.progress > 0f) R.string.action_resume else R.string.action_play,
                        ),
                        accent = true,
                        onClick = { onPlay(item) },
                        modifier = Modifier.focusRequester(playButton),
                    )
                    TvAction(
                        label = if (favourite) "★" else "☆",
                        onClick = {
                            favourite = !favourite
                            onToggleFavourite()
                        },
                    )
                    // Live has no file to keep, only a stream that never ends.
                    if (!item.isLive) {
                        TvAction(
                            label = when {
                                download?.state == DownloadState.DONE ->
                                    "✓  " + stringResource(R.string.action_downloaded)
                                download?.state == DownloadState.FAILED ->
                                    stringResource(R.string.action_download_failed)
                                download != null -> stringResource(
                                    R.string.action_downloading,
                                    (download.progress * 100).toInt(),
                                )
                                else -> "↓  " + stringResource(R.string.action_download)
                            },
                            onClick = { if (download == null) onDownload() else onRemoveDownload() },
                        )
                    }
                } else {
                    // A series is a container, not a stream. Saying so beats a
                    // Play button that does nothing.
                    Text(
                        text = stringResource(R.string.detail_episodes_soon),
                        color = colors.textTertiary,
                        fontSize = 15.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Corners.control))
                            .background(colors.surfaceRaised)
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    )
                }
                TvAction(
                    label = stringResource(R.string.action_back),
                    onClick = onBack,
                    modifier = if (item.isPlayable) Modifier else Modifier.focusRequester(playButton),
                )
            }

            if (item.progress > 0f) {
                Box(
                    modifier = Modifier
                        .width(280.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.outline),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.progress)
                            .fillMaxHeight()
                            .background(colors.accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun TvAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val colors = MaurimaxTheme.colors
    val shape = RoundedCornerShape(Corners.control)

    Card(
        onClick = onClick,
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(
            containerColor = if (accent) colors.accent else colors.surfaceRaised,
            focusedContainerColor = if (accent) colors.accent else colors.surfaceRaised,
        ),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, colors.accent), shape = shape),
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(elevationColor = colors.focusGlow, elevation = 12.dp),
        ),
        scale = CardDefaults.scale(focusedScale = 1.06f),
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(48.dp)
                .padding(horizontal = Spacing.lg),
        ) {
            Text(
                text = label,
                color = if (accent) colors.onAccent else colors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun TvPill(text: String, accent: Boolean = false) {
    val colors = MaurimaxTheme.colors
    Text(
        text = text,
        color = if (accent) colors.accentText else colors.textSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceRaised)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
    )
}
