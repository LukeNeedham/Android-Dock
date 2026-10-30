package com.lukeneedham.androiddock

import android.content.Context

/** [SetupState] for the ViewModels: whether each permission the dock needs has been granted. */
internal class SetupStatus(private val context: Context) {
    fun isAccessibilityEnabled() = SetupState.isAccessibilityEnabled(context)
    fun isUsageAccessGranted() = SetupState.isUsageAccessGranted(context)
}
