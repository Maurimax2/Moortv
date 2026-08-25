package com.maurimax.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maurimax.core.data.PortalFailure
import com.maurimax.core.designsystem.Corners
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.Credentials

/**
 * The accounts already on this device.
 *
 * One box in a household commonly carries more than one line, and the reason
 * that is painful today is the password: a customer who has to fetch a slip of
 * paper to switch simply does not switch. So the password is never asked for
 * twice — picking a row signs straight in.
 *
 * Removing is deliberately quiet: it needs a second tap to confirm, because a
 * mis-tap here means finding that slip of paper after all.
 */
@Composable
fun AccountList(
    accounts: List<Credentials>,
    signingIn: Boolean,
    error: PortalFailure?,
    onUse: (Credentials) -> Unit,
    onForget: (Credentials) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    var confirming by remember { mutableStateOf<String?>(null) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.accounts_title),
            color = colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stringResource(R.string.accounts_subtitle),
            color = colors.textSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )

        error?.let { failure ->
            Text(
                text = failure.message(),
                color = colors.accentText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Corners.tile))
                    .background(colors.surfaceRaised)
                    .padding(Spacing.md),
            )
        }

        accounts.forEach { account ->
            AccountRow(
                account = account,
                busy = signingIn,
                confirmingRemoval = confirming == account.username,
                onUse = { if (!signingIn) onUse(account) },
                onAskRemove = { confirming = account.username },
                onCancelRemove = { confirming = null },
                onConfirmRemove = {
                    confirming = null
                    onForget(account)
                },
            )
        }

        TextButton(onClick = onAdd, modifier = Modifier.padding(top = Spacing.sm)) {
            Text(
                text = "+  " + stringResource(R.string.accounts_add),
                color = colors.accentText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AccountRow(
    account: Credentials,
    busy: Boolean,
    confirmingRemoval: Boolean,
    onUse: () -> Unit,
    onAskRemove: () -> Unit,
    onCancelRemove: () -> Unit,
    onConfirmRemove: () -> Unit,
) {
    val colors = MaurimaxTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.card))
            .background(colors.surface.copy(alpha = 0.96f))
            .border(1.dp, colors.outline, RoundedCornerShape(Corners.card)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !busy, onClick = onUse)
                .padding(Spacing.md),
        ) {
            Initial(account.username)

            Text(
                text = account.username,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            if (busy) {
                CircularProgressIndicator(
                    color = colors.accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                TextButton(onClick = onAskRemove) {
                    Text(
                        text = stringResource(R.string.accounts_remove),
                        color = colors.textTertiary,
                        fontSize = 13.sp,
                    )
                }
            }
        }

        if (confirmingRemoval) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.accounts_remove_confirm, account.username),
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onCancelRemove) {
                    Text(stringResource(R.string.accounts_cancel), color = colors.textSecondary, fontSize = 13.sp)
                }
                TextButton(onClick = onConfirmRemove) {
                    Text(
                        text = stringResource(R.string.accounts_remove),
                        color = colors.accentText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

/**
 * A username in a violet disc. There are no avatars to show — the panel serves
 * none — and a row of identical generic icons is worse than a letter that at
 * least differs between lines.
 */
@Composable
internal fun Initial(username: String, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val colors = MaurimaxTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(colors.identity),
    ) {
        Text(
            text = username.take(1).uppercase(),
            color = colors.textPrimary,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = FontWeight.Black,
        )
    }
}
