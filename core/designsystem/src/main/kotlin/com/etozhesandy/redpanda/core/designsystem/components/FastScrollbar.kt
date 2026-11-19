package com.etozhesandy.redpanda.core.designsystem.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ThumbWidth = 28.dp
private val ThumbHeight = 56.dp
private const val HIDE_DELAY_MS = 1200L

/**
 * A draggable thumb along the right edge of a long grid, for crossing thousands of items at once.
 *
 * It shows itself while the grid scrolls or is being dragged and fades out afterwards, and takes no
 * touches while hidden, so it never gets between the reader and the tiles beneath it. While
 * dragging, a bubble beside the thumb reads out [label] for the item the thumb points at.
 *
 * [contentTopPadding] is the space the grid reserves above its first row, so the track starts
 * below the floating controls instead of beneath them.
 */
@Composable
fun FastScrollbar(
    state: LazyGridState,
    itemCount: Int,
    label: (index: Int) -> String,
    modifier: Modifier = Modifier,
    contentTopPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    if (itemCount <= 1) return
    val scope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var isRecentlyScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(state.isScrollInProgress) {
        if (state.isScrollInProgress) {
            isRecentlyScrolled = true
        } else {
            delay(HIDE_DELAY_MS)
            isRecentlyScrolled = false
        }
    }
    val isVisible = isDragging || isRecentlyScrolled

    BoxWithConstraints(modifier = modifier.fillMaxHeight().padding(top = contentTopPadding)) {
        val density = LocalDensity.current
        val travelPx = with(density) { (maxHeight - ThumbHeight).toPx() }.coerceAtLeast(1f)
        val scrollFraction = state.firstVisibleItemIndex / (itemCount - 1).toFloat()
        val fraction = (if (isDragging) dragFraction else scrollFraction).coerceIn(0f, 1f)
        val labelIndex = (fraction * (itemCount - 1)).roundToInt()

        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, (fraction * travelPx).roundToInt()) }
                    .width(ThumbWidth)
                    .height(ThumbHeight)
                    .pointerInput(itemCount, travelPx) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragFraction = scrollFraction
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                        ) { change, dy ->
                            change.consume()
                            dragFraction = (dragFraction + dy / travelPx).coerceIn(0f, 1f)
                            val target = (dragFraction * (itemCount - 1)).roundToInt()
                            scope.launch { state.scrollToItem(target) }
                        }
                    },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(ThumbHeight)
                        .padding(vertical = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            RoundedCornerShape(4.dp),
                        ),
                )
            }
        }
        if (isDragging) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = ThumbWidth + 8.dp)
                    .offset { IntOffset(0, (fraction * travelPx).roundToInt()) }
                    .height(ThumbHeight),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                ) {
                    Text(
                        text = label(labelIndex),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
