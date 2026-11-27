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

    /** Hours and minutes from a time such as 23, 23:30, 11pm or 7 am. -1 when it is not a time. */
    private fun toMinutes(h0: Int, m: Int, ampm: String): Int {
        if (h0 > 24 || m > 59) return -1
        var h = h0
        if (ampm == "pm" && h < 12) h += 12
        if (ampm == "am" && h == 12) h = 0
        if (h == 24) h = 0
        return h * 60 + m
    }

    private fun hhmm(min: Int): String = String.format("%02d:%02d", min / 60, min % 60)

    /** The card buttons under each message, by the words people use for them. */
    private val CARD_BUTTONS = listOf(
        Triple("app", listOf("app"), "The app's own buttons"),
        Triple("read", listOf("read"), "Mark as read"),
        Triple("snooze", listOf("snooze"), "Snooze"),
        Triple("mute", listOf("mute"), "Mute chat"),
        Triple("open", listOf("open"), "Open"),
        Triple("clear", listOf("clear"), "Clear all of the app")
    )

    /** The animation parts and the words that name them. */
    private val ANIM_WORDS = mapOf(
        "open" to listOf("open", "opens", "opening"), "close" to listOf("close", "closes", "closing"),
        "note" to listOf("note", "pen"), "clear" to listOf("clear"), "gear" to listOf("gear", "settings"),
        "refresh" to listOf("refresh", "reset"), "mic" to listOf("mic", "recorder"), "edit" to listOf("pencil", "edit"),
        "title" to listOf("title"), "badge" to listOf("badge", "count"), "row" to listOf("arrives", "arriving", "row"),
        "press" to listOf("touch", "press", "tap", "feedback"), "list" to listOf("appear", "appears", "list")
    )

    /**
     * Settings that are not a single number or switch: quiet hours with times, pinned and muted apps,
     * card buttons (hide, show, move), My pages, pictures of the backgrounds, and the animation style of each part.
     */
    private fun extras(c: Context, input: String): Helper.Result? {
        val raw = input.trim().trimEnd('.', '!', '?')
        val low = raw.lowercase()
        val w = low.split(Regex("[^a-z]+")).filter { it.isNotEmpty() }
        val widgetWords = setOf("widget", "pencil", "panel", "dial", "shortcut", "where")

        // Background pictures: "remove the widget picture", "change the panel background picture"
        val picKind = when {
            w.contains("widget") -> "widget"
            w.contains("dial") || w.contains("dials") || w.contains("strip") -> "dial"
            w.contains("panel") -> "panel"
            else -> null
        }
        if (picKind != null && w.any { it == "picture" || it == "photo" || it == "image" || it == "pictures" } &&
            w.none { it == "strength" || it == "opacity" }) {
            if (w.any { it in setOf("remove", "clear", "delete", "without", "no") }) {
                Look.clearImage(c, picKind)
                redraw(c)
                return Helper.Result("The $picKind picture is removed.", true, "")
            }
            // Choosing a photo needs the photo picker, which the Settings screen opens
            return Helper.Result("Pick the new picture.", false, "", "pic:$picKind")
        }

        // Pin to the top, or mute, one app by name: "pin WhatsApp", "unmute Gmail"
        val pinMatch = Regex("^(?:please )?(un)?(pin|mute)\\s+(?:the\\s+)?(.+)$", RegexOption.IGNORE_CASE).find(raw)
        if (pinMatch != null && w.none { it in setOf("icon", "picture", "photo", "image", "emoji", "chat", "message", "where", "button", "tile", "dial", "shortcut", "pencil", "note", "buttons") }) {
            val off = pinMatch.groupValues[1].isNotEmpty()
            val kind = pinMatch.groupValues[2].lowercase()
            val appName = pinMatch.groupValues[3].trim().replace(Regex("\\s+(?:to the )?(?:top|first)$", RegexOption.IGNORE_CASE), "")
            val app = findApp(c, appName) ?: return Helper.Result("I did not find an app called \"$appName\".", false, "")
            if (kind == "pin") Prefs.setPinned(c, app.first, !off) else Prefs.setMuted(c, app.first, !off)
            redraw(c)
            val what = when {
                kind == "pin" && off -> "no longer pinned to the top"
                kind == "pin" -> "pinned to the top"
                off -> "no longer muted"
                else -> "muted"
            }
            return Helper.Result("${app.second} is $what.", true, "")
        }

        // Quiet hours with times: "quiet hours from 11pm to 7am"
        if (w.contains("quiet") && (w.contains("hours") || w.contains("time") || w.contains("from") || w.contains("between"))) {
            val times = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", RegexOption.IGNORE_CASE).findAll(low)
                .map { m -> toMinutes(m.groupValues[1].toInt(), m.groupValues[2].ifEmpty { "0" }.toInt(), m.groupValues[3].lowercase()) }
                .filter { it >= 0 }.toList()
            if (times.size >= 2) {
                Prefs.setQuietStart(c, times[0])
                Prefs.setQuietEnd(c, times[1])
                Prefs.setQuietOn(c, true)
                redraw(c)
                return Helper.Result("Quiet hours are on from ${hhmm(times[0])} to ${hhmm(times[1])}.", true, "")
            }
        }

        // Card buttons under a message: hide, show, or move to the top, the bottom or a place
        val onCard = (w.contains("button") || w.contains("buttons") || w.contains("card")) && w.none { it in widgetWords }
        val key = CARD_BUTTONS.firstOrNull { (_, words, _) -> words.any { it in w } }
        if (onCard && key != null) {
            val (k, _, title) = key
            val moving = w.any { it in setOf("move", "put", "place") }
            if (!moving && (w.any { it in setOf("hide", "remove", "without", "off", "stop") } || w.any { it in setOf("show", "bring", "back", "on") })) {
                val hide = w.any { it in setOf("hide", "remove", "without", "off", "stop") }
                val set = Prefs.cardBtnHidden(c).toMutableSet()
                if (hide) set.add(k) else set.remove(k)
                Prefs.setCardBtnHidden(c, set)
                redraw(c)
                return Helper.Result("$title is ${if (hide) "hidden" else "shown"} under messages.", true, "")
            }
            if (moving) {
                val order = Prefs.cardBtnOrder(c).toMutableList()
                order.remove(k)
                val target = when {
                    w.any { it in setOf("top", "first", "start", "beginning") } -> 0
                    w.any { it in setOf("bottom", "last", "end") } -> order.size
                    else -> Regex("(\\d+)").find(low)?.groupValues?.get(1)?.toIntOrNull()?.minus(1)
                }
                if (target != null) {
                    order.add(target.coerceIn(0, order.size), k)
                    Prefs.setCardBtnOrder(c, order)
                    redraw(c)
                    return Helper.Result("$title is now at place ${order.indexOf(k) + 1} under messages.", true, "")
                }
            }
        }

        // My pages: "add page Work with WhatsApp and Gmail", "delete page Work", "add Gmail to page Work"
        Regex("^(?:please )?(?:add|make|create|new)\\s+(?:a |my )?(?:new )?page\\s+(?:called |named )?(.+?)(?:\\s+with\\s+(.+))?$", RegexOption.IGNORE_CASE).find(raw)?.let { m ->
            val name = m.groupValues[1].trim().trim('"').take(30)
            val names = m.groupValues[2].split(Regex("\\s*(?:,| and )\\s*")).map { it.trim() }.filter { it.isNotEmpty() }
            val found = names.map { n -> findApp(c, n) ?: return Helper.Result("I did not find an app called \"$n\".", false, "") }
            val pkgs = found.map { it.first }
            val notOn = found.filter { it.first !in Prefs.enabled(c) }
            if (notOn.isNotEmpty()) {
                return Helper.Result("Choose ${notOn.joinToString(", ") { it.second }} on the main screen first, then add them to a page.", false, "")
            }
            val pages = Prefs.customPages(c)
            val idx = pages.indexOfFirst { it.name.equals(name, ignoreCase = true) }
            val base = pages.map { p -> p.copy(apps = p.apps.filter { it !in pkgs }) }.toMutableList()
            if (idx >= 0) base[idx] = base[idx].copy(apps = base[idx].apps + pkgs) else base.add(CPage(name, pkgs, emptyList()))
            Prefs.setCustomPages(c, base)
            Prefs.setScrollMode(c, 2)
            redraw(c)
            return Helper.Result("Page \"$name\" is ready${if (pkgs.isEmpty()) "" else " with ${found.joinToString(", ") { it.second }}"}. My pages is on.", true, "")
        }
        Regex("^(?:please )?(?:delete|remove)\\s+(?:the )?page\\s+(.+)$", RegexOption.IGNORE_CASE).find(raw)?.let { m ->
            val name = m.groupValues[1].trim()
            val pages = Prefs.customPages(c)
            if (pages.none { it.name.equals(name, ignoreCase = true) }) return Helper.Result("There is no page called \"$name\".", false, "")
            Prefs.setCustomPages(c, pages.filter { !it.name.equals(name, ignoreCase = true) })
            redraw(c)
            return Helper.Result("Deleted the page \"$name\".", true, "")
        }
        Regex("^(?:please )?(?:add|put|move)\\s+(.+?)\\s+(?:to|on)\\s+(?:the )?page\\s+(.+)$", RegexOption.IGNORE_CASE).find(raw)?.let { m ->
            val appName = m.groupValues[1].trim()
            val name = m.groupValues[2].trim()
            val pages = Prefs.customPages(c)
            val idx = pages.indexOfFirst { it.name.equals(name, ignoreCase = true) }
            if (idx >= 0) {
                val app = findApp(c, appName) ?: return Helper.Result("I did not find an app called \"$appName\".", false, "")
                val base = pages.map { p -> p.copy(apps = p.apps.filter { it != app.first }) }.toMutableList()
                base[idx] = base[idx].copy(apps = base[idx].apps + app.first)
                Prefs.setCustomPages(c, base)
                redraw(c)
                return Helper.Result("${app.second} is on the page \"${base[idx].name}\".", true, "")
            }
        }

        // Animation of one part: "animation for the panel open fade", "turn off the mic animation"
        if (w.contains("animation") || w.contains("animations")) {
            val el = Alive.ELEMENTS.firstOrNull { e -> ANIM_WORDS[e.key].orEmpty().any { it in w } }
            if (el != null) {
                val style = el.styles.firstOrNull { st ->
                    w.contains(st.first) || st.second.lowercase().split(Regex("[^a-z]+")).any { it.length >= 4 && it in w }
                }
                val off = w.any { it == "off" || it == "none" }
                val pick = when {
                    style != null -> style.first
                    off -> "off"
                    else -> null
                }
                if (pick != null) {
                    Prefs.setAnimStyle(c, el.key, pick)
                    redraw(c)
                    val name = el.label.substringBefore(" (")
                    return Helper.Result(if (pick == "off") "$name has no animation." else "$name now uses ${style!!.second}.", true, "")
                }
            }
        }
        return null
    }

    /** When a reset was asked for: the time it was asked, so "confirm" only works for a short while. */
    private var resetAskedAt = 0L

    /**
     * The pencil menu's own actions by name: saved layouts (save, load, delete, list) and reset everything.
     * Reset asks first; the next "confirm" within two minutes does it.
     */
    private fun pencil(c: Context, input: String): Helper.Result? {
        val raw = input.trim().trimEnd('.', '!', '?')
        val lower = raw.lowercase().replace(Regex("\\s+"), " ")

        Regex("^(?:please )?(?:save|store|keep)\\s+(?:this |the |my |current )?(?:layout|look|widget|arrangement)(?:\\s+(?:as|called|named))?\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(raw)?.let {
            val name = it.groupValues[1].trim().trim('"').take(30)
            val ok = Prefs.savePreset(c, name)
            return Helper.Result(if (ok) "Saved the layout as \"$name\". Load it with \"load layout $name\"." else "Type a name for the layout first.", false, "")
        }
        Regex("^(?:please )?(?:load|open|use|apply|restore)\\s+(?:the |my |saved )?layout\\s+(.+)$", RegexOption.IGNORE_CASE).find(raw)?.let {
            val wanted = it.groupValues[1].trim()
            val name = Prefs.presetNames(c).firstOrNull { n -> n.equals(wanted, ignoreCase = true) } ?: wanted
            if (name !in Prefs.presetNames(c)) {
                val have = Prefs.presetNames(c)
                return Helper.Result(if (have.isEmpty()) "You have no saved layouts yet. Save one with \"save this layout as Work\"."
                    else "There is no layout called \"$wanted\". Saved layouts: ${have.joinToString(", ")}.", false, "")
            }
            val ok = Prefs.loadPreset(c, name, Prefs.SCOPE_PENCIL)
            redraw(c)
            return Helper.Result(if (ok) "Loaded the layout \"$name\"." else "Could not load \"$name\".", ok, "")
        }
        Regex("^(?:please )?(?:delete|remove|forget)\\s+(?:the |my )?layout\\s+(.+)$", RegexOption.IGNORE_CASE).find(raw)?.let {
            val wanted = it.groupValues[1].trim()
            val name = Prefs.presetNames(c).firstOrNull { n -> n.equals(wanted, ignoreCase = true) }
                ?: return Helper.Result("There is no layout called \"$wanted\".", false, "")
            Prefs.deletePreset(c, name)
            return Helper.Result("Deleted the layout \"$name\".", false, "")
        }
        if (Regex("^(?:please )?(?:list|show|which|what are my|what)\\s+(?:my |the |saved )?layouts?\\??$").matches(lower)) {
            val have = Prefs.presetNames(c)
            return Helper.Result(if (have.isEmpty()) "You have no saved layouts yet." else "Saved layouts: ${have.joinToString(", ")}.", false, "")
        }
        val wantsReset = Regex("\\b(reset|start over|back to normal|back to default)\\b").containsMatchIn(lower) &&
            Regex("\\b(everything|all|the widget|the look|the whole|default|standard)\\b").containsMatchIn(lower)
        if (wantsReset && !Regex("\\b(icon|text|colour|color|size)\\b").containsMatchIn(lower)) {
            resetAskedAt = System.currentTimeMillis()
            return Helper.Result("This puts the widget back to the standard layout and removes ALL themes, colours, sizes and moved places. " +
                "Your apps, contacts, shortcuts and notes are kept. Say \"confirm reset\" within two minutes to do it.", false, "")
        }
        if (Regex("\\b(confirm|yes)\\b").containsMatchIn(lower) && System.currentTimeMillis() - resetAskedAt < 120_000L) {
            resetAskedAt = 0L
            Prefs.resetAllLook(c, Prefs.SCOPE_PENCIL)
            redraw(c)
            return Helper.Result("Everything is back to the default look. You can undo this from the pencil menu.", true, "")
        }
        return null
    }

    /** Handles a sentence that adds, changes, removes or tidies. Returns null when the sentence is not one of those. */
    fun handle(c: Context, input: String, intent: String? = null): Helper.Result? {
        val w = words(input)
        if (w.isEmpty()) return null
        pencil(c, input)?.let { return it }
        extras(c, input)?.let { return it }
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
