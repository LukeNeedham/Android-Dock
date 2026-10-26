package com.lukeneedham.androiddock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * What the user sees when they open the app: a checklist that walks them through the setup the
 * dock needs, plus links to the trigger and fan layout pages. The setup steps are re-checked every
 * time the user comes back from system settings. It only calls [Root]; the corner sheet is the separate
 * [DockActivity], which has to be its own window.
 */
class MainActivity : ComponentActivity() {

    /** Bumped on every resume so the setup checks re-run. */
    private var resumes by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openCount++
        setContent { Root(resumes) }
    }

    override fun onDestroy() {
        openCount--
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        resumes++
    }

    companion object {
        @Volatile
        private var openCount = 0

        /** Whether the settings app is open now, so the sheet lists it like any other app. */
        val isOpen get() = openCount > 0
    }
}
