package com.maurimax.core.data

import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem
import com.maurimax.core.model.MediaKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * In-memory catalog for previews and tests. Deliberately the only place fake
 * data lives, so nothing real ever falls back to it silently.
 */
class FakeContentRepository : ContentRepository {

    override fun rows(tab: CatalogTab): Flow<List<ContentRow>> = flowOf(
        when (tab) {
            CatalogTab.LIVE -> live
            CatalogTab.MOVIES -> movies
            CatalogTab.SERIES -> series
        },
    )

    private companion object {
        fun item(id: String, title: String, kind: MediaKind, rating: String = "") =
            MediaItem(id = id, title = title, kind = kind, rating = rating)

        val live = listOf(
            ContentRow(
                "Entertainment",
                listOf(
                    item("l1", "Channel One HD", MediaKind.LIVE),
                    item("l2", "Channel Two HD", MediaKind.LIVE),
                    item("l3", "Sports Extra", MediaKind.CATCH_UP),
                ),
            ),
            ContentRow(
                "News",
                listOf(
                    item("l4", "World News 24", MediaKind.LIVE),
                    item("l5", "Business Today", MediaKind.LIVE),
                ),
            ),
        )

        val movies = listOf(
            ContentRow(
                "Action",
                listOf(
                    item("m1", "Low Orbit", MediaKind.MOVIE),
                    item("m2", "Salt and Static", MediaKind.MOVIE),
                ),
            ),
            ContentRow(
                "Drama",
                listOf(
                    item("m3", "Northbound", MediaKind.MOVIE),
                    item("m4", "Nine Bridges", MediaKind.MOVIE),
                ),
            ),
        )

        val series = listOf(
            ContentRow(
                "Box sets",
                listOf(
                    item("s1", "The Long Platform", MediaKind.SERIES),
                    item("s2", "Paper Cities", MediaKind.SERIES),
                ),
            ),
        )
    }
}
