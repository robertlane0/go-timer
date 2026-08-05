package com.gotimer.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.gotimer.R
import com.gotimer.scheduler.NotificationAction
import com.gotimer.scheduler.NotificationReceiver
import com.gotimer.scheduler.NotificationType
import com.gotimer.ui.MainActivity

/**
 * Builds the concrete notifications posted by the notification receiver.
 *
 * Pure presentation: text, channel, icon, a tap-through content intent, and
 * shade quick actions. Channel constants come from [NotificationChannels].
 */
object NotificationFactory {

    /**
     * Notification for the full dice pool, on the high priority dice channel,
     * with a "Just Played" quick action that resets the refill timers.
     */
    fun diceFull(context: Context): Notification =
        baseBuilder(context, NotificationChannels.CHANNEL_DICE_ALERTS)
            .setContentTitle("Dice are full")
            .setContentText("Your dice pool has reached maximum capacity.")
            .addAction(quickAction(context, NotificationType.DICE_FULL, NotificationAction.PLAYED, ACTION_PLAYED_LABEL))
            .addAction(snoozeAction(context, NotificationType.DICE_FULL))
            .build()

    /**
     * Notification that the Free Gift is claimable, on the gift channel,
     * with a "Claimed" quick action that resets the gift timer.
     */
    fun giftReady(context: Context): Notification =
        baseBuilder(context, NotificationChannels.CHANNEL_GIFT_ALERTS)
            .setContentTitle("Free Gift ready")
            .setContentText("Your free gift from the store is waiting.")
            .addAction(quickAction(context, NotificationType.GIFT_READY, NotificationAction.CLAIMED, ACTION_CLAIMED_LABEL))
            .addAction(snoozeAction(context, NotificationType.GIFT_READY))
            .build()

    /**
     * Season reminder with [hoursRemaining] (1 or 24), on the season channel,
     * with a snooze quick action.
     */
    fun seasonReminder(context: Context, hoursRemaining: Int): Notification {
        val type = if (hoursRemaining == HOURS_1) {
            NotificationType.SEASON_1H
        } else {
            NotificationType.SEASON_24H
        }
        return baseBuilder(context, NotificationChannels.CHANNEL_SEASON_ALERTS)
            .setContentTitle("Season ending soon")
            .setContentText("The current season ends in $hoursRemaining hour" +
                if (hoursRemaining == 1) "!" else "s!")
            .addAction(snoozeAction(context, type))
            .build()
    }

    /**
     * Persistent status notification showing current dice, refill, and gift
     * information. Uses [setOngoing][Notification.Builder.setOngoing] so it
     * cannot be swiped away; the app manages its lifecycle.
     *
     * @param diceText e.g. `"450 / 800 dice"`
     * @param refillText e.g. `"Next refill in 45m"` or `"Refill in 45m"`
     * @param giftText e.g. `"Gift ready"` or `"Gift in 2h 15m"`
     */
    fun persistentStatus(
        context: Context,
        diceText: String,
        refillText: String,
        giftText: String,
    ): Notification =
        baseBuilder(context, NotificationChannels.CHANNEL_PERSISTENT_STATUS)
            .setContentTitle("GO! Timer")
            .setContentText(diceText)
            .setSubText("Status")
            .setOngoing(true)
            .setStyle(
                Notification.BigTextStyle()
                    .bigText("$diceText\n$refillText\n$giftText")
                    .setBigContentTitle("GO! Timer"),
            )
            .build()

    private fun snoozeAction(context: Context, type: NotificationType): Notification.Action =
        quickAction(context, type, NotificationAction.SNOOZE, ACTION_SNOOZE_LABEL)

    /**
     * Builds an action that broadcasts [action] back to the notification
     * receiver, tagged with the source [type].
     */
    private fun quickAction(
        context: Context,
        type: NotificationType,
        action: NotificationAction,
        label: String,
    ): Notification.Action {
        val intent = Intent(context, NotificationReceiver::class.java)
            .setAction(action.action)
            .putExtra(NotificationAction.EXTRA_NOTIFICATION_TYPE, type.name)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ACTION_REQUEST_CODE_BASE + type.ordinal * ACTION_REQUEST_CODE_STRIDE + action.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_notification),
            label,
            pendingIntent,
        ).build()
    }

    private fun baseBuilder(context: Context, channelId: String): Notification.Builder {
        return Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(launchIntent(context))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
    }

    private fun launchIntent(context: Context): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val ACTION_REQUEST_CODE_BASE = 1_000
    private const val ACTION_REQUEST_CODE_STRIDE = 10
    private const val ACTION_PLAYED_LABEL = "Just Played"
    private const val ACTION_CLAIMED_LABEL = "Claimed"
    private const val ACTION_SNOOZE_LABEL = "Snooze"
    private const val HOURS_1 = 1
}
