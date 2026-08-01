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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scheduler: TestCoroutineScheduler
        get() = mainDispatcherRule.testDispatcher.scheduler

    private val now = 1_700_000_000_000L

    private var clock: Long = 1_700_000_000_000L

    private fun repository(): DiceRepository =
        DiceRepository(TestDataStores.create(temporaryFolder, scheduler))

    private fun viewModel(repository: DiceRepository): DashboardViewModel =
        DashboardViewModel(
            repository = repository,
            notificationScheduler = FakeNotificationRescheduler(),
            clock = { clock },
        )

    @Test
    fun `uiState reflects the persisted state`() = runTest(scheduler) {
        val repository = repository()
        repository.updateDiceCount(32)
        repository.resetRefillTimer(20, now)
        repository.saveSettings(UserPreferences(seasonName = "Monopoly Origins"), now)
        val viewModel = viewModel(repository)

        val state = viewModel.uiState.first()
        assertEquals(32, state.currentDice)
        assertEquals("Monopoly Origins", state.seasonName)
        assertEquals("20m 00s", state.nextRefillCountdownText)
    }

    @Test
    fun `countdown text advances with the one second tick`() = runTest(scheduler) {
        val repository = repository()
        repository.resetRefillTimer(20, now)
        val viewModel = viewModel(repository)

        val first = viewModel.uiState.first()
        assertEquals("20m 00s", first.nextRefillCountdownText)

        clock = now + TimeConstants.MILLIS_PER_MINUTE
        advanceTimeBy(1_000)
        runCurrent()

        val second = viewModel.uiState.first()
        assertEquals("19m 00s", second.nextRefillCountdownText)
    }

    @Test
    fun `onJustPlayed zeroes dice and reschedules alarms`() = runTest(scheduler) {
        val repository = repository()
        repository.updateDiceCount(40)
        val fakeScheduler = FakeNotificationRescheduler()
        val viewModel = DashboardViewModel(repository, fakeScheduler, clock = { now })

        viewModel.onJustPlayed()

        assertEquals(0, repository.appState.first().currentDice)
        assertEquals(1, fakeScheduler.rescheduleCount)
    }

    @Test
    fun `onClaimFreeGift restarts the gift cycle and reschedules`() = runTest(scheduler) {
        val repository = repository()
        val fakeScheduler = FakeNotificationRescheduler()
        val viewModel = DashboardViewModel(repository, fakeScheduler, clock = { now })

        viewModel.onClaimFreeGift()

        val state = repository.appState.first()
        assertEquals(
            now + UserPreferences.FREE_GIFT_INTERVAL_HOURS * TimeConstants.MILLIS_PER_HOUR,
            state.freeGiftEpoch,
        )
        assertEquals(1, fakeScheduler.rescheduleCount)
    }
}
