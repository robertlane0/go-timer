package com.gotimer.calculations

import com.gotimer.util.TimeConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectionCalculatorTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `isDiceFull is true only at or above capacity`() {
        assertTrue(ProjectionCalculator.isDiceFull(80, 80))
        assertTrue(ProjectionCalculator.isDiceFull(85, 80))
        assertFalse(ProjectionCalculator.isDiceFull(79, 80))
        assertFalse(ProjectionCalculator.isDiceFull(0, 80))
    }

    @Test
    fun `remaining dice is never negative`() {
        assertEquals(65, ProjectionCalculator.calculateRemainingDice(15, 80))
        assertEquals(0, ProjectionCalculator.calculateRemainingDice(80, 80))
        assertEquals(0, ProjectionCalculator.calculateRemainingDice(95, 80))
    }

    @Test
    fun `full cycles follows the specification formula`() {
        assertEquals(6, ProjectionCalculator.calculateFullCyclesNeeded(15, 80, 10))
        assertEquals(7, ProjectionCalculator.calculateFullCyclesNeeded(0, 80, 10))
        assertEquals(0, ProjectionCalculator.calculateFullCyclesNeeded(75, 80, 10))
        assertEquals(0, ProjectionCalculator.calculateFullCyclesNeeded(80, 80, 10))
    }

    @Test
    fun `full cycles rounds partial refills up`() {
        assertEquals(6, ProjectionCalculator.calculateFullCyclesNeeded(19, 80, 10))
        assertEquals(5, ProjectionCalculator.calculateFullCyclesNeeded(20, 80, 10))
        assertEquals(5, ProjectionCalculator.calculateFullCyclesNeeded(21, 80, 10))
    }

    @Test
    fun `full cycles is zero for a non-positive refill rate`() {
        assertEquals(0, ProjectionCalculator.calculateFullCyclesNeeded(15, 80, 0))
        assertEquals(0, ProjectionCalculator.calculateFullCyclesNeeded(15, 80, -5))
    }

    @Test
    fun `total minutes to full matches the specification example`() {
        assertEquals(
            380,
            ProjectionCalculator.calculateTotalMinutesToFull(15, 80, 10, minutesToNextRefill = 20),
        )
    }

    @Test
    fun `total minutes uses ceiling minutes and clamps negatives`() {
        assertEquals(
            420,
            ProjectionCalculator.calculateTotalMinutesToFull(0, 80, 10, minutesToNextRefill = 0),
        )
        assertEquals(
            360,
            ProjectionCalculator.calculateTotalMinutesToFull(15, 80, 10, minutesToNextRefill = -20),
        )
    }

    @Test
    fun `minutes to next refill counts whole minutes up`() {
        assertEquals(20, ProjectionCalculator.calculateMinutesToNextRefill(now + 20 * TimeConstants.MILLIS_PER_MINUTE, now))
        assertEquals(21, ProjectionCalculator.calculateMinutesToNextRefill(now + 20 * TimeConstants.MILLIS_PER_MINUTE + 30_000, now))
        assertEquals(0, ProjectionCalculator.calculateMinutesToNextRefill(now - TimeConstants.MILLIS_PER_MINUTE, now))
    }

    @Test
    fun `refills passed counts completed cycles after the next refill`() {
        val nextRefill = now + TimeConstants.MILLIS_PER_HOUR
        assertEquals(0, ProjectionCalculator.calculateRefillsPassed(nextRefill, now))
        assertEquals(0, ProjectionCalculator.calculateRefillsPassed(nextRefill, nextRefill - 1))
        assertEquals(1, ProjectionCalculator.calculateRefillsPassed(nextRefill, nextRefill))
        assertEquals(1, ProjectionCalculator.calculateRefillsPassed(nextRefill, nextRefill + 59 * TimeConstants.MILLIS_PER_MINUTE))
        assertEquals(2, ProjectionCalculator.calculateRefillsPassed(nextRefill, nextRefill + TimeConstants.MILLIS_PER_HOUR))
        assertEquals(3, ProjectionCalculator.calculateRefillsPassed(nextRefill, nextRefill + 2 * TimeConstants.MILLIS_PER_HOUR + 30_000))
    }

    @Test
    fun `effective dice accrues the hourly rate per completed cycle`() {
        val nextRefill = now + TimeConstants.MILLIS_PER_HOUR
        assertEquals(0, ProjectionCalculator.calculateEffectiveDice(0, 80, 10, nextRefill, now))
        assertEquals(10, ProjectionCalculator.calculateEffectiveDice(0, 80, 10, nextRefill, nextRefill + 30 * TimeConstants.MILLIS_PER_MINUTE))
        assertEquals(20, ProjectionCalculator.calculateEffectiveDice(0, 80, 10, nextRefill, nextRefill + TimeConstants.MILLIS_PER_HOUR))
        assertEquals(35, ProjectionCalculator.calculateEffectiveDice(15, 80, 10, nextRefill, nextRefill + TimeConstants.MILLIS_PER_HOUR))
    }

    @Test
    fun `effective dice is capped at max capacity`() {
        val nextRefill = now + TimeConstants.MILLIS_PER_HOUR
        assertEquals(80, ProjectionCalculator.calculateEffectiveDice(0, 80, 10, nextRefill, nextRefill + 20 * TimeConstants.MILLIS_PER_HOUR))
        assertEquals(80, ProjectionCalculator.calculateEffectiveDice(75, 80, 10, nextRefill, nextRefill + TimeConstants.MILLIS_PER_HOUR))
    }

    @Test
    fun `effective dice ignores a non-positive refill rate`() {
        val nextRefill = now + TimeConstants.MILLIS_PER_HOUR
        assertEquals(32, ProjectionCalculator.calculateEffectiveDice(32, 80, 0, nextRefill, nextRefill + TimeConstants.MILLIS_PER_HOUR))
        assertEquals(32, ProjectionCalculator.calculateEffectiveDice(32, 80, -5, nextRefill, nextRefill + TimeConstants.MILLIS_PER_HOUR))
    }

    @Test
    fun `projection epoch equals now plus total minutes`() {
        val projection = ProjectionCalculator.calculateProjectionEpoch(
            currentDice = 15,
            maxDice = 80,
            hourlyRefillRate = 10,
            nextRefillEpoch = now + 20 * TimeConstants.MILLIS_PER_MINUTE,
            now = now,
        )
        assertEquals(now + 380 * TimeConstants.MILLIS_PER_MINUTE, projection)
    }

    @Test
    fun `projection is null when dice are already full`() {
        assertNull(
            ProjectionCalculator.calculateProjectionEpoch(80, 80, 10, now + TimeConstants.MILLIS_PER_HOUR, now),
        )
    }

    @Test
    fun `gift remaining is zero when already claimable`() {
        val claimable = now + 8 * TimeConstants.MILLIS_PER_HOUR
        assertEquals(8 * TimeConstants.MILLIS_PER_HOUR, ProjectionCalculator.calculateGiftRemainingMillis(claimable, now))
        assertEquals(0, ProjectionCalculator.calculateGiftRemainingMillis(now - TimeConstants.MILLIS_PER_HOUR, now))
    }

    @Test
    fun `season remaining is zero when season has ended`() {
        val end = now + 14 * TimeConstants.MILLIS_PER_DAY
        assertEquals(14 * TimeConstants.MILLIS_PER_DAY, ProjectionCalculator.calculateSeasonRemainingMillis(end, now))
        assertEquals(0, ProjectionCalculator.calculateSeasonRemainingMillis(now - TimeConstants.MILLIS_PER_DAY, now))
    }

    @Test
    fun `progress fraction is clamped to zero and one`() {
        assertEquals(0.4f, ProjectionCalculator.calculateProgress(32, 80), 0.0001f)
        assertEquals(0f, ProjectionCalculator.calculateProgress(0, 80), 0.0001f)
        assertEquals(1f, ProjectionCalculator.calculateProgress(80, 80), 0.0001f)
        assertEquals(1f, ProjectionCalculator.calculateProgress(95, 80), 0.0001f)
        assertEquals(0f, ProjectionCalculator.calculateProgress(-5, 80), 0.0001f)
    }

    @Test
    fun `progress is zero when capacity is not positive`() {
        assertEquals(0f, ProjectionCalculator.calculateProgress(10, 0), 0.0001f)
        assertEquals(0f, ProjectionCalculator.calculateProgress(10, -80), 0.0001f)
    }
}
