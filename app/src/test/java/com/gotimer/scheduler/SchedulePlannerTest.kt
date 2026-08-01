package com.gotimer.scheduler

import com.gotimer.model.AppState
import com.gotimer.model.UserPreferences
import com.gotimer.util.TimeConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulePlannerTest {

    private val now = 1_700_000_000_000L

    private fun state(
        currentDice: Int = 32,
        maxDice: Int = 80,
        refillRate: Int = 10,
        nextRefillInMinutes: Long = 20,
        giftInMillis: Long = 2 * TimeConstants.MILLIS_PER_HOUR,
        seasonInMillis: Long = 10 * TimeConstants.MILLIS_PER_DAY,
        notificationsEnabled: Boolean = true,
        leadMinutes: Int = 5,
    ): AppState = AppState.fromSettings(
        settings = UserPreferences(
            seasonEndEpoch = now + seasonInMillis,
            notificationsEnabled = notificationsEnabled,
            notificationLeadMinutes = leadMinutes,
        ),
        currentDice = currentDice,
        nextRefillEpoch = now + nextRefillInMinutes * TimeConstants.MILLIS_PER_MINUTE,
        freeGiftEpoch = now + giftInMillis,
    )

    @Test
    fun `plan is empty when notifications are disabled`() {
        val plan = SchedulePlanner.buildPlan(state(notificationsEnabled = false), now)
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `plan arms all four events with lead times`() {
        val plan = SchedulePlanner.buildPlan(state(), now)

        assertEquals(
            listOf(
                NotificationType.GIFT_READY,
                NotificationType.DICE_FULL,
                NotificationType.SEASON_24H,
                NotificationType.SEASON_1H,
            ),
            plan.map { it.type },
        )
        assertEquals(
            now + 2 * TimeConstants.MILLIS_PER_HOUR - 5 * TimeConstants.MILLIS_PER_MINUTE,
            plan.first { it.type == NotificationType.GIFT_READY }.triggerAtMillis,
        )
        assertEquals(
            now + 260 * TimeConstants.MILLIS_PER_MINUTE - 5 * TimeConstants.MILLIS_PER_MINUTE,
            plan.first { it.type == NotificationType.DICE_FULL }.triggerAtMillis,
        )
        assertEquals(
            now + 10 * TimeConstants.MILLIS_PER_DAY - 24 * TimeConstants.MILLIS_PER_HOUR,
            plan.first { it.type == NotificationType.SEASON_24H }.triggerAtMillis,
        )
        assertEquals(
            now + 10 * TimeConstants.MILLIS_PER_DAY - TimeConstants.MILLIS_PER_HOUR,
            plan.first { it.type == NotificationType.SEASON_1H }.triggerAtMillis,
        )
    }

    @Test
    fun `plan skips dice full when the pool is already full`() {
        val plan = SchedulePlanner.buildPlan(state(currentDice = 80), now)

        assertTrue(plan.none { it.type == NotificationType.DICE_FULL })
        assertEquals(3, plan.size)
    }

    @Test
    fun `plan skips dice full when the projection moment has passed`() {
        val plan = SchedulePlanner.buildPlan(
            state(currentDice = 79, nextRefillInMinutes = 0),
            now,
        )

        assertTrue(plan.none { it.type == NotificationType.DICE_FULL })
    }

    @Test
    fun `plan skips the gift when it is already claimable`() {
        val plan = SchedulePlanner.buildPlan(
            state(giftInMillis = 0, seasonInMillis = TimeConstants.MILLIS_PER_DAY),
            now,
        )

        assertTrue(plan.none { it.type == NotificationType.GIFT_READY })
    }

    @Test
    fun `plan arms only the one hour reminder when the 24 hour threshold passed`() {
        val plan = SchedulePlanner.buildPlan(
            state(seasonInMillis = 20 * TimeConstants.MILLIS_PER_HOUR),
            now,
        )

        val seasonAlarms = plan.filter {
            it.type == NotificationType.SEASON_24H || it.type == NotificationType.SEASON_1H
        }
        assertEquals(
            listOf(NotificationType.SEASON_1H),
            seasonAlarms.map { it.type },
        )
    }

    @Test
    fun `plan skips season reminders when the season has ended`() {
        val plan = SchedulePlanner.buildPlan(
            state(seasonInMillis = 0),
            now,
        )

        assertTrue(plan.none { it.type == NotificationType.SEASON_24H || it.type == NotificationType.SEASON_1H })
    }

    @Test
    fun `plan uses event time when lead time is zero`() {
        val plan = SchedulePlanner.buildPlan(state(leadMinutes = 0), now)

        assertEquals(
            now + 2 * TimeConstants.MILLIS_PER_HOUR,
            plan.first { it.type == NotificationType.GIFT_READY }.triggerAtMillis,
        )
    }

    @Test
    fun `plan clamps negative lead times to zero`() {
        val plan = SchedulePlanner.buildPlan(state(leadMinutes = -5), now)

        assertEquals(
            now + 2 * TimeConstants.MILLIS_PER_HOUR,
            plan.first { it.type == NotificationType.GIFT_READY }.triggerAtMillis,
        )
    }

    @Test
    fun `plan returns alarms sorted by trigger time`() {
        val plan = SchedulePlanner.buildPlan(state(), now)

        val triggers = plan.map { it.triggerAtMillis }
        assertEquals(triggers.sorted(), triggers)
    }
}
