package com.maurimax.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maurimax.core.data.ContentRepository
import com.maurimax.core.data.Download
import com.maurimax.core.data.DownloadState
import com.maurimax.core.data.Graph
import com.maurimax.core.data.SavedItem
import com.maurimax.core.data.toSavedItem
import com.maurimax.core.data.PortalFailure
import com.maurimax.core.data.toPortalFailure
import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.MediaKind
import com.maurimax.core.model.Season
import com.maurimax.core.model.Credentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class HomeUiState(
    val tab: CatalogTab = CatalogTab.LIVE,
    val rows: List<ContentRow> = emptyList(),
    val loading: Boolean = true,
    /** Showing what was cached while the panel is asked for something newer. */
    val refreshing: Boolean = false,
    val failure: PortalFailure? = null,
    val query: String = "",
    /** Titles started and not finished, most recent first. Empty when none. */
    val resume: List<MediaItem> = emptyList(),
    /** Titles the customer starred. */
    val favourites: List<MediaItem> = emptyList(),
    /** Titles kept on the device, finished or still arriving. */
    val downloads: List<Download> = emptyList(),
    /** Episodes of the series currently open, empty when none is. */
    val seasons: List<Season> = emptyList(),
    val seasonsLoading: Boolean = false,
    val seasonsFailure: PortalFailure? = null,
) {

    /** Downloads that have actually landed and will play right now. */
    val playableDownloads: List<MediaItem>
        get() = downloads.filter { it.state == DownloadState.DONE }.map { it.item }

    /** The catalogue rails, untouched by search. */
    val visibleRows: List<ContentRow> get() = rows

    /**
     * What a search finds, as one flat list.
     *
     * Scoped to the section on screen and never across the app: someone in
     * Films is looking for a film, and a channel in those results is noise.
     * Flat rather than grouped by category, because a customer searching knows
     * the title and not which folder a reseller filed it under — which is
     * exactly how every other player on this panel behaves.
     */
    val searchResults: List<MediaItem>
        get() {
            val q = query.trim()
            if (q.isBlank()) return emptyList()
            return rows.asSequence()
                .flatMap { it.items.asSequence() }
                .filter { it.title.contains(q, ignoreCase = true) }
                .distinctBy { it.id }
                .take(MAX_RESULTS)
                .toList()
        }

    /** How many titles this section holds in total. */
    val total: Int get() = rows.sumOf { it.items.size }

    /**
     * Personal rows only make sense for the tab they belong to: a film the
     * customer paused should not appear while they are browsing live channels.
     */
    fun personalFor(tab: CatalogTab): Pair<List<MediaItem>, List<MediaItem>> {
        fun matches(item: MediaItem) = when (tab) {
            CatalogTab.LIVE -> item.isLive
            CatalogTab.MOVIES -> item.kind == MediaKind.MOVIE
            CatalogTab.SERIES -> item.kind == MediaKind.SERIES
        }
        // Resume is meaningless for a live channel, so it is films and series only.
        val resumable = if (tab.isLiveSection) emptyList() else resume.filter(::matches)
        return resumable to favourites.filter(::matches)
    }

    /** Kept titles belonging to this tab. Never live: a channel has no file. */
    fun downloadsFor(tab: CatalogTab): List<MediaItem> =
        if (tab.isLiveSection) {
            emptyList()
        } else {
            playableDownloads.filter {
                when (tab) {
                    CatalogTab.MOVIES -> it.kind == MediaKind.MOVIE
                    CatalogTab.SERIES -> it.kind == MediaKind.SERIES
                    CatalogTab.LIVE -> false
                }
            }
        }

    val searching: Boolean get() = query.isNotBlank()
    val noResults: Boolean get() = searching && searchResults.isEmpty() && !loading
    val isEmpty: Boolean get() = !loading && failure == null && rows.isEmpty()

    private companion object {
        /** A search is for finding one title, not for browsing the catalogue. */
        const val MAX_RESULTS = 200
    }
}

/**
 * Shared by both form factors. `HomeScreenMobile` and `HomeScreenTv` are two
 * renderings of the same [HomeUiState] — there is no phone logic and no TV
 * logic, only phone layout and TV layout.
 */
class HomeViewModel(
    private val repository: ContentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * A panel's full catalogue is a slow fetch, so a tab already visited is
     * restored instantly instead of refetched on every switch.
     */
    private val cache = mutableMapOf<CatalogTab, List<ContentRow>>()

    /**
     * The in-flight load. A large panel takes seconds to answer, so without
     * cancelling, a slow response for the tab you just left arrives after the
     * new one and overwrites it — which shows films under Live TV.
     */
    private var loadJob: Job? = null

    /** The in-flight episode fetch, cancelled when another series is opened. */
    private var episodesJob: Job? = null

    init {
        refreshLibrary()
        load(CatalogTab.LIVE)
    }

    fun selectTab(tab: CatalogTab) {
        if (tab == _uiState.value.tab && _uiState.value.failure == null) return

        val cached = cache[tab]
        if (cached != null) {
            // Already fetched this session: instant, and no request at all.
            loadJob?.cancel()
            _uiState.value = _uiState.value.copy(
                tab = tab,
                rows = cached,
                loading = false,
                refreshing = false,
                failure = null,
                query = "",
            )
        } else {
            load(tab)
        }
    }

    /**
     * Re-reads the on-device lists.
     *
     * Called when the screen resumes, because playback happens in another
     * activity: without this, coming back from watching something would show a
     * stale continue-watching row that does not include what was just watched.
     */
    fun refreshLibrary() {
        _uiState.update {
            it.copy(
                resume = Graph.continueWatching().map(SavedItem::toMediaItem),
                favourites = Graph.favourites().map(SavedItem::toMediaItem),
                downloads = Graph.downloads(),
            )
        }
    }

    fun toggleFavourite(item: MediaItem): Boolean {
        val nowFavourite = Graph.toggleFavourite(item.toSavedItem())
        refreshLibrary()
        return nowFavourite
    }

    fun isFavourite(item: MediaItem): Boolean = Graph.isFavourite(item.id)

    // ---- downloads --------------------------------------------------------

    fun download(item: MediaItem) {
        Graph.startDownload(item)
        refreshLibrary()
    }

    fun removeDownload(item: MediaItem) {
        Graph.removeDownload(item.id)
        refreshLibrary()
    }

    /**
     * A download that failed is a dead end otherwise: the only control on it
     * removes it, and the customer has to work out that they then have to press
     * the same square again. On the connection this app is built for, a failed
     * download is not an edge case.
     */
    fun retryDownload(item: MediaItem) {
        Graph.removeDownload(item.id)
        Graph.startDownload(item)
        refreshLibrary()
    }

    fun removeFromResume(item: MediaItem) {
        Graph.forgetProgress(item.id)
        refreshLibrary()
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun clearQuery() = _uiState.update { it.copy(query = "") }

    fun retry() = load(_uiState.value.tab)

    // ---- episodes ---------------------------------------------------------

    /**
     * Loads the episodes of a series.
     *
     * Only on open, never in bulk: the panel serves no episodes with the series
     * list, so this is one request per series and asking for all of them up
     * front would be hundreds of requests for a screen showing twenty posters.
     */
    fun openSeries(item: MediaItem) {
        episodesJob?.cancel()
        if (item.kind != MediaKind.SERIES) {
            _uiState.update { it.copy(seasons = emptyList(), seasonsLoading = false, seasonsFailure = null) }
            return
        }

        episodesJob = viewModelScope.launch {
            _uiState.update { it.copy(seasons = emptyList(), seasonsLoading = true, seasonsFailure = null) }

            runCatching { repository.seasons(item) }
                .onSuccess { seasons ->
                    _uiState.update { it.copy(seasons = seasons, seasonsLoading = false) }
                }
                .onFailure { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    _uiState.update {
                        it.copy(seasonsLoading = false, seasonsFailure = error.toPortalFailure())
                    }
                }
        }
    }

    /** Leaves the series page, so a stale episode list cannot flash on the next one. */
    fun closeSeries() {
        episodesJob?.cancel()
        _uiState.update { it.copy(seasons = emptyList(), seasonsLoading = false, seasonsFailure = null) }
    }

    /**
     * Fills a tab.
     *
     * Three things happen in order, and the customer sees the first one
     * immediately: whatever this account saw last time is drawn from disk, then
     * each rail replaces it as the panel answers, then the finished tab is
     * written back for next launch. A returning customer never waits on a blank
     * screen, and a slow panel costs freshness rather than usefulness.
     */
    private fun load(tab: CatalogTab) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val remembered = cache[tab] ?: Graph.cachedRows(tab)

            _uiState.value = _uiState.value.copy(
                tab = tab,
                rows = remembered,
                loading = remembered.isEmpty(),
                refreshing = remembered.isNotEmpty(),
                failure = null,
                query = "",
            )

            try {
                repository.rows(tab).collect { rows ->
                    // Cancellation is not instant, so a late emission still has
                    // to prove it belongs to the tab on screen.
                    if (_uiState.value.tab != tab) return@collect
                    cache[tab] = rows
                    // Posters from any tab will do; the sign-in wall just needs art.
                    Graph.rememberPosters(rows.flatMap { it.items }.map { it.artworkUrl })
                    _uiState.update { it.copy(rows = rows, loading = false, failure = null) }
                }
                if (_uiState.value.tab == tab) {
                    _uiState.update { it.copy(refreshing = false) }
                    Graph.cacheRows(tab, _uiState.value.rows)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (_uiState.value.tab != tab) return@launch
                _uiState.update {
                    // Stale rails beat an error page: the customer can still
                    // open what they were watching yesterday.
                    it.copy(
                        loading = false,
                        refreshing = false,
                        failure = error.toPortalFailure().takeIf { _ -> it.rows.isEmpty() },
                    )
                }
            }
        }
    }

    companion object {
        /** Builds a home screen bound to the signed-in customer's catalogue. */
        fun factory(credentials: Credentials): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(Graph.contentRepository(credentials)) }
        }
    }
}
