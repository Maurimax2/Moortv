package com.maurimax.feature.home

import com.maurimax.core.data.ContentRepository
import com.maurimax.core.data.FakeContentRepository
import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun `starts on live tv in a loading state`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeContentRepository())

        assertEquals(CatalogTab.LIVE, viewModel.uiState.value.tab)
        assertTrue(viewModel.uiState.value.loading)
    }

    @Test
    fun `exposes the rows the repository returns`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeContentRepository())
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.rows.size)
        assertEquals("Entertainment", state.rows.first().title)
        assertNull(state.error)
    }

    @Test
    fun `switching tab loads that tab's catalogue`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeContentRepository())
        testScheduler.advanceUntilIdle()

        viewModel.selectTab(CatalogTab.MOVIES)
        testScheduler.advanceUntilIdle()

        assertEquals(CatalogTab.MOVIES, viewModel.uiState.value.tab)
        assertEquals("Action", viewModel.uiState.value.rows.first().title)
    }

    @Test
    fun `a revisited tab is served from cache without refetching`() = runTest(dispatcher) {
        var calls = 0
        val counting = object : ContentRepository {
            override suspend fun rows(tab: CatalogTab): List<ContentRow> {
                calls++
                return FakeContentRepository().rows(tab)
            }
        }

        val viewModel = HomeViewModel(counting)
        testScheduler.advanceUntilIdle()
        viewModel.selectTab(CatalogTab.MOVIES)
        testScheduler.advanceUntilIdle()
        viewModel.selectTab(CatalogTab.LIVE)
        testScheduler.advanceUntilIdle()

        assertEquals(2, calls)
        assertEquals(CatalogTab.LIVE, viewModel.uiState.value.tab)
    }

    @Test
    fun `a failing repository surfaces an error and can be retried`() = runTest(dispatcher) {
        var fail = true
        val flaky = object : ContentRepository {
            override suspend fun rows(tab: CatalogTab): List<ContentRow> {
                if (fail) error("portal unreachable")
                return FakeContentRepository().rows(tab)
            }
        }

        val viewModel = HomeViewModel(flaky)
        testScheduler.advanceUntilIdle()
        assertEquals("portal unreachable", viewModel.uiState.value.error)

        fail = false
        viewModel.retry()
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
        assertEquals(2, viewModel.uiState.value.rows.size)
    }

    @Test
    fun `an empty catalogue is distinguishable from a failure`() = runTest(dispatcher) {
        val empty = object : ContentRepository {
            override suspend fun rows(tab: CatalogTab) = emptyList<ContentRow>()
        }

        val viewModel = HomeViewModel(empty)
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertNull(viewModel.uiState.value.error)
    }
}
