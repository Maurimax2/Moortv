package com.maurimax.core.data

import com.maurimax.core.model.Account
import com.maurimax.core.model.Credentials
import com.maurimax.core.network.PortalProbe
import com.maurimax.core.network.XtreamApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    /**
     * Only for the diagnostic probe, never for a request the app makes
     * normally — the portal is compiled in and the API already holds it.
     */
    private val portalUrl: String = "",
) {

    fun savedCredentials(): Credentials? = credentialStore.load()

    /** Every account saved on this device, most recently used first. */
    fun savedAccounts(): List<Credentials> = credentialStore.all()

    suspend fun signIn(username: String, password: String, remember: Boolean = true): LoginResult {
        val response = try {
            api.login(username, password)
        } catch (cancelled: CancellationException) {
            // Leaving the screen mid-request is not a failed sign-in, and
            // swallowing this told the customer their panel was broken.
            throw cancelled
        } catch (e: Exception) {
            val failure = e.toPortalFailure()
            // Only where the message alone cannot be acted on. A rejected
            // password explains itself and needs no second request.
            if (failure is PortalFailure.UnexpectedResponse ||
                failure is PortalFailure.NoConnection
            ) {
                recordDiagnostic(e, username, password)
            }
            return LoginResult.Failure(failure)
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
        PortalDiagnostics.clear()
        return LoginResult.Success(account)
    }

    /**
     * Asks the panel the same question again, without parsing the answer, and
     * writes down what came back — including under a couple of other client
     * names, because a panel that serves one and refuses another looks
     * identical to a panel that is simply broken.
     */
    private suspend fun recordDiagnostic(error: Throwable, username: String, password: String) {
        val summary = "${error.javaClass.simpleName}: ${error.message.orEmpty().take(140)}"
        if (portalUrl.isBlank()) {
            PortalDiagnostics.record(summary)
            return
        }
        val probe = runCatching {
            withContext(Dispatchers.IO) { PortalProbe.run(portalUrl, username, password) }
        }.getOrElse { "probe failed: ${it.javaClass.simpleName}" }

        PortalDiagnostics.record("$summary\n$probe")
    }

    /**
     * Signs out without forgetting. The account stays in the list so the
     * customer can hop back to it, or to another line, without typing.
     */
    fun signOut() = credentialStore.clear()
}
