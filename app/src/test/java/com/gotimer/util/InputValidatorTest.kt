package com.gotimer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InputValidatorTest {

    @Test
    fun `clamp keeps in-range values and clamps out-of-range values`() {
        assertEquals(5, InputValidator.clamp(5, 0, 10))
        assertEquals(0, InputValidator.clamp(-3, 0, 10))
        assertEquals(10, InputValidator.clamp(25, 0, 10))
    }

    @Test
    fun `clamp returns min in degenerate ranges`() {
        assertEquals(7, InputValidator.clamp(3, 7, 2))
    }

    @Test
    fun `clampDiceCount enforces the zero to max range`() {
        assertEquals(32, InputValidator.clampDiceCount(32, 80))
        assertEquals(0, InputValidator.clampDiceCount(-4, 80))
        assertEquals(80, InputValidator.clampDiceCount(120, 80))
        assertEquals(0, InputValidator.clampDiceCount(5, -80))
    }

    @Test
    fun `clampRefillMinutes enforces the zero to sixty range`() {
        assertEquals(30, InputValidator.clampRefillMinutes(30))
        assertEquals(0, InputValidator.clampRefillMinutes(-5))
        assertEquals(60, InputValidator.clampRefillMinutes(90))
    }

    @Test
    fun `timestamps are valid only when strictly positive`() {
        assertTrue(InputValidator.isValidTimestamp(1))
        assertTrue(InputValidator.isValidTimestamp(1_700_000_000_000))
        assertFalse(InputValidator.isValidTimestamp(0))
        assertFalse(InputValidator.isValidTimestamp(-100))
    }

    @Test
    fun `fallbackEpoch protects against corrupt timestamps`() {
        val fallback = 42L
        assertEquals(123L, InputValidator.fallbackEpoch(123L, fallback))
        assertEquals(fallback, InputValidator.fallbackEpoch(0L, fallback))
        assertEquals(fallback, InputValidator.fallbackEpoch(-5L, fallback))
    }
}
