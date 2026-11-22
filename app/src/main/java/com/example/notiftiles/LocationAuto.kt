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

    /** The ongoing Google Maps navigation notification, or null when you are not navigating. */
    fun navNotification(): StatusBarNotification? {
        val svc = NotifListener.instance ?: return null
        return try {
            svc.activeNotifications?.firstOrNull {
                it.packageName == MAPS && (it.notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Presses "Exit navigation" in the Google Maps notification. True if it was sent. */
    fun exitNavigation(): Boolean {
        val acts = navNotification()?.notification?.actions ?: return false
        val re = Regex("exit|end|stop|quit|إنهاء|انهاء|خروج|إيقاف", RegexOption.IGNORE_CASE)
        var pick: Notification.Action? = acts.firstOrNull { re.containsMatchIn(it.title ?: "") }
        if (pick == null && acts.size == 1) pick = acts[0]
        val pi = pick?.actionIntent ?: return false
        return try {
            pi.send()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun canWrite(c: Context): Boolean = c.checkSelfPermission(PERM) == PackageManager.PERMISSION_GRANTED

    fun isOn(c: Context): Boolean =
        (c.getSystemService(Context.LOCATION_SERVICE) as LocationManager).isLocationEnabled

    @Suppress("DEPRECATION")
    private fun setMode(c: Context, on: Boolean): Boolean {
        return try {
            Settings.Secure.putInt(
                c.contentResolver,
                Settings.Secure.LOCATION_MODE,
                if (on) Settings.Secure.LOCATION_MODE_HIGH_ACCURACY else Settings.Secure.LOCATION_MODE_OFF
            )
        } catch (e: Exception) {
            false
        }
    }

    /** True when Location is on afterwards without any pop-up. False: no permission (or it failed), use the pop-up. */
    fun turnOn(c: Context): Boolean {
        if (!canWrite(c)) return false
        if (isOn(c)) return true
        if (!setMode(c, true)) return false
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
