package com.maurimax.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.R as DS
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.CatalogTab

/** The drawables live in the design system; this is just shorthand for them. */
private object Icons {
    val home = DS.drawable.ic_nav_home
    val movies = DS.drawable.ic_nav_movies
    val series = DS.drawable.ic_nav_series
    val profile = DS.drawable.ic_nav_profile
}

/** Where the bottom bar can go. Profile is not a catalogue section. */
sealed interface Destination {
    data class Section(val tab: CatalogTab) : Destination
    data object Profile : Destination
}

private data class NavItem(
    val destination: Destination,
    val icon: Int,
    val label: Int,
)

/**
 * The bottom bar.
 *
 * Four places, and each one is something the panel actually serves. There is no
 * sports tab: this panel has no sports section, its football is live channels
 * inside the live catalogue, and a tab that has to invent its own contents by
 * reading category names is a tab that will one day be empty.
 *
 * It sits on a gradient rather than a filled bar, so the catalogue runs
 * underneath it and the screen stays one surface instead of two stacked ones.
 */
@Composable
fun MaurimaxBottomBar(
    current: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    val items = remember {
        listOf(
            NavItem(Destination.Section(CatalogTab.LIVE), Icons.home, R.string.nav_home),
            NavItem(Destination.Section(CatalogTab.MOVIES), Icons.movies, R.string.nav_movies),
            NavItem(Destination.Section(CatalogTab.SERIES), Icons.series, R.string.nav_series),
            NavItem(Destination.Profile, Icons.profile, R.string.nav_profile),
        )
    }

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.35f to colors.ground.copy(alpha = 0.92f),
                    1f to colors.ground,
                ),
            )
            .padding(
                top = Spacing.md,
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + Spacing.sm,
            ),
    ) {
        items.forEach { item ->
            NavButton(
                item = item,
                selected = item.destination == current,
                onClick = { onSelect(item.destination) },
            )
        }
    }
}

@Composable
private fun NavButton(item: NavItem, selected: Boolean, onClick: () -> Unit) {
    val colors = MaurimaxTheme.colors
    val tint = if (selected) colors.textPrimary else colors.textTertiary

    // The selected mark fades rather than slides: five items in a row on a
    // narrow phone means the travel would be longer than the item is wide.
    val markAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "nav mark",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .height(3.dp)
                .size(width = 16.dp, height = 3.dp)
                .alpha(markAlpha)
                .background(colors.accent),
        )
        Image(
            painter = painterResource(item.icon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(tint),
            modifier = Modifier.size(23.dp),
        )
        androidx.compose.material3.Text(
            text = stringResource(item.label),
            color = tint,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
