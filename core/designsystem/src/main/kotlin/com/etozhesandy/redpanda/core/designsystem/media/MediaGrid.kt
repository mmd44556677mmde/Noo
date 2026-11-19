package com.etozhesandy.redpanda.core.designsystem.media

import androidx.compose.ui.draw.clip
import androidx.compose.runtime.getValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import com.etozhesandy.redpanda.core.designsystem.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.model.Attachment

/** The square tile every media grid uses: quarter-gutter padding, full width, 1:1. */
val MediaGridTileModifier: Modifier = Modifier.padding(4.dp).fillMaxWidth().aspectRatio(1f)

/**
 * The square-tile gallery shared by the chat's media tabs and every media screen in a profile.
 *
 * The column count follows [imageWidthDp] rather than a fixed number: it is a user setting, so the
 * same grid is a wall of thumbnails or a handful of large tiles depending on what they picked.
 * [tile] receives [MediaGridTileModifier] — grids differ in what they draw per cell, never in the
 * shape of the cell itself.
 */
@Composable
fun <T> MediaGrid(
    items: List<T>,
    key: (T) -> Any,
    imageWidthDp: Int,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(4.dp),
    tile: @Composable LazyGridItemScope.(T, Modifier) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = imageWidthDp.dp),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        items(items, key = key) { item -> tile(item, MediaGridTileModifier) }
    }
}

/** [MediaGrid] over attachments, drawn with the shared [AttachmentThumbnail]. */
@Composable
fun MediaGrid(
    attachments: List<Attachment>,
    imageWidthDp: Int,
    onAttachmentClick: (Attachment) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(4.dp),
    markedId: String? = null,
    highlightedId: String? = null,
) {
    MediaGrid(
        items = attachments,
        key = { it.id },
        imageWidthDp = imageWidthDp,
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
    ) { attachment, tileModifier ->
        Box(modifier = tileModifier) {
            AttachmentThumbnail(
                attachment = attachment,
                onClick = { onAttachmentClick(attachment) },
                modifier = Modifier.matchParentSize(),
            )
            if (attachment.id == highlightedId) BlinkingFrame(Modifier.matchParentSize())
            if (attachment.id == markedId) {
                Icon(
                    Icons.Default.Bookmark,
                    contentDescription = stringResource(R.string.attachment_bookmarked),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp),
                )
            }
        }
    }
}

/**
 * A pulsing frame over a tile, to catch the eye when the grid has scrolled to it. It draws over the
 * picture without taking touches, so the tile stays tappable while it blinks.
 */
@Composable
private fun BlinkingFrame(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val alpha by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 350), RepeatMode.Reverse),
        label = "blinkAlpha",
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = alpha * 0.45f))
            .border(3.dp, color.copy(alpha = 0.3f + alpha * 0.7f), RoundedCornerShape(8.dp)),
    )
}
