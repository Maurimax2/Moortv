package com.maurimax.core.data

import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.Category
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.Credentials
import com.maurimax.core.model.LiveChannel
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.MediaKind
import com.maurimax.core.model.Movie
import com.maurimax.core.model.Series
import com.maurimax.core.network.XtreamApi
import com.maurimax.core.network.XtreamUrls
import com.maurimax.core.network.dto.CategoryDto
import com.maurimax.core.network.dto.LiveStreamDto
import com.maurimax.core.network.dto.SeriesDto
import com.maurimax.core.network.dto.VodStreamDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Reads the catalog from the portal for the signed-in customer.
 *
 * Categories come back as a flat list and streams come back all at once, so
 * rows are assembled client-side rather than with a request per row.
 */
class XtreamContentRepository(
    private val api: XtreamApi,
    private val urls: XtreamUrls,
    private val credentials: Credentials,
    /** Rows on the home screen. A panel can expose hundreds of categories. */
    private val maxRows: Int = 12,
    /** Items per row. The rail is horizontally scrollable, not endless. */
    private val maxItemsPerRow: Int = 20,
) : ContentRepository {

    override suspend fun rows(tab: CatalogTab): List<ContentRow> = withContext(Dispatchers.IO) {
        when (tab) {
            CatalogTab.LIVE -> sections(
                categories = { liveCategories() },
                items = { liveChannels() },
                categoryOf = LiveChannel::categoryId,
                toMediaItem = LiveChannel::toMediaItem,
            )

            CatalogTab.MOVIES -> sections(
                categories = { movieCategories() },
                items = { movies() },
                categoryOf = Movie::categoryId,
                toMediaItem = Movie::toMediaItem,
            )

            CatalogTab.SERIES -> sections(
                categories = { seriesCategories() },
                items = { series() },
                categoryOf = Series::categoryId,
                toMediaItem = Series::toMediaItem,
            )
        }
    }

    suspend fun liveCategories(): List<Category> =
        api.liveCategories(credentials.username, credentials.password).map(CategoryDto::toModel)

    suspend fun liveChannels(categoryId: String? = null): List<LiveChannel> =
        api.liveStreams(credentials.username, credentials.password, categoryId)
            .map(LiveStreamDto::toModel)

    suspend fun movieCategories(): List<Category> =
        api.vodCategories(credentials.username, credentials.password).map(CategoryDto::toModel)

    suspend fun movies(categoryId: String? = null): List<Movie> =
        api.vodStreams(credentials.username, credentials.password, categoryId)
            .map(VodStreamDto::toModel)

    suspend fun seriesCategories(): List<Category> =
        api.seriesCategories(credentials.username, credentials.password).map(CategoryDto::toModel)

    suspend fun series(categoryId: String? = null): List<Series> =
        api.series(credentials.username, credentials.password, categoryId)
            .map(SeriesDto::toModel)

    /** The playable URL for a channel. */
    fun streamUrl(channel: LiveChannel): String =
        urls.live(credentials.username, credentials.password, channel.streamId)

    fun streamUrl(movie: Movie): String =
        urls.movie(credentials.username, credentials.password, movie.streamId, movie.containerExtension)

    /**
     * Categories and items arrive as two flat lists, so rows are assembled here
     * rather than with a request per category. Both fetches run concurrently:
     * on a large panel the item list is the slow one.
     */
    private suspend fun <T> sections(
        categories: suspend () -> List<Category>,
        items: suspend () -> List<T>,
        categoryOf: (T) -> String,
        toMediaItem: (T) -> MediaItem,
    ): List<ContentRow> = coroutineScope {
        val categoriesJob = async { categories() }
        val itemsJob = async { items() }

        val grouped = itemsJob.await().groupBy(categoryOf)

        categoriesJob.await()
            .asSequence()
            .mapNotNull { category ->
                val entries = grouped[category.id].orEmpty()
                if (entries.isEmpty()) {
                    null
                } else {
                    ContentRow(
                        title = category.name,
                        items = entries.take(maxItemsPerRow).map(toMediaItem),
                    )
                }
            }
            .take(maxRows)
            .toList()
    }
}

internal fun CategoryDto.toModel() = Category(id = id, name = name)

internal fun LiveStreamDto.toModel() = LiveChannel(
    streamId = streamId,
    name = name,
    logoUrl = icon,
    categoryId = categoryId,
    epgChannelId = epgChannelId,
    hasCatchUp = hasArchive == 1,
    channelNumber = num,
)

internal fun VodStreamDto.toModel() = Movie(
    streamId = streamId,
    name = name,
    posterUrl = icon,
    categoryId = categoryId,
    containerExtension = containerExtension,
    rating = rating,
)

internal fun SeriesDto.toModel() = Series(
    seriesId = seriesId,
    name = name,
    posterUrl = cover,
    categoryId = categoryId,
    plot = plot,
    rating = rating,
)

internal fun Movie.toMediaItem() = MediaItem(
    id = "movie-$streamId",
    title = name,
    kind = MediaKind.MOVIE,
    artworkUrl = posterUrl,
    rating = rating,
)

internal fun Series.toMediaItem() = MediaItem(
    id = "series-$seriesId",
    title = name,
    kind = MediaKind.SERIES,
    artworkUrl = posterUrl,
    rating = rating,
    description = plot,
)

/** Live channels have no year, rating or runtime; the kind carries the label. */
internal fun LiveChannel.toMediaItem() = MediaItem(
    id = "live-$streamId",
    title = name,
    kind = if (hasCatchUp) MediaKind.CATCH_UP else MediaKind.LIVE,
    artworkUrl = logoUrl,
)

