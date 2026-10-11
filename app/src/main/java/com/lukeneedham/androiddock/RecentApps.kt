package com.lukeneedham.androiddock

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper

/** Works out which apps the user has recently had open, from the system's usage events. */
object RecentApps {

    class App(val packageName: String, val label: String, val icon: Drawable)

    /**
     * What the sheet shows: the app on screen now, which is null on the home screen (or when
     * there is nothing to go on), and the recent apps other than it.
     */
    class Recents(val current: App?, val others: List<App>)

    /** The recent apps other than the one on screen; see [loadRecents]. */
    fun load(context: Context, limit: Int): List<App> = loadRecents(context, limit).others

    /**
     * The app on screen now, and up to [limit] of the most recently used apps from the last
     * day, most recent first, not counting the one on screen. Android gives no list of running
     * apps to ordinary apps, so this is built from usage events (needs usage access). Runs off
     * the main thread.
     *
     * The app on screen is not filtered by the blacklist or by dismissals, since it is open
     * whatever the user thinks of it. A launcher on screen means the home screen, so there is
     * no current app.
     *
     * Apps the user dismissed from the sheet stay out until they have been used again, and
     * blacklisted apps never show.
     *
     * The dock's own screens are left out, except the settings app, which counts as an app like
     * any other while it is open. The sheet and the accessibility service never count.
     */
    fun loadRecents(context: Context, limit: Int): Recents {
        if (!SetupState.isUsageAccessGranted(context)) return Recents(null, emptyList())
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
        val current = currentApp?.takeIf { it !in launchers }?.let { appOf(packages, it) }
        val dismissed = DockPrefs.getDismissed(context)
        val blacklist = DockPrefs.getBlacklist(context)
        val others = lastUsed.entries
            // A blacklisted app still counts as the app on screen above; it is only never listed.
            .filter { it.key != currentApp && it.key !in launchers && it.key !in blacklist }
            // Dismissed by the user, and not used since.
            .filter { it.value > (dismissed[it.key] ?: 0L) }
            .sortedByDescending { it.value }
            .asSequence()
            .mapNotNull { (pkg, _) -> appOf(packages, pkg) }
            .take(limit)
            .toList()
        return Recents(current, others)
    }

    /** [pkg] as an [App], or null if it cannot be opened from the sheet. */
    private fun appOf(packages: PackageManager, pkg: String): App? {
        if (packages.getLaunchIntentForPackage(pkg) == null) return null
        return try {
            val info = packages.getApplicationInfo(pkg, 0)
            App(pkg, packages.getApplicationLabel(info).toString(), packages.getApplicationIcon(info))
        } catch (e: Exception) {
            null
        }
    }

    /** Opens [app], bringing its task forward if it is already running. */
    fun launch(context: Context, app: App) {
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /**
     * Asks the system to end [app]'s background processes. Best effort: it does nothing to an app
     * that is on screen or running a foreground service, and gives no result.
     */
    fun kill(context: Context, app: App) {
        // Ending our own processes would take the accessibility service down with them.
        if (app.packageName == context.packageName) return
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        manager.killBackgroundProcesses(app.packageName)
        DockLog.log(context, "asked the system to end ${app.packageName}")
    }

    /**
     * Closes [app], which is on screen. The system will not end the process of an app the user
     * is looking at, so this first opens the home screen, and then asks for [app] to be ended a
     * few times: its process takes a moment to count as background. Call it while the sheet is
     * still on screen, which is what lets it start an activity.
     */
    fun homeAndKill(context: Context, app: App) {
        val appContext = context.applicationContext
        try {
            appContext.startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: RuntimeException) {
            DockLog.log(appContext, "could not open Home to leave ${app.packageName}: $e")
        }
        val handler = Handler(Looper.getMainLooper())
        repeat(KILL_TRIES) { attempt ->
            handler.postDelayed({ kill(appContext, app) }, KILL_FIRST_DELAY_MS + attempt * KILL_RETRY_MS)
        }
    }

    private const val KILL_TRIES = 3
    private const val KILL_FIRST_DELAY_MS = 600L
    private const val KILL_RETRY_MS = 700L

    // Usage event types that are not in the public API before Android 10; the system reports them
    // either way.
    private const val ACTIVITY_STOPPED = 23
    private const val ACTIVITY_DESTROYED = 24

    private const val WINDOW_MS = 24L * 60 * 60 * 1000
}
