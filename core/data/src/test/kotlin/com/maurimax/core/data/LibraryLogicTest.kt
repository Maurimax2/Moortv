package com.maurimax.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The progress arithmetic, which is what decides whether a title appears in
 * continue-watching at all.
 */
class LibraryLogicTest {

    private fun item(position: Long, duration: Long) =
        SavedItem(id = "x", title = "x", kind = "MOVIE", positionMs = position, durationMs = duration)

    @Test
    fun `progress is the fraction watched`() {
        assertEquals(0.5f, item(50, 100).progress, 0.001f)
        assertEquals(0.25f, item(25, 100).progress, 0.001f)
    }

    @Test
    fun `a stream with no duration reports no progress rather than dividing by zero`() {
        // Live channels and some panel streams report duration 0.
        assertEquals(0f, item(90_000, 0).progress, 0.001f)
    }

    @Test
    fun `progress never escapes zero to one`() {
        // Players can report a position past the reported duration near the end.
        assertEquals(1f, item(120, 100).progress, 0.001f)
        assertEquals(0f, item(-5, 100).progress, 0.001f)
    }
}
