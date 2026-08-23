package com.maurimax.core.data

import com.maurimax.core.model.Account
import com.maurimax.core.model.Credentials
import com.maurimax.core.network.XtreamApi

/** Why a sign-in attempt did not succeed. Each maps to a distinct message. */
sealed interface LoginFailure {
    /** The panel answered, and said no. */
    data object BadCredentials : LoginFailure

    /** Credentials are valid but the account is expired, banned or disabled. */
    data class Inactive(val status: String) : LoginFailure

    /** The panel could not be reached at all. */
    data class Unreachable(val cause: String) : LoginFailure
}

sealed interface LoginResult {
    data class Success(val account: Account) : LoginResult
    data class Failure(val reason: LoginFailure) : LoginResult
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

    suspend fun signIn(username: String, password: String, remember: Boolean = true): LoginResult {
        val response = try {
            api.login(username, password)
        } catch (e: Exception) {
            return LoginResult.Failure(
                LoginFailure.Unreachable(e.message ?: e::class.simpleName ?: "unknown error"),
            )
        }

        val info = response.userInfo
        if (info == null || info.auth != 1) {
            return LoginResult.Failure(LoginFailure.BadCredentials)
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
            return LoginResult.Failure(LoginFailure.Inactive(account.status))
        }

        if (remember) {
            credentialStore.save(Credentials(username, password))
        }
        return LoginResult.Success(account)
    }

    fun signOut() = credentialStore.clear()
}
