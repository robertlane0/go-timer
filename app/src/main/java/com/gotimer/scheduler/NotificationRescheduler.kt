package com.gotimer.scheduler

/**
 * Abstraction over notification alarm (re)scheduling.
 *
 * ViewModels depend on this interface so they stay testable without an
 * Android context or AlarmManager; [NotificationScheduler] is the production
 * implementation.
 */
interface NotificationRescheduler {

    /**
     * Recomputes the full alarm plan from persisted state and arms it,
     * replacing any previously scheduled alarms.
     */
    suspend fun rescheduleAll(now: Long = System.currentTimeMillis())

    /**
     * Cancels every scheduled alarm.
     */
    fun cancelAll()

    /**
     * Re-arms the alarm for [type] so it fires [minutes] from now, typically
     * used by the notification shade snooze action. The rest of the plan is
     * left untouched.
     */
    fun snooze(type: NotificationType, minutes: Int = SNOOZE_MINUTES)

    /** Default delay for the snooze action, in minutes. */
    companion object {
        const val SNOOZE_MINUTES = 15
    }
}
