package com.gotimer.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Verifies that settings survive an activity recreation (the ViewModel is
 * destroyed and re-reads DataStore), which is the closest instrumented
 * equivalent to an app reload.
 */
class SettingsPersistenceTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun grantNotificationPermission() {
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant com.gotimer android.permission.POST_NOTIFICATIONS")
            .close()
    }

    @Test
    fun seasonNameSurvivesActivityRecreation() {
        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("Season Name").performTextClearance()
        composeRule.onNodeWithText("Season Name").performTextInput("QA Season")
        composeRule.onNodeWithText("SAVE").performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitForIdle()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("QA Season").assertExists()
    }
}
