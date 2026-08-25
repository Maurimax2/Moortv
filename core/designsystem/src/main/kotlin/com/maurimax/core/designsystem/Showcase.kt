package com.maurimax.core.designsystem

import androidx.annotation.DrawableRes
import com.maurimax.core.model.League

enum class ShowcaseKind { FILM, SERIES }

/** A bundled title, with the only two facts about it that are actually known. */
data class ShowcaseTitle(
    @DrawableRes val poster: Int,
    val title: String,
    val kind: ShowcaseKind,
)

/**
 * A subject cut out of its background — a character or a player on white.
 *
 * Useless as a poster tile in a dark interface and excellent as the foreground
 * of a hero, which is the only place they are used.
 */
data class ShowcaseSubject(
    @DrawableRes val image: Int,
    val title: String,
)

/**
 * The artwork that ships inside the app.
 *
 * It exists because the panel is slow and its own artwork is unreliable: on a
 * first launch, on a dead connection, and behind every broken poster URL, the
 * screen has to be worth looking at anyway. API artwork always wins when it
 * loads — this is the floor, not the ceiling.
 *
 * Only the title and whether it is a film or a series are recorded. Years,
 * runtimes and ratings are not known here, and inventing them would put
 * made-up facts in front of a customer.
 */
object Showcase {

    val films: List<ShowcaseTitle> = listOf(
        ShowcaseTitle(R.drawable.poster_spiderman, "Spider-Man: Brand New Day", ShowcaseKind.FILM),
        ShowcaseTitle(R.drawable.poster_batman, "The Batman", ShowcaseKind.FILM),
        ShowcaseTitle(R.drawable.poster_oppenheimer, "Oppenheimer", ShowcaseKind.FILM),
        ShowcaseTitle(R.drawable.poster_odyssey, "The Odyssey", ShowcaseKind.FILM),
        ShowcaseTitle(R.drawable.poster_fury, "Fury", ShowcaseKind.FILM),
    )

    val series: List<ShowcaseTitle> = listOf(
        ShowcaseTitle(R.drawable.poster_got, "Game of Thrones", ShowcaseKind.SERIES),
        ShowcaseTitle(R.drawable.poster_breakingbad, "Breaking Bad", ShowcaseKind.SERIES),
        ShowcaseTitle(R.drawable.poster_casadepapel, "La Casa de Papel", ShowcaseKind.SERIES),
        ShowcaseTitle(R.drawable.poster_walkingdead, "The Walking Dead", ShowcaseKind.SERIES),
        ShowcaseTitle(R.drawable.poster_punisher, "The Punisher", ShowcaseKind.SERIES),
        ShowcaseTitle(R.drawable.poster_mentalist, "The Mentalist", ShowcaseKind.SERIES),
    )

    val all: List<ShowcaseTitle> = films + series

    /** Characters, for a hero that needs a face rather than a poster. */
    val characters: List<ShowcaseSubject> = listOf(
        ShowcaseSubject(R.drawable.cutout_homelander, "The Boys"),
        ShowcaseSubject(R.drawable.cutout_heisenberg, "Breaking Bad"),
        ShowcaseSubject(R.drawable.cutout_tyrion, "Game of Thrones"),
    )

    /** Players, for the same job in the football section. */
    val players: List<ShowcaseSubject> = listOf(
        ShowcaseSubject(R.drawable.player_01, "Cristiano Ronaldo"),
        ShowcaseSubject(R.drawable.player_02, "Lionel Messi"),
        ShowcaseSubject(R.drawable.player_03, "Erling Haaland"),
        ShowcaseSubject(R.drawable.player_04, "Lamine Yamal"),
    )

    val leagues: List<League> = LeagueStrip

    /**
     * A stable pick for a given key.
     *
     * Deliberately not random: a hero that changes on every recomposition
     * flickers, and one that changes on every scroll feels broken. The same
     * account and day get the same artwork.
     */
    fun <T> pick(items: List<T>, key: Any): T = items[Math.floorMod(key.hashCode(), items.size)]
}
