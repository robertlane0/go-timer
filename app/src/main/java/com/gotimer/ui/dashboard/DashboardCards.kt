package com.gotimer.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
