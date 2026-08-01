package com.gotimer.calculations

import com.gotimer.util.TimeConstants

/**
 * Pure calculation engine for dice, gift, and season timing.
 *
 * Every function is side-effect free: time is always passed in explicitly so
 * results are deterministic and unit-testable. The dashboard projection follows
 * the specification formulas exactly:
 *
 * ```
 * Remaining Dice Needed  = Max Dice - Current Dice
 * Full Cycles Needed     = ceil(Remaining / Hourly Rate) - 1
 * Total Minutes to Full  = Minutes to Next Refill + (Full Cycles * 60)
 * ```
 */
object ProjectionCalculator {

    /**
     * True when the dice pool is already at or above capacity.
     *
     * @param currentDice Current dice count.
     * @param maxDice Maximum dice capacity.
     */
    fun isDiceFull(currentDice: Int, maxDice: Int): Boolean = currentDice >= maxDice

    /**
     * Number of dice still needed to reach [maxDice], never negative.
     */
    fun calculateRemainingDice(currentDice: Int, maxDice: Int): Int =
        (maxDice - currentDice).coerceAtLeast(0)

    /**
     * Number of complete one-hour cycles needed after the immediate next refill
     * before the pool is full.
     *
     * Assumes an hourly refill rate of [hourlyRefillRate] dice. A non-positive
     * rate cannot produce a projection and yields zero cycles.
     */
    fun calculateFullCyclesNeeded(
        currentDice: Int,
        maxDice: Int,
        hourlyRefillRate: Int,
    ): Int {
        val remaining = calculateRemainingDice(currentDice, maxDice)
        if (remaining <= 0 || hourlyRefillRate <= 0) return 0
        val refillsNeeded = ceilDiv(remaining, hourlyRefillRate)
        return (refillsNeeded - 1).coerceAtLeast(0)
    }

    /**
     * Number of refill cycles that have completed by [now].
     *
     * Refills recur every [TimeConstants.MINUTES_PER_HOUR] minutes starting at
     * [nextRefillEpoch]; the stored dice count is the baseline at the start of
     * the current cycle, so every completed cycle adds one refill. Returns
     * zero while the next refill has not happened yet.
     */
    fun calculateRefillsPassed(nextRefillEpoch: Long, now: Long): Long {
        val elapsedMillis = now - nextRefillEpoch
        if (elapsedMillis < 0) return 0
        return elapsedMillis / TimeConstants.MILLIS_PER_HOUR + 1
    }

    /**
     * Epoch milliseconds of the next upcoming refill at [now].
     *
     * Refills recur every [TimeConstants.MINUTES_PER_HOUR] minutes starting at
     * [baselineNextRefill], so once that moment passes the next boundary is
     * one full cycle ahead. Returns [baselineNextRefill] unchanged while it is
     * still in the future.
     */
    fun calculateNextRefillEpoch(baselineNextRefill: Long, now: Long): Long =
        baselineNextRefill +
            calculateRefillsPassed(baselineNextRefill, now) * TimeConstants.MILLIS_PER_HOUR

    /**
     * Effective dice count at [now], derived from the stored baseline
     * [currentDice] plus [hourlyRefillRate] for every refill cycle completed
     * since the baseline was anchored, capped into `0..maxDice`.
     *
     * A non-positive [hourlyRefillRate] adds no refills; a non-positive
     * [maxDice] caps the result at zero.
     */
    fun calculateEffectiveDice(
        currentDice: Int,
        maxDice: Int,
        hourlyRefillRate: Int,
        nextRefillEpoch: Long,
        now: Long,
    ): Int {
        val rate = hourlyRefillRate.coerceAtLeast(0)
        val accrued = if (rate > 0) calculateRefillsPassed(nextRefillEpoch, now) * rate else 0L
        val capacity = maxDice.coerceAtLeast(0)
        return (currentDice.toLong().coerceAtLeast(0L) + accrued)
            .coerceIn(0L, capacity.toLong())
            .toInt()
    }

    /**
     * Total minutes until the dice pool reaches capacity, starting from
     * [minutesToNextRefill].
     *
     * Inputs below zero are clamped to zero. Validation of user-supplied
     * minutes (spec range `0..60`) is the responsibility of the update layer;
     * this function accepts computed values such as far-future epochs.
     */
    fun calculateTotalMinutesToFull(
        currentDice: Int,
        maxDice: Int,
        hourlyRefillRate: Int,
        minutesToNextRefill: Long,
    ): Long {
        val minutes = minutesToNextRefill.coerceAtLeast(0L)
        val cycles = calculateFullCyclesNeeded(currentDice, maxDice, hourlyRefillRate)
        return minutes + cycles.toLong() * TimeConstants.MINUTES_PER_HOUR
    }

    /**
     * Whole minutes until [nextRefillEpoch], counting up so the refill is
     * never announced early. Returns zero when the moment has passed.
     */
    fun calculateMinutesToNextRefill(nextRefillEpoch: Long, now: Long): Long {
        val remainingMillis = nextRefillEpoch - now
        if (remainingMillis <= 0) return 0
        return ceilDiv(remainingMillis, TimeConstants.MILLIS_PER_MINUTE)
    }

    /**
     * Epoch milliseconds at which the dice pool is projected to reach capacity.
     *
     * Returns `null` when the pool is already full (the projection is
     * automatically disabled). With a non-positive [hourlyRefillRate] the
     * projection degrades to the next refill moment.
     */
    fun calculateProjectionEpoch(
        currentDice: Int,
        maxDice: Int,
        hourlyRefillRate: Int,
        nextRefillEpoch: Long,
        now: Long,
    ): Long? {
        if (isDiceFull(currentDice, maxDice)) return null
        val minutesToNext = calculateMinutesToNextRefill(nextRefillEpoch, now)
        val totalMinutes = calculateTotalMinutesToFull(
            currentDice = currentDice,
            maxDice = maxDice,
            hourlyRefillRate = hourlyRefillRate,
            minutesToNextRefill = minutesToNext,
        )
        return now + totalMinutes * TimeConstants.MILLIS_PER_MINUTE
    }

    /**
     * Milliseconds until the Free Gift becomes claimable, clamped to zero.
     */
    fun calculateGiftRemainingMillis(freeGiftEpoch: Long, now: Long): Long =
        (freeGiftEpoch - now).coerceAtLeast(0L)

    /**
     * Milliseconds until the active season ends, clamped to zero.
     */
    fun calculateSeasonRemainingMillis(seasonEndEpoch: Long, now: Long): Long =
        (seasonEndEpoch - now).coerceAtLeast(0L)

    /**
     * Progress fraction `[0.0, 1.0]` of the dice pool, for progress indicators.
     *
     * A non-positive [maxDice] yields zero progress.
     */
    fun calculateProgress(currentDice: Int, maxDice: Int): Float {
        if (maxDice <= 0) return 0f
        return (currentDice.toFloat() / maxDice.toFloat()).coerceIn(0f, 1f)
    }

    /**
     * Ceiling integer division for non-negative [dividend] and positive
     * [divisor], avoiding floating point rounding issues.
     */
    private fun ceilDiv(dividend: Long, divisor: Long): Long =
        (dividend + divisor - 1) / divisor

    private fun ceilDiv(dividend: Int, divisor: Int): Int =
        (dividend + divisor - 1) / divisor
}
