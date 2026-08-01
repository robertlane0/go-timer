package com.gotimer.testing

import com.gotimer.scheduler.NotificationRescheduler

/**
 * In-memory stand-in for the notification scheduler that records calls.
 */
class FakeNotificationRescheduler : NotificationRescheduler {

    var rescheduleCount = 0
        private set

    var cancelCount = 0
        private set

    var lastNow: Long? = null
        private set

    override suspend fun rescheduleAll(now: Long) {
        rescheduleCount += 1
        lastNow = now
    }

    override fun cancelAll() {
        cancelCount += 1
    }
}
