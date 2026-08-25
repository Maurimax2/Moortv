package com.maurimax.core.data

import android.content.Context
import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.MediaKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
private data class CachedItem(
    val id: String,
    val title: String,
    val kind: String,
    val artworkUrl: String = "",
    val rating: String = "",
    val description: String = "",
    val year: Int = 0,
    val durationMinutes: Int = 0,
    val playbackUrl: String = "",
)

@Serializable
private data class CachedRow(val title: String, val items: List<CachedItem>)

/**
 * The last catalogue this account saw, kept on the device.
 *
 * A returning customer should never look at an empty screen while the panel
 * thinks about it. The rails they saw last time are drawn immediately and
 * replaced as the live ones arrive — so the app is useful in the first frame
 * and merely more current a few seconds later.
 *
 * Per account, because two lines on one box are two different catalogues.
 */
object CatalogCache {

    private const val PREFS = "maurimax.catalog"
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(context: Context, owner: String, tab: CatalogTab): List<ContentRow> {
        val raw = prefs(context, owner).getString(tab.name, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(CachedRow.serializer()), raw)
                .map { row -> ContentRow(row.title, row.items.map(CachedItem::toMediaItem)) }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, owner: String, tab: CatalogTab, rows: List<ContentRow>) {
        if (rows.isEmpty()) return
        val cached = rows.map { row -> CachedRow(row.title, row.items.map(MediaItem::toCached)) }
        prefs(context, owner).edit()
            .putString(tab.name, json.encodeToString(ListSerializer(CachedRow.serializer()), cached))
            .apply()
    }

    /** Removing an account takes its catalogue with it. */
    fun erase(context: Context, owner: String) {
        prefs(context, owner).edit().clear().apply()
    }

    private fun prefs(context: Context, owner: String) = context.applicationContext
        .getSharedPreferences("$PREFS.${owner.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")}", Context.MODE_PRIVATE)
}

private fun MediaItem.toCached() = CachedItem(
    id = id,
    title = title,
    kind = kind.name,
    artworkUrl = artworkUrl,
    rating = rating,
    description = description,
    year = year,
    durationMinutes = durationMinutes,
    playbackUrl = playbackUrl,
)

private fun CachedItem.toMediaItem() = MediaItem(
    id = id,
    title = title,
    kind = runCatching { MediaKind.valueOf(kind) }.getOrDefault(MediaKind.LIVE),
    artworkUrl = artworkUrl,
    rating = rating,
    description = description,
    year = year,
    durationMinutes = durationMinutes,
    playbackUrl = playbackUrl,
)
