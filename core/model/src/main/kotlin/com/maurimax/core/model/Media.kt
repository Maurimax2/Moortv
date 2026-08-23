package com.maurimax.core.model

/**
 * What a tile is, so the UI can label it in the customer's language.
 *
 * The label is deliberately not a string here: this module is pure Kotlin with
 * no access to resources, and an English word stored in the domain would be an
 * English word on an Arabic screen.
 */
enum class MediaKind { LIVE, CATCH_UP, MOVIE, SERIES }

/** A single playable title in the MAURIMAX catalog. */
data class MediaItem(
    val id: String,
    val title: String,
    val kind: MediaKind,
    /** Fallback artwork tint, as 0xAARRGGBB, shown until [artworkUrl] loads. */
    val artworkTint: Long,
    /** Channel logo or poster from the portal. Empty when the panel has none. */
    val artworkUrl: String = "",
    /** Panel-supplied score, already formatted. Empty when the panel has none. */
    val rating: String = "",
    val description: String = "",
    val year: Int = 0,
    val durationMinutes: Int = 0,
    /** 0f..1f — how far through the customer is. 0f means unwatched. */
    val progress: Float = 0f,
)

/**
 * The three things the portal serves. Each is a top-level destination, because
 * a customer looking for a film is not browsing live channels.
 */
enum class CatalogTab {
    LIVE,
    MOVIES,
    SERIES,
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
