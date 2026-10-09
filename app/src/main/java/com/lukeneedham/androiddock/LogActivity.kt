package com.lukeneedham.androiddock

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** Shows what [DockLog] has recorded, newest at the bottom, so gestures can be debugged on-device. */
class LogActivity : AppCompatActivity() {

    private lateinit var logText: TextView
    private lateinit var scroll: ScrollView
    private val handler = Handler(Looper.getMainLooper())
    private var refreshScheduled = false

    // A drag writes many lines a second, so refreshes are batched rather than one per line.
    private val onLogChanged: () -> Unit = {
        if (!refreshScheduled) {
            refreshScheduled = true
            handler.postDelayed({
                refreshScheduled = false
                refresh()
            }, REFRESH_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        logText = findViewById(R.id.log_text)
        scroll = findViewById(R.id.scroll)
        findViewById<View>(R.id.refresh).setOnClickListener { refresh(forceScroll = true) }
        findViewById<View>(R.id.copy).setOnClickListener { copy() }
        findViewById<View>(R.id.clear).setOnClickListener {
            DockLog.clear(this)
            refresh(forceScroll = true)
        }
    }

    override fun onStart() {
        super.onStart()
        DockLog.addListener(onLogChanged)
        refresh(forceScroll = true)
    }

    override fun onStop() {
        DockLog.removeListener(onLogChanged)
        handler.removeCallbacksAndMessages(null)
        refreshScheduled = false
        super.onStop()
    }

    /** Reloads the log. Keeps following the newest line only if the user was already at the bottom. */
    private fun refresh(forceScroll: Boolean = false) {
        val content = scroll.getChildAt(0)
        val atBottom = content == null ||
            content.bottom - (scroll.height + scroll.scrollY) <= AT_BOTTOM_SLOP_PX
        val text = DockLog.read(this)
        logText.text = text.ifEmpty { getString(R.string.log_empty) }
        if (forceScroll || atBottom) scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun copy() {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Android Dock log", DockLog.read(this)))
        Toast.makeText(this, R.string.log_copied, Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val REFRESH_INTERVAL_MS = 200L
        const val AT_BOTTOM_SLOP_PX = 48
    }
}
