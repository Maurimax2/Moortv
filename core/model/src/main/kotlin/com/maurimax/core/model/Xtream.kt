package com.maurimax.core.model

/** A live TV channel from the portal. */
data class LiveChannel(
    val streamId: Int,
    val name: String,
    val logoUrl: String,
    val categoryId: String,
    val epgChannelId: String,
    val hasCatchUp: Boolean,
    val channelNumber: Int,
)

/** A film. [containerExtension] decides the playback URL, so it travels with the item. */
data class Movie(
    val streamId: Int,
    val name: String,
    val posterUrl: String,
    val categoryId: String,
    val containerExtension: String,
    val rating: String,
)

data class Series(
    val seriesId: Int,
    val name: String,
    val posterUrl: String,
    val categoryId: String,
    val plot: String,
    val rating: String,
)

/** A live/VOD/series grouping as the panel defines it. */
data class Category(
    val id: String,
    val name: String,
)

/** What the portal says about the signed-in account. */
data class Account(
    val username: String,
    val status: String,
    val expiresAtEpochSeconds: Long?,
    val isTrial: Boolean,
    val activeConnections: Int,
    val maxConnections: Int,
) {
    val isActive: Boolean get() = status.equals("Active", ignoreCase = true)
}

/** Stored credentials for the one portal this app is built against. */
data class Credentials(
    val username: String,
    val password: String,
)
