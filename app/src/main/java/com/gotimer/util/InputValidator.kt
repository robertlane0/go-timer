package com.gotimer.util

/**
 * Validation and guard rails for user-supplied values.
 *
 * The application must never crash on invalid input: values are clamped to
 * their specification ranges and impossible timestamps fall back safely.
 * These functions live outside the calculation engine because they protect
 * the UI boundary, while projection math assumes already-valid inputs.
 */
object InputValidator {

    /** Clamps [value] into `[min, max]`. Returns [min] in the degenerate `min > max` case. */
    fun clamp(value: Int, min: Int, max: Int): Int {
        if (min > max) return min
        return value.coerceIn(min, max)
    }

    /**
     * Clamps a current dice count into `0..maxDice` as required by the
     * specification. A negative capacity is treated as zero.
     */
    fun clampDiceCount(currentDice: Int, maxDice: Int): Int =
        clamp(currentDice, MIN_DICE, maxDice.coerceAtLeast(0))

    /**
     * Clamps refill minutes into `0..60`, the specification's valid range.
     */
    fun clampRefillMinutes(minutes: Int): Int =
        clamp(minutes, MIN_REFILL_MINUTES, MAX_REFILL_MINUTES)

    /**
     * True when [epoch] is a plausible epoch-milliseconds timestamp
     * (strictly positive). Zero and negatives represent unset or corrupt data.
     */
    fun isValidTimestamp(epoch: Long): Boolean = epoch > 0

    /**
     * Returns [epoch] when valid, otherwise [fallback]. Guards consumers
     * against corrupted persisted timestamps.
     */
    fun fallbackEpoch(epoch: Long, fallback: Long): Long =
        if (isValidTimestamp(epoch)) epoch else fallback

    private const val MIN_DICE = 0
    private const val MIN_REFILL_MINUTES = 0
    private const val MAX_REFILL_MINUTES = 60
}
