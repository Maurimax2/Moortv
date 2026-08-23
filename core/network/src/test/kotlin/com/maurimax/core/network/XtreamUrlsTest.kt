package com.maurimax.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class XtreamUrlsTest {

    private val urls = XtreamUrls("http://portal.example.com:8080")

    @Test
    fun `live stream uses the live path and hls extension`() {
        assertEquals(
            "http://portal.example.com:8080/live/bob/hunter2/1234.m3u8",
            urls.live("bob", "hunter2", 1234),
        )
    }

    @Test
    fun `a trailing slash on the portal url does not double up`() {
        val trailing = XtreamUrls("http://portal.example.com:8080/")
        assertEquals(
            "http://portal.example.com:8080/live/bob/hunter2/1.m3u8",
            trailing.live("bob", "hunter2", 1),
        )
    }

    @Test
    fun `credentials with url-unsafe characters are encoded`() {
        assertEquals(
            "http://portal.example.com:8080/live/a%2Bb/p%2Fw/7.m3u8",
            urls.live("a+b", "p/w", 7),
        )
    }

    @Test
    fun `movies use the panel's container extension`() {
        assertEquals(
            "http://portal.example.com:8080/movie/bob/hunter2/55.mkv",
            urls.movie("bob", "hunter2", 55, "mkv"),
        )
    }

    @Test
    fun `a blank container extension falls back to mp4`() {
        assertEquals(
            "http://portal.example.com:8080/movie/bob/hunter2/55.mp4",
            urls.movie("bob", "hunter2", 55, ""),
        )
    }

    @Test
    fun `episodes use the series path`() {
        assertEquals(
            "http://portal.example.com:8080/series/bob/hunter2/900.mp4",
            urls.episode("bob", "hunter2", 900, "mp4"),
        )
    }
}
