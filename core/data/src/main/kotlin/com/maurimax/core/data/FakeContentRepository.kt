package com.maurimax.core.data

import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * In-memory catalog so the whole UI can be built and demoed before the real
 * content source exists. Deliberately the only place fake data lives.
 */
class FakeContentRepository : ContentRepository {

    override fun homeRows(): Flow<List<ContentRow>> = flow {
        emit(rows)
    }

    private companion object {
        val rows = listOf(
            ContentRow(
                title = "Continue watching",
                items = listOf(
                    MediaItem(
                        id = "cw-1",
                        title = "Desert Signal",
                        year = 2025,
                        genre = "Thriller",
                        description = "A radio engineer picks up a broadcast that has not been " +
                            "transmitted for forty years.",
                        artworkTint = 0xFF7B3FA0,
                        durationMinutes = 118,
                        progress = 0.62f,
                    ),
                    MediaItem(
                        id = "cw-2",
                        title = "Northbound",
                        year = 2024,
                        genre = "Drama",
                        description = "Two strangers share a freight train across a closing border.",
                        artworkTint = 0xFF1F6F8B,
                        durationMinutes = 96,
                        progress = 0.18f,
                    ),
                    MediaItem(
                        id = "cw-3",
                        title = "The Quiet Harvest",
                        year = 2023,
                        genre = "Documentary",
                        description = "Four seasons on a farm that has refused to modernise.",
                        artworkTint = 0xFF4C7A34,
                        durationMinutes = 84,
                        progress = 0.91f,
                    ),
                ),
            ),
            ContentRow(
                title = "Trending now",
                items = listOf(
                    MediaItem(
                        id = "tr-1",
                        title = "Salt and Static",
                        year = 2026,
                        genre = "Sci-fi",
                        description = "A coastal town wakes to find the tide running backwards.",
                        artworkTint = 0xFFB5482E,
                        durationMinutes = 131,
                    ),
                    MediaItem(
                        id = "tr-2",
                        title = "Paper Cities",
                        year = 2025,
                        genre = "Animation",
                        description = "An architect's unbuilt drawings come to life at night.",
                        artworkTint = 0xFFC98A21,
                        durationMinutes = 102,
                    ),
                    MediaItem(
                        id = "tr-3",
                        title = "Low Orbit",
                        year = 2025,
                        genre = "Action",
                        description = "A salvage crew has ninety minutes before the station burns up.",
                        artworkTint = 0xFF2E4FA0,
                        durationMinutes = 109,
                    ),
                    MediaItem(
                        id = "tr-4",
                        title = "Hollow Season",
                        year = 2024,
                        genre = "Horror",
                        description = "The village festival has one rule, and this year someone breaks it.",
                        artworkTint = 0xFF6B2140,
                        durationMinutes = 94,
                    ),
                ),
            ),
            ContentRow(
                title = "Because you watched Northbound",
                items = listOf(
                    MediaItem(
                        id = "rec-1",
                        title = "Interior Weather",
                        year = 2023,
                        genre = "Drama",
                        description = "A meteorologist reads her own forecasts as a diary.",
                        artworkTint = 0xFF37606B,
                        durationMinutes = 88,
                    ),
                    MediaItem(
                        id = "rec-2",
                        title = "Nine Bridges",
                        year = 2022,
                        genre = "Drama",
                        description = "A bridge inspector counts down his last crossings.",
                        artworkTint = 0xFF8A5A2B,
                        durationMinutes = 112,
                    ),
                    MediaItem(
                        id = "rec-3",
                        title = "The Long Platform",
                        year = 2024,
                        genre = "Mystery",
                        description = "A night porter is the only witness to a train that never arrives.",
                        artworkTint = 0xFF3F3F6B,
                        durationMinutes = 105,
                    ),
                ),
            ),
        )
    }
}
