package com.maurimax.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest

/** What kind of image a tile holds, which decides the surface underneath it. */
enum class ArtworkKind {
    /**
     * Channel logos. Almost universally authored as light marks on black, so
     * they get a dark plate in both themes — on a light tile they read as black
     * boxes floating on paper.
     */
    CHANNEL_LOGO,

    /** Film and series key art. Fills the tile, so the plate barely shows. */
    POSTER,
}

/**
 * Artwork with a graceful failure mode.
 *
 * Panels routinely serve a missing or broken image. The plate underneath is a
 * single neutral surface rather than a colour derived from the title: a wall of
 * differently tinted rectangles reads as a rendering fault, not as design. When
 * there is no image at all the tile carries the name, because an empty coloured
 * block tells the customer nothing.
 */
@Composable
fun Artwork(
    url: String,
    title: String,
    kind: ArtworkKind,
    modifier: Modifier = Modifier,
    /**
     * Bundled artwork to stand in when the panel has none or its image will
     * not load. Films and series get one automatically, keyed off the title so
     * the same film always falls back to the same poster; channels do not,
     * because a film poster behind a news channel is a lie rather than a
     * fallback.
     */
    fallback: Int? = defaultFallbackFor(kind, title),
) {
    var loaded by remember(url) { mutableStateOf(false) }
    var failed by remember(url) { mutableStateOf(false) }

    val colors = MaurimaxTheme.colors
    val onDarkPlate = kind == ArtworkKind.CHANNEL_LOGO

    val plate = if (onDarkPlate) {
        Brush.linearGradient(listOf(Color(0xFF14101C), Color(0xFF0C0812)))
    } else {
        Brush.linearGradient(listOf(colors.surfaceRaised, colors.surface))
    }

    val imageAlpha by animateFloatAsState(
        targetValue = if (loaded) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "artwork-fade",
    )

    Box(modifier = modifier.background(plate)) {
        // Bundled art sits under everything: it fills the frame the moment the
        // screen draws, and whatever the panel sends fades in over it. A tile
        // is never an empty rectangle, even offline.
        if (fallback != null && (!loaded || failed)) {
            Image(
                painter = painterResource(fallback),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // The name shows until artwork arrives, and stays if none ever does.
        if ((!loaded || failed) && fallback == null) {
            BasicText(
                text = title,
                style = TextStyle(
                    color = if (onDarkPlate) {
                        Color.White.copy(alpha = 0.72f)
                    } else {
                        colors.textTertiary
                    },
                    fontSize = fallbackSizeFor(title),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = fallbackSizeFor(title) * 1.25f,
                ),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = Spacing.sm),
            )
        }

        if (url.isNotBlank() && !failed) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                // A logo is fitted with breathing room; key art fills the frame.
                contentScale = if (onDarkPlate) ContentScale.Fit else ContentScale.Crop,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Success -> loaded = true
                        is AsyncImagePainter.State.Error -> failed = true
                        else -> Unit
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (onDarkPlate) Modifier.padding(Spacing.md) else Modifier)
                    .alpha(imageAlpha),
            )
        }
    }
}

/** Long channel names need to step down or they truncate to nothing useful. */
private fun fallbackSizeFor(title: String): TextUnit = when {
    title.length > 28 -> 11.sp
    title.length > 16 -> 12.sp
    else -> 13.sp
}

/**
 * Which bundled poster stands in for a title.
 *
 * Keyed off the name rather than picked at random, so a film keeps the same
 * stand-in every time it is drawn — a tile that changes artwork between scrolls
 * looks broken.
 */
private fun defaultFallbackFor(kind: ArtworkKind, title: String): Int? = when (kind) {
    ArtworkKind.CHANNEL_LOGO -> null
    ArtworkKind.POSTER -> if (title.isBlank()) null else Showcase.pick(Showcase.all, title).poster
}
