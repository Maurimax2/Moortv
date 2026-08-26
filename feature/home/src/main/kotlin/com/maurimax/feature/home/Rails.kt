package com.maurimax.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maurimax.core.designsystem.Artwork
import com.maurimax.core.designsystem.ArtworkKind
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.designsystem.badgeRes
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.Sports

/** How many titles a rail shows before "see all" takes over. */
private const val PREVIEW = 20

/** Poster, portrait. Large enough that the artwork is worth looking at. */
private val PosterWidth = 124.dp
private val PosterHeight = 186.dp

/** Channel, landscape, because a logo is a wide mark. */
private val ChannelWidth = 152.dp
private val ChannelHeight = 88.dp

/**
 * A titled row of artwork.
 *
 * The image is the card — no surface around it, no caption under a poster that
 * already carries its name. Channels keep their caption, because a logo often
 * does not say which channel it is and half of them have no logo at all.
 */
@Composable
fun Rail(
    row: ContentRow,
    onItemClick: (MediaItem) -> Unit,
    onSeeAll: ((ContentRow) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier,
    ) {
        if (row.title.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onSeeAll != null) Modifier.clickable { onSeeAll(row) } else Modifier,
                    )
                    .padding(horizontal = Spacing.md),
            ) {
                // Recognised faster than the words beside it.
                Sports.badge(row.title)?.let { league ->
                    Image(
                        painter = painterResource(league.badgeRes),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaurimaxTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                // The count is the point: a rail shows a handful, and without
                // this there is no way to know whether the category holds nine
                // titles or nine hundred.
                Text(
                    text = row.items.size.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaurimaxTheme.colors.textTertiary,
                )
                if (onSeeAll != null && row.items.size > PREVIEW) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.see_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaurimaxTheme.colors.accentText,
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(horizontal = Spacing.md),
        ) {
            // Position is part of the key: a panel is free to return the same
            // id twice in one category, and a duplicate key crashes the row.
            // A rail is a shortlist. Everything else is behind "see all",
            // because a horizontal list nobody can reach the end of is not a
            // way to browse nine hundred titles.
            itemsIndexed(row.items.take(PREVIEW), key = { index, item -> "$index-${item.id}" }) { _, item ->
                if (item.isLive) {
                    ChannelCard(item = item, onClick = { onItemClick(item) })
                } else {
                    PosterCard(item = item, onClick = { onItemClick(item) })
                }
            }
        }
    }
}

@Composable
private fun PosterCard(item: MediaItem, onClick: () -> Unit) {
    val colors = MaurimaxTheme.colors

    Box(
        modifier = Modifier
            .width(PosterWidth)
            .height(PosterHeight)
            .clip(RoundedCornerShape(6.dp))
            .background(colors.surface)
            .clickable(onClick = onClick),
    ) {
        Artwork(
            url = item.artworkUrl,
            title = item.title,
            kind = ArtworkKind.POSTER,
            modifier = Modifier.fillMaxSize(),
        )

        if (item.progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(colors.outline),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.progress)
                        .fillMaxSize()
                        .background(colors.accent),
                )
            }
        }
    }
}

@Composable
private fun ChannelCard(item: MediaItem, onClick: () -> Unit) {
    val colors = MaurimaxTheme.colors

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .width(ChannelWidth)
            .clickable(onClick = onClick),
    ) {
        Artwork(
            url = item.artworkUrl,
            title = item.title,
            kind = ArtworkKind.CHANNEL_LOGO,
            modifier = Modifier
                .fillMaxWidth()
                .height(ChannelHeight)
                .clip(RoundedCornerShape(6.dp)),
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * What the screen looks like while the panel is still answering.
 *
 * Shapes where the artwork will be, in the sizes it will actually occupy, so
 * the page does not jump when the rails arrive. Deliberately still: a shimmer
 * sweeping across an empty screen draws the eye to the fact that there is
 * nothing there yet.
 */
@Composable
fun RailSkeleton(portrait: Boolean, modifier: Modifier = Modifier) {
    val colors = MaurimaxTheme.colors

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = Spacing.md)
                .width(132.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.surface),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(horizontal = Spacing.md),
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(if (portrait) PosterWidth else ChannelWidth)
                        .height(if (portrait) PosterHeight else ChannelHeight)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surface),
                )
            }
        }
    }
}
