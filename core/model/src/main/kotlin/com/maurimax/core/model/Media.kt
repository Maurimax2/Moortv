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
    /** Channel logo or poster from the portal. Empty when the panel has none. */
    val artworkUrl: String = "",
    /** Panel-supplied score, already formatted. Empty when the panel has none. */
    val rating: String = "",
    val description: String = "",
    val year: Int = 0,
    val durationMinutes: Int = 0,
    /** 0f..1f — how far through the customer is. 0f means unwatched. */
    val progress: Float = 0f,
    /**
     * Where this plays from. Empty for a series, which is a container rather
     * than a stream — its episodes each have their own URL.
     */
    val playbackUrl: String = "",
) {
    val isPlayable: Boolean get() = playbackUrl.isNotBlank()
    val isLive: Boolean get() = kind == MediaKind.LIVE || kind == MediaKind.CATCH_UP
}

/**
 * The three things the portal serves. Each is a top-level destination, because
 * a customer looking for a film is not browsing live channels.
 */
enum class CatalogTab {
    LIVE,

    /**
     * Football and the rest of it, pulled out of the live catalogue.
     *
     * Not a fourth thing the portal serves — the panel has no genres — but the
     * thing most of this audience opens the app for, and finding it inside two
     * hundred alphabetical categories is not finding it.
     */
    SPORTS,
    MOVIES,
    SERIES,
    ;

    /** Sport is live channels, so it reads from the same section of the panel. */
    val isLiveSection: Boolean get() = this == LIVE || this == SPORTS

    /**
     * Live logos are wide marks on transparent backgrounds; film and series art
     * is portrait key art. Rendering one as the other looks broken, so the tab
     * carries its own shape.
     */
    val usesPortraitArt: Boolean get() = !isLiveSection
}

/** A titled horizontal row of titles, e.g. "Continue watching". */
data class ContentRow(
    val title: String,
    val items: List<MediaItem>,
)

/** One episode of a series. Unlike the series itself, this actually plays. */
data class Episode(
    val id: String,
    val title: String,
    val season: Int,
    val number: Int,
    val plot: String = "",
    val artworkUrl: String = "",
    val durationMinutes: Int = 0,
    val playbackUrl: String = "",
)

/** A season and its episodes, in the order they should be watched. */
data class Season(
    val number: Int,
    val episodes: List<Episode>,
)

/**
 * An episode as the rest of the app sees it.
 *
 * Everything downstream — the player, continue-watching, downloads — is built
 * around [MediaItem], and an episode is a playable title like any other. Making
 * it one here means none of that needed a second code path.
 */
fun Episode.toMediaItem() = MediaItem(
    id = id,
    title = title.ifBlank { "S${season}E$number" },
    kind = MediaKind.SERIES,
    artworkUrl = artworkUrl,
    description = plot,
    durationMinutes = durationMinutes,
    playbackUrl = playbackUrl,
)
