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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maurimax.core.designsystem.Artwork
import com.maurimax.core.designsystem.ArtworkKind
import com.maurimax.core.designsystem.Corners
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.MediaItem

/**
 * The featured title at the top of a tab.
 *
 * A catalogue that opens straight into rows of small tiles has no focal point —
 * everything is the same size, so nothing is being recommended. One large piece
 * of artwork with a play button gives the screen somewhere to start, which is
 * what every mature streaming home does.
 */
@Composable
fun HeroBanner(
    item: MediaItem,
    onPlay: (MediaItem) -> Unit,
    onOpen: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(if (item.isLive) 16f / 10f else 3f / 4f)
            .clickable { onOpen(item) },
    ) {
        Artwork(
            url = item.artworkUrl,
            title = item.title,
            kind = if (item.isLive) ArtworkKind.CHANNEL_LOGO else ArtworkKind.POSTER,
            modifier = Modifier.fillMaxSize(),
        )

        // Fades into the page so the rows below feel attached to the hero
        // rather than sitting under a photograph.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to colors.ground.copy(alpha = 0.35f),
                        0.78f to colors.ground.copy(alpha = 0.92f),
                        1f to colors.ground,
                    ),
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            Text(
                text = item.title,
                color = colors.textPrimary,
                fontSize = 26.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            HeroMeta(item)

            PlayButton(
                onClick = { if (item.isPlayable) onPlay(item) else onOpen(item) },
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}

@Composable
private fun HeroMeta(item: MediaItem) {
    val colors = MaurimaxTheme.colors
    val kind = stringResource(item.kind.labelRes)
    val score = item.rating.trim().toDoubleOrNull()

    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.isLive) {
            LiveDot()
        }
        Text(
            text = if (score != null && score > 0.0) {
                "$kind  ·  ★ ${item.rating.trim()}"
            } else {
                kind
            },
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** A live channel gets a marker, since "live" is the whole value of the tile. */
@Composable
private fun LiveDot() {
    Box(
        modifier = Modifier
            .size(7.dp)
            .background(MaurimaxTheme.colors.accent, RoundedCornerShape(50)),
    )
}

@Composable
fun PlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(46.dp)
            .background(colors.accent, RoundedCornerShape(Corners.control))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.xl),
    ) {
        PlayGlyph(color = colors.onAccent)
        Text(
            text = stringResource(R.string.action_play),
            color = colors.onAccent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Drawn rather than imported, so the lockup needs no icon dependency. */
@Composable
fun PlayGlyph(color: Color, size: androidx.compose.ui.unit.Dp = 13.dp) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(this@Canvas.size.width, this@Canvas.size.height / 2f)
            lineTo(0f, this@Canvas.size.height)
            close()
        }
        drawPath(path, color)
    }
}
