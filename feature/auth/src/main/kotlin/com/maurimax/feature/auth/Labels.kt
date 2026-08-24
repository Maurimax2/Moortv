package com.maurimax.feature.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.maurimax.core.data.PortalFailure
import com.maurimax.core.data.messageRes

/**
 * Customer-facing wording for a failed sign-in. The cause and its message live
 * in :core:data so the catalogue screens say the same thing about the same
 * failure.
 */
@Composable
internal fun PortalFailure.message(): String =
    if (this is PortalFailure.Inactive) {
        stringResource(messageRes, status)
    } else {
        stringResource(messageRes)
    }
