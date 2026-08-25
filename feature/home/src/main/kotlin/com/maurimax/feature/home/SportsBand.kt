package com.maurimax.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maurimax.core.designsystem.Brand
import com.maurimax.core.designsystem.Corners
import com.maurimax.core.designsystem.FootballRenders
import com.maurimax.core.designsystem.LeagueStrip
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.designsystem.badgeRes

/**
 * The header of the football section.
 *
 * The rest of the app is built out of whatever artwork the panel happens to
 * serve, which for live channels is a wall of small logos on black. This is the
 * one place with art of its own — a player and the competition badges — because
 * a section has to look like somewhere you arrived, not like the same rail with
 * a different filter on it.
 *
 * Aligned to the end rather than the right: in Arabic the whole layout mirrors,
 * and the copy has to stay on the side the reader starts from.
 */
@Composable
fun SportsBand(
    modifier: Modifier = Modifier,
    height: Dp = 148.dp,
    titleSize: Int = 26,
    badgeSize: Dp = 26.dp,
) {
    val colors = MaurimaxTheme.colors
    // One face per launch, so the section is not the same picture every time
    // and no single player becomes the app's mascot.
    val render = remember { FootballRenders.random() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(Corners.card))
            .background(
                Brush.horizontalGradient(
                    listOf(Brand.VioletDeep, Brand.Violet, Brand.OrangeDeep),
                ),
            ),
    ) {
        Image(
            painter = painterResource(render),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomEnd,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxHeight()
                .padding(start = Spacing.xl),
        )

        // A wash back toward the start side, so the copy never sits on a kit.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    Brush.horizontalGradient(
                        0f to Brand.VioletDeep.copy(alpha = 0.92f),
                        0.55f to Brand.VioletDeep.copy(alpha = 0.35f),
                        1f to Brand.VioletDeep.copy(alpha = 0f),
                    ),
                ),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(Spacing.md),
        ) {
            Text(
                text = stringResource(R.string.sports_band_title),
                color = colors.textPrimary,
                fontSize = titleSize.sp,
                lineHeight = (titleSize * 1.2f).sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
            )
            Text(
                text = stringResource(R.string.sports_band_subtitle),
                color = colors.textPrimary.copy(alpha = 0.85f),
                fontSize = (titleSize * 0.5f).sp,
                lineHeight = (titleSize * 0.75f).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.52f),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = Spacing.xs),
            ) {
                LeagueStrip.forEach { league ->
                    Image(
                        painter = painterResource(league.badgeRes),
                        contentDescription = null,
                        modifier = Modifier.size(badgeSize),
                    )
                }
            }
        }
    }
}
