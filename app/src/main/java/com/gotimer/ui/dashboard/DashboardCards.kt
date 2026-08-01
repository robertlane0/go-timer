package com.gotimer.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gotimer.ui.components.ActionButton
import com.gotimer.ui.components.CountdownCard
import com.gotimer.ui.components.ProgressCard
import com.gotimer.viewmodel.DashboardUiState
import com.gotimer.viewmodel.TimelineEvent

/**
 * High-contrast season countdown banner at the top of the dashboard.
 *
 * @param seasonName Active season title.
 * @param countdownText Remaining season time, e.g. `14d 06h 22m`.
 */
@Composable
fun SeasonHeroBanner(
    seasonName: String,
    countdownText: String,
    modifier: Modifier = Modifier,
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary,
        ),
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "\uD83C\uDFC6 ${seasonName.uppercase()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = "Ends in $countdownText",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

/**
 * Dice tracker card with capacity, progress, next refill, and full projection.
 */
@Composable
fun DiceTrackerCard(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier,
) {
    ProgressCard(
        title = "\uD83C\uDFB2 DICE REFILL TRACKER",
        current = uiState.currentDice,
        max = uiState.maxDice,
        progress = uiState.diceProgress,
        modifier = modifier,
    ) {
        Text(
            text = "Next Refill (+${uiState.refillRatePerHour}): ${uiState.nextRefillCountdownText}",
            style = MaterialTheme.typography.bodyLarge,
        )
        val projectionCountdown = uiState.fullProjectionCountdownText
        val projectionClock = uiState.fullProjectionClockText
        if (projectionCountdown != null && projectionClock != null) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Full in $projectionCountdown",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "At $projectionClock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Free Gift tracker card with status and instant claim action.
 *
 * @param onClaimInvoked "Claimed Just Now" handler.
 */
@Composable
fun GiftTrackerCard(
    uiState: DashboardUiState,
    onClaimInvoked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CountdownCard(
        title = "\uD83C\uDF81 FREE GIFT TRACKER",
        statusText = if (uiState.giftReady) {
            "${uiState.giftCountdownText} \uD83C\uDF89"
        } else {
            "Claimable in ${uiState.giftCountdownText}"
        },
        statusColor = if (uiState.giftReady) {
            MaterialTheme.colorScheme.primary
        } else {
            null
        },
        modifier = modifier,
    ) {
        ActionButton(
            label = "Claimed Just Now",
            onClick = onClaimInvoked,
        )
    }
}

/**
 * Chronological list of upcoming events: next dice refill, Free Gift,
 * full dice projection, and season end. Renders nothing when [events] is
 * empty. All text is pre-computed in the UI state layer.
 */
@Composable
fun TimelineCard(
    events: List<TimelineEvent>,
    modifier: Modifier = Modifier,
) {
    if (events.isEmpty()) {
        return
    }
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "UPCOMING",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            events.forEach { event ->
                TimelineRow(event = event)
            }
        }
    }
}

/**
 * One timeline row: label with a leading dot, clock time, and relative
 * countdown.
 */
@Composable
private fun TimelineRow(event: TimelineEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        )
        Text(
            text = event.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        Text(
            text = "At ${event.clockTimeText}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "· in ${event.countdownText}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
