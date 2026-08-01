package com.gotimer.scheduler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.gotimer.repository.DiceRepository
import kotlinx.coroutines.flow.first

/**
 * Central scheduler that arms and cancels the application's notification
 * alarms through [AlarmManager].
 *
 * Events are computed by [SchedulePlanner] from the persisted state and
 * scheduled as one-shot exact alarms with a graceful, inexact fallback where
 * exact alarms are not permitted (API 31+ special access). Alarms point at
 * [NotificationReceiver], which posts the actual notification.
 *
 * @param context Application context used to reach AlarmManager.
 * @param repository Source of the state every (re)schedule is derived from.
 */
class NotificationScheduler(
    private val context: Context,
    private val repository: DiceRepository,
) : NotificationRescheduler {

    /**
     * Recomputes the full alarm plan from the persisted state at [now] and
     * arms it, replacing any previously scheduled alarms.
     */
    override suspend fun rescheduleAll(now: Long) {
        val state = repository.appState.first()
        val plan = SchedulePlanner.buildPlan(state, now)
        cancelAll()
        plan.forEach { scheduleAlarm(it.type, it.triggerAtMillis) }
    }

    /**
     * Cancels every alarm the scheduler could have armed. Notification
     * channels and already-posted notifications are unaffected.
     */
    override fun cancelAll() {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        NotificationType.entries.forEach { type ->
            alarmManager.cancel(pendingIntent(type))
        }
    }

    /**
     * Re-arms the alarm for [type] to fire [minutes] from now without
     * touching the rest of the plan. Used by the snooze quick action.
     */
    override fun snooze(type: NotificationType, minutes: Int) {
        scheduleAlarm(type, System.currentTimeMillis() + minutes * MILLIS_PER_MINUTE)
    }

    @SuppressLint("MissingPermission")
    private fun scheduleAlarm(type: NotificationType, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntent(type)
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                INEXACT_WINDOW_MILLIS,
                pendingIntent,
            )
        }
    }

    /**
     * Exact alarms are default-granted below API 31 and via
     * `USE_EXACT_ALARM` on API 33+; on API 31-32 they depend on the
     * `SCHEDULE_EXACT_ALARM` special access.
     */
    private fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun pendingIntent(type: NotificationType): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).setAction(type.action)
        return PendingIntent.getBroadcast(
            context,
            type.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val INEXACT_WINDOW_MILLIS = 10 * 60_000L
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
