package com.maurimax.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountTest {

    private fun account(expiry: Long?) = Account(
        username = "bob",
        status = "Active",
        expiresAtEpochSeconds = expiry,
        isTrial = false,
        activeConnections = 0,
        maxConnections = 1,
    )

    private val now = 1_700_000_000L

    @Test
    fun `counts whole days and rounds down`() {
        // Eleven hours left is today, not tomorrow. Saying "1 day" the evening
        // before a line lapses is how somebody misses a renewal.
        assertEquals(0, account(now + 11 * 3600).daysRemaining(now))
        assertEquals(1, account(now + 25 * 3600).daysRemaining(now))
        assertEquals(30, account(now + 30 * 86_400).daysRemaining(now))
    }

    @Test
    fun `an expired line is zero rather than negative`() {
        assertEquals(0, account(now - 86_400).daysRemaining(now))
    }

    @Test
    fun `a line with no expiry reports nothing rather than guessing`() {
        assertNull(account(null).daysRemaining(now))
    }
}
