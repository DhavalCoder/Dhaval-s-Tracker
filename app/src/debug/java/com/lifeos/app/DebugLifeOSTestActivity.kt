package com.lifeos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/** Debug-only host activity used by Compose instrumentation tests in the target app process. */
class DebugLifeOSTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LockScreen(onUnlock = {}) }
    }
}
