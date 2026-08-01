package com.gotimer.scheduler

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gotimer.datastore.appDataStore
import com.gotimer.notifications.NotificationChannels
import com.gotimer.notifications.NotificationFactory
import com.gotimer.repository.DiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives the alarm broadcasts armed by [NotificationScheduler] and posts
 * the corresponding notification, and handles the shade quick actions built
 * by the notification factory.
 *
 * Channels are re-registered before posting as a cheap, idempotent safety
 * net. Unknown actions are ignored.
 *
 * Quick actions mutate the repository (which lives in DataStore) off the
 * main thread, then re-derive the alarm plan; the snooze action instead arms
 * a single deferred alarm without disturbing the rest of the plan.
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
            NotificationAction.PLAYED.action ->
                runQuickAction(context, intent) { repository, _ ->
                    repository.executeJustPlayedAction()
                    NotificationScheduler(context, repository).rescheduleAll()
                }
            NotificationAction.CLAIMED.action ->
                runQuickAction(context, intent) { repository, _ ->
                    repository.claimFreeGift()
                    NotificationScheduler(context, repository).rescheduleAll()
                }
            NotificationAction.SNOOZE.action ->
                runQuickAction(context, intent) { repository, type ->
                    NotificationScheduler(context, repository).snooze(type)
                }
        }
    }

    private fun post(
        manager: NotificationManager,
        type: NotificationType,
        notification: android.app.Notification,
    ) {
        manager.notify(type.notificationId, notification)
    }

    /**
     * Resolves the action's source [NotificationType], dismisses its
     * notification, and runs [onAction] against the repository off the main
     * thread. The broadcast result is held open until the work completes.
     */
    private fun runQuickAction(
        context: Context,
        intent: Intent,
        onAction: suspend (repository: DiceRepository, type: NotificationType) -> Unit,
    ) {
        val type = NotificationType.entries.firstOrNull {
            it.name == intent.getStringExtra(NotificationAction.EXTRA_NOTIFICATION_TYPE)
        } ?: return
        context.getSystemService(NotificationManager::class.java).cancel(type.notificationId)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                onAction(DiceRepository(context.appDataStore), type)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val HOURS_24 = 24
        const val HOURS_1 = 1
    }
}
