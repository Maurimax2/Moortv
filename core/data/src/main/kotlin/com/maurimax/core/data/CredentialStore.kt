package com.maurimax.core.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.maurimax.core.model.Credentials
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The accounts saved on this device, and which one is signed in.
 *
 * A household shares one television and one subscription reseller, so the same
 * box is routinely used with more than one line — a second screen for the
 * children, a line that has not expired yet, a line bought for the football.
 * Retyping a password every time is the thing that makes that painful, so every
 * account the customer has used stays here and is one tap away.
 *
 * There is still no host: every account on this list belongs to the one portal
 * compiled into the build.
 */
interface CredentialStore {
    /** The account currently signed in, or null when signed out. */
    fun load(): Credentials?

    /** Signs in as this account, remembering it if it is new. */
    fun save(credentials: Credentials)

    /** Signs out. The account stays on the device so it can be picked again. */
    fun clear()

    /** Every account on this device, most recently used first. */
    fun all(): List<Credentials>

    /** Removes an account from the device entirely, password and all. */
    fun forget(username: String)
}

@Serializable
private data class StoredAccount(
    val username: String,
    val password: String,
    val lastUsed: Long = 0,
)

/**
 * The accounts on this device.
 *
 * Encrypted at rest with a key held in the Android keystore wherever that is
 * possible, which is Android 6 and up, and in plain preferences where it is
 * not. The boxes this is sold on include cheap Android 5 sticks, and refusing
 * to run on them is a worse answer than storing a reseller's line the way every
 * other player on those boxes already does.
 *
 * The same fallback covers a working keystore that throws anyway. Cheap
 * hardware does that, and an app that cannot start is not more secure than one
 * that starts.
 */
class DeviceCredentialStore(context: Context) : CredentialStore {

    private val prefs: SharedPreferences by lazy {
        (encrypted(context) ?: context.getSharedPreferences(FILE, Context.MODE_PRIVATE))
            .also(::migrate)
    }

    private fun encrypted(context: Context): SharedPreferences? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        return runCatching {
            val key = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                FILE,
                key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrNull()
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override fun load(): Credentials? {
        val active = prefs.getString(KEY_ACTIVE, null) ?: return null
        return stored().firstOrNull { it.username == active }?.let {
            Credentials(it.username, it.password)
        }
    }

    override fun save(credentials: Credentials) {
        val rest = stored().filterNot { it.username == credentials.username }
        val entry = StoredAccount(
            username = credentials.username,
            password = credentials.password,
            lastUsed = System.currentTimeMillis(),
        )
        write(listOf(entry) + rest)
        prefs.edit().putString(KEY_ACTIVE, credentials.username).apply()
    }

    override fun clear() {
        // Only the pointer: signing out of one line must not wipe the others.
        prefs.edit().remove(KEY_ACTIVE).apply()
    }

    override fun all(): List<Credentials> =
        stored().sortedByDescending { it.lastUsed }.map { Credentials(it.username, it.password) }

    override fun forget(username: String) {
        write(stored().filterNot { it.username == username })
        if (prefs.getString(KEY_ACTIVE, null) == username) clear()
    }

    private fun stored(): List<StoredAccount> {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        // A blob written by an older build must never crash the app.
        return runCatching {
            json.decodeFromString(ListSerializer(StoredAccount.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    private fun write(accounts: List<StoredAccount>) {
        prefs.edit()
            .putString(KEY_ACCOUNTS, json.encodeToString(ListSerializer(StoredAccount.serializer()), accounts))
            .apply()
    }

    /**
     * Earlier builds held exactly one account in two flat keys. Anyone updating
     * would otherwise be signed out and asked to type their password again, so
     * the old pair is folded into the list once and then removed.
     */
    private fun migrate(prefs: SharedPreferences) {
        val user = prefs.getString(LEGACY_USER, null) ?: return
        val pass = prefs.getString(LEGACY_PASS, null)

        if (pass != null && !prefs.contains(KEY_ACCOUNTS)) {
            val seeded = listOf(StoredAccount(user, pass, System.currentTimeMillis()))
            prefs.edit()
                .putString(KEY_ACCOUNTS, json.encodeToString(ListSerializer(StoredAccount.serializer()), seeded))
                .putString(KEY_ACTIVE, user)
                .apply()
        }
        prefs.edit().remove(LEGACY_USER).remove(LEGACY_PASS).apply()
    }

    private companion object {
        const val FILE = "maurimax.credentials"
        const val KEY_ACCOUNTS = "accounts"
        const val KEY_ACTIVE = "active"
        const val LEGACY_USER = "username"
        const val LEGACY_PASS = "password"
    }
}

/** For tests and previews. */
class InMemoryCredentialStore(credentials: Credentials? = null) : CredentialStore {

    private val accounts = mutableListOf<Credentials>()
    private var active: String? = null

    init {
        credentials?.let { save(it) }
    }

    override fun load(): Credentials? = accounts.firstOrNull { it.username == active }

    override fun save(credentials: Credentials) {
        accounts.removeAll { it.username == credentials.username }
        accounts.add(0, credentials)
        active = credentials.username
    }

    override fun clear() { active = null }

    override fun all(): List<Credentials> = accounts.toList()

    override fun forget(username: String) {
        accounts.removeAll { it.username == username }
        if (active == username) active = null
    }
}
