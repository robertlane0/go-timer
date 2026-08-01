package com.gotimer.ui.widgets

import com.gotimer.calculations.CountdownFormatter
import com.gotimer.calculations.ProjectionCalculator
import com.gotimer.model.AppState

/**
 * Display values for the home screen widget, mapped from [AppState] at a
 * given [now]. Pure so it can be unit tested without Android dependencies.
 *
 * @property diceText Current and maximum dice, e.g. `32/80`.
 * @property fullProjectionText Full pool status, e.g. `Full in 4h 20m` or `Full!`.
 * @property nextRefillText Remaining time to the next refill, e.g. `20m 00s`.
 * @property giftText Remaining time to the Free Gift, or `Ready` when claimable.
 */
data class GoTimerWidgetModel(
    val diceText: String,
    val fullProjectionText: String,
    val nextRefillText: String,
    val giftText: String,
) {
    companion object {
        /** Short "ready" label used when the gift can be claimed. */
        const val GIFT_READY_TEXT = "Ready"

        /**
         * Maps [state] to widget display values anchored at [now]. Full
         * pool status uses the same projection rules as the dashboard.
         */
        fun from(state: AppState, now: Long): GoTimerWidgetModel {
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
            return GoTimerWidgetModel(
                diceText = "${state.currentDice}/${state.maxDice}",
                fullProjectionText = projectionEpoch?.let {
                    "Full in ${CountdownFormatter.formatCountdown((it - now).coerceAtLeast(0L))}"
                } ?: "Full!",
                nextRefillText = CountdownFormatter.formatCountdown(nextRefillRemaining),
                giftText = if (giftRemaining == 0L) {
                    GIFT_READY_TEXT
                } else {
                    CountdownFormatter.formatCountdown(giftRemaining)
                },
            )
        }
    }
}
