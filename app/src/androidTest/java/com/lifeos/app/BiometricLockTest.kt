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
    fun lockScreenAppearsAndUnlockActionIsHandled() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertTrue("Lock screen did not appear",
                device.wait(Until.hasObject(By.text("Welcome back")), 8_000))

            val unlock = device.findObject(By.text("Unlock with fingerprint"))
            assertTrue("Unlock control is missing", unlock != null)
            unlock.click()

            // On emulators without an enrolled fingerprint, the app must remain safely locked.
            // On enrolled devices, the Android system biometric prompt should be presented.
            val promptOrLocked = device.wait(
                Until.hasObject(By.text("Unlock LIFEOS")), 3_000
            ) || device.wait(Until.hasObject(By.text("Welcome back")), 3_000)
            assertTrue("Unlock action did not show the system prompt or retain the lock screen",
                promptOrLocked)
        } finally {
            scenario.close()
        }
    }
}
