package com.maurimax.core.model

/** A competition the app carries a badge for. */
enum class League { CHAMPIONS, PREMIER, LALIGA, SERIE_A, BUNDESLIGA, SAUDI }

/**
 * Recognising a competition in a panel's own words.
 *
 * Xtream panels carry no notion of a genre — a category is a free-text name a
 * reseller typed, in whatever language and spelling they felt like. Reading
 * those names is the only way to know that a live category is the Premier
 * League, and a badge beside its heading is recognised across a room far faster
 * than the words next to it.
 */
object Sports {

    /** Badge lookup. Ordered, because "premier" also appears in some UCL names. */
    private val BADGES = listOf(
        League.CHAMPIONS to listOf("champions", "uefa", "دوري ابطال", "أبطال أوروبا", "ucl"),
        League.PREMIER to listOf("premier", "epl", "الدوري الانجليزي", "الإنجليزي", "anglaise"),
        League.LALIGA to listOf("laliga", "la liga", "الدوري الاسباني", "الإسباني", "espagne"),
        League.SERIE_A to listOf("serie a", "seriea", "الدوري الايطالي", "الإيطالي", "italie"),
        League.BUNDESLIGA to listOf("bundesliga", "الدوري الالماني", "الألماني", "allemagne"),
        League.SAUDI to listOf("roshn", "saudi", "السعودي", "المحترفين"),
    )

    fun badge(name: String): League? {
        val text = name.normalise()
        return BADGES.firstOrNull { (_, words) -> words.any { text.contains(it.normalise()) } }?.first
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
