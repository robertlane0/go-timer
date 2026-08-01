package com.gotimer.datastore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** Name of the DataStore backing file used by GO! Timer. */
const val APP_DATASTORE_NAME = "go_timer_preferences"

/**
 * Application-scoped [androidx.datastore.core.DataStore] delegate.
 *
 * The delegate guarantees a single DataStore instance per process. The
 * repository layer receives the instance explicitly so tests can supply a
 * DataStore backed by a temporary file.
 */
val Context.appDataStore by preferencesDataStore(name = APP_DATASTORE_NAME)
