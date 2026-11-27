package com.example.notiftiles

import android.content.Context

/**
 * Teach the helper your own words, on this phone only.
 *  - "learn <your words> means <what to do>" (also "teach ...", "remember ...", "when I say X do Y")
 *    saves it. Later, typing <your words> runs <what to do>.
 *  - "forget <your words>" removes one taught phrase; "my words" lists them all.
 * Example: learn make it pop means make the widget more solid
 */
object Teach {
    private const val KEY = "teach_map"

    /** A phrase in its plain form: lower case, letters and digits only, single spaces. */
    private fun norm(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9 ]+"), " ").replace(Regex("\\s+"), " ").trim()

    private fun load(c: Context): LinkedHashMap<String, String> {
        val m = LinkedHashMap<String, String>()
        val raw = Prefs.store(c).getString(KEY, "") ?: ""
        for (line in raw.split("\n")) {
            val i = line.indexOf('\t')
            if (i > 0) m[line.substring(0, i)] = line.substring(i + 1)
        }
        return m
    }

    private fun save(c: Context, m: Map<String, String>) {
        Prefs.store(c).edit().putString(KEY, m.entries.joinToString("\n") { it.key + "\t" + it.value }).apply()
    }

    private val LEARN = Regex("^(?:learn|teach|remember)\\s+(?:that\\s+)?(.+?)\\s+(?:means|=|is)\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val WHEN = Regex("^when\\s+i\\s+(?:say|type)\\s+(.+?)\\s+(?:do|then)\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val FORGET = Regex("^forget\\s+(.+)$", RegexOption.IGNORE_CASE)
    private val LIST_WORDS = setOf("my words", "list my words", "show my words", "what did i teach you")

    /** Handles a teaching command. Returns the message to show, or null when [input] is not one. */
    fun handle(c: Context, input: String): String? {
        val t = input.trim()
        val teach = LEARN.find(t) ?: WHEN.find(t)
        if (teach != null) {
            val phrase = norm(teach.groupValues[1])
            val cmd = teach.groupValues[2].replace(Regex("[\t\n]"), " ").trim()
            if (phrase.isEmpty() || cmd.isEmpty()) return "Say it like: learn <your words> means <what to do>."
            val m = load(c)
            m[phrase] = cmd
            save(c, m)
            return "Learned: \"$phrase\" now means \"$cmd\"."
        }
        FORGET.find(t)?.let { f ->
            val phrase = norm(f.groupValues[1])
            val m = load(c)
            if (m.remove(phrase) == null) return "I had no taught words called \"$phrase\"."
            save(c, m)
            return "Forgot \"$phrase\"."
        }
        if (norm(t) in LIST_WORDS) {
            val m = load(c)
            return if (m.isEmpty()) "You have not taught me any words yet. Try: learn make it pop means make the widget more solid"
            else m.entries.joinToString("\n") { "\"${it.key}\" means \"${it.value}\"" }
        }
        return null
    }

    /** The command taught for [input], or null. Tries the words as typed, then with spelling mistakes fixed. */
    fun lookup(c: Context, input: String): String? {
        val m = load(c)
        if (m.isEmpty()) return null
        return m[norm(input)] ?: m[norm(Helper.fixTypos(input, emptyList()))]
    }
}
