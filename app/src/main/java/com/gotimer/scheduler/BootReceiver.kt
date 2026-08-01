package com.gotimer.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gotimer.datastore.appDataStore
import com.gotimer.repository.DiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Rebuilds all notification alarms after a device reboot, since alarms do not
 * survive power cycles.
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
                val repository = DiceRepository(context.applicationContext.appDataStore)
                NotificationScheduler(context.applicationContext, repository).rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
