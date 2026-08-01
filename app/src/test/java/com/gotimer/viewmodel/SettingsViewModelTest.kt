package com.gotimer.viewmodel

import com.gotimer.model.UserPreferences
import com.gotimer.repository.DiceRepository
import com.gotimer.testing.FakeNotificationRescheduler
import com.gotimer.testing.MainDispatcherRule
import com.gotimer.testing.TestDataStores
import com.gotimer.util.TimeConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scheduler: TestCoroutineScheduler
        get() = mainDispatcherRule.testDispatcher.scheduler

    private val now = 1_700_000_000_000L

    @Test
    fun `settings flow reflects persisted preferences`() = runTest(scheduler) {
        val repository = DiceRepository(TestDataStores.create(temporaryFolder, scheduler))
        repository.saveSettings(
            UserPreferences(
                seasonName = "Monopoly Origins",
                seasonEndEpoch = now + 12 * TimeConstants.MILLIS_PER_DAY,
                maxDice = 120,
                hourlyRefillRate = 12,
                notificationLeadMinutes = 10,
            ),
            now,
        )
        val viewModel = SettingsViewModel(repository, FakeNotificationRescheduler())

        val settings = viewModel.settings.first()

        assertEquals("Monopoly Origins", settings.seasonName)
        assertEquals(120, settings.maxDice)
        assertEquals(12, settings.hourlyRefillRate)
        assertEquals(10, settings.notificationLeadMinutes)
    }

    @Test
    fun `save persists preferences and reschedules alarms`() = runTest(scheduler) {
        val repository = DiceRepository(TestDataStores.create(temporaryFolder, scheduler))
        val fakeScheduler = FakeNotificationRescheduler()
        val viewModel = SettingsViewModel(repository, fakeScheduler)
        val newPreferences = UserPreferences(
            seasonName = "Sticker Parade",
            seasonEndEpoch = now + 12 * TimeConstants.MILLIS_PER_DAY,
            maxDice = 200,
            hourlyRefillRate = 20,
            justPlayedZeroDice = false,
        )

        viewModel.save(newPreferences)

        val settings = repository.appState.first().settings
        assertEquals(newPreferences, settings)
        assertEquals(1, fakeScheduler.rescheduleCount)
    }
}
