package com.maurimax.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.maurimax.core.data.ContentRepository
import com.maurimax.core.data.FakeContentRepository
import com.maurimax.core.model.ContentRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val rows: List<ContentRow>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

/**
 * Shared by both form factors. `HomeScreenMobile` and `HomeScreenTv` are two
 * renderings of the same [HomeUiState] — there is no phone logic and no TV logic,
 * only phone layout and TV layout.
 */
class HomeViewModel(
    private val repository: ContentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            repository.homeRows()
                .catch { error ->
                    _uiState.value = HomeUiState.Error(error.message ?: "Could not load the catalog")
                }
                .collect { rows -> _uiState.value = HomeUiState.Ready(rows) }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(FakeContentRepository()) }
        }
    }
}
