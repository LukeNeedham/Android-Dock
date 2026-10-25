package com.lukeneedham.androiddock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

/** What the home page shows. */
internal data class SettingsUiState(
    val accessibilityOk: Boolean,
    val usageAccessOk: Boolean,
    val fan: FanPreviewState,
    val sortType: DockPrefs.SortType,
    val hiddenCount: Int,
)
