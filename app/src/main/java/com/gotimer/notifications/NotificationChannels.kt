package com.gotimer.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Registers the three notification channels specified for GO! Timer.
 *
 * Channel registration is idempotent and must happen before any notification
 * referencing a channel is posted; the notification receiver re-registers
 * defensively before posting.
 */
object NotificationChannels {

    /** High priority channel for full dice capacity alerts. */
    const val CHANNEL_DICE_ALERTS = "channel_dice_alerts"

    /** Medium priority channel for Free Gift availability alerts. Uses the default importance tier. */
    const val CHANNEL_GIFT_ALERTS = "channel_gift_alerts"

    /** Default priority channel for season ending reminders. */
    const val CHANNEL_SEASON_ALERTS = "channel_season_alerts"

    /** Low priority channel for the persistent status notification. */
    const val CHANNEL_PERSISTENT_STATUS = "channel_persistent_status"

    /**
     * Creates the three channels on the device. Safe to call repeatedly; an
     * existing channel's settings are left untouched by the system.
     */
    fun register(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_DICE_ALERTS,
                    "Dice Alerts",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "Notifies when your dice pool is full" },
                NotificationChannel(
                    CHANNEL_GIFT_ALERTS,
                    "Free Gift Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Notifies when the Free Gift is ready to claim" },
                NotificationChannel(
                    CHANNEL_SEASON_ALERTS,
                    "Season Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Reminds you before the season ends" },
                NotificationChannel(
                    CHANNEL_PERSISTENT_STATUS,
                    "Persistent Status",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Shows ongoing dice, refill, and gift status"
                    setShowBadge(false)
                },
            ),
        )
    }
}
