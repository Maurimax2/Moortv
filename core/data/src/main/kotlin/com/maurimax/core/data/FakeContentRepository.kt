package com.maurimax.core.data

import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem

/**
 * In-memory catalog for previews and tests. Deliberately the only place fake
 * data lives, so nothing real ever falls back to it silently.
 */
class FakeContentRepository : ContentRepository {

    override suspend fun rows(tab: CatalogTab): List<ContentRow> = when (tab) {
        CatalogTab.LIVE -> live
        CatalogTab.MOVIES -> movies
        CatalogTab.SERIES -> series
    }

    private companion object {
        fun item(id: String, title: String, year: Int, genre: String, tint: Long) = MediaItem(
            id = id,
            title = title,
            year = year,
            genre = genre,
            description = "",
            artworkTint = tint,
            durationMinutes = 0,
        )

        val live = listOf(
            ContentRow(
                "Entertainment",
                listOf(
                    item("l1", "Channel One HD", 0, "Live", 0xFF7B3FA0),
                    item("l2", "Channel Two HD", 0, "Live", 0xFF1F6F8B),
                    item("l3", "Sports Extra", 0, "Catch-up", 0xFF4C7A34),
                ),
            ),
            ContentRow(
                "News",
                listOf(
                    item("l4", "World News 24", 0, "Live", 0xFFB5482E),
                    item("l5", "Business Today", 0, "Live", 0xFF2E4FA0),
                ),
            ),
        )

        val movies = listOf(
            ContentRow(
                "Action",
                listOf(
                    item("m1", "Low Orbit", 2025, "Action", 0xFF2E4FA0),
                    item("m2", "Salt and Static", 2026, "Sci-fi", 0xFFB5482E),
                ),
            ),
            ContentRow(
                "Drama",
                listOf(
                    item("m3", "Northbound", 2024, "Drama", 0xFF1F6F8B),
                    item("m4", "Nine Bridges", 2022, "Drama", 0xFF8A5A2B),
                ),
            ),
        )

        val series = listOf(
            ContentRow(
                "Box sets",
                listOf(
                    item("s1", "The Long Platform", 2024, "Mystery", 0xFF3F3F6B),
                    item("s2", "Paper Cities", 2025, "Animation", 0xFFC98A21),
                ),
            ),
        )
    }
}
