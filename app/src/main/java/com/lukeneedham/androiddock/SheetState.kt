package com.lukeneedham.androiddock

/**
 * Whether the sheet is meant to be open. Every tap on the corner flips it, so a tap always does
 * the opposite of the one before and is never dropped, whatever state the sheet's activity or its
 * animations are in. The activity follows this value, and reports back when it closes itself.
 */
object SheetState {
    private var wanted = false

    /** Flips the state, and returns whether the sheet is now meant to be open. */
    @Synchronized
    fun toggle(): Boolean {
        wanted = !wanted
        return wanted
    }

    @Synchronized
    fun want(open: Boolean) {
        wanted = open
    }

    @Synchronized
    fun isWanted() = wanted
}
