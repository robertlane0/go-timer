package com.gotimer.viewmodel

import com.gotimer.calculations.CountdownFormatter
import com.gotimer.calculations.ProjectionCalculator
import com.gotimer.model.AppState
import com.gotimer.util.TimeConstants

/**
 * One upcoming event in the dashboard timeline.
 *
 * @property label Human-readable event name.
 * @property epochMillis When the event occurs.
 * @property clockTimeText Clock time of the event, e.g. `8:30 PM`.
 * @property countdownText Remaining time, e.g. `20m`.
 */
data class TimelineEvent(
    val label: String,
    val epochMillis: Long,
    val clockTimeText: String,
    val countdownText: String,
)

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
 * @property timelineEvents Upcoming events sorted chronologically.
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
    val timelineEvents: List<TimelineEvent>,
) {

    companion object {
        /** Status text shown on the gift card when claimable. */
        const val GIFT_READY_TEXT = "READY TO CLAIM"

        /**
         * Maps [state] to display values anchored at [now]. All calculation
         * is delegated to the calculation engine; this function only wires
         * inputs to outputs.
         *
         * The refill epoch and dice count held in [state] are derived from the
         * stored baseline at the moment the state flow last emitted. Since that
         * flow only re-emits when DataStore changes, both are re-derived here at
         * [now] so a completed refill cycle rolls the countdown forward to the
         * next hourly boundary while the screen stays visible instead of
         * pinning at zero. The fixed gift and season timestamps are used as-is.
         */
        fun from(state: AppState, now: Long): DashboardUiState {
            val nextRefillEpoch = ProjectionCalculator.calculateNextRefillEpoch(
                state.nextRefillEpoch,
                now,
            )
            val currentDice = ProjectionCalculator.calculateEffectiveDice(
                currentDice = state.currentDice,
                maxDice = state.maxDice,
                hourlyRefillRate = state.refillRatePerHour,
                nextRefillEpoch = state.nextRefillEpoch,
                now = now,
            )
            val seasonRemaining = ProjectionCalculator.calculateSeasonRemainingMillis(
                state.seasonEndEpoch,
                now,
            )
            val nextRefillRemaining = (nextRefillEpoch - now).coerceAtLeast(0L)
            val giftRemaining = ProjectionCalculator.calculateGiftRemainingMillis(
                state.freeGiftEpoch,
                now,
            )
            val projectionEpoch = ProjectionCalculator.calculateProjectionEpoch(
                currentDice = currentDice,
                maxDice = state.maxDice,
                hourlyRefillRate = state.refillRatePerHour,
                nextRefillEpoch = nextRefillEpoch,
                now = now,
            )

            return DashboardUiState(
                seasonName = state.seasonName,
                seasonCountdownText = CountdownFormatter.formatCountdown(seasonRemaining),
                currentDice = currentDice,
                maxDice = state.maxDice,
                refillRatePerHour = state.refillRatePerHour,
                diceProgress = ProjectionCalculator.calculateProgress(currentDice, state.maxDice),
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
                timelineEvents = buildTimeline(
                    state = state,
                    nextRefillEpoch = nextRefillEpoch,
                    projectionEpoch = projectionEpoch,
                    now = now,
                ),
            )
        }

        /**
         * Collects the upcoming events in chronological order, skipping any
         * that have already passed: next dice refill, Free Gift available,
         * full dice projection, and season end. Display strings are computed
         * here so the UI never performs calculations.
         */
        private fun buildTimeline(
            state: AppState,
            nextRefillEpoch: Long,
            projectionEpoch: Long?,
            now: Long,
        ): List<TimelineEvent> {
            fun event(label: String, epoch: Long): TimelineEvent = TimelineEvent(
                label = label,
                epochMillis = epoch,
                clockTimeText = CountdownFormatter.formatClockTime(epoch),
                countdownText = CountdownFormatter.formatCountdown((epoch - now).coerceAtLeast(0L)),
            )
            val candidates = listOfNotNull(
                event(TIMELINE_REFILL_LABEL, nextRefillEpoch)
                    .takeIf { it.epochMillis > now },
                event(TIMELINE_GIFT_LABEL, state.freeGiftEpoch)
                    .takeIf { it.epochMillis > now },
                projectionEpoch?.let { event(TIMELINE_DICE_FULL_LABEL, it) }
                    ?.takeIf { it.epochMillis > now },
                event(TIMELINE_SEASON_END_LABEL, state.seasonEndEpoch)
                    .takeIf { it.epochMillis > now },
            )
            return candidates.sortedBy { it.epochMillis }
        }

        private const val TIMELINE_REFILL_LABEL = "Next Dice Refill"
        private const val TIMELINE_GIFT_LABEL = "Free Gift Available"
        private const val TIMELINE_DICE_FULL_LABEL = "Dice Full"
        private const val TIMELINE_SEASON_END_LABEL = "Season End"
    }
}
