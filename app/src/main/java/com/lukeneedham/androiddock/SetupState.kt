package com.lukeneedham.androiddock

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.os.Process
import android.provider.Settings

/** Whether each permission the dock needs has been granted. */
object SetupState {

    fun isAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val service = ComponentName(context, DockAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == service }
    }

    @Suppress("DEPRECATION")
    fun isUsageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isComplete(context: Context) =
        isAccessibilityEnabled(context) && isUsageAccessGranted(context)
}
