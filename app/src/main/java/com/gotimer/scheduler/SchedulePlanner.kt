package com.gotimer.scheduler

import com.gotimer.calculations.ProjectionCalculator
import com.gotimer.model.AppState
import com.gotimer.util.TimeConstants

/**
 * The distinct notification event kinds the scheduler can arm.
 *
 * @property action Broadcast action used by the alarm PendingIntent and the receiver.
 * @property notificationId Stable id under which the posted notification appears.
 */
enum class NotificationType(val action: String, val notificationId: Int) {
    DICE_FULL("com.gotimer.action.DICE_FULL", 101),
    GIFT_READY("com.gotimer.action.GIFT_READY", 102),
    SEASON_24H("com.gotimer.action.SEASON_24H", 103),
    SEASON_1H("com.gotimer.action.SEASON_1H", 104),
}

/**
 * One planned alarm delivery.
 *
 * @property type Which notification will be shown.
 * @property triggerAtMillis Epoch milliseconds when the alarm should fire.
 */
data class ScheduledAlarm(
    val type: NotificationType,
    val triggerAtMillis: Long,
)

/**
 * Pure planner that decides which notification alarms to arm for a given
 * application state. No Android dependencies, so the scheduling math is fully
 * unit-testable.
 *
 * Rules:
 * - Dice Full fires at the projected full-pool epoch minus the lead time;
 *   skipped while the pool is already full or when the moment has passed.
 * - Free Gift fires at the claimable epoch minus the lead time; skipped when
 *   the gift is already claimable.
 * - Season reminders fire exactly at 24 hours and 1 hour before the season
 *   ends; only future thresholds are armed.
 * - The whole plan is empty while notifications are disabled.
 *
 * Alarms are returned sorted by trigger time.
 */
object SchedulePlanner {

    /**
     * Computes the alarms to arm for [state] relative to [now].
     */
    fun buildPlan(state: AppState, now: Long): List<ScheduledAlarm> {
        if (!state.settings.notificationsEnabled) return emptyList()

        val leadMillis = state.settings.notificationLeadMinutes.coerceAtLeast(0) *
            TimeConstants.MILLIS_PER_MINUTE

        val alarms = mutableListOf<ScheduledAlarm>()

        ProjectionCalculator.calculateProjectionEpoch(
            currentDice = state.currentDice,
            maxDice = state.maxDice,
            hourlyRefillRate = state.refillRatePerHour,
            nextRefillEpoch = state.nextRefillEpoch,
            now = now,
        )?.let { projectionEpoch ->
            val trigger = projectionEpoch - leadMillis
            if (trigger > now) {
                alarms += ScheduledAlarm(NotificationType.DICE_FULL, trigger)
            }
        }

        val giftTrigger = state.freeGiftEpoch - leadMillis
        if (giftTrigger > now) {
            alarms += ScheduledAlarm(NotificationType.GIFT_READY, giftTrigger)
        }

        val seasonEnd = state.seasonEndEpoch
        val reminder24Hours = seasonEnd - 24 * TimeConstants.MILLIS_PER_HOUR
        if (reminder24Hours > now) {
            alarms += ScheduledAlarm(NotificationType.SEASON_24H, reminder24Hours)
        }
        val reminder1Hour = seasonEnd - TimeConstants.MILLIS_PER_HOUR
        if (reminder1Hour > now) {
            alarms += ScheduledAlarm(NotificationType.SEASON_1H, reminder1Hour)
        }

        return alarms.sortedBy { it.triggerAtMillis }
    }
}
