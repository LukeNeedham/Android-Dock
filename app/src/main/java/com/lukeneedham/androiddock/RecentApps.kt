package com.lukeneedham.androiddock

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

/** Works out which apps the user has recently had open, from the system's usage events. */
object RecentApps {

    class App(val packageName: String, val label: String, val icon: Drawable)

    /**
     * Up to [limit] of the most recently used apps from the last day, most recent first, not
     * counting the app on screen now. Android gives no list of running apps to ordinary apps, so
     * this is built from usage events (needs usage access). Runs off the main thread.
     *
     * Apps the user dismissed from the sheet stay out until they have been used again.
     *
     * The dock's own screens are left out, except the settings app, which counts as an app like
     * any other while it is open. The sheet and the accessibility service never count.
     */
    fun load(context: Context, limit: Int): List<App> {
        if (!SetupState.isUsageAccessGranted(context)) return emptyList()
        val packages = context.packageManager
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usage.queryEvents(now - WINDOW_MS, now)
        val settingsOpen = SettingsActivity.isOpen
        val lastUsed = HashMap<String, Long>()
        // The activities of each app that have been started and not yet destroyed. Swiping an
        // app away in the system's recents finishes them all, so an app with none left is no
        // longer open, and is left out until it is opened again.
        val liveActivities = HashMap<String, MutableSet<String>>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = event.eventType
            if (type != UsageEvents.Event.ACTIVITY_RESUMED &&
                type != UsageEvents.Event.ACTIVITY_PAUSED &&
                type != ACTIVITY_STOPPED &&
                type != ACTIVITY_DESTROYED
            ) continue
            if (event.packageName == context.packageName) {
                if (!settingsOpen || event.className != SettingsActivity::class.java.name) continue
            }
            val live = liveActivities.getOrPut(event.packageName) { HashSet() }
            val activity = event.className.orEmpty()
            if (type == ACTIVITY_DESTROYED) live.remove(activity) else live.add(activity)
            if (type == UsageEvents.Event.ACTIVITY_RESUMED || type == UsageEvents.Event.ACTIVITY_PAUSED) {
                lastUsed[event.packageName] = event.timeStamp
            }
        }
        val closed = lastUsed.keys.filter { liveActivities[it].isNullOrEmpty() }
        if (closed.isNotEmpty()) DockLog.log(context, "recent apps: left out closed apps $closed")
        lastUsed.keys.removeAll(closed.toSet())
        val launchers = packages
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()
        // The app on screen is the most recently used one. It is already on screen behind the
        // sheet, so it is left out.
        val currentApp = lastUsed.entries.maxByOrNull { it.value }?.key
        val dismissed = DockPrefs.getDismissed(context)
        return lastUsed.entries
            .filter { it.key != currentApp && it.key !in launchers }
            // Dismissed by the user, and not used since.
            .filter { it.value > (dismissed[it.key] ?: 0L) }
            .sortedByDescending { it.value }
            .asSequence()
            .mapNotNull { (pkg, _) ->
                if (packages.getLaunchIntentForPackage(pkg) == null) return@mapNotNull null
                try {
                    val info = packages.getApplicationInfo(pkg, 0)
                    App(pkg, packages.getApplicationLabel(info).toString(), packages.getApplicationIcon(info))
                } catch (e: Exception) {
                    null
                }
            }
            .take(limit)
            .toList()
    }

    /** Opens [app], bringing its task forward if it is already running. */
    fun launch(context: Context, app: App) {
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    // Usage event types that are not in the public API before Android 10; the system reports them
    // either way.
    private const val ACTIVITY_STOPPED = 23
    private const val ACTIVITY_DESTROYED = 24

    private const val WINDOW_MS = 24L * 60 * 60 * 1000
}
