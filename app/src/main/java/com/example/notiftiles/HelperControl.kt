package com.example.notiftiles

import android.content.Context

/**
 * Lets the helper change any setting in the list [HelperSettings.ENTRIES] by its name:
 *   "set the dial size to 50", "make the text size bigger", "turn the panel border off", "make the accent red",
 *   "panel background colour black", "make the widget more transparent".
 * It finds the setting whose name words are all in the sentence (small spelling mistakes are forgiven), then reads what to do:
 * a number, a bigger / smaller word, on / off, or a colour. When it is not sure it says nothing and the other helper rules go on.
 */
object HelperControl {
    private val UP = setOf("bigger", "larger", "more", "increase", "up", "wider", "taller", "raise", "higher", "grow", "enlarge", "longer", "greater")
    private val DOWN = setOf("smaller", "less", "decrease", "down", "lower", "shrink", "reduce", "narrower", "shorter", "tiny", "fewer", "thinner")
    private val TOP = setOf("maximum", "biggest", "largest", "highest", "max", "full")
    private val BOTTOM = setOf("minimum", "smallest", "lowest", "min")
    private val AUTO = setOf("auto", "automatic", "default", "normal", "standard", "original", "reset")

    private fun norm(w: String): String {
        var s = w.lowercase()
        s = when (s) {
            "colour", "colours", "colors" -> "color"
            "centre" -> "center"
            "microphone" -> "mic"
            "transparency" -> "transparent"
            else -> s
        }
        if (s.length > 3 && s.endsWith("s") && !s.endsWith("ss")) s = s.dropLast(1)
        return s
    }

    private fun tokens(text: String): List<String> =
        Regex("[a-z0-9]+").findAll(text.lowercase()).map { norm(it.value) }.toList()

    private fun phraseIn(sentence: List<String>, phrase: String): Boolean =
        tokens(phrase).all { p -> sentence.any { s -> s == p || (p.length >= 5 && s.length >= 5 && Helper.near(s, p)) } }

    private class Hit(val entry: HelperSettings.Entry, val inverse: Boolean, val score: Int)

    private fun find(sentence: List<String>): Hit? {
        var best: Hit? = null
        for (e in HelperSettings.ENTRIES) {
            for (p in e.phrases) {
                val n = tokens(p).size
                if (phraseIn(sentence, p) && (best == null || n > best.score)) best = Hit(e, false, n)
            }
            for (p in e.inverse) {
                val n = tokens(p).size
                if (phraseIn(sentence, p) && (best == null || n > best.score)) best = Hit(e, true, n)
            }
        }
        return best
    }

    private fun parseHex(input: String): Int? {
        val m = Regex("#([0-9a-fA-F]{6})\\b").find(input) ?: return null
        return (0xFF000000L or m.groupValues[1].toLong(16)).toInt()
    }

    private fun colourName(argb: Int, input: String): String {
        val w = input.lowercase().split(Regex("[^a-z]+")).firstOrNull { Helper.colourOf(it) == argb }
        return w ?: String.format("#%06X", argb and 0xFFFFFF)
    }

    /** Short list of what the helper can do. */
    const val HELP = "I can: change any setting by name (\"dial size 50\", \"turn the panel border off\", \"accent red\"), " +
        "add or remove fast dials (\"add fast dial Mom 0501234567\") and shortcuts (\"add shortcut WhatsApp\"), " +
        "tidy the rows like the pencil's Fix layout (\"tidy the spaces between rows\"), merge, move and resize items, " +
        "change themes and icons (also the Where to? pin: \"change the pin icon to a car\"), add a note, and undo or redo. " +
        "I also understand everyday wishes (\"the text is too small\", \"it is too bright\", \"too crowded\", \"I am bored of this look\") " +
        "and several changes in one sentence (\"make the text bigger and turn animations off\")."

    fun handle(c: Context, input: String): Helper.Result? {
        val raw = input.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }
        if (raw.isNotEmpty() && raw.size <= 4 && (raw.contains("help") || (raw.contains("what") && raw.contains("do")))) {
            return Helper.Result(HELP, false, "")
        }
        // "make X like Y" is handled by the copy command, not here
        if (Helper.parseSentence(input)?.first == "copy") return null
        val sentence = tokens(input)
        val hit = find(sentence) ?: return null
        val e = hit.entry
        val label = e.label

        when (e.kind) {
            HelperSettings.BOOL -> {
                val on = Helper.onOffIn(input) ?: return null
                HelperSettings.setBool(c, e.id, on)
                TilesWidget.rebuildAll(c.applicationContext)
                return Helper.Result("$label is now ${if (on) "on" else "off"}.", true, "")
            }
            HelperSettings.INT -> {
                val cur = HelperSettings.getInt(c, e.id)
                val n = Regex("(?<![a-zA-Z0-9#])\\d+").find(input)?.value?.toIntOrNull()
                var up = raw.any { it in UP }
                var down = raw.any { it in DOWN }
                if (hit.inverse) { val t = up; up = down; down = t }
                val v = when {
                    n != null -> n
                    raw.any { it in TOP } -> if (hit.inverse) e.min else e.max
                    raw.any { it in BOTTOM } -> if (hit.inverse) e.max else e.min
                    up && !down -> cur + e.step
                    down && !up -> cur - e.step
                    else -> return null
                }.coerceIn(e.min, e.max)
                HelperSettings.setInt(c, e.id, v)
                TilesWidget.rebuildAll(c.applicationContext)
                return Helper.Result("$label is now $v.", true, "")
            }
            else -> {
                val argb = parseHex(input) ?: raw.firstNotNullOfOrNull { Helper.colourOf(it) }
                if (argb != null) {
                    HelperSettings.setInt(c, e.id, argb)
                    TilesWidget.rebuildAll(c.applicationContext)
                    return Helper.Result("$label is now ${colourName(argb, input)}.", true, "")
                }
                if (raw.any { it in AUTO }) {
                    // The accent has no automatic setting: its normal blue is put back
                    val back = if (e.id == "accent") 0xFF3B82F6.toInt() else 0
                    HelperSettings.setInt(c, e.id, back)
                    TilesWidget.rebuildAll(c.applicationContext)
                    return Helper.Result("$label is back to ${if (back == 0) "automatic" else "the normal colour"}.", true, "")
                }
                return null
            }
        }
    }
}
