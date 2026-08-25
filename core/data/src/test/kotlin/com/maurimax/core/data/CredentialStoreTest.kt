package com.maurimax.core.data

import com.maurimax.core.model.Credentials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the contract both stores implement. The encrypted one cannot be
 * exercised without a device keystore, so the in-memory one stands in — which
 * only works if the two agree on what signing out, switching and forgetting
 * mean.
 */
class CredentialStoreTest {

    private val bob = Credentials("bob", "hunter2")
    private val amina = Credentials("amina", "correcthorse")

    @Test
    fun `saving a second account keeps the first`() {
        val store = InMemoryCredentialStore()

        store.save(bob)
        store.save(amina)

        assertEquals(setOf(bob, amina), store.all().toSet())
    }

    @Test
    fun `the last account saved is the one signed in`() {
        val store = InMemoryCredentialStore()

        store.save(bob)
        store.save(amina)

        assertEquals(amina, store.load())
    }

    @Test
    fun `signing out leaves every account on the device`() {
        val store = InMemoryCredentialStore()
        store.save(bob)
        store.save(amina)

        store.clear()

        assertNull(store.load())
        assertEquals(2, store.all().size)
    }

    @Test
    fun `switching back does not duplicate an account`() {
        val store = InMemoryCredentialStore()
        store.save(bob)
        store.save(amina)

        store.save(bob)

        assertEquals(2, store.all().size)
        assertEquals(bob, store.load())
    }

    @Test
    fun `a changed password replaces the stored one`() {
        val store = InMemoryCredentialStore()
        store.save(bob)

        store.save(bob.copy(password = "renewed"))

        assertEquals(listOf(Credentials("bob", "renewed")), store.all())
    }

    @Test
    fun `forgetting the signed-in account signs out`() {
        val store = InMemoryCredentialStore()
        store.save(bob)
        store.save(amina)

        store.forget(amina.username)

        assertNull(store.load())
        assertEquals(listOf(bob), store.all())
    }

    @Test
    fun `forgetting another account leaves this one signed in`() {
        val store = InMemoryCredentialStore()
        store.save(bob)
        store.save(amina)

        store.forget(bob.username)

        assertEquals(amina, store.load())
        assertTrue(store.all().none { it.username == bob.username })
    }
}
