package com.maurimax.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maurimax.core.designsystem.Artwork
import com.maurimax.core.designsystem.ArtworkKind
import com.maurimax.core.data.PortalFailure
import com.maurimax.core.data.messageRes
import com.maurimax.core.designsystem.Corners
import com.maurimax.core.designsystem.MaurimaxTheme
import com.maurimax.core.designsystem.BrandLockup
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.CatalogTab
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem

@Composable
fun HomeScreenMobile(
    viewModel: HomeViewModel,
    onItemClick: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreenMobile(
        state = state,
        onTabSelect = viewModel::selectTab,
        onQueryChange = viewModel::onQueryChange,
        onRetry = viewModel::retry,
        onItemClick = onItemClick,
        modifier = modifier,
    )
}

@Composable
fun HomeScreenMobile(
    state: HomeUiState,
    onTabSelect: (CatalogTab) -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onItemClick: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaurimaxTheme.colors.ground),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Masthead(
                tab = state.tab,
                query = state.query,
                onTabSelect = onTabSelect,
                onQueryChange = onQueryChange,
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.loading -> CircularProgressIndicator(
                        color = MaurimaxTheme.colors.accent,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    state.failure != null -> ErrorPanel(
                        failure = state.failure,
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    state.noResults -> Text(
                        text = stringResource(R.string.search_no_results, state.query),
                        color = MaurimaxTheme.colors.textSecondary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(Spacing.lg),
                    )

                    state.isEmpty -> Text(
                        text = stringResource(R.string.home_empty),
                        color = MaurimaxTheme.colors.textSecondary,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    else -> Catalog(state = state, onItemClick = onItemClick)
                }
            }
        }
    }
}

@Composable
private fun Masthead(
    tab: CatalogTab,
    query: String,
    onTabSelect: (CatalogTab) -> Unit,
    onQueryChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .background(MaurimaxTheme.colors.ground)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
    ) {
        BrandLockup(
            fontSize = 18.sp,
            markHeight = 26.dp,
            modifier = Modifier.padding(start = Spacing.md, top = Spacing.md, bottom = Spacing.sm),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            modifier = Modifier.padding(horizontal = Spacing.md),
        ) {
            CatalogTab.entries.forEach { entry ->
                TabLabel(
                    label = stringResource(entry.labelRes),
                    selected = entry == tab,
                    onClick = { onTabSelect(entry) },
                )
            }
        }

        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.padding(
                start = Spacing.md,
                end = Spacing.md,
                top = Spacing.sm,
                bottom = Spacing.sm,
            ),
        )
    }
}

/** Filters the section already on screen, so results appear as you type. */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Corners.control))
            .background(colors.surfaceRaised)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = colors.textPrimary, fontSize = 15.sp),
            cursorBrush = SolidColor(colors.accent),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_hint),
                        color = colors.textTertiary,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            },
            modifier = Modifier.weight(1f),
        )

        if (query.isNotEmpty()) {
            Text(
                text = stringResource(R.string.search_clear),
                color = colors.accentText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onQueryChange("") }
                    .padding(start = Spacing.sm),
            )
        }
    }
}

/**
 * An underline rather than a filled pill: the accent is spent on one thing at a
 * time, and artwork is what should carry colour on this screen.
 */
@Composable
private fun TabLabel(label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(bottom = Spacing.xs),
    ) {
        Text(
            text = label,
            color = if (selected) MaurimaxTheme.colors.textPrimary else MaurimaxTheme.colors.textTertiary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 15.sp,
            modifier = Modifier.padding(vertical = Spacing.sm),
        )
        AnimatedVisibility(visible = selected, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .width(24.dp)
                    .background(MaurimaxTheme.colors.accent, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun Catalog(state: HomeUiState, onItemClick: (MediaItem) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        contentPadding = PaddingValues(
            top = Spacing.sm,
            bottom = Spacing.xl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        // Only when nothing is being searched: during a search the answer is
        // the results, not a recommendation.
        val featured = if (state.query.isBlank()) {
            state.visibleRows.firstOrNull()?.items?.firstOrNull()
        } else {
            null
        }

        featured?.let { hero ->
            item(key = "hero-${hero.id}") {
                HeroBanner(
                    item = hero,
                    onPlay = onItemClick,
                    onOpen = onItemClick,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
            }
        }

        if (state.query.isBlank()) {
            val (resume, favourites) = state.personalFor(state.tab)

            if (resume.isNotEmpty()) {
                item(key = "row-resume") {
                    MobileRow(
                        row = ContentRow(stringResource(R.string.row_continue_watching), resume),
                        tab = state.tab,
                        onItemClick = onItemClick,
                    )
                }
            }
            if (favourites.isNotEmpty()) {
                item(key = "row-favourites") {
                    MobileRow(
                        row = ContentRow(stringResource(R.string.row_favourites), favourites),
                        tab = state.tab,
                        onItemClick = onItemClick,
                    )
                }
            }
        }

        items(state.visibleRows, key = { it.title }) { row ->
            MobileRow(row = row, tab = state.tab, onItemClick = onItemClick)
        }
    }
}

@Composable
private fun MobileRow(row: ContentRow, tab: CatalogTab, onItemClick: (MediaItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = row.title,
            color = MaurimaxTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(horizontal = Spacing.md),
        ) {
            items(row.items, key = { it.id }) { item ->
                MobileTile(item = item, tab = tab, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun MobileTile(item: MediaItem, tab: CatalogTab, onClick: () -> Unit) {
    val portrait = tab.usesPortraitArt
    val tileWidth = if (portrait) 112.dp else 132.dp
    val tileHeight = if (portrait) 168.dp else 76.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .width(tileWidth)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(tileHeight)
                .clip(RoundedCornerShape(Corners.tile))
                .background(MaurimaxTheme.colors.surfaceRaised),
        ) {
            Artwork(
                url = item.artworkUrl,
                title = item.title,
                kind = if (portrait) ArtworkKind.POSTER else ArtworkKind.CHANNEL_LOGO,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (item.progress > 0f) {
            LinearProgressIndicator(
                progress = { item.progress },
                color = MaurimaxTheme.colors.accent,
                trackColor = MaurimaxTheme.colors.outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
            )
        }

        Text(
            text = item.title,
            color = MaurimaxTheme.colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        itemLabel(item)?.let { label ->
            Text(
                text = label,
                color = MaurimaxTheme.colors.textTertiary,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ErrorPanel(
    failure: PortalFailure,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = modifier.padding(Spacing.lg),
    ) {
        Text(
            text = failureMessage(failure),
            color = MaurimaxTheme.colors.textSecondary,
            fontSize = 15.sp,
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(Corners.control),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaurimaxTheme.colors.accent,
                contentColor = MaurimaxTheme.colors.textPrimary,
            ),
        ) {
            Text(stringResource(R.string.home_retry), fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * The caption under a tile: a rating when the panel supplies a real one.
 *
 * Panels send "0" for anything unrated, and printing "★ 0" on half a catalogue
 * makes every title look badly reviewed rather than simply unscored.
 */
@Composable
private fun itemLabel(item: MediaItem): String? {
    val score = item.rating.trim().toDoubleOrNull()
    if (score == null || score <= 0.0) return null
    return "★ ${item.rating.trim()}"
}

/** The specific reason the catalogue could not load, in the customer's language. */
@Composable
private fun failureMessage(failure: PortalFailure): String =
    if (failure is PortalFailure.Inactive) {
        stringResource(failure.messageRes, failure.status)
    } else {
        stringResource(failure.messageRes)
    }
