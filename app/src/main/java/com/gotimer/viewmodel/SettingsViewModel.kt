package com.gotimer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotimer.model.UserPreferences
import com.gotimer.repository.DiceRepository
import com.gotimer.scheduler.NotificationRescheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the settings window.
 *
 * Exposes the persisted preferences and persists validated changes. After
 * every save the notification scheduler is re-armed, because capacity,
 * rates, season end, and lead times all affect the alarm plan.
 *
 * @param repository Persistence entry point.
 * @param notificationScheduler Re-arms alarms after settings change.
 */
class SettingsViewModel(
    private val repository: DiceRepository,
    private val notificationScheduler: NotificationRescheduler,
) : ViewModel() {

    /**
     * The current persisted preferences, mapped from the shared state flow.
     */
    val settings: StateFlow<UserPreferences> = repository.appState
        .map { it.settings }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = UserPreferences.defaults(System.currentTimeMillis()),
        )

    /**
     * Validates and persists [newPreferences], then re-arms all notification
     * alarms for the new configuration.
     */
    fun save(newPreferences: UserPreferences) {
        viewModelScope.launch {
            repository.saveSettings(newPreferences)
            notificationScheduler.rescheduleAll()
        }
    }

    private companion object {
        const val SUBSCRIBE_TIMEOUT_MILLIS = 5_000L
    }
}
