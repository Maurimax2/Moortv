package com.maurimax.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maurimax.core.designsystem.ChannelList
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem

/**
 * The channel list, over the running stream.
 *
 * The stream is the page and this is a layer on it — not a column beside a
 * video shrunk into a corner. Nothing here is boxed: the panel is a wash that
 * fades into the picture, so the match is still the thing you are looking at
 * while you decide what to watch next.
 */
@Composable
fun ChannelOverlay(
    open: Boolean,
    groups: List<ContentRow>,
    selectedGroup: Int,
    onGroupSelect: (Int) -> Unit,
    playing: MediaItem?,
    onChannelClick: (MediaItem) -> Unit,
    /** Digits typed on the remote, shown as they are entered. Empty when none. */
    typed: String,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    /** A thumb needs something to press. A remote already has the D-pad. */
    showOpenButton: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors

    Box(modifier = modifier.fillMaxSize()) {
        // What is on, bottom leading. Without EPG there is no programme to
        // name, so this says the one thing that is true: which channel this is.
        AnimatedVisibility(
            visible = !open,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            NowPlaying(playing)
        }

        if (typed.isNotEmpty()) {
            TypedNumber(typed, modifier = Modifier.align(Alignment.TopStart))
        }

        if (showOpenButton) {
            AnimatedVisibility(
                visible = !open,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd),
            ) {
                OpenChannels(onOpen)
            }
        }

        // Tapping the picture puts it back. On a phone that is the gesture
        // people try first, and on a remote Back does the same thing.
        if (open) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose,
                    ),
            )
        }

        AnimatedVisibility(
            visible = open,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(LAYER_WIDTH)
                    // A wash rather than a wall: solid where the text sits,
                    // gone by the time it reaches the picture.
                    .background(
                        Brush.horizontalGradient(
                            0f to colors.ground.copy(alpha = 0f),
                            0.10f to colors.ground.copy(alpha = 0.90f),
                            0.30f to colors.ground.copy(alpha = 0.98f),
                            1f to colors.ground,
                        ),
                    ),
            ) {
                ChannelList(
                    groups = groups,
                    selectedGroup = selectedGroup,
                    onGroupSelect = onGroupSelect,
                    onChannelClick = onChannelClick,
                    playingId = playing?.id,
                    contentPadding = PaddingValues(horizontal = Spacing.md),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = Spacing.lg,
                            bottom = Spacing.lg,
                            start = Spacing.lg,
                            end = Spacing.md,
                        ),
                )
            }
        }
    }
}

@Composable
private fun NowPlaying(playing: MediaItem?, modifier: Modifier = Modifier) {
    if (playing == null) return
    val colors = MaurimaxTheme.colors

    Column(
        modifier = modifier.padding(start = Spacing.xl, bottom = Spacing.xl, end = Spacing.xl),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.accent),
            )
            if (playing.number > 0) {
                Text(
                    text = playing.number.toString(),
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
        Text(
            text = playing.title,
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

/**
 * The digits as they are typed.
 *
 * A number on a remote is how someone who watches the same channel every night
 * gets to it — three presses instead of a scroll through nine thousand.
 */
@Composable
private fun TypedNumber(typed: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier
            .padding(start = Spacing.xl, top = Spacing.xl)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Text(
            text = typed,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun OpenChannels(onOpen: () -> Unit) {
    val colors = MaurimaxTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier
            .padding(Spacing.xl)
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onOpen)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm + 2.dp),
    ) {
        Text(
            text = stringResource(R.string.player_channels),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Wide enough for a channel name, narrow enough to leave the picture. */
private val LAYER_WIDTH = 400.dp
