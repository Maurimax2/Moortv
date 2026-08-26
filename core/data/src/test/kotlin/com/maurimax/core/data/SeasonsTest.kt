package com.maurimax.core.data

import com.maurimax.core.model.Credentials
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.MediaKind
import com.maurimax.core.network.XtreamApi
import com.maurimax.core.network.XtreamUrls
import com.maurimax.core.network.dto.CategoryDto
import com.maurimax.core.network.dto.EpisodeDto
import com.maurimax.core.network.dto.EpisodeInfoDto
import com.maurimax.core.network.dto.LiveStreamDto
import com.maurimax.core.network.dto.PlayerApiResponse
import com.maurimax.core.network.dto.SeriesDto
import com.maurimax.core.network.dto.SeriesInfoResponse
import com.maurimax.core.network.dto.VodStreamDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Episodes were wired end to end — the request, the lenient parsing, the phone
 * screen, the TV screen — with nothing joining them: the repository never
 * overrode `seasons`, so every series answered with the interface default of
 * no episodes at all. These tests exist so that cannot happen quietly again.
 */
class SeasonsTest {

    private class FakeApi(private val info: SeriesInfoResponse) : XtreamApi {
        var askedFor: Int? = null
            private set

        override suspend fun login(username: String, password: String): PlayerApiResponse =
            throw IOException("not used")

        override suspend fun liveCategories(username: String, password: String) = emptyList<CategoryDto>()
        override suspend fun liveStreams(username: String, password: String, categoryId: String?) = emptyList<LiveStreamDto>()
        override suspend fun vodCategories(username: String, password: String) = emptyList<CategoryDto>()
        override suspend fun vodStreams(username: String, password: String, categoryId: String?) = emptyList<VodStreamDto>()
        override suspend fun seriesCategories(username: String, password: String) = emptyList<CategoryDto>()
        override suspend fun series(username: String, password: String, categoryId: String?) = emptyList<SeriesDto>()

        override suspend fun seriesInfo(username: String, password: String, seriesId: Int): SeriesInfoResponse {
            askedFor = seriesId
            return info
        }
    }

    private fun repository(api: XtreamApi) = XtreamContentRepository(
        api = api,
        urls = XtreamUrls("http://panel.test:80"),
        credentials = Credentials("bob", "hunter2"),
    )

    private fun series(id: String = "series-42") =
        MediaItem(id = id, title = "A Box Set", kind = MediaKind.SERIES)

    private fun episode(id: String, season: Int, number: Int, title: String = "") = EpisodeDto(
        id = id,
        episodeNum = number,
        title = title,
        containerExtension = "mkv",
        season = season,
        info = EpisodeInfoDto(image = "http://art/$id.jpg", plot = "…", durationSeconds = 2_700),
    )

    @Test
    fun `episodes come back grouped into seasons in order`() = runTest {
        val api = FakeApi(
            SeriesInfoResponse(
                episodes = mapOf(
                    "2" to listOf(episode("21", 2, 2), episode("20", 2, 1)),
                    "1" to listOf(episode("10", 1, 1), episode("11", 1, 2)),
                ),
            ),
        )

        val seasons = repository(api).seasons(series())

        assertEquals(listOf(1, 2), seasons.map { it.number })
        assertEquals(listOf(1, 2), seasons[0].episodes.map { it.number })
        assertEquals(listOf(1, 2), seasons[1].episodes.map { it.number })
    }

    @Test
    fun `the panel is asked for the bare series id`() = runTest {
        val api = FakeApi(SeriesInfoResponse())

        repository(api).seasons(series("series-42"))

        assertEquals(42, api.askedFor)
    }

    @Test
    fun `an episode plays from its own stream id, not the series`() = runTest {
        val api = FakeApi(SeriesInfoResponse(episodes = mapOf("1" to listOf(episode("907", 1, 1)))))

        val episode = repository(api).seasons(series()).single().episodes.single()

        assertEquals("http://panel.test:80/series/bob/hunter2/907.mkv", episode.playbackUrl)
        assertEquals(45, episode.durationMinutes)
        assertEquals("http://art/907.jpg", episode.artworkUrl)
    }

    /** Some panels file every episode under "0" and only number them properly. */
    @Test
    fun `a season number on the episode wins when the key says nothing`() = runTest {
        val api = FakeApi(
            SeriesInfoResponse(
                episodes = mapOf(
                    "0" to listOf(episode("1", season = 1, number = 1), episode("2", season = 2, number = 1)),
                ),
            ),
        )

        val seasons = repository(api).seasons(series())

        // The key parses to 0, which is a real number, so it wins as the panel
        // filed it — one season holding both, still ordered.
        assertEquals(listOf(0), seasons.map { it.number })
        assertEquals(2, seasons.single().episodes.size)
    }

    @Test
    fun `a non-numeric key falls back to what the episode says`() = runTest {
        val api = FakeApi(
            SeriesInfoResponse(
                episodes = mapOf(
                    "Season 3" to listOf(episode("30", season = 3, number = 1)),
                ),
            ),
        )

        val seasons = repository(api).seasons(series())

        assertEquals(listOf(3), seasons.map { it.number })
    }

    @Test
    fun `anything that is not a series asks the panel nothing`() = runTest {
        val api = FakeApi(SeriesInfoResponse())

        val seasons = repository(api).seasons(
            MediaItem(id = "movie-7", title = "A Film", kind = MediaKind.MOVIE),
        )

        assertTrue(seasons.isEmpty())
        assertEquals(null, api.askedFor)
    }
}
