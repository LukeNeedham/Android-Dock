package com.lukeneedham.androiddock

import android.app.Application

/** Installs the crash handler, so a crash in any component leaves a trace in the debug log. */
class DockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                DockLog.logCrash(this, thread, error)
            } catch (_: Throwable) {
                // Never let logging hide the crash itself.
            }
            // Rethrow to the system's handler, so the app still crashes as it would have.
            if (previous != null) previous.uncaughtException(thread, error) else throw error
        }
    }
}
