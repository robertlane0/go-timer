package com.gotimer.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.rules.TemporaryFolder

/**
 * Builds isolated DataStore instances backed by temporary files for tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
object TestDataStores {

    /**
     * Creates a DataStore writing to a fresh file inside [folder], running on
     * the [scheduler] so its work participates in virtual time.
     */
    fun create(
        folder: TemporaryFolder,
        scheduler: TestCoroutineScheduler,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(UnconfinedTestDispatcher(scheduler)),
    ) {
        folder.newFile("test.preferences_pb")
    }
}
