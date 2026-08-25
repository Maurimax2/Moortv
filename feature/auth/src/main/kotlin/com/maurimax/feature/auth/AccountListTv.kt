@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.maurimax.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Text
import com.maurimax.core.data.PortalFailure
import com.maurimax.core.designsystem.Corners
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.Credentials

/**
 * The accounts on this box, for a remote.
 *
 * The whole point on a television is that nobody types: an on-screen keyboard
 * with a D-pad is the worst input device in the house, and a household with two
 * lines would otherwise use one of them. Picking a row signs straight in.
 */
@Composable
fun AccountListTv(
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
    val first = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.accounts_title),
            color = colors.textPrimary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = stringResource(R.string.accounts_subtitle),
            color = colors.textSecondary,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )

        error?.let { failure ->
            Text(
                text = failure.message(),
                color = colors.accentText,
                fontSize = 16.sp,
                lineHeight = 23.sp,
                modifier = Modifier.padding(bottom = Spacing.sm),
            )
        }

        accounts.forEachIndexed { index, account ->
            val confirmingThis = confirming == account.username

            TvRowCard(
                onClick = {
                    if (!signingIn) {
                        confirming = null
                        onUse(account)
                    }
                },
                modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                ) {
                    Initial(account.username, size = 36.dp)
                    Text(
                        text = account.username,
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Removal is its own focusable step rather than a long press: a
            // gesture nobody can see is a feature nobody uses.
            TvRowCard(
                onClick = {
                    if (confirmingThis) {
                        confirming = null
                        onForget(account)
                    } else {
                        confirming = account.username
                    }
                },
                accent = confirmingThis,
            ) {
                Text(
                    text = if (confirmingThis) {
                        stringResource(R.string.accounts_remove_confirm, account.username)
                    } else {
                        stringResource(R.string.accounts_remove)
                    },
                    color = if (confirmingThis) colors.accentText else colors.textTertiary,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                )
            }
        }

        TvRowCard(onClick = onAdd, modifier = Modifier.padding(top = Spacing.sm)) {
            Text(
                text = "+  " + stringResource(R.string.accounts_add),
                color = colors.accentText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            )
        }
    }
}

@Composable
private fun TvRowCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = MaurimaxTheme.colors
    val shape = RoundedCornerShape(Corners.control)

    Card(
        onClick = onClick,
        shape = CardDefaults.shape(shape),
        colors = CardDefaults.colors(
            containerColor = if (accent) colors.surfaceRaised else colors.surface,
            focusedContainerColor = colors.surfaceRaised,
        ),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, colors.accent), shape = shape),
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(elevationColor = colors.focusGlow, elevation = 10.dp),
        ),
        scale = CardDefaults.scale(focusedScale = 1.03f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) { content() }
    }
}
