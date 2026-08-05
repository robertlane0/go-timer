package com.gotimer.scheduler

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gotimer.datastore.appDataStore
import com.gotimer.notifications.NotificationChannels
import com.gotimer.notifications.NotificationFactory
import com.gotimer.notifications.PersistentNotificationManager
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
 * a single deferred alarm without disturbing the rest of the plan. The
 * persistent status notification is refreshed after mutations, re-posted when
 * the user dismisses it, and re-posted at state boundaries via the
 * [NotificationType.PERSISTENT_REFRESH] trigger.
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
                    PersistentNotificationManager(context, repository).update()
                }
            NotificationAction.CLAIMED.action ->
                runQuickAction(context, intent) { repository, _ ->
                    repository.claimFreeGift()
                    NotificationScheduler(context, repository).rescheduleAll()
                    PersistentNotificationManager(context, repository).update()
                }
            NotificationAction.SNOOZE.action ->
                runQuickAction(context, intent) { repository, type ->
                    NotificationScheduler(context, repository).snooze(type)
                }
            NotificationAction.PERSISTENT_STATUS_DISMISSED.action ->
                restorePersistentStatus(context)
            NotificationType.PERSISTENT_REFRESH.action ->
                runPersistentRefresh(context)
        }
    }

    /**
     * Re-posts the persistent status notification at a state boundary and
     * re-arms the next boundary alarm. Held open with [goAsync] because the
     * work needs DataStore reads. No-op when the tile is disabled (the
     * manager cancels it and the plan arms nothing).
     */
    private fun runPersistentRefresh(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = DiceRepository(context.appDataStore)
                PersistentNotificationManager(context, repository).update()
                NotificationScheduler(context, repository).rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Re-posts the persistent status notification after the user dismissed
     * it, so it reappears instead of staying gone. Held open with [goAsync]
     * because rebuilding requires a DataStore read. No-op when the feature is
     * disabled (the manager cancels instead).
     */
    private fun restorePersistentStatus(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = DiceRepository(context.appDataStore)
                PersistentNotificationManager(context, repository).update()
            } finally {
                pendingResult.finish()
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
