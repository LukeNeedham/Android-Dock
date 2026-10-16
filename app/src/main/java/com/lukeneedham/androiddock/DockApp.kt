package com.lukeneedham.androiddock

import android.app.Application
import android.content.pm.ApplicationInfo
import com.lukeneedham.common.DebugKit
import com.lukeneedham.common.DebugKitConfig

/**
 * Starts the shared debug kit (logger and crash handler). This runs when the process is created,
 * before any activity or the accessibility service, so everything that logs afterwards is covered.
 */
class DockApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        DebugKit.init(DebugKitConfig(enabled = isDebuggable))
    }
}
