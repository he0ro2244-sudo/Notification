package com.example.notiftiles

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

// Presses the red record button of the phone's Recorder right after the Notif Tiles mic opens it.
// Only listens to the Recorder app (see res/xml/recorder_tap_service.xml).
class RecorderTapService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (System.currentTimeMillis() > pendingUntil) return
        pendingUntil = 0L
        handler.postDelayed({ tap() }, 900)
    }

    override fun onInterrupt() {}

    private fun tap() {
        val b = getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
        val x = b.width() * Recorder.tapX(this) / 100f
        val y = b.height() * Recorder.tapY(this) / 100f
        val p = Path()
        p.moveTo(x, y)
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 80))
            .build()
        dispatchGesture(g, null, null)
    }

    companion object {
        /** Time up to which the next Recorder window should get its red button pressed. */
        @Volatile
        var pendingUntil = 0L

        fun enabled(c: Context): Boolean {
            val s = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return s.contains(c.packageName) && s.contains("RecorderTapService")
        }
    }
}
