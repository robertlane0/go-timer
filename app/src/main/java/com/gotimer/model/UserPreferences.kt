package com.gotimer.model

import com.gotimer.util.TimeConstants

/**
 * Immutable set of persistent user preferences for GO! Timer.
 *
 * Holds the configuration surfaced in the Settings window: season details,
 * dice capacity and refill rate, the "Just Played" batch-action flags, and
 * notification options. Only this object is persisted; derived values such as
 * projections are always recalculated from timestamps.
 *
 * @property seasonName Name displayed on the season countdown hero banner.
 * @property seasonEndEpoch Epoch milliseconds when the active season ends.
 * @property maxDice Cap on free dice accrual.
 * @property hourlyRefillRate Number of dice generated per hour.
 * @property justPlayedZeroDice If true, "Just Played" sets current dice to zero.
 * @property justPlayedResetRefill If true, "Just Played" resets the refill timer to 60 minutes.
 * @property justPlayedResetGift If true, "Just Played" resets the Free Gift timer to 8 hours.
 * @property notificationsEnabled If true, system notifications are active.
 * @property notificationLeadMinutes Minutes before an event completes to send the alert.
 */
data class UserPreferences(
    val seasonName: String = DEFAULT_SEASON_NAME,
    val seasonEndEpoch: Long = NO_TIMESTAMP,
    val maxDice: Int = DEFAULT_MAX_DICE,
    val hourlyRefillRate: Int = DEFAULT_HOURLY_REFILL_RATE,
    val justPlayedZeroDice: Boolean = true,
    val justPlayedResetRefill: Boolean = true,
    val justPlayedResetGift: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val notificationLeadMinutes: Int = DEFAULT_LEAD_MINUTES,
) {

    companion object {
        /** Default season banner title when no season is configured. */
        const val DEFAULT_SEASON_NAME = "Current Season"

        /** Sentinel for an unset or invalid epoch timestamp. */
        const val NO_TIMESTAMP = 0L

        /** Default cap on free dice accrual. */
        const val DEFAULT_MAX_DICE = 80

        /** Default number of dice generated per hour. */
        const val DEFAULT_HOURLY_REFILL_RATE = 10

        /** Default notification lead time in minutes before event completion. */
        const val DEFAULT_LEAD_MINUTES = 5

        /** Default season length used when creating preferences from scratch. */
        const val DEFAULT_SEASON_DURATION_DAYS = 30L

        /** Interval between dice refill increments, in minutes. */
        const val REFILL_INTERVAL_MINUTES = 60

        /** Duration of the store Free Gift cycle, in hours. */
        const val FREE_GIFT_INTERVAL_HOURS = 8L

        /**
         * Creates defaults anchored to [now]: the season is configured to end
         * 30 days after the current moment, matching the specification default.
         */
        fun defaults(now: Long): UserPreferences =
            UserPreferences(
                seasonEndEpoch = now + DEFAULT_SEASON_DURATION_DAYS * TimeConstants.MILLIS_PER_DAY,
            )
    }
}
