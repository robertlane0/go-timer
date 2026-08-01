package com.gotimer.ui.widgets

import com.gotimer.model.AppState
import com.gotimer.model.UserPreferences
import com.gotimer.util.TimeConstants
import org.junit.Assert.assertEquals
import org.junit.Test

class GoTimerWidgetModelTest {

    private val now = 1_700_000_000_000L

    private fun state(
        currentDice: Int = 32,
        nextRefillInMinutes: Long = 20,
        giftInHours: Long = 2,
    ): AppState = AppState.fromSettings(
        settings = UserPreferences(
            seasonName = "Monopoly Origins",
            seasonEndEpoch = now + 14 * TimeConstants.MILLIS_PER_DAY,
        ),
        currentDice = currentDice,
        nextRefillEpoch = now + nextRefillInMinutes * TimeConstants.MILLIS_PER_MINUTE,
        freeGiftEpoch = now + giftInHours * TimeConstants.MILLIS_PER_HOUR,
    )

    @Test
    fun `maps dice, refill, gift, and projection from state`() {
        val model = GoTimerWidgetModel.from(state(), now)

        assertEquals("32/80", model.diceText)
        assertEquals("Full in 4h 20m", model.fullProjectionText)
        assertEquals("20m 00s", model.nextRefillText)
        assertEquals("2h 00m", model.giftText)
    }

    @Test
    fun `gift shows ready text when claimable`() {
        val model = GoTimerWidgetModel.from(
            state(giftInHours = 0),
            now,
        )

        assertEquals(GoTimerWidgetModel.GIFT_READY_TEXT, model.giftText)
    }

    @Test
    fun `full pool shows full text and no projection`() {
        val model = GoTimerWidgetModel.from(
            state(currentDice = 80),
            now,
        )

        assertEquals("80/80", model.diceText)
        assertEquals("Full!", model.fullProjectionText)
    }

    @Test
    fun `countdowns never go negative`() {
        val model = GoTimerWidgetModel.from(
            state(
                nextRefillInMinutes = -10,
                giftInHours = -1,
            ),
            now,
        )

        assertEquals("00m 00s", model.nextRefillText)
        assertEquals(GoTimerWidgetModel.GIFT_READY_TEXT, model.giftText)
    }
}
