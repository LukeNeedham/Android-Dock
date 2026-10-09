package com.lukeneedham.androiddock

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * What the user sees when they open the app: a checklist that walks them through the setup the
 * dock needs. Each step shows whether it is done, and the screen re-checks every time the user
 * comes back from system settings.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var steps: LinearLayout
    private lateinit var allSet: View
    private val stepViews = mutableListOf<StepView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        steps = findViewById(R.id.steps)
        allSet = findViewById(R.id.all_set)
        setUpPositionSlider()
        findViewById<View>(R.id.view_log).setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }
        findViewById<View>(R.id.try_it).setOnClickListener {
            startActivity(Intent(this, DockActivity::class.java))
        }

        stepViews += addStep(
            title = R.string.step_accessibility_title,
            description = R.string.step_accessibility_description,
            hint = R.string.step_accessibility_hint,
            action = R.string.step_accessibility_action,
            isDone = { SetupState.isAccessibilityEnabled(this) },
            onAction = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            secondaryAction = R.string.step_app_info_action,
            onSecondaryAction = {
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null),
                    ),
                )
            },
        )
        stepViews += addStep(
            title = R.string.step_usage_title,
            description = R.string.step_usage_description,
            action = R.string.step_usage_action,
            isDone = { SetupState.isUsageAccessGranted(this) },
            onAction = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
        )
    }

    private fun setUpPositionSlider() {
        val seek = findViewById<SeekBar>(R.id.position_seek)
        val value = findViewById<TextView>(R.id.position_value)
        seek.max = DockPrefs.MAX_BOTTOM_OFFSET_DP
        seek.progress = DockPrefs.bottomOffsetDp(this)
        value.text = getString(R.string.position_value, seek.progress)
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                value.text = getString(R.string.position_value, progress)
                if (fromUser) DockPrefs.setBottomOffsetDp(this@SettingsActivity, progress)
            }

            override fun onStartTrackingTouch(bar: SeekBar) = Unit

            override fun onStopTrackingTouch(bar: SeekBar) = Unit
        })
    }

    override fun onResume() {
        super.onResume()
        stepViews.forEach { it.refresh() }
        allSet.visibility = if (SetupState.isComplete(this)) View.VISIBLE else View.GONE
    }

    private fun addStep(
        title: Int,
        description: Int,
        action: Int,
        isDone: () -> Boolean,
        onAction: () -> Unit,
        hint: Int? = null,
        secondaryAction: Int? = null,
        onSecondaryAction: (() -> Unit)? = null,
    ): StepView {
        val root = LayoutInflater.from(this).inflate(R.layout.item_step, steps, false)
        root.findViewById<TextView>(R.id.step_title).setText(title)
        root.findViewById<TextView>(R.id.step_description).setText(description)
        root.findViewById<TextView>(R.id.step_hint).apply {
            if (hint != null) {
                setText(hint)
                visibility = View.VISIBLE
            }
        }
        val button = root.findViewById<MaterialButton>(R.id.step_action).apply {
            setText(action)
            setOnClickListener { onAction() }
        }
        val buttons = mutableListOf(button)
        if (secondaryAction != null && onSecondaryAction != null) {
            buttons += root.findViewById<MaterialButton>(R.id.step_secondary_action).apply {
                setText(secondaryAction)
                setOnClickListener { onSecondaryAction() }
            }
        }
        steps.addView(root)
        return StepView(buttons, root.findViewById(R.id.step_title), title, isDone)
    }

    /** Shows a step as done with a tick on its title, and hides its buttons. */
    private inner class StepView(
        private val buttons: List<MaterialButton>,
        private val titleView: TextView,
        private val title: Int,
        private val isDone: () -> Boolean,
    ) {
        fun refresh() {
            val done = isDone()
            val text = getString(title)
            titleView.text = if (done) "$text  ✓" else text
            buttons.forEach { it.visibility = if (done) View.GONE else View.VISIBLE }
        }
    }
}
