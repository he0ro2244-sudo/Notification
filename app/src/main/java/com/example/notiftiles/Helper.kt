package com.example.notiftiles

import android.content.Context

/**
 * The offline helper. It reads a sentence for what it is about (the object: text, widget, animations) and what to do with it
 * (the direction: bigger, less, off, a theme name). No internet and no AI model: it knows the words listed here, so it
 * understands many ordinary sentences but not every one. What it does not understand is looked up in Settings instead.
 */
object Helper {
    /** [message] is shown to the person, [changed] says a setting was changed, [search] is the word to look for in Settings. */
    class Result(val message: String, val changed: Boolean, val search: String)

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
    private val NEGATION = setOf("less", "not", "no", "never", "solid", "opaque")
    private val RESET = setOf("normal", "default", "standard", "reset", "original", "back")

    private fun hasAny(words: List<String>, set: Set<String>): Boolean = words.any { set.contains(it) }

    private fun redraw(c: Context) = TilesWidget.rebuildAll(c.applicationContext)

    fun handle(c: Context, input: String): Result {
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

        // Not understood: look for the first useful word in Settings
        val search = words.firstOrNull { it.length > 2 && it !in STOP } ?: ""
        return Result("I did not change anything. Showing Settings that match \"$search\".", false, search)
    }
}
