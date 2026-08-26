package com.maurimax.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maurimax.core.model.ContentRow
import com.maurimax.core.model.MediaItem

/**
 * A live channel list, for browsing and for switching mid-stream.
 *
 * The same component in both places on purpose: what you see while choosing a
 * channel and what you see while changing one should not be two different
 * lists with two different behaviours.
 *
 * One line per channel rather than two. Without EPG there is nothing true to
 * put on a second line — repeating the channel's own name under itself is
 * furniture, not information — and a single line puts half again as many
 * channels on the screen, which is what a list of thousands actually needs.
 *
 * Built on foundation rather than tv-material so one implementation serves a
 * remote and a thumb: focus draws the same state a press does.
 */
@Composable
fun ChannelList(
    groups: List<ContentRow>,
    selectedGroup: Int,
    onGroupSelect: (Int) -> Unit,
    onChannelClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    /** The channel on screen right now, drawn as playing. Null while browsing. */
    playingId: String? = null,
    onChannelFocus: (MediaItem) -> Unit = {},
    /** Puts the remote on the first channel when the list appears. */
    autoFocusFirst: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val group = groups.getOrNull(selectedGroup)
    val channels = group?.items.orEmpty()
    val listState = rememberLazyListState()
    val firstRow = remember { FocusRequester() }

    // Changing group starts at the top of the new one rather than wherever the
    // last one happened to be scrolled to.
    LaunchedEffect(selectedGroup) { listState.scrollToItem(0) }

    // Once, when there is finally something to focus. Not on every change:
    // pulling focus back to the top while someone is scrolling is worse than
    // not focusing at all.
    LaunchedEffect(autoFocusFirst, channels.isNotEmpty()) {
        if (autoFocusFirst && channels.isNotEmpty()) runCatching { firstRow.requestFocus() }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = modifier,
    ) {
        if (groups.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                contentPadding = contentPadding,
            ) {
                itemsIndexed(groups, key = { index, row -> "$index-${row.title}" }) { index, row ->
                    GroupChip(
                        title = row.title,
                        count = row.items.size,
                        selected = index == selectedGroup,
                        onClick = { onGroupSelect(index) },
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(1.dp),
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
                ChannelRow(
                    channel = channel,
                    playing = channel.id == playingId,
                    onClick = { onChannelClick(channel) },
                    onFocus = { onChannelFocus(channel) },
                    modifier = if (index == 0) Modifier.focusRequester(firstRow) else Modifier,
                )
            }
        }
    }
}

/**
 * A group, with how many channels are in it.
 *
 * A row of chips rather than a column down the side of the screen: a
 * permanent third column costs a quarter of the width for something touched
 * once a session.
 */
@Composable
private fun GroupChip(
    title: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaurimaxTheme.colors
    var focused by remember { mutableStateOf(false) }
    val active = selected || focused

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                when {
                    selected -> colors.primaryFill
                    focused -> colors.surfaceRaised
                    else -> colors.secondaryFill
                },
            )
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs + 2.dp),
    ) {
        Text(
            text = title,
            color = if (selected) colors.onPrimaryFill else if (active) colors.textPrimary else colors.textTertiary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = count.toString(),
            color = if (selected) colors.onPrimaryFill.copy(alpha = 0.55f) else colors.textTertiary,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ChannelRow(
    channel: MediaItem,
    playing: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaurimaxTheme.colors
    var focused by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        focused -> colors.surfaceRaised
                        playing -> colors.secondaryFill
                        else -> Color.Transparent
                    },
                )
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocus()
                }
                .focusable(interactionSource = remember { MutableInteractionSource() })
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        ) {
            // Monospaced so a column of numbers lines up as a column. A panel
            // that numbers nothing leaves this blank rather than showing zeros.
            if (channel.number > 0) {
                Text(
                    text = channel.number.toString(),
                    color = if (focused) colors.accentText else colors.textTertiary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    modifier = Modifier.width(40.dp),
                )
            }

            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 28.dp)
                    .clip(RoundedCornerShape(4.dp)),
            ) {
                Artwork(
                    url = channel.artworkUrl,
                    title = "",
                    kind = ArtworkKind.CHANNEL_LOGO,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Text(
                text = channel.title,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            // The one that is on. Nothing else on the row says so.
            if (playing) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.accent),
                )
            }
        }

        // A bar on the leading edge rather than a box around everything: on a
        // list this dense, an outline per row is a cage.
        if (focused) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(3.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.accent),
            )
        }
    }
}
