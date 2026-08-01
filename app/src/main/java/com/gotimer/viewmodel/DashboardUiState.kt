package com.gotimer.viewmodel

import com.gotimer.calculations.CountdownFormatter
import com.gotimer.calculations.ProjectionCalculator
import com.gotimer.model.AppState
import com.gotimer.util.TimeConstants

/**
 * Immutable presentation state for the dashboard screen.
 *
 * Every field is ready for display: countdown strings, progress fraction,
 * and clock times. The mapping from [AppState] is pure so it can be unit
 * tested without a ViewModel.
 *
 * @property seasonName Active season banner title.
 * @property seasonCountdownText Season remaining as `14d 06h 22m`.
 * @property currentDice Current dice count.
 * @property maxDice Maximum dice capacity.
 * @property refillRatePerHour Dice generated per hour, shown on the refill line.
 * @property diceProgress Progress fraction in `[0.0, 1.0]`.
 * @property nextRefillCountdownText Time to the next refill, with seconds.
 * @property fullProjectionCountdownText Remaining time until full, or null when full.
 * @property fullProjectionClockText Clock time of full projection, or null when full.
 * @property giftCountdownText Free Gift countdown with seconds, or the ready text.
 * @property giftReady True when the Free Gift can be claimed now.
 */
data class DashboardUiState(
    val seasonName: String,
    val seasonCountdownText: String,
    val currentDice: Int,
    val maxDice: Int,
    val refillRatePerHour: Int,
    val diceProgress: Float,
    val nextRefillCountdownText: String,
    val fullProjectionCountdownText: String?,
    val fullProjectionClockText: String?,
    val giftCountdownText: String,
    val giftReady: Boolean,
) {

    companion object {
        /** Status text shown on the gift card when claimable. */
        const val GIFT_READY_TEXT = "READY TO CLAIM"

        /**
         * Maps [state] to display values anchored at [now]. All calculation
         * is delegated to the calculation engine; this function only wires
         * inputs to outputs.
         */
        fun from(state: AppState, now: Long): DashboardUiState {
            val seasonRemaining = ProjectionCalculator.calculateSeasonRemainingMillis(
                state.seasonEndEpoch,
                now,
            )
            val nextRefillRemaining = (state.nextRefillEpoch - now).coerceAtLeast(0L)
            val giftRemaining = ProjectionCalculator.calculateGiftRemainingMillis(
                state.freeGiftEpoch,
                now,
            )
            val projectionEpoch = ProjectionCalculator.calculateProjectionEpoch(
                currentDice = state.currentDice,
                maxDice = state.maxDice,
                hourlyRefillRate = state.refillRatePerHour,
                nextRefillEpoch = state.nextRefillEpoch,
                now = now,
            )

            return DashboardUiState(
                seasonName = state.seasonName,
                seasonCountdownText = CountdownFormatter.formatCountdown(seasonRemaining),
                currentDice = state.currentDice,
                maxDice = state.maxDice,
                refillRatePerHour = state.refillRatePerHour,
                diceProgress = ProjectionCalculator.calculateProgress(state.currentDice, state.maxDice),
                nextRefillCountdownText = CountdownFormatter.formatCountdown(nextRefillRemaining),
                fullProjectionCountdownText = projectionEpoch?.let {
                    CountdownFormatter.formatCountdown((it - now).coerceAtLeast(0L))
                },
                fullProjectionClockText = projectionEpoch?.let {
                    CountdownFormatter.formatClockTime(it)
                },
                giftCountdownText = if (giftRemaining == 0L) {
                    GIFT_READY_TEXT
                } else {
                    CountdownFormatter.formatCountdownWithSeconds(giftRemaining)
                },
                giftReady = giftRemaining == 0L,
            )
        }
    }
}
