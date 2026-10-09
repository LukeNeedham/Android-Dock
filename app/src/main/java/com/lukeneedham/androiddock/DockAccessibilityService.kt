package com.lukeneedham.androiddock

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Will watch the bottom-right corner of the navigation bar and open [DockActivity] when it is
 * tapped. Not implemented yet.
 */
class DockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
