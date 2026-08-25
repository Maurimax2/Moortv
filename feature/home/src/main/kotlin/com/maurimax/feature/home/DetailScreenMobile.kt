package com.maurimax.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maurimax.core.data.Download
import com.maurimax.core.data.DownloadState
import com.maurimax.core.designsystem.Artwork
import com.maurimax.core.designsystem.ArtworkKind
import com.maurimax.core.designsystem.Corners
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.data.Graph
import com.maurimax.core.model.Episode
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.MediaKind
import com.maurimax.core.model.Season
import kotlinx.coroutines.delay

/**
 * A title's own page.
 *
 * Key art at full width, the poster overlapping it, then the copy — the layout
 * every catalogue uses because it lets one image carry the mood while the
 * information stays on a readable surface below.
 */
@Composable
fun DetailScreenMobile(
    item: MediaItem,
    onPlay: (MediaItem) -> Unit,
    isFavourite: Boolean = false,
    onToggleFavourite: () -> Unit = {},
    download: Download? = null,
    onDownload: () -> Unit = {},
    onRemoveDownload: () -> Unit = {},
    onRetryDownload: () -> Unit = {},
    onRefresh: () -> Unit = {},
    seasons: List<Season> = emptyList(),
    episodesLoading: Boolean = false,
    onPlayEpisode: (Episode) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var favourite by remember(item.id) { mutableStateOf(isFavourite) }
    val colors = MaurimaxTheme.colors

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
    val kind = stringResource(item.kind.labelRes)
    val score = item.rating.trim().toDoubleOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f),
            ) {
                Artwork(
                    url = item.artworkUrl,
                    title = item.title,
                    kind = if (item.isLive) ArtworkKind.CHANNEL_LOGO else ArtworkKind.POSTER,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.5f to colors.ground.copy(alpha = 0.3f),
                                0.85f to colors.ground.copy(alpha = 0.95f),
                                1f to colors.ground,
                            ),
                        ),
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.padding(horizontal = Spacing.lg),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                )

                Text(
                    // Only what the panel actually sent. Most titles here carry
                    // a kind and nothing else, and a row of pills around two
                    // words is chrome standing in for information.
                    text = buildList {
                        add(kind)
                        if (score != null && score > 0.0) add("★ ${item.rating.trim()}")
                        if (item.year > 0) add(item.year.toString())
                        if (item.durationMinutes > 0) {
                            add(stringResource(R.string.detail_minutes, item.durationMinutes))
                        }
                    }.joinToString("  ·  "),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                )

                if (item.isPlayable) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DetailPlayButton(
                            resuming = item.progress > 0f,
                            onClick = { onPlay(item) },
                            modifier = Modifier.weight(1f),
                        )
                        FavouriteButton(
                            favourite = favourite,
                            onClick = {
                                favourite = !favourite
                                onToggleFavourite()
                            },
                        )
                        // Live has no file to keep, only a stream that never ends.
                        if (!item.isLive) {
                            DownloadButton(
                                download = download,
                                onDownload = onDownload,
                                onRemove = onRemoveDownload,
                                onRetry = onRetryDownload,
                            )
                        }
                    }

                    if (download != null) {
                        DownloadNote(download)
                    }
                } else {
                    // A series is a container: the star is still meaningful,
                    // but there is nothing here to press play on. The episodes
                    // below are what plays.
                    FavouriteButton(
                        favourite = favourite,
                        onClick = {
                            favourite = !favourite
                            onToggleFavourite()
                        },
                    )
                }

                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        color = colors.textSecondary,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                    )
                }

                if (item.kind == MediaKind.SERIES) {
                    EpisodeList(
                        seasons = seasons,
                        loading = episodesLoading,
                        onPlay = onPlayEpisode,
                    )
                }

                Box(Modifier.height(Spacing.xl))
            }
        }

        BackButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacing.md),
        )
    }
}

/** The one action on the page, in the same white the rest of the app uses. */
@Composable
private fun DetailPlayButton(resuming: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaurimaxTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.primaryFill)
            .clickable(onClick = onClick),
    ) {
        Spacer(Modifier.weight(1f))
        PlayGlyph(color = colors.onPrimaryFill, size = 12.dp)
        Text(
            text = stringResource(if (resuming) R.string.action_resume else R.string.action_play),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onPrimaryFill,
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
    }
}

/**
 * Star toggle.
 *
 * Its own square beside Play rather than an icon in the corner: favouriting is
 * a deliberate act, and burying it in chrome makes it feel accidental.
 */
@Composable
private fun FavouriteButton(favourite: Boolean, onClick: () -> Unit) {
    val colors = MaurimaxTheme.colors
    val label = stringResource(
        if (favourite) R.string.action_favourite_remove else R.string.action_favourite_add,
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (favourite) colors.primaryFill else colors.secondaryFill)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Text(
            text = if (favourite) "★" else "☆",
            color = if (favourite) colors.onPrimaryFill else colors.textPrimary,
            fontSize = 20.sp,
        )
    }
}

/** Sits over artwork, so it carries its own dark disc rather than relying on it. */
@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x8C000000))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = "‹",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Keep-on-device toggle.
 *
 * Three states in one square, because that is what the customer is actually
 * asking at any moment: not yet, on its way, or already here. The percentage
 * is the whole answer while it runs — a spinner would say nothing about
 * whether it is worth waiting for on this connection.
 */
@Composable
private fun DownloadButton(
    download: Download?,
    onDownload: () -> Unit,
    onRemove: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = MaurimaxTheme.colors
    val done = download?.state == DownloadState.DONE
    val running = download != null && !done && download.state != DownloadState.FAILED
    val failed = download?.state == DownloadState.FAILED
    val label = stringResource(
        if (download == null || failed) R.string.action_download else R.string.action_download_remove,
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (done) colors.primaryFill else colors.secondaryFill)
            .clickable {
                when {
                    download == null -> onDownload()
                    failed -> onRetry()
                    else -> onRemove()
                }
            }
            .semantics { contentDescription = label },
    ) {
        when {
            done -> Text("✓", color = colors.onPrimaryFill, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            failed -> Text("↻", color = colors.accentText, fontSize = 20.sp)
            running -> Text(
                text = "${(download.progress * 100).toInt()}%",
                color = colors.accentText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            else -> Text("↓", color = colors.textPrimary, fontSize = 20.sp)
        }
    }
}

/** Says in words what the square only hints at. */
@Composable
private fun DownloadNote(download: Download) {
    val colors = MaurimaxTheme.colors
    val text = when (download.state) {
        DownloadState.DONE -> stringResource(R.string.action_downloaded)
        DownloadState.FAILED -> stringResource(R.string.action_download_failed)
        else -> stringResource(R.string.action_downloading, (download.progress * 100).toInt())
    }

    Text(
        text = text,
        color = if (download.state == DownloadState.FAILED) colors.accentText else colors.textTertiary,
        fontSize = 13.sp,
    )
}

/**
 * The episodes of a series, one season at a time.
 *
 * A season picker rather than one long list: a box set on this panel runs to
 * two hundred entries, and scrolling past nine seasons to reach the tenth is
 * not browsing. The season on screen opens on the first one, which is where
 * somebody who has not watched it starts.
 */
@Composable
private fun EpisodeList(
    seasons: List<Season>,
    loading: Boolean,
    onPlay: (Episode) -> Unit,
) {
    val colors = MaurimaxTheme.colors

    if (loading) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            CircularProgressIndicator(
                color = colors.accent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.detail_episodes_loading),
                color = colors.textSecondary,
                fontSize = 14.sp,
            )
        }
        return
    }

    if (seasons.isEmpty()) {
        Text(
            text = stringResource(R.string.detail_episodes_none),
            color = colors.textTertiary,
            fontSize = 14.sp,
        )
        return
    }

    var selected by remember(seasons) { mutableStateOf(seasons.first().number) }
    val season = seasons.firstOrNull { it.number == selected } ?: seasons.first()

    // Read once for the whole list rather than once per row: this is a disk
    // read, and a box set has two hundred rows.
    val watched = remember(seasons) {
        Graph.continueWatching().associate { it.id to it.progress }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.detail_episodes),
            color = colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        // Only worth a picker when there is more than one thing to pick.
        if (seasons.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(seasons, key = { it.number }) { entry ->
                    val active = entry.number == selected
                    Text(
                        text = stringResource(R.string.detail_season, entry.number),
                        color = if (active) colors.onAccent else colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (active) colors.accent else colors.surfaceRaised)
                            .clickable { selected = entry.number }
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    )
                }
            }
        }

        season.episodes.forEach { episode ->
            EpisodeRow(
                episode = episode,
                progress = watched[episode.id] ?: 0f,
                onPlay = { onPlay(episode) },
            )
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, progress: Float, onPlay: () -> Unit) {
    val colors = MaurimaxTheme.colors
    val code = stringResource(R.string.detail_episode_code, episode.season, episode.number)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.tile))
            .background(colors.surface)
            .clickable(onClick = onPlay)
            .padding(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(54.dp)
                .clip(RoundedCornerShape(Corners.tile))
                .background(colors.surfaceRaised),
        ) {
            Artwork(
                url = episode.artworkUrl,
                title = "",
                kind = ArtworkKind.POSTER,
                modifier = Modifier.fillMaxSize(),
            )
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .background(colors.accent),
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                // A panel often sends no episode title at all, and "الحلقة 3"
                // beats an empty row.
                text = episode.title.ifBlank { stringResource(R.string.detail_episode, episode.number) },
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (episode.durationMinutes > 0) {
                    code + "  ·  " + stringResource(R.string.detail_minutes, episode.durationMinutes)
                } else {
                    code
                },
                color = colors.textTertiary,
                fontSize = 12.sp,
            )
        }

        PlayGlyph(color = colors.accentText, size = 12.dp)
    }
}
