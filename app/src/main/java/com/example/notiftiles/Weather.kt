package com.example.notiftiles

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Weather from Open-Meteo (free, no API key). City and unit are chosen in Look & colors.
object Weather {
    private const val EVERY_MS = 30 * 60 * 1000L

    private fun sp(c: Context) = c.getSharedPreferences("nt", Context.MODE_PRIVATE)

    fun city(c: Context): String = sp(c).getString("w_city", "Amman, Jordan") ?: "Amman, Jordan"
    fun unit(c: Context): String = sp(c).getString("w_unit", "c") ?: "c"

    fun setUnit(c: Context, u: String) {
        sp(c).edit().putString("w_unit", u).putLong("w_try", 0L).putBoolean("w_has", false).apply()
    }

    private fun url(c: Context): String {
        val lat = sp(c).getString("w_lat", "31.95")
        val lon = sp(c).getString("w_lon", "35.93")
        return "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code,is_day&timezone=auto" +
            (if (unit(c) == "f") "&temperature_unit=fahrenheit" else "")
    }

    private fun get(u: String): String {
        val conn = URL(u).openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    /** Blocking. Finds a city by name and makes it the weather location. Returns its name, or null. */
    fun lookup(c: Context, name: String): String? {
        return try {
            val body = get(
                "https://geocoding-api.open-meteo.com/v1/search?name=" +
                    URLEncoder.encode(name, "UTF-8") + "&count=1&language=en&format=json"
            )
            val r = org.json.JSONObject(body).optJSONArray("results")?.optJSONObject(0) ?: return null
            val label = r.optString("name") + (r.optString("country").let { if (it.isNotEmpty()) ", $it" else "" })
            sp(c).edit()
                .putString("w_lat", r.getDouble("latitude").toString())
                .putString("w_lon", r.getDouble("longitude").toString())
                .putString("w_city", label)
                .putLong("w_try", 0L)
                .putBoolean("w_has", false)
                .apply()
            label
        } catch (e: Exception) {
            null
        }
    }

    fun stale(c: Context): Boolean =
        System.currentTimeMillis() - sp(c).getLong("w_try", 0L) > EVERY_MS

    /** Blocking. Call from a background thread. */
    fun fetch(c: Context) {
        sp(c).edit().putLong("w_try", System.currentTimeMillis()).apply()
        try {
            val cur = org.json.JSONObject(get(url(c))).getJSONObject("current")
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
