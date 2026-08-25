package com.maurimax.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.maurimax.core.designsystem.Brand

/**
 * Video with the standard Media3 controls.
 *
 * The controls come from PlayerView rather than being rebuilt in Compose: it
 * already handles scrubbing, buffering states, D-pad on a remote and the
 * accessibility surface, and a hand-rolled version of that is a lot of surface
 * to get subtly wrong.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    url: String,
    title: String,
    isLive: Boolean,
    startPositionMs: Long = 0L,
    onProgress: (positionMs: Long, durationMs: Long) -> Unit = { _, _ -> },
    onBack: () -> Unit,
) {
    val context = LocalContext.current

    var buffering by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf(0) }

    val player = remember(attempt) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            // Picks up where the customer left off. Skipped for live, which has
            // no meaningful position to return to.
            if (!isLive && startPositionMs > 0) seekTo(startPositionMs)
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) failed = false
            }

            override fun onPlayerError(error: PlaybackException) {
                failed = true
                buffering = false
            }
        }
        player.addListener(listener)
        onDispose {
            // Read the position before releasing, or it is gone.
            val position = player.currentPosition
            val duration = player.duration.takeIf { it > 0 } ?: 0L
            if (position > 0) onProgress(position, duration)

            player.removeListener(listener)
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    controllerShowTimeoutMs = 3_000
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    // A live stream cannot be scrubbed, so hiding the bar avoids
                    // offering something that does nothing.
                    setShowFastForwardButton(!isLive)
                    setShowRewindButton(!isLive)
                    setKeepContentOnPlayerReset(true)
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )

        if (buffering && !failed) {
            CircularProgressIndicator(
                color = Brand.Orange,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        if (failed) {
            PlaybackError(
                onRetry = { attempt++ },
                onBack = onBack,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun PlaybackError(
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.player_error),
            color = Color.White,
            fontSize = 16.sp,
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Brand.Orange,
                contentColor = Color.White,
            ),
        ) {
            Text(stringResource(R.string.player_retry), fontWeight = FontWeight.SemiBold)
        }
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x33FFFFFF),
                contentColor = Color.White,
            ),
        ) {
            Text(stringResource(R.string.player_back))
        }
    }
}
