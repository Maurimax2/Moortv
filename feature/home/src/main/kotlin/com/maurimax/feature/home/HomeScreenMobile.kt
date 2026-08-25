package com.maurimax.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maurimax.core.data.PortalFailure
import com.maurimax.core.data.messageRes
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.R as DS
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem

@Composable
fun HomeScreenMobile(
    viewModel: HomeViewModel,
    onItemClick: (MediaItem) -> Unit = {},
    account: String = "",
    onSwitchAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreenMobile(
        state = state,
        onQueryChange = viewModel::onQueryChange,
        onRetry = viewModel::retry,
        onItemClick = onItemClick,
        account = account,
        onSwitchAccount = onSwitchAccount,
        modifier = modifier,
    )
}

/**
 * The catalogue.
 *
 * One scrolling surface: the artwork runs under the top bar and under the
 * navigation, both of which sit on gradients rather than filled bars, so the
 * screen reads as a single page rather than a strip of chrome above a list and
 * another below it.
 */
@Composable
fun HomeScreenMobile(
    state: HomeUiState,
    onQueryChange: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onItemClick: (MediaItem) -> Unit = {},
    account: String = "",
    onSwitchAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    var searching by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground),
    ) {
        when {
            state.loading -> LoadingCatalogue(state.tab)

            state.failure != null && state.rows.isEmpty() ->
                Failure(state.failure, state.playableDownloads, onRetry, onItemClick)

            state.isEmpty -> EmptySection(state.tab)

            else -> Catalogue(state = state, onItemClick = onItemClick)
        }

        TopBar(
            query = state.query,
            searching = searching,
            onSearchToggle = {
                searching = !searching
                if (!searching) onQueryChange("")
            },
            onQueryChange = onQueryChange,
            account = account,
            onAccountClick = onSwitchAccount,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun Catalogue(state: HomeUiState, onItemClick: (MediaItem) -> Unit) {
    val rows = state.railsFor()
    val hero = if (state.query.isBlank()) state.heroItem() else null

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        contentPadding = PaddingValues(
            // Rails clear the navigation; the hero deliberately does not clear
            // the top bar, which floats over its artwork.
            bottom = 96.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        if (hero != null) {
            item(key = "hero") {
                Hero(item = hero, onPlay = onItemClick, onOpen = onItemClick)
            }
        } else {
            item(key = "top-space") {
                Spacer(
                    Modifier.height(
                        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp,
                    ),
                )
            }
        }

        itemsIndexed(rows, key = { index, row -> "$index-${row.title}" }) { _, row ->
            Rail(row = row, onItemClick = onItemClick)
        }

        if (state.noResults) {
            item(key = "no-results") {
                Text(
                    text = stringResource(R.string.search_no_results, state.query),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaurimaxTheme.colors.textSecondary,
                    modifier = Modifier.padding(Spacing.lg),
                )
            }
        }
    }
}

/**
 * The top bar.
 *
 * Nothing but the mark until it is needed: search expands in place rather than
 * pushing the artwork down, because a permanent search field at the top of a
 * catalogue is a field nobody uses taking the best space on the screen.
 */
@Composable
private fun TopBar(
    query: String,
    searching: Boolean,
    onSearchToggle: () -> Unit,
    onQueryChange: (String) -> Unit,
    account: String,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Spacing.md,
                end = Spacing.md,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + Spacing.sm,
                bottom = Spacing.sm,
            ),
    ) {
        Image(
            painter = painterResource(DS.drawable.ic_mark),
            contentDescription = "MAURIMAX",
            modifier = Modifier.height(28.dp),
        )

        AnimatedVisibility(
            visible = searching,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.weight(1f),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .padding(horizontal = Spacing.md),
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = colors.textPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(colors.accent),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (!searching) Spacer(Modifier.weight(1f))

        Image(
            painter = painterResource(DS.drawable.ic_search),
            contentDescription = stringResource(R.string.search_hint),
            colorFilter = ColorFilter.tint(colors.textPrimary),
            modifier = Modifier
                .size(22.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSearchToggle,
                ),
        )

        if (account.isNotBlank()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.identity)
                    .clickable(onClick = onAccountClick),
            ) {
                Text(
                    text = account.take(1).uppercase(),
                    color = colors.textPrimary,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

/** Shapes where the artwork will be, so nothing jumps when the rails land. */
@Composable
private fun LoadingCatalogue(tab: CatalogTab) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = Spacing.md)
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaurimaxTheme.colors.surface),
        )
        repeat(2) { RailSkeleton(portrait = tab.usesPortraitArt) }
    }
}

@Composable
private fun EmptySection(tab: CatalogTab) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(
                if (tab == CatalogTab.SPORTS) R.string.sports_empty else R.string.home_empty,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaurimaxTheme.colors.textSecondary,
            modifier = Modifier.align(Alignment.Center).padding(Spacing.lg),
        )
    }
}

/**
 * A failure with whatever is already on the device underneath it.
 *
 * Being unable to reach the panel is exactly when a download earns its keep,
 * so this screen leads to them rather than dead-ending.
 */
@Composable
private fun Failure(
    failure: PortalFailure,
    downloads: List<MediaItem>,
    onRetry: () -> Unit,
    onItemClick: (MediaItem) -> Unit,
) {
    val colors = MaurimaxTheme.colors

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 96.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
        ) {
            Text(
                text = if (failure is PortalFailure.Inactive) {
                    stringResource(failure.messageRes, failure.status)
                } else {
                    stringResource(failure.messageRes)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.primaryFill)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = Spacing.xl),
            ) {
                Text(
                    text = stringResource(R.string.home_retry),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimaryFill,
                )
            }
        }

        if (downloads.isNotEmpty()) {
            Text(
                text = stringResource(R.string.downloads_offline),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = Spacing.md),
            )
            Rail(
                row = ContentRow(stringResource(R.string.row_downloads), downloads),
                onItemClick = onItemClick,
            )
        }
    }
}

/**
 * What this tab shows, in order.
 *
 * Kept titles first, then what was left unfinished, then what was starred,
 * then the panel's own categories. Someone who downloaded a film or stopped
 * halfway through one is reaching for it, not browsing — and during a search
 * all of that is dropped, because the answer to a search is the results.
 */
@Composable
private fun HomeUiState.railsFor(): List<ContentRow> {
    val downloadsTitle = stringResource(R.string.row_downloads)
    val resumeTitle = stringResource(R.string.row_continue_watching)
    val favouritesTitle = stringResource(R.string.row_favourites)

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
 * The title the screen opens on.
 *
 * The first thing in the first catalogue rail rather than anything personal:
 * the hero is the section introducing itself, and leading with something the
 * customer has already watched makes the whole screen look like a history page.
 */
private fun HomeUiState.heroItem(): MediaItem? = visibleRows.firstOrNull()?.items?.firstOrNull()
