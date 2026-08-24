package com.maurimax.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext

/**
 * Artwork with a graceful failure mode.
 *
 * Panels routinely serve a missing, broken or tiny logo, and a grey box for
 * every third tile is what makes a catalogue look cheap. Until the image
 * resolves — and permanently, if it never does — the tile shows a tinted plate
 * carrying the title, which reads as deliberate rather than broken.
 */
@Composable
fun Artwork(
    url: String,
    title: String,
    fallbackTint: Long,
    modifier: Modifier = Modifier,
    /** Portrait key art is cropped to fill; wide channel logos are fit inside. */
    crop: Boolean = true,
    fallbackTextSize: TextUnit = 13.sp,
    /**
     * Whether the fallback plate carries the title. False where the layout
     * already labels the tile underneath — printing the name twice is the
     * clearest tell of a cheap catalogue.
     */
    showFallbackLabel: Boolean = true,
) {
    var loaded by remember(url) { mutableStateOf(false) }
    var failed by remember(url) { mutableStateOf(false) }

    val imageAlpha by animateFloatAsState(
        targetValue = if (loaded) 1f else 0f,
        label = "artwork-fade",
    )

    val light = MaurimaxTheme.colors.isLight
    Box(modifier = modifier.background(plateBrush(fallbackTint, light))) {
        // The plate stays behind the image rather than being swapped out, so a
        // logo with transparency has something considered to sit on.
        if ((!loaded || failed) && showFallbackLabel) {
            BasicText(
                text = title,
                style = TextStyle(
                    // On a pale plate white would vanish, so the label follows the theme.
                    color = if (light) {
                        MaurimaxTheme.colors.textSecondary
                    } else {
                        Color.White.copy(alpha = 0.92f)
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = fallbackTextSize,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(Spacing.sm),
            )
        }

        if (url.isNotBlank() && !failed) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(false)
                    .build(),
                contentDescription = title,
                contentScale = if (crop) ContentScale.Crop else ContentScale.Fit,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> loaded = true
                        is AsyncImagePainter.State.Error -> failed = true
                        else -> Unit
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (crop) Spacing.xs * 0 else Spacing.sm)
                    .alpha(imageAlpha),
            )
        }
    }
}

/**
 * A two-stop wash from the item's tint, so plates differ but stay in family.
 *
 * On light the tint is lightened rather than shown at full strength: a
 * saturated block on paper reads as an error state, while the same block on
 * near-black reads as artwork.
 */
private fun plateBrush(tint: Long, light: Boolean): Brush {
    val base = Color(tint)
    return if (light) {
        Brush.linearGradient(
            listOf(
                base.copy(alpha = 0.30f).compositeOver(Color.White),
                base.copy(alpha = 0.14f).compositeOver(Color.White),
            ),
        )
    } else {
        Brush.linearGradient(
            listOf(
                base.copy(alpha = 0.92f),
                base.copy(alpha = 0.45f),
            ),
        )
    }
}
