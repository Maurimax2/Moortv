package com.maurimax.core.network

import com.maurimax.core.network.dto.SeriesInfoResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `episodes` is the least consistent field on the whole API, so every shape a
 * real panel has been seen to send is pinned here.
 */
class SeriesInfoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    private fun parse(body: String) = json.decodeFromString(SeriesInfoResponse.serializer(), body)

    @Test
    fun `episodes keyed by season are read`() {
        val result = parse(
            """
            {"info":{"name":"A Show","cover":"http://x/c.jpg"},
             "episodes":{"1":[
               {"id":"5501","episode_num":1,"title":"Pilot","container_extension":"mkv",
                "season":1,"info":{"movie_image":"http://x/1.jpg","duration_secs":2700}},
               {"id":"5502","episode_num":2,"title":"Second","container_extension":"mp4","season":1}
             ]}}
            """.trimIndent(),
        )

        assertEquals(listOf("1"), result.episodes.keys.toList())
        assertEquals(2, result.episodes.getValue("1").size)
        assertEquals("5501", result.episodes.getValue("1").first().id)
        assertEquals(2700, result.episodes.getValue("1").first().info?.durationSeconds)
    }

    @Test
    fun `episodes sent as an array use the index as the season`() {
        val result = parse(
            """
            {"episodes":[
              [{"id":"1","episode_num":1,"title":"One"}],
              [{"id":"2","episode_num":1,"title":"Two"}]
            ]}
            """.trimIndent(),
        )

        assertEquals(listOf("0", "1"), result.episodes.keys.toList())
        assertEquals("2", result.episodes.getValue("1").first().id)
    }

    @Test
    fun `numbers sent as strings are still numbers`() {
        val result = parse("""{"episodes":{"2":[{"id":"9","episode_num":"7","season":"2"}]}}""")

        val episode = result.episodes.getValue("2").first()
        assertEquals(7, episode.episodeNum)
        assertEquals(2, episode.season)
    }

    @Test
    fun `one malformed episode does not cost the whole series`() {
        val result = parse(
            """
            {"episodes":{"1":[
              {"id":"1","title":"Good"},
              {"id":"2","info":[]},
              {"id":"3","title":"Also good"}
            ]}}
            """.trimIndent(),
        )

        // The middle entry sends an array where an object belongs, which is a
        // real panel quirk. The two good episodes must survive it.
        val ids = result.episodes.getValue("1").map { it.id }
        assertTrue(ids.containsAll(listOf("1", "3")))
    }

    @Test
    fun `a series with no episodes parses as empty rather than failing`() {
        assertTrue(parse("""{"info":{"name":"Empty"},"episodes":[]}""").episodes.isEmpty())
        assertTrue(parse("""{"info":{"name":"Empty"},"episodes":{}}""").episodes.isEmpty())
        assertTrue(parse("""{"info":{"name":"Empty"}}""").episodes.isEmpty())
    }
}
