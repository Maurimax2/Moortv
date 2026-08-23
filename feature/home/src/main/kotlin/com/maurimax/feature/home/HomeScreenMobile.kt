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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maurimax.core.designsystem.Artwork
import com.maurimax.core.designsystem.Brand
import com.maurimax.core.designsystem.Corners
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
        onRetry = viewModel::retry,
        onItemClick = onItemClick,
        modifier = modifier,
    )
}

@Composable
fun HomeScreenMobile(
    state: HomeUiState,
    onTabSelect: (CatalogTab) -> Unit = {},
    onRetry: () -> Unit = {},
    onItemClick: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brand.Ink),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Masthead(tab = state.tab, onTabSelect = onTabSelect)

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.loading -> CircularProgressIndicator(
                        color = Brand.Accent,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    state.error != null -> ErrorPanel(
                        message = state.error,
                        onRetry = onRetry,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    state.isEmpty -> Text(
                        text = "Nothing in ${state.tab.label.lowercase()} yet.",
                        color = Brand.TextSecondary,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    else -> Catalog(state = state, onItemClick = onItemClick)
                }
            }
        }
    }
}

@Composable
private fun Masthead(tab: CatalogTab, onTabSelect: (CatalogTab) -> Unit) {
    Column(
        modifier = Modifier
            .background(Brand.Ink)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
    ) {
        Text(
            text = "MAURIMAX",
            color = Brand.Accent,
            fontWeight = FontWeight.Black,
            fontSize = 21.sp,
            letterSpacing = 4.sp,
            modifier = Modifier.padding(start = Spacing.md, top = Spacing.md, bottom = Spacing.sm),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            modifier = Modifier.padding(horizontal = Spacing.md),
        ) {
            CatalogTab.entries.forEach { entry ->
                TabLabel(
                    label = entry.label,
                    selected = entry == tab,
                    onClick = { onTabSelect(entry) },
                )
            }
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
            color = if (selected) Brand.TextPrimary else Brand.TextTertiary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 15.sp,
            modifier = Modifier.padding(vertical = Spacing.sm),
        )
        AnimatedVisibility(visible = selected, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .width(24.dp)
                    .background(Brand.Accent, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun Catalog(state: HomeUiState, onItemClick: (MediaItem) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        contentPadding = PaddingValues(
            top = Spacing.md,
            bottom = Spacing.xl + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
    ) {
        items(state.rows, key = { it.title }) { row ->
            MobileRow(row = row, tab = state.tab, onItemClick = onItemClick)
        }
    }
}

@Composable
private fun MobileRow(row: ContentRow, tab: CatalogTab, onItemClick: (MediaItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = row.title,
            color = Brand.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
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
    val tileWidth = if (portrait) 116.dp else 148.dp
    val tileHeight = if (portrait) 174.dp else 84.dp

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
                .background(Brand.SurfaceRaised),
        ) {
            Artwork(
                url = item.artworkUrl,
                title = item.title,
                fallbackTint = item.artworkTint,
                crop = portrait,
                showFallbackLabel = false,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (item.progress > 0f) {
            LinearProgressIndicator(
                progress = { item.progress },
                color = Brand.Accent,
                trackColor = Brand.Outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
            )
        }

        Text(
            text = item.title,
            color = Brand.TextPrimary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = item.genre,
            color = Brand.TextTertiary,
            fontSize = 11.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = modifier.padding(Spacing.lg),
    ) {
        Text(text = message, color = Brand.TextSecondary, fontSize = 15.sp)
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Brand.Accent,
                contentColor = Brand.TextPrimary,
            ),
        ) {
            Text("Try again", fontWeight = FontWeight.SemiBold)
        }
    }
}
