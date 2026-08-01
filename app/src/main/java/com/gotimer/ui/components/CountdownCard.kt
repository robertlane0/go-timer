package com.gotimer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Container card with a title and a prominent status line, used for
 * countdown-style trackers such as the Free Gift card.
 *
 * @param title Card heading.
 * @param statusText Primary status text (e.g. a countdown or "READY TO CLAIM").
 * @param statusColor Optional color override for the status text.
 * @param content Extra rows rendered below the status.
 */
@Composable
fun CountdownCard(
    title: String,
    statusText: String,
    modifier: Modifier = Modifier,
    statusColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineMedium,
                color = statusColor ?: MaterialTheme.colorScheme.onSurface,
            )
            content()
        }
    }
}
