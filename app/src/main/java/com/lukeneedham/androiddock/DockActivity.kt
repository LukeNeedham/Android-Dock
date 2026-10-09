package com.lukeneedham.androiddock

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Hosts the bottom sheet of open apps. Has no UI of its own: the activity is transparent and
 * exists only to show the sheet. When the sheet goes away, the whole task is removed, so the
 * app never lingers in the system task switcher.
 */
class DockActivity : AppCompatActivity() {

    private var sheet: BottomSheetDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showSheet()
    }

    private fun showSheet() {
        // TODO: replace the placeholder with the list of open apps.
        val content = TextView(this).apply {
            setText(R.string.sheet_title)
            setPadding(PADDING_PX, PADDING_PX, PADDING_PX, PADDING_PX)
        }
        sheet = BottomSheetDialog(this).apply {
            setContentView(content)
            setOnDismissListener { finishAndRemoveTask() }
            show()
        }
    }

    override fun onDestroy() {
        sheet?.setOnDismissListener(null)
        sheet?.dismiss()
        super.onDestroy()
    }

    private companion object {
        const val PADDING_PX = 64
    }
}
