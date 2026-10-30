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
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

object KoinModules {
    private val data = module {
        single { DockSettings(androidContext()) }
        single { SetupStatus(androidContext()) }
        single { InstalledApps(androidContext()) }
    }

    private val viewModels = module {
        viewModelOf(::BlacklistViewModel)
        viewModelOf(::FanBackgroundViewModel)
        viewModelOf(::FanLayoutViewModel)
        viewModelOf(::OnboardingViewModel)
        viewModelOf(::SettingsViewModel)
        viewModelOf(::TriggerViewModel)
    }

    val modules: List<Module> = listOf(data, viewModels)
}
