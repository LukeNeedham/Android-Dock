package com.lukeneedham.androiddock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Runs after the app is updated. The system can switch the accessibility service off when it
 * does, so this turns it back on, if the adb grant that allows it has been done.
 */
class UpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val wasEnabled = SetupState.isAccessibilityEnabled(context)
        val enabled = SetupState.enableAccessibility(context)
        DockLog.log(
            context,
            "app updated: accessibility " + when {
                wasEnabled -> "still on"
                enabled -> "was off, switched back on"
                SetupState.canWriteSecureSettings(context) -> "was off, could not switch on"
                else -> "was off, adb grant missing so not switched on"
            },
        )
    }
}
