package com.maurimax.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.maurimax.core.designsystem.Artwork
import com.maurimax.core.designsystem.ArtworkKind
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.MediaItem

/**
 * Everything in one place — a whole category, or everything a search found.
 *
 * A grid rather than more rails: a rail is for browsing a shortlist, and this
 * is the screen a customer reaches when the shortlist was not enough. Three
 * columns of portrait art, two of channel logos, because a logo is a wide mark
 * and squeezing it into a poster's frame wastes most of the tile.
 */
@Composable
fun MediaGrid(
    title: String,
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    val landscape = items.firstOrNull()?.isLive == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.md),
        ) {
            if (onBack != null) {
                Text(
                    text = "‹",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.textPrimary,
                    modifier = Modifier.clickable(onClick = onBack),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.count_titles, items.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(if (landscape) 2 else 3),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = PaddingValues(
                start = Spacing.md,
                end = Spacing.md,
                bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            ),
        ) {
            itemsIndexed(items, key = { index, item -> "$index-${item.id}" }) { _, item ->
                GridTile(item = item, landscape = landscape, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun GridTile(item: MediaItem, landscape: Boolean, onClick: () -> Unit) {
    val colors = MaurimaxTheme.colors

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (landscape) 16f / 9f else 2f / 3f)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.surface),
        ) {
            Artwork(
                url = item.artworkUrl,
                title = item.title,
                kind = if (item.isLive) ArtworkKind.CHANNEL_LOGO else ArtworkKind.POSTER,
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
        // Channel logos rarely say which channel they are, and a poster in a
        // grid is small enough that its own title can be hard to read.
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
