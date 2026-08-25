package com.maurimax.feature.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.maurimax.core.data.Graph
import com.maurimax.core.data.SavedItem
import com.maurimax.core.model.MediaItem

/**
 * Full-screen playback.
 *
 * Its own activity so it can own orientation, the keep-awake flag and the
 * system bars without the catalogue having to unwind any of that afterwards.
 */
class PlayerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val streamUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val isLive = intent.getBooleanExtra(EXTRA_LIVE, false)
        val itemId = intent.getStringExtra(EXTRA_ID).orEmpty()
        val artwork = intent.getStringExtra(EXTRA_ARTWORK).orEmpty()
        val kind = intent.getStringExtra(EXTRA_KIND).orEmpty()

        // A downloaded copy wins over the panel: it plays instantly, costs no
        // data, and keeps working when the connection does not.
        val url = Graph.localUrl(itemId) ?: streamUrl

        // Where this customer got to last time. Live has no meaningful position.
        val startAt = if (isLive || itemId.isBlank()) {
            0L
        } else {
            Graph.resumePosition(itemId)
        }

        // A phone plays video landscape; a TV is already landscape and must not
        // be told otherwise.
        if (!resources.getBoolean(R.bool.is_television)) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }

        // Nobody wants the screen dimming twenty minutes into a match.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            PlayerScreen(
                url = url,
                title = title,
                isLive = isLive,
                startPositionMs = startAt,
                onProgress = { position, duration ->
                    // Written as playback stops rather than continuously: a
                    // preference write per second would be wasteful and adds
                    // nothing, since only the last position matters.
                    if (!isLive && itemId.isNotBlank()) {
                        Graph.recordProgress(
                            SavedItem(
                                id = itemId,
                                title = title,
                                kind = kind.ifBlank { "MOVIE" },
                                artworkUrl = artwork,
                                playbackUrl = streamUrl,
                                positionMs = position,
                                durationMs = duration,
                            ),
                        )
                    }
                },
                onBack = { finish() },
            )
        }
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_LIVE = "live"
        private const val EXTRA_ID = "id"
        private const val EXTRA_ARTWORK = "artwork"
        private const val EXTRA_KIND = "kind"

        /** The only way in, so callers cannot get the extras wrong. */
        fun intent(context: Context, item: MediaItem): Intent =
            Intent(context, PlayerActivity::class.java)
                .putExtra(EXTRA_URL, item.playbackUrl)
                .putExtra(EXTRA_TITLE, item.title)
                .putExtra(EXTRA_LIVE, item.isLive)
                .putExtra(EXTRA_ID, item.id)
                .putExtra(EXTRA_ARTWORK, item.artworkUrl)
                .putExtra(EXTRA_KIND, item.kind.name)
    }
}
