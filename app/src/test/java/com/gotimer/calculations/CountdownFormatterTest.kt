package com.gotimer.calculations

import com.gotimer.util.TimeConstants
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class CountdownFormatterTest {

    @Test
    fun `countdown shows days hours and minutes for multi-day durations`() {
        val millis = 14 * TimeConstants.MILLIS_PER_DAY + 6 * TimeConstants.MILLIS_PER_HOUR + 22 * TimeConstants.MILLIS_PER_MINUTE
        assertEquals("14d 06h 22m", CountdownFormatter.formatCountdown(millis))
    }

    @Test
    fun `countdown omits zero days and zero seconds`() {
        assertEquals("4h 24m", CountdownFormatter.formatCountdown(4 * TimeConstants.MILLIS_PER_HOUR + 24 * TimeConstants.MILLIS_PER_MINUTE))
    }

    @Test
    fun `countdown falls back to minutes and seconds under one hour`() {
        assertEquals(
            "24m 15s",
            CountdownFormatter.formatCountdown(24 * TimeConstants.MILLIS_PER_MINUTE + 15_000),
        )
    }

    @Test
    fun `countdown handles zero and negative durations`() {
        assertEquals("00m 00s", CountdownFormatter.formatCountdown(0))
        assertEquals("00m 00s", CountdownFormatter.formatCountdown(-TimeConstants.MILLIS_PER_MINUTE))
    }

    @Test
    fun `countdown with seconds always includes seconds`() {
        val millis = 5 * TimeConstants.MILLIS_PER_HOUR + 42 * TimeConstants.MILLIS_PER_MINUTE + 10_000
        assertEquals("05h 42m 10s", CountdownFormatter.formatCountdownWithSeconds(millis))
    }

    @Test
    fun `countdown with seconds shows days and hours`() {
        val millis = TimeConstants.MILLIS_PER_DAY + 3 * TimeConstants.MILLIS_PER_HOUR + 5 * TimeConstants.MILLIS_PER_MINUTE + 7_000
        assertEquals("1d 03h 05m 07s", CountdownFormatter.formatCountdownWithSeconds(millis))
    }

    @Test
    fun `countdown with seconds is zero-safe`() {
        assertEquals("00m 00s", CountdownFormatter.formatCountdownWithSeconds(0))
        assertEquals("00m 00s", CountdownFormatter.formatCountdownWithSeconds(-1))
    }

    @Test
    fun `clock time formats as twelve hour with am and pm`() {
        val utc = ZoneId.of("UTC")

        val evening = ZonedDateTime.of(2026, 1, 1, 20, 30, 0, 0, utc).toInstant().toEpochMilli()
        assertEquals("8:30 PM", CountdownFormatter.formatClockTime(evening, utc))

        val morning = ZonedDateTime.of(2026, 1, 1, 8, 5, 0, 0, utc).toInstant().toEpochMilli()
        assertEquals("8:05 AM", CountdownFormatter.formatClockTime(morning, utc))

        val midnight = ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, utc).toInstant().toEpochMilli()
        assertEquals("12:00 AM", CountdownFormatter.formatClockTime(midnight, utc))
    }
}
