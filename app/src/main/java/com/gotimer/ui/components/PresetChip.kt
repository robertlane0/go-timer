package com.gotimer.ui.components

import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Tappable Material 3 selection chip used by the quick update sheet.
 *
 * @param label Chip text.
 * @param selected Whether the chip is currently selected.
 * @param onClick Invoked when the chip is tapped.
 */
@Composable
fun PresetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { androidx.compose.material3.Text(label) },
        modifier = modifier,
    )
}
