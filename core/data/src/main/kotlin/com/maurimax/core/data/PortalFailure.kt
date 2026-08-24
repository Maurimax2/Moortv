package com.maurimax.core.data

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Why a call to the portal did not succeed.
 *
 * Every case gets its own message, because they need different actions from the
 * customer. Collapsing them — as an earlier version did, by catching every
 * exception as "unreachable" — tells someone with a mistyped password to check
 * their internet, which sends them looking in the wrong place entirely.
 */
sealed interface PortalFailure {

    /** The panel answered and rejected the credentials. */
    data object BadCredentials : PortalFailure

    /** Credentials are valid, but the account is expired, banned or disabled. */
    data class Inactive(val status: String) : PortalFailure

    /** Nothing reached the panel: no network, DNS failure, timeout. */
    data object NoConnection : PortalFailure

    /** The panel is up but broken, or overloaded. */
    data class ServerError(val code: Int) : PortalFailure

    /** The panel replied with something that is not the API — often an error page. */
    data object UnexpectedResponse : PortalFailure
}

/**
 * Maps a thrown error onto a cause.
 *
 * The distinction that matters most is HTTP versus IO. A panel that rejects a
 * login commonly answers 401 or 403 rather than a JSON body saying `auth: 0`,
 * and Retrofit turns that into an [HttpException] — which is an answer, not a
 * connection problem.
 */
fun Throwable.toPortalFailure(): PortalFailure = when (this) {
    is HttpException -> when (code()) {
        401, 403 -> PortalFailure.BadCredentials
        in 500..599 -> PortalFailure.ServerError(code())
        else -> PortalFailure.UnexpectedResponse
    }

    // Reached the panel, but the body was not the API: an HTML error or block page.
    is SerializationException -> PortalFailure.UnexpectedResponse

    is IOException -> PortalFailure.NoConnection

    else -> PortalFailure.UnexpectedResponse
}

/** Customer-facing message for a cause. Resolved by the UI with `stringResource`. */
val PortalFailure.messageRes: Int
    get() = when (this) {
        PortalFailure.BadCredentials -> R.string.portal_bad_credentials
        is PortalFailure.Inactive -> R.string.portal_inactive
        PortalFailure.NoConnection -> R.string.portal_no_connection
        is PortalFailure.ServerError -> R.string.portal_server_error
        PortalFailure.UnexpectedResponse -> R.string.portal_unexpected
    }
