package com.example.notiftiles

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * Things the helper DOES besides changing one setting (those are in Helper, LayoutHelper and HelperSettings):
 *  - tidy the spaces between rows, the same as the pencil's "Fix layout" and "Tidy up" buttons;
 *  - add, change and remove fast-dial circles, and show or hide the fast-dial row;
 *  - add and remove shortcut circles (an app), and show or hide the shortcuts row;
 *  - add a quick note.
 * Everything here can still be done by hand on the normal screens. The helper only presses the same buttons.
 */
object HelperActions {
    /** Words that are never part of a name or an app. */
    private val NAME_STOP = setOf(
        "add", "new", "set", "change", "make", "put", "create", "please", "can", "you", "i", "want", "to", "a", "an", "the", "my",
        "for", "as", "with", "named", "called", "name", "fast", "dial", "dials", "dialer", "contact", "number", "phone", "slot",
        "circle", "circles", "in", "on", "at", "of", "and", "call", "remove", "delete", "clear", "empty", "erase", "rename",
        "update", "shortcut", "shortcuts", "quick", "icon", "button", "row", "show", "hide", "turn", "off", "first", "second",
        "third", "fourth", "fifth", "last", "one", "two", "three", "four", "five", "it", "that", "this", "me", "no", "use", "pls",
        "hey", "could", "would", "from", "link", "app", "application", "drop", "rid", "get", "also", "now", "is", "be", "should"
    )
    private val ADD_WORDS = setOf("add", "new", "set", "change", "make", "put", "create", "rename", "update", "use", "replace", "assign")
    private val REMOVE_WORDS = setOf("remove", "delete", "clear", "empty", "erase", "drop", "rid")
    private val HIDE_WORDS = setOf("hide", "off", "disable", "without", "stop")
    private val SHOW_WORDS = setOf("show", "on", "enable", "display", "start")
    private val ORDINALS = mapOf("first" to 1, "second" to 2, "third" to 3, "fourth" to 4, "fifth" to 5, "last" to -1,
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5)

    /** Words that show the sentence changes a look or a size (\"make dial 2 bigger\"), which the settings handle, not a dial or shortcut to add. */
    private val SETTING_WORDS = setOf("names", "size", "bigger", "smaller", "larger", "wider", "taller", "shorter", "colour", "color", "text",
        "border", "background", "opacity", "transparent", "bold", "font", "shape", "round", "square", "increase", "decrease", "gap", "spacing")

    private fun redraw(c: Context) = TilesWidget.rebuildAll(c.applicationContext)

    private fun words(input: String): List<String> =
        input.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }.map { Helper.canon(it) }

    private fun cap(s: String): String = s.split(" ").filter { it.isNotEmpty() }.joinToString(" ") { it.replaceFirstChar { ch -> ch.uppercase() } }

    /** Handles a sentence that adds, changes, removes or tidies. Returns null when the sentence is not one of those. */
    fun handle(c: Context, input: String, intent: String? = null): Helper.Result? {
        val w = words(input)
        if (w.isEmpty()) return null
        val isNote = w.contains("note") || w.contains("notes")
        val isDial = w.contains("dial") || w.contains("dials") || w.contains("dialer") || intent?.startsWith("dial_") == true
        val isShort = w.contains("shortcut") || w.contains("shortcuts") || intent?.startsWith("shortcut_") == true
        val hasAdd = w.any { it in ADD_WORDS }

        // A quick note: "add a note buy milk", "note: call the bank"
        if (isNote && (w.contains("add") || w.contains("new") || w.contains("create") || w.contains("write") || w.contains("save")) &&
            !isDial && !isShort) {
            val body = input.replace(Regex("^\\s*(?:please\\s+)?(?:can you\\s+)?(?:add|new|create|write|save)\\s+(?:a\\s+|new\\s+)?(?:quick\\s+)?notes?\\s*(?:saying|that says|:|-|to say)?\\s*", RegexOption.IGNORE_CASE), "").trim()
            if (body.isEmpty() || body.equals(input.trim(), true)) return null
            Prefs.addNote(c, body)
            return Helper.Result("Note added: $body", true, "")
        }

        if (isDial) return dialCommand(c, input, w, hasAdd)
        if (isShort) return shortcutCommand(c, input, w, hasAdd)
        val raw = input.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }
        return tidyCommand(c, input, (w + raw).distinct(), intent)
    }

    // ---------------------------------------------------------------- tidy

    private val TIDY_WORDS = setOf("tidy", "neat", "neaten", "even", "evenly", "equal", "equally", "same", "uniform", "align", "straighten",
        "organize", "organise", "fix", "arrange", "pack", "packed", "overlap", "overlapping", "clean")
    private val TIDY_OBJECTS = setOf("space", "spaces", "gap", "gaps", "spacing", "row", "rows", "layout", "overlap", "overlapping")

    private fun tidyCommand(c: Context, input: String, w: List<String>, intent: String?): Helper.Result? {
        val asked = intent == "tidy" || intent == "tidy_even" || intent == "tidy_pack" || intent == "fix_layout"
        val moreLess = w.any { it in setOf("bigger", "larger", "more", "less", "smaller", "increase", "decrease", "reduce", "wider", "narrower") }
        val sentenceOk = w.any { it in TIDY_WORDS } && w.any { it in TIDY_OBJECTS || it.startsWith("space") || it.startsWith("gap") }
        if (!asked && (!sentenceOk || moreLess)) return null
        val pack = intent == "tidy_pack" || w.any { it == "pack" || it == "packed" || it == "closer" || it == "tight" }
        val even = intent == "tidy_even" || w.any { it == "even" || it == "evenly" || it == "equal" || it == "equally" }
        if (!Prefs.freeOn(c)) {
            // Rows layout: one gap value is shared by every row, so the spaces are already equal. Back to the designed 6.
            Prefs.setRowGap(c, 6)
            redraw(c)
            return Helper.Result("The rows now share one equal gap (6, as designed). To tidy items you placed by hand, use Move first.", true, "")
        }
        val i = Intent(c, MoveActivity::class.java).putExtra("auto_tidy", true)
        if (pack) i.putExtra("tidy_mode", 2) else if (even && !w.contains("fix")) i.putExtra("tidy_mode", 1) else i.putExtra("fix_all", true)
        if (c !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            c.startActivity(i)
            val what = if (pack) "packed closer" else if (even && !w.contains("fix")) "lined up with equal spaces" else "laid out again with the same space between every two rows"
            Helper.Result("Tidying: the items are $what, like the pencil's Fix layout. It closes by itself.", true, "")
        } catch (e: Exception) {
            Helper.Result("I could not open Move to tidy. Open the pencil and press Fix layout.", false, "")
        }
    }

    // ---------------------------------------------------------------- slots

    /** The slot number (1..max) the sentence names ("dial 2", "slot 3", "the second dial"), or null. The text with that part cut out is returned too. */
    private fun slotOf(input: String, w: List<String>, max: Int): Pair<Int?, String> {
        val rx = Regex("(?:dial|dials|slot|circle|shortcut|shortcuts|number|no|#)\\s*#?\\s*([1-9])(?!\\d)", RegexOption.IGNORE_CASE)
        val m = rx.find(input)
        if (m != null) {
            val n = m.groupValues[1].toInt()
            if (n in 1..max) return Pair(n, input.removeRange(m.range))
        }
        for (x in w) {
            val v = ORDINALS[x] ?: continue
            if (v == -1) return Pair(max, input)
            if (v in 1..max) return Pair(v, input)
        }
        return Pair(null, input)
    }

    private fun phoneIn(text: String): Pair<String, String> {
        val m = Regex("\\+?\\d[\\d ()\\-]{4,}\\d").find(text) ?: return Pair("", text)
        return Pair(m.value.replace(Regex("[ ()\\-]"), ""), text.removeRange(m.range))
    }

    /** The name left in the sentence once command words, slot and number are removed. */
    private fun nameIn(text: String): String {
        val toks = Regex("[\\p{L}][\\p{L}'\u2019]*").findAll(text).map { it.value }.filter { it.lowercase() !in NAME_STOP }.toList()
        return cap(toks.joinToString(" "))
    }

    // ---------------------------------------------------------------- fast dial

    private fun dialCommand(c: Context, input: String, w: List<String>, hasAdd: Boolean): Helper.Result? {
        val max = Prefs.DIAL_SLOTS
        val remove = w.any { it in REMOVE_WORDS }
        val (slot, afterSlot) = slotOf(input, w, max)
        val (number, afterNumber) = phoneIn(afterSlot)
        val name = nameIn(afterNumber)
        if (number.isEmpty() && w.any { it in SETTING_WORDS }) return null

        if (remove && (slot != null || name.isNotEmpty())) {
            val i = if (slot != null) slot - 1 else (0 until max).firstOrNull { Prefs.dialName(c, it).equals(name, true) }
            if (i == null || (Prefs.dialName(c, i).isEmpty() && Prefs.dialNumber(c, i).isEmpty()))
                return Helper.Result("I did not find that fast dial. Say its number, for example: remove dial 2.", false, "")
            val old = Prefs.dialName(c, i).ifEmpty { "dial ${i + 1}" }
            Prefs.clearDial(c, i)
            redraw(c)
            return Helper.Result("Fast dial ${i + 1} ($old) removed.", true, "")
        }
        val oldNumber = if (slot != null) Prefs.dialNumber(c, slot - 1) else ""
        val rename = slot != null && name.isNotEmpty() && oldNumber.isNotEmpty() && Helper.colourOf(name.lowercase()) == null
        val plainAdd = w.contains("add") || w.contains("new") || w.contains("create")
        if (hasAdd && (number.isNotEmpty() || rename || (plainAdd && name.isNotEmpty()))) {
            // Which slot: the one named, a dial with the same name, or the first empty one
            val i = slot?.minus(1)
                ?: (if (name.isNotEmpty()) (0 until max).firstOrNull { Prefs.dialName(c, it).equals(name, true) } else null)
                ?: (0 until max).firstOrNull { Prefs.dialName(c, it).isEmpty() && Prefs.dialNumber(c, it).isEmpty() }
                ?: return Helper.Result("All $max fast-dial slots are full. Say which one to replace, for example: set dial 2 to Ali 0551234567.", false, "")
            val newName = name.ifEmpty { Prefs.dialName(c, i) }
            val newNumber = number.ifEmpty { Prefs.dialNumber(c, i) }
            if (newNumber.isEmpty()) return Helper.Result("Add the phone number too, for example: add fast dial $newName 0501234567.", false, "")
            Prefs.setDial(c, i, newName.ifEmpty { newNumber }, newNumber)
            if (!Prefs.show(c, "dial")) Prefs.setShow(c, "dial", true)
            redraw(c)
            if (Prefs.freeOn(c)) Prefs.markTidy(c, true)
            return Helper.Result("Fast dial ${i + 1}: ${newName.ifEmpty { newNumber }} $newNumber.", true, "")
        }
        // The whole row on or off
        val hide = w.any { it in HIDE_WORDS || it in REMOVE_WORDS }
        val show = w.any { it in SHOW_WORDS }
        if (hide != show) {
            Prefs.setShow(c, "dial", show)
            redraw(c)
            return Helper.Result("The fast-dial row is now ${if (show) "shown" else "hidden"}.", true, "")
        }
        return null
    }

    // ---------------------------------------------------------------- shortcuts

    private fun launcherApps(c: Context): List<Pair<String, String>> {
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return c.packageManager.queryIntentActivities(launch, 0).map { it.activityInfo.packageName }.distinct()
            .filter { it != c.packageName }.map { Pair(it, Feed.label(c, it)) }
    }

    /** The app whose name best matches [name]: the exact name, then a name that contains it, then a close spelling. */
    private fun findApp(c: Context, name: String): Pair<String, String>? {
        val q = name.lowercase().trim()
        if (q.isEmpty()) return null
        val apps = launcherApps(c)
        apps.firstOrNull { it.second.lowercase() == q }?.let { return it }
        val flat = q.replace(" ", "")
        apps.firstOrNull { it.second.lowercase().replace(" ", "") == flat }?.let { return it }
        apps.filter { it.second.lowercase().contains(q) || q.contains(it.second.lowercase()) && it.second.length >= 3 }
            .minByOrNull { it.second.length }?.let { return it }
        var best: Pair<String, String>? = null
        var bestD = Int.MAX_VALUE
        for (a in apps) {
            val l = a.second.lowercase()
            if (Helper.near(l, q) || q.split(" ").any { t -> t.length >= 4 && l.split(" ").any { lw -> Helper.near(lw, t) } }) {
                if (l.length < bestD) { bestD = l.length; best = a }
            }
        }
        return best
    }

    private fun shortcutCommand(c: Context, input: String, w: List<String>, hasAdd: Boolean): Helper.Result? {
        val max = Prefs.QUICK_SLOTS
        val remove = w.any { it in REMOVE_WORDS }
        val (slot, afterSlot) = slotOf(input, w, max)
        val name = nameIn(afterSlot)
        if (w.any { it in SETTING_WORDS }) return null

        if (remove && (slot != null || name.isNotEmpty())) {
            val i = if (slot != null) slot - 1 else (0 until max).firstOrNull { Prefs.quick(c, it)?.name.equals(name, true) }
            val q = i?.let { Prefs.quick(c, it) }
            if (i == null || q == null) return Helper.Result("I did not find that shortcut. Say its number, for example: remove shortcut 2.", false, "")
            Prefs.clearQuick(c, i)
            redraw(c)
            return Helper.Result("Shortcut ${i + 1} (${q.name}) removed.", true, "")
        }
        if (hasAdd && name.isNotEmpty()) {
            val app = findApp(c, name) ?: return Helper.Result("I could not find an app called \"$name\" on this phone. For a screen inside an app, use the + in the shortcuts row.", false, "")
            val i = slot?.minus(1) ?: (0 until max).firstOrNull { Prefs.quick(c, it) == null }
                ?: return Helper.Result("All $max shortcut slots are full. Say which one to replace, for example: set shortcut 2 to ${app.second}.", false, "")
            Prefs.setQuick(c, i, Quick("app", app.second, app.first, ""))
            val wasOff = !Prefs.quickOn(c)
            if (wasOff) Prefs.setQuickOn(c, true)
            redraw(c)
            if (Prefs.freeOn(c)) Prefs.markTidy(c, true)
            return Helper.Result("Shortcut ${i + 1}: ${app.second}.${if (wasOff) " The shortcuts row is now on." else ""}", true, "")
        }
        val hide = w.any { it in HIDE_WORDS || it in REMOVE_WORDS }
        val show = w.any { it in SHOW_WORDS }
        if (hide != show) {
            Prefs.setQuickOn(c, show)
            redraw(c)
            return Helper.Result("The shortcuts row is now ${if (show) "shown" else "hidden"}.", true, "")
        }
        return null
    }
}
