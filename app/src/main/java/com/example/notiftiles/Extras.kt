package com.example.notiftiles

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

// Snooze, quiet hours, "mark all read" and the one small alarm that wakes them (also used by the auto theme).
// Battery: no polling and no wake-up alarm; a single inexact alarm for the next change only, or none at all.
object Extras {
    fun minutesNow(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE)
    }

    /** True when minute-of-day [m] is inside start..end (the window may cross midnight). */
    fun inWindow(m: Int, start: Int, end: Int): Boolean =
        if (start == end) false else if (start < end) m in start until end else (m >= start || m < end)

    fun fmt(min: Int): String = String.format(java.util.Locale.getDefault(), "%02d:%02d", min / 60, min % 60)

    fun fmtTime(ms: Long): String =
        java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(ms))

    /** The next moment (today or tomorrow) when the clock shows [minute] of the day. */
    fun nextAt(minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, minute / 60)
        cal.set(Calendar.MINUTE, minute % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    fun snoozed(c: Context): Boolean = Prefs.snoozeUntil(c) > System.currentTimeMillis()

    fun quietActive(c: Context): Boolean {
        if (snoozed(c)) return true
        if (!Prefs.quietOn(c)) return false
        return inWindow(minutesNow(), Prefs.quietStart(c), Prefs.quietEnd(c))
    }

    /** Text for the widget while nothing is shown because of snooze / quiet hours, else null. */
    fun quietText(c: Context): String? {
        if (snoozed(c)) return "Snoozed until " + fmtTime(Prefs.snoozeUntil(c))
        if (Prefs.quietOn(c) && inWindow(minutesNow(), Prefs.quietStart(c), Prefs.quietEnd(c))) {
            return "Quiet hours until " + fmt(Prefs.quietEnd(c))
        }
        return null
    }

    /** Counts every shown message as read (and clears it from the phone's shade if that option is on). */
    fun markAllRead(c: Context) {
        val svc = NotifListener.instance
        for (n in Feed.items(c, true)) {
            Feed.markDone(c, n, Feed.handledStamp(n))
            if (Prefs.dismissHandled(c) && n.isClearable) svc?.cancelNotification(n.key)
        }
    }

    /** Applies what is due (auto theme) and sets the next alarm. Cheap; call it whenever settings change. */
    fun tick(c: Context) {
        Themes.applyAuto(c)
        schedule(c)
    }

    fun schedule(c: Context) {
        try {
            val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                c, 98, Intent(c, TilesWidget::class.java).setAction(TilesWidget.ACTION_TICK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            var t = Long.MAX_VALUE
            val sn = Prefs.snoozeUntil(c)
            if (sn > System.currentTimeMillis()) t = sn
            if (Prefs.quietOn(c)) t = minOf(t, nextAt(Prefs.quietStart(c)), nextAt(Prefs.quietEnd(c)))
            if (Prefs.autoTheme(c)) t = minOf(t, nextAt(Prefs.autoDayStart(c)), nextAt(Prefs.autoNightStart(c)))
            if (t == Long.MAX_VALUE) am.cancel(pi) else am.set(AlarmManager.RTC, t + 1000L, pi)
        } catch (e: Exception) {
            // the widget still refreshes on its normal schedule
        }
    }
}
