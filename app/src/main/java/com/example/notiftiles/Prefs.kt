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

    // ---- Shortcuts row under the fast dial (5 slots): an app, an activity of an app, or a Nova-style shortcut ----
    const val QUICK_SLOTS = 5
    fun quickOn(c: Context): Boolean = sp(c).getBoolean("quick_on", false)
    fun setQuickOn(c: Context, v: Boolean) = sp(c).edit().putBoolean("quick_on", v).apply()
    fun quick(c: Context, i: Int): Quick? {
        val t = sp(c).getString("quick_$i", null) ?: return null
        return try {
            val o = org.json.JSONObject(t)
            Quick(o.optString("t"), o.optString("n"), o.optString("p"), o.optString("g"))
        } catch (e: Exception) {
            null
        }
    }
    fun setQuick(c: Context, i: Int, q: Quick) = sp(c).edit().putString(
        "quick_$i", org.json.JSONObject().put("t", q.type).put("n", q.name).put("p", q.pkg).put("g", q.target).toString()
    ).apply()
    fun clearQuick(c: Context, i: Int) = sp(c).edit().remove("quick_$i").apply()

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

    /** Top-row items merged into ONE shape (any of clock, weather, date; fewer than 2 = nothing merged). */
    fun merged(c: Context): Set<String> =
        (sp(c).getString("lay_merge", "") ?: "").split(",").filter { it in ITEMS }.toSet()
    fun setMerged(c: Context, s: Set<String>) = sp(c).edit().putString("lay_merge", s.joinToString(",")).apply()

    fun pillFirst(c: Context): Boolean = sp(c).getBoolean("lay_pill_first", false)
    fun setPillFirst(c: Context, v: Boolean) = sp(c).edit().putBoolean("lay_pill_first", v).apply()

    /** Where the "Where to?" box sits in the top row: 0 = before the 1st circle ... 3 = after the 3rd (the old "at the end"). */
    fun pillPos(c: Context): Int {
        val s = sp(c)
        return if (s.contains("lay_pill_pos")) s.getInt("lay_pill_pos", 3).coerceIn(0, 3) else if (pillFirst(c)) 0 else 3
    }
    fun setPillPos(c: Context, v: Int) =
        sp(c).edit().putInt("lay_pill_pos", v.coerceIn(0, 3)).putBoolean("lay_pill_first", v == 0).apply()

    /** The sections of the widget above the tiles, top to bottom: top = clock/weather/date/Where to?, dial, quick = shortcuts row, head = Inbox row. */
    val BLOCKS = listOf("top", "dial", "quick", "head")
    val ITEM_KEYS = listOf("w_title", "w_note", "w_clear", "w_mic", "w_gear", "w_edit")
    fun blockOrder(c: Context): List<String> {
        val l = (sp(c).getString("lay_blocks", "top,dial,quick,head") ?: "").split(",").filter { it in BLOCKS }.distinct()
        return if (l.size == BLOCKS.size) l else BLOCKS
    }
    fun setBlockOrder(c: Context, l: List<String>) = sp(c).edit().putString("lay_blocks", l.joinToString(",")).apply()

    /** Size of one single item (w_title, w_note, w_clear, w_mic, w_gear) in percent, on top of its section's size. */
    fun itemScale(c: Context, key: String): Int = sp(c).getInt("lay_iscale_$key", 100).coerceIn(50, 250)
    fun setItemScale(c: Context, key: String, v: Int) = sp(c).edit().putInt("lay_iscale_$key", v.coerceIn(50, 250)).apply()

    /** A small pencil on the widget that opens Arrange & resize (off until switched on). */
    fun editBtn(c: Context): Boolean = sp(c).getBoolean("lay_editbtn", false)
    fun setEditBtn(c: Context, v: Boolean) = sp(c).edit().putBoolean("lay_editbtn", v).apply()

    /** Size of one widget section in percent (100 = as before). */
    fun blockScale(c: Context, key: String): Int = sp(c).getInt("lay_scale_$key", 100).coerceIn(50, 200)
    fun setBlockScale(c: Context, key: String, v: Int) = sp(c).edit().putInt("lay_scale_$key", v.coerceIn(50, 200)).apply()
    fun resetArrange(c: Context) {
        val e = sp(c).edit()
        for (k in listOf("lay_blocks", "lay_order", "lay_pill_pos", "lay_pill_first", "lay_merge", "lay_editbtn")) e.remove(k)
        for (k in sp(c).all.keys) if (k.startsWith("lay_iscale_")) e.remove(k)
        for (k in BLOCKS) e.remove("lay_scale_$k")
        e.apply()
    }

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

    // Also switch Location off after a navigation started from "Where to?" when Location was already on before
    fun locOffAlways(c: Context): Boolean = sp(c).getBoolean("loc_off_always", true)
    fun setLocOffAlways(c: Context, v: Boolean) = sp(c).edit().putBoolean("loc_off_always", v).apply()

    // Closing the Google Maps pop-up (picture-in-picture window) also ends the navigation (needs the Accessibility helper)
    fun exitOnPopupClose(c: Context): Boolean = sp(c).getBoolean("nav_exit_popup", true)
    fun setExitOnPopupClose(c: Context, v: Boolean) = sp(c).edit().putBoolean("nav_exit_popup", v).apply()

    // "Where to?" opens the Google Maps search screen directly (the Accessibility helper taps Maps' search bar)
    fun mapsSearchDirect(c: Context): Boolean = sp(c).getBoolean("dest_maps_direct", false)
    fun setMapsSearchDirect(c: Context, v: Boolean) = sp(c).edit().putBoolean("dest_maps_direct", v).apply()

    // Location that Notif Tiles switched on by itself for navigation, so it can switch it off again afterwards
    fun autoLocationOff(c: Context): Boolean = sp(c).getBoolean("loc_auto_off", true)
    fun setAutoLocationOff(c: Context, v: Boolean) = sp(c).edit().putBoolean("loc_auto_off", v).apply()
    fun locOwned(c: Context): Boolean = sp(c).getBoolean("loc_owned", false)
    fun locSince(c: Context): Long = sp(c).getLong("loc_since", 0L)
    fun locSeen(c: Context): Boolean = sp(c).getBoolean("loc_seen", false)
    fun setLocSeen(c: Context, v: Boolean) = sp(c).edit().putBoolean("loc_seen", v).apply()
    fun setLocOwned(c: Context, on: Boolean) = sp(c).edit()
        .putBoolean("loc_owned", on)
        .putLong("loc_since", if (on) System.currentTimeMillis() else 0L)
        .putBoolean("loc_seen", false)
        .apply()

    // ---- Borders (0 thickness = none; colour 0 = automatic) ----
    fun borderWidth(c: Context): Int = sp(c).getInt("look_bw", 0)
    fun setBorderWidth(c: Context, v: Int) = sp(c).edit().putInt("look_bw", v).apply()
    fun borderColor(c: Context): Int = sp(c).getInt("look_bc", 0)
    fun setBorderColor(c: Context, v: Int) = sp(c).edit().putInt("look_bc", v).apply()
    fun borderPanel(c: Context): Boolean = sp(c).getBoolean("look_bpanel", true)
    fun setBorderPanel(c: Context, v: Boolean) = sp(c).edit().putBoolean("look_bpanel", v).apply()
    fun borderTiles(c: Context): Boolean = sp(c).getBoolean("look_btiles", true)
    fun setBorderTiles(c: Context, v: Boolean) = sp(c).edit().putBoolean("look_btiles", v).apply()
    // Own thickness + colour for each border ("panel", "tiles", "widget"); until one is set it follows the shared values above.
    fun borderWidthOf(c: Context, kind: String): Int =
        sp(c).getInt("look_bw_$kind", -1).let { if (it >= 0) it else borderWidth(c) }
    fun setBorderWidthOf(c: Context, kind: String, v: Int) = sp(c).edit().putInt("look_bw_$kind", v).apply()
    fun borderColorOf(c: Context, kind: String): Int =
        if (sp(c).contains("look_bc_$kind")) sp(c).getInt("look_bc_$kind", 0) else borderColor(c)
    fun setBorderColorOf(c: Context, kind: String, v: Int) = sp(c).edit().putInt("look_bc_$kind", v).apply()
    fun clearBorderParts(c: Context) {
        val e = sp(c).edit()
        for (k in listOf("panel", "tiles", "widget")) e.remove("look_bw_$k").remove("look_bc_$k")
        e.apply()
    }
    fun borderWidget(c: Context): Boolean = sp(c).getBoolean("look_bwidget", false)
    fun setBorderWidget(c: Context, v: Boolean) = sp(c).edit().putBoolean("look_bwidget", v).apply()

    // ---- Panel text sizes per part (percent, on top of the overall text size) ----
    val PANEL_GROUPS = listOf(
        "head" to "Header title (Inbox, Back)",
        "hdr" to "+ Note button",
        "clear" to "Clear all button",
        "gear" to "Settings icon",
        "title" to "Message titles",
        "text" to "Message text and notes",
        "small" to "Small labels (app name, counts)",
        "buttons" to "Buttons and reply box"
    )
    // Separate text colour per part (0 = automatic / the overall text colour)
    val WIDGET_PARTS = listOf(
        "title" to "Widget: Inbox title",
        "mic" to "Widget: mic icon",
        "note" to "Widget: + Note",
        "clear" to "Widget: Clear all",
        "gear" to "Widget: settings icon",
        "refresh" to "Widget: refresh icon",
        "edit" to "Widget: pencil (arrange) button",
        "clock" to "Widget: clock",
        "weather" to "Widget: weather",
        "date" to "Widget: date",
        "where" to "Widget: Where to?",
        "empty" to "Widget: Nothing new text",
        "updated" to "Widget: Updated line (weather)",
        "dialletter" to "Widget: fast-dial letters (inside circles)",
        "dialnames" to "Widget: fast-dial names (under circles)",
        "tapp" to "Widget tiles: app name",
        "ttitle" to "Widget tiles: message title",
        "ttext" to "Widget tiles: message text",
        "pages" to "Widget: page buttons"
    )
    // Wording of the widget's empty message and of the weather "Updated" line (blank = original wording)
    const val EMPTY_DEFAULT = "Nothing new from your apps"
    const val UPDATED_DEFAULT = "Updated"
    fun emptyText(c: Context): String = (sp(c).getString("look_empty_text", "") ?: "").ifBlank { EMPTY_DEFAULT }
    fun setEmptyText(c: Context, v: String) = sp(c).edit().putString("look_empty_text", v.trim()).apply()
    // Show or hide the widget's "Nothing new" message (the refresh button stays)
    fun showEmptyText(c: Context): Boolean = sp(c).getBoolean("look_empty_show", true)
    fun setShowEmptyText(c: Context, v: Boolean) = sp(c).edit().putBoolean("look_empty_show", v).apply()
    fun updatedLabel(c: Context): String = (sp(c).getString("look_updated_text", "") ?: "").ifBlank { UPDATED_DEFAULT }
    fun setUpdatedLabel(c: Context, v: String) = sp(c).edit().putString("look_updated_text", v.trim()).apply()
    /** Size of the weather "Updated" line, percent (100 = as designed). */
    fun updSize(c: Context): Int = sp(c).getInt("look_upd_size", 100).coerceIn(50, 250)
    fun setUpdSize(c: Context, v: Int) = sp(c).edit().putInt("look_upd_size", v).apply()
    fun partColor(c: Context, k: String): Int = sp(c).getInt("look_pc_$k", 0)
    fun setPartColor(c: Context, k: String, v: Int) = sp(c).edit().putInt("look_pc_$k", v).apply()
    fun grpColor(c: Context, g: String): Int = sp(c).getInt("look_gc_$g", 0)
    fun setGrpColor(c: Context, g: String, v: Int) = sp(c).edit().putInt("look_gc_$g", v).apply()
    fun grpSize(c: Context, g: String): Int = sp(c).getInt("look_grp_$g", 100)
    fun setGrpSize(c: Context, g: String, v: Int) = sp(c).edit().putInt("look_grp_$g", v).apply()

    // ---- Fast dial: ask before calling ----
    fun dialAsk(c: Context): Boolean = sp(c).getBoolean("dial_ask", false)
    fun setDialAsk(c: Context, v: Boolean) = sp(c).edit().putBoolean("dial_ask", v).apply()

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
    // Each fast-dial circle can have its own background and text colour (0 = automatic)
    fun dialCircleOf(c: Context, i: Int): Int = sp(c).getInt("look_dialc_$i", 0)
    fun setDialCircleOf(c: Context, i: Int, v: Int) = sp(c).edit().putInt("look_dialc_$i", v).apply()
    fun dialTextOf(c: Context, i: Int): Int = sp(c).getInt("look_dialt_$i", 0)
    fun setDialTextOf(c: Context, i: Int, v: Int) = sp(c).edit().putInt("look_dialt_$i", v).apply()
    fun dialCircle(c: Context): Int = sp(c).getInt("look_dialcircle", 0)
    fun setDialCircle(c: Context, v: Int) = sp(c).edit().putInt("look_dialcircle", v).apply()
    fun squareShape(c: Context): Boolean = sp(c).getBoolean("look_square", false)
    // Shape of the clock / weather / date / fast-dial shapes: index into Look.SHAPES (old Round / Rounded square choice is kept)
    fun shape(c: Context): Int {
        val s = sp(c)
        return if (s.contains("look_shape")) s.getInt("look_shape", 0).coerceIn(0, Look.SHAPES.size - 1)
        else if (squareShape(c)) 1 else 0
    }
    fun setShape(c: Context, v: Int) = sp(c).edit().putInt("look_shape", v).putBoolean("look_square", v == 1).apply()
    // Colour of the icons and the Clear all button (mic, settings, refresh), set by the themes (0 = automatic)
    fun iconColor(c: Context): Int = sp(c).getInt("look_icon", 0)
    fun setIconColor(c: Context, v: Int) = sp(c).edit().putInt("look_icon", v).apply()
    fun setSquareShape(c: Context, v: Boolean) = sp(c).edit().putBoolean("look_square", v).apply()
    fun gearSize(c: Context): Int = sp(c).getInt("look_szgear", 22)
    fun setGearSize(c: Context, v: Int) = sp(c).edit().putInt("look_szgear", v).apply()
    fun micSize(c: Context): Int = sp(c).getInt("look_szmic", 22)
    fun setMicSize(c: Context, v: Int) = sp(c).edit().putInt("look_szmic", v).apply()
    fun refreshShow(c: Context): Boolean = sp(c).getBoolean("look_refresh", true)
    fun setRefreshShow(c: Context, v: Boolean) = sp(c).edit().putBoolean("look_refresh", v).apply()
    fun refreshSize(c: Context): Int = sp(c).getInt("look_szrefresh", 16)
    fun setRefreshSize(c: Context, v: Int) = sp(c).edit().putInt("look_szrefresh", v).apply()
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

    // ---- Time stamp in front of each message line: 0 = smart (time today, Yesterday, date), 1 = date + time, 2 = time only, 3 = none
    fun stampStyle(c: Context): Int = sp(c).getInt("msg_stamp", 0).coerceIn(0, 3)
    fun setStampStyle(c: Context, v: Int) = sp(c).edit().putInt("msg_stamp", v).apply()

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
    /** Just opening a message card and closing it again counts as read (off = it stays until you act on it). */
    fun countLeave(c: Context): Boolean = sp(c).getBoolean("cnt_leave", false)
    fun setCountLeave(c: Context, v: Boolean) = sp(c).edit().putBoolean("cnt_leave", v).apply()
    fun countReply(c: Context): Boolean = sp(c).getBoolean("cnt_reply", true)
    fun setCountReply(c: Context, v: Boolean) = sp(c).edit().putBoolean("cnt_reply", v).apply()

    /** Also remove the notification from the phone's notification shade once it is handled here. */
    fun dismissHandled(c: Context): Boolean = sp(c).getBoolean("cnt_dismiss", true)
    fun setDismissHandled(c: Context, v: Boolean) = sp(c).edit().putBoolean("cnt_dismiss", v).apply()

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

    // ---- Snooze and quiet hours (minutes of the day) ----
    fun snoozeUntil(c: Context): Long = sp(c).getLong("sn_until", 0L)
    fun setSnoozeUntil(c: Context, v: Long) = sp(c).edit().putLong("sn_until", v).apply()
    fun quietOn(c: Context): Boolean = sp(c).getBoolean("sn_quiet", false)
    fun setQuietOn(c: Context, v: Boolean) = sp(c).edit().putBoolean("sn_quiet", v).apply()
    fun quietStart(c: Context): Int = sp(c).getInt("sn_qstart", 22 * 60)
    fun setQuietStart(c: Context, v: Int) = sp(c).edit().putInt("sn_qstart", v).apply()
    fun quietEnd(c: Context): Int = sp(c).getInt("sn_qend", 7 * 60)
    fun setQuietEnd(c: Context, v: Int) = sp(c).edit().putInt("sn_qend", v).apply()

    // ---- What the Clear all button does: 0 = clear the notifications, 1 = only mark them read ----
    fun clearMode(c: Context): Int = sp(c).getInt("look_clearmode", 0)
    fun setClearMode(c: Context, v: Int) = sp(c).edit().putInt("look_clearmode", v).apply()

    // ---- Per app: pinned to the top, muted, own tile colour (0 = automatic) ----
    fun pinned(c: Context): Set<String> = HashSet(sp(c).getStringSet("app_pinned", emptySet()) ?: emptySet())
    fun setPinned(c: Context, pkg: String, on: Boolean) {
        val s = HashSet(pinned(c))
        if (on) s.add(pkg) else s.remove(pkg)
        sp(c).edit().putStringSet("app_pinned", s).apply()
    }
    fun muted(c: Context): Set<String> = HashSet(sp(c).getStringSet("app_muted", emptySet()) ?: emptySet())
    fun setMuted(c: Context, pkg: String, on: Boolean) {
        val s = HashSet(muted(c))
        if (on) s.add(pkg) else s.remove(pkg)
        sp(c).edit().putStringSet("app_muted", s).apply()
    }
    fun appColor(c: Context, pkg: String): Int = sp(c).getInt("look_appc_$pkg", 0)
    fun setAppColor(c: Context, pkg: String, v: Int) = sp(c).edit().putInt("look_appc_$pkg", v).apply()

    // ---- Automatic day / night theme ----
    fun autoTheme(c: Context): Boolean = sp(c).getBoolean("look_auto", false)
    fun setAutoTheme(c: Context, v: Boolean) = sp(c).edit().putBoolean("look_auto", v).apply()
    fun autoDayTheme(c: Context): Int = sp(c).getInt("look_auto_day", 6)
    fun setAutoDayTheme(c: Context, v: Int) = sp(c).edit().putInt("look_auto_day", v).apply()
    fun autoNightTheme(c: Context): Int = sp(c).getInt("look_auto_night", 1)
    fun setAutoNightTheme(c: Context, v: Int) = sp(c).edit().putInt("look_auto_night", v).apply()
    fun autoDayStart(c: Context): Int = sp(c).getInt("look_auto_ds", 7 * 60)
    fun setAutoDayStart(c: Context, v: Int) = sp(c).edit().putInt("look_auto_ds", v).apply()
    fun autoNightStart(c: Context): Int = sp(c).getInt("look_auto_ns", 19 * 60)
    fun setAutoNightStart(c: Context, v: Int) = sp(c).edit().putInt("look_auto_ns", v).apply()
    /** Which phase (1 = day, 0 = night) was applied last, -1 = none yet. */
    fun autoLast(c: Context): Int = sp(c).getInt("look_auto_last", -1)
    fun setAutoLast(c: Context, v: Int) = sp(c).edit().putInt("look_auto_last", v).apply()

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

    // ---- Show / hide any item (default: shown) ----
    val SHOW_ITEMS = listOf(
        "w_title" to "Widget: Inbox title",
        "w_count" to "Widget: message count badge",
        "w_mic" to "Widget: mic icon",
        "w_note" to "Widget: + Note",
        "w_clear" to "Widget: Clear all / Read all",
        "w_gear" to "Widget: settings icon",
        "t_app" to "Widget tiles: app name",
        "t_cnt" to "Widget tiles: count badge",
        "t_title" to "Widget tiles: message title",
        "t_text" to "Widget tiles: message text",
        "p_title" to "Panel: Inbox title",
        "p_badge" to "Panel: message count badge",
        "p_note" to "Panel: + Note",
        "p_clear" to "Panel: Clear all / Read all",
        "p_icon" to "Panel tiles: app icon",
        "p_cnt" to "Panel tiles: count badge",
        "p_title_t" to "Panel tiles: message title",
        "p_text" to "Panel tiles: message text"
    )
    fun showItem(c: Context, k: String): Boolean = sp(c).getBoolean("lay_item_$k", true)
    fun setShowItem(c: Context, k: String, v: Boolean) = sp(c).edit().putBoolean("lay_item_$k", v).apply()

    // ---- Font format: letter case of the widget text, and 3D icons ----
    /** 0 = as typed, 1 = UPPERCASE, 2 = lowercase, 3 = Each Word Capital (widget text only) */
    fun textCase(c: Context): Int = sp(c).getInt("look_case", 0).coerceIn(0, 3)
    fun setTextCase(c: Context, v: Int) = sp(c).edit().putInt("look_case", v).apply()
    fun icons3d(c: Context): Boolean = sp(c).getBoolean("look_3d", false)
    fun setIcons3d(c: Context, v: Boolean) = sp(c).edit().putBoolean("look_3d", v).apply()

    // ---- Scrolling, pages, grouping and order of the messages ----
    /** 0 = scroll up / down (as before), 1 = pages (widget: ‹ › buttons; panel: swipe left / right or ‹ ›) */
    fun scrollMode(c: Context): Int = sp(c).getInt("scr_mode", 0).coerceIn(0, 2)
    /** True for both Pages and My pages (the page bar and swiping are on). */
    fun pagesOn(c: Context): Boolean = scrollMode(c) >= 1
    /** My pages: pages you add yourself, each with its own apps and shortcuts. Stored as one JSON text. */
    fun customPages(c: Context): List<CPage> {
        val s = sp(c).getString("pg_custom", "") ?: ""
        if (s.isBlank()) return emptyList()
        return try {
            val a = org.json.JSONArray(s)
            fun strs(j: org.json.JSONArray?): List<String> = if (j == null) emptyList() else (0 until j.length()).map { j.getString(it) }
            (0 until a.length()).map {
                val o = a.getJSONObject(it)
                CPage(o.optString("n", "Page"), strs(o.optJSONArray("a")), strs(o.optJSONArray("s")))
            }
        } catch (e: Exception) { emptyList() }
    }
    fun setCustomPages(c: Context, l: List<CPage>) {
        val a = org.json.JSONArray()
        for (p in l) a.put(org.json.JSONObject().put("n", p.name).put("a", org.json.JSONArray(p.apps)).put("s", org.json.JSONArray(p.shortcuts)))
        sp(c).edit().putString("pg_custom", a.toString()).apply()
    }
    fun setScrollMode(c: Context, v: Int) = sp(c).edit().putInt("scr_mode", v).apply()
    /** Tiles per page. */
    fun pageSize(c: Context): Int = sp(c).getInt("scr_size", 6).coerceIn(2, 20)
    fun setPageSize(c: Context, v: Int) = sp(c).edit().putInt("scr_size", v).apply()
    /** Current page (0 = first) of "w" (widget) or "p" (panel). */
    fun page(c: Context, key: String): Int = sp(c).getInt("scr_page_$key", 0).coerceAtLeast(0)
    fun setPage(c: Context, key: String, v: Int) = sp(c).edit().putInt("scr_page_$key", v).apply()
    /** Endless: pages wrap around, and the widget list repeats when you reach its end. */
    fun loop(c: Context): Boolean = sp(c).getBoolean("scr_loop", false)
    fun setLoop(c: Context, v: Boolean) = sp(c).edit().putBoolean("scr_loop", v).apply()
    /** 0 = one tile per chat (as before), 1 = one tile per app (all its chats together) */
    fun groupMode(c: Context): Int = sp(c).getInt("scr_group", 0).coerceIn(0, 1)
    fun setGroupMode(c: Context, v: Int) = sp(c).edit().putInt("scr_group", v).apply()
    /** 0 = newest first (as before), 1 = oldest first, 2 = my own app order, 3 = app name A-Z */
    fun sortMode(c: Context): Int = sp(c).getInt("scr_sort", 0).coerceIn(0, 3)
    fun setSortMode(c: Context, v: Int) = sp(c).edit().putInt("scr_sort", v).apply()
    fun appOrder(c: Context): List<String> =
        (sp(c).getString("scr_order", "") ?: "").split(",").filter { it.isNotBlank() }
    fun setAppOrder(c: Context, l: List<String>) = sp(c).edit().putString("scr_order", l.joinToString(",")).apply()
}
