package com.gotimer.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gotimer.datastore.appDataStore
import com.gotimer.notifications.PersistentNotificationManager
import com.gotimer.repository.DiceRepository
import com.gotimer.ui.widgets.GoTimerWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Rebuilds all notification alarms after a device reboot, since alarms do not
 * survive power cycles, refreshes the home screen widget, and restores the
 * persistent status notification if enabled.
 *
 * Reads the persisted state, recalculates remaining times, and arms the full
 * plan via [NotificationScheduler]. Uses [goAsync] so the receiver stays alive
 * while the DataStore is read.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val repository = DiceRepository(appContext.appDataStore)
                NotificationScheduler(appContext, repository).rescheduleAll()
                GoTimerWidget().updateAll(appContext)
                PersistentNotificationManager(appContext, repository).update()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
