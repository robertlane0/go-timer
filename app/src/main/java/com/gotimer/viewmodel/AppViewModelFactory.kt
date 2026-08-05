package com.gotimer.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gotimer.datastore.appDataStore
import com.gotimer.notifications.PersistentNotificationManager
import com.gotimer.repository.DiceRepository
import com.gotimer.scheduler.NotificationScheduler

/**
 * Constructs the application's ViewModels with their production dependencies.
 *
 * The repository and scheduler are cheap, stateless wrappers over the shared
 * DataStore and AlarmManager, so each ViewModel can build its own instances
 * without shared mutable state.
 */
object AppViewModelFactory {

    /**
     * Creates a [ViewModelProvider.Factory] bound to [context].
     */
    fun create(context: Context): ViewModelProvider.Factory {
        val appContext = context.applicationContext
        return viewModelFactory {
            initializer {
                DashboardViewModel(
                    repository = repository(appContext),
                    notificationScheduler = scheduler(appContext),
                    persistentNotificationManager = persistentNotificationManager(appContext),
                )
            }
            initializer {
                SettingsViewModel(
                    repository = repository(appContext),
                    notificationScheduler = scheduler(appContext),
                    persistentNotificationManager = persistentNotificationManager(appContext),
                )
            }
            initializer {
                UpdateViewModel(
                    repository = repository(appContext),
                    notificationScheduler = scheduler(appContext),
                )
            }
        }
    }

    private fun repository(context: Context): DiceRepository =
        DiceRepository(context.appDataStore)

    private fun scheduler(context: Context): NotificationScheduler =
        NotificationScheduler(context, repository(context))

    private fun persistentNotificationManager(context: Context): PersistentNotificationManager =
        PersistentNotificationManager(context, repository(context))
}
