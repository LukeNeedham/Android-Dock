package com.lukeneedham.androiddock

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import java.util.concurrent.ConcurrentHashMap

/**
 * The flashlight. It needs no permission, but the system does not say whether it is on unless
 * asked to report changes, so [track] listens for them and [toggle] flips what was last heard.
 */
object Torch {

    private val states = ConcurrentHashMap<String, Boolean>()
    private var tracking = false

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            states[cameraId] = enabled
        }
    }

    /** Starts following the torch, so the first [toggle] knows whether it is on. */
    @Synchronized
    fun track(context: Context) {
        if (tracking) return
        tracking = true
        manager(context).registerTorchCallback(callback, null)
    }

    /** Turns the torch off if it is on, and on if it is off. Does nothing without a flash. */
    fun toggle(context: Context) {
        track(context)
        val manager = manager(context)
        try {
            val camera = manager.cameraIdList.firstOrNull {
                manager.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            manager.setTorchMode(camera, !(states[camera] ?: false))
        } catch (e: Exception) {
            DockLog.log(context, "could not toggle the torch: $e")
        }
    }

    private fun manager(context: Context) =
        context.applicationContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
}
