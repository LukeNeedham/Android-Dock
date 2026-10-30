package com.lukeneedham.androiddock.di

import com.lukeneedham.androiddock.BlacklistViewModel
import com.lukeneedham.androiddock.DockSettings
import com.lukeneedham.androiddock.FanBackgroundViewModel
import com.lukeneedham.androiddock.FanLayoutViewModel
import com.lukeneedham.androiddock.InstalledApps
import com.lukeneedham.androiddock.OnboardingViewModel
import com.lukeneedham.androiddock.SettingsViewModel
import com.lukeneedham.androiddock.SetupStatus
import com.lukeneedham.androiddock.TriggerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { DockSettings(androidContext()) }
    single { SetupStatus(androidContext()) }
    single { InstalledApps(androidContext()) }
    viewModelOf(::BlacklistViewModel)
    viewModelOf(::FanBackgroundViewModel)
    viewModelOf(::FanLayoutViewModel)
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::TriggerViewModel)
}
