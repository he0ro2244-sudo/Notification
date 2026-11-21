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
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

// Presses the red record button of the phone's Recorder right after the Notif Tiles mic opens it.
// Only listens to the Recorder app (see res/xml/recorder_tap_service.xml).
class RecorderTapService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (System.currentTimeMillis() > pendingUntil) return
        pendingUntil = 0L
        backs = 0
        handler.postDelayed({ tap() }, 900)
    }

    override fun onInterrupt() {}

    private var backs = 0

    private fun tap() {
        // If the Recorder resumed on a saved recording's page (play button where the record button
        // should be), go back to its main screen first instead of pressing play.
        val r0 = rootInActiveWindow
        if (r0 != null && backs < 2 &&
            (r0.findAccessibilityNodeInfosByText("Transcribe").isNotEmpty() ||
                r0.findAccessibilityNodeInfosByText("AI Summary").isNotEmpty())
        ) {
            backs++
            performGlobalAction(GLOBAL_ACTION_BACK)
            handler.postDelayed({ tap() }, 900)
            return
        }
        // 1) Try to find the record button by its label / id and click it.
        val root = rootInActiveWindow
        if (root != null) {
            var n: AccessibilityNodeInfo? = findRecordButton(root)
            while (n != null && !n.isClickable) n = n.parent
            if (n != null && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                Toast.makeText(this, "Notif Tiles: pressed record button", Toast.LENGTH_SHORT).show()
                return
            }
        }
        // 2) Fallback: tap at the saved position.
        val b = getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
        val x = b.width() * Recorder.tapX(this) / 100f
        val y = b.height() * Recorder.tapY(this) / 100f
        val p = Path()
        p.moveTo(x, y)
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 80))
            .build()
        dispatchGesture(g, null, null)
        Toast.makeText(this, "Notif Tiles: tapped at ${Recorder.tapX(this)}%, ${Recorder.tapY(this)}%", Toast.LENGTH_SHORT).show()
    }

    private val good = Regex("record|start|rec_|btn_rec|录音|开始")
    private val bad = Regex("stop|pause|list|setting|file|play|search|more|menu|back|mark|tab")

    private fun findRecordButton(n: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val id = (n.viewIdResourceName ?: "").substringAfter(":id/", "")
        val label = (id + " " + (n.contentDescription?.toString() ?: "")).lowercase()
        if (label.isNotBlank() && n.isVisibleToUser && good.containsMatchIn(label) && !bad.containsMatchIn(label)) return n
        for (i in 0 until n.childCount) {
            val c = n.getChild(i) ?: continue
            val r = findRecordButton(c)
            if (r != null) return r
        }
        return null
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
