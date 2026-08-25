package com.maurimax.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/** Bundled key art, so the very first launch is not a blank screen. */
private val BUNDLED = listOf(
    R.drawable.wall_01, R.drawable.wall_02, R.drawable.wall_03, R.drawable.wall_04,
    R.drawable.wall_05, R.drawable.wall_06, R.drawable.wall_07, R.drawable.wall_08,
    R.drawable.wall_09, R.drawable.wall_10, R.drawable.wall_11, R.drawable.wall_12,
)

/**
 * A wall of key art behind the sign-in screen.
 *
 * Three columns drifting slowly in alternate directions, dimmed hard and washed
 * to the theme ground so nothing competes with the form. Built from posters the
 * catalogue served last time, so the backdrop is the customer's own service
 * rather than stock imagery — and it costs nothing, since the images are
 * already in the disk cache.
 *
 * With no remembered posters, which is the very first launch, it renders
 * nothing and the caller's plain background shows through.
 */
@Composable
fun PosterWall(
    posters: List<String>,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    val columns = 3

    // Remembered artwork first; bundled art only until there is any. After one
    // sign-in the wall is the customer's own catalogue.
    val remote = posters.filter { it.isNotBlank() }
    val useBundled = remote.isEmpty()
    val count = if (useBundled) BUNDLED.size else remote.size
    val perColumn = ((count + columns - 1) / columns).coerceAtLeast(2)

    // Slow drift, so the screen breathes without ever asking to be watched.
    val drift = rememberInfiniteTransition(label = "poster-drift")
    val shift by drift.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "poster-shift",
    )

    Box(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
        ) {
            repeat(columns) { column ->
                val range = (column * perColumn) until minOf((column + 1) * perColumn, count)
                val indices = if (range.isEmpty()) (0 until minOf(perColumn, count)) else range

                // Alternate columns move opposite ways, which reads as depth
                // rather than as one sheet sliding.
                val direction = if (column % 2 == 0) 1f else -1f

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .offset(y = (shift * 26f * direction).dp),
                ) {
                    indices.forEach { index ->
                        val tile = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceRaised)

                        if (useBundled) {
                            Image(
                                painter = painterResource(BUNDLED[index % BUNDLED.size]),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = tile,
                            )
                        } else {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(remote[index % remote.size])
                                    .crossfade(false)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = tile,
                            )
                        }
                    }
                }
            }
        }

        // Two washes: an even dim over the art, then a fade into the ground so
        // the form at the bottom sits on solid colour.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.ground.copy(alpha = 0.74f)),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to colors.ground.copy(alpha = 0.55f),
                        0.42f to colors.ground.copy(alpha = 0.86f),
                        0.72f to colors.ground,
                        1f to colors.ground,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x1A4E0D83)),
        )
    }
}
