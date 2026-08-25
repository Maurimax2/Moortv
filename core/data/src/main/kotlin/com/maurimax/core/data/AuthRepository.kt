package com.maurimax.core.data

import com.maurimax.core.model.Account
import com.maurimax.core.model.Credentials
import com.maurimax.core.network.XtreamApi

sealed interface LoginResult {
    data class Success(val account: Account) : LoginResult
    data class Failure(val reason: PortalFailure) : LoginResult
}

/**
 * Sign-in against the fixed portal. There is no host field anywhere in this
 * class on purpose: the portal is compiled in, and a customer supplies only a
 * username and password.
 */
class AuthRepository(
    private val api: XtreamApi,
    private val credentialStore: CredentialStore,
) {

    fun savedCredentials(): Credentials? = credentialStore.load()

    /** Every account saved on this device, most recently used first. */
    fun savedAccounts(): List<Credentials> = credentialStore.all()

    suspend fun signIn(username: String, password: String, remember: Boolean = true): LoginResult {
        val response = try {
            api.login(username, password)
        } catch (e: Exception) {
            return LoginResult.Failure(e.toPortalFailure())
        }

        val info = response.userInfo
        if (info == null || info.auth != 1) {
            return LoginResult.Failure(PortalFailure.BadCredentials)
        }

        val account = Account(
            username = info.username.ifBlank { username },
            status = info.status,
            expiresAtEpochSeconds = info.expiryEpoch.toLongOrNull(),
            isTrial = info.isTrial == "1",
            activeConnections = info.activeConnections.toIntOrNull() ?: 0,
            maxConnections = info.maxConnections.toIntOrNull() ?: 0,
        )

        // A panel can authenticate an account it will not stream to.
        if (!account.isActive && account.status.isNotBlank()) {
            return LoginResult.Failure(PortalFailure.Inactive(account.status))
        }

        if (remember) {
            credentialStore.save(Credentials(username, password))
        }
        return LoginResult.Success(account)
    }

    /**
     * Signs out without forgetting. The account stays in the list so the
     * customer can hop back to it, or to another line, without typing.
     */
    fun signOut() = credentialStore.clear()
}
