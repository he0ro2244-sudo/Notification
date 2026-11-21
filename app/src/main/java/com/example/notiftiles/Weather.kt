package com.example.notiftiles

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Weather from Open-Meteo (free, no API key). City and unit are chosen in Look & colors.
object Weather {
    private const val EVERY_MS = 15 * 60 * 1000L

    private fun sp(c: Context) = c.getSharedPreferences("nt", Context.MODE_PRIVATE)

    fun city(c: Context): String = sp(c).getString("w_city", "Amman, Jordan") ?: "Amman, Jordan"
    fun unit(c: Context): String = sp(c).getString("w_unit", "c") ?: "c"

    /** om = Open-Meteo best match, ecmwf / icon = Open-Meteo with that model, metno = Norwegian Met Institute (yr.no) */
    val SOURCES = listOf("om" to "Open-Meteo", "ecmwf" to "ECMWF", "icon" to "ICON (DWD)", "metno" to "Yr / MET Norway")
    fun source(c: Context): String = sp(c).getString("w_src", "om") ?: "om"
    fun setSource(c: Context, v: String) {
        sp(c).edit().putString("w_src", v).putLong("w_try", 0L).apply()
    }
    fun sourceName(c: Context): String = SOURCES.firstOrNull { it.first == source(c) }?.second ?: "Open-Meteo"

    /** Manual adjustment in whole degrees (shown unit), for when a source runs a bit warm or cold. */
    fun offset(c: Context): Int = sp(c).getInt("w_off", 0)
    fun setOffset(c: Context, v: Int) = sp(c).edit().putInt("w_off", v).apply()

    fun showUpdated(c: Context): Boolean = sp(c).getBoolean("w_showupd", true)
    fun setShowUpdated(c: Context, v: Boolean) = sp(c).edit().putBoolean("w_showupd", v).apply()

    /** "Updated 14:05 · Open-Meteo", or "" when nothing has been fetched yet. */
    fun updatedText(c: Context): String {
        val t = sp(c).getLong("w_time", 0L)
        if (t == 0L) return ""
        val time = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(t))
        return "Updated $time · ${sourceName(c)}"
    }

    fun setUnit(c: Context, u: String) {
        sp(c).edit().putString("w_unit", u).putLong("w_try", 0L).putBoolean("w_has", false).apply()
    }

    private fun url(c: Context): String {
        val lat = sp(c).getString("w_lat", "31.95")
        val lon = sp(c).getString("w_lon", "35.93")
        val model = when (source(c)) {
            "ecmwf" -> "&models=ecmwf_ifs025"
            "icon" -> "&models=icon_seamless"
            else -> ""
        }
        return "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code,is_day&timezone=auto" + model +
            (if (unit(c) == "f") "&temperature_unit=fahrenheit" else "")
    }

    private fun metnoUrl(c: Context): String {
        val lat = sp(c).getString("w_lat", "31.95")
        val lon = sp(c).getString("w_lon", "35.93")
        return "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=$lat&lon=$lon"
    }

    /** MET Norway symbol_code (e.g. "partlycloudy_day") to the WMO-style code used by [emoji]. */
    private fun metnoCode(sym: String): Int {
        val base = sym.substringBefore("_")
        return when {
            "thunder" in base -> 95
            "snow" in base || "sleet" in base -> 71
            "heavyrain" in base -> 65
            "showers" in base -> 80
            "rain" in base -> 61
            base == "clearsky" -> 0
            base == "fair" -> 1
            base == "partlycloudy" -> 2
            base == "cloudy" -> 3
            base == "fog" -> 45
            else -> 3
        }
    }

    private fun get(u: String): String {
        val conn = URL(u).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "NotifTiles/0.9 (personal widget)")
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
            if (source(c) == "metno") {
                val ts = org.json.JSONObject(get(metnoUrl(c))).getJSONObject("properties")
                    .getJSONArray("timeseries").getJSONObject(0).getJSONObject("data")
                var t = ts.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature")
                if (unit(c) == "f") t = t * 9.0 / 5.0 + 32.0
                val sym = ts.optJSONObject("next_1_hours")?.optJSONObject("summary")?.optString("symbol_code")
                    ?: ts.optJSONObject("next_6_hours")?.optJSONObject("summary")?.optString("symbol_code") ?: "cloudy"
                sp(c).edit()
                    .putInt("w_temp", Math.round(t).toInt())
                    .putInt("w_code", metnoCode(sym))
                    .putInt("w_day", if (sym.endsWith("_night")) 0 else 1)
                    .putBoolean("w_has", true)
                    .putLong("w_time", System.currentTimeMillis())
                    .apply()
            } else {
                val cur = org.json.JSONObject(get(url(c))).getJSONObject("current")
                sp(c).edit()
                    .putInt("w_temp", Math.round(cur.getDouble("temperature_2m")).toInt())
                    .putInt("w_code", cur.getInt("weather_code"))
                    .putInt("w_day", cur.optInt("is_day", 1))
                    .putBoolean("w_has", true)
                    .putLong("w_time", System.currentTimeMillis())
                    .apply()
            }
        } catch (e: Exception) {
            // keep the last known values, but try again in about 2 minutes instead of waiting the full interval
            sp(c).edit().putLong("w_try", System.currentTimeMillis() - EVERY_MS + 2 * 60 * 1000L).apply()
        }
    }

    /** Two-line text for the circle: icon, then degrees. */
    fun text(c: Context): String {
        val p = sp(c)
        if (!p.getBoolean("w_has", false)) return "🌡️\n--°"
        return emoji(p.getInt("w_code", 0), p.getInt("w_day", 1) == 1) + "\n" + (p.getInt("w_temp", 0) + offset(c)) + "°"
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
