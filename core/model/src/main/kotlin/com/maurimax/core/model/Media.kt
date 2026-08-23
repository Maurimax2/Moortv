package com.maurimax.core.model

/** A single playable title in the MAURIMAX catalog. */
data class MediaItem(
    val id: String,
    val title: String,
    val year: Int,
    val genre: String,
    val description: String,
    /** Fallback artwork tint, as 0xAARRGGBB, shown until [artworkUrl] loads. */
    val artworkTint: Long,
    val durationMinutes: Int,
    /** 0f..1f — how far through the user is. 0f means unwatched. */
    val progress: Float = 0f,
    /** Channel logo or poster from the portal. Empty when the panel has none. */
    val artworkUrl: String = "",
)

/**
 * The three things the portal serves. Each is a top-level destination, because
 * a customer looking for a film is not browsing live channels.
 */
enum class CatalogTab(val label: String) {
    LIVE("Live TV"),
    MOVIES("Movies"),
    SERIES("Series"),
    ;

    /**
     * Live logos are wide marks on transparent backgrounds; film and series art
     * is portrait key art. Rendering one as the other looks broken, so the tab
     * carries its own shape.
     */
    val usesPortraitArt: Boolean get() = this != LIVE
}

/** A titled horizontal row of titles, e.g. "Continue watching". */
data class ContentRow(
    val title: String,
    val items: List<MediaItem>,
)
