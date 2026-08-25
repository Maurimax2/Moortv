package com.maurimax.core.model

/** A competition the app carries a badge for. */
enum class League { CHAMPIONS, PREMIER, LALIGA, SERIE_A, BUNDESLIGA, SAUDI }

/**
 * Recognising sport in a panel's own words.
 *
 * Xtream panels carry no notion of a genre — a category is a free-text name a
 * reseller typed, in whatever language and spelling they felt like. So the only
 * way to build a football section is to read those names, in the three
 * languages this catalogue actually uses.
 *
 * Deliberately generous rather than precise: a sports tab that misses beIN
 * because the reseller wrote "BeIn Sport HD" is useless, while an extra
 * wrestling channel in it costs nobody anything.
 */
object Sports {

    /**
     * Words that mean sport somewhere in this catalogue. Arabic first, since
     * that is what most of this panel is named in.
     */
    private val VOCABULARY = listOf(
        // Arabic
        "رياض", "الرياضية", "مباريات", "دوري", "كأس", "بي ان", "بين سبورت", "بى ان",
        // French
        "sport", "foot", "ligue", "coupe",
        // English and brand names that are only ever sport
        "sports", "football", "soccer", "bein", "ssc", "dazn", "espn", "eurosport",
        "premier", "laliga", "la liga", "serie a", "bundesliga", "champions", "uefa",
        "match", "arena", "abu dhabi sp", "ad sport", "alkass", "canal+ sport",
        "sky sp", "supersport", "motogp", "formula", "nba", "ufc", "wwe",
        "tennis", "basket", "handball", "boxing", "rugby", "golf", "wrestling",
    )

    /** Badge lookup. Ordered, because "premier" also appears in some UCL names. */
    private val BADGES = listOf(
        League.CHAMPIONS to listOf("champions", "uefa", "دوري ابطال", "أبطال أوروبا", "ucl"),
        League.PREMIER to listOf("premier", "epl", "الدوري الانجليزي", "الإنجليزي", "anglaise"),
        League.LALIGA to listOf("laliga", "la liga", "الدوري الاسباني", "الإسباني", "espagne"),
        League.SERIE_A to listOf("serie a", "seriea", "الدوري الايطالي", "الإيطالي", "italie"),
        League.BUNDESLIGA to listOf("bundesliga", "الدوري الالماني", "الألماني", "allemagne"),
        League.SAUDI to listOf("roshn", "saudi", "السعودي", "المحترفين"),
    )

    /**
     * Majors first. A customer opening a football section is looking for the
     * match tonight, and the panel's own ordering is whatever order the
     * reseller happened to add categories in.
     */
    private val PRIORITY = listOf(
        League.CHAMPIONS, League.PREMIER, League.LALIGA,
        League.SERIE_A, League.BUNDESLIGA, League.SAUDI,
    )

    fun isSport(name: String): Boolean {
        val text = name.normalise()
        return VOCABULARY.any { text.contains(it.normalise()) }
    }

    fun badge(name: String): League? {
        val text = name.normalise()
        return BADGES.firstOrNull { (_, words) -> words.any { text.contains(it.normalise()) } }?.first
    }

    /** Lower sorts earlier. Anything unrecognised falls in behind the majors. */
    fun rank(name: String): Int {
        val league = badge(name) ?: return PRIORITY.size + if (isGeneralSport(name)) 0 else 1
        return PRIORITY.indexOf(league)
    }

    /** A general sports channel beats a category that only mentions a sport. */
    private fun isGeneralSport(name: String): Boolean {
        val text = name.normalise()
        return listOf("bein", "بي ان", "ssc", "sport", "رياض").any { text.contains(it.normalise()) }
    }

    /**
     * Case, Arabic diacritics and the alef/hamza spellings a reseller is free
     * to pick between — إ, أ, آ and ا are the same letter as far as a customer
     * searching for the Spanish league is concerned.
     */
    private fun String.normalise(): String = lowercase()
        .map { char ->
            when (char) {
                'أ', 'إ', 'آ', 'ٱ' -> 'ا'
                'ى' -> 'ي'
                'ة' -> 'ه'
                else -> char
            }
        }
        .filterNot { it in DIACRITICS }
        .joinToString("")

    private val DIACRITICS = setOf('ً', 'ٌ', 'ٍ', 'َ', 'ُ', 'ِ', 'ّ', 'ْ')
}
