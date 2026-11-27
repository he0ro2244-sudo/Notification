package com.example.notiftiles

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle

// Behind the weather circle: opens the weather app installed on this phone.
class OpenWeatherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Also refresh the number on the widget.
        sendBroadcast(Intent(this, TilesWidget::class.java).setAction(TilesWidget.ACTION_WEATHER))
        val launch = findWeatherApp()
        try {
            if (launch != null) {
                startActivity(launch)
            } else {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather+Amman+Jordan"))
                )
            }
        } catch (e: Exception) {
            // nothing to open
        }
        finish()
    }

    private fun findWeatherApp(): Intent? {
        val pm = packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(main, 0)
        // 1) well-known weather apps (Google, Samsung, Xiaomi, Huawei, OnePlus, Oppo/realme, etc.)
        for (pkg in KNOWN) {
            val hit = apps.firstOrNull { it.activityInfo.packageName == pkg } ?: continue
            return pm.getLaunchIntentForPackage(hit.activityInfo.packageName)
        }
        // 2) any app whose name or package says "weather"
        for (ri in apps) {
            val label = ri.loadLabel(pm).toString().lowercase()
            val pkg = ri.activityInfo.packageName.lowercase()
            if ("weather" in label || "weather" in pkg || "الطقس" in label || "طقس" in label) {
                return pm.getLaunchIntentForPackage(ri.activityInfo.packageName)
            }
        }
        return null
    }

    companion object {
        private val KNOWN = listOf(
            "com.google.android.apps.weather",
            "com.sec.android.daemonapp",
            "com.miui.weather2",
            "com.huawei.android.totemweather",
            "net.oneplus.weather",
            "com.oplus.weather",
            "com.coloros.weather2",
            "com.coloros.weather.service",
            "com.motorola.weather",
            "com.accuweather.android",
            "com.weather.Weather",
            "com.yahoo.mobile.client.android.weather"
        )
    }
}
