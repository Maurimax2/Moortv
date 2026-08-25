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
import com.maurimax.core.model.MediaItem
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
    onRefresh: () -> Unit = {},
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
                    color = colors.textPrimary,
                    fontSize = 27.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Black,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Pill(text = kind)
                    if (score != null && score > 0.0) {
                        Pill(text = "★ ${item.rating.trim()}", accent = true)
                    }
                }

                if (item.isPlayable) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PlayButton(
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
                            )
                        }
                    }

                    if (download != null) {
                        DownloadNote(download)
                    }
                } else {
                    // Honest rather than a dead button: a series needs its
                    // episode list before anything here can be played.
                    Text(
                        text = stringResource(R.string.detail_episodes_soon),
                        color = colors.textTertiary,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Corners.control))
                            .background(colors.surfaceRaised)
                            .padding(Spacing.md),
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

/**
 * Star toggle.
 *
 * Its own square beside Play rather than an icon in the corner: favouriting is
 * a deliberate act, and burying it in chrome makes it feel accidental.
 */
@Composable
private fun FavouriteButton(favourite: Boolean, onClick: () -> Unit) {
    val colors = MaurimaxTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(Corners.control))
            .background(if (favourite) colors.accent else colors.surfaceRaised)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (favourite) "remove from favourites" else "add to favourites"
            },
    ) {
        Text(
            text = if (favourite) "★" else "☆",
            color = if (favourite) colors.onAccent else colors.textSecondary,
            fontSize = 21.sp,
        )
    }
}

@Composable
private fun Pill(text: String, accent: Boolean = false) {
    val colors = MaurimaxTheme.colors
    Text(
        text = text,
        color = if (accent) colors.accentText else colors.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceRaised)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    )
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
) {
    val colors = MaurimaxTheme.colors
    val done = download?.state == DownloadState.DONE
    val running = download != null && !done && download.state != DownloadState.FAILED

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(Corners.control))
            .background(if (done) colors.accent else colors.surfaceRaised)
            .clickable { if (download == null) onDownload() else onRemove() }
            .semantics {
                contentDescription = if (download == null) "download" else "remove download"
            },
    ) {
        when {
            done -> Text("✓", color = colors.onAccent, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            running -> Text(
                text = "${(download.progress * 100).toInt()}%",
                color = colors.accentText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            else -> Text("↓", color = colors.textSecondary, fontSize = 21.sp)
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
