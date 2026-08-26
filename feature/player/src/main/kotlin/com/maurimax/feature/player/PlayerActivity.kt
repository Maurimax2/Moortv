package com.maurimax.feature.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.maurimax.core.data.Graph
import com.maurimax.core.data.LiveQueue
import com.maurimax.core.data.SavedItem
import com.maurimax.core.designsystem.MaurimaxMobileTheme
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.MediaKind
import kotlinx.coroutines.delay

/**
 * Full-screen playback.
 *
 * Its own activity so it can own orientation, the keep-awake flag and the
 * system bars without the catalogue having to unwind any of that afterwards.
 *
 * For a live channel it is also where channels are changed: the list arrives
 * over the running stream rather than sending the customer back to a menu, so
 * the picture never stops while they decide.
 */
class PlayerActivity : ComponentActivity() {

    /** Set by the composition so remote presses can reach it. */
    private var onKey: ((KeyEvent) -> Boolean)? = null

    /**
     * Taken before Compose sees it.
     *
     * A television's number keys and channel keys have to work while the video
     * has focus, and the Media3 controller claims the D-pad the moment it is
     * visible — so this arrives here first and passes on what it does not use.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        onKey?.invoke(event) == true || super.dispatchKeyEvent(event)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val streamUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val isLive = intent.getBooleanExtra(EXTRA_LIVE, false)
        val itemId = intent.getStringExtra(EXTRA_ID).orEmpty()
        val artwork = intent.getStringExtra(EXTRA_ARTWORK).orEmpty()
        val kind = intent.getStringExtra(EXTRA_KIND).orEmpty()

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
            // Video is watched in the dark on both form factors, whatever the
            // customer chose for the catalogue.
            MaurimaxMobileTheme(dark = true) {
                if (isLive) {
                    LivePlayback(
                        launchedId = itemId,
                        launchedTitle = title,
                        launchedUrl = streamUrl,
                        launchedArtwork = artwork,
                        onRemote = resources.getBoolean(R.bool.is_television),
                        register = { handler -> onKey = handler },
                        onBack = { finish() },
                    )
                } else {
                    // Where this customer got to last time.
                    val startAt = remember { Graph.resumePosition(itemId) }
                    // A downloaded copy wins over the panel: it plays instantly,
                    // costs no data, and works when the connection does not.
                    val url = remember { Graph.localUrl(itemId) ?: streamUrl }

                    PlayerScreen(
                        url = url,
                        title = title,
                        isLive = false,
                        startPositionMs = startAt,
                        onProgress = { position, duration ->
                            // Written as playback stops rather than continuously:
                            // a preference write per second adds nothing, since
                            // only the last position matters.
                            if (itemId.isNotBlank()) {
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
        }
    }

    override fun onDestroy() {
        onKey = null
        super.onDestroy()
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

/**
 * Live playback, with the channel list on top of it.
 *
 * The remote does the three things a remote does on a television: up and down
 * change channel, a number goes straight to one, and OK opens the list.
 */
@Composable
private fun LivePlayback(
    launchedId: String,
    launchedTitle: String,
    launchedUrl: String,
    launchedArtwork: String,
    onRemote: Boolean,
    register: ((KeyEvent) -> Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val groups = remember { LiveQueue.groups() }
    val flat = remember(groups) { groups.flatMap { it.items } }

    // The channel that started this, taken from the queue where possible so it
    // carries its number, and rebuilt from the intent when the queue is empty.
    val launched = remember(flat) {
        flat.firstOrNull { it.id == launchedId }
            ?: MediaItem(
                id = launchedId,
                title = launchedTitle,
                kind = MediaKind.LIVE,
                artworkUrl = launchedArtwork,
                playbackUrl = launchedUrl,
            )
    }

    var playing by remember { mutableStateOf(launched) }
    var open by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    var group by remember {
        mutableStateOf(groups.indexOfFirst { row -> row.items.any { it.id == launched.id } }.coerceAtLeast(0))
    }

    /** Steps through the whole line rather than the group, the way a box does. */
    fun step(by: Int) {
        if (flat.isEmpty()) return
        val at = flat.indexOfFirst { it.id == playing.id }
        if (at < 0) return
        playing = flat[(at + by + flat.size) % flat.size]
    }

    // Typing stops, and a moment later the channel changes. No confirm key:
    // nobody presses OK after dialling a channel on a television.
    LaunchedEffect(typed) {
        if (typed.isEmpty()) return@LaunchedEffect
        delay(1_200)
        val wanted = typed.toIntOrNull()
        val match = wanted?.let { number -> flat.firstOrNull { it.number == number } }
        if (match != null) {
            playing = match
            open = false
        }
        typed = ""
    }

    val handler: (KeyEvent) -> Boolean = handler@{ event ->
        if (event.action != KeyEvent.ACTION_DOWN) return@handler false
        val digit = event.keyCode - KeyEvent.KEYCODE_0
        when {
            digit in 0..9 -> {
                // Four is the longest number any panel gives out, and it stops
                // a stuck key turning into nonsense.
                typed = (typed + digit).takeLast(4)
                true
            }

            open -> false

            event.keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                event.keyCode == KeyEvent.KEYCODE_CHANNEL_UP -> {
                step(-1)
                true
            }

            event.keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                event.keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                step(1)
                true
            }

            // Only worth opening if there is something to open.
            (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                event.keyCode == KeyEvent.KEYCODE_ENTER ||
                event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) && flat.isNotEmpty() -> {
                open = true
                true
            }

            else -> false
        }
    }

    // Re-registered after each composition so the handler is never reading a
    // channel or a typed number from a frame that has already gone.
    SideEffect { register(handler) }

    // Back closes the list before it leaves the channel.
    BackHandler(enabled = open) { open = false }

    PlayerScreen(
        url = playing.playbackUrl,
        title = playing.title,
        isLive = true,
        overlayOpen = open,
        onBack = onBack,
    ) {
        ChannelOverlay(
            open = open,
            groups = groups,
            selectedGroup = group,
            onGroupSelect = { group = it },
            playing = playing,
            onChannelClick = { channel ->
                playing = channel
                open = false
            },
            typed = typed,
            onOpen = { open = true },
            onClose = { open = false },
            showOpenButton = !onRemote && flat.isNotEmpty(),
        )
    }
}
