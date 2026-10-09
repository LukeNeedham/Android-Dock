package com.lukeneedham.androiddock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/**
 * Runs after the app is updated. The system switches the accessibility service off when it does,
 * so this turns it back on, if the adb grant that allows it has been done. The system can also
 * undo the change while it finishes the update, so it checks again a few times afterwards.
 */
class UpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                restore(context, "app updated")
                RECHECK_DELAYS_MS.forEachIndexed { index, delay -> scheduleRecheck(context, index, delay) }
            }
            ACTION_RECHECK -> restore(context, "recheck ${intent.getIntExtra(EXTRA_INDEX, 0) + 1}")
        }
    }

    private fun restore(context: Context, event: String) {
        val wasEnabled = SetupState.isAccessibilityEnabled(context)
        val enabled = SetupState.enableAccessibility(context)
        DockLog.log(
            context,
            "$event: accessibility " + when {
                wasEnabled -> "on"
                enabled -> "was off, switched back on"
                SetupState.canWriteSecureSettings(context) -> "was off, could not switch on"
                else -> "was off, adb grant missing so not switched on"
            },
        )
    }

    private fun scheduleRecheck(context: Context, index: Int, delayMs: Long) {
        val intent = Intent(context, UpdateReceiver::class.java)
            .setAction(ACTION_RECHECK)
            .putExtra(EXTRA_INDEX, index)
        val pending = PendingIntent.getBroadcast(
            context,
            index,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayMs, pending)
    }

    private companion object {
        const val ACTION_RECHECK = "com.lukeneedham.androiddock.RECHECK_ACCESSIBILITY"
        const val EXTRA_INDEX = "index"
        val RECHECK_DELAYS_MS = listOf(10_000L, 30_000L, 60_000L)
    }
}
