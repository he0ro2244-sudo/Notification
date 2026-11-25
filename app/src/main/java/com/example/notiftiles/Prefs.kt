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
    /** The settings file itself, for listeners (see ChangeTracker). */
    fun store(c: Context) = sp(c)

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
    fun setQuickOn(c: Context, v: Boolean) { sp(c).edit().putBoolean("quick_on", v).apply(); markTidy(c, v) }
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
    // ---- Shortcuts that belong to one app (shown on that app's opened card in the panel) ----
    fun appShortcuts(c: Context, pkg: String): List<Quick> {
        val t = sp(c).getString("appsc_$pkg", null) ?: return emptyList()
        return try {
            val a = org.json.JSONArray(t)
            (0 until a.length()).map { val o = a.getJSONObject(it); Quick(o.optString("t"), o.optString("n"), o.optString("p"), o.optString("g")) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    private fun putAppShortcuts(c: Context, pkg: String, l: List<Quick>) {
        val a = org.json.JSONArray()
        for (q in l) a.put(org.json.JSONObject().put("t", q.type).put("n", q.name).put("p", q.pkg).put("g", q.target))
        if (l.isEmpty()) sp(c).edit().remove("appsc_$pkg").apply() else sp(c).edit().putString("appsc_$pkg", a.toString()).apply()
    }
    fun addAppShortcut(c: Context, pkg: String, q: Quick) = putAppShortcuts(c, pkg, appShortcuts(c, pkg) + q)
    fun removeAppShortcut(c: Context, pkg: String, i: Int) =
        putAppShortcuts(c, pkg, appShortcuts(c, pkg).filterIndexed { k, _ -> k != i })

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

    /** Everything that changes how the widget looks and where things sit goes back to the standard widget: themes, colours, fonts, shapes, sizes (Size / Width / Height),
     *  switches, order, merge, free places. Your apps, contacts, shortcuts, notes, weather place and Where to? favourites are kept.
     *  [scope] says whose undo history gets the "before" copy: the pencil's or Settings' (each has its own). */
    fun resetAllLook(c: Context, scope: String) {
        val before = fullJson(c)
        resetLook(c)
        val e = sp(c).edit()
        e.remove("free_on").remove("free_pos").remove("free_ref_w").remove("free_grid_top").remove("free_snap")
        e.remove("free_prev_has").remove("free_prev_on").remove("free_prev_pos").remove("free_prev_ref")
        // The standard widget: top row (clock, weather, date, Where to?), fast-dial circles, shortcuts row, Inbox row
        // (title, mic, + Note, Clear all, pencil, gear), refresh icon and weather line. Pencil stays on so Reset can be reached.
        e.putBoolean("quick_on", true).putBoolean("lay_editbtn", true)
        e.apply()
        // A reset that changed nothing (already standard) is not a step, so it can never push the real "before" out of the history
        if (fullJson(c) != before) pushSnapshot(c, scope, before, "Reset everything to standard")
    }

    // ---- Undo and saved layouts: a copy of every look / layout setting (custom background pictures are not part of it) ----
    const val SCOPE_PENCIL = "pencil"
    const val SCOPE_SETTINGS = "settings"
    private const val HIST_MAX = 25

    /** Keys that make up "the look": tracked for undo, copied into saved layouts. The one-step Move undo copy is not part of it. */
    fun isLookKey(k: String) = (k.startsWith("look_") || k.startsWith("lay_") || k.startsWith("free_") || k == "quick_on") && !k.startsWith("free_prev_")

    /** The same settings always give the same text (keys sorted), so two copies can be compared. */
    fun snapshotJson(c: Context): String {
        val o = org.json.JSONObject()
        for (ent in sp(c).all.entries.sortedBy { it.key }) {
            val k = ent.key
            if (!isLookKey(k)) continue
            val v = ent.value
            val e = org.json.JSONObject()
            when (v) {
                is Boolean -> e.put("t", "b").put("v", v)
                is Int -> e.put("t", "i").put("v", v)
                is Long -> e.put("t", "l").put("v", v)
                is Float -> e.put("t", "f").put("v", v.toDouble())
                is String -> e.put("t", "s").put("v", v)
                else -> continue
            }
            o.put(k, e)
        }
        return o.toString()
    }

    private fun restoreJson(c: Context, json: String): Boolean = try {
        val o = org.json.JSONObject(json)
        val e = sp(c).edit()
        for (k in sp(c).all.keys) if (isLookKey(k)) e.remove(k)
        e.remove("free_prev_has") // the one-step Move undo would point at places that no longer exist
        val iter = o.keys()
        while (iter.hasNext()) {
            val k = iter.next()
            if (!isLookKey(k)) continue
            val x = o.getJSONObject(k)
            when (x.getString("t")) {
                "b" -> e.putBoolean(k, x.getBoolean("v"))
                "i" -> e.putInt(k, x.getInt("v"))
                "l" -> e.putLong(k, x.getLong("v"))
                "f" -> e.putFloat(k, x.getDouble("v").toFloat())
                "s" -> e.putString(k, x.getString("v"))
            }
        }
        e.apply()
        true
    } catch (ex: Exception) {
        false
    }

    // ---- Full copy for undo: every setting the person can change (look, layout, contacts, shortcuts, notes, filters, weather place, ...).
    // Left out: what the app fills in by itself (weather readings, page counters, snooze timers, recent searches), the undo lists and saved layouts.
    private val NOT_TRACKED = setOf(
        "w_try", "w_has", "w_temp", "w_code", "w_day", "w_time", "w_appcode", "w_apptemp", "w_apptime", "w_upd_note", "w_upd_src", "w_upd_time",
        "w_gaptime", "weather_code", "handled", "chat_snz", "sn_until", "seen", "battery_asked", "undo_look", "loc_seen", "loc_since",
        "dest_recent", "ui_preview", "log"
    )

    fun isTrackedKey(k: String): Boolean =
        k !in NOT_TRACKED && !k.startsWith("hist_") && !k.startsWith("h2_") && !k.startsWith("r2_") && !k.startsWith("preset_") && !k.startsWith("scr_page_") && !k.startsWith("free_prev_")

    fun fullJson(c: Context): String {
        val o = org.json.JSONObject()
        for (ent in sp(c).all.entries.sortedBy { it.key }) {
            val k = ent.key
            if (!isTrackedKey(k)) continue
            val v = ent.value
            val e = org.json.JSONObject()
            when (v) {
                is Boolean -> e.put("t", "b").put("v", v)
                is Int -> e.put("t", "i").put("v", v)
                is Long -> e.put("t", "l").put("v", v)
                is Float -> e.put("t", "f").put("v", v.toDouble())
                is String -> e.put("t", "s").put("v", v)
                is Set<*> -> e.put("t", "ss").put("v", org.json.JSONArray(v.map { it.toString() }.sorted()))
                else -> continue
            }
            o.put(k, e)
        }
        return o.toString()
    }

    private fun restoreFull(c: Context, json: String): Boolean = try {
        val o = org.json.JSONObject(json)
        val e = sp(c).edit()
        for (k in sp(c).all.keys) if (isTrackedKey(k)) e.remove(k)
        e.remove("free_prev_has")
        val iter = o.keys()
        while (iter.hasNext()) {
            val k = iter.next()
            if (!isTrackedKey(k)) continue
            val x = o.getJSONObject(k)
            when (x.getString("t")) {
                "b" -> e.putBoolean(k, x.getBoolean("v"))
                "i" -> e.putInt(k, x.getInt("v"))
                "l" -> e.putLong(k, x.getLong("v"))
                "f" -> e.putFloat(k, x.getDouble("v").toFloat())
                "s" -> e.putString(k, x.getString("v"))
                "ss" -> {
                    val a = x.getJSONArray("v")
                    val set = HashSet<String>()
                    for (i in 0 until a.length()) set.add(a.getString(i))
                    e.putStringSet(k, set)
                }
            }
        }
        e.apply()
        true
    } catch (ex: Exception) {
        false
    }

    // ---- Undo / redo history. One list per scope (the pencil screens and Settings each keep their own), newest last.
    // Every step keeps: when, a short description of what changed, and a full copy of the settings (s).
    // In the undo list s = how things were BEFORE the step. In the redo list s = how things were AFTER the step.
    class HistEntry(val t: Long, val l: String, val s: String)

    private fun listGet(c: Context, key: String): MutableList<HistEntry> {
        val raw = sp(c).getString(key, "") ?: ""
        if (raw.isEmpty()) return ArrayList()
        return try {
            val a = org.json.JSONArray(raw)
            val l = ArrayList<HistEntry>()
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                l.add(HistEntry(o.getLong("t"), o.getString("l"), o.getString("s")))
            }
            l
        } catch (e: Exception) {
            ArrayList()
        }
    }

    private fun listPut(c: Context, key: String, l: List<HistEntry>) {
        if (l.isEmpty()) {
            sp(c).edit().remove(key).apply()
        } else {
            val a = org.json.JSONArray()
            for (x in l) a.put(org.json.JSONObject().put("t", x.t).put("l", x.l).put("s", x.s))
            sp(c).edit().putString(key, a.toString()).apply()
        }
    }

    private fun uKey(scope: String) = "h2_$scope"
    private fun rKey(scope: String) = "r2_$scope"

    /** Adds a step (a new change always clears the redo list). [before] is the copy from just before the change. */
    fun pushSnapshot(c: Context, scope: String, before: String, label: String) {
        val l = listGet(c, uKey(scope))
        if (l.isNotEmpty() && l[l.size - 1].s == before) return
        l.add(HistEntry(System.currentTimeMillis(), label, before))
        while (l.size > HIST_MAX) l.removeAt(0)
        listPut(c, uKey(scope), l)
        listPut(c, rKey(scope), emptyList())
    }

    fun undoSteps(c: Context, scope: String): Int = listGet(c, uKey(scope)).size
    fun redoSteps(c: Context, scope: String): Int = listGet(c, rKey(scope)).size
    fun hasUndo(c: Context, scope: String): Boolean = undoSteps(c, scope) > 0

    /** Newest first. */
    fun undoList(c: Context, scope: String): List<HistEntry> = listGet(c, uKey(scope)).reversed()

    /** Next one to redo first. */
    fun redoList(c: Context, scope: String): List<HistEntry> = listGet(c, rKey(scope)).reversed()

    /** Takes back the last [n] steps (default 1). The steps move to the redo list. */
    fun undoLook(c: Context, scope: String, n: Int = 1): Boolean {
        val l = listGet(c, uKey(scope))
        if (l.isEmpty() || n < 1) return false
        val count = minOf(n, l.size)
        val idx = l.size - count
        val current = fullJson(c)
        val redo = listGet(c, rKey(scope))
        // state after step j = the "before" copy of step j+1 (the newest step: how things are now)
        for (j in l.size - 1 downTo idx) {
            val after = if (j == l.size - 1) current else l[j + 1].s
            redo.add(HistEntry(l[j].t, l[j].l, after))
        }
        if (!restoreFull(c, l[idx].s)) return false
        listPut(c, uKey(scope), l.subList(0, idx))
        listPut(c, rKey(scope), redo)
        return true
    }

    /** Puts back the next [n] steps that were taken back. */
    fun redoLook(c: Context, scope: String, n: Int = 1): Boolean {
        val redo = listGet(c, rKey(scope))
        if (redo.isEmpty() || n < 1) return false
        val count = minOf(n, redo.size)
        val hist = listGet(c, uKey(scope))
        var cur = fullJson(c)
        var last = ""
        for (k in 0 until count) {
            val e = redo.removeAt(redo.size - 1)
            hist.add(HistEntry(e.t, e.l, cur))
            cur = e.s
            last = e.s
        }
        if (!restoreFull(c, last)) return false
        while (hist.size > HIST_MAX) hist.removeAt(0)
        listPut(c, uKey(scope), hist)
        listPut(c, rKey(scope), redo)
        return true
    }

    /** A short description of what differs between two copies, e.g. "Look: font size, text color". */
    fun describe(before: String, after: String): String {
        return try {
            val b = org.json.JSONObject(before)
            val a = org.json.JSONObject(after)
            val keys = java.util.TreeSet<String>()
            val ib = b.keys(); while (ib.hasNext()) keys.add(ib.next())
            val ia = a.keys(); while (ia.hasNext()) keys.add(ia.next())
            val changed = keys.filter { b.optJSONObject(it)?.toString() != a.optJSONObject(it)?.toString() }
            if (changed.isEmpty()) return "Change"
            fun group(k: String): String = when (k.substringBefore('_')) {
                "look" -> "Look"
                "lay" -> "Layout"
                "free" -> "Moved places"
                "quick" -> "Shortcuts"
                "dial" -> "Fast dial"
                "w" -> "Weather"
                "scr" -> "Pages & screen"
                "cnt" -> "Counting"
                "sn" -> "Snooze"
                "msg" -> "Messages"
                "note", "notes" -> "Notes"
                else -> "Settings"
            }
            fun pretty(k: String): String = (if (k.contains('_')) k.substringAfter('_') else k).replace('_', ' ')
            val groups = changed.map { group(it) }.distinct()
            if (groups.size == 1) {
                val names = changed.map { pretty(it) }
                val shown = names.take(3).joinToString(", ")
                groups[0] + ": " + shown + (if (names.size > 3) " (+" + (names.size - 3) + " more)" else "")
            } else {
                groups.joinToString(", ") + " (" + changed.size + " settings)"
            }
        } catch (e: Exception) {
            "Change"
        }
    }

    fun presetNames(c: Context): List<String> =
        sp(c).all.keys.filter { it.startsWith("preset_") }.map { it.removePrefix("preset_") }.sortedBy { it.lowercase() }
    fun savePreset(c: Context, name: String): Boolean {
        val n = name.trim().take(30)
        if (n.isEmpty()) return false
        sp(c).edit().putString("preset_$n", snapshotJson(c)).apply()
        return true
    }
    fun loadPreset(c: Context, name: String, scope: String): Boolean {
        val j = sp(c).getString("preset_$name", "") ?: ""
        if (j.isEmpty()) return false
        val before = fullJson(c)
        val ok = restoreJson(c, j)
        if (ok && fullJson(c) != before) pushSnapshot(c, scope, before, "Loaded layout " + name)
        return ok
    }
    fun deletePreset(c: Context, name: String) = sp(c).edit().remove("preset_$name").apply()

    // ---- Accent colour (buttons, badge) ----
    fun accent(c: Context): Int = sp(c).getInt("look_accent", 0xFF3B82F6.toInt())
    fun setAccent(c: Context, v: Int) = sp(c).edit().putInt("look_accent", v).apply()

    // ---- Widget arrangement ----
    val ITEMS = listOf("clock", "weather", "date")

    /** Items that can be moved. Each one sits in exactly one row: the top row ("top") or the Inbox row ("head"). */
    val TOP_ITEMS = listOf("clock", "weather", "date", "maps")
    val HEAD_ITEMS = listOf("title", "count", "mic", "note", "clear", "edit", "gear")
    val ALL_ITEMS = TOP_ITEMS + HEAD_ITEMS

    private fun rowPref(c: Context, key: String): MutableList<String> =
        (sp(c).getString(key, "") ?: "").split(",").filter { it in ALL_ITEMS }.distinct().toMutableList()

    /** Before items could be moved: the three circles in their order, the Where to? box at its place. */
    private fun legacyTop(c: Context): MutableList<String> {
        val s = sp(c)
        val o = (s.getString("lay_order", "clock,weather,date") ?: "").split(",").filter { it in ITEMS }.distinct()
        val l = (if (o.size == 3) o else ITEMS).toMutableList()
        val pos = (if (s.contains("lay_pill_pos")) s.getInt("lay_pill_pos", 3) else if (pillFirst(c)) 0 else 3).coerceIn(0, 3)
        l.add(pos.coerceAtMost(l.size), "maps")
        return l
    }

    /** Which items sit in the top row ("top") and in the Inbox row ("head"), in order. */
    fun rows(c: Context): Map<String, List<String>> {
        val s = sp(c)
        val moved = s.contains("lay_row_top") || s.contains("lay_row_head")
        val top = if (moved) rowPref(c, "lay_row_top") else legacyTop(c)
        val head = (if (moved) rowPref(c, "lay_row_head") else HEAD_ITEMS.toMutableList()).filter { it !in top }.toMutableList()
        // An item that is in neither row goes back to its usual row
        for (k in ALL_ITEMS) if (k !in top && k !in head) { if (k in TOP_ITEMS) top.add(k) else head.add(k) }
        return mapOf("top" to top, "head" to head)
    }

    /** Saves which items sit in each row. */
    fun setRows(c: Context, top: List<String>, head: List<String>) {
        val t = top.filter { it in ALL_ITEMS }.distinct()
        val h = head.filter { it in ALL_ITEMS && it !in t }.distinct()
        sp(c).edit().putString("lay_row_top", t.joinToString(",")).putString("lay_row_head", h.joinToString(",")).apply()
    }

    /** The circles of the top row in their order (clock, weather, date, as far as they sit in the top row). */
    fun order(c: Context): List<String> = rows(c)["top"]!!.filter { it in ITEMS }

    fun setOrder(c: Context, l: List<String>) {
        val r = rows(c)
        val picks = l.filter { it in ITEMS && it in r["top"]!! }.toMutableList()
        val top = r["top"]!!.map { if (it in ITEMS && picks.isNotEmpty()) picks.removeAt(0) else it }
        setRows(c, top, r["head"]!!)
    }

    /** key: clock, weather, date, maps, dial */
    fun show(c: Context, key: String): Boolean = sp(c).getBoolean("lay_show_$key", true)
    fun setShow(c: Context, key: String, v: Boolean) { sp(c).edit().putBoolean("lay_show_$key", v).apply(); markTidy(c, v) }

    /** Top-row items merged into ONE shape (any of clock, weather, date; fewer than 2 = nothing merged). */
    fun merged(c: Context): Set<String> =
        (sp(c).getString("lay_merge", "") ?: "").split(",").filter { it in ITEMS }.toSet()
    fun setMerged(c: Context, s: Set<String>) = sp(c).edit().putString("lay_merge", s.joinToString(",")).apply()

    /** Merged shape: false = parts one under the other (default), true = side by side on one line. */
    fun mergeHoriz(c: Context): Boolean = sp(c).getBoolean("lay_merge_h", false)
    fun setMergeHoriz(c: Context, v: Boolean) = sp(c).edit().putBoolean("lay_merge_h", v).apply()

    /** Extra space around each part of the merged shape, in dp (a bigger area to tap for each app). 0 = as before. */
    fun mergeGap(c: Context): Int = sp(c).getInt("lay_merge_gap", 0).coerceIn(0, 30)
    fun setMergeGap(c: Context, v: Int) = sp(c).edit().putInt("lay_merge_gap", v).apply()

    fun pillFirst(c: Context): Boolean = sp(c).getBoolean("lay_pill_first", false)
    fun setPillFirst(c: Context, v: Boolean) = sp(c).edit().putBoolean("lay_pill_first", v).apply()

    /** Where the "Where to?" box sits in the top row: 0 = before the 1st item ... 3 = after the 3rd (3 when it is not in the top row). */
    fun pillPos(c: Context): Int = rows(c)["top"]!!.indexOf("maps").let { if (it < 0) 3 else it }

    fun setPillPos(c: Context, v: Int) {
        val r = rows(c)
        val top = r["top"]!!.filter { it != "maps" }.toMutableList()
        top.add(v.coerceIn(0, top.size), "maps")
        setRows(c, top, r["head"]!!.filter { it != "maps" })
    }

    /** Whether an item is shown: the switch for it in Arrange & resize. */
    fun itemOn(c: Context, key: String): Boolean = when (key) {
        "title", "count", "mic", "note", "clear", "gear" -> showItem(c, "w_$key")
        "edit" -> editBtn(c)
        else -> show(c, key)
    }

    fun setItemOn(c: Context, key: String, v: Boolean) = when (key) {
        "title", "count", "mic", "note", "clear", "gear" -> setShowItem(c, "w_$key", v)
        "edit" -> setEditBtn(c, v)
        else -> setShow(c, key, v)
    }

    /** The sections of the widget above the tiles, top to bottom: top = clock/weather/date/Where to?, dial, quick = shortcuts row, head = Inbox row. */
    val BLOCKS = listOf("top", "dial", "quick", "head")
    val ITEM_KEYS = listOf("w_title", "w_note", "w_clear", "w_mic", "w_gear", "w_edit")
    fun blockOrder(c: Context): List<String> {
        val l = (sp(c).getString("lay_blocks", "top,dial,quick,head") ?: "").split(",").filter { it in BLOCKS }.distinct()
        return if (l.size == BLOCKS.size) l else BLOCKS
    }
    fun setBlockOrder(c: Context, l: List<String>) = sp(c).edit().putString("lay_blocks", l.joinToString(",")).apply()

    /** Size of one single item (w_title, w_note, w_clear, w_mic, w_gear) in percent, on top of its section's size. */
    /** "Keep every item of this row the same size": while on, changing the size / width / height of one item sets all items of the row. */
    fun matchRow(c: Context, group: String): Boolean = sp(c).getBoolean("lay_match_$group", false)
    fun setMatchRow(c: Context, group: String, v: Boolean) = sp(c).edit().putBoolean("lay_match_$group", v).apply()
    fun itemScale(c: Context, key: String): Int = sp(c).getInt("lay_iscale_$key", 100).coerceIn(50, 250)
    fun setItemScale(c: Context, key: String, v: Int) = sp(c).edit().putInt("lay_iscale_$key", v.coerceIn(50, 250)).apply()

    /** Extra width / height of one box item in percent (100 = the Size slider alone), on top of its Size. */
    fun itemW(c: Context, key: String): Int = sp(c).getInt("lay_wscale_$key", 100).coerceIn(50, 250)
    fun setItemW(c: Context, key: String, v: Int) = sp(c).edit().putInt("lay_wscale_$key", v.coerceIn(50, 250)).apply()
    fun itemH(c: Context, key: String): Int = sp(c).getInt("lay_hscale_$key", 100).coerceIn(50, 250)
    fun setItemH(c: Context, key: String, v: Int) = sp(c).edit().putInt("lay_hscale_$key", v.coerceIn(50, 250)).apply()

    /** Width of the Where to? box in the normal (rows) layout, in dp. 0 = it fills the rest of its row (as before). */
    fun whereWidth(c: Context): Int = sp(c).getInt("lay_where_w", 0).coerceIn(0, 400)
    fun setWhereWidth(c: Context, v: Int) = sp(c).edit().putInt("lay_where_w", v.coerceIn(0, 400)).apply()

    /** The small pencil on the widget (opens Move / Arrange & resize / Reset). On by default so Reset can always be reached. */
    fun editBtn(c: Context): Boolean = sp(c).getBoolean("lay_editbtn", true)
    fun setEditBtn(c: Context, v: Boolean) { sp(c).edit().putBoolean("lay_editbtn", v).apply(); markTidy(c, v) }

    /** Size of one widget section in percent (100 = as before). */
    fun blockScale(c: Context, key: String): Int = sp(c).getInt("lay_scale_$key", 100).coerceIn(50, 200)
    fun setBlockScale(c: Context, key: String, v: Int) = sp(c).edit().putInt("lay_scale_$key", v.coerceIn(50, 200)).apply()
    fun resetArrange(c: Context) {
        val e = sp(c).edit()
        for (k in listOf("lay_blocks", "lay_order", "lay_pill_pos", "lay_pill_first", "lay_merge", "lay_merge_h", "lay_merge_gap", "lay_where_w", "lay_editbtn", "lay_row_top", "lay_row_head")) e.remove(k)
        for (k in sp(c).all.keys) if (k.startsWith("lay_iscale_") || k.startsWith("lay_wscale_") || k.startsWith("lay_hscale_") || k.startsWith("lay_match_")) e.remove(k)
        for (k in BLOCKS) e.remove("lay_scale_$k")
        e.remove("free_on").remove("free_pos").remove("free_ref_w").remove("free_grid_top")
        e.apply()
    }

    // ---- Free layout: every item of the widget sits at its own place (off until the first item is moved) ----
    /** A place on the widget in dp from its top-left corner; [w] is a width in dp (0 = the item's own width). */
    class FreePos(val x: Float, val y: Float, val w: Float)

    /** Every item that can be placed freely: the 11 items, the merged shape, the 5 fast-dial and the 5 shortcut circles. */
    val FREE_KEYS: List<String> = ALL_ITEMS + listOf("merge") +
        (0 until DIAL_SLOTS).map { "d$it" } + (0 until QUICK_SLOTS).map { "q$it" }

    /**
     * Something was switched ON or added in the free layout: it may now sit on top of other items. Remember that, and
     * when this is called from a screen, open Move for a moment (it fixes the overlaps by itself, then closes).
     */
    fun markTidy(c: Context, shown: Boolean) {
        if (!shown || !freeOn(c)) return
        setTidyPending(c, true)
        if (c is android.app.Activity && c !is MoveActivity && !c.isFinishing) {
            try {
                c.startActivity(android.content.Intent(c, MoveActivity::class.java).putExtra("auto_tidy", true))
            } catch (e: Exception) {
            }
        }
    }

    /** Set when something was added to the widget (a shortcut circle): Move then fixes overlaps by itself once. */
    fun tidyPending(c: Context): Boolean = sp(c).getBoolean("tidy_pending", false)
    fun setTidyPending(c: Context, v: Boolean) = sp(c).edit().putBoolean("tidy_pending", v).apply()
    fun freeOn(c: Context): Boolean = sp(c).getBoolean("free_on", false)
    fun setFreeOn(c: Context, v: Boolean) = sp(c).edit().putBoolean("free_on", v).apply()

    // The parsed places are remembered while the saved text stays the same (the widget reads them on every redraw)
    private var freeRaw: String? = null
    private var freeParsed: Map<String, FreePos> = emptyMap()

    fun freePos(c: Context): Map<String, FreePos> {
        val raw = sp(c).getString("free_pos", "") ?: ""
        synchronized(this) {
            if (raw != freeRaw) {
                freeParsed = FreeCodec.parse(raw, FREE_KEYS).mapValues { FreePos(it.value.x, it.value.y, it.value.w) }
                freeRaw = raw
            }
            return LinkedHashMap(freeParsed)
        }
    }

    fun setFreePos(c: Context, m: Map<String, FreePos>) {
        val s = FreeCodec.encode(m.mapValues { FreeCodec.Spot(it.value.x, it.value.y, it.value.w) }, FREE_KEYS)
        sp(c).edit().putString("free_pos", s).apply()
    }

    /** Width of the widget (dp) the saved places were made for, 0 = unknown. Places are stretched when the widget is resized. */
    fun freeRefW(c: Context): Float = sp(c).getFloat("free_ref_w", 0f)
    fun setFreeRefW(c: Context, v: Float) {
        if (v > 0f) sp(c).edit().putFloat("free_ref_w", v).apply() else sp(c).edit().remove("free_ref_w").apply()
    }
    fun freeScaleFor(c: Context, curW: Float): Float {
        val ref = freeRefW(c)
        return if (ref <= 0f || curW <= 0f) 1f else (curW / ref).coerceIn(0.5f, 2f)
    }

    /** Where the message tiles start, in dp from the top of the widget. */
    fun freeGridTop(c: Context): Float = sp(c).getFloat("free_grid_top", 120f).coerceAtLeast(0f)
    fun setFreeGridTop(c: Context, v: Float) = sp(c).edit().putFloat("free_grid_top", v.coerceAtLeast(0f)).apply()

    /** While moving, items snap to a 4dp grid (on by default: lines things up). */
    /** Gap (dp) used by "Tidy up: pack items closer", and whether a packed row is centred in the widget. */
    fun freeGap(c: Context): Int = sp(c).getInt("free_gap", 12).coerceIn(0, 40)
    fun setFreeGap(c: Context, v: Int) = sp(c).edit().putInt("free_gap", v).apply()
    fun freePackCenter(c: Context): Boolean = sp(c).getBoolean("free_pack_center", false)
    fun setFreePackCenter(c: Context, v: Boolean) = sp(c).edit().putBoolean("free_pack_center", v).apply()

    /** Space (dp) below each block of the widget in the rows layout: top row, fast dial, shortcuts row, Inbox row. 6 = as designed. */
    fun rowGap(c: Context): Int = sp(c).getInt("lay_row_gap", 6).coerceIn(0, 30)
    fun setRowGap(c: Context, v: Int) = sp(c).edit().putInt("lay_row_gap", v).apply()

    // ---- Message filters and limits
    /** Messages containing any of these words (comma separated) are hidden from the widget and the panel. */
    fun muteWords(c: Context): String = sp(c).getString("flt_words", "") ?: ""
    fun setMuteWords(c: Context, v: String) = sp(c).edit().putString("flt_words", v.trim()).apply()
    /** Messages from these names (comma separated) are always listed first. */
    fun vipNames(c: Context): String = sp(c).getString("flt_vip", "") ?: ""
    fun setVipNames(c: Context, v: String) = sp(c).edit().putString("flt_vip", v.trim()).apply()
    /** Messages older than this many hours are hidden; 0 = never. */
    fun maxAgeHours(c: Context): Int = sp(c).getInt("flt_age", 0).coerceIn(0, 720)
    fun setMaxAgeHours(c: Context, v: Int) = sp(c).edit().putInt("flt_age", v).apply()
    /** Most tiles drawn on the widget (fewer = lighter and faster); 0 = all. */
    fun maxTiles(c: Context): Int = sp(c).getInt("flt_max", 0).coerceIn(0, 100)
    fun setMaxTiles(c: Context, v: Int) = sp(c).edit().putInt("flt_max", v).apply()

    // ---- Mute one chat (not the whole app); the key is "package|chat title" (see Feed.chatKey)
    fun mutedChats(c: Context): Set<String> = HashSet(sp(c).getStringSet("chat_muted", emptySet()) ?: emptySet())
    fun setChatMuted(c: Context, key: String, on: Boolean) {
        val s = HashSet(mutedChats(c))
        if (on) s.add(key) else s.remove(key)
        sp(c).edit().putStringSet("chat_muted", s).apply()
    }

    // ---- Reply templates: one per line, shown as quick buttons above the reply box
    private const val DEFAULT_TEMPLATES = "On my way\nThanks\nOK\nI'll call you later"
    fun replyTemplatesRaw(c: Context): String = sp(c).getString("reply_tpl", DEFAULT_TEMPLATES) ?: DEFAULT_TEMPLATES
    fun setReplyTemplates(c: Context, v: String) = sp(c).edit().putString("reply_tpl", v.trim()).apply()
    fun replyTemplates(c: Context): List<String> =
        replyTemplatesRaw(c).split("\n").map { it.trim() }.filter { it.isNotEmpty() }.take(12)

    // ---- Snooze one chat: key -> time (ms) when it comes back
    fun chatSnoozeMap(c: Context): Map<String, Long> {
        val raw = sp(c).getString("chat_snz", "") ?: ""
        if (raw.isEmpty()) return emptyMap()
        return try {
            val o = org.json.JSONObject(raw)
            val now = System.currentTimeMillis()
            val m = HashMap<String, Long>()
            for (k in o.keys()) {
                val t = o.getLong(k)
                if (t > now) m[k] = t
            }
            m
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** until = 0 wakes the chat again. */
    fun setChatSnooze(c: Context, key: String, until: Long) {
        val m = HashMap(chatSnoozeMap(c))
        if (until <= 0L) m.remove(key) else m[key] = until
        while (m.size > 60) {
            val oldest = m.minByOrNull { it.value }?.key ?: break
            m.remove(oldest)
        }
        val o = org.json.JSONObject()
        for ((k, v) in m) o.put(k, v)
        sp(c).edit().putString("chat_snz", o.toString()).apply()
    }

    /** Settings: opening a section closes the others. */
    fun oneSection(c: Context): Boolean = sp(c).getBoolean("ui_one_section", false)
    fun setOneSection(c: Context, v: Boolean) = sp(c).edit().putBoolean("ui_one_section", v).apply()

    /** One step of undo for Move: the places before the last drag or tidy-up. */
    fun saveFreePrev(c: Context) {
        val p = sp(c)
        p.edit().putBoolean("free_prev_has", true).putBoolean("free_prev_on", p.getBoolean("free_on", false))
            .putString("free_prev_pos", p.getString("free_pos", "") ?: "").putFloat("free_prev_ref", p.getFloat("free_ref_w", 0f)).apply()
    }
    fun hasFreePrev(c: Context): Boolean = sp(c).getBoolean("free_prev_has", false)
    fun undoFree(c: Context) {
        val p = sp(c)
        if (!p.getBoolean("free_prev_has", false)) return
        val e = p.edit().putBoolean("free_on", p.getBoolean("free_prev_on", false)).putString("free_pos", p.getString("free_prev_pos", "") ?: "")
        val ref = p.getFloat("free_prev_ref", 0f)
        if (ref > 0f) e.putFloat("free_ref_w", ref) else e.remove("free_ref_w")
        e.remove("free_prev_has").apply()
    }

    fun freeSnap(c: Context): Boolean = sp(c).getBoolean("free_snap", true)
    fun setFreeSnap(c: Context, v: Boolean) = sp(c).edit().putBoolean("free_snap", v).apply()

    /** Width of the Where to? box in the free layout, in dp (0 = as it was). */
    fun setFreeWidth(c: Context, key: String, w: Float) {
        val m = freePos(c).toMutableMap()
        val p = m[key] ?: return
        m[key] = FreePos(p.x, p.y, w)
        setFreePos(c, m)
    }

    /** Back to the rows layout (the places are forgotten). */
    fun resetFree(c: Context) {
        sp(c).edit().remove("free_on").remove("free_pos").remove("free_ref_w").remove("free_grid_top").apply()
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
        "merge" to "Widget: merged shape (clock + weather + date together)",
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
    // Text size per part of the widget (percent, 100 = as designed)
    val WIDGET_SIZE_PARTS = listOf(
        "title" to "Inbox title",
        "note" to "+ Note",
        "clear" to "Clear all / Read all",
        "clock" to "Clock",
        "weather" to "Weather",
        "date" to "Date",
        "merge" to "Merged shape (clock, weather, date together)",
        "where" to "Where to? text",
        "empty" to "Nothing new text",
        "dialletter" to "Fast-dial letters (inside circles)",
        "dialnames" to "Names under fast-dial circles",
        "quickletter" to "Shortcut letters (inside circles)",
        "quicknames" to "Names under shortcut circles",
        "tapp" to "Tiles: app name",
        "ttitle" to "Tiles: message title",
        "ttext" to "Tiles: message text",
        "pages" to "Page buttons"
    )
    fun partSize(c: Context, k: String): Int = sp(c).getInt("look_ps_$k", 100).coerceIn(50, 300)
    fun setPartSize(c: Context, k: String, v: Int) = sp(c).edit().putInt("look_ps_$k", v).apply()
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
    fun setRefreshShow(c: Context, v: Boolean) { sp(c).edit().putBoolean("look_refresh", v).apply(); markTidy(c, v) }
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
    /** The "+ Add" circle: empty fast-dial circles and the shortcuts row's Add circle can be hidden (add contacts and shortcuts from Settings instead). */
    fun dialAdd(c: Context): Boolean = sp(c).getBoolean("lay_dial_add", true)
    fun setDialAdd(c: Context, v: Boolean) { sp(c).edit().putBoolean("lay_dial_add", v).apply(); markTidy(c, v) }
    fun quickAdd(c: Context): Boolean = sp(c).getBoolean("lay_quick_add", true)
    /** After a shortcut is chosen in the free layout, hide the + circle so no new item appears and crowds the row. */
    fun keepPlusHidden(c: Context): Boolean = sp(c).getBoolean("keep_plus_hidden", true)
    fun setKeepPlusHidden(c: Context, v: Boolean) = sp(c).edit().putBoolean("keep_plus_hidden", v).apply()
    fun setQuickAdd(c: Context, v: Boolean) { sp(c).edit().putBoolean("lay_quick_add", v).apply(); markTidy(c, v) }

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
    /** The bottom popup closes after Clear all / Read all instead of staying open (empty). */
    fun closeAfterClear(c: Context): Boolean = sp(c).getBoolean("close_after_clear", true)
    fun setCloseAfterClear(c: Context, v: Boolean) = sp(c).edit().putBoolean("close_after_clear", v).apply()
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
        "p_gear" to "Panel: settings icon",
        "p_back" to "Panel: ‹ Back (in an opened message)",
        "p_mic" to "Panel: mic (new, off until you switch it on)",
        "p_edit" to "Panel: pencil (new, off until you switch it on)",
        "p_refresh" to "Panel: refresh (new, off until you switch it on)",
        "p_icon" to "Panel tiles: app icon",
        "p_cnt" to "Panel tiles: count badge",
        "p_title_t" to "Panel tiles: message title",
        "p_text" to "Panel tiles: message text"
    )
    // Items added later start hidden, so nothing changes until they are switched on
    private val OFF_BY_DEFAULT = setOf("p_mic", "p_edit", "p_refresh")
    fun showItem(c: Context, k: String): Boolean = sp(c).getBoolean("lay_item_$k", k !in OFF_BY_DEFAULT)
    fun setShowItem(c: Context, k: String, v: Boolean) { sp(c).edit().putBoolean("lay_item_$k", v).apply(); markTidy(c, v) }

    // ---- Font format: letter case of the widget text, and 3D icons ----
    /** 0 = as typed, 1 = UPPERCASE, 2 = lowercase, 3 = Each Word Capital (widget text only) */
    fun textCase(c: Context): Int = sp(c).getInt("look_case", 0).coerceIn(0, 3)
    fun setTextCase(c: Context, v: Int) = sp(c).edit().putInt("look_case", v).apply()
    // Emoji in place of the built-in icons / words ("" = the normal one). Keys: note, mic, reset, clear, edit, gear, inbox
    fun emojiOf(c: Context, k: String): String = sp(c).getString("look_emoji_$k", "") ?: ""
    fun setEmojiOf(c: Context, k: String, v: String) = sp(c).edit().putString("look_emoji_$k", v.trim().take(8)).apply()
    // Message tiles in the panel: corner roundness (-1 = as designed), border style (0 solid, 1 dashed, 2 dotted),
    // own fill colour (0 = each app's colour) and how solid the fill is (100 = as designed)
    fun tileRadius(c: Context): Int = sp(c).getInt("look_tile_radius", -1)
    fun setTileRadius(c: Context, v: Int) = sp(c).edit().putInt("look_tile_radius", v).apply()
    fun tileBorderStyle(c: Context): Int = sp(c).getInt("look_tile_bstyle", 0)
    fun setTileBorderStyle(c: Context, v: Int) = sp(c).edit().putInt("look_tile_bstyle", v).apply()
    fun tileFill(c: Context): Int = sp(c).getInt("look_tile_fill", 0)
    fun setTileFill(c: Context, v: Int) = sp(c).edit().putInt("look_tile_fill", v).apply()
    fun tileFillAlpha(c: Context): Int = sp(c).getInt("look_tile_falpha", 100)
    fun setTileFillAlpha(c: Context, v: Int) = sp(c).edit().putInt("look_tile_falpha", v).apply()
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
    /** Swipe-through: the widget's tiles are fixed (not a scrolling list), so up / down swipes over them reach the launcher. */
    fun swipeThrough(c: Context): Boolean = sp(c).getBoolean("scr_thru", false)
    fun setSwipeThrough(c: Context, v: Boolean) = sp(c).edit().putBoolean("scr_thru", v).apply()
    /** Swipe-through: false = only the top line of a tile (app name and count) opens it, the rest passes swipes to the launcher; true = the whole tile opens it (a swipe starting on a tile is then taken by the widget). */
    fun swipeTapAll(c: Context): Boolean = sp(c).getBoolean("scr_tap_all", false)
    /** Phone Back button inside an opened message: false = closes the panel, true = goes back to the tiles first. */
    fun backToTiles(c: Context): Boolean = sp(c).getBoolean("back_to_tiles", false)
    fun setBackToTiles(c: Context, v: Boolean) = sp(c).edit().putBoolean("back_to_tiles", v).apply()
    fun setSwipeTapAll(c: Context, v: Boolean) = sp(c).edit().putBoolean("scr_tap_all", v).apply()
    /** Swipe-through: also let the name line of a tile (e.g. the sender) open it, besides the top line. */
    fun swipeTapName(c: Context): Boolean = sp(c).getBoolean("scr_tap_name", false)
    fun setSwipeTapName(c: Context, v: Boolean) = sp(c).edit().putBoolean("scr_tap_name", v).apply()
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
