package com.maurimax.core.data

import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.MediaKind

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
        fun item(id: String, title: String, kind: MediaKind, tint: Long, rating: String = "") =
            MediaItem(id = id, title = title, kind = kind, artworkTint = tint, rating = rating)

        val live = listOf(
            ContentRow(
                "Entertainment",
                listOf(
                    item("l1", "Channel One HD", MediaKind.LIVE, 0xFF7B3FA0),
                    item("l2", "Channel Two HD", MediaKind.LIVE, 0xFF1F6F8B),
                    item("l3", "Sports Extra", MediaKind.CATCH_UP, 0xFF4C7A34),
                ),
            ),
            ContentRow(
                "News",
                listOf(
                    item("l4", "World News 24", MediaKind.LIVE, 0xFFB5482E),
                    item("l5", "Business Today", MediaKind.LIVE, 0xFF2E4FA0),
                ),
            ),
        )

        val movies = listOf(
            ContentRow(
                "Action",
                listOf(
                    item("m1", "Low Orbit", MediaKind.MOVIE, 0xFF2E4FA0),
                    item("m2", "Salt and Static", MediaKind.MOVIE, 0xFFB5482E),
                ),
            ),
            ContentRow(
                "Drama",
                listOf(
                    item("m3", "Northbound", MediaKind.MOVIE, 0xFF1F6F8B),
                    item("m4", "Nine Bridges", MediaKind.MOVIE, 0xFF8A5A2B),
                ),
            ),
        )

        val series = listOf(
            ContentRow(
                "Box sets",
                listOf(
                    item("s1", "The Long Platform", MediaKind.SERIES, 0xFF3F3F6B),
                    item("s2", "Paper Cities", MediaKind.SERIES, 0xFFC98A21),
                ),
            ),
        )
    }
}
