package com.lukeneedham.androiddock

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
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

    /** Whether the adb grant has been done, which lets the app switch its own service back on. */
    fun canWriteSecureSettings(context: Context) =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Switches the accessibility service back on if the system turned it off, as it can when the
     * app is updated. Needs the adb grant. Returns whether the service is now enabled.
     */
    fun enableAccessibility(context: Context): Boolean {
        if (isAccessibilityEnabled(context)) return true
        if (!canWriteSecureSettings(context)) return false
        val service = ComponentName(context, DockAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        )
        val updated = if (enabled.isNullOrEmpty()) service else "$enabled:$service"
        return try {
            Settings.Secure.putString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, updated)
            Settings.Secure.putInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
            true
        } catch (e: SecurityException) {
            false
        }
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
