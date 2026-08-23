package com.maurimax.core.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.maurimax.core.model.Credentials

/** Where the customer's username and password live between launches. */
interface CredentialStore {
    fun load(): Credentials?
    fun save(credentials: Credentials)
    fun clear()
}

/**
 * Backed by EncryptedSharedPreferences, so the password is encrypted at rest
 * with a key held in the Android keystore rather than sitting in plaintext XML.
 */
class EncryptedCredentialStore(context: Context) : CredentialStore {

    private val prefs by lazy {
        val key = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "maurimax.credentials",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun load(): Credentials? {
        val user = prefs.getString(KEY_USER, null) ?: return null
        val pass = prefs.getString(KEY_PASS, null) ?: return null
        return Credentials(user, pass)
    }

    override fun save(credentials: Credentials) {
        prefs.edit()
            .putString(KEY_USER, credentials.username)
            .putString(KEY_PASS, credentials.password)
            .apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_USER = "username"
        const val KEY_PASS = "password"
    }
}

/** For tests and previews. */
class InMemoryCredentialStore(private var credentials: Credentials? = null) : CredentialStore {
    override fun load() = credentials
    override fun save(credentials: Credentials) { this.credentials = credentials }
    override fun clear() { credentials = null }
}
