package com.example.notiftiles

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock

// Behind the clock circle: opens the clock app installed on this phone.
class OpenClockActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            startActivity(findClockApp() ?: Intent(AlarmClock.ACTION_SHOW_ALARMS))
        } catch (e: Exception) {
            // nothing to open
        }
        finish()
    }

    private fun findClockApp(): Intent? {
        val pm = packageManager
        val apps = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        )
        for (pkg in KNOWN) {
            val hit = apps.firstOrNull { it.activityInfo.packageName == pkg } ?: continue
            return pm.getLaunchIntentForPackage(hit.activityInfo.packageName)
        }
        for (ri in apps) {
            val label = ri.loadLabel(pm).toString().lowercase()
            val pkg = ri.activityInfo.packageName.lowercase()
            if (label == "clock" || "deskclock" in pkg || "clockpackage" in pkg || "ساعة" in label) {
                return pm.getLaunchIntentForPackage(ri.activityInfo.packageName)
            }
        }
        return null
    }

    companion object {
        private val KNOWN = listOf(
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.android.deskclock",
            "com.huawei.deskclock",
            "com.oneplus.deskclock",
            "com.oplus.alarmclock",
            "com.coloros.alarmclock",
            "com.motorola.blur.alarmclock"
        )
    }
}
