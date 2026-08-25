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
 *
 * Every list is kept per account. Two lines on one box are two different
 * people — usually a parent and a child — and showing one of them what the
 * other stopped watching is both wrong and, occasionally, awkward.
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

    fun recordProgress(context: Context, owner: String, item: SavedItem) {
        val existing = resumeList(context, owner).filterNot { it.id == item.id }
        val fresh = item.copy(updatedAt = System.currentTimeMillis())

        val updated = if (fresh.progress >= DONE_PROGRESS || fresh.progress < MIN_PROGRESS) {
            // Finished, or barely touched: drop it rather than clutter the row.
            existing
        } else {
            (listOf(fresh) + existing).take(MAX_RESUME)
        }
        write(context, owner, KEY_RESUME, updated)
    }

    /** Most recent first, which is the only order this row makes sense in. */
    fun continueWatching(context: Context, owner: String): List<SavedItem> =
        resumeList(context, owner).sortedByDescending { it.updatedAt }

    fun resumePosition(context: Context, owner: String, itemId: String): Long =
        resumeList(context, owner).firstOrNull { it.id == itemId }?.positionMs ?: 0L

    fun forget(context: Context, owner: String, itemId: String) {
        write(context, owner, KEY_RESUME, resumeList(context, owner).filterNot { it.id == itemId })
    }

    // ---- favourites -------------------------------------------------------

    fun favourites(context: Context, owner: String): List<SavedItem> =
        read(context, owner, KEY_FAVOURITES).sortedByDescending { it.updatedAt }

    fun isFavourite(context: Context, owner: String, itemId: String): Boolean =
        read(context, owner, KEY_FAVOURITES).any { it.id == itemId }

    /** Returns the new state, so a caller can show it without re-reading. */
    fun toggleFavourite(context: Context, owner: String, item: SavedItem): Boolean {
        val current = read(context, owner, KEY_FAVOURITES)
        val without = current.filterNot { it.id == item.id }
        val nowFavourite = without.size == current.size

        write(
            context,
            owner,
            KEY_FAVOURITES,
            if (nowFavourite) listOf(item.copy(updatedAt = System.currentTimeMillis())) + without else without,
        )
        return nowFavourite
    }

    /** Drops everything an account saved, for when it is removed from the device. */
    fun erase(context: Context, owner: String) {
        prefs(context, owner).edit().clear().apply()
    }

    // ---- storage ----------------------------------------------------------

    private fun resumeList(context: Context, owner: String) = read(context, owner, KEY_RESUME)

    private fun read(context: Context, owner: String, key: String): List<SavedItem> {
        val raw = prefs(context, owner).getString(key, null) ?: return emptyList()
        // A stored blob written by an older build must never crash the app.
        return runCatching {
            json.decodeFromString(ListSerializer(SavedItem.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun write(context: Context, owner: String, key: String, items: List<SavedItem>) {
        prefs(context, owner).edit()
            .putString(key, json.encodeToString(ListSerializer(SavedItem.serializer()), items))
            .apply()
    }

    /**
     * One file per account, plus a one-time hand-over of the shared file that
     * builds before multiple accounts wrote to.
     *
     * Only the first account to ask inherits it: back then there was exactly
     * one account, so it is that customer's history, and a second line added
     * afterwards must start empty rather than adopt someone else's.
     */
    private fun prefs(context: Context, owner: String) =
        context.applicationContext.let { app ->
            val shared = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (owner.isBlank()) return@let shared

            val mine = app.getSharedPreferences("$PREFS.${owner.fileSafe()}", Context.MODE_PRIVATE)
            if (shared.contains(KEY_RESUME) || shared.contains(KEY_FAVOURITES)) {
                mine.edit()
                    .putString(KEY_RESUME, shared.getString(KEY_RESUME, null))
                    .putString(KEY_FAVOURITES, shared.getString(KEY_FAVOURITES, null))
                    .apply()
                shared.edit().clear().apply()
            }
            mine
        }
}

/**
 * A username becomes part of a file name, and a panel is free to hand out one
 * with a slash or a dot in it.
 */
private fun String.fileSafe() = map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")

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
