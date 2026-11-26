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
    private fun firstEmoji(input: String): String? {
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

    private fun hasAny(words: List<String>, set: Set<String>): Boolean = words.any { set.contains(it) }

    private fun redraw(c: Context) = TilesWidget.rebuildAll(c.applicationContext)

    /** The label of the Settings row that best matches the words of [t] (from [labels], the texts on the Settings screen), or null. */
    private fun bestLabel(t: String, labels: List<String>): String? {
        val keys = t.split(Regex("[^a-z]+")).filter { it.length >= 3 && it !in STOP }
        if (keys.isEmpty()) return null
        var best: String? = null
        var bestScore = 0
        for (raw in labels) {
            val l = raw.lowercase()
            val score = keys.count { l.contains(it) }
            if (score == 0) continue
            val b = best
            if (score > bestScore || (score == bestScore && b != null && raw.length < b.length)) {
                best = raw
                bestScore = score
            }
        }
        return best?.lineSequence()?.first()?.trim()
    }

    fun handle(c: Context, input: String, labels: List<String> = emptyList()): Result {
        val t = input.lowercase().trim()
        if (t.isEmpty()) return Result("Type what you want to change, for example: make the widget more transparent.", false, "")
        // Words, with "see-through" and "see through" both read as "see" + "through"
        val words = t.replace("see-through", "seethrough").replace("see through", "seethrough")
            .split(Regex("[^a-z]+")).filter { it.isNotEmpty() }.map { if (it == "seethrough") "transparent" else it }

        // A theme by its name (whole word, so "think" does not pick Ink)
        val named = Themes.LIST.drop(1).firstOrNull { th -> words.contains(th.name.lowercase()) }
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
