package com.example.notiftiles

import android.content.Context

class Note(val id: String, val text: String)

object Prefs {
    val USUAL = listOf(
        // WhatsApp
        "com.whatsapp", "com.whatsapp.w4b",
        // Facebook
        "com.facebook.katana", "com.facebook.orca", "com.facebook.lite",
        // Phone / calls
        "com.google.android.dialer", "com.samsung.android.dialer", "com.samsung.android.incallui",
        "com.android.server.telecom", "com.android.dialer", "com.android.incallui",
        "com.android.phone", "com.android.contacts",
        // Messages / SMS
        "com.google.android.apps.messaging", "com.samsung.android.messaging",
        "com.android.mms", "com.android.messaging",
        // Mail and work
        "com.google.android.gm", "com.microsoft.office.outlook", "com.microsoft.teams",
        // Social
        "com.instagram.android", "com.snapchat.android"
    )

    private fun sp(c: Context) = c.getSharedPreferences("nt", Context.MODE_PRIVATE)

    fun enabled(c: Context): Set<String> =
        HashSet(sp(c).getStringSet("enabled", emptySet()) ?: emptySet())

    fun setEnabled(c: Context, pkg: String, on: Boolean) {
        val s = HashSet(enabled(c))
        if (on) s.add(pkg) else s.remove(pkg)
        sp(c).edit().putStringSet("enabled", s).apply()
    }

    fun enableAll(c: Context, pkgs: Collection<String>) {
        val s = HashSet(enabled(c))
        s.addAll(pkgs)
        sp(c).edit().putStringSet("enabled", s).apply()
    }

    fun seen(c: Context): Set<String> =
        HashSet(sp(c).getStringSet("seen", emptySet()) ?: emptySet())

    fun addSeen(c: Context, pkg: String) {
        val s = HashSet(seen(c))
        if (s.add(pkg)) sp(c).edit().putStringSet("seen", s).apply()
    }

    fun notes(c: Context): List<Note> {
        val raw = sp(c).getString("notes", "[]") ?: "[]"
        return try {
            val arr = org.json.JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Note(o.getString("id"), o.getString("text"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveNotes(c: Context, list: List<Note>) {
        val arr = org.json.JSONArray()
        for (n in list) {
            val o = org.json.JSONObject()
            o.put("id", n.id)
            o.put("text", n.text)
            arr.put(o)
        }
        sp(c).edit().putString("notes", arr.toString()).apply()
    }

    fun addNote(c: Context, text: String) {
        saveNotes(c, listOf(Note(System.currentTimeMillis().toString(), text)) + notes(c))
    }

    fun updateNote(c: Context, id: String, text: String) {
        saveNotes(c, notes(c).map { if (it.id == id) Note(id, text) else it })
    }

    fun deleteNote(c: Context, id: String) {
        saveNotes(c, notes(c).filter { it.id != id })
    }

    // ---- Fast dial (5 slots) ----
    const val DIAL_SLOTS = 5

    fun dialName(c: Context, i: Int): String = sp(c).getString("dial_name_$i", "") ?: ""
    fun dialNumber(c: Context, i: Int): String = sp(c).getString("dial_num_$i", "") ?: ""

    fun setDial(c: Context, i: Int, name: String, number: String) {
        sp(c).edit().putString("dial_name_$i", name).putString("dial_num_$i", number).apply()
    }

    fun clearDial(c: Context, i: Int) {
        sp(c).edit().remove("dial_name_$i").remove("dial_num_$i").apply()
    }

    // ---- Look & colors (0 = automatic / follow the phone's light or dark mode) ----
    fun panelBg(c: Context): Int = sp(c).getInt("look_panel_bg", 0)
    fun scrimColor(c: Context): Int = sp(c).getInt("look_scrim_color", 0xFF000000.toInt())
    fun scrimAlpha(c: Context): Int = sp(c).getInt("look_scrim_alpha", 40)
    fun widgetBg(c: Context): Int = sp(c).getInt("look_widget_bg", 0)
    fun widgetAlpha(c: Context): Int = sp(c).getInt("look_widget_alpha", 93)

    fun setPanelBg(c: Context, v: Int) = sp(c).edit().putInt("look_panel_bg", v).apply()
    fun setScrimColor(c: Context, v: Int) = sp(c).edit().putInt("look_scrim_color", v).apply()
    fun setScrimAlpha(c: Context, v: Int) = sp(c).edit().putInt("look_scrim_alpha", v).apply()
    fun setWidgetBg(c: Context, v: Int) = sp(c).edit().putInt("look_widget_bg", v).apply()
    fun setWidgetAlpha(c: Context, v: Int) = sp(c).edit().putInt("look_widget_alpha", v).apply()

    fun resetLook(c: Context) {
        val e = sp(c).edit()
        for (k in sp(c).all.keys) if (k.startsWith("look_") || k.startsWith("lay_")) e.remove(k)
        e.apply()
        Look.clearAllImages(c)
    }

    // ---- Accent colour (buttons, badge) ----
    fun accent(c: Context): Int = sp(c).getInt("look_accent", 0xFF3B82F6.toInt())
    fun setAccent(c: Context, v: Int) = sp(c).edit().putInt("look_accent", v).apply()

    // ---- Widget arrangement ----
    val ITEMS = listOf("clock", "weather", "date")

    fun order(c: Context): List<String> {
        val l = (sp(c).getString("lay_order", "clock,weather,date") ?: "").split(",").filter { it in ITEMS }.distinct()
        return if (l.size == 3) l else ITEMS
    }

    fun setOrder(c: Context, l: List<String>) = sp(c).edit().putString("lay_order", l.joinToString(",")).apply()

    /** key: clock, weather, date, maps, dial */
    fun show(c: Context, key: String): Boolean = sp(c).getBoolean("lay_show_$key", true)
    fun setShow(c: Context, key: String, v: Boolean) = sp(c).edit().putBoolean("lay_show_$key", v).apply()

    fun pillFirst(c: Context): Boolean = sp(c).getBoolean("lay_pill_first", false)
    fun setPillFirst(c: Context, v: Boolean) = sp(c).edit().putBoolean("lay_pill_first", v).apply()

    // ---- "Where to?" ----
    fun travelMode(c: Context): String = sp(c).getString("dest_mode", "driving") ?: "driving"
    fun setTravelMode(c: Context, v: String) = sp(c).edit().putString("dest_mode", v).apply()

    fun fav(c: Context, key: String): String = sp(c).getString("dest_fav_$key", "") ?: ""
    fun setFav(c: Context, key: String, v: String) = sp(c).edit().putString("dest_fav_$key", v.trim()).apply()

    fun recents(c: Context): List<String> =
        (sp(c).getString("dest_recent", "") ?: "").split("\n").filter { it.isNotBlank() }

    fun addRecent(c: Context, q: String) {
        val l = listOf(q) + recents(c).filter { !it.equals(q, ignoreCase = true) }
        sp(c).edit().putString("dest_recent", l.take(10).joinToString("\n")).apply()
    }

    fun clearRecents(c: Context) = sp(c).edit().remove("dest_recent").apply()

    fun rememberPlaces(c: Context): Boolean = sp(c).getBoolean("dest_remember", true)
    fun setRememberPlaces(c: Context, v: Boolean) = sp(c).edit().putBoolean("dest_remember", v).apply()

    fun suggestions(c: Context): Boolean = sp(c).getBoolean("dest_suggest", true)
    fun setSuggestions(c: Context, v: Boolean) = sp(c).edit().putBoolean("dest_suggest", v).apply()

    fun voiceButton(c: Context): Boolean = sp(c).getBoolean("dest_voice", true)
    fun setVoiceButton(c: Context, v: Boolean) = sp(c).edit().putBoolean("dest_voice", v).apply()

    fun navNow(c: Context): Boolean = sp(c).getBoolean("dest_nav_now", true)
    fun setNavNow(c: Context, v: Boolean) = sp(c).edit().putBoolean("dest_nav_now", v).apply()

    fun autoLocation(c: Context): Boolean = sp(c).getBoolean("dest_auto_loc", true)
    fun setAutoLocation(c: Context, v: Boolean) = sp(c).edit().putBoolean("dest_auto_loc", v).apply()

    // ---- Fonts ----
    fun fontFamily(c: Context): String = sp(c).getString("look_font", "sans-serif") ?: "sans-serif"
    fun setFontFamily(c: Context, v: String) = sp(c).edit().putString("look_font", v).apply()
    /** 0 = as designed, 1 = all bold, 2 = no bold */
    fun boldMode(c: Context): Int = sp(c).getInt("look_bold", 0)
    fun setBoldMode(c: Context, v: Int) = sp(c).edit().putInt("look_bold", v).apply()
    fun italic(c: Context): Boolean = sp(c).getBoolean("look_italic", false)
    fun setItalic(c: Context, v: Boolean) = sp(c).edit().putBoolean("look_italic", v).apply()
    fun fontScale(c: Context): Int = sp(c).getInt("look_fscale", 100)
    fun setFontScale(c: Context, v: Int) = sp(c).edit().putInt("look_fscale", v).apply()

    // ---- Text colour, shapes, sizes (0 = automatic) ----
    fun textColor(c: Context): Int = sp(c).getInt("look_text", 0)
    fun setTextColor(c: Context, v: Int) = sp(c).edit().putInt("look_text", v).apply()
    fun circleColor(c: Context): Int = sp(c).getInt("look_circle", 0)
    fun setCircleColor(c: Context, v: Int) = sp(c).edit().putInt("look_circle", v).apply()
    fun dialCircle(c: Context): Int = sp(c).getInt("look_dialcircle", 0)
    fun setDialCircle(c: Context, v: Int) = sp(c).edit().putInt("look_dialcircle", v).apply()
    fun squareShape(c: Context): Boolean = sp(c).getBoolean("look_square", false)
    fun setSquareShape(c: Context, v: Boolean) = sp(c).edit().putBoolean("look_square", v).apply()
    fun topSize(c: Context): Int = sp(c).getInt("look_sztop", 50)
    fun setTopSize(c: Context, v: Int) = sp(c).edit().putInt("look_sztop", v).apply()
    fun dialSize(c: Context): Int = sp(c).getInt("look_szdial", 42)
    fun setDialSize(c: Context, v: Int) = sp(c).edit().putInt("look_szdial", v).apply()

    // ---- Fast-dial strip background colour + images (kind: widget, dial, panel) ----
    fun dialBg(c: Context): Int = sp(c).getInt("look_dialbg", 0)
    fun setDialBg(c: Context, v: Int) = sp(c).edit().putInt("look_dialbg", v).apply()
    fun dialBgAlpha(c: Context): Int = sp(c).getInt("look_dialalpha", 70)
    fun setDialBgAlpha(c: Context, v: Int) = sp(c).edit().putInt("look_dialalpha", v).apply()
    fun imgAlpha(c: Context, kind: String): Int = sp(c).getInt("look_imgalpha_$kind", 100)
    fun setImgAlpha(c: Context, kind: String, v: Int) = sp(c).edit().putInt("look_imgalpha_$kind", v).apply()

    // ---- Layout extras ----
    fun dialNames(c: Context): Boolean = sp(c).getBoolean("lay_dialnames", true)
    fun setDialNames(c: Context, v: Boolean) = sp(c).edit().putBoolean("lay_dialnames", v).apply()
    /** 0 = follow phone, 1 = 12-hour, 2 = 24-hour */
    fun clockFmt(c: Context): Int = sp(c).getInt("lay_clockfmt", 0)
    fun setClockFmt(c: Context, v: Int) = sp(c).edit().putInt("lay_clockfmt", v).apply()
    val DATE_FORMATS = listOf(
        "EEE\nd MMM" to "Sat / 21 Nov",
        "d MMM" to "21 Nov",
        "EEE d" to "Sat 21",
        "d/M" to "21/11",
        "M/d" to "11/21",
        "EEEE" to "Saturday"
    )
    fun dateFmt(c: Context): Int = sp(c).getInt("lay_datefmt", 0).coerceIn(0, DATE_FORMATS.size - 1)
    fun setDateFmt(c: Context, v: Int) = sp(c).edit().putInt("lay_datefmt", v).apply()

    // ---- Counts: what counts as "handled" (read or replied) ----
    fun countRead(c: Context): Boolean = sp(c).getBoolean("cnt_read", true)
    fun setCountRead(c: Context, v: Boolean) = sp(c).edit().putBoolean("cnt_read", v).apply()
    fun countReply(c: Context): Boolean = sp(c).getBoolean("cnt_reply", true)
    fun setCountReply(c: Context, v: Boolean) = sp(c).edit().putBoolean("cnt_reply", v).apply()

    /** notification key -> time up to which it no longer counts (a newer post counts again). */
    fun handledMap(c: Context): Map<String, Long> {
        val raw = sp(c).getString("handled", "") ?: ""
        if (raw.isEmpty()) return emptyMap()
        return try {
            val o = org.json.JSONObject(raw)
            val m = HashMap<String, Long>()
            for (k in o.keys()) m[k] = o.getLong(k)
            m
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun markHandled(c: Context, key: String, until: Long) {
        val m = HashMap(handledMap(c))
        m[key] = maxOf(until, m[key] ?: 0L)
        while (m.size > 60) {
            val oldest = m.minByOrNull { it.value }?.key ?: break
            m.remove(oldest)
        }
        val o = org.json.JSONObject()
        for ((k, v) in m) o.put(k, v)
        sp(c).edit().putString("handled", o.toString()).apply()
    }

    /** Panel sheet colour actually used. */
    fun panelBgColor(c: Context): Int {
        val v = panelBg(c)
        if (v != 0) return v
        return if (Feed.isDark(c)) 0xFF1C1C20.toInt() else 0xFFF2F2F5.toInt()
    }

    /** The dim ("shadow") drawn behind the panel. */
    fun scrimArgb(c: Context): Int =
        ((scrimAlpha(c) * 255 / 100) shl 24) or (scrimColor(c) and 0xFFFFFF)

    /** Widget background colour including its opacity. */
    fun widgetBgArgb(c: Context): Int {
        val v = widgetBg(c)
        val base = if (v != 0) v else c.getColor(R.color.nt_bg)
        return ((widgetAlpha(c) * 255 / 100) shl 24) or (base and 0xFFFFFF)
    }
}
