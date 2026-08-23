package com.maurimax.feature.home

import com.maurimax.core.data.ContentRepository
import com.maurimax.core.data.FakeContentRepository
import com.maurimax.core.model.ContentRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `starts in loading before the repository emits`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeContentRepository())

        assertTrue(viewModel.uiState.value is HomeUiState.Loading)
    }

    @Test
    fun `exposes the rows the repository emits`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeContentRepository())

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Ready)
        assertEquals(3, (state as HomeUiState.Ready).rows.size)
        assertEquals("Continue watching", state.rows.first().title)
    }

    @Test
    fun `surfaces a failing repository as an error state`() = runTest(dispatcher) {
        val failing = object : ContentRepository {
            override fun homeRows(): Flow<List<ContentRow>> = flow { error("catalog offline") }
        }

        val viewModel = HomeViewModel(failing)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is HomeUiState.Error)
        assertEquals("catalog offline", (state as HomeUiState.Error).message)
    }
}
