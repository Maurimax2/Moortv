@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.maurimax.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import androidx.tv.material3.Text
import com.maurimax.core.data.PortalFailure
import com.maurimax.core.data.messageRes
import com.maurimax.core.designsystem.Artwork
import com.maurimax.core.designsystem.ArtworkKind
import com.maurimax.core.designsystem.Brand
import com.maurimax.core.designsystem.BrandLockup
import com.maurimax.core.designsystem.Corners
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.Scrims
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.designsystem.badgeRes
import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.Sports
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem
import com.maurimax.core.designsystem.R as DS

/** How many titles a TV rail shows. Travel on a remote is slow. */
private const val TV_RAIL_PREVIEW = 25

@Composable
fun HomeScreenTv(
    viewModel: HomeViewModel,
    onItemClick: (MediaItem) -> Unit = {},
    account: String = "",
    onAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreenTv(
        state = state,
        onTabSelect = viewModel::selectTab,
        onRetry = viewModel::retry,
        onItemClick = onItemClick,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::clearQuery,
        account = account,
        onAccount = onAccount,
        modifier = modifier,
    )
}

/**
 * The ten-foot catalogue.
 *
 * Built around focus rather than around a pointer: the artwork behind
 * everything is whatever the remote is currently on, so moving the D-pad
 * changes the whole screen rather than just outlining a tile. That is the
 * difference between browsing and scrolling, and it is what a TV interface has
 * that a phone does not.
 */
@Composable
fun HomeScreenTv(
    state: HomeUiState,
    onTabSelect: (CatalogTab) -> Unit = {},
    onRetry: () -> Unit = {},
    onItemClick: (MediaItem) -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onClearQuery: () -> Unit = {},
    account: String = "",
    onAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    var spotlight by remember(state.tab) { mutableStateOf<MediaItem?>(null) }
    var searchOpen by remember(state.tab) { mutableStateOf(false) }

    val rows = state.tvRows()
    val hero = spotlight ?: rows.firstOrNull()?.items?.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground),
    ) {
        // Key art only. A channel's logo is a small mark on black, and blowing
        // one up to fill two thirds of a television is a blurred rectangle
        // behind the whole interface — charcoal is better than that.
        if (hero != null && !hero.isLive && hero.artworkUrl.isNotBlank()) Backdrop(hero)

        Column(modifier = Modifier.fillMaxSize()) {
            TvTabBar(
                tab = state.tab,
                onTabSelect = onTabSelect,
                searchOpen = searchOpen,
                query = state.query,
                onSearchToggle = {
                    searchOpen = !searchOpen
                    if (!searchOpen) onClearQuery()
                },
                onQueryChange = onQueryChange,
                account = account,
                onAccount = onAccount,
                modifier = Modifier.padding(
                    start = Spacing.tvOverscan,
                    end = Spacing.tvOverscan,
                    top = Spacing.lg,
                ),
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    // A search replaces the catalogue rather than filtering it
                    // in place, and only ever searches the section on screen.
                    state.searching -> if (state.noResults) {
                        Text(
                            text = stringResource(R.string.search_no_results, state.query),
                            color = colors.textSecondary,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        TvResults(
                            items = state.searchResults,
                            onFocus = { spotlight = it },
                            onItemClick = onItemClick,
                        )
                    }

                    state.loading -> Text(
                        text = stringResource(R.string.home_loading),
                        color = colors.textSecondary,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    state.failure != null -> TvOfflinePanel(
                        failure = state.failure,
                        downloads = state.playableDownloads,
                        onRetry = onRetry,
                        onItemClick = onItemClick,
                    )

                    rows.isEmpty() -> Text(
                        text = stringResource(R.string.home_empty),
                        color = colors.textSecondary,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    else -> TvCatalog(
                        rows = rows,
                        tab = state.tab,
                        hero = hero,
                        total = state.total,
                        catalogueRows = state.visibleRows.size,
                        loading = state.refreshing || state.loading,
                        onFocus = { spotlight = it },
                        onItemClick = onItemClick,
                    )
                }
            }
        }
    }
}

/** Full-bleed art for whatever the remote is on, faded into the page. */
@Composable
private fun Backdrop(item: MediaItem) {
    Box(modifier = Modifier.fillMaxSize()) {
        Crossfade(
            targetState = item,
            animationSpec = tween(durationMillis = 320),
            label = "backdrop",
        ) { current ->
            Artwork(
                url = current.artworkUrl,
                title = "",
                kind = ArtworkKind.POSTER,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(Brand.HERO_ART_FRACTION),
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(Scrims.heroSide()))
        Box(modifier = Modifier.fillMaxSize().background(Scrims.heroFade()))
    }
}

@Composable
private fun TvTabBar(
    tab: CatalogTab,
    onTabSelect: (CatalogTab) -> Unit,
    searchOpen: Boolean,
    query: String,
    onSearchToggle: () -> Unit,
    onQueryChange: (String) -> Unit,
    account: String,
    onAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        BrandLockup(fontSize = 18.sp, markHeight = 26.dp, modifier = Modifier.padding(end = Spacing.lg))

        CatalogTab.entries.forEach { entry ->
            var focused by remember { mutableStateOf(false) }
            val selected = entry == tab

            Card(
                onClick = { onTabSelect(entry) },
                shape = CardDefaults.shape(RoundedCornerShape(Corners.control)),
                colors = CardDefaults.colors(
                    containerColor = if (selected) colors.surfaceRaised else Color.Transparent,
                    focusedContainerColor = colors.surfaceRaised,
                ),
                border = CardDefaults.border(
                    border = if (selected) {
                        Border(BorderStroke(2.dp, colors.accent), shape = RoundedCornerShape(Corners.control))
                    } else {
                        Border.None
                    },
                    focusedBorder = Border(
                        BorderStroke(2.dp, colors.accent),
                        shape = RoundedCornerShape(Corners.control),
                    ),
                ),
                scale = CardDefaults.scale(focusedScale = 1.04f),
                modifier = Modifier.onFocusChanged { focused = it.isFocused },
            ) {
                Text(
                    text = stringResource(entry.labelRes),
                    color = if (focused || selected) colors.textPrimary else colors.textTertiary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                )
            }
        }

        Box(modifier = Modifier.weight(1f))

        // Search sits before the account pill, so the D-pad reaches the thing
        // that is used constantly before the thing that is used twice a year.
        TvSearchControl(
            open = searchOpen,
            query = query,
            hint = stringResource(R.string.search_in, stringResource(tab.labelRes)),
            onToggle = onSearchToggle,
            onQueryChange = onQueryChange,
        )

        // The line in use sits at the far end of the bar, where a D-pad reaches
        // it by travelling past the tabs rather than through them.
        if (account.isNotBlank()) {
            Card(
                onClick = onAccount,
                shape = CardDefaults.shape(RoundedCornerShape(50)),
                colors = CardDefaults.colors(
                    containerColor = colors.identity,
                    focusedContainerColor = colors.identity,
                ),
                border = CardDefaults.border(
                    focusedBorder = Border(
                        BorderStroke(3.dp, colors.accent),
                        shape = RoundedCornerShape(50),
                    ),
                ),
                scale = CardDefaults.scale(focusedScale = 1.08f),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(38.dp),
                ) {
                    Text(
                        text = account.take(1).uppercase(),
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Composable
private fun TvCatalog(
    rows: List<ContentRow>,
    tab: CatalogTab,
    hero: MediaItem?,
    total: Int,
    catalogueRows: Int,
    loading: Boolean,
    onFocus: (MediaItem) -> Unit,
    onItemClick: (MediaItem) -> Unit,
) {
    val firstCard = remember(tab) { FocusRequester() }
    // Keyed on the tab alone: the library refreshes when the customer comes
    // back from watching something, and pulling focus back to the first card
    // then would throw away where they were on the remote.
    LaunchedEffect(tab) {
        if (rows.isNotEmpty()) runCatching { firstCard.requestFocus() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (hero != null) {
            HeroCopy(
                item = hero,
                modifier = Modifier.padding(
                    start = Spacing.tvOverscan,
                    end = Spacing.tvOverscan,
                    top = Spacing.sm,
                ),
            )
        }

        // Across a room the count is the only thing that says whether this is
        // the whole catalogue or the start of it.
        Text(
            text = if (loading) {
                stringResource(R.string.count_summary_loading, total, catalogueRows)
            } else {
                stringResource(R.string.count_summary, total, catalogueRows)
            },
            color = MaurimaxTheme.colors.textTertiary,
            fontSize = 14.sp,
            modifier = Modifier.padding(
                start = Spacing.tvOverscan,
                end = Spacing.tvOverscan,
                top = Spacing.sm,
            ),
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            contentPadding = PaddingValues(
                start = Spacing.tvOverscan,
                end = Spacing.tvOverscan,
                top = Spacing.sm,
                bottom = Spacing.xl,
            ),
        ) {
            itemsIndexed(rows, key = { index, row -> "$index-${row.title}" }) { rowIndex, row ->
                TvRow(
                    row = row,
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
    val colors = MaurimaxTheme.colors
    val kind = stringResource(item.kind.labelRes)
    val score = item.rating.trim().toDoubleOrNull()

    Column(modifier = modifier.width(620.dp)) {
        Text(
            text = item.title,
            color = colors.textPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 36.sp,
            lineHeight = 40.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = Spacing.xs),
        ) {
            if (item.isLive) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(colors.accent, RoundedCornerShape(50)),
                )
            }
            Text(
                text = if (score != null && score > 0.0) "★ ${item.rating.trim()}  ·  $kind" else kind,
                color = colors.accentText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            if (item.progress > 0f) {
                Text(
                    text = "· ${(item.progress * 100).toInt()}%",
                    color = colors.textSecondary,
                    fontSize = 15.sp,
                )
            }
        }

        if (item.description.isNotBlank()) {
            Text(
                text = item.description,
                color = colors.textSecondary,
                fontSize = 15.sp,
                lineHeight = 22.sp,
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
    firstCard: FocusRequester?,
    onFocus: (MediaItem) -> Unit,
    onItemClick: (MediaItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        // A panel can hand back a row with no name, and a blank header is
        // worse than none.
        if (row.title.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Recognised across a room, which the words beside it are not.
                Sports.badge(row.title)?.let { league ->
                    Image(
                        painter = painterResource(league.badgeRes),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Text(
                    text = row.title,
                    color = MaurimaxTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Without the count there is no way to tell a category of nine
                // titles from one of nine hundred.
                Text(
                    text = row.items.size.toString(),
                    color = MaurimaxTheme.colors.textTertiary,
                    fontSize = 14.sp,
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            // Room for a focused card to scale and draw its border outside.
            contentPadding = PaddingValues(vertical = Spacing.sm),
        ) {
            itemsIndexed(row.items, key = { _, item -> item.id }) { index, item ->
                TvTile(
                    item = item,
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
    onFocus: (MediaItem) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    val portrait = !item.isLive
    val tileWidth = if (portrait) 132.dp else 196.dp
    val tileHeight = if (portrait) 198.dp else 110.dp

    Card(
        onClick = onClick,
        scale = CardDefaults.scale(focusedScale = 1.09f),
        shape = CardDefaults.shape(RoundedCornerShape(Corners.card)),
        colors = CardDefaults.colors(
            containerColor = colors.surfaceRaised,
            focusedContainerColor = colors.surfaceRaised,
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                BorderStroke(3.dp, colors.accent),
                shape = RoundedCornerShape(Corners.card),
            ),
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(elevationColor = colors.focusGlow, elevation = 14.dp),
        ),
        modifier = modifier
            .width(tileWidth)
            .onFocusChanged { if (it.isFocused) onFocus(item) },
    ) {
        Column {
            Artwork(
                url = item.artworkUrl,
                title = item.title,
                kind = if (portrait) ArtworkKind.POSTER else ArtworkKind.CHANNEL_LOGO,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(tileHeight),
            )
            // A resume bar on the tile itself, so a half-watched title is
            // recognisable without focusing it first.
            if (item.progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(colors.outline),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.progress)
                            .height(3.dp)
                            .background(colors.accent),
                    )
                }
            }
        }
    }
}

@Composable
private fun TvErrorPanel(
    failure: PortalFailure,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = modifier,
    ) {
        Text(
            text = if (failure is PortalFailure.Inactive) {
                stringResource(failure.messageRes, failure.status)
            } else {
                stringResource(failure.messageRes)
            },
            color = colors.textSecondary,
            fontSize = 17.sp,
        )
        Card(
            onClick = onRetry,
            scale = CardDefaults.scale(focusedScale = 1.06f),
            border = CardDefaults.border(
                focusedBorder = Border(BorderStroke(2.dp, colors.accent)),
            ),
        ) {
            Text(
                text = stringResource(R.string.home_retry),
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        }
    }
}

/**
 * What this tab shows on TV: what the customer left unfinished, then what they
 * starred, then the panel's own categories.
 *
 * Personal rows come first because a ten-foot interface opens where the remote
 * already is — the first card is focused on entry, so whatever sits there is
 * one OK away. During a search they are dropped: the answer to a search is the
 * results, not a recommendation.
 */
@Composable
private fun HomeUiState.tvRows(): List<ContentRow> {
    val resumeTitle = stringResource(R.string.row_continue_watching)
    val favouritesTitle = stringResource(R.string.row_favourites)
    val downloadsTitle = stringResource(R.string.row_downloads)
    return remember(tab, rows, resume, favourites, downloads, query) {
        buildList {
            if (query.isBlank()) {
                val (unfinished, starred) = personalFor(tab)
                val kept = downloadsFor(tab)
                if (kept.isNotEmpty()) add(ContentRow(downloadsTitle, kept))
                if (unfinished.isNotEmpty()) add(ContentRow(resumeTitle, unfinished))
                if (starred.isNotEmpty()) add(ContentRow(favouritesTitle, starred))
            }
            addAll(visibleRows)
        }
    }
}

/**
 * Search, on a remote.
 *
 * Closed it is one glyph in the bar; open it is a field that takes the
 * television's own keyboard. Nothing about it is global: it searches the
 * section on screen, because someone in Films looking for a title does not
 * want a channel called the same thing.
 */
@Composable
private fun TvSearchControl(
    open: Boolean,
    query: String,
    hint: String,
    onToggle: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    val colors = MaurimaxTheme.colors
    val field = remember { FocusRequester() }

    // Opening the field is only useful if the keyboard follows it.
    LaunchedEffect(open) { if (open) runCatching { field.requestFocus() } }

    if (!open) {
        Card(
            onClick = onToggle,
            shape = CardDefaults.shape(RoundedCornerShape(50)),
            colors = CardDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = colors.surfaceRaised,
            ),
            border = CardDefaults.border(
                focusedBorder = Border(
                    BorderStroke(2.dp, colors.accent),
                    shape = RoundedCornerShape(50),
                ),
            ),
            scale = CardDefaults.scale(focusedScale = 1.08f),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(38.dp)) {
                Image(
                    painter = painterResource(DS.drawable.ic_search),
                    contentDescription = hint,
                    colorFilter = ColorFilter.tint(colors.textPrimary),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier
            .width(360.dp)
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(colors.surfaceRaised)
            .padding(horizontal = Spacing.md),
    ) {
        Image(
            painter = painterResource(DS.drawable.ic_search),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.textTertiary),
            modifier = Modifier.size(18.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = colors.textPrimary, fontSize = 16.sp),
            cursorBrush = SolidColor(colors.accent),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = hint,
                        color = colors.textTertiary,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(field),
        )
    }
}

/** Search results, as a wall rather than a rail: there is no category here. */
@Composable
private fun TvResults(
    items: List<MediaItem>,
    onFocus: (MediaItem) -> Unit,
    onItemClick: (MediaItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(6),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        contentPadding = PaddingValues(
            start = Spacing.tvOverscan,
            end = Spacing.tvOverscan,
            top = Spacing.md,
            bottom = Spacing.xl,
        ),
    ) {
        items(items, key = { it.id }) { item ->
            TvTile(
                item = item,
                onFocus = onFocus,
                onClick = { onItemClick(item) },
            )
        }
    }
}

/**
 * A failure, with whatever is already on the box underneath it.
 *
 * The moment the panel is unreachable is the moment a download earns its
 * keep, so this screen has to lead to them rather than dead-end.
 */
@Composable
private fun TvOfflinePanel(
    failure: PortalFailure,
    downloads: List<MediaItem>,
    onRetry: () -> Unit,
    onItemClick: (MediaItem) -> Unit,
) {
    if (downloads.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            TvErrorPanel(
                failure = failure,
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.tvOverscan, vertical = Spacing.lg),
    ) {
        TvErrorPanel(failure = failure, onRetry = onRetry)
        Text(
            text = stringResource(R.string.downloads_offline),
            color = MaurimaxTheme.colors.textSecondary,
            fontSize = 16.sp,
        )
        TvRow(
            row = ContentRow(stringResource(R.string.row_downloads), downloads),
            firstCard = null,
            onFocus = {},
            onItemClick = onItemClick,
        )
    }
}
