package com.gotimer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gotimer.model.AppState
import com.gotimer.repository.DiceRepository
import com.gotimer.scheduler.NotificationRescheduler
import com.gotimer.util.InputValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the quick update sheet.
 *
 * Manages chip/preset selections and custom dice input, validating custom
 * values before they reach the repository. SAVE applies only the categories
 * the user touched, then re-arms notification alarms.
 *
 * @param repository Persistence entry point.
 * @param notificationScheduler Re-arms alarms after timers change.
 * @param clock Time source, injectable for deterministic tests.
 */
class UpdateViewModel(
    private val repository: DiceRepository,
    private val notificationScheduler: NotificationRescheduler,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val appState: StateFlow<AppState> = repository.appState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
            initialValue = AppState.defaults(System.currentTimeMillis()),
        )

    private val selection: MutableStateFlow<UpdateUiState> = MutableStateFlow(UpdateUiState())

    /**
     * Current selection state merged with persisted settings, so dice presets
     * always follow the configured capacity and refill rate.
     */
    val uiState: StateFlow<UpdateUiState> = combine(selection, appState) { current, state ->
        current.copy(
            maxDice = state.maxDice,
            dicePresets = dicePresets(state.maxDice, state.refillRatePerHour),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS),
        initialValue = UpdateUiState(),
    )

    /**
     * Selects the dice preset chip [count].
     */
    fun selectDice(count: Int) {
        selection.update { it.copy(selectedDice = count) }
    }

    /**
     * Selects the refill chip [minutes].
     */
    fun selectRefillMinutes(minutes: Int) {
        selection.update { it.copy(selectedRefillMinutes = minutes) }
    }

    /**
     * Selects the Free Gift chip [option].
     */
    fun selectGiftOption(option: GiftOption) {
        selection.update { it.copy(selectedGiftOption = option) }
    }

    /**
     * Parses [input] as a custom dice count and selects it, clamped into
     * `0..maxDice`. Unparseable input is ignored; the sheet never crashes.
     */
    fun onCustomDiceInput(input: String) {
        val parsed = input.trim().toIntOrNull() ?: return
        val clamped = InputValidator.clampDiceCount(parsed, uiState.value.maxDice)
        selection.update { it.copy(selectedDice = clamped) }
    }

    /**
     * Clears every selection, leaving the current state untouched on save.
     */
    fun clearSelection() {
        selection.update { it.copy(selectedDice = null, selectedRefillMinutes = null, selectedGiftOption = null) }
    }

    /**
     * Applies the selected categories to the persisted state and re-arms
     * notification alarms. Untouched categories are left alone.
     */
    fun save() {
        viewModelScope.launch {
            val selection = uiState.value
            val now = clock()
            selection.selectedDice?.let { repository.updateDiceCount(it, now) }
            selection.selectedRefillMinutes?.let { repository.resetRefillTimer(it, now) }
            selection.selectedGiftOption?.let {
                repository.resetFreeGiftTimer(it.hoursUntilClaimable, now)
            }
            notificationScheduler.rescheduleAll(now)
        }
    }

    /**
     * Builds the dice chips: zero, multiples of the hourly refill rate, and
     * the maximum capacity, de-duplicated. A non-positive rate degrades to
     * just zero and max.
     */
    private fun dicePresets(maxDice: Int, hourlyRefillRate: Int): List<Int> {
        if (maxDice <= 0) return emptyList()
        val multiples = if (hourlyRefillRate > 0) {
            (0..maxDice step hourlyRefillRate).toList()
        } else {
            listOf(0)
        }
        return (multiples + maxDice).distinct()
    }

    private companion object {
        const val SUBSCRIBE_TIMEOUT_MILLIS = 5_000L
    }
}
