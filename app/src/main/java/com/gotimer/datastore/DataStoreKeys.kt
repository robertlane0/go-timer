package com.gotimer.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.Preferences

/**
 * Central registry of every DataStore preference key persisted by GO! Timer.
 *
 * Only baseline values and configuration are stored; derived values such as
 * projections are never persisted. Defaults belong to the mapping layer, not
 * here, so keys stay a pure list of identifiers.
 */
object DataStoreKeys {

    /** Active season display name. */
    val SEASON_NAME: Preferences.Key<String> = stringPreferencesKey("season_name")

    /** Epoch milliseconds when the active season ends. */
    val SEASON_END_EPOCH: Preferences.Key<Long> = longPreferencesKey("season_end_epoch")

    /** Cap on free dice accrual. */
    val MAX_DICE: Preferences.Key<Int> = intPreferencesKey("max_dice")

    /** Number of dice generated per hour. */
    val HOURLY_REFILL_RATE: Preferences.Key<Int> = intPreferencesKey("hourly_refill_rate")

    /** Current dice count, clamped to `0..maxDice` when mapped. */
    val CURRENT_DICE: Preferences.Key<Int> = intPreferencesKey("current_dice")

    /** Epoch milliseconds of the next hourly dice refill. */
    val NEXT_REFILL_EPOCH: Preferences.Key<Long> = longPreferencesKey("next_refill_epoch")

    /** Epoch milliseconds when the Free Gift becomes claimable. */
    val FREE_GIFT_EPOCH: Preferences.Key<Long> = longPreferencesKey("free_gift_epoch")

    /** If true, "Just Played" sets current dice to zero. */
    val JUST_PLAYED_ZERO_DICE: Preferences.Key<Boolean> = booleanPreferencesKey("just_played_zero_dice")

    /** If true, "Just Played" resets the refill timer to 60 minutes. */
    val JUST_PLAYED_RESET_REFILL: Preferences.Key<Boolean> = booleanPreferencesKey("just_played_reset_refill")

    /** If true, "Just Played" resets the Free Gift timer to 8 hours. */
    val JUST_PLAYED_RESET_GIFT: Preferences.Key<Boolean> = booleanPreferencesKey("just_played_reset_gift")

    /** If true, system notifications are active. */
    val NOTIFICATIONS_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("notifications_enabled")

    /** Minutes before an event completes to send the alert. */
    val NOTIFICATION_LEAD_TIME_MINUTES: Preferences.Key<Int> = intPreferencesKey("notification_lead_time_minutes")
}
