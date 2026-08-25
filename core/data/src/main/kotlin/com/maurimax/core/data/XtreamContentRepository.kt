package com.maurimax.core.data

import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.Episode
import com.maurimax.core.model.Category
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.Credentials
import com.maurimax.core.model.LiveChannel
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.MediaKind
import com.maurimax.core.model.Movie
import com.maurimax.core.model.Season
import com.maurimax.core.model.Series
import com.maurimax.core.model.Sports
import com.maurimax.core.network.XtreamApi
import com.maurimax.core.network.XtreamUrls
import com.maurimax.core.network.dto.CategoryDto
import com.maurimax.core.network.dto.EpisodeDto
import com.maurimax.core.network.dto.LiveStreamDto
import com.maurimax.core.network.dto.SeriesDto
import com.maurimax.core.network.dto.VodStreamDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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

    private companion object {
        /**
         * How many category requests are in flight at once. Enough to fill the
         * screen quickly, few enough that a phone on a weak connection is not
         * fighting itself for bandwidth.
         */
        const val CONCURRENT_CATEGORIES = 4
    }

    /**
     * One rail at a time.
     *
     * The panel is asked for its categories — a small list — and then for the
     * channels of each category on its own. It is never asked for the whole
     * catalogue: `get_live_streams` without a category returns every stream on
     * the server, which here is tens of megabytes and tens of thousands of
     * objects to parse before a single tile can be drawn. That request is what
     * made the app look dead on a real connection.
     *
     * Rails are emitted as they land, so the screen starts filling in about the
     * time one small request takes rather than after all of them.
     */
    override fun rows(tab: CatalogTab): Flow<List<ContentRow>> = flow {
        val wanted = categoriesFor(tab)
            .let { all -> if (tab == CatalogTab.SPORTS) all.filterSport() else all }
            .take(maxRows)

        if (wanted.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val filled = mutableListOf<ContentRow>()
        for (batch in wanted.chunked(CONCURRENT_CATEGORIES)) {
            val loaded = coroutineScope {
                batch.map { category ->
                    async {
                        // One category failing is one missing rail, not an
                        // empty screen — panels routinely have a category that
                        // errors while the rest are fine.
                        category to runCatching { itemsIn(tab, category.id) }.getOrDefault(emptyList())
                    }
                }.awaitAll()
            }

            loaded.forEach { (category, items) ->
                if (items.isNotEmpty()) {
                    filled += ContentRow(category.name, items.take(maxItemsPerRow))
                }
            }
            emit(filled.toList())
        }
    }.flowOn(Dispatchers.IO)

    /** Majors first: somebody opening the football is looking for tonight. */
    private fun List<Category>.filterSport() =
        filter { Sports.isSport(it.name) }.sortedBy { Sports.rank(it.name) }

    private suspend fun categoriesFor(tab: CatalogTab): List<Category> = when (tab) {
        // Sport is a view of the live catalogue, not a section the panel has.
        CatalogTab.LIVE, CatalogTab.SPORTS -> liveCategories()
        CatalogTab.MOVIES -> movieCategories()
        CatalogTab.SERIES -> seriesCategories()
    }

    private suspend fun itemsIn(tab: CatalogTab, categoryId: String): List<MediaItem> = when (tab) {
        CatalogTab.LIVE, CatalogTab.SPORTS -> liveChannels(categoryId).map { it.toItem() }
        CatalogTab.MOVIES -> movies(categoryId).map { it.toItem() }
        CatalogTab.SERIES -> series(categoryId).map { it.toItem() }
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

    /**
     * Playback URLs are built here rather than in the UI, because they are the
     * one place the customer's credentials appear in a path — that stays inside
     * the data layer.
     */
    private fun LiveChannel.toItem() = MediaItem(
        id = "live-$streamId",
        title = name,
        kind = if (hasCatchUp) MediaKind.CATCH_UP else MediaKind.LIVE,
        artworkUrl = logoUrl,
        playbackUrl = urls.live(credentials.username, credentials.password, streamId),
    )

    private fun Movie.toItem() = MediaItem(
        id = "movie-$streamId",
        title = name,
        kind = MediaKind.MOVIE,
        artworkUrl = posterUrl,
        rating = rating,
        playbackUrl = urls.movie(
            credentials.username,
            credentials.password,
            streamId,
            containerExtension,
        ),
    )

    /** A series is a container: its episodes carry the streams, not the series. */
    private fun Series.toItem() = MediaItem(
        id = "series-$seriesId",
        title = name,
        kind = MediaKind.SERIES,
        artworkUrl = posterUrl,
        rating = rating,
        description = plot,
    )

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
