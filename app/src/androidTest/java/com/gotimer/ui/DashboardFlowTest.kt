package com.gotimer.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end flows through the real MainActivity, ViewModels, and DataStore:
 * the Quick Update sheet applying chip choices and the "Just Played" batch
 * action reflecting back on the dashboard.
 */
class DashboardFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun grantNotificationPermission() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant com.gotimer android.permission.POST_NOTIFICATIONS")
            .close()
    }

    @Test
    fun updateSheetSavesSelectedDiceToDashboard() {
        composeRule.onNodeWithText("UPDATE").performClick()

        composeRule.onNodeWithText("60").performClick()
        composeRule.onNodeWithText("SAVE").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("60 / 80").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("60 / 80").assertExists()
    }

    @Test
    fun justPlayedAppliesDefaultActionToDashboard() {
        composeRule.onNodeWithText("JUST PLAYED", substring = true).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("0 / 80").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("0 / 80").assertExists()
    }
}
