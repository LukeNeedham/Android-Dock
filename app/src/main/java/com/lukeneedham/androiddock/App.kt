package com.lukeneedham.androiddock

import android.app.Application
import com.lukeneedham.androiddock.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import com.lukeneedham.common.DebugKit
import com.lukeneedham.common.DebugKitConfig

/**
 * Starts the shared debug kit (logger and crash handler). This runs when the process is created,
 * before any activity or the accessibility service, so everything that logs afterwards is covered.
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugKit.init(DebugKitConfig(enabled = BuildConfig.DEBUG))
        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
    }
}
