package com.lukeneedham.androiddock

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/** Shows what [DockLog] has recorded, newest at the bottom, so gestures can be debugged on-device. */
class LogActivity : AppCompatActivity() {

    private lateinit var logText: TextView
    private lateinit var scroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        logText = findViewById(R.id.log_text)
        scroll = findViewById(R.id.scroll)
        findViewById<View>(R.id.refresh).setOnClickListener { refresh() }
        findViewById<View>(R.id.copy).setOnClickListener { copy() }
        findViewById<View>(R.id.clear).setOnClickListener {
            DockLog.clear(this)
            refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val text = DockLog.read(this)
        logText.text = text.ifEmpty { getString(R.string.log_empty) }
        scroll.post { scroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun copy() {
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Android Dock log", DockLog.read(this)))
        Toast.makeText(this, R.string.log_copied, Toast.LENGTH_SHORT).show()
    }
}
