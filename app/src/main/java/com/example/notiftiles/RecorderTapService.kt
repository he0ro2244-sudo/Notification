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

// Does what you do by hand: opens the Recorder (its list screen), then taps the red record button.
// The tap is repeated until the Recorder really shows that it is recording, so a slow start
// doesn't make it miss. Only listens to recorder apps (see res/xml/recorder_tap_service.xml).
class RecorderTapService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var attempts = 0
    private var backs = 0
    private var running = false
    private var tapped = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkgName = event.packageName?.toString() ?: ""
        if (pkgName in Weather.APPS) {
            maybeReadWeather()
            return
        }
        val now = System.currentTimeMillis()
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            if (now < learnUntil) learn(event)
            return
        }
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (now > pendingUntil || running) return
        pendingUntil = 0L
        running = true
        attempts = 0
        backs = 0
        tapped = false
        handler.postDelayed({ attempt() }, 500)
    }

    override fun onInterrupt() {}

    // ---- Copy the temperature shown in the phone's weather app to the widget ----
    private var lastRead = 0L
    private var readQueued = false
    private val bigTemp = Regex("^\\s*(-?\\d{1,3})\\s*°\\s*[CFcf]?\\s*$")
    private val condLine = Regex("^\\s*([A-Za-z][A-Za-z ]{2,28}?)\\s+-?\\d{1,3}°\\s*/\\s*-?\\d{1,3}°")

    private fun maybeReadWeather() {
        if (!Weather.useApp(this) || readQueued) return
        if (System.currentTimeMillis() - lastRead < 2500) return
        readQueued = true
        if (autoCloseUntil > 0L && !timeoutQueued) {
            timeoutQueued = true
            val wait = (autoCloseUntil - System.currentTimeMillis()).coerceAtLeast(500L) + 200L
            handler.postDelayed({
                timeoutQueued = false
                if (autoCloseUntil > 0L) {
                    autoCloseUntil = 0L
                    toast("could not read the weather app's temperature")
                    if ((rootInActiveWindow?.packageName?.toString() ?: "") in Weather.APPS) performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }, wait)
        }
        handler.postDelayed({
            readQueued = false
            readWeather()
        }, 700)
    }

    private var timeoutQueued = false

    // After a refresh-button read: leave the weather app again (a second Back if it stayed open).
    private fun closeWeatherApp() {
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_BACK)
            handler.postDelayed({
                if ((rootInActiveWindow?.packageName?.toString() ?: "") in Weather.APPS) performGlobalAction(GLOBAL_ACTION_BACK)
            }, 700)
        }, 500)
    }

    private fun readWeather() {
        val root = rootInActiveWindow ?: return
        if ((root.packageName?.toString() ?: "") !in Weather.APPS) return
        var bestH = 0
        var bestTemp: Int? = null
        var cond: String? = null
        val r = Rect()
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || depth > 40) return
            val t = n.text?.toString()
            if (t != null) {
                val m = bigTemp.matchEntire(t)
                if (m != null && n.isVisibleToUser) {
                    n.getBoundsInScreen(r)
                    if (r.height() > bestH) {
                        bestH = r.height()
                        bestTemp = m.groupValues[1].toIntOrNull()
                    }
                } else if (cond == null) {
                    condLine.find(t)?.let { cond = it.groupValues[1].trim() }
                }
            }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        walk(root, 0)
        val temp = bestTemp ?: return
        val before = Weather.appTemp(this)
        val beforeTime = Weather.appTime(this)
        Weather.saveFromApp(this, temp, cond)
        lastRead = System.currentTimeMillis()
        if (Weather.mode(this) == "both") {
            val app = applicationContext
            Thread {
                Weather.learnGap(app)
                try {
                    TilesWidget.refresh(app)
                } catch (e: Exception) {
                }
            }.start()
        }
        if (autoCloseUntil > System.currentTimeMillis()) {
            autoCloseUntil = 0L
            toast("widget weather set to $temp°")
            closeWeatherApp()
        } else if (temp != before || lastRead - beforeTime > 5 * 60 * 1000L) {
            toast("widget weather set to $temp°")
        }
        try {
            TilesWidget.refresh(applicationContext)
        } catch (e: Exception) {
        }
    }

    private fun toast(s: String) = Toast.makeText(this, "Notif Tiles: $s", Toast.LENGTH_SHORT).show()

    private fun attempt() {
        val root = rootInActiveWindow
        val inRecorder = root != null && Recorder.KNOWN.any { it.first == root.packageName?.toString() }
        if (root != null && inRecorder && isRecording(root)) {
            toast("recording started")
            running = false
            return
        }
        // Without a learned recording-screen button we can't tell it started, so press only once.
        if (tapped && Recorder.learnedId(this).isBlank()) {
            running = false
            return
        }
        if (attempts >= 6) {
            toast("could not start recording")
            running = false
            return
        }
        attempts++
        if (root != null && inRecorder) {
            if (onSavedRecordingPage(root) && backs < 2) {
                backs++
                performGlobalAction(GLOBAL_ACTION_BACK)
            } else {
                tap()
                tapped = true
            }
        }
        handler.postDelayed({ attempt() }, 1300)
    }

    // A saved recording's page has these buttons; the red play button there is not "record".
    private fun onSavedRecordingPage(root: AccessibilityNodeInfo): Boolean =
        root.findAccessibilityNodeInfosByText("Transcribe").isNotEmpty() ||
            root.findAccessibilityNodeInfosByText("AI Summary").isNotEmpty()

    // The recording screen has the save (stop) button, learned from a real recording.
    private fun isRecording(root: AccessibilityNodeInfo): Boolean {
        val id = Recorder.learnedId(this)
        if (id.isNotBlank() && root.findAccessibilityNodeInfosByViewId(id).any { it.isVisibleToUser }) return true
        return root.findAccessibilityNodeInfosByViewId(root.packageName.toString() + ":id/recorder_save")
            .any { it.isVisibleToUser }
    }

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

    // "Learn the red button": remember the button you really pressed (used to tell when recording has started).
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
        // Keep the saved tap position: the learned button here is the one on the recording screen.
        Recorder.setLearned(this, n.viewIdResourceName ?: "", n.contentDescription?.toString() ?: "", Recorder.tapX(this), Recorder.tapY(this))
        Toast.makeText(this, "Notif Tiles learned: " + (n.viewIdResourceName ?: "button"), Toast.LENGTH_LONG).show()
    }

    companion object {
        /** Time up to which the next Recorder window should get its red button pressed. */
        @Volatile
        var pendingUntil = 0L
        /** Set by the widget's refresh button: read the weather app, then close it again. */
        @Volatile var autoCloseUntil = 0L

        /** While set, the next click inside the Recorder is remembered. */
        @Volatile
        var learnUntil = 0L

        fun enabled(c: Context): Boolean {
            val s = Settings.Secure.getString(c.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            return s.contains(c.packageName) && s.contains("RecorderTapService")
        }
    }
}
