package com.gotimer.viewmodel

import com.gotimer.model.AppState
import com.gotimer.model.UserPreferences
import com.gotimer.util.TimeConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardUiStateTest {

    private val now = 1_700_000_000_000L

    private fun state(
        currentDice: Int = 32,
        nextRefillInMinutes: Long = 20,
        giftInHours: Long = 2,
        seasonInDays: Long = 14,
    ): AppState = AppState.fromSettings(
        settings = UserPreferences(
            seasonName = "Monopoly Origins",
            seasonEndEpoch = now + seasonInDays * TimeConstants.MILLIS_PER_DAY,
        ),
        currentDice = currentDice,
        nextRefillEpoch = now + nextRefillInMinutes * TimeConstants.MILLIS_PER_MINUTE,
        freeGiftEpoch = now + giftInHours * TimeConstants.MILLIS_PER_HOUR,
    )

    @Test
    fun `maps every display field from state`() {
        val uiState = DashboardUiState.from(state(), now)

        assertEquals("Monopoly Origins", uiState.seasonName)
        assertEquals("14d 00h 00m", uiState.seasonCountdownText)
        assertEquals(32, uiState.currentDice)
        assertEquals(80, uiState.maxDice)
        assertEquals(0.4f, uiState.diceProgress, 0.0001f)
        assertEquals("20m 00s", uiState.nextRefillCountdownText)
        assertEquals("4h 20m", uiState.fullProjectionCountdownText)
        assertNotNull(uiState.fullProjectionClockText)
        assertEquals("02h 00m 00s", uiState.giftCountdownText)
        assertFalse(uiState.giftReady)
    }

    @Test
    fun `gift shows ready text when claimable`() {
        val uiState = DashboardUiState.from(
            state(giftInHours = 0),
            now,
        )

        assertTrue(uiState.giftReady)
        assertEquals(DashboardUiState.GIFT_READY_TEXT, uiState.giftCountdownText)
    }

    @Test
    fun `full projection is null when the pool is full`() {
        val uiState = DashboardUiState.from(
            state(currentDice = 80),
            now,
        )

        assertNull(uiState.fullProjectionCountdownText)
        assertNull(uiState.fullProjectionClockText)
    }

    @Test
    fun `countdowns never go negative`() {
        val uiState = DashboardUiState.from(
            state(
                nextRefillInMinutes = -10,
                giftInHours = -1,
                seasonInDays = -1,
            ),
            now,
        )

        assertEquals("00m 00s", uiState.nextRefillCountdownText)
        assertEquals("00m 00s", uiState.seasonCountdownText)
        assertTrue(uiState.giftReady)
    }
}
