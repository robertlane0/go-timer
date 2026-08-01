package com.gotimer.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.gotimer.R
import com.gotimer.ui.MainActivity

/**
 * Builds the concrete notifications posted by the notification receiver.
 *
 * Pure presentation: text, channel, icon, and a tap-through content intent.
 * Channel constants come from [NotificationChannels].
 */
object NotificationFactory {

    /**
     * Notification for the full dice pool, on the high priority dice channel.
     */
    fun diceFull(context: Context): Notification =
        baseBuilder(context, NotificationChannels.CHANNEL_DICE_ALERTS)
            .setContentTitle("Dice are full")
            .setContentText("Your dice pool has reached maximum capacity.")
            .build()

    /**
     * Notification that the Free Gift is claimable, on the gift channel.
     */
    fun giftReady(context: Context): Notification =
        baseBuilder(context, NotificationChannels.CHANNEL_GIFT_ALERTS)
            .setContentTitle("Free Gift ready")
            .setContentText("Your free gift from the store is waiting.")
            .build()

    /**
     * Season reminder with [hoursRemaining] (1 or 24), on the season channel.
     */
    fun seasonReminder(context: Context, hoursRemaining: Int): Notification =
        baseBuilder(context, NotificationChannels.CHANNEL_SEASON_ALERTS)
            .setContentTitle("Season ending soon")
            .setContentText("The current season ends in $hoursRemaining hour" +
                if (hoursRemaining == 1) "!" else "s!")
            .build()

    private fun baseBuilder(context: Context, channelId: String): Notification.Builder {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context).setPriority(Notification.PRIORITY_DEFAULT)
        }
        return builder
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
}
