package com.maurimax.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.maurimax.core.data.LoginFailure

/**
 * Customer-facing wording for a failed sign-in.
 *
 * A customer cannot fix a server outage, so the unreachable case tells them what
 * to check rather than surfacing the underlying exception.
 */
@Composable
internal fun LoginFailure.message(): String = when (this) {
    LoginFailure.BadCredentials -> stringResource(R.string.auth_error_credentials)
    is LoginFailure.Inactive -> stringResource(R.string.auth_error_inactive, status)
    is LoginFailure.Unreachable -> stringResource(R.string.auth_error_unreachable)
}
