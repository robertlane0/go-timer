package com.gotimer.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.gotimer.datastore.DataStoreKeys
import com.gotimer.model.UserPreferences
import com.gotimer.util.TimeConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DiceRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val now = 1_700_000_000_000L

    private fun TestScope.testDataStore(): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
        ) {
            temporaryFolder.newFile("test.preferences_pb")
        }

    @Test
    fun `empty store emits default state`() = runTest {
        val repository = DiceRepository(testDataStore())

        val state = repository.appState.first()

        assertEquals(UserPreferences.DEFAULT_SEASON_NAME, state.seasonName)
        assertEquals(UserPreferences.DEFAULT_MAX_DICE, state.currentDice)
        assertEquals(UserPreferences.DEFAULT_MAX_DICE, state.maxDice)
        assertEquals(UserPreferences.DEFAULT_HOURLY_REFILL_RATE, state.refillRatePerHour)
        assertTrue(state.seasonEndEpoch > now)
        assertTrue(state.nextRefillEpoch > now)
        assertTrue(state.freeGiftEpoch <= System.currentTimeMillis() + 1_000)
    }

    @Test
    fun `updateDiceCount stores and clamps the count`() = runTest {
        val repository = DiceRepository(testDataStore())

        repository.updateDiceCount(32)
        assertEquals(32, repository.appState.first().currentDice)

        repository.updateDiceCount(120)
        assertEquals(80, repository.appState.first().currentDice)

        repository.updateDiceCount(-5)
        assertEquals(0, repository.appState.first().currentDice)
    }

    @Test
    fun `updateDiceCount respects a custom max dice`() = runTest {
        val repository = DiceRepository(testDataStore())

        repository.saveSettings(UserPreferences(maxDice = 50), now)
        repository.updateDiceCount(60)

        assertEquals(50, repository.appState.first().currentDice)
    }

    @Test
    fun `resetRefillTimer stores now plus minutes and clamps`() = runTest {
        val repository = DiceRepository(testDataStore())

        repository.resetRefillTimer(20, now)
        assertEquals(now + 20 * TimeConstants.MILLIS_PER_MINUTE, repository.appState.first().nextRefillEpoch)

        repository.resetRefillTimer(90, now)
        assertEquals(now + 60 * TimeConstants.MILLIS_PER_MINUTE, repository.appState.first().nextRefillEpoch)

        repository.resetRefillTimer(-10, now)
        assertEquals(now, repository.appState.first().nextRefillEpoch)
    }

    @Test
    fun `claimFreeGift sets the gift eight hours out`() = runTest {
        val repository = DiceRepository(testDataStore())

        repository.claimFreeGift(now)

        assertEquals(
            now + UserPreferences.FREE_GIFT_INTERVAL_HOURS * TimeConstants.MILLIS_PER_HOUR,
            repository.appState.first().freeGiftEpoch,
        )
    }

    @Test
    fun `just played applies default flags`() = runTest {
        val repository = DiceRepository(testDataStore())
        repository.updateDiceCount(40)
        repository.resetRefillTimer(15, now)
        repository.claimFreeGift(now)

        repository.executeJustPlayedAction(now)

        val state = repository.appState.first()
        assertEquals(0, state.currentDice)
        assertEquals(now + TimeConstants.MILLIS_PER_HOUR, state.nextRefillEpoch)
        assertEquals(
            now + UserPreferences.FREE_GIFT_INTERVAL_HOURS * TimeConstants.MILLIS_PER_HOUR,
            state.freeGiftEpoch,
        )
    }

    @Test
    fun `just played with all flags also resets the gift`() = runTest {
        val repository = DiceRepository(testDataStore())
        repository.saveSettings(
            UserPreferences(
                justPlayedZeroDice = true,
                justPlayedResetRefill = true,
                justPlayedResetGift = true,
            ),
            now,
        )

        repository.executeJustPlayedAction(now)

        val state = repository.appState.first()
        assertEquals(0, state.currentDice)
        assertEquals(now + TimeConstants.MILLIS_PER_HOUR, state.nextRefillEpoch)
        assertEquals(
            now + UserPreferences.FREE_GIFT_INTERVAL_HOURS * TimeConstants.MILLIS_PER_HOUR,
            state.freeGiftEpoch,
        )
    }

    @Test
    fun `just played with all flags disabled changes nothing`() = runTest {
        val repository = DiceRepository(testDataStore())
        repository.updateDiceCount(40)
        repository.resetRefillTimer(15, now)
        repository.claimFreeGift(now)
        repository.saveSettings(
            UserPreferences(
                justPlayedZeroDice = false,
                justPlayedResetRefill = false,
                justPlayedResetGift = false,
            ),
            now,
        )

        repository.executeJustPlayedAction(now)

        val state = repository.appState.first()
        assertEquals(40, state.currentDice)
        assertEquals(now + 15 * TimeConstants.MILLIS_PER_MINUTE, state.nextRefillEpoch)
        assertEquals(
            now + UserPreferences.FREE_GIFT_INTERVAL_HOURS * TimeConstants.MILLIS_PER_HOUR,
            state.freeGiftEpoch,
        )
    }

    @Test
    fun `saveSettings round-trips through the flow`() = runTest {
        val repository = DiceRepository(testDataStore())
        val preferences = UserPreferences(
            seasonName = "Monopoly Origins",
            seasonEndEpoch = now + 12 * TimeConstants.MILLIS_PER_DAY,
            maxDice = 120,
            hourlyRefillRate = 12,
            justPlayedZeroDice = false,
            justPlayedResetRefill = true,
            justPlayedResetGift = true,
            notificationsEnabled = false,
            notificationLeadMinutes = 10,
        )

        repository.saveSettings(preferences, now)

        val state = repository.appState.first()
        assertEquals(preferences, state.settings)
        assertEquals(120, state.maxDice)
        assertEquals("Monopoly Origins", state.seasonName)
    }

    @Test
    fun `saveSettings clamps invalid settings`() = runTest {
        val repository = DiceRepository(testDataStore())

        repository.saveSettings(
            UserPreferences(
                seasonEndEpoch = -100,
                maxDice = -50,
                hourlyRefillRate = -5,
                notificationLeadMinutes = -10,
            ),
            now,
        )

        val state = repository.appState.first()
        assertEquals(0, state.maxDice)
        assertEquals(0, state.refillRatePerHour)
        assertEquals(0, state.settings.notificationLeadMinutes)
        assertTrue(state.seasonEndEpoch > now)
    }

    @Test
    fun `corrupt runtime timestamps fall back safely`() = runTest {
        val dataStore = testDataStore()
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.NEXT_REFILL_EPOCH] = -5
            preferences[DataStoreKeys.FREE_GIFT_EPOCH] = -5
        }
        val repository = DiceRepository(dataStore)

        val state = repository.appState.first()

        assertTrue(state.nextRefillEpoch > now)
        assertTrue(state.freeGiftEpoch >= 0)
    }

    @Test
    fun `corrupt dice values are clamped on read`() = runTest {
        val dataStore = testDataStore()
        dataStore.edit { preferences ->
            preferences[DataStoreKeys.MAX_DICE] = 80
            preferences[DataStoreKeys.CURRENT_DICE] = 9_999
        }

        val state = DiceRepository(dataStore).appState.first()

        assertEquals(80, state.currentDice)
    }
}
