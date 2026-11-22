package com.example.notiftiles

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/** One result line of "Check everything". level: 0 = fine, 1 = optional / note, 2 = needs attention. */
class CheckLine(val level: Int, val text: String, val short: String = "", val fix: Intent? = null)

// One place that checks everything Notif Tiles depends on.
object SelfCheck {
    fun run(c: Context): List<CheckLine> {
        val out = ArrayList<CheckLine>()
        fun ok(t: String) = out.add(CheckLine(0, t))
        fun note(t: String, short: String = "", fix: Intent? = null) = out.add(CheckLine(1, t, short, fix))
        fun bad(t: String, short: String, fix: Intent?) = out.add(CheckLine(2, t, short, fix))
        fun granted(p: String) = c.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED
        val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + c.packageName))

        // ---- Needed for the tiles
        val access = Access.enabled(c)
        val listenerSettings = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        when {
            access && NotifListener.instance != null -> ok("Notification access: connected")
            access -> bad("Notification access is on but not connected. Switch it off and on again.", "notification access", listenerSettings)
            else -> bad("Notification access is OFF. Tiles cannot show messages without it.", "notification access", listenerSettings)
        }
        val chosen = Prefs.enabled(c).size
        if (chosen > 0) ok("Apps chosen: $chosen") else bad("No apps chosen yet. Choose them on the main screen.", "choose apps", Intent(c, MainActivity::class.java))
        try {
            val ids = AppWidgetManager.getInstance(c).getAppWidgetIds(ComponentName(c, TilesWidget::class.java))
            if (ids.isNotEmpty()) ok("Widget: on the home screen") else note("Widget: not on the home screen yet. Add it from the widget picker.")
        } catch (e: Exception) {
        }
        try {
            note("Messages showing now: " + Feed.items(c).size)
        } catch (e: Exception) {
        }

        // ---- Optional helpers
        if (RecorderTapService.enabled(c)) ok("Accessibility helper: ON")
        else note("Accessibility helper: OFF. Needed for the mic button (phone recorder), closing the Maps pop-up and the backup Exit.", "accessibility", Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        if (LocationAuto.canWrite(c)) ok("Location permission (one-time adb): granted")
        else note("Location permission (one-time adb): not granted, so Google's Turn on pop-up appears. Granted once, it stays through updates.")
        out.add(CheckLine(1, "Location is " + (if (LocationAuto.isOn(c)) "ON" else "OFF") + " now"))
        out.add(CheckLine(1, if (LocationAuto.navNotification() != null) "Maps navigation: running" else "Maps navigation: not running"))

        // ---- Permissions
        if (granted("android.permission.POST_NOTIFICATIONS")) ok("Notification permission: allowed")
        else note("Notification permission is off (needed for the recording notification).", "notifications", appDetails)
        if (granted("android.permission.RECORD_AUDIO")) ok("Microphone permission: allowed")
        else note("Microphone permission: not allowed yet (asked the first time you record inside the app).")
        if (granted("android.permission.CALL_PHONE")) ok("Phone call permission: allowed")
        else note("Phone call permission: not allowed yet (fast dial asks the first time).")
        try {
            val pm = c.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isIgnoringBatteryOptimizations(c.packageName)) ok("Battery: unrestricted")
            else note("Battery is optimised: the phone may stop Notif Tiles in the background. Set Battery to Unrestricted in the app info.", "battery", appDetails)
        } catch (e: Exception) {
        }

        // ---- Features
        val rec = listOf("com.coloros.soundrecorder", "com.oplus.soundrecorder").firstOrNull { c.packageManager.getLaunchIntentForPackage(it) != null }
        if (rec != null) ok("Phone recorder found: $rec") else note("Phone recorder not found: the mic records inside Notif Tiles instead.")
        if (Weather.stale(c)) note("Weather: ${Weather.city(c)} (${Weather.sourceName(c)}) has not refreshed lately. Tap the refresh icon.")
        else ok("Weather: ${Weather.city(c)} (${Weather.sourceName(c)}) is up to date")
        when (Prefs.scrollMode(c)) {
            2 -> ok("Pages: My pages, " + Prefs.customPages(c).size + " page(s)")
            1 -> ok("Pages: pages of " + Prefs.pageSize(c) + " tiles")
            else -> ok("Pages: scroll up / down")
        }
        try {
            note("Version " + c.packageManager.getPackageInfo(c.packageName, 0).versionName)
        } catch (e: Exception) {
        }
        return out
    }

    fun text(lines: List<CheckLine>): String {
        val bad = lines.count { it.level == 2 }
        val sb = StringBuilder()
        sb.append(if (bad == 0) "Everything needed is OK.\n\n" else "$bad thing(s) need attention.\n\n")
        for (l in lines) sb.append(if (l.level == 0) "✔ " else if (l.level == 2) "✘ " else "• ").append(l.text).append("\n")
        return sb.toString().trimEnd()
    }
}
