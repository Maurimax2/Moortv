package com.maurimax.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Real category names of the shape resellers actually type, which is the only
 * thing this matcher has to work against — the panel itself has no idea what a
 * competition is.
 */
class SportsTest {

    @Test
    fun `badges are found regardless of case and spacing`() {
        assertEquals(League.LALIGA, Sports.badge("ES | LaLiga"))
        assertEquals(League.LALIGA, Sports.badge("la liga santander"))
        assertEquals(League.SERIE_A, Sports.badge("IT | SERIE A"))
        assertEquals(League.BUNDESLIGA, Sports.badge("DE Bundesliga"))
        assertEquals(League.SAUDI, Sports.badge("دوري روشن السعودي"))
    }

    @Test
    fun `spelling of the alef does not change the answer`() {
        // A reseller is free to write either, and both mean the same league.
        assertEquals(League.PREMIER, Sports.badge("الدوري الانجليزي"))
        assertEquals(League.PREMIER, Sports.badge("الدوري الإنجليزي"))
    }

    @Test
    fun `the champions league wins over a name that also says premier`() {
        assertEquals(League.CHAMPIONS, Sports.badge("UEFA Champions League Premier"))
    }

    @Test
    fun `a category with no competition in its name gets no badge`() {
        assertNull(Sports.badge("قنوات رياضية"))
        assertNull(Sports.badge("أفلام عربية"))
    }
}
