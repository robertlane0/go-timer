package com.gotimer.datastore

import androidx.datastore.preferences.core.Preferences
import com.gotimer.calculations.ProjectionCalculator
import com.gotimer.model.AppState
import com.gotimer.model.UserPreferences
import com.gotimer.util.InputValidator
import com.gotimer.util.TimeConstants

/**
 * Maps raw [Preferences] into immutable domain models.
 *
 * Reads are defensive: missing keys fall back to specification defaults and
 * corrupt timestamps fall back to sensible values anchored at [now], so a
 * damaged store can never crash the UI.
 */
object PreferencesMapper {

    /**
     * Builds the persisted settings from [preferences], applying defaults and
     * sanitizing ranges. An invalid season end timestamp is replaced by the
     * default 30-day horizon from [now].
     */
    fun toUserPreferences(preferences: Preferences, now: Long): UserPreferences = UserPreferences(
        seasonName = preferences[DataStoreKeys.SEASON_NAME] ?: UserPreferences.DEFAULT_SEASON_NAME,
        seasonEndEpoch = InputValidator.fallbackEpoch(
            preferences[DataStoreKeys.SEASON_END_EPOCH] ?: UserPreferences.NO_TIMESTAMP,
            now + UserPreferences.DEFAULT_SEASON_DURATION_DAYS * TimeConstants.MILLIS_PER_DAY,
        ),
        maxDice = (preferences[DataStoreKeys.MAX_DICE] ?: UserPreferences.DEFAULT_MAX_DICE)
            .coerceAtLeast(0),
        hourlyRefillRate = (preferences[DataStoreKeys.HOURLY_REFILL_RATE]
            ?: UserPreferences.DEFAULT_HOURLY_REFILL_RATE).coerceAtLeast(0),
        justPlayedZeroDice = preferences[DataStoreKeys.JUST_PLAYED_ZERO_DICE] ?: true,
        justPlayedResetRefill = preferences[DataStoreKeys.JUST_PLAYED_RESET_REFILL] ?: true,
        justPlayedResetGift = preferences[DataStoreKeys.JUST_PLAYED_RESET_GIFT] ?: false,
        notificationsEnabled = preferences[DataStoreKeys.NOTIFICATIONS_ENABLED] ?: true,
        notificationLeadMinutes = (preferences[DataStoreKeys.NOTIFICATION_LEAD_TIME_MINUTES]
            ?: UserPreferences.DEFAULT_LEAD_MINUTES).coerceAtLeast(0),
    )

    /**
     * Builds the full application state snapshot from [preferences], clamping
     * dice into `0..maxDice` and falling back on invalid runtime timestamps.
     *
     * Fresh installs start with a full dice pool, the next refill in one hour,
     * and the Free Gift immediately claimable.
     */
    fun toAppState(preferences: Preferences, now: Long): AppState {
        val settings = toUserPreferences(preferences, now)
        return AppState.fromSettings(
            settings = settings,
            currentDice = InputValidator.clampDiceCount(
                preferences[DataStoreKeys.CURRENT_DICE] ?: UserPreferences.DEFAULT_MAX_DICE,
                settings.maxDice,
            ),
            nextRefillEpoch = InputValidator.fallbackEpoch(
                preferences[DataStoreKeys.NEXT_REFILL_EPOCH] ?: UserPreferences.NO_TIMESTAMP,
                now + TimeConstants.MILLIS_PER_HOUR,
            ),
            freeGiftEpoch = InputValidator.fallbackEpoch(
                preferences[DataStoreKeys.FREE_GIFT_EPOCH] ?: UserPreferences.NO_TIMESTAMP,
                now,
            ),
        )
    }
}
