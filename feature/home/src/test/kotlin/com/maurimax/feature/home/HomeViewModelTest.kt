package com.maurimax.feature.home

import com.maurimax.core.data.ContentRepository
import com.maurimax.core.data.FakeContentRepository
import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    /** Builds a repository from a lambda, so each test states only what it needs. */
    private fun repositoryOf(rows: (CatalogTab) -> Flow<List<ContentRow>>) =
        object : ContentRepository {
            override fun rows(tab: CatalogTab) = rows(tab)
        }

    private fun row(title: String) =
        ContentRow(title, listOf(MediaItem(id = title, title = title, kind = MediaKind.LIVE)))

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
        assertNull(state.failure)
    }

    @Test
    fun `rails show as they arrive rather than only when the tab is finished`() =
        runTest(dispatcher) {
            // The reported symptom: a spinner and nothing else. The panel is
            // asked one category at a time now, so the first rail must reach
            // the screen without waiting for the last.
            val trickle = repositoryOf {
                flow {
                    emit(listOf(row("First")))
                    delay(10_000)
                    emit(listOf(row("First"), row("Second")))
                }
            }

            val viewModel = HomeViewModel(trickle)
            testScheduler.advanceTimeBy(1)
            testScheduler.runCurrent()

            val early = viewModel.uiState.value
            assertFalse("the screen must not still be loading", early.loading)
            assertEquals(listOf("First"), early.rows.map { it.title })

            testScheduler.advanceUntilIdle()
            assertEquals(listOf("First", "Second"), viewModel.uiState.value.rows.map { it.title })
        }

    @Test
    fun `content already on screen survives a failure partway through`() = runTest(dispatcher) {
        val diesHalfway = repositoryOf {
            flow {
                emit(listOf(row("First")))
                error("portal went away")
            }
        }

        val viewModel = HomeViewModel(diesHalfway)
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        // An error page that throws away working rails is worse than stale rails.
        assertEquals(listOf("First"), state.rows.map { it.title })
        assertNull(state.failure)
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
        val counting = repositoryOf { tab ->
            calls++
            FakeContentRepository().rows(tab)
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
        val flaky = repositoryOf { tab ->
            if (fail) flow<List<ContentRow>> { error("portal unreachable") }
            else FakeContentRepository().rows(tab)
        }

        val viewModel = HomeViewModel(flaky)
        testScheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.failure)

        fail = false
        viewModel.retry()
        testScheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.failure)
        assertEquals(2, viewModel.uiState.value.rows.size)
    }

    @Test
    fun `an empty catalogue is distinguishable from a failure`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(repositoryOf { flow { emit(emptyList()) } })
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `a slow response for an abandoned tab never lands on the current one`() =
        runTest(dispatcher) {
            // The reported symptom: switching tabs showed the wrong catalogue,
            // because a slow request finished after the tab had already changed.
            val slowLive = repositoryOf { tab ->
                flow {
                    if (tab == CatalogTab.LIVE) delay(5_000)
                    emitAll(FakeContentRepository().rows(tab))
                }
            }

            val viewModel = HomeViewModel(slowLive)
            viewModel.selectTab(CatalogTab.MOVIES)
            testScheduler.advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(CatalogTab.MOVIES, state.tab)
            assertEquals("Action", state.rows.first().title)
        }

    /**
     * A catalogue this size takes hundreds of requests to walk, so leaving the
     * tab half way through is the normal case rather than the edge one. Before
     * this, the half-full result was cached and served for the rest of the
     * session: the customer saw a fraction of the catalogue and had no way to
     * ask for the rest.
     */
    @Test
    fun `a tab left half loaded finishes when it is opened again`() = runTest(dispatcher) {
        val slow = repositoryOf { tab ->
            flow {
                if (tab == CatalogTab.LIVE) {
                    emit(listOf(row("first")))
                    delay(5_000)
                    emit(listOf(row("first"), row("second")))
                } else {
                    emit(listOf(row("movies")))
                }
            }
        }

        val viewModel = HomeViewModel(slow)
        testScheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.rows.size)

        viewModel.selectTab(CatalogTab.MOVIES)
        testScheduler.advanceUntilIdle()

        viewModel.selectTab(CatalogTab.LIVE)
        testScheduler.advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.rows.size)
    }

    /**
     * A resumed load starts emitting from the first category again, so without
     * merging, a tab of two hundred rails would empty itself back to one and
     * fill up again while the customer was looking at it.
     */
    @Test
    fun `a resumed load never shrinks what is already on screen`() = runTest(dispatcher) {
        val slow = repositoryOf { tab ->
            flow {
                if (tab != CatalogTab.LIVE) {
                    emit(listOf(row("movies")))
                    return@flow
                }
                emit(listOf(row("a")))
                delay(1_000)
                emit(listOf(row("a"), row("b")))
                delay(1_000)
                emit(listOf(row("a"), row("b"), row("c")))
            }
        }

        val viewModel = HomeViewModel(slow)
        testScheduler.advanceTimeBy(1_500)
        assertEquals(2, viewModel.uiState.value.rows.size)

        viewModel.selectTab(CatalogTab.MOVIES)
        testScheduler.advanceUntilIdle()

        // Back on Live, the walk starts over at its first rail — and the two
        // already collected are still on screen while it catches up.
        viewModel.selectTab(CatalogTab.LIVE)
        testScheduler.runCurrent()
        assertEquals(2, viewModel.uiState.value.rows.size)

        testScheduler.advanceUntilIdle()
        assertEquals(3, viewModel.uiState.value.rows.size)
    }

    @Test
    fun `search stays inside the section on screen`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(FakeContentRepository())
        testScheduler.advanceUntilIdle()

        viewModel.selectTab(CatalogTab.MOVIES)
        testScheduler.advanceUntilIdle()
        viewModel.onQueryChange("n")

        val results = viewModel.uiState.value.searchResults
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.kind == MediaKind.MOVIE })
    }
}
