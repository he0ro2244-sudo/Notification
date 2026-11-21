package com.example.notiftiles

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL

// Amman, Jordan weather from Open-Meteo (free, no API key).
object Weather {
    private const val URL_STR =
        "https://api.open-meteo.com/v1/forecast?latitude=31.95&longitude=35.93" +
            "&current=temperature_2m,weather_code,is_day&timezone=Asia%2FAmman"
    private const val EVERY_MS = 30 * 60 * 1000L

    private fun sp(c: Context) = c.getSharedPreferences("nt", Context.MODE_PRIVATE)

    fun stale(c: Context): Boolean =
        System.currentTimeMillis() - sp(c).getLong("w_try", 0L) > EVERY_MS

    /** Blocking. Call from a background thread. */
    fun fetch(c: Context) {
        sp(c).edit().putLong("w_try", System.currentTimeMillis()).apply()
        try {
            val conn = URL(URL_STR).openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            val body = try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
            val cur = org.json.JSONObject(body).getJSONObject("current")
            sp(c).edit()
                .putInt("w_temp", Math.round(cur.getDouble("temperature_2m")).toInt())
                .putInt("w_code", cur.getInt("weather_code"))
                .putInt("w_day", cur.optInt("is_day", 1))
                .putBoolean("w_has", true)
                .apply()
        } catch (e: Exception) {
            // keep the last known values
        }
    }

    /** Two-line text for the circle: icon, then degrees. */
    fun text(c: Context): String {
        val p = sp(c)
        if (!p.getBoolean("w_has", false)) return "🌡️\n--°"
        return emoji(p.getInt("w_code", 0), p.getInt("w_day", 1) == 1) + "\n" + p.getInt("w_temp", 0) + "°"
    }

    private fun emoji(code: Int, day: Boolean): String = when (code) {
        0 -> if (day) "☀️" else "🌙"
        1 -> if (day) "🌤️" else "🌙"
        2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..57 -> "🌦️"
        in 61..67 -> "🌧️"
        in 71..77 -> "❄️"
        in 80..82 -> "🌦️"
        85, 86 -> "❄️"
        in 95..99 -> "⛈️"
        else -> "🌡️"
    }
}
