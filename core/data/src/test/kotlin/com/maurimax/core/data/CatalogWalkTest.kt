package com.maurimax.core.data

import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.Credentials
import com.maurimax.core.network.XtreamApi
import com.maurimax.core.network.XtreamUrls
import com.maurimax.core.network.dto.CategoryDto
import com.maurimax.core.network.dto.LiveStreamDto
import com.maurimax.core.network.dto.PlayerApiResponse
import com.maurimax.core.network.dto.SeriesDto
import com.maurimax.core.network.dto.SeriesInfoResponse
import com.maurimax.core.network.dto.VodStreamDto
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * How the catalogue is fetched.
 *
 * The reported symptom was a tab that took minutes and still did not hold
 * everything. A category at a time is two hundred round trips on a connection
 * that charges a third of a second for each one, so the customer left before
 * the walk finished and never saw most of what they were paying for. The whole
 * catalogue in one request finishes the tab in one round trip; the per-category
 * walk stays as the head start and as the fallback for a panel that will not
 * serve it.
 */
class CatalogWalkTest {

    /**
     * Counts what the panel was actually asked for.
     *
     * Thread safe on purpose: the repository asks for four categories at once,
     * on the IO dispatcher, so a plain list here loses entries and the test
     * fails on a schedule of its own rather than on the code.
     */
    private class CountingApi(
        private val categories: List<CategoryDto>,
        private val streams: List<LiveStreamDto>,
        private val bulkFails: Boolean = false,
    ) : XtreamApi {
        private val bulk = AtomicInteger(0)
        val bulkCalls: Int get() = bulk.get()

        private val asked = ConcurrentLinkedQueue<String>()
        val perCategoryCalls: List<String> get() = asked.toList()

        override suspend fun login(username: String, password: String): PlayerApiResponse =
            throw IOException("not used")

        override suspend fun liveCategories(username: String, password: String) = categories

        override suspend fun liveStreams(
            username: String,
            password: String,
            categoryId: String?,
        ): List<LiveStreamDto> {
            if (categoryId == null) {
                bulk.incrementAndGet()
                if (bulkFails) throw IOException("too big for this panel")
                return streams
            }
            asked += categoryId
            return streams.filter { it.categoryId == categoryId }
        }

        override suspend fun vodCategories(username: String, password: String) = emptyList<CategoryDto>()
        override suspend fun vodStreams(username: String, password: String, categoryId: String?) = emptyList<VodStreamDto>()
        override suspend fun seriesCategories(username: String, password: String) = emptyList<CategoryDto>()
        override suspend fun series(username: String, password: String, categoryId: String?) = emptyList<SeriesDto>()
        override suspend fun seriesInfo(username: String, password: String, seriesId: Int) =
            SeriesInfoResponse()
    }

    private fun repository(api: XtreamApi) = XtreamContentRepository(
        api = api,
        urls = XtreamUrls("http://panel.example"),
        credentials = Credentials("user", "pass"),
    )

    /** Twenty categories, three channels in each. */
    private fun categories(count: Int) =
        (1..count).map { CategoryDto(id = "c$it", name = "Category $it") }

    private fun streams(categoryCount: Int) =
        (1..categoryCount).flatMap { c ->
            (1..3).map { n ->
                LiveStreamDto(
                    streamId = c * 100 + n,
                    name = "Channel $c-$n",
                    categoryId = "c$c",
                    num = c * 100 + n,
                )
            }
        }

    @Test
    fun `the whole catalogue arrives in one request rather than one per category`() = runTest {
        val api = CountingApi(categories(20), streams(20))

        val emissions = repository(api).rows(CatalogTab.LIVE).toList()

        // Every category is present, and the last emission is the whole tab.
        assertEquals(20, emissions.last().size)
        assertEquals(60, emissions.last().sumOf { it.items.size })
        assertEquals(1, api.bulkCalls)
        // Only the head start was fetched a category at a time — nowhere near
        // one request per category.
        assertTrue(
            "asked for ${api.perCategoryCalls.size} categories individually",
            api.perCategoryCalls.size <= 8,
        )
    }

    @Test
    fun `rails keep the order the panel listed its categories in`() = runTest {
        val api = CountingApi(categories(20), streams(20))

        val rows = repository(api).rows(CatalogTab.LIVE).toList().last()

        assertEquals((1..20).map { "Category $it" }, rows.map { it.title })
    }

    /**
     * The head start earns its keep or it is only wasted requests. Holding the
     * bulk answer back proves a rail reaches the screen without it.
     */
    @Test
    fun `something is on screen before the bulk answer lands`() = runTest {
        val held = CompletableDeferred<Unit>()
        val counting = CountingApi(categories(20), streams(20))
        val slowBulk = object : XtreamApi by counting {
            override suspend fun liveStreams(
                username: String,
                password: String,
                categoryId: String?,
            ): List<LiveStreamDto> {
                if (categoryId == null) held.await()
                return counting.liveStreams(username, password, categoryId)
            }
        }

        // Takes the first emission and walks away, so the bulk request is still
        // held when this returns.
        val first = repository(slowBulk).rows(CatalogTab.LIVE).first()

        assertTrue(first.isNotEmpty())
        held.complete(Unit)
    }

    @Test
    fun `a panel that will not serve the whole catalogue is walked category by category`() =
        runTest {
            val api = CountingApi(categories(20), streams(20), bulkFails = true)

            val rows = repository(api).rows(CatalogTab.LIVE).toList().last()

            assertEquals(20, rows.size)
            assertEquals(60, rows.sumOf { it.items.size })
            // Every category, and none of them asked for twice.
            assertEquals(20, api.perCategoryCalls.size)
            assertEquals(20, api.perCategoryCalls.toSet().size)
        }

    @Test
    fun `a tab served whole counts as complete`() = runTest {
        val api = CountingApi(categories(20), streams(20))
        val repository = repository(api)

        repository.rows(CatalogTab.LIVE).toList()

        assertTrue(repository.wasComplete(CatalogTab.LIVE))
    }

    @Test
    fun `a tab is not complete while a category is still refusing`() = runTest {
        val refuses = object : XtreamApi by CountingApi(categories(4), streams(4), bulkFails = true) {
            override suspend fun liveStreams(
                username: String,
                password: String,
                categoryId: String?,
            ): List<LiveStreamDto> = throw IOException("panel is having a moment")
        }
        val repository = repository(refuses)

        repository.rows(CatalogTab.LIVE).toList()

        assertFalse(repository.wasComplete(CatalogTab.LIVE))
    }
}
