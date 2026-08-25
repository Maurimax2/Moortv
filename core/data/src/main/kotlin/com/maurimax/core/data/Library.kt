package com.maurimax.core.data

import android.content.Context
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.MediaKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * One saved title, flattened so it can be rendered without going back to the
 * portal. Continue-watching has to work before the catalogue finishes loading —
 * or on a slow connection, at all — so everything a tile needs is stored.
 */
@Serializable
data class SavedItem(
    val id: String,
    val title: String,
    val kind: String,
    val artworkUrl: String = "",
    val playbackUrl: String = "",
    val rating: String = "",
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val updatedAt: Long = 0,
) {
    /** 0f..1f. Zero when the panel gave no duration, which some streams do not. */
    val progress: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    fun toMediaItem() = MediaItem(
        id = id,
        title = title,
        kind = runCatching { MediaKind.valueOf(kind) }.getOrDefault(MediaKind.MOVIE),
        artworkUrl = artworkUrl,
        rating = rating,
        progress = progress,
        playbackUrl = playbackUrl,
    )
}

/**
 * Continue-watching and favourites, kept on the device.
 *
 * Deliberately local rather than on the panel: Xtream has no API for either, so
 * anything server-side would need infrastructure that does not exist yet. The
 * cost is that the list does not follow a customer to a second device, which is
 * the right trade for now and the thing to revisit if accounts ever sync.
 */
object Library {

    private const val PREFS = "maurimax.library"
    private const val KEY_RESUME = "resume"
    private const val KEY_FAVOURITES = "favourites"

    /** Below this a title is treated as "not really started". */
    private const val MIN_PROGRESS = 0.02f

    /** Past this it is finished, and offering to resume it is noise. */
    private const val DONE_PROGRESS = 0.95f

    private const val MAX_RESUME = 20

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ---- continue watching ------------------------------------------------

    fun recordProgress(context: Context, item: SavedItem) {
        val existing = resumeList(context).filterNot { it.id == item.id }
        val fresh = item.copy(updatedAt = System.currentTimeMillis())

        val updated = if (fresh.progress >= DONE_PROGRESS || fresh.progress < MIN_PROGRESS) {
            // Finished, or barely touched: drop it rather than clutter the row.
            existing
        } else {
            (listOf(fresh) + existing).take(MAX_RESUME)
        }
        write(context, KEY_RESUME, updated)
    }

    /** Most recent first, which is the only order this row makes sense in. */
    fun continueWatching(context: Context): List<SavedItem> =
        resumeList(context).sortedByDescending { it.updatedAt }

    fun resumePosition(context: Context, itemId: String): Long =
        resumeList(context).firstOrNull { it.id == itemId }?.positionMs ?: 0L

    fun forget(context: Context, itemId: String) {
        write(context, KEY_RESUME, resumeList(context).filterNot { it.id == itemId })
    }

    // ---- favourites -------------------------------------------------------

    fun favourites(context: Context): List<SavedItem> =
        read(context, KEY_FAVOURITES).sortedByDescending { it.updatedAt }

    fun isFavourite(context: Context, itemId: String): Boolean =
        read(context, KEY_FAVOURITES).any { it.id == itemId }

    /** Returns the new state, so a caller can show it without re-reading. */
    fun toggleFavourite(context: Context, item: SavedItem): Boolean {
        val current = read(context, KEY_FAVOURITES)
        val without = current.filterNot { it.id == item.id }
        val nowFavourite = without.size == current.size

        write(
            context,
            KEY_FAVOURITES,
            if (nowFavourite) listOf(item.copy(updatedAt = System.currentTimeMillis())) + without else without,
        )
        return nowFavourite
    }

    // ---- storage ----------------------------------------------------------

    private fun resumeList(context: Context) = read(context, KEY_RESUME)

    private fun read(context: Context, key: String): List<SavedItem> {
        val raw = prefs(context).getString(key, null) ?: return emptyList()
        // A stored blob written by an older build must never crash the app.
        return runCatching {
            json.decodeFromString(ListSerializer(SavedItem.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun write(context: Context, key: String, items: List<SavedItem>) {
        prefs(context).edit()
            .putString(key, json.encodeToString(ListSerializer(SavedItem.serializer()), items))
            .apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** Flattens a catalogue item for storage. */
fun MediaItem.toSavedItem(positionMs: Long = 0, durationMs: Long = 0) = SavedItem(
    id = id,
    title = title,
    kind = kind.name,
    artworkUrl = artworkUrl,
    playbackUrl = playbackUrl,
    rating = rating,
    positionMs = positionMs,
    durationMs = durationMs,
)
