package com.maurimax.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.MediaItem
import kotlinx.coroutines.delay

/**
 * The title the screen opens on.
 *
 * Full-bleed artwork carried down into the page by a long gradient, with the
 * name and two actions sitting on the dark end of it. Not a card: a hero inside
 * a rounded panel is a banner, and a banner is something you scroll past.
 *
 * The panel serves portrait key art and no backdrops, so the image is cropped
 * from the top — where the subject of a poster almost always is — rather than
 * letterboxed into a wide frame with bars either side.
 */
@Composable
fun Hero(
    items: List<MediaItem>,
    onPlay: (MediaItem) -> Unit,
    inMyList: (MediaItem) -> Boolean,
    onToggleMyList: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    if (items.isEmpty()) return

    var index by remember(items) { mutableStateOf(0) }

    // Nine seconds: long enough to read a title and press play, short enough
    // that a second one is seen before anybody scrolls past. A section with a
    // single title does not sit there ticking.
    LaunchedEffect(items) {
        while (items.size > 1) {
            delay(9_000)
            index = (index + 1) % items.size
        }
    }

    val item = items[index.coerceIn(items.indices)]

    // 460 rather than filling most of the screen: on an 800dp phone that
    // leaves exactly enough for one complete rail under it, and a rail cut off
    // by the navigation is what makes a catalogue look like it ran out.
    Box(modifier = modifier.fillMaxWidth().height(460.dp)) {
        Crossfade(
            targetState = item,
            animationSpec = tween(durationMillis = 700),
            label = "hero art",
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            // Only the panel's own artwork. A bundled poster behind a title
            // from the server is a picture of a different film.
            Box(modifier = Modifier.fillMaxSize().background(colors.surface)) {
                if (current.artworkUrl.isNotBlank()) {
                    AsyncImage(
                        model = current.artworkUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.42f to colors.ground.copy(alpha = 0.20f),
                        0.72f to colors.ground.copy(alpha = 0.88f),
                        1f to colors.ground,
                    ),
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.displayLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            HeroMeta(item)

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Spacing.md),
            ) {
                HeroAction(
                    label = stringResource(R.string.action_play),
                    icon = com.maurimax.core.designsystem.R.drawable.ic_play,
                    primary = true,
                    onClick = { onPlay(item) },
                )
                val saved = inMyList(item)
                HeroAction(
                    label = stringResource(
                        if (saved) R.string.action_in_my_list else R.string.action_my_list,
                    ),
                    icon = if (saved) {
                        com.maurimax.core.designsystem.R.drawable.ic_check
                    } else {
                        com.maurimax.core.designsystem.R.drawable.ic_plus
                    },
                    primary = false,
                    onClick = { onToggleMyList(item) },
                )
            }
        }
    }
}

/**
 * Only what is actually known.
 *
 * The panel sends a rating for some titles, a year for almost none and a
 * runtime for fewer still, so this prints whatever it has and nothing else —
 * a fabricated "2026 · Action · 2h 10m" under every title would be inventing
 * facts about a customer's own catalogue.
 */
@Composable
private fun HeroMeta(item: MediaItem) {
    val colors = MaurimaxTheme.colors
    val kind = stringResource(item.kind.labelRes)
    val score = item.rating.trim().toDoubleOrNull()

    val parts = buildList {
        if (item.isLive) add(stringResource(R.string.kind_live)) else add(kind)
        if (score != null && score > 0.0) add("★ ${item.rating.trim()}")
        if (item.year > 0) add(item.year.toString())
        if (item.durationMinutes > 0) add(stringResource(R.string.detail_minutes, item.durationMinutes))
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = Spacing.sm),
    ) {
        if (item.isLive) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.accent),
            )
        }
        Text(
            text = parts.joinToString("  ·  "),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun HeroAction(
    label: String,
    icon: Int,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaurimaxTheme.colors
    val content = if (primary) colors.onPrimaryFill else colors.textPrimary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (primary) colors.primaryFill else colors.secondaryFill)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.lg),
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(content),
            modifier = Modifier.size(if (primary) 13.dp else 15.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontSize = 15.sp,
            color = content,
            maxLines = 1,
        )
    }
}
