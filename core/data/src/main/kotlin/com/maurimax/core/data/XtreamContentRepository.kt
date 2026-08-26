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
import com.maurimax.core.network.XtreamApi
import com.maurimax.core.network.XtreamUrls
import com.maurimax.core.network.dto.CategoryDto
import com.maurimax.core.network.dto.EpisodeDto
import com.maurimax.core.network.dto.LiveStreamDto
import com.maurimax.core.network.dto.SeriesDto
import com.maurimax.core.network.dto.VodStreamDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

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
     * Categories already fetched this session, per tab.
     *
     * A catalogue of this size takes a few hundred requests to walk, and a
     * customer switching tabs half way through cancels the walk. Without this,
     * coming back would start it again from the first category and the tab
     * would never finish; with it, the walk resumes where it stopped and the
     * requests already paid for are not paid for twice.
     *
     * Only successful answers are kept. A category that errored is left absent
     * so the next walk retries it rather than remembering it as empty forever.
     */
    private val categoryItems = ConcurrentHashMap<String, List<MediaItem>>()

    /** The category lists themselves, which also do not change mid-session. */
    private val categoryLists = ConcurrentHashMap<CatalogTab, List<Category>>()

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
        // Every category the panel has, not a first page of them. A customer
        // paying for the whole catalogue should be able to reach the whole
        // catalogue; the rails arrive one batch at a time, so a long list costs
        // patience rather than a blank screen.
        val wanted = categoryLists[tab]
            ?: categoriesFor(tab).also { categoryLists[tab] = it }

        if (wanted.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val filled = mutableListOf<ContentRow>()
        for (batch in wanted.chunked(CONCURRENT_CATEGORIES)) {
            val loaded: List<Pair<Category, List<MediaItem>>> = coroutineScope {
                batch.map { category ->
                    async {
                        // One category failing is one missing rail, not an
                        // empty screen — panels routinely have a category that
                        // errors while the rest are fine.
                        val key = "${tab.name}/${category.id}"
                        val items = categoryItems[key]
                            ?: runCatching { itemsIn(tab, category.id) }
                                .onSuccess { categoryItems[key] = it }
                                .getOrDefault(emptyList<MediaItem>())
                        category to items
                    }
                }.awaitAll()
            }

            loaded.forEach { (category, items) ->
                // The whole category is kept, not a preview of it: the rail
                // shows the first of them and the count beside its name is the
                // rest, which is only honest if the rest is actually here.
                if (items.isNotEmpty()) filled += ContentRow(category.name, items)
            }
            emit(filled.toList())
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun categoriesFor(tab: CatalogTab): List<Category> = when (tab) {
        CatalogTab.LIVE -> liveCategories()
        CatalogTab.MOVIES -> movieCategories()
        CatalogTab.SERIES -> seriesCategories()
    }

    private suspend fun itemsIn(tab: CatalogTab, categoryId: String): List<MediaItem> = when (tab) {
        CatalogTab.LIVE -> liveChannels(categoryId).map { it.toItem() }
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
        number = channelNumber,
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

    /**
     * The episodes of one series.
     *
     * The panel serves no episodes with the series list at all — this is the
     * only call that returns anything playable for a series, and it is one
     * request per series, which is why it happens when a customer opens one
     * rather than in bulk.
     *
     * Seasons come back keyed by number as a string, and panels are careless
     * with both the key and the `season` field inside each episode: some send
     * one, some the other, some disagree with themselves. The episode's own
     * number wins when it has one, and the key is the fallback — because a
     * panel that sends the seasons as an array rather than an object leaves us
     * with the array index as the key, and an index is off by one from the
     * season it stands for.
     */
    override suspend fun seasons(item: MediaItem): List<Season> {
        if (item.kind != MediaKind.SERIES) return emptyList()

        // "series-123" is ours; the panel only knows 123.
        val seriesId = item.id.removePrefix("series-").toIntOrNull() ?: return emptyList()

        val response = withContext(Dispatchers.IO) {
            api.seriesInfo(credentials.username, credentials.password, seriesId)
        }

        return response.episodes
            .flatMap { (key, episodes) ->
                val fromKey = key.trim().toIntOrNull() ?: 0
                episodes.map { dto ->
                    dto.toModel(dto.season.takeIf { it > 0 } ?: fromKey)
                }
            }
            // Grouped here rather than trusting the panel's own grouping: a
            // panel that files every episode under one key still numbers them,
            // and this keeps a box set in seasons rather than as one list of
            // two hundred.
            .groupBy { it.season }
            .toSortedMap()
            .map { (number, episodes) ->
                Season(number = number, episodes = episodes.sortedBy { it.number })
            }
    }

    private fun EpisodeDto.toModel(seasonNumber: Int): Episode {
        val streamId = id.trim().toIntOrNull() ?: 0
        return Episode(
            id = "episode-$streamId",
            title = title,
            season = seasonNumber,
            number = episodeNum,
            plot = info?.plot.orEmpty(),
            artworkUrl = info?.image.orEmpty(),
            // Panels give seconds; everything on screen is in minutes.
            durationMinutes = (info?.durationSeconds ?: 0) / 60,
            playbackUrl = if (streamId > 0) {
                urls.episode(credentials.username, credentials.password, streamId, containerExtension)
            } else {
                ""
            },
        )
    }

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
