package com.gotimer.calculations

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats durations and epoch timestamps into the display strings used across
 * the dashboard.
 *
 * All formatters are pure and locale/zone aware only through explicit
 * parameters. Two countdown styles are offered to match the specification:
 * a compact style without seconds (hero banner, dice card) and a style that
 * always includes seconds (Free Gift card).
 */
object CountdownFormatter {

    private val CLOCK_TIME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.US)

    /**
     * Formats [millis] without seconds, compact style:
     * `14d 06h 22m` / `4h 24m` / `24m 15s`. Hours are zero-padded only when
     * days are present. Negative durations format as zero.
     */
    fun formatCountdown(millis: Long): String {
        val (days, hours, minutes, seconds) = split(millis)
        return when {
            days > 0 -> "%dd %02dh %02dm".format(days, hours, minutes)
            hours > 0 -> "%dh %02dm".format(hours, minutes)
            else -> "%02dm %02ds".format(minutes, seconds)
        }
    }

    /**
     * Formats [millis] always including seconds:
     * `14d 06h 22m 03s` / `05h 42m 10s` / `24m 15s`. Negative durations
     * format as zero.
     */
    fun formatCountdownWithSeconds(millis: Long): String {
        val (days, hours, minutes, seconds) = split(millis)
        return when {
            days > 0 -> "%dd %02dh %02dm %02ds".format(days, hours, minutes, seconds)
            hours > 0 -> "%02dh %02dm %02ds".format(hours, minutes, seconds)
            else -> "%02dm %02ds".format(minutes, seconds)
        }
    }

    /**
     * Formats [epoch] as a 12-hour clock time such as `8:30 PM` in [zoneId].
     */
    fun formatClockTime(epoch: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val zoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), zoneId)
        return zoned.format(CLOCK_TIME_FORMATTER)
    }

    /**
     * Splits [millis] into day/hour/minute/second components for a display
     * string. Non-positive durations collapse to all zeros.
     */
    private fun split(millis: Long): TimeParts {
        val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
        return TimeParts(
            days = (totalSeconds / 86_400L).toInt(),
            hours = (totalSeconds % 86_400L / 3_600L).toInt(),
            minutes = (totalSeconds % 3_600L / 60L).toInt(),
            seconds = (totalSeconds % 60L).toInt(),
        )
    }

    private data class TimeParts(
        val days: Int,
        val hours: Int,
        val minutes: Int,
        val seconds: Int,
    )
}
