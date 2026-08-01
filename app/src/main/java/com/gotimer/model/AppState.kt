package com.gotimer.model

import com.gotimer.util.TimeConstants

/**
 * Immutable snapshot of the application's runtime state.
 *
 * Only baseline timestamps and raw values are held here; every displayed value
 * (countdowns, projections, progress) is derived by the calculation layer.
 * Season settings and dice parameters are sourced from the nested
 * [settings] preferences.
 *
 * @property currentDice Current dice count, always clamped to `0..maxDice`.
 * @property nextRefillEpoch Epoch milliseconds of the next hourly dice refill.
 * @property freeGiftEpoch Epoch milliseconds when the Free Gift becomes claimable.
 * @property settings The persisted user preferences in effect.
 * @property seasonName Display name of the active season.
 * @property seasonEndEpoch Epoch milliseconds when the active season ends.
 * @property maxDice Cap on free dice accrual.
 * @property refillRatePerHour Number of dice generated per hour.
 */
data class AppState(
    val currentDice: Int,
    val nextRefillEpoch: Long,
    val freeGiftEpoch: Long,
    val settings: UserPreferences,
) {
    val seasonName: String
        get() = settings.seasonName

    val seasonEndEpoch: Long
        get() = settings.seasonEndEpoch

    val maxDice: Int
        get() = settings.maxDice

    val refillRatePerHour: Int
        get() = settings.hourlyRefillRate

    companion object {

        /**
         * Builds a state snapshot from persisted [settings] plus the mutable
         * runtime values. Keeps the settings source in one place so the state
         * never drifts from what was saved.
         */
        fun fromSettings(
            settings: UserPreferences,
            currentDice: Int,
            nextRefillEpoch: Long,
            freeGiftEpoch: Long,
        ): AppState = AppState(
            currentDice = currentDice,
            nextRefillEpoch = nextRefillEpoch,
            freeGiftEpoch = freeGiftEpoch,
            settings = settings,
        )

        /**
         * Creates a fresh, consistent state anchored to [now]: full dice, the
         * next refill due in one hour, the Free Gift ready immediately, and
         * default preferences with the season ending 30 days out.
         */
        fun defaults(now: Long): AppState = fromSettings(
            settings = UserPreferences.defaults(now),
            currentDice = UserPreferences.DEFAULT_MAX_DICE,
            nextRefillEpoch = now + TimeConstants.MILLIS_PER_HOUR,
            freeGiftEpoch = now,
        )
    }
}
