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
