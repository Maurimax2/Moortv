package com.maurimax.core.designsystem

import androidx.annotation.DrawableRes
import com.maurimax.core.model.League

/**
 * Bundled football artwork.
 *
 * Bundled rather than fetched because the panel serves neither: it has channel
 * logos and nothing else. A competition badge beside a category name is the
 * fastest thing on the screen to recognise — faster than reading "الدوري
 * الإنجليزي الممتاز" — and a badge that has to load over the same connection
 * that is already struggling with the catalogue would arrive too late to help.
 */
@get:DrawableRes
val League.badgeRes: Int
    get() = when (this) {
        League.CHAMPIONS -> R.drawable.league_ucl
        League.PREMIER -> R.drawable.league_premier
        League.LALIGA -> R.drawable.league_laliga
        League.SERIE_A -> R.drawable.league_seriea
        League.BUNDESLIGA -> R.drawable.league_bundesliga
        League.SAUDI -> R.drawable.league_saudi
    }

/** The order the badges are shown in as a strip. Majors first, as elsewhere. */
val LeagueStrip: List<League> = listOf(
    League.CHAMPIONS,
    League.PREMIER,
    League.LALIGA,
    League.SERIE_A,
    League.BUNDESLIGA,
    League.SAUDI,
)

/** Cut-out player renders, for the one place the section needs a face. */
val FootballRenders: List<Int> = listOf(
    R.drawable.player_01,
    R.drawable.player_02,
    R.drawable.player_03,
    R.drawable.player_04,
)
