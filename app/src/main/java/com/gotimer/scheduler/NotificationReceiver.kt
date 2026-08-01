package com.gotimer.scheduler

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gotimer.notifications.NotificationChannels
import com.gotimer.notifications.NotificationFactory

/**
 * Receives the alarm broadcasts armed by [NotificationScheduler] and posts
 * the corresponding notification.
 *
 * Channels are re-registered before posting as a cheap, idempotent safety
 * net. Unknown actions are ignored.
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationChannels.register(context)
        val manager = context.getSystemService(NotificationManager::class.java)

        when (intent.action) {
            NotificationType.DICE_FULL.action ->
                post(manager, NotificationType.DICE_FULL, NotificationFactory.diceFull(context))
            NotificationType.GIFT_READY.action ->
                post(manager, NotificationType.GIFT_READY, NotificationFactory.giftReady(context))
            NotificationType.SEASON_24H.action ->
                post(manager, NotificationType.SEASON_24H, NotificationFactory.seasonReminder(context, HOURS_24))
            NotificationType.SEASON_1H.action ->
                post(manager, NotificationType.SEASON_1H, NotificationFactory.seasonReminder(context, HOURS_1))
        }
    }

    private fun post(
        manager: NotificationManager,
        type: NotificationType,
        notification: android.app.Notification,
    ) {
        manager.notify(type.notificationId, notification)
    }

    private companion object {
        const val HOURS_24 = 24
        const val HOURS_1 = 1
    }
}
