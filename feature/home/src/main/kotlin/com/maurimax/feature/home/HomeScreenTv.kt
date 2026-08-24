@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.maurimax.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Text
import com.maurimax.core.designsystem.Artwork
import com.maurimax.core.designsystem.Brand
import com.maurimax.core.designsystem.Corners
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.BrandLockup
import com.maurimax.core.designsystem.Scrims
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem

@Composable
fun HomeScreenTv(
    viewModel: HomeViewModel,
    onItemClick: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreenTv(
        state = state,
        onTabSelect = viewModel::selectTab,
        onRetry = viewModel::retry,
        onItemClick = onItemClick,
        modifier = modifier,
    )
}

@Composable
fun HomeScreenTv(
    state: HomeUiState,
    onTabSelect: (CatalogTab) -> Unit = {},
    onRetry: () -> Unit = {},
    onItemClick: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // The hero follows focus. On a TV the remote is the pointer, so what is
    // focused is what the customer is considering — showing it large is the
    // whole difference between a grid of thumbnails and a storefront.
    var spotlight by remember(state.tab) { mutableStateOf<MediaItem?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaurimaxTheme.colors.ground),
    ) {
        val hero = spotlight ?: state.rows.firstOrNull()?.items?.firstOrNull()
        if (hero != null) {
            Backdrop(item = hero)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            TvTabBar(
                tab = state.tab,
                onTabSelect = onTabSelect,
                modifier = Modifier.padding(
                    start = Spacing.tvOverscan,
                    top = Spacing.lg,
                    end = Spacing.tvOverscan,
                ),
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.loading -> Text(
                        text = stringResource(R.string.home_loading),
                        color = MaurimaxTheme.colors.textSecondary,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    state.failed -> TvErrorPanel(
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    state.isEmpty -> Text(
                        text = stringResource(R.string.home_empty),
                        color = MaurimaxTheme.colors.textSecondary,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    else -> TvCatalog(
                        state = state,
                        hero = hero,
                        onFocus = { spotlight = it },
                        onItemClick = onItemClick,
                    )
                }
            }
        }
    }
}

/** Full-bleed art for the focused title, washed out so rows stay readable over it. */
@Composable
private fun Backdrop(item: MediaItem) {
    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(targetState = item, label = "backdrop") { current ->
            Artwork(
                url = current.artworkUrl,
                title = "",
                fallbackTint = current.artworkTint,
                crop = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(Brand.HERO_ART_FRACTION),
            )
        }
        // Three washes. The first darkens the artwork itself so overlaid copy is
        // legible whatever the poster looks like — needed in light mode too. The
        // other two fade the art into this theme's ground.
        Box(modifier = Modifier.fillMaxSize().background(Scrims.onArtwork))
        Box(modifier = Modifier.fillMaxSize().background(Scrims.heroSide()))
        Box(modifier = Modifier.fillMaxSize().background(Scrims.heroFade()))
    }
}

@Composable
private fun TvTabBar(
    tab: CatalogTab,
    onTabSelect: (CatalogTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        BrandLockup(fontSize = 18.sp, markHeight = 26.dp, modifier = Modifier.padding(end = Spacing.lg))

        CatalogTab.entries.forEach { entry ->
            var focused by remember { mutableStateOf(false) }
            val selected = entry == tab

            Card(
                onClick = { onTabSelect(entry) },
                colors = CardDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = MaurimaxTheme.colors.surfaceRaised,
                ),
                shape = CardDefaults.shape(RoundedCornerShape(Corners.control)),
                border = CardDefaults.border(
                    focusedBorder = Border(
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaurimaxTheme.colors.accent),
                        shape = RoundedCornerShape(Corners.control),
                    ),
                ),
                scale = CardDefaults.scale(focusedScale = 1.02f),
                modifier = Modifier.onFocusChanged { focused = it.isFocused },
            ) {
                Text(
                    text = stringResource(entry.labelRes),
                    color = when {
                        focused || selected -> MaurimaxTheme.colors.textPrimary
                        else -> MaurimaxTheme.colors.textTertiary
                    },
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun TvCatalog(
    state: HomeUiState,
    hero: MediaItem?,
    onFocus: (MediaItem) -> Unit,
    onItemClick: (MediaItem) -> Unit,
) {
    val firstCard = remember(state.tab) { FocusRequester() }
    LaunchedEffect(state.tab, state.rows) {
        if (state.rows.isNotEmpty()) runCatching { firstCard.requestFocus() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (hero != null) {
            HeroCopy(
                item = hero,
                modifier = Modifier.padding(
                    start = Spacing.tvOverscan,
                    top = Spacing.sm,
                    end = Spacing.tvOverscan,
                ),
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = PaddingValues(
                start = Spacing.tvOverscan,
                end = Spacing.tvOverscan,
                top = Spacing.sm,
                bottom = Spacing.xl,
            ),
        ) {
            itemsIndexed(state.rows, key = { _, row -> row.title }) { rowIndex, row ->
                TvRow(
                    row = row,
                    tab = state.tab,
                    firstCard = firstCard.takeIf { rowIndex == 0 },
                    onFocus = onFocus,
                    onItemClick = onItemClick,
                )
            }
        }
    }
}

@Composable
private fun HeroCopy(item: MediaItem, modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(560.dp)) {
        Text(
            // On artwork, not on the page: white in both themes, over the scrim.
            text = item.title,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 34.sp,
            lineHeight = 38.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = heroLabel(item),
            color = MaurimaxTheme.colors.accentText,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = Spacing.xs),
        )
        if (item.description.isNotBlank()) {
            Text(
                text = item.description,
                color = MaurimaxTheme.colors.textSecondary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = Spacing.sm),
            )
        }
    }
}

@Composable
private fun TvRow(
    row: ContentRow,
    tab: CatalogTab,
    firstCard: FocusRequester?,
    onFocus: (MediaItem) -> Unit,
    onItemClick: (MediaItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = row.title,
            color = MaurimaxTheme.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            // A focused card scales to 1.08 and draws a border outside its bounds;
            // without room to grow it collides with the row title above it.
            contentPadding = PaddingValues(vertical = Spacing.sm),
        ) {
            itemsIndexed(row.items, key = { _, item -> item.id }) { index, item ->
                TvTile(
                    item = item,
                    tab = tab,
                    onFocus = onFocus,
                    onClick = { onItemClick(item) },
                    modifier = if (index == 0 && firstCard != null) {
                        Modifier.focusRequester(firstCard)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}

@Composable
private fun TvTile(
    item: MediaItem,
    tab: CatalogTab,
    onFocus: (MediaItem) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val portrait = tab.usesPortraitArt
    val tileWidth = if (portrait) 132.dp else 196.dp
    val tileHeight = if (portrait) 198.dp else 110.dp

    Card(
        onClick = onClick,
        scale = CardDefaults.scale(focusedScale = 1.08f),
        shape = CardDefaults.shape(RoundedCornerShape(Corners.card)),
        colors = CardDefaults.colors(
            containerColor = MaurimaxTheme.colors.surfaceRaised,
            focusedContainerColor = MaurimaxTheme.colors.surfaceRaised,
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(3.dp, MaurimaxTheme.colors.accent),
                shape = RoundedCornerShape(Corners.card),
            ),
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(elevationColor = MaurimaxTheme.colors.focusGlow, elevation = 12.dp),
        ),
        modifier = modifier
            .width(tileWidth)
            .onFocusChanged { if (it.isFocused) onFocus(item) },
    ) {
        Artwork(
            url = item.artworkUrl,
            title = item.title,
            fallbackTint = item.artworkTint,
            crop = portrait,
            fallbackTextSize = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
                .height(tileHeight),
        )
    }
}

@Composable
private fun TvErrorPanel(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.home_error),
            color = MaurimaxTheme.colors.textSecondary,
            fontSize = 17.sp,
        )
        Card(onClick = onRetry, scale = CardDefaults.scale(focusedScale = 1.05f)) {
            Text(
                text = stringResource(R.string.home_retry),
                color = MaurimaxTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        }
    }
}

/** Rating first on TV when the panel has one; it is the strongest signal there. */
@Composable
private fun heroLabel(item: MediaItem): String {
    val kind = stringResource(item.kind.labelRes)
    return if (item.rating.isBlank()) kind else "★ ${item.rating}  ·  $kind"
}
