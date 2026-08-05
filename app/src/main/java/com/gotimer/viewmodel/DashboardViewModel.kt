package com.gotimer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotimer.model.AppState
import com.gotimer.notifications.PersistentNotificationManager
import com.gotimer.repository.DiceRepository
import com.gotimer.scheduler.NotificationRescheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the dashboard screen.
 *
 * Combines the persisted state with a one-second tick so countdowns stay
 * fresh while the screen is visible. The tick flow only runs while the UI is
 * subscribed, so no work happens in the background. The tick re-anchors each
 * render at the current time, so a refill boundary rolls the countdown over
 * to the next hour instead of pinning at zero. All display math is delegated
 * to [DashboardUiState.from].
 *
 * @param repository State source and mutation target.
 * @param notificationScheduler Re-arms alarms after quick actions change timers.
 * @param persistentNotificationManager Updates the persistent status notification.
 * @param clock Time source, injectable for deterministic tests.
 */
class DashboardViewModel(
    private val repository: DiceRepository,
    private val notificationScheduler: NotificationRescheduler,
    private val persistentNotificationManager: PersistentNotificationManager? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /**
     * Reactive dashboard state, recomputed every second while collected.
     */
    val uiState: StateFlow<DashboardUiState> = combine(
        repository.appState,
        ticker(),
    ) { state, currentTime ->
        DashboardUiState.from(state, currentTime)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
        initialValue = DashboardUiState.from(
            AppState.defaults(clock()),
            clock(),
        ),
    )

    /**
     * Emits the current time immediately, then once per second while there
     * are subscribers.
     */
    private fun ticker(): Flow<Long> = flow {
        while (true) {
            emit(clock())
            delay(TICK_INTERVAL_MILLIS)
        }
    }

    /**
     * Executes the configured "Just Played" batch update and re-arms
     * notification alarms for the new timers. Also refreshes the persistent
     * status notification.
     */
    fun onJustPlayed() {
        viewModelScope.launch {
            repository.executeJustPlayedAction(clock())
            notificationScheduler.rescheduleAll(clock())
            persistentNotificationManager?.update()
        }
    }

    /**
     * Restarts the Free Gift 8-hour cycle ("Claimed Just Now") and re-arms
     * notification alarms. Also refreshes the persistent status notification.
     */
    fun onClaimFreeGift() {
        viewModelScope.launch {
            repository.claimFreeGift(clock())
            notificationScheduler.rescheduleAll(clock())
            persistentNotificationManager?.update()
        }
    }

    private companion object {
        const val TICK_INTERVAL_MILLIS = 1_000L
        const val SUBSCRIBE_TIMEOUT_MILLIS = 5_000L
    }
}
