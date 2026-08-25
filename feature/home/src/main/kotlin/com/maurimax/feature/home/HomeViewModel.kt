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
    val failure: PortalFailure? = null,
    val query: String = "",
    /** Titles started and not finished, most recent first. Empty when none. */
    val resume: List<MediaItem> = emptyList(),
    /** Titles the customer starred. */
    val favourites: List<MediaItem> = emptyList(),
    /** Titles kept on the device, finished or still arriving. */
    val downloads: List<Download> = emptyList(),
) {

    /** Downloads that have actually landed and will play right now. */
    val playableDownloads: List<MediaItem>
        get() = downloads.filter { it.state == DownloadState.DONE }.map { it.item }

    /**
     * What the catalogue shows right now.
     *
     * Search filters the tab already in memory rather than asking the panel:
     * the whole section is loaded anyway, so results are instant and work even
     * when the panel is slow. Rows that lose every title are dropped, so an
     * empty category header never sits over nothing.
     */
    val visibleRows: List<ContentRow>
        get() {
            val q = query.trim()
            if (q.isBlank()) return rows
            return rows.mapNotNull { row ->
                val hits = row.items.filter { it.title.contains(q, ignoreCase = true) }
                if (hits.isEmpty()) null else row.copy(items = hits)
            }
        }

    /**
     * Personal rows only make sense for the tab they belong to: a film the
     * customer paused should not appear while they are browsing live channels.
     */
    fun personalFor(tab: CatalogTab): Pair<List<MediaItem>, List<MediaItem>> {
        fun matches(item: MediaItem) = when (tab) {
            CatalogTab.LIVE, CatalogTab.SPORTS -> item.isLive
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
                    CatalogTab.LIVE, CatalogTab.SPORTS -> false
                }
            }
        }

    val noResults: Boolean get() = query.isNotBlank() && visibleRows.isEmpty() && !loading
    val isEmpty: Boolean get() = !loading && failure == null && rows.isEmpty()
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

    init {
        refreshLibrary()
        load(CatalogTab.LIVE)
    }

    fun selectTab(tab: CatalogTab) {
        if (tab == _uiState.value.tab && _uiState.value.failure == null) return

        val cached = cache[tab]
        if (cached != null) {
            _uiState.value = _uiState.value.copy(
                tab = tab,
                rows = cached,
                loading = false,
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

    fun removeFromResume(item: MediaItem) {
        Graph.forgetProgress(item.id)
        refreshLibrary()
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun clearQuery() = _uiState.update { it.copy(query = "") }

    fun retry() = load(_uiState.value.tab)

    private fun load(tab: CatalogTab) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                tab = tab,
                rows = emptyList(),
                loading = true,
                failure = null,
                query = "",
            )

            runCatching { repository.rows(tab) }
                .onSuccess { rows ->
                    // Cancellation is not instant, so a late response still has to
                    // prove it belongs to the tab on screen before it is shown.
                    if (_uiState.value.tab != tab) return@onSuccess
                    cache[tab] = rows
                    // Posters from any tab will do; the sign-in wall just needs art.
                    Graph.rememberPosters(rows.flatMap { it.items }.map { it.artworkUrl })
                    _uiState.update { it.copy(rows = rows, loading = false, failure = null) }
                }
                .onFailure { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    if (_uiState.value.tab != tab) return@onFailure
                    _uiState.update {
                        it.copy(loading = false, failure = error.toPortalFailure())
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
