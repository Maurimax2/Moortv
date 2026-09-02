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
import kotlinx.coroutines.flow.channelFlow
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

        /**
         * How many categories are worth fetching one at a time while the whole
         * catalogue is on its way. Two screenfuls: enough that the customer has
         * something to look at and to start moving through, few enough that the
         * requests thrown away when the bulk answer lands are not worth counting.
         */
        const val HEAD_START = 8
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
     * Tabs whose last walk hit a category the panel would not answer for.
     *
     * The rail is left out rather than shown empty, which is right — but the
     * caller has to know the difference between a tab that finished and a tab
     * that finished with holes in it, or the holes become permanent.
     */
    private val incomplete = ConcurrentHashMap.newKeySet<CatalogTab>()

    /**
     * The catalogue for a tab.
     *
     * The panel will answer two different ways and this asks it both at once,
     * because each is bad at what the other is good at.
     *
     * Per category is one small request per rail. The first tiles land in about
     * the time one request takes, and the last ones land in about the time two
     * hundred of them take: on a connection with a third of a second of latency
     * that is minutes, four at a time, and a customer who leaves before it
     * finishes has genuinely never seen most of what they pay for.
     *
     * In bulk is one request for every stream on the account. It is megabytes
     * and tens of thousands of objects, so nothing at all can be drawn until it
     * lands — but when it lands, the tab is finished, in one round trip instead
     * of two hundred.
     *
     * So: the bulk request goes first and runs the whole time, a handful of
     * categories are fetched individually to put something on screen while it
     * travels, and whatever the bulk request returns replaces all of it and
     * completes the tab. The individual head start is thrown away, which costs
     * a few requests and buys the customer a screen that is never blank.
     */
    override fun rows(tab: CatalogTab): Flow<List<ContentRow>> = channelFlow {
        // A fresh walk starts whole until something goes wrong in it.
        incomplete -= tab

        // Every category the panel has, not a first page of them. A customer
        // paying for the whole catalogue should be able to reach the whole
        // catalogue.
        val wanted = categoryLists[tab]
            ?: categoriesFor(tab).also { categoryLists[tab] = it }

        if (wanted.isEmpty()) {
            send(emptyList())
            return@channelFlow
        }

        // Started before anything else, because it is the long pole and every
        // request below it is filling time while it travels.
        val whole = async { runCatching { everythingIn(tab) } }

        val filled = mutableListOf<ContentRow>()
        val fetched = mutableSetOf<String>()

        // Something on screen quickly. Stops as soon as the bulk answer is in,
        // since anything fetched after that point is a request thrown away.
        for (batch in wanted.take(HEAD_START).chunked(CONCURRENT_CATEGORIES)) {
            if (whole.isCompleted) break
            appendCategories(tab, batch, filled, fetched)
            send(filled.toList())
        }

        val grouped = whole.await().getOrNull()
        if (grouped != null) {
            // Whole, whatever the head start ran into: a category that refused a
            // request of its own is still in here.
            incomplete -= tab
            // One pass over the answer, in the panel's own category order so the
            // rails do not rearrange themselves under the customer.
            val rows = mutableListOf<ContentRow>()
            for (category in wanted) {
                val items = grouped[category.id].orEmpty()
                if (items.isEmpty()) continue
                categoryItems[key(tab, category.id)] = items
                rows += ContentRow(category.name, items)
            }
            send(rows)
            return@channelFlow
        }

        // The panel would not hand over the whole catalogue — some will not, and
        // some time out on a request that size. Back to one rail at a time, from
        // wherever the head start reached.
        for (batch in wanted.filterNot { it.id in fetched }.chunked(CONCURRENT_CATEGORIES)) {
            appendCategories(tab, batch, filled, fetched)
            send(filled.toList())
        }
    }.flowOn(Dispatchers.IO)

    /** Fetches one batch of categories at once and appends the rails they hold. */
    private suspend fun appendCategories(
        tab: CatalogTab,
        batch: List<Category>,
        into: MutableList<ContentRow>,
        fetched: MutableSet<String>,
    ) {
        val loaded: List<Pair<Category, List<MediaItem>>> = coroutineScope {
            batch.map { category ->
                async {
                    // One category failing is one missing rail, not an empty
                    // screen — panels routinely have a category that errors
                    // while the rest are fine.
                    val items = categoryItems[key(tab, category.id)]
                        ?: runCatching { itemsIn(tab, category.id) }
                            .onSuccess { categoryItems[key(tab, category.id)] = it }
                            .onFailure { incomplete += tab }
                            .getOrDefault(emptyList<MediaItem>())
                    category to items
                }
            }.awaitAll()
        }

        loaded.forEach { (category, items) ->
            fetched += category.id
            // The whole category is kept, not a preview of it: the rail shows
            // the first of them and the count beside its name is the rest,
            // which is only honest if the rest is actually here.
            if (items.isNotEmpty()) into += ContentRow(category.name, items)
        }
    }

    /**
     * Every stream on the account for this tab, filed under its category id.
     *
     * The panel is free to file a stream under a category it never listed, and
     * those are dropped rather than shown in a rail with no name.
     */
    private suspend fun everythingIn(tab: CatalogTab): Map<String, List<MediaItem>> = when (tab) {
        CatalogTab.LIVE -> liveChannels().groupBy(LiveChannel::categoryId)
            .mapValues { (_, channels) -> channels.map { it.toItem() } }
        CatalogTab.MOVIES -> movies().groupBy(Movie::categoryId)
            .mapValues { (_, films) -> films.map { it.toItem() } }
        CatalogTab.SERIES -> series().groupBy(Series::categoryId)
            .mapValues { (_, shows) -> shows.map { it.toItem() } }
    }

    private fun key(tab: CatalogTab, categoryId: String) = "${tab.name}/$categoryId"

    /** False once any category of this tab has failed since the walk began. */
    override fun wasComplete(tab: CatalogTab): Boolean = tab !in incomplete

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
