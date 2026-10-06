package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Interactive draggable Vertical scrollbar for LazyListState (Reader mode)
 * Remains passive by default, activates only during drag/scroll, and cleanly shifts back to passive mode when not in use.
 */
@Composable
fun LazyListVerticalScrollbar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
) {
    val coroutineScope = rememberCoroutineScope()
    val layoutInfo = listState.layoutInfo
    val totalItems = layoutInfo.totalItemsCount
    val visibleItems = layoutInfo.visibleItemsInfo.size

    if (totalItems > 0 && visibleItems < totalItems) {
        val firstVisible = listState.firstVisibleItemIndex
        val maxIndex = (totalItems - visibleItems).coerceAtLeast(1)
        val progress = (firstVisible.toFloat() / maxIndex).coerceIn(0f, 1f)
        val thumbFraction = (visibleItems.toFloat() / totalItems).coerceIn(0.08f, 0.85f)

        var isDragging by remember { mutableStateOf(false) }
        var isRecentlyActive by remember { mutableStateOf(false) }

        // Track list scrolling so scrollbar activates during movement, then shifts to passive
        val isScrollInProgress = listState.isScrollInProgress
        LaunchedEffect(isScrollInProgress, isDragging) {
            if (isScrollInProgress || isDragging) {
                isRecentlyActive = true
            } else {
                // Short cooldown then switch to passive mode
                delay(600)
                isRecentlyActive = false
            }
        }

        val thumbColor by animateColorAsState(
            targetValue = if (isDragging || isRecentlyActive) activeColor else inactiveColor,
            animationSpec = tween(durationMillis = 200),
            label = "thumb_color"
        )
        val thumbWidth by animateDpAsState(
            targetValue = if (isDragging) 8.dp else 4.dp,
            animationSpec = tween(durationMillis = 200),
            label = "thumb_width"
        )

        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(28.dp), // Generous touch target
            contentAlignment = Alignment.TopCenter
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val availableHeight = maxHeight
                val density = LocalDensity.current
                val availableHeightPx = with(density) { availableHeight.toPx() }
                val thumbHeight = availableHeight * thumbFraction
                val thumbHeightPx = with(density) { thumbHeight.toPx() }
                val trackHeightPx = (availableHeightPx - thumbHeightPx).coerceAtLeast(1f)
                val thumbOffset = (availableHeight - thumbHeight) * progress

                // Stable drag state
                val draggableState = rememberDraggableState { delta ->
                    val currentOffsetPx = progress * trackHeightPx
                    val newOffsetPx = (currentOffsetPx + delta).coerceIn(0f, trackHeightPx)
                    val newFraction = newOffsetPx / trackHeightPx
                    val targetIndex = (newFraction * maxIndex).toInt()
                    coroutineScope.launch {
                        listState.scrollToItem(targetIndex.coerceIn(0, totalItems - 1))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(totalItems, maxIndex) {
                            detectTapGestures { offset ->
                                val targetFraction = (offset.y / availableHeightPx).coerceIn(0f, 1f)
                                val targetIndex = (targetFraction * maxIndex).toInt()
                                coroutineScope.launch {
                                    listState.scrollToItem(targetIndex.coerceIn(0, totalItems - 1))
                                }
                            }
                        }
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Vertical,
                            onDragStarted = { isDragging = true },
                            onDragStopped = { isDragging = false }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp)
                            .offset(y = thumbOffset)
                            .height(thumbHeight)
                            .width(thumbWidth)
                            .clip(RoundedCornerShape(4.dp))
                            .background(thumbColor)
                    )
                }
            }
        }
    }
}

/**
 * Interactive draggable Vertical scrollbar for ScrollState (Editor mode)
 * Active while dragging/scrolling, shifts to passive mode when not in use.
 */
@Composable
fun VerticalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
) {
    val coroutineScope = rememberCoroutineScope()
    val maxScroll = scrollState.maxValue

    if (maxScroll > 0) {
        val currentScroll = scrollState.value
        val progress = (currentScroll.toFloat() / maxScroll).coerceIn(0f, 1f)
        var isDragging by remember { mutableStateOf(false) }
        var isRecentlyActive by remember { mutableStateOf(false) }

        val isScrollInProgress = scrollState.isScrollInProgress
        LaunchedEffect(isScrollInProgress, isDragging) {
            if (isScrollInProgress || isDragging) {
                isRecentlyActive = true
            } else {
                delay(600)
                isRecentlyActive = false
            }
        }

        val thumbColor by animateColorAsState(
            targetValue = if (isDragging || isRecentlyActive) activeColor else inactiveColor,
            animationSpec = tween(durationMillis = 200),
            label = "v_thumb_color"
        )
        val thumbWidth by animateDpAsState(
            targetValue = if (isDragging) 8.dp else 4.dp,
            animationSpec = tween(durationMillis = 200),
            label = "v_thumb_width"
        )

        Box(
            modifier = modifier
                .fillMaxHeight()
                .width(28.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val availableHeight = maxHeight
                val density = LocalDensity.current
                val availableHeightPx = with(density) { availableHeight.toPx() }
                val thumbHeight = (availableHeight * 0.18f).coerceAtLeast(36.dp)
                val thumbHeightPx = with(density) { thumbHeight.toPx() }
                val trackHeightPx = (availableHeightPx - thumbHeightPx).coerceAtLeast(1f)
                val thumbOffset = (availableHeight - thumbHeight) * progress

                val draggableState = rememberDraggableState { delta ->
                    val currentOffsetPx = progress * trackHeightPx
                    val newOffsetPx = (currentOffsetPx + delta).coerceIn(0f, trackHeightPx)
                    val newFraction = newOffsetPx / trackHeightPx
                    val targetScroll = (newFraction * maxScroll).toInt()
                    coroutineScope.launch {
                        scrollState.scrollTo(targetScroll.coerceIn(0, maxScroll))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(maxScroll) {
                            detectTapGestures { offset ->
                                val targetFraction = (offset.y / availableHeightPx).coerceIn(0f, 1f)
                                val targetScroll = (targetFraction * maxScroll).toInt()
                                coroutineScope.launch {
                                    scrollState.scrollTo(targetScroll.coerceIn(0, maxScroll))
                                }
                            }
                        }
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Vertical,
                            onDragStarted = { isDragging = true },
                            onDragStopped = { isDragging = false }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp)
                            .offset(y = thumbOffset)
                            .height(thumbHeight)
                            .width(thumbWidth)
                            .clip(RoundedCornerShape(4.dp))
                            .background(thumbColor)
                    )
                }
            }
        }
    }
}

/**
 * Interactive draggable Horizontal scrollbar for ScrollState (when text wrap is disabled)
 * Active while dragging/scrolling, shifts to passive mode when not in use.
 */
@Composable
fun HorizontalScrollbar(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
) {
    val coroutineScope = rememberCoroutineScope()
    val maxScroll = scrollState.maxValue

    if (maxScroll > 0) {
        val currentScroll = scrollState.value
        val progress = (currentScroll.toFloat() / maxScroll).coerceIn(0f, 1f)
        var isDragging by remember { mutableStateOf(false) }
        var isRecentlyActive by remember { mutableStateOf(false) }

        val isScrollInProgress = scrollState.isScrollInProgress
        LaunchedEffect(isScrollInProgress, isDragging) {
            if (isScrollInProgress || isDragging) {
                isRecentlyActive = true
            } else {
                delay(600)
                isRecentlyActive = false
            }
        }

        val thumbColor by animateColorAsState(
            targetValue = if (isDragging || isRecentlyActive) activeColor else inactiveColor,
            animationSpec = tween(durationMillis = 200),
            label = "h_thumb_color"
        )
        val thumbHeight by animateDpAsState(
            targetValue = if (isDragging) 8.dp else 4.dp,
            animationSpec = tween(durationMillis = 200),
            label = "h_thumb_height"
        )

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(28.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val availableWidth = maxWidth
                val density = LocalDensity.current
                val availableWidthPx = with(density) { availableWidth.toPx() }
                val thumbWidth = (availableWidth * 0.22f).coerceAtLeast(40.dp)
                val thumbWidthPx = with(density) { thumbWidth.toPx() }
                val trackWidthPx = (availableWidthPx - thumbWidthPx).coerceAtLeast(1f)
                val thumbOffset = (availableWidth - thumbWidth) * progress

                val draggableState = rememberDraggableState { delta ->
                    val currentOffsetPx = progress * trackWidthPx
                    val newOffsetPx = (currentOffsetPx + delta).coerceIn(0f, trackWidthPx)
                    val newFraction = newOffsetPx / trackWidthPx
                    val targetScroll = (newFraction * maxScroll).toInt()
                    coroutineScope.launch {
                        scrollState.scrollTo(targetScroll.coerceIn(0, maxScroll))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(maxScroll) {
                            detectTapGestures { offset ->
                                val targetFraction = (offset.x / availableWidthPx).coerceIn(0f, 1f)
                                val targetScroll = (targetFraction * maxScroll).toInt()
                                coroutineScope.launch {
                                    scrollState.scrollTo(targetScroll.coerceIn(0, maxScroll))
                                }
                            }
                        }
                        .draggable(
                            state = draggableState,
                            orientation = Orientation.Horizontal,
                            onDragStarted = { isDragging = true },
                            onDragStopped = { isDragging = false }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 4.dp)
                            .offset(x = thumbOffset)
                            .width(thumbWidth)
                            .height(thumbHeight)
                            .clip(RoundedCornerShape(4.dp))
                            .background(thumbColor)
                    )
                }
            }
        }
    }
}
