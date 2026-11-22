package com.example.notiftiles

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.provider.Settings
import android.service.notification.StatusBarNotification
import android.widget.Toast

// Switches Location on by itself for "Where to?" and off again when the Google Maps navigation has ended.
// Android only lets an app do this after a one-time permission (granted with adb):
//   adb shell pm grant com.example.notiftiles android.permission.WRITE_SECURE_SETTINGS
// Without it nothing here does anything and the normal Google "Turn on" pop-up is used.
object LocationAuto {
    const val MAPS = "com.google.android.apps.maps"
    private const val PERM = "android.permission.WRITE_SECURE_SETTINGS"

    private val EXIT_STRICT = Regex("exit|إنهاء|انهاء|خروج", RegexOption.IGNORE_CASE)
    private val EXIT_BROAD = Regex("end|stop|quit|إيقاف", RegexOption.IGNORE_CASE)

    private fun mapsOngoing(): List<StatusBarNotification> {
        val svc = NotifListener.instance ?: return emptyList()
        return try {
            svc.activeNotifications?.filter {
                it.packageName == MAPS && (it.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun hasExitAction(n: StatusBarNotification): Boolean =
        n.notification.actions?.any { EXIT_STRICT.containsMatchIn(it.title ?: "") } == true

    /** The ongoing Google Maps navigation notification, or null when you are not navigating. */
    fun navNotification(): StatusBarNotification? {
        val all = mapsOngoing()
        // Maps can keep other ongoing notifications too: prefer the one that really is the navigation.
        return all.firstOrNull { hasExitAction(it) }
            ?: all.firstOrNull { it.notification.category == Notification.CATEGORY_NAVIGATION }
            ?: all.firstOrNull()
    }

    /** Presses "Exit navigation" in the Google Maps notification. True if it was sent. */
    fun exitNavigation(): Boolean {
        val cands = mapsOngoing()
        for (n in cands) {
            val acts = n.notification.actions ?: continue
            var pick: Notification.Action? = acts.firstOrNull { EXIT_STRICT.containsMatchIn(it.title ?: "") }
            if (pick == null) pick = acts.firstOrNull { EXIT_BROAD.containsMatchIn(it.title ?: "") }
            if (pick == null && acts.size == 1) pick = acts[0]
            val pi = pick?.actionIntent ?: continue
            if (sendAction(pi)) return true
        }
        return false
    }

    // Android 14+ wants the sender of someone else's PendingIntent to say it may start a screen.
    private fun sendAction(pi: android.app.PendingIntent): Boolean {
        val ctx = NotifListener.instance
        return try {
            if (ctx != null && android.os.Build.VERSION.SDK_INT >= 34) {
                val o = android.app.ActivityOptions.makeBasic()
                o.setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                pi.send(ctx, 0, null, null, null, null, o.toBundle())
            } else {
                pi.send()
            }
            true
        } catch (e: Exception) {
            try {
                pi.send()
                true
            } catch (x: Exception) {
                false
            }
        }
    }

    /**
     * Exit navigation with proof: press the button, then CHECK the Maps notification is really gone.
     * If it is not, escalate (tap Exit inside Maps), and always report what really happened.
     */
    fun exitSolid(c: Context, done: (String) -> Unit) {
        val app = c.applicationContext
        val h = android.os.Handler(android.os.Looper.getMainLooper())
        if (NotifListener.instance == null) {
            done("Exit failed: Notification access is not connected, so Notif Tiles cannot see Maps")
            return
        }
        if (navNotification() == null) {
            done("Not navigating: no Maps navigation notification found")
            return
        }
        val sent = exitNavigation()
        var checks = 0
        val check = object : Runnable {
            override fun run() {
                checks++
                if (navNotification() == null) {
                    done("Navigation ended")
                    return
                }
                if (checks < 6) {
                    h.postDelayed(this, 500)
                    return
                }
                done(
                    if (exitViaMapsScreen(app)) "Exit button did not end it: tapping Exit inside Maps"
                    else if (sent) "Exit was sent but Maps kept navigating. Turn the Accessibility helper ON for the backup"
                    else "Exit failed: no Exit button found in the Maps notification"
                )
            }
        }
        h.postDelayed(check, 500)
    }

    /** Plain report of every piece "Where to?", Exit and Location depend on. */
    fun report(c: Context): String {
        val sb = StringBuilder()
        sb.append(if (NotifListener.instance != null) "✔ Notification access: connected\n" else "✘ Notification access: NOT connected (turn it on for Notif Tiles)\n")
        val n = navNotification()
        if (n != null) {
            val titles = n.notification.actions?.joinToString(" | ") { (it.title ?: "?").toString() }
            sb.append("✔ Maps navigation notification: found\n   Its buttons: ${titles ?: "none"}\n")
        } else {
            sb.append("• Maps navigation notification: none right now (start navigation to test Exit)\n")
        }
        sb.append(
            if (canWrite(c)) "✔ Location permission (adb): granted\n"
            else "✘ Location permission (adb): NOT granted, so Google's Turn on pop-up appears. Run: adb shell pm grant com.example.notiftiles android.permission.WRITE_SECURE_SETTINGS\n"
        )
        sb.append(if (isOn(c)) "• Location is ON now\n" else "• Location is OFF now\n")
        sb.append(if (RecorderTapService.enabled(c)) "✔ Accessibility helper: ON\n" else "✘ Accessibility helper: OFF (needed for closing the pop-up and for the backup Exit)\n")
        sb.append("• Notif Tiles will switch Location off after navigation: ${if (Prefs.locOwned(c)) "yes" else "no (it did not switch it on)"}")
        return sb.toString()
    }

    /**
     * Fallback when the Maps notification has no Exit button to press (newer Maps versions draw it themselves):
     * open Maps and let the Accessibility helper tap its own Exit button. True if that was started.
     */
    fun exitViaMapsScreen(c: Context): Boolean {
        if (!RecorderTapService.enabled(c)) return false
        val i = c.packageManager.getLaunchIntentForPackage(MAPS) ?: return false
        RecorderTapService.mapsExitUntil = System.currentTimeMillis() + 8000L
        return try {
            c.startActivity(i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            RecorderTapService.mapsExitUntil = 0L
            false
        }
    }

    fun canWrite(c: Context): Boolean = c.checkSelfPermission(PERM) == PackageManager.PERMISSION_GRANTED

    fun isOn(c: Context): Boolean =
        (c.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isLocationEnabled

    @Suppress("DEPRECATION")
    private fun setMode(c: Context, on: Boolean): Boolean {
        var ok = false
        try {
            ok = Settings.Secure.putInt(
                c.contentResolver,
                Settings.Secure.LOCATION_MODE,
                if (on) Settings.Secure.LOCATION_MODE_HIGH_ACCURACY else Settings.Secure.LOCATION_MODE_OFF
            )
        } catch (e: Exception) {
        }
        // Newer Android: ask the system's own switch too (may be refused on some phones; harmless then).
        try {
            val lm = c.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val m = LocationManager::class.java.getMethod(
                "setLocationEnabledForUser", Boolean::class.javaPrimitiveType, android.os.UserHandle::class.java
            )
            m.invoke(lm, on, android.os.Process.myUserHandle())
            ok = true
        } catch (e: Throwable) {
        }
        return ok
    }

    /** Navigation is about to start from "Where to?": remember to switch Location off afterwards, even if it was already on. */
    fun armForNavigation(c: Context) {
        if (!canWrite(c) || !Prefs.autoLocationOff(c) || !Prefs.locOffAlways(c) || !isOn(c)) return
        Prefs.setLocOwned(c, true)
        NotifListener.instance?.watchLocation()
    }

    /** Settings test: switches Location off right now and reports what really happened. */
    fun testOff(c: Context, done: (String) -> Unit) {
        if (!canWrite(c)) {
            done("Permission not granted yet: run the adb command shown above")
            return
        }
        if (!isOn(c)) {
            done("Location is already off. Turn it on, then try again")
            return
        }
        val wrote = setMode(c, false)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            done(
                if (!isOn(c)) "Location switched off. This phone allows it"
                else "Android refused (write " + (if (wrote) "accepted" else "failed") + ", Location still on)"
            )
        }, 700)
    }

    /** True when Location is on afterwards without any pop-up. False: no permission (or it failed), use the pop-up. */
    fun turnOn(c: Context): Boolean {
        if (!canWrite(c)) return false
        if (isOn(c)) return true
        if (!setMode(c, true)) return false
        // The write can take a moment to show; if Android really refused, say so and let the normal pop-up run.
        var tries = 0
        while (!isOn(c) && tries < 6) {
            try {
                Thread.sleep(60)
            } catch (e: InterruptedException) {
            }
            tries++
        }
        if (!isOn(c)) return false
        Prefs.setLocOwned(c, true)
        NotifListener.instance?.watchLocation()
        return true
    }

    /** Navigation is over: switch Location off again, but only if Notif Tiles was the one that switched it on. */
    fun release(c: Context) {
        if (Prefs.locOwned(c) && Prefs.autoLocationOff(c) && canWrite(c)) {
            if (setMode(c, false)) {
                Toast.makeText(c, "Navigation ended: Location switched off", Toast.LENGTH_SHORT).show()
            }
        }
        Prefs.setLocOwned(c, false)
    }
}
