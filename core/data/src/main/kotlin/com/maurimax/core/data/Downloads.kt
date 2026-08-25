package com.maurimax.core.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.MediaKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/** What a downloaded title is doing right now. */
enum class DownloadState { QUEUED, RUNNING, DONE, FAILED }

/** A title being kept on the device, and how far along it is. */
data class Download(
    val item: MediaItem,
    val state: DownloadState,
    /** 0f..1f. Zero until the server says how big the file is, which is normal. */
    val progress: Float,
    val bytes: Long,
    val totalBytes: Long,
    /** Where it landed. Empty until the download finishes. */
    val fileUri: String,
)

@Serializable
private data class DownloadRecord(
    val id: String,
    val title: String,
    val kind: String,
    val artworkUrl: String = "",
    val sourceUrl: String = "",
    val rating: String = "",
    val description: String = "",
    /** The system DownloadManager's own id, which is how progress is read. */
    val systemId: Long = 0,
    val addedAt: Long = 0,
)

/**
 * Keeping a film on the device.
 *
 * Built on the platform's DownloadManager rather than on a service of our own.
 * Films on this panel are ordinary files served over HTTP, and the system
 * downloader already survives the app being killed, the screen going off, a
 * change of network and a reboot — all of which matter far more here than
 * anywhere else, because a customer downloading a two-gigabyte film in
 * Nouakchott is doing it precisely because their connection is not dependable.
 *
 * Live channels are never downloadable: there is no file, only a stream that
 * does not end.
 */
object Downloads {

    private const val PREFS = "maurimax.downloads"
    private const val KEY_ITEMS = "items"
    private const val FOLDER = "maurimax"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** True when this is the sort of thing that can be kept at all. */
    fun canDownload(item: MediaItem): Boolean = item.isPlayable && !item.isLive

    fun isDownloaded(context: Context, owner: String, itemId: String): Boolean =
        all(context, owner).any { it.item.id == itemId && it.state == DownloadState.DONE }

    /**
     * The file on this device, or null. Playback prefers it: once a title is
     * downloaded, watching it should cost nothing and work with the aeroplane
     * mode on.
     */
    fun localUrl(context: Context, owner: String, itemId: String): String? =
        all(context, owner)
            .firstOrNull { it.item.id == itemId && it.state == DownloadState.DONE }
            ?.fileUri
            ?.takeIf { it.isNotBlank() }

    /** Starts a download, or does nothing if this title is already being kept. */
    fun start(context: Context, owner: String, item: MediaItem) {
        if (!canDownload(item)) return
        if (records(context, owner).any { it.id == item.id }) return

        val app = context.applicationContext
        val manager = app.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return

        val request = DownloadManager.Request(Uri.parse(item.playbackUrl))
            .setTitle(item.title)
            .setDescription(MAURIMAX)
            // App-specific external storage: no permission to ask for, and the
            // files leave with the app rather than outliving it in the gallery.
            .setDestinationInExternalFilesDir(
                app,
                Environment.DIRECTORY_MOVIES,
                "$FOLDER/${fileNameFor(owner, item)}",
            )
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val systemId = runCatching { manager.enqueue(request) }.getOrNull() ?: return

        write(
            app,
            owner,
            records(app, owner) + DownloadRecord(
                id = item.id,
                title = item.title,
                kind = item.kind.name,
                artworkUrl = item.artworkUrl,
                sourceUrl = item.playbackUrl,
                rating = item.rating,
                description = item.description,
                systemId = systemId,
                addedAt = System.currentTimeMillis(),
            ),
        )
    }

    /** Stops a download if it is running, and deletes the file if it is not. */
    fun remove(context: Context, owner: String, itemId: String) {
        val app = context.applicationContext
        val record = records(app, owner).firstOrNull { it.id == itemId } ?: return

        val manager = app.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        // Removing from the system downloader deletes the partial file too.
        runCatching { manager?.remove(record.systemId) }

        // A finished download is no longer the downloader's business, so its
        // file has to go explicitly.
        localFile(app, owner, record)?.let { file -> runCatching { file.delete() } }

        write(app, owner, records(app, owner).filterNot { it.id == itemId })
    }

    /** Everything this account is keeping, newest first. */
    fun all(context: Context, owner: String): List<Download> {
        val app = context.applicationContext
        val stored = records(app, owner)
        if (stored.isEmpty()) return emptyList()

        val manager = app.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        return stored.sortedByDescending { it.addedAt }.map { record ->
            progressOf(app, owner, manager, record)
        }
    }

    // ---- the system downloader --------------------------------------------

    private fun progressOf(
        context: Context,
        owner: String,
        manager: DownloadManager?,
        record: DownloadRecord,
    ): Download {
        val query = DownloadManager.Query().setFilterById(record.systemId)
        val cursor = runCatching { manager?.query(query) }.getOrNull()

        var state = DownloadState.QUEUED
        var soFar = 0L
        var total = 0L
        var uri = ""

        cursor?.use {
            if (it.moveToFirst()) {
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                soFar = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                uri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)).orEmpty()

                state = when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadState.DONE
                    DownloadManager.STATUS_FAILED -> DownloadState.FAILED
                    DownloadManager.STATUS_RUNNING -> DownloadState.RUNNING
                    else -> DownloadState.QUEUED
                }
            }
        }

        // The system downloader forgets entries eventually. A file still on
        // disk is a finished download regardless of what it remembers.
        if (uri.isBlank()) {
            val file = localFile(context, owner, record)
            if (file != null && file.length() > 0) {
                state = DownloadState.DONE
                uri = Uri.fromFile(file).toString()
                soFar = file.length()
                total = file.length()
            } else if (cursor == null) {
                state = DownloadState.FAILED
            }
        }

        return Download(
            item = record.toMediaItem(),
            state = state,
            progress = if (total > 0) (soFar.toFloat() / total).coerceIn(0f, 1f) else 0f,
            bytes = soFar,
            totalBytes = total,
            fileUri = if (state == DownloadState.DONE) uri else "",
        )
    }

    private fun localFile(context: Context, owner: String, record: DownloadRecord): File? {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: return null
        return File(File(dir, FOLDER), fileNameFor(owner, record.id, record.sourceUrl))
    }

    /**
     * Named by account and title id, so two lines downloading the same film
     * each keep their own copy rather than one deleting the other's.
     */
    private fun fileNameFor(owner: String, item: MediaItem): String =
        fileNameFor(owner, item.id, item.playbackUrl)

    private fun fileNameFor(owner: String, itemId: String, sourceUrl: String): String {
        val extension = sourceUrl.substringAfterLast('.', "").take(5).ifBlank { "mp4" }
        return "${safe(owner)}-${safe(itemId)}.${safe(extension)}"
    }

    private fun safe(value: String) =
        value.map { if (it.isLetterOrDigit()) it else '_' }.joinToString("")

    // ---- storage ----------------------------------------------------------

    private fun records(context: Context, owner: String): List<DownloadRecord> {
        val raw = prefs(context, owner).getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(DownloadRecord.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun write(context: Context, owner: String, items: List<DownloadRecord>) {
        prefs(context, owner).edit()
            .putString(KEY_ITEMS, json.encodeToString(ListSerializer(DownloadRecord.serializer()), items))
            .apply()
    }

    private fun prefs(context: Context, owner: String) = context.applicationContext
        .getSharedPreferences("$PREFS.${safe(owner)}", Context.MODE_PRIVATE)

    private fun DownloadRecord.toMediaItem() = MediaItem(
        id = id,
        title = title,
        kind = runCatching { MediaKind.valueOf(kind) }.getOrDefault(MediaKind.MOVIE),
        artworkUrl = artworkUrl,
        rating = rating,
        description = description,
        playbackUrl = sourceUrl,
    )

    private const val MAURIMAX = "MAURIMAX"
}
