package com.gotimer.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gotimer.ui.components.ActionButton
import com.gotimer.ui.components.SwipeToAction
import com.gotimer.viewmodel.DashboardUiState

/**
 * Main dashboard screen: header, season banner, dice and gift cards, and the
 * sticky "Just Played" action bar. Stateless; every value comes from
 * [uiState] and every action through a callback.
 */
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onJustPlayed: () -> Unit,
    onClaimFreeGift: () -> Unit,
    onOpenUpdate: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            JustPlayedActionBar(
                onJustPlayed = onJustPlayed,
                modifier = Modifier.safeDrawingPadding(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppHeader(
                onOpenUpdate = onOpenUpdate,
                onOpenSettings = onOpenSettings,
            )
            SeasonHeroBanner(
                seasonName = uiState.seasonName,
                countdownText = uiState.seasonCountdownText,
            )
            SwipeToAction(
                hintText = "JUST PLAYED",
                direction = SwipeToDismissBoxValue.EndToStart,
                onAction = onJustPlayed,
            ) {
                DiceTrackerCard(uiState = uiState)
            }
            SwipeToAction(
                hintText = "CLAIMED",
                direction = SwipeToDismissBoxValue.StartToEnd,
                onAction = onClaimFreeGift,
            ) {
                GiftTrackerCard(
                    uiState = uiState,
                    onClaimInvoked = onClaimFreeGift,
                )
            }
            TimelineCard(events = uiState.timelineEvents)
        }
    }
}

/**
 * Top bar with the app title, UPDATE, and settings actions.
 */
@Composable
private fun AppHeader(
    onOpenUpdate: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "GO! Timer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenUpdate) {
            Text("UPDATE")
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Settings",
            )
        }
    }
}

/**
 * Sticky bottom action bar with the "Just Played" batch button.
 */
@Composable
private fun JustPlayedActionBar(
    onJustPlayed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        ActionButton(
            label = "\u26A1 JUST PLAYED",
            onClick = onJustPlayed,
        )
    }
}
