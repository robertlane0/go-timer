package com.gotimer.util

/**
 * Shared time arithmetic constants used across the calculation and persistence layers.
 *
 * All timestamps in the application are epoch milliseconds, so these constants keep
 * conversions between minutes, hours, and days free of magic numbers.
 */
object TimeConstants {

    /** Minutes in one hour. */
    const val MINUTES_PER_HOUR = 60

    /** Milliseconds in one minute. */
    const val MILLIS_PER_MINUTE = 60_000L

    /** Milliseconds in one hour. */
    const val MILLIS_PER_HOUR = 3_600_000L

    /** Milliseconds in one day. */
    const val MILLIS_PER_DAY = 86_400_000L
}
