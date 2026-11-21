package com.example.notiftiles

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
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
        val now = System.currentTimeMillis()
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            if (now < learnUntil) learn(event)
            return
        }
        if (now > pendingUntil) return
        if (now - lastTap < 15000) return // never press twice (a second press would stop the recording)
        pendingUntil = 0L
        backs = 0
        Toast.makeText(this, "Notif Tiles: Recorder opened (" + (event.className ?: "?").toString().substringAfterLast('.') + ")", Toast.LENGTH_SHORT).show()
        handler.postDelayed({ tap() }, 1200)
    }

    override fun onInterrupt() {}

    private var backs = 0
    private var lastTap = 0L

    // Remember the button the person really pressed, so it can be pressed the same way later.
    private fun learn(event: AccessibilityEvent) {
        val n = event.source
        if (n == null) {
            Toast.makeText(this, "Notif Tiles: couldn't read that button", Toast.LENGTH_LONG).show()
            return
        }
        learnUntil = 0L
        val r = Rect()
        n.getBoundsInScreen(r)
        val b = getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
        val xp = (r.centerX() * 100f / b.width()).toInt().coerceIn(1, 99)
        val yp = (r.centerY() * 100f / b.height()).toInt().coerceIn(1, 99)
        Recorder.setLearned(this, n.viewIdResourceName ?: "", n.contentDescription?.toString() ?: "", xp, yp)
        Toast.makeText(this, "Notif Tiles learned: " + Recorder.learnedSummary(this), Toast.LENGTH_LONG).show()
    }

    private fun findLearned(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val id = Recorder.learnedId(this)
        if (id.isNotBlank()) {
            val l = root.findAccessibilityNodeInfosByViewId(id)
            val v = l.firstOrNull { it.isVisibleToUser }
            if (v != null) return v
        }
        val d = Recorder.learnedDesc(this)
        if (d.isNotBlank()) return findByDesc(root, d)
        return null
    }

    private fun findByDesc(n: AccessibilityNodeInfo, d: String): AccessibilityNodeInfo? {
        if (n.isVisibleToUser && n.contentDescription?.toString() == d) return n
        for (i in 0 until n.childCount) {
            val c = n.getChild(i) ?: continue
            val r = findByDesc(c, d)
            if (r != null) return r
        }
        return null
    }

    private fun alreadyRecording(root: AccessibilityNodeInfo): Boolean {
        val l = (root.contentDescription?.toString() ?: "") + " " + (root.text?.toString() ?: "")
        if (Regex("stop|pause|resume", RegexOption.IGNORE_CASE).containsMatchIn(l) && root.isVisibleToUser) return true
        for (i in 0 until root.childCount) {
            val c = root.getChild(i) ?: continue
            if (alreadyRecording(c)) return true
        }
        return false
    }

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
        val r1 = rootInActiveWindow
        if (r1 != null && alreadyRecording(r1)) {
            Toast.makeText(this, "Notif Tiles: already recording, not pressing", Toast.LENGTH_SHORT).show()
            return
        }
        lastTap = System.currentTimeMillis()
        // 1) Try to find the record button by its label / id and click it.
        val root = rootInActiveWindow
        if (root != null) {
            var n: AccessibilityNodeInfo? = findLearned(root) ?: (if (Recorder.hasLearned(this)) null else findRecordButton(root))
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

        /** While set, the next click inside the Recorder is remembered as "the red button". */
        @Volatile
        var learnUntil = 0L

        fun enabled(c: Context): Boolean {
            val s = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return s.contains(c.packageName) && s.contains("RecorderTapService")
        }
    }
}
