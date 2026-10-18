package com.lukeneedham.androiddock

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.lukeneedham.common.logger.Logger

/**
 * Places a [CornerTouchView] for each trigger over the chosen corner of the screen, on top of the
 * navigation bar. Each one's position, size and colour come from [DockPrefs]. Touching it opens the sheet, and a long press opens the sheet
 * in its actions mode (Home, Back, Recents, Fan settings).
 */
class DockAccessibilityService : AccessibilityService() {

    private val triggerViews = mutableListOf<CornerTouchView>()
    private val triggerParams = mutableListOf<WindowManager.LayoutParams>()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (DockPrefs.isTriggerKey(key)) syncTriggers()
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Torch.track(this)
        val navMode = Settings.Secure.getInt(contentResolver, "navigation_mode", -1)
        Logger.i(
            "service connected sdk=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL} " +
                "navigationMode=$navMode (0=3-button 1=2-button 2=gesture)",
        )
        syncTriggers()
        DockPrefs.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
    }

    private fun addTriggerView(index: Int) {
        val params = WindowManager.LayoutParams(
            0,
            0,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        fillLayout(params, index)
        val view = CornerTouchView(this, index).apply {
            onPress = ::onCornerPress
            onLongPress = ::openActions
        }
        getSystemService(WindowManager::class.java).addView(view, params)
        triggerViews.add(view)
        triggerParams.add(params)
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).toInt()

    /** Sets a window's size and offsets, measured from the chosen corner, from the prefs of trigger [index]. */
    private fun fillLayout(params: WindowManager.LayoutParams, index: Int) {
        val corner = DockPrefs.getCorner(this, index)
        params.gravity = (if (corner.bottom) Gravity.BOTTOM else Gravity.TOP) or
            (if (corner.end) Gravity.RIGHT else Gravity.LEFT)
        params.width = dpToPx(DockPrefs.get(this, DockPrefs.Setting.WIDTH, index))
        params.height = dpToPx(DockPrefs.get(this, DockPrefs.Setting.HEIGHT, index))
        params.x = dpToPx(DockPrefs.get(this, DockPrefs.Setting.HORIZONTAL_OFFSET, index))
        params.y = dpToPx(DockPrefs.get(this, DockPrefs.Setting.VERTICAL_OFFSET, index))
    }

    /** Brings the windows in line with the prefs: adds or removes triggers, then lays out and paints each. */
    private fun syncTriggers() {
        val wm = getSystemService(WindowManager::class.java)
        val count = DockPrefs.getTriggerCount(this)
        while (triggerViews.size > count) {
            wm.removeView(triggerViews.removeAt(triggerViews.lastIndex))
            triggerParams.removeAt(triggerParams.lastIndex)
        }
        while (triggerViews.size < count) addTriggerView(triggerViews.size)
        triggerViews.forEachIndexed { i, view ->
            fillLayout(triggerParams[i], i)
            wm.updateViewLayout(view, triggerParams[i])
            view.applyColor()
        }
        Logger.i(
            "triggers set: " + (0 until count).joinToString(" ") { i ->
                "[$i corner=${DockPrefs.getCorner(this, i).name.lowercase()} " + DockPrefs.Setting.entries.joinToString(" ") { "${it.name.lowercase()}=${DockPrefs.get(this, it, i)}dp" } + "]"
            },
        )
    }

    private var lastPressAt = 0L

    /** The trigger whose press is being handled, which the sheet opens in the corner of. */
    private var pressedTrigger = 0

    /**
     * A press opens or closes the sheet. A second press within the double-tap time instead
     * switches to the app used before the current one.
     */
    private fun onCornerPress(view: CornerTouchView) {
        pressedTrigger = view.trigger
        val now = SystemClock.uptimeMillis()
        val isDouble = now - lastPressAt <= ViewConfiguration.getDoubleTapTimeout()
        // The press itself gives the feedback, so the sheet closing from it does not.
        when {
            isDouble -> Haptics.confirm(view)
            DockActivity.isOpen -> Haptics.tick(view)
            else -> Haptics.tap(view)
        }
        // A third press starts a new tap rather than a second double tap.
        lastPressAt = if (isDouble) 0L else now
        if (isDouble) switchToPreviousApp() else openSheet()
    }

    private fun switchToPreviousApp() {
        DockActivity.close(haptic = false)
        Thread {
            val app = RecentApps.load(this, 1).firstOrNull()
            if (app == null) {
                Logger.i("double tap: no previous app")
                return@Thread
            }
            try {
                RecentApps.launch(this, app)
                Logger.i("double tap: switching to ${app.packageName}")
            } catch (e: RuntimeException) {
                Logger.w("could not switch to ${app.packageName}", e)
            }
        }.start()
    }

    /** Every press does the opposite of the one before, so none is dropped. */
    private fun openSheet() {
        if (DockActivity.isOpen) {
            DockActivity.close(haptic = false)
            Logger.i("closing sheet")
            return
        }
        DockActivity.altRequested = false
        DockActivity.requestedTrigger = pressedTrigger
        DockActivity.starting = true
        try {
            startActivity(Intent(this, DockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Logger.i("opening sheet")
        } catch (e: RuntimeException) {
            DockActivity.starting = false
            Logger.w("could not open sheet", e)
        }
    }

    /**
     * A long press on the button shows the sheet's actions. The press that started it has
     * already opened the sheet, which is switched over; if that press closed an open sheet
     * instead, the sheet is opened again, showing the actions.
     */
    private fun openActions(view: CornerTouchView) {
        Haptics.longPress(view)
        pressedTrigger = view.trigger
        if (DockActivity.isOpen) {
            DockActivity.showActions()
            Logger.i("long press: showing actions")
            return
        }
        DockActivity.altRequested = true
        DockActivity.requestedTrigger = pressedTrigger
        DockActivity.starting = true
        try {
            startActivity(Intent(this, DockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Logger.i("long press: opening actions")
        } catch (e: RuntimeException) {
            DockActivity.starting = false
            DockActivity.altRequested = false
            Logger.w("could not open actions", e)
        }
    }

    private fun removeTriggerViews() {
        triggerViews.forEach { getSystemService(WindowManager::class.java).removeView(it) }
        triggerViews.clear()
        triggerParams.clear()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        Logger.i("service unbound")
        if (instance === this) instance = null
        DockPrefs.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        removeTriggerViews()
        return super.onUnbind(intent)
    }

    companion object {
        @Volatile
        private var instance: DockAccessibilityService? = null

        /**
         * Performs a global action (Home, Back, Recents) as the user would. It waits a moment
         * first, so the sheet that asked has gone and the action acts on what was under it.
         * Returns false if the service is not running.
         */
        fun perform(action: Int): Boolean {
            val service = instance ?: return false
            service.handler.postDelayed({ service.performGlobalAction(action) }, ACTION_DELAY_MS)
            return true
        }

        private const val ACTION_DELAY_MS = 350L
    }
}
