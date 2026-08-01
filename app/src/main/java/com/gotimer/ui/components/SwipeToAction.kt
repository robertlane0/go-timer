package com.gotimer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Wraps [content] in a swipe-to-act container: swiping in [direction] fires
 * [onAction] once the dismiss threshold is crossed, then the content bounces
 * back to its resting position. Dragging the opposite way is disabled.
 *
 * @param hintText Label revealed behind the content while swiping.
 * @param direction The swipe direction that triggers the action; pass
 *   [SwipeToDismissBoxValue.StartToEnd] (right) or
 *   [SwipeToDismissBoxValue.EndToStart] (left).
 * @param onAction Invoked when the swipe crosses the threshold.
 */
@Composable
fun SwipeToAction(
    hintText: String,
    direction: SwipeToDismissBoxValue,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onAction()
            }
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = direction == SwipeToDismissBoxValue.StartToEnd,
        enableDismissFromEndToStart = direction == SwipeToDismissBoxValue.EndToStart,
        backgroundContent = {
            SwipeHint(
                text = hintText,
                direction = direction,
            )
        },
        content = content,
    )
}

/**
 * Tinted background revealed during the swipe, with the hint aligned toward
 * the swipe direction.
 */
@Composable
private fun SwipeHint(
    text: String,
    direction: SwipeToDismissBoxValue,
) {
    val containerColor = if (direction == SwipeToDismissBoxValue.EndToStart) {
        MaterialTheme.colorScheme.tertiaryContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (direction == SwipeToDismissBoxValue.EndToStart) {
        MaterialTheme.colorScheme.onTertiaryContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(containerColor)
            .padding(horizontal = 20.dp),
        contentAlignment = if (direction == SwipeToDismissBoxValue.EndToStart) {
            Alignment.CenterEnd
        } else {
            Alignment.CenterStart
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
    }
}
