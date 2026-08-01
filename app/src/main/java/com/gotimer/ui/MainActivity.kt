package com.gotimer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gotimer.notifications.NotificationChannels
import com.gotimer.ui.dashboard.DashboardScreen
import com.gotimer.ui.settings.SettingsScreen
import com.gotimer.ui.theme.GoTimerTheme
import com.gotimer.ui.update.UpdateSheet
import com.gotimer.ui.widgets.GoTimerWidget
import androidx.glance.appwidget.updateAll
import com.gotimer.viewmodel.AppViewModelFactory
import com.gotimer.viewmodel.DashboardViewModel
import com.gotimer.viewmodel.SettingsViewModel
import com.gotimer.viewmodel.UpdateViewModel
import kotlinx.coroutines.launch

/**
 * Single-activity app hosting the dashboard, settings window, and update
 * sheet. Navigation between the dashboard and settings is simple state-based
 * switching; the update sheet is a modal bottom sheet.
 */
class MainActivity : ComponentActivity() {

    private val viewModelFactory by lazy { AppViewModelFactory.create(this) }

    private val dashboardViewModel: DashboardViewModel by viewModels { viewModelFactory }
    private val settingsViewModel: SettingsViewModel by viewModels { viewModelFactory }
    private val updateViewModel: UpdateViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationChannels.register(this)
        setContent {
            GoTimerTheme {
                var showSettings by rememberSaveable { mutableStateOf(false) }
                var showUpdateSheet by rememberSaveable { mutableStateOf(false) }
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }

                requestNotificationPermissionIfNeeded(permissionLauncher)

                if (showSettings) {
                    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
                    SettingsScreen(
                        settings = settings,
                        onSave = settingsViewModel::save,
                        onBack = { showSettings = false },
                    )
                } else {
                    val uiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                    DashboardScreen(
                        uiState = uiState,
                        onJustPlayed = dashboardViewModel::onJustPlayed,
                        onClaimFreeGift = dashboardViewModel::onClaimFreeGift,
                        onOpenUpdate = { showUpdateSheet = true },
                        onOpenSettings = { showSettings = true },
                    )
                }

                if (showUpdateSheet) {
                    val updateUiState by updateViewModel.uiState.collectAsStateWithLifecycle()
                    UpdateSheet(
                        uiState = updateUiState,
                        onSelectDice = updateViewModel::selectDice,
                        onSelectRefillMinutes = updateViewModel::selectRefillMinutes,
                        onSelectGiftOption = updateViewModel::selectGiftOption,
                        onCustomDiceInput = updateViewModel::onCustomDiceInput,
                        onClearSelection = updateViewModel::clearSelection,
                        onSave = {
                            updateViewModel.save()
                            showUpdateSheet = false
                        },
                        onDismiss = { showUpdateSheet = false },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Refresh the home screen widget whenever the app is opened so it
        // reflects the latest persisted state without waiting for the
        // periodic launcher update.
        lifecycleScope.launch {
            GoTimerWidget().updateAll(this@MainActivity)
        }
    }

    /**
     * Requests the POST_NOTIFICATIONS runtime permission on Android 13+ if it
     * has not been granted yet. No-op elsewhere.
     */
    private fun requestNotificationPermissionIfNeeded(
        launcher: androidx.activity.compose.ManagedActivityResultLauncher<String, Boolean>,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
