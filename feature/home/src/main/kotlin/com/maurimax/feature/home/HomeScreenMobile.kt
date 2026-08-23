package com.maurimax.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maurimax.core.designsystem.Brand
import com.maurimax.core.designsystem.Spacing
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem

@Composable
fun HomeScreenMobile(
    viewModel: HomeViewModel,
    onItemClick: (MediaItem) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreenMobile(state = state, onItemClick = onItemClick, modifier = modifier)
}

@Composable
fun HomeScreenMobile(
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
            HomeUiState.Loading -> CircularProgressIndicator(
                color = Brand.Accent,
                modifier = Modifier.align(Alignment.Center),
            )

            is HomeUiState.Error -> Text(
                text = state.message,
                color = Brand.TextSecondary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(Spacing.lg),
            )

            is HomeUiState.Ready -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                contentPadding = PaddingValues(bottom = Spacing.xl),
            ) {
                item { Wordmark() }
                items(state.rows, key = { it.title }) { row ->
                    MobileRow(row = row, onItemClick = onItemClick)
                }
            }
        }
    }
}

@Composable
private fun Wordmark() {
    Text(
        text = "MAURIMAX",
        color = Brand.Accent,
        fontWeight = FontWeight.Black,
        fontSize = 24.sp,
        letterSpacing = 3.sp,
        modifier = Modifier.padding(start = Spacing.md, top = Spacing.xl, bottom = Spacing.sm),
    )
}

@Composable
private fun MobileRow(row: ContentRow, onItemClick: (MediaItem) -> Unit) {
    Column {
        Text(
            text = row.title,
            style = MaterialTheme.typography.titleMedium,
            color = Brand.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = PaddingValues(horizontal = Spacing.md),
        ) {
            items(row.items, key = { it.id }) { item ->
                MobilePoster(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
private fun MobilePoster(item: MediaItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(132.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(196.dp)
                .clip(RoundedCornerShape(Spacing.sm))
                .background(artworkBrush(item.artworkTint)),
        ) {
            Text(
                text = item.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(Spacing.sm),
            )
        }
        if (item.progress > 0f) {
            LinearProgressIndicator(
                progress = { item.progress },
                color = Brand.Accent,
                trackColor = Brand.Outline,
                modifier = Modifier
                    .padding(top = Spacing.xs)
                    .fillMaxWidth()
                    .height(3.dp),
            )
        }
        Text(
            text = "${item.year} · ${item.genre}",
            color = Brand.TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

/**
 * Placeholder artwork until real posters land in M5: a vertical wash of the
 * title's tint so rows still read as distinct blocks.
 */
internal fun artworkBrush(tint: Long): Brush {
    val base = Color(tint)
    return Brush.verticalGradient(
        listOf(
            base.copy(alpha = 0.95f),
            base.copy(alpha = 0.55f),
            Color.Black.copy(alpha = 0.85f),
        ),
    )
}
