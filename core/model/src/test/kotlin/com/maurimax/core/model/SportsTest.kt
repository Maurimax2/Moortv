package com.maurimax.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * These are real category names of the shape resellers actually type, which is
 * the only thing this matcher has to work against — the panel itself has no
 * idea what a sport is.
 */
class SportsTest {

    @Test
    fun `arabic sports categories are recognised`() {
        assertTrue(Sports.isSport("قنوات رياضية"))
        assertTrue(Sports.isSport("بي ان سبورت"))
        assertTrue(Sports.isSport("الدوري الإنجليزي الممتاز"))
    }

    @Test
    fun `french and english categories are recognised`() {
        assertTrue(Sports.isSport("FR | SPORT"))
        assertTrue(Sports.isSport("beIN Sports HD"))
        assertTrue(Sports.isSport("Ligue 1"))
    }

    @Test
    fun `everything else is left alone`() {
        assertFalse(Sports.isSport("أفلام عربية"))
        assertFalse(Sports.isSport("Documentaires"))
        assertFalse(Sports.isSport("KIDS | CARTOON"))
    }

    @Test
    fun `spelling of the alef does not change the answer`() {
        // A reseller is free to write either, and both mean the same league.
        assertEquals(League.PREMIER, Sports.badge("الدوري الانجليزي"))
        assertEquals(League.PREMIER, Sports.badge("الدوري الإنجليزي"))
    }

    @Test
    fun `badges are found regardless of case and spacing`() {
        assertEquals(League.LALIGA, Sports.badge("ES | LaLiga"))
        assertEquals(League.LALIGA, Sports.badge("la liga santander"))
        assertEquals(League.SERIE_A, Sports.badge("IT | SERIE A"))
        assertEquals(League.BUNDESLIGA, Sports.badge("DE Bundesliga"))
        assertEquals(League.SAUDI, Sports.badge("دوري روشن السعودي"))
    }

    @Test
    fun `the champions league wins over a name that also says premier`() {
        assertEquals(League.CHAMPIONS, Sports.badge("UEFA Champions League Premier"))
    }

    @Test
    fun `a category with no competition in its name gets no badge`() {
        assertNull(Sports.badge("قنوات رياضية"))
    }

    @Test
    fun `the majors sort ahead of everything else`() {
        val names = listOf(
            "Tennis",
            "Bundesliga",
            "UEFA Champions League",
            "beIN Sports",
            "English Premier League",
        )

        assertEquals(
            listOf("UEFA Champions League", "English Premier League", "Bundesliga", "beIN Sports", "Tennis"),
            names.sortedBy(Sports::rank),
        )
    }
}
