package com.example.notiftiles

import android.content.Context

/**
 * The offline helper. It reads a sentence for what it is about (the object: text, widget, animations) and what to do with it
 * (the direction: bigger, less, off, a theme name). No internet and no AI model: it knows the words listed here, so it
 * understands many ordinary sentences but not every one. What it does not understand is looked up in Settings instead.
 */
object Helper {
    /** [message] is shown to the person, [changed] says a setting was changed, [search] is the word to look for in Settings. */
    class Result(val message: String, val changed: Boolean, val search: String, val openSlot: String? = null, val rowLabel: String? = null)

    private val STOP = setOf("make", "the", "more", "less", "and", "with", "for", "can", "you", "want", "change", "set", "please",
        "turn", "off", "on", "do", "its", "this", "that", "how", "some", "bit", "very", "much", "into", "my", "i", "to", "a", "an",
        "is", "it", "me", "should", "would", "could", "like", "be", "become", "get", "let", "lets", "i'd", "id", "something", "so")

    private val WIDGET = setOf("widget", "widgets", "background", "backdrop", "home", "screen", "box")
    private val TEXT = setOf("text", "texts", "font", "fonts", "writing", "letters", "words", "size", "labels")
    private val ANIM = setOf("animation", "animations", "animated", "motion", "moving", "movement", "effects")
    private val SEE_THROUGH = setOf("transparent", "transparency", "clear", "faded", "fade", "translucent", "invisible", "ghost")
    private val SOLID = setOf("solid", "opaque", "full", "visible", "dense", "thick")
    private val BIGGER = setOf("bigger", "larger", "big", "increase", "enlarge", "grow", "up", "more", "huge", "large", "raise")
    private val SMALLER = setOf("smaller", "small", "decrease", "reduce", "shrink", "down", "less", "tiny", "lower", "little")
    private val OFF = setOf("off", "stop", "disable", "disabled", "remove", "hide", "kill", "no", "without", "quit", "pause")
    private val ON = setOf("on", "enable", "enabled", "start", "back", "restore", "return", "show")
    private val DARK = setOf("dark", "night", "black", "darker", "midnight")
    private val LIGHT = setOf("light", "lighter", "bright", "day", "white")
    // Emoji icons: which icon a word means (the keys of EmojiPicker.SLOTS)
    private val SLOT_WORDS = listOf(
        "inbox" to "inbox", "mail" to "inbox", "mailbox" to "inbox", "envelope" to "inbox",
        "notes" to "note", "note" to "note",
        "mic" to "mic", "microphone" to "mic", "recorder" to "mic",
        "refresh" to "reset", "reset" to "reset",
        "pencil" to "edit", "edit" to "edit",
        "gear" to "gear", "cog" to "gear")
    private val ICON_WORDS = setOf("icon", "icons", "picture", "emoji", "symbol", "image")
    private val CHANGE_WORDS = setOf("change", "set", "make", "use", "replace", "pick")

    /** The first emoji in the text, or null. */
    fun firstEmoji(input: String): String? {
        var i = 0
        while (i < input.length) {
            val cp = input.codePointAt(i)
            if (Character.getType(cp) == Character.OTHER_SYMBOL.toInt()) {
                var end = i + Character.charCount(cp)
                if (end < input.length && input.codePointAt(end) == 0xFE0F) end++
                return input.substring(i, end)
            }
            i += Character.charCount(cp)
        }
        return null
    }

    private val NEGATION = setOf("less", "not", "no", "never", "solid", "opaque")
    private val RESET = setOf("normal", "default", "standard", "reset", "original", "back")

    /** The first number in the sentence, or null. */
    fun numberIn(input: String): Int? = Regex("\\d+").find(input)?.value?.toIntOrNull()

    /** true for "on" words, false for "off" words, null when the sentence has neither. */
    fun onOffIn(input: String): Boolean? {
        val w = input.lowercase().split(Regex("[^a-z]+"))
        return when {
            w.any { it in ON } -> true
            w.any { it in OFF } -> false
            else -> null
        }
    }

    private val ARTICLES = setOf("the", "my", "a", "an", "this", "that", "all", "its", "your")

    /**
     * A command sentence, as (mode, target, value). Modes: "set" (change X to Y, turn X on), "copy" (match X with Y, make X like Y).
     * Returns null when the sentence is not a command of that shape.
     */
    fun parseSentence(input: String): Triple<String, String, String>? {
        val t = input.lowercase().replace(Regex("[^a-z0-9 ]+"), " ").replace(Regex("\\s+"), " ").trim()
            .split(" ").filter { it.isNotEmpty() }.joinToString(" ") { canon(it) }
        fun clean(s: String): String = s.split(" ").filter { it.isNotEmpty() && it !in ARTICLES }.joinToString(" ")
        Regex("^(?:make|match)\\s+(.+?)\\s+(?:like|same as|as)\\s+(.+)$").find(t)?.let {
            return Triple("copy", clean(it.groupValues[1]), clean(it.groupValues[2]))
        }
        Regex("^match\\s+(.+?)\\s+(?:with|to)\\s+(.+)$").find(t)?.let {
            return Triple("copy", clean(it.groupValues[1]), clean(it.groupValues[2]))
        }
        Regex("^(?:turn|switch)\\s+(.+?)\\s+(on|off)$").find(t)?.let {
            return Triple("set", clean(it.groupValues[1]), it.groupValues[2])
        }
        Regex("^(?:change|set|make|put|switch|use|turn)\\s+(.+?)\\s+(?:to|into|as)\\s+(.+)$").find(t)?.let {
            return Triple("set", clean(it.groupValues[1]), clean(it.groupValues[2]))
        }
        Regex("^(.+?)\\s+(?:to|into)\\s+(.+)$").find(t)?.let {
            return Triple("set", clean(it.groupValues[1]), clean(it.groupValues[2]))
        }
        return null
    }

    private val COLOURS = mapOf(
        "red" to 0xFFE53935.toInt(), "orange" to 0xFFFB8C00.toInt(), "yellow" to 0xFFFDD835.toInt(),
        "green" to 0xFF43A047.toInt(), "blue" to 0xFF1E88E5.toInt(), "navy" to 0xFF1A237E.toInt(),
        "purple" to 0xFF8E24AA.toInt(), "violet" to 0xFF7C3AED.toInt(), "pink" to 0xFFEC407A.toInt(),
        "black" to 0xFF000000.toInt(), "white" to 0xFFFFFFFF.toInt(), "grey" to 0xFF9E9E9E.toInt(),
        "gray" to 0xFF9E9E9E.toInt(), "brown" to 0xFF6D4C41.toInt(), "teal" to 0xFF00897B.toInt(),
        "cyan" to 0xFF00ACC1.toInt(), "gold" to 0xFFFFD700.toInt(), "silver" to 0xFFC0C0C0.toInt(),
        "lime" to 0xFF84CC16.toInt(), "indigo" to 0xFF3F51B5.toInt(), "maroon" to 0xFF800000.toInt(),
        "beige" to 0xFFF5F5DC.toInt(), "mint" to 0xFF98FF98.toInt(), "peach" to 0xFFFFCBA4.toInt(),
        "coral" to 0xFFFF7F50.toInt(), "turquoise" to 0xFF40E0D0.toInt()
    )

    /** The colour for a colour word, or null. */
    fun colourOf(word: String): Int? = COLOURS[word.lowercase()]

    /** Buttons that do something to your settings or data (reset, delete, ...) are never pressed by a sentence. */
    fun isDestructive(label: String): Boolean =
        listOf("reset", "delete", "clear", "remove", "forget", "restore", "rebuild", "logout", "sign", "backup", "import", "export", "erase", "undo")
            .any { label.lowercase().contains(it) }

    private fun hasAny(words: List<String>, set: Set<String>): Boolean = words.any { w -> set.any { s -> w == s || (s.length >= 4 && near(w, s)) } }

    /** True when one of [keys] is in [words], allowing a small spelling mistake. */
    fun wordIn(words: List<String>, vararg keys: String): Boolean = words.any { w -> keys.any { k -> w == k || (k.length >= 4 && near(w, k)) } }

    /** The command word a typed word stands for: itself, or the closest known word, when it is a letter or two wrong or sounds the same. */
    fun canon(w: String): String {
        if (w.length < 4 || w in BIG_VOCAB) return w
        var best: String? = null
        var bestD = Int.MAX_VALUE
        for (v in BIG_VOCAB) {
            if (v.length < 4 || !near(w, v)) continue
            val d = lev(w, v)
            if (d < bestD) { bestD = d; best = v }
        }
        if (best != null) return best
        // Sounds the same ("colur" -> "colour", "incres" -> "increase")
        val key = soundKey(w)
        if (key.length >= 3) BIG_VOCAB.firstOrNull { v -> v.length >= 4 && soundKey(v) == key }?.let { return it }
        return w
    }

    /** Plain edit distance between two words. */
    private fun lev(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
        }
        return d[a.length][b.length]
    }

    /** A rough spelling-by-sound key: words that sound alike get the same key. */
    private fun soundKey(w: String): String {
        var s = w.lowercase().filter { it in 'a'..'z' }
        s = s.replace("ph", "f").replace("ck", "k").replace("qu", "kw").replace("x", "ks")
            .replace(Regex("c(?=[eiy])"), "s").replace("c", "k")
            .replace(Regex("(.)\\1+"), "\$1")
        if (s.length > 3 && s.endsWith("e")) s = s.dropLast(1)
        if (s.isEmpty()) return s
        return s.first() + s.drop(1).filter { it !in "aeiouy" }
    }

    /** Every word the helper knows, longer than two letters. Used to correct typed words. */
    private val BIG_VOCAB: List<String> by lazy {
        (VOCAB + BIGGER + SMALLER + OFF + ON + DARK + LIGHT + SEE_THROUGH + SOLID + WIDGET + TEXT + ANIM + ICON_WORDS +
            CHANGE_WORDS + RESET + COLOURS.keys + SLOT_WORDS.map { it.first } + SYN.keys)
            .filter { it.length >= 3 }.distinct().toList()
    }

    private val VOCAB = linkedSetOf(
        "move", "put", "merge", "combine", "unmerge", "split", "separate", "gap", "spacing", "space", "between", "free",
        "width", "wide", "wider", "narrower", "height", "tall", "taller", "shorter", "bigger", "smaller", "larger", "more", "less",
        "increase", "decrease", "raise", "lower", "top", "bottom", "under", "below", "inbox", "first", "start", "hide", "show",
        "before", "after", "left", "right", "up", "down", "undo", "redo", "change", "set", "make", "turn", "switch", "on", "off",
        "to", "into", "as", "with", "like", "same", "of", "transparent", "solid", "opaque", "text", "font", "size", "animation",
        "animations", "dark", "light", "random", "icon", "icons", "picture", "emoji", "normal", "default", "reset", "clock",
        "weather", "date", "maps", "title", "count", "mic", "note", "clear", "edit", "gear", "setting", "settings", "colour",
        "color", "accent", "theme", "widget", "background", "fast", "dial", "shortcut", "shape", "opacity", "through", "night", "day"
    )

    private fun redraw(c: Context) = TilesWidget.rebuildAll(c.applicationContext)

    /** The label of the Settings row that best matches the words of [t] (from [labels], the texts on the Settings screen), or null. */
    /** Words that mean the same thing in a sentence and in a setting name. */
    private val SYN = mapOf(
        "color" to listOf("colour"), "colors" to listOf("colour"), "colours" to listOf("colour"),
        "font" to listOf("text"), "letters" to listOf("text"), "writing" to listOf("text"),
        "picture" to listOf("image", "photo", "pic"), "photo" to listOf("picture"), "pic" to listOf("picture"),
        "space" to listOf("gap", "spacing"), "spacing" to listOf("gap"),
        "width" to listOf("width"), "height" to listOf("height"), "size" to listOf("size"),
        "transparent" to listOf("opacity"), "transparency" to listOf("opacity"), "opacity" to listOf("opacity"),
        "animation" to listOf("animation", "motion"), "motion" to listOf("animation"),
        "mail" to listOf("inbox"), "inbox" to listOf("inbox"), "message" to listOf("message"),
        "clock" to listOf("clock", "time"), "time" to listOf("clock", "time"),
        "background" to listOf("background", "backdrop"), "backdrop" to listOf("background"),
        "accent" to listOf("accent"), "button" to listOf("button"), "buttons" to listOf("button"),
        "theme" to listOf("theme", "look"), "look" to listOf("look"),
        "shortcut" to listOf("shortcut", "fast", "dial"), "dial" to listOf("dial", "fast"), "circle" to listOf("circle", "dial"),
        "shape" to listOf("shape", "square", "round"),
        "shadow" to listOf("shadow", "scrim"), "panel" to listOf("panel", "sheet"),
        "notes" to listOf("note"), "note" to listOf("note"), "mic" to listOf("mic", "recorder"),
        "weather" to listOf("weather"), "date" to listOf("date"), "title" to listOf("title")
    )

    /** A simple plural / verb ending removed: "shortcuts" -> "shortcut", "colouring" -> "colour". */
    private fun stem(w: String): String = when {
        w.length > 5 && w.endsWith("ing") -> w.dropLast(3)
        w.length > 4 && w.endsWith("ed") -> w.dropLast(2)
        w.length > 3 && w.endsWith("s") -> w.dropLast(1)
        else -> w
    }

    /** True when the two words are the same, or differ by a letter or two (longer words allow more). */
    fun near(a: String, b: String): Boolean {
        if (a == b) return true
        // Two letters swapped next to each other ("chnage" for "change") count as one mistake
        if (a.length == b.length && a.length >= 4) {
            val diff = (a.indices).filter { a[it] != b[it] }
            if (diff.size == 2 && diff[1] == diff[0] + 1 && a[diff[0]] == b[diff[1]] && a[diff[1]] == b[diff[0]]) return true
        }
        val max = if (a.length >= 8) 2 else if (a.length >= 4) 1 else 0
        if (max == 0 || Math.abs(a.length - b.length) > max) return false
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i
        for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
        }
        return d[a.length][b.length] <= max
    }

    private fun wordMatches(key: String, labelWords: List<String>): Boolean = labelWords.any { lw ->
        lw == key || (key.length >= 4 && lw.startsWith(key)) || (lw.length >= 4 && key.startsWith(lw)) || near(lw, key)
    }

    /**
     * The label of the setting that best matches the words of [t], from [labels] (the texts on the Settings screen), or null.
     * Only short setting names are candidates: section descriptions and sentences are skipped.
     */
    fun bestLabel(t: String, labels: List<String>): String? {
        val keys = LinkedHashSet<String>()
        for (w in t.split(Regex("[^a-z]+")).filter { it.length >= 3 && it !in STOP }) {
            val st = stem(w)
            keys.add(st)
            SYN[w]?.let { keys.addAll(it) }
            SYN[st]?.let { keys.addAll(it) }
        }
        if (keys.isEmpty()) return null
        var best: String? = null
        var bestScore = 0
        for (raw in labels) {
            val line = raw.lineSequence().firstOrNull()?.trim() ?: continue
            if (line.isEmpty() || line.length > 40 || line.contains(".") || line.contains(":")) continue
            if (line.startsWith("▸") || line.startsWith("▾")) continue
            val lw = line.lowercase().split(Regex("[^a-z]+")).filter { it.length >= 2 }.map { stem(it) }
            val score = keys.count { wordMatches(it, lw) }
            if (score == 0) continue
            val b = best
            if (score > bestScore || (score == bestScore && b != null && line.length < b.length)) {
                best = line
                bestScore = score
            }
        }
        return best ?: similarLabel(t, labels)
    }

    /** Letter triples of a word, with a space at each end: "font" -> "  f", " fo", "fon", "ont", "nt ". */
    private fun triples(w: String): Set<String> {
        val p = " $w "
        return (0..p.length - 3).map { p.substring(it, it + 3) }.toSet()
    }

    /** How alike two words look, 0 to 1, from the letter triples they share (spelling mistakes keep most of them). */
    private fun alike(a: String, b: String): Double {
        val ta = triples(a)
        val tb = triples(b)
        return 2.0 * ta.intersect(tb).size / (ta.size + tb.size)
    }

    /**
     * The setting whose words look most like the words of [t], even when spelled quite wrong ("fonst siz" -> "Font size").
     * Each typed word is matched to its closest word in the setting name; the average must be high enough.
     */
    private fun similarLabel(t: String, labels: List<String>): String? {
        val words = t.split(Regex("[^a-z]+")).filter { it.length >= 3 && it !in STOP }
        if (words.isEmpty()) return null
        var best: String? = null
        var bestScore = 0.0
        for (raw in labels) {
            val line = raw.lineSequence().firstOrNull()?.trim() ?: continue
            if (line.isEmpty() || line.length > 40 || line.contains(".") || line.contains(":")) continue
            if (line.startsWith("▸") || line.startsWith("▾")) continue
            val lw = line.lowercase().split(Regex("[^a-z]+")).filter { it.length >= 2 }
            if (lw.isEmpty()) continue
            val score = words.sumOf { w -> lw.maxOf { alike(w, it) } } / words.size
            if (score > bestScore) { best = line; bestScore = score }
        }
        return if (bestScore >= 0.6) best else null
    }

    fun handle(c: Context, input: String, labels: List<String> = emptyList()): Result {
        val t = input.lowercase().trim()
        if (t.isEmpty()) return Result("Type what you want to change, for example: make the widget more transparent.", false, "")
        // Words, with "see-through" and "see through" both read as "see" + "through"
        val words = t.replace("see-through", "seethrough").replace("see through", "seethrough")
            .split(Regex("[^a-z]+")).filter { it.isNotEmpty() }.map { if (it == "seethrough") "transparent" else canon(it) }

        // A theme by its name (whole word, so "think" does not pick Ink)
        val named = Themes.LIST.drop(1).firstOrNull { th -> wordIn(words, th.name.lowercase()) && th.name.length >= 4 }
        if (named != null) {
            Themes.apply(c, named)
            redraw(c)
            return Result("Theme set to ${named.name}.", true, "")
        }
        if (hasAny(words, setOf("random", "surprise"))) {
            val th = Themes.LIST.drop(1).random()
            Themes.apply(c, th)
            redraw(c)
            return Result("Random theme: ${th.name}.", true, "")
        }
        // Widget background opacity
        val negated = hasAny(words, NEGATION)
        if (hasAny(words, WIDGET) && hasAny(words, SEE_THROUGH) && !negated) {
            Prefs.setWidgetAlpha(c, 40)
            redraw(c)
            return Result("Widget background is now 40% opaque.", true, "")
        }
        if (hasAny(words, WIDGET) && (hasAny(words, SOLID) || (hasAny(words, SEE_THROUGH) && negated))) {
            Prefs.setWidgetAlpha(c, 100)
            redraw(c)
            return Result("Widget background is now 100% opaque.", true, "")
        }
        // Text size: "text" with a direction, or "normal" for the reset
        if (hasAny(words, TEXT) && hasAny(words, RESET) && !hasAny(words, BIGGER) && !hasAny(words, SMALLER)) {
            Prefs.setFontScale(c, 100)
            redraw(c)
            return Result("Text size is back to 100%.", true, "")
        }
        if (hasAny(words, TEXT) && hasAny(words, BIGGER)) {
            val v = (Prefs.fontScale(c) + 15).coerceAtMost(160)
            Prefs.setFontScale(c, v)
            redraw(c)
            return Result("Text size is now $v%.", true, "")
        }
        if (hasAny(words, TEXT) && hasAny(words, SMALLER)) {
            val v = (Prefs.fontScale(c) - 15).coerceAtLeast(70)
            Prefs.setFontScale(c, v)
            redraw(c)
            return Result("Text size is now $v%.", true, "")
        }
        // Animations on or off
        if (hasAny(words, ANIM) && hasAny(words, OFF)) {
            Prefs.setAliveOn(c, false)
            redraw(c)
            return Result("Animations are off.", true, "")
        }
        if (hasAny(words, ANIM) && hasAny(words, ON)) {
            Prefs.setAliveOn(c, true)
            redraw(c)
            return Result("Animations are on.", true, "")
        }
        // "Remove the word from the notes icon": the Notes button shows its emoji alone, with no word
        if (hasAny(words, setOf("word", "words", "text", "label")) && hasAny(words, setOf("note", "notes")) &&
            hasAny(words, setOf("remove", "without", "no", "take", "off", "only", "just", "hide", "drop"))) {
            if (Prefs.emojiOf(c, "note").isEmpty()) Prefs.setEmojiOf(c, "note", Look.EMOJI_NOTE.first())
            redraw(c)
            return Result("The Notes button now shows only its icon, ${Prefs.emojiOf(c, "note")}, with no word.", true, "")
        }
        // Emoji icon of one place (Inbox, Notes, Mic, Reset, Pencil, Gear)
        val slot = if (t.contains("clear all")) "clear" else SLOT_WORDS.firstOrNull { words.contains(it.first) }?.second
        if (slot != null && (hasAny(words, ICON_WORDS) || hasAny(words, CHANGE_WORDS) || firstEmoji(input) != null)) {
            val label = EmojiPicker.SLOTS.first { it.key == slot }.label
            val e = firstEmoji(input)
            if (e != null) {
                Prefs.setEmojiOf(c, slot, e)
                redraw(c)
                return Result("$label icon is now $e.", true, "")
            }
            if (hasAny(words, setOf("normal", "default", "standard", "original"))) {
                Prefs.setEmojiOf(c, slot, "")
                redraw(c)
                return Result("$label icon is back to the normal one.", true, "")
            }
            return Result("Pick the new icon for $label from the list.", false, "", slot)
        }
        // Whole look
        if (hasAny(words, DARK)) {
            Themes.apply(c, Themes.LIST.first { it.name == "Midnight" })
            redraw(c)
            return Result("Dark look: Midnight theme.", true, "")
        }
        if (hasAny(words, LIGHT)) {
            Themes.apply(c, Themes.LIST.first { it.name == "Light" })
            redraw(c)
            return Result("Light look: Light theme.", true, "")
        }

        // Not understood as a change: find the Settings row whose name matches the words best and open it
        val row = bestLabel(t, labels)
        if (row != null) return Result("", false, row, null, row)
        return Result("I did not find a setting for that. Try other words, or use the search box at the top of Settings.", false, "")
    }
}
