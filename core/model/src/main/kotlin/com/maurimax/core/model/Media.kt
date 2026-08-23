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

/** A titled horizontal row of titles, e.g. "Continue watching". */
data class ContentRow(
    val title: String,
    val items: List<MediaItem>,
)
