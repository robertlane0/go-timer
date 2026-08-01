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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val scheduler: TestCoroutineScheduler
        get() = mainDispatcherRule.testDispatcher.scheduler

    private val now = 1_700_000_000_000L

    @Test
    fun `dice presets derive from capacity and refill rate`() = runTest(scheduler) {
        val repository = DiceRepository(TestDataStores.create(temporaryFolder, scheduler), clock = { now })
        repository.saveSettings(
            UserPreferences(maxDice = 80, hourlyRefillRate = 10),
            now,
        )
        val viewModel = UpdateViewModel(repository, FakeNotificationRescheduler())

        val uiState = viewModel.uiState.first()

        assertEquals(listOf(0, 10, 20, 30, 40, 50, 60, 70, 80), uiState.dicePresets)
        assertEquals(80, uiState.maxDice)
    }

    @Test
    fun `dice presets include max when not a multiple of the rate`() = runTest(scheduler) {
        val repository = DiceRepository(TestDataStores.create(temporaryFolder, scheduler), clock = { now })
        repository.saveSettings(
            UserPreferences(maxDice = 50, hourlyRefillRate = 10),
            now,
        )
        val viewModel = UpdateViewModel(repository, FakeNotificationRescheduler())

        val uiState = viewModel.uiState.first()

        assertEquals(listOf(0, 10, 20, 30, 40, 50), uiState.dicePresets)
    }

    @Test
    fun `selections enable save`() = runTest(scheduler) {
        val viewModel = UpdateViewModel(
            DiceRepository(TestDataStores.create(temporaryFolder, scheduler), clock = { now }),
            FakeNotificationRescheduler(),
        )
        viewModel.uiState.first()

        assertFalse(viewModel.uiState.value.saveEnabled)

        viewModel.selectDice(20)
        assertTrue(viewModel.uiState.value.saveEnabled)
        assertEquals(20, viewModel.uiState.value.selectedDice)

        viewModel.selectRefillMinutes(30)
        viewModel.selectGiftOption(GiftOption.FOUR_HOURS_LEFT)
        assertEquals(30, viewModel.uiState.value.selectedRefillMinutes)
        assertEquals(GiftOption.FOUR_HOURS_LEFT, viewModel.uiState.value.selectedGiftOption)

        viewModel.clearSelection()
        assertFalse(viewModel.uiState.value.saveEnabled)
        assertNull(viewModel.uiState.value.selectedDice)
    }

    @Test
    fun `custom dice input is parsed and clamped`() = runTest(scheduler) {
        val repository = DiceRepository(TestDataStores.create(temporaryFolder, scheduler), clock = { now })
        repository.saveSettings(UserPreferences(maxDice = 50), now)
        val viewModel = UpdateViewModel(
            repository,
            FakeNotificationRescheduler(),
            clock = { now },
        )
        viewModel.uiState.first()

        viewModel.onCustomDiceInput(" 42 ")
        assertEquals(42, viewModel.uiState.value.selectedDice)

        viewModel.onCustomDiceInput("500")
        assertEquals(50, viewModel.uiState.value.selectedDice)

        viewModel.onCustomDiceInput("-3")
        assertEquals(0, viewModel.uiState.value.selectedDice)

        viewModel.onCustomDiceInput("abc")
        assertEquals(0, viewModel.uiState.value.selectedDice)
    }

    @Test
    fun `save applies only selected categories and reschedules`() = runTest(scheduler) {
        val repository = DiceRepository(TestDataStores.create(temporaryFolder, scheduler), clock = { now })
        repository.updateDiceCount(10)
        repository.claimFreeGift(now)
        val giftBefore = repository.appState.first().freeGiftEpoch
        val fakeScheduler = FakeNotificationRescheduler()
        val viewModel = UpdateViewModel(
            repository,
            fakeScheduler,
            clock = { now },
        )
        viewModel.uiState.first()

        viewModel.selectDice(50)
        viewModel.selectRefillMinutes(30)
        viewModel.save()

        val state = repository.appState.first()
        assertEquals(50, state.currentDice)
        assertEquals(now + 30 * TimeConstants.MILLIS_PER_MINUTE, state.nextRefillEpoch)
        assertEquals(giftBefore, state.freeGiftEpoch)
        assertEquals(1, fakeScheduler.rescheduleCount)
    }

    @Test
    fun `save with no selections changes nothing`() = runTest(scheduler) {
        val repository = DiceRepository(TestDataStores.create(temporaryFolder, scheduler), clock = { now })
        repository.updateDiceCount(10)
        repository.resetRefillTimer(15, now)
        repository.claimFreeGift(now)
        val before = repository.appState.first()
        val fakeScheduler = FakeNotificationRescheduler()
        val viewModel = UpdateViewModel(repository, fakeScheduler)
        viewModel.uiState.first()

        viewModel.save()

        val state = repository.appState.first()
        assertEquals(before.currentDice, state.currentDice)
        assertEquals(before.nextRefillEpoch, state.nextRefillEpoch)
        assertEquals(before.freeGiftEpoch, state.freeGiftEpoch)
        assertEquals(1, fakeScheduler.rescheduleCount)
    }
}
