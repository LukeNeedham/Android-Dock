package com.lukeneedham.androiddock

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The apps that never show in the sheet ([hidden], with their names and icons), and the apps the user can pick to hide
 * ([pickableApps]: every app with a launcher icon, null until they are loaded).
 */
internal data class BlacklistUiState(
    val hidden: List<AppEntry>,
    val pickableApps: List<AppEntry>? = null,
)
