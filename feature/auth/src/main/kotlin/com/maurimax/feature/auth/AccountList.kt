package com.maurimax.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maurimax.core.data.PortalFailure
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
 * Removing needs a second tap to confirm, because a mis-tap here means finding
 * that slip of paper after all.
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
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.accounts_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.textPrimary,
        )

        error?.let { failure ->
            Text(
                text = failure.message(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.accentText,
                modifier = Modifier.padding(bottom = Spacing.xs),
            )
        }

        accounts.forEach { account ->
            AccountRow(
                account = account,
                busy = signingIn,
                confirmingRemoval = confirming == account.username,
                onUse = {
                    confirming = null
                    if (!signingIn) onUse(account)
                },
                onAskRemove = { confirming = account.username },
                onCancelRemove = { confirming = null },
                onConfirmRemove = {
                    confirming = null
                    onForget(account)
                },
            )
        }

        Text(
            text = stringResource(R.string.accounts_add),
            style = MaterialTheme.typography.labelLarge,
            color = colors.textSecondary,
            modifier = Modifier
                .padding(top = Spacing.xs)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAdd)
                .padding(vertical = Spacing.sm),
        )
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
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.outline, RoundedCornerShape(12.dp)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !busy, onClick = onUse)
                .padding(horizontal = Spacing.md, vertical = 14.dp),
        ) {
            Initial(account.username)

            Text(
                text = account.username,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Text(
                text = stringResource(R.string.accounts_remove),
                style = MaterialTheme.typography.labelMedium,
                color = colors.textTertiary,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAskRemove)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            )
        }

        if (confirmingRemoval) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.padding(
                    start = Spacing.md,
                    end = Spacing.md,
                    bottom = Spacing.md,
                ),
            ) {
                Text(
                    text = stringResource(R.string.accounts_remove_confirm, account.username),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    Text(
                        text = stringResource(R.string.accounts_cancel),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.clickable(onClick = onCancelRemove),
                    )
                    Text(
                        text = stringResource(R.string.accounts_remove),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accentText,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onConfirmRemove),
                    )
                }
            }
        }
    }
}

/**
 * A username in a violet disc.
 *
 * The panel serves no avatars, and a row of identical generic icons says less
 * than the first letter of the line it belongs to. This is the one place in the
 * interface the brand violet appears as a fill.
 */
@Composable
internal fun Initial(username: String, size: Dp = 38.dp) {
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
