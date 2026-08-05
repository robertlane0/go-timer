package com.gotimer.notifications

import android.app.NotificationManager
import android.content.Context
import com.gotimer.calculations.CountdownFormatter
import com.gotimer.calculations.ProjectionCalculator
import com.gotimer.model.AppState
import com.gotimer.repository.DiceRepository
import kotlinx.coroutines.flow.first

/**
 * Manages the persistent status notification that shows ongoing dice, refill,
 * and gift information in the notification shade.
 *
 * The notification is non-dismissible (ongoing) and is shown only when the
 * user has enabled it in settings. All updates go through [update], which
 * reads the latest state from [repository] and rebuilds the notification
 * content. When the feature is disabled or notifications are off, the
 * notification is canceled.
 *
 * @param context Application context used to access NotificationManager.
 * @param repository Source of the application state.
 */
class PersistentNotificationManager(
    private val context: Context,
    private val repository: DiceRepository,
) {

    private val notificationManager: NotificationManager
        get() = context.getSystemService(NotificationManager::class.java)

    /**
     * Reads the current state and updates (or cancels) the persistent
     * notification accordingly. Safe to call at any time; no-op when the
     * feature is disabled.
     */
    suspend fun update() {
        val state = repository.appState.first()
        if (!state.settings.persistentNotificationEnabled ||
            !state.settings.notificationsEnabled
        ) {
            cancel()
            return
        }
        val now = System.currentTimeMillis()
        show(state, now)
    }

    /**
     * Cancels the persistent notification if it is currently posted.
     */
    fun cancel() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun show(state: AppState, now: Long) {
        NotificationChannels.register(context)
        val notification = buildNotification(state, now)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(state: AppState, now: Long): android.app.Notification {
        val effectiveDice = ProjectionCalculator.calculateEffectiveDice(
            currentDice = state.currentDice,
            maxDice = state.maxDice,
            hourlyRefillRate = state.refillRatePerHour,
            nextRefillEpoch = state.nextRefillEpoch,
            now = now,
        )
        val nextRefillEpoch = ProjectionCalculator.calculateNextRefillEpoch(
            state.nextRefillEpoch,
            now,
        )
        val giftRemaining = ProjectionCalculator.calculateGiftRemainingMillis(
            state.freeGiftEpoch,
            now,
        )

        val diceText = "$effectiveDice / ${state.maxDice} dice"
        val refillText = if (effectiveDice >= state.maxDice) {
            "Dice full"
        } else {
            "Refill at ${CountdownFormatter.formatClockTime(nextRefillEpoch)}"
        }
        val giftText = if (giftRemaining == 0L) {
            "Gift ready to claim"
        } else {
            "Gift at ${CountdownFormatter.formatClockTime(state.freeGiftEpoch)}"
        }

        return NotificationFactory.persistentStatus(context, diceText, refillText, giftText)
    }

    private companion object {
        const val NOTIFICATION_ID = 200
    }
}
