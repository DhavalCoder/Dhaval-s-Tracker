package com.lifeos.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BiometricLockTest {
    @Test
    fun lockScreenRendersOnDevice() {
        val scenario = ActivityScenario.launch(DebugLifeOSTestActivity::class.java)
        try {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertTrue("Lock screen did not appear",
                device.wait(Until.hasObject(By.text("Welcome back")), 10_000))
            assertTrue("Fingerprint unlock control is missing",
                device.hasObject(By.text("Unlock with fingerprint")))
            assertTrue("Fallback explanation is missing",
                device.hasObject(By.textContains("phone PIN, pattern or password")))
        } finally {
            scenario.close()
        }
    }
}
