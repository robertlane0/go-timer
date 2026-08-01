package com.gotimer.scheduler

/**
 * Quick actions attached to notifications so the user can act directly from
 * the shade without opening the app.
 *
 * @property action Broadcast action routed back to [NotificationReceiver].
 */
enum class NotificationAction(val action: String) {
    /** Marks the dice pool as played down: resets the refill timers. */
    PLAYED("com.gotimer.action.DICE_PLAYED"),

    /** Claims the Free Gift: resets the gift timer. */
    CLAIMED("com.gotimer.action.GIFT_CLAIMED"),

    /** Defers the notification by the scheduler's snooze interval. */
    SNOOZE("com.gotimer.action.SNOOZE"),
    ;

    companion object {
        /**
         * Intent extra holding the [NotificationType] name the action belongs
         * to, e.g. to re-arm the correct snooze alarm.
         */
        const val EXTRA_NOTIFICATION_TYPE = "com.gotimer.extra.NOTIFICATION_TYPE"
    }
}
