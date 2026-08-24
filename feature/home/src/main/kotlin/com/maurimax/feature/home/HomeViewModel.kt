package com.maurimax.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maurimax.core.data.ContentRepository
import com.maurimax.core.data.Graph
import com.maurimax.core.data.PortalFailure
import com.maurimax.core.data.toPortalFailure
import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
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
) {
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
        load(CatalogTab.LIVE)
    }

    fun selectTab(tab: CatalogTab) {
        if (tab == _uiState.value.tab && _uiState.value.failure == null) return

        val cached = cache[tab]
        if (cached != null) {
            _uiState.value = HomeUiState(tab = tab, rows = cached, loading = false)
        } else {
            load(tab)
        }
    }

    fun retry() = load(_uiState.value.tab)

    private fun load(tab: CatalogTab) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = HomeUiState(tab = tab, loading = true)

            runCatching { repository.rows(tab) }
                .onSuccess { rows ->
                    // Cancellation is not instant, so a late response still has to
                    // prove it belongs to the tab on screen before it is shown.
                    if (_uiState.value.tab != tab) return@onSuccess
                    cache[tab] = rows
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
