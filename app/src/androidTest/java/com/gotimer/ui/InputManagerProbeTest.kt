package com.gotimer.ui

import android.hardware.input.InputManager
import android.os.Build
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Guards the instrumented UI tests against the known Android 17 (API 37)
 * breakage: the platform removed `InputManager.getInstance()`, which compose
 * ui-test (through 1.11.4) still calls when injecting gestures. On such
 * images this test skips and the rest of the suite is reported as skipped
 * rather than failing. Runs normally on API 36 and below.
 */
class InputManagerProbeTest {

    @Test
    fun composeGestureInjectionSupported() {
        val methodExists = runCatching {
            InputManager::class.java.getMethod("getInstance").invoke(null) != null
        }.getOrDefault(false)
        assumeTrue(
            "InputManager.getInstance() was removed on API ${Build.VERSION.SDK_INT}; " +
                "compose ui-test cannot inject gestures on this image",
            methodExists,
        )
    }
}
