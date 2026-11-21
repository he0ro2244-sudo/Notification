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
    val SOURCES = listOf(
        "om" to "Open-Meteo",
        "ecmwf" to "ECMWF",
        "icon" to "ICON (DWD)",
        "gfs" to "GFS (NOAA)",
        "gem" to "GEM (Canada)",
        "mf" to "Météo-France",
        "ukmo" to "UK Met Office",
        "metno" to "Yr / MET Norway",
        "metar" to "Nearest airport (observed)"
    )
    fun source(c: Context): String = sp(c).getString("w_src", "om") ?: "om"
    fun setSource(c: Context, v: String) {
        sp(c).edit().putString("w_src", v).putLong("w_try", 0L).remove("w_gap").apply()
    }
    fun sourceName(c: Context): String = SOURCES.firstOrNull { it.first == source(c) }?.second ?: "Open-Meteo"

    /** Manual adjustment in whole degrees (shown unit), for when a source runs a bit warm or cold. */
    fun offset(c: Context): Int = sp(c).getInt("w_off", 0)
    fun setOffset(c: Context, v: Int) = sp(c).edit().putInt("w_off", v).apply()

    fun showUpdated(c: Context): Boolean = sp(c).getBoolean("w_showupd", true)
    fun setShowUpdated(c: Context, v: Boolean) = sp(c).edit().putBoolean("w_showupd", v).apply()

    /** "Updated 14:05 · Open-Meteo", or "" when nothing has been fetched yet. */
    fun updatedText(c: Context): String {
        val sh = shown(c) ?: return ""
        if (sh.time == 0L) return ""
        val time = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(sh.time))
        return "Updated $time · ${sh.label}"
    }

    // ---- Temperature copied from the phone's own weather app (read on screen by the helper) ----
    val APPS = listOf(
        "com.google.android.apps.weather", "com.sec.android.daemonapp", "com.miui.weather2",
        "com.huawei.android.totemweather", "net.oneplus.weather", "com.oplus.weather", "com.coloros.weather2",
        "com.coloros.weather.service", "com.heytap.weather", "com.realme.weather", "com.motorola.weather",
        "com.accuweather.android", "com.weather.Weather", "com.yahoo.mobile.client.android.weather"
    )

    /** both = phone app's exact number when copied, then online + learned difference; app = phone app only; online = online only */
    fun mode(c: Context): String = sp(c).getString("w_mode", "both") ?: "both"
    fun setMode(c: Context, v: String) = sp(c).edit().putString("w_mode", v).apply()

    /** True when copying from the phone's weather app is wanted at all. */
    fun useApp(c: Context): Boolean = mode(c) != "online"

    fun appTime(c: Context): Long = sp(c).getLong("w_apptime", 0L)
    fun appTemp(c: Context): Int = sp(c).getInt("w_apptemp", 0)

    fun gapKnown(c: Context): Boolean = sp(c).contains("w_gap")
    fun gap(c: Context): Int = sp(c).getInt("w_gap", 0)
    private fun signed(n: Int): String = if (n > 0) "+$n" else n.toString()

    /** Right after a copy the phone's number is shown as is; later the online number plus the learned difference. */
    private const val APP_EXACT_MS = 20 * 60 * 1000L

    private class Shown(val temp: Int, val code: Int, val day: Boolean, val label: String, val time: Long)

    private fun shown(c: Context): Shown? {
        val p = sp(c)
        val m = mode(c)
        val hasApp = m != "online" && appTime(c) > 0L
        val hasOnline = p.getBoolean("w_has", false)
        val appAge = System.currentTimeMillis() - appTime(c)
        fun app(): Shown {
            val ac = p.getInt("w_appcode", -1)
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            return Shown(appTemp(c), if (ac >= 0) ac else p.getInt("w_code", 0), hour in 6..17, "Phone weather app", appTime(c))
        }
        fun online(adj: Int): Shown = Shown(
            p.getInt("w_temp", 0) + adj, p.getInt("w_code", 0), p.getInt("w_day", 1) == 1,
            sourceName(c) + (if (adj != 0) ", " + signed(adj) + "° to match phone" else ""), p.getLong("w_time", 0L)
        )
        return when (m) {
            "online" -> if (hasOnline) online(0) else null
            "app" -> if (hasApp) app() else if (hasOnline) online(0) else null
            else -> when {
                hasApp && (appAge <= APP_EXACT_MS || !hasOnline) -> app()
                hasApp && gapKnown(c) -> online(gap(c))
                hasOnline -> online(0)
                hasApp -> app()
                else -> null
            }
        }
    }

    /** Blocking. Right after a copy from the phone app: fetch the online number and remember the difference. */
    fun learnGap(c: Context) {
        if (mode(c) != "both") return
        if (!fetch(c)) return
        sp(c).edit().putInt("w_gap", appTemp(c) - sp(c).getInt("w_temp", 0)).putLong("w_gaptime", System.currentTimeMillis()).apply()
    }

    /** Launch intent of the phone's weather app, or null. */
    fun findApp(pm: android.content.pm.PackageManager): android.content.Intent? {
        val main = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(main, 0)
        for (pkg in APPS) {
            val hit = apps.firstOrNull { it.activityInfo.packageName == pkg } ?: continue
            return pm.getLaunchIntentForPackage(hit.activityInfo.packageName)
        }
        for (ri in apps) {
            val label = ri.loadLabel(pm).toString().lowercase()
            val pkg = ri.activityInfo.packageName.lowercase()
            if ("weather" in label || "weather" in pkg || "الطقس" in label || "طقس" in label) {
                return pm.getLaunchIntentForPackage(ri.activityInfo.packageName)
            }
        }
        return null
    }

    fun saveFromApp(c: Context, temp: Int, cond: String?) {
        val e = sp(c).edit().putInt("w_apptemp", temp).putLong("w_apptime", System.currentTimeMillis())
        e.putInt("w_appcode", if (cond == null) -1 else condCode(cond))
        e.apply()
    }

    /** Words the app shows ("Clear", "Mostly cloudy", "Light rain") to the WMO-style code used by [emoji]. */
    private fun condCode(text: String): Int {
        val t = text.lowercase()
        return when {
            "thunder" in t || "storm" in t -> 95
            "snow" in t || "sleet" in t -> 71
            "rain" in t || "shower" in t || "drizzle" in t -> 61
            "fog" in t || "mist" in t || "haze" in t || "dust" in t -> 45
            "partly" in t || "mostly clear" in t -> 2
            "cloud" in t || "overcast" in t -> 3
            "clear" in t || "sunny" in t -> 0
            else -> -1
        }
    }

    fun setUnit(c: Context, u: String) {
        sp(c).edit().putString("w_unit", u).putLong("w_try", 0L).putBoolean("w_has", false).remove("w_gap").apply()
    }

    private fun url(c: Context, src: String): String {
        val lat = sp(c).getString("w_lat", "31.95")
        val lon = sp(c).getString("w_lon", "35.93")
        val model = when (src) {
            "ecmwf" -> "&models=ecmwf_ifs025"
            "icon" -> "&models=icon_seamless"
            "gfs" -> "&models=gfs_seamless"
            "gem" -> "&models=gem_seamless"
            "mf" -> "&models=meteofrance_seamless"
            "ukmo" -> "&models=ukmo_seamless"
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

    class Reading(val temp: Int, val code: Int, val day: Int)

    private fun metarCode(wx: String, cover: String): Int = when {
        "TS" in wx -> 95
        "SN" in wx || "SG" in wx -> 71
        "RA" in wx || "DZ" in wx || "SH" in wx -> 61
        "FG" in wx || "BR" in wx || "HZ" in wx -> 45
        cover == "CLR" || cover == "SKC" || cover == "CAVOK" || cover == "NCD" || cover == "NSC" -> 0
        cover == "FEW" -> 1
        cover == "SCT" -> 2
        else -> 3
    }

    /** Blocking. One reading from the given source, or null when it fails. Nothing is saved. */
    fun read(c: Context, src: String): Reading? = try {
        val f = unit(c) == "f"
        when (src) {
            "metno" -> {
                val ts = org.json.JSONObject(get(metnoUrl(c))).getJSONObject("properties")
                    .getJSONArray("timeseries").getJSONObject(0).getJSONObject("data")
                var t = ts.getJSONObject("instant").getJSONObject("details").getDouble("air_temperature")
                if (f) t = t * 9.0 / 5.0 + 32.0
                val sym = ts.optJSONObject("next_1_hours")?.optJSONObject("summary")?.optString("symbol_code")
                    ?: ts.optJSONObject("next_6_hours")?.optJSONObject("summary")?.optString("symbol_code") ?: "cloudy"
                Reading(Math.round(t).toInt(), metnoCode(sym), if (sym.endsWith("_night")) 0 else 1)
            }
            "metar" -> {
                val lat = (sp(c).getString("w_lat", "31.95") ?: "31.95").toDouble()
                val lon = (sp(c).getString("w_lon", "35.93") ?: "35.93").toDouble()
                val box = "${lat - 0.7},${lon - 0.7},${lat + 0.7},${lon + 0.7}"
                val arr = org.json.JSONArray(get("https://aviationweather.gov/api/data/metar?bbox=$box&format=json"))
                var best: org.json.JSONObject? = null
                var bestD = Double.MAX_VALUE
                for (k in 0 until arr.length()) {
                    val o = arr.getJSONObject(k)
                    if (o.isNull("temp") || !o.has("lat") || !o.has("lon")) continue
                    val dl = o.getDouble("lat") - lat
                    val dn = o.getDouble("lon") - lon
                    val d = dl * dl + dn * dn
                    if (d < bestD) { bestD = d; best = o }
                }
                val o = best ?: return null
                var t = o.getDouble("temp")
                if (f) t = t * 9.0 / 5.0 + 32.0
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                Reading(
                    Math.round(t).toInt(),
                    metarCode(o.optString("wxString", ""), o.optString("cover", "")),
                    if (hour in 6..17) 1 else 0
                )
            }
            else -> {
                val cur = org.json.JSONObject(get(url(c, src))).getJSONObject("current")
                Reading(Math.round(cur.getDouble("temperature_2m")).toInt(), cur.getInt("weather_code"), cur.optInt("is_day", 1))
            }
        }
    } catch (e: Exception) {
        null
    }

    /** Blocking. Reads every source once, for the "Compare sources" picker. */
    fun compare(c: Context): List<Triple<String, String, Int?>> =
        SOURCES.map { (k, name) -> Triple(k, name, read(c, k)?.temp) }

    /** Blocking. Call from a background thread. Returns true when new data was stored. */
    fun fetch(c: Context): Boolean {
        sp(c).edit().putLong("w_try", System.currentTimeMillis()).apply()
        val r = read(c, source(c))
        if (r == null) {
            // keep the last known values, but try again in about 2 minutes instead of waiting the full interval
            sp(c).edit().putLong("w_try", System.currentTimeMillis() - EVERY_MS + 2 * 60 * 1000L).apply()
            return false
        }
        sp(c).edit()
            .putInt("w_temp", r.temp)
            .putInt("w_code", r.code)
            .putInt("w_day", r.day)
            .putBoolean("w_has", true)
            .putLong("w_time", System.currentTimeMillis())
            .apply()
        return true
    }

    /** "16° (Open-Meteo)" style summary for messages. */
    fun summary(c: Context): String {
        val sh = shown(c) ?: return "no data yet"
        return "${sh.temp + offset(c)}° · ${sh.label}"
    }

    /** Two-line text for the circle: icon, then degrees. */
    fun text(c: Context): String {
        val sh = shown(c) ?: return "🌡️\n--°"
        return emoji(sh.code, sh.day) + "\n" + (sh.temp + offset(c)) + "°"
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
