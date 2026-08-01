package com.gotimer.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.gotimer.datastore.DataStoreKeys
import com.gotimer.datastore.PreferencesMapper
import com.gotimer.model.AppState
import com.gotimer.model.UserPreferences
import com.gotimer.util.InputValidator
import com.gotimer.util.TimeConstants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Data access abstraction between the [androidx.datastore.core.DataStore] and
 * the ViewModel layer.
 *
 * Exposes a continuous [Flow] of [AppState] derived from persisted
 * preferences and applies every state mutation as a single atomic DataStore
 * edit. All mutations sanitize input via [InputValidator] so invalid values
 * never reach the store.
 *
 * @param dataStore The preferences DataStore backing the application state.
 */
class DiceRepository(private val dataStore: DataStore<Preferences>) {

    /**
     * Continuous snapshot of the application state.
     *
     * Missing or corrupt data is mapped to safe defaults; a damaged store
     * file degrades to an empty preferences map instead of failing the flow.
     */
    val appState: Flow<AppState> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { PreferencesMapper.toAppState(it, System.currentTimeMillis()) }

    /**
     * Stores [count] as the current dice count, clamped into `0..maxDice`.
     */
    suspend fun updateDiceCount(count: Int) {
        dataStore.edit { preferences ->
            val maxDice = preferences[DataStoreKeys.MAX_DICE]
                ?: UserPreferences.DEFAULT_MAX_DICE
            preferences[DataStoreKeys.CURRENT_DICE] =
                InputValidator.clampDiceCount(count, maxDice.coerceAtLeast(0))
        }
    }

    /**
     * Resets the next refill timer so the refill lands [minutesToNext] minutes
     * after [now], clamped to the specification range `0..60`.
     */
    suspend fun resetRefillTimer(minutesToNext: Int, now: Long = System.currentTimeMillis()) {
        val minutes = InputValidator.clampRefillMinutes(minutesToNext).toLong()
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.NEXT_REFILL_EPOCH] =
                now + minutes * TimeConstants.MILLIS_PER_MINUTE
        }
    }

    /**
     * Restarts the Free Gift cycle: the gift becomes claimable 8 hours after
     * [now].
     */
    suspend fun claimFreeGift(now: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.FREE_GIFT_EPOCH] =
                now + UserPreferences.FREE_GIFT_INTERVAL_HOURS * TimeConstants.MILLIS_PER_HOUR
        }
    }

    /**
     * Applies the configured "Just Played" batch update in one atomic write:
     * optionally zeroing dice, resetting the refill timer to 60 minutes, and
     * restarting the Free Gift cycle.
     */
    suspend fun executeJustPlayedAction(now: Long = System.currentTimeMillis()) {
        dataStore.edit { preferences ->
            if (preferences[DataStoreKeys.JUST_PLAYED_ZERO_DICE] ?: true) {
                preferences[DataStoreKeys.CURRENT_DICE] = 0
            }
            if (preferences[DataStoreKeys.JUST_PLAYED_RESET_REFILL] ?: true) {
                preferences[DataStoreKeys.NEXT_REFILL_EPOCH] =
                    now + TimeConstants.MILLIS_PER_HOUR
            }
            if (preferences[DataStoreKeys.JUST_PLAYED_RESET_GIFT] ?: false) {
                preferences[DataStoreKeys.FREE_GIFT_EPOCH] =
                    now + UserPreferences.FREE_GIFT_INTERVAL_HOURS * TimeConstants.MILLIS_PER_HOUR
            }
        }
    }

    /**
     * Persists [userPreferences]. Non-positive dice capacity, refill rate, and
     * lead minutes are clamped to zero, and an impossible season end timestamp
     * falls back to the default 30-day horizon from [now].
     */
    suspend fun saveSettings(
        userPreferences: UserPreferences,
        now: Long = System.currentTimeMillis(),
    ) {
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.SEASON_NAME] = userPreferences.seasonName
            preferences[DataStoreKeys.SEASON_END_EPOCH] = InputValidator.fallbackEpoch(
                userPreferences.seasonEndEpoch,
                now + UserPreferences.DEFAULT_SEASON_DURATION_DAYS * TimeConstants.MILLIS_PER_DAY,
            )
            preferences[DataStoreKeys.MAX_DICE] = userPreferences.maxDice.coerceAtLeast(0)
            preferences[DataStoreKeys.HOURLY_REFILL_RATE] =
                userPreferences.hourlyRefillRate.coerceAtLeast(0)
            preferences[DataStoreKeys.JUST_PLAYED_ZERO_DICE] = userPreferences.justPlayedZeroDice
            preferences[DataStoreKeys.JUST_PLAYED_RESET_REFILL] = userPreferences.justPlayedResetRefill
            preferences[DataStoreKeys.JUST_PLAYED_RESET_GIFT] = userPreferences.justPlayedResetGift
            preferences[DataStoreKeys.NOTIFICATIONS_ENABLED] = userPreferences.notificationsEnabled
            preferences[DataStoreKeys.NOTIFICATION_LEAD_TIME_MINUTES] =
                userPreferences.notificationLeadMinutes.coerceAtLeast(0)
        }
    }
}
