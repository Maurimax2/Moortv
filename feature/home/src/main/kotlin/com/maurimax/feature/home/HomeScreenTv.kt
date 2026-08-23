@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.maurimax.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.maurimax.core.designsystem.Brand
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem

@Composable
fun HomeScreenTv(
    viewModel: HomeViewModel,
    onItemClick: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreenTv(state = state, onItemClick = onItemClick, modifier = modifier)
}

@Composable
fun HomeScreenTv(
    state: HomeUiState,
    onItemClick: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brand.Ink),
    ) {
        when (state) {
            HomeUiState.Loading -> Text(
                text = "Loading…",
                color = Brand.TextSecondary,
                modifier = Modifier.align(Alignment.Center),
            )

            is HomeUiState.Error -> Text(
                text = state.message,
                color = Brand.TextSecondary,
                modifier = Modifier.align(Alignment.Center),
            )

            is HomeUiState.Ready -> TvCatalog(rows = state.rows, onItemClick = onItemClick)
        }
    }
}

@Composable
private fun TvCatalog(rows: List<ContentRow>, onItemClick: (MediaItem) -> Unit) {
    // The first card of the first row takes focus on launch, so the remote has
    // somewhere to start. Without this a TV app opens with nothing focusable.
    val firstCard = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstCard.requestFocus() } }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        contentPadding = PaddingValues(
            start = Spacing.tvOverscan,
            end = Spacing.tvOverscan,
            top = Spacing.tvOverscan,
            bottom = Spacing.xl,
        ),
    ) {
        item { TvHero(rows.firstOrNull()?.items?.firstOrNull()) }

        itemsIndexed(rows) { rowIndex, row ->
            Column {
                Text(
                    text = row.title,
                    color = Brand.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = Spacing.sm),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    itemsIndexed(row.items) { itemIndex, item ->
                        TvPoster(
                            item = item,
                            onClick = { onItemClick(item) },
                            modifier = if (rowIndex == 0 && itemIndex == 0) {
                                Modifier.focusRequester(firstCard)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvHero(item: MediaItem?) {
    if (item == null) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(artworkBrush(item.artworkTint)),
    ) {
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.lg)) {
            Text(
                text = "MAURIMAX",
                color = Brand.Accent,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                letterSpacing = 4.sp,
            )
            Text(
                text = item.title,
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 44.sp,
            )
            Text(
                text = "${item.year} · ${item.genre} · ${item.durationMinutes} min",
                color = Brand.TextSecondary,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun TvPoster(item: MediaItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        scale = CardDefaults.scale(focusedScale = 1.1f),
        modifier = modifier.width(200.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(artworkBrush(item.artworkTint)),
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.sm),
            )
        }
    }
}
