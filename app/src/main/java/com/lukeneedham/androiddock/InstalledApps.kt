package com.lukeneedham.androiddock

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** An app's name and icon, for the lists of apps. */
internal class AppEntry(val packageName: String, val label: String, val icon: ImageBitmap?)

private const val APP_ICON_PX = 96

/** Names and icons of the apps on the device, from the package manager. Slow: call it off the main thread. */
internal class InstalledApps(private val context: Context) {
    fun loadAppEntry(packageName: String): AppEntry {
        val packageManager = context.packageManager
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            AppEntry(
                packageName,
                packageManager.getApplicationLabel(info).toString(),
                packageManager.getApplicationIcon(info).toBitmap(APP_ICON_PX, APP_ICON_PX).asImageBitmap(),
            )
        } catch (e: Exception) {
            AppEntry(packageName, packageName, null)
        }
    }

    /** Every app with a launcher icon, by name. */
    fun loadLaunchableApps(): List<AppEntry> =
        context.packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .map { loadAppEntry(it) }
            .sortedBy { it.label.lowercase() }
}
