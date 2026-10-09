package com.lukeneedham.androiddock

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

/**
 * A log that survives the process being killed, so a gesture can be debugged on a device that
 * is not attached to a computer. Lines are appended to a file in the app's private storage and
 * read back by [LogActivity]. Also forwarded to logcat.
 *
 * Writes happen on a single background thread, so calling [log] from a touch handler is cheap.
 */
object DockLog {

    private const val TAG = "DockLog"
    private const val FILE_NAME = "dock.log"
    private const val MAX_BYTES = 256_000L
    private const val KEEP_CHARS = 128_000

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<() -> Unit>()
    private val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    fun log(context: Context, message: String) {
        Log.d(TAG, message)
        val file = file(context)
        val line = "${timeFormat.format(Date())}  $message\n"
        executor.execute {
            file.appendText(line)
            if (file.length() > MAX_BYTES) trim(file)
            mainHandler.post { listeners.forEach { it() } }
        }
    }

    /** Calls [listener] on the main thread after each line is written. Remove it when done. */
    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    /** The whole log, oldest line first. Waits for pending writes, so call it sparingly. */
    fun read(context: Context): String {
        val file = file(context)
        return executor.submit<String> { if (file.exists()) file.readText() else "" }.get()
    }

    fun clear(context: Context) {
        val file = file(context)
        executor.submit { file.delete() }.get()
    }

    private fun file(context: Context) = File(context.applicationContext.filesDir, FILE_NAME)

    /** Keeps the newest part of the log, starting at a line boundary. */
    private fun trim(file: File) {
        val text = file.readText()
        val tail = text.takeLast(KEEP_CHARS)
        file.writeText(tail.substringAfter('\n', tail))
    }
}
