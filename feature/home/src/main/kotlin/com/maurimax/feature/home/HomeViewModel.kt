package com.maurimax.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maurimax.core.data.ContentRepository
import com.maurimax.core.data.Graph
import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.Credentials
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val tab: CatalogTab = CatalogTab.LIVE,
    val rows: List<ContentRow> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
) {
    val isEmpty: Boolean get() = !loading && error == null && rows.isEmpty()
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

    init {
        load(CatalogTab.LIVE)
    }

    fun selectTab(tab: CatalogTab) {
        if (tab == _uiState.value.tab && _uiState.value.error == null) return

        val cached = cache[tab]
        if (cached != null) {
            _uiState.value = HomeUiState(tab = tab, rows = cached, loading = false)
        } else {
            load(tab)
        }
    }

    fun retry() = load(_uiState.value.tab)

    private fun load(tab: CatalogTab) {
        viewModelScope.launch {
            _uiState.value = HomeUiState(tab = tab, loading = true)

            runCatching { repository.rows(tab) }
                .onSuccess { rows ->
                    cache[tab] = rows
                    _uiState.update { it.copy(rows = rows, loading = false, error = null) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = error.message ?: "Could not load ${tab.label.lowercase()}",
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
