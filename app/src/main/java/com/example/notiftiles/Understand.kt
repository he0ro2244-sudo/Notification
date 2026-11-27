package com.example.notiftiles

import android.content.Context

/**
 * Helps the offline helper understand more ways of saying things. It runs BEFORE the older rules and the offline model, and only ever
 * turns what was said into a plainer sentence they already know, so nothing that worked before is taken away:
 *  - [parts]: one message with several changes ("make the text bigger and turn animations off") becomes one sentence per change;
 *  - [rewrite]: politeness and filler are dropped ("could you please ..., thanks"), number words become numbers ("fifty"),
 *    other words for the same thing are matched ("speed dial", "wallpaper", "night mode"), and feelings become changes
 *    ("the text is too small" -> bigger text, "too bright" -> dark look, "it is boring" -> a random theme, "too crowded" -> more gap);
 *  - [pinIcon]: changes the icon of the Where to? box ("change the pin icon to a car", "use the red pin", "use the Maps icon").
 * Notes are never changed: everything after "add a note" stays exactly as typed.
 */
object Understand {
    private val IC = RegexOption.IGNORE_CASE

    // ------------------------------------------------------------------ one message, several changes

    /** Words that start a new change. A sentence is only cut where one of these follows "and", "then", "also", a comma or a semicolon. */
    private val VERBS = setOf(
        "make", "turn", "set", "change", "add", "remove", "delete", "hide", "show", "put", "use", "increase", "decrease", "switch",
        "move", "merge", "tidy", "fix", "reset", "undo", "redo", "enable", "disable", "stop", "start", "pick", "choose", "give",
        "let", "get", "lower", "raise", "bring", "clear", "rename", "unmerge", "split", "match", "paint", "colour", "color"
    )

    private val NOTE_ADD = Regex("""^\s*(?:please\s+)?(?:(?:add|new|create|write|save)\s+(?:a\s+|new\s+)?(?:quick\s+)?notes?\b|note\s*[:\-])""", IC)

    private val SEPARATOR = Regex("""(?:,\s*)?(?:\band\s+then\b|\band\s+also\b|\band\b|\bthen\b|\balso\b)\s+|\s*[;&,]\s*""", IC)

    /** The separate changes inside [input] (just [input] when it holds one). */
    fun parts(input: String): List<String> {
        val text = input.trim()
        if (text.isEmpty() || NOTE_ADD.containsMatchIn(text)) return listOf(text)
        val pieces = ArrayList<String>()
        var start = 0
        for (m in SEPARATOR.findAll(text)) {
            if (m.range.first < start) continue
            val after = text.substring(m.range.last + 1).trim()
            val first = after.split(Regex("""\s+""")).firstOrNull()?.lowercase()?.replace(Regex("[^a-z]"), "") ?: ""
            if (first in VERBS) {
                val piece = text.substring(start, m.range.first).trim()
                if (piece.isNotEmpty()) pieces.add(piece)
                start = m.range.last + 1
            }
        }
        val last = text.substring(start).trim()
        if (last.isNotEmpty()) pieces.add(last)
        return if (pieces.isEmpty()) listOf(text) else pieces
    }

    // ------------------------------------------------------------------ plainer words

    private val PREFIXES = listOf(
        Regex("""^(?:hey|hi|hello|hiya|ok|okay|yo|so|well|um|uh|oh|helper|assistant)\b[\s,!.:;\-]*""", IC),
        Regex("""^(?:please|pls|plz|kindly|just|simply|actually|maybe|perhaps)\b[\s,]*""", IC),
        Regex("""^(?:could|can|would|will|may)\s+you\s+(?:please\s+|just\s+|kindly\s+)?""", IC),
        Regex("""^(?:go\s+ahead\s+and|try\s+to|try\s+and|help\s+me\s+to|help\s+me|let\s+me|let's|lets|let\s+us)\s+""", IC),
        // "I want to change ...", "I'd like you to ..."
        Regex("""^i(?:'d|\s+would|\s+will)?\s+(?:like|want|need|wish|prefer|have|got)\s+(?:you\s+to|me\s+to|to)\s+""", IC),
        Regex("""^i\s+(?:wanna|gotta|am\s+trying\s+to|am\s+going\s+to|m\s+trying\s+to)\s+""", IC),
        // "how do I make the text bigger", "where is the font size"
        Regex("""^(?:how|where)\s+(?:do|can|could|should|would)\s+i\s+""", IC),
        Regex("""^(?:how\s+to|how\s+can\s+we|where\s+is|where's|where\s+are|whats|what's|what\s+is|what\s+are|find\s+me|take\s+me\s+to|go\s+to|search\s+for)\s+(?:the\s+)?""", IC)
    )

    // "I want the text bigger" -> "make the text bigger"; "I like blue" -> "blue"
    private val WANT = Regex("""^i(?:'d|\s+would)?\s+(?:like|want|need|prefer|wish|love)\s+(?:for\s+)?""", IC)
    private val NOUN_START = Regex("""^(?:the|my|it|a|an|everything|all|this|that|these|those)\b""", IC)

    private val SUFFIX = Regex("""(?:[\s,.!?]+(?:please|pls|plz|thanks|thank\s+you|thx|for\s+me|now|right\s+now|ok|okay|asap|again))+[\s.!?]*$""", IC)
    private val TAIL = Regex("""[\s.!?]+$""")

    private fun stripFiller(input: String): String {
        var t = input.trim()
        for (pass in 0 until 4) {
            val before = t
            for (p in PREFIXES) t = p.replaceFirst(t, "")
            val w = WANT.find(t)
            if (w != null) {
                val rest = t.substring(w.range.last + 1)
                t = if (NOUN_START.containsMatchIn(rest)) "make $rest" else rest
            }
            t = SUFFIX.replace(t, "")
            t = TAIL.replace(t, "")
            t = t.trim()
            if (t == before) break
        }
        return t
    }

    /** Typed words are matched to the few long words the feelings below depend on ("transparant", "anoying"). */
    private val DICT = listOf(
        "transparent", "animation", "animations", "annoying", "distracting", "unreadable", "readable", "crowded", "cluttered",
        "cramped", "squeezed", "boring", "brighter", "blinding", "wallpaper", "background", "bigger", "smaller", "larger", "widget"
    )

    private fun fixTypos(t: String): String = Regex("""[A-Za-z]{6,}""").replace(t) { m ->
        val lw = m.value.lowercase()
        if (lw in DICT) m.value else (DICT.firstOrNull { d -> Helper.near(lw, d) } ?: m.value)
    }

    private val SWAPS: List<Pair<Regex, String>> = listOf(
        Regex("""\b(?:speed\s*dials?|quick\s*dials?)\b""", IC) to "fast dial",
        Regex("""\bwallpaper\b""", IC) to "background",
        Regex("""\b(?:tint|shade|hue)\b""", IC) to "colour",
        Regex("""\b(?:night|dark)\s+(?:mode|theme)\b""", IC) to "dark look",
        Regex("""\b(?:day|light|bright)\s+(?:mode|theme)\b""", IC) to "light look",
        Regex("""\b(?:cog\s*wheel|gear\s*wheel|settings\s+wheel|cog)\b""", IC) to "gear",
        Regex("""\b(?:zoom\s+in|blow\s+up|magnify)\b""", IC) to "bigger",
        Regex("""\bzoom\s+out\b""", IC) to "smaller",
        Regex("""\b(?:boost|amplify|pump\s+up|crank\s+up|ramp\s+up|turn\s+up)\b""", IC) to "increase",
        Regex("""\b(?:tone\s+down|dial\s+down|cut\s+down|scale\s+down|turn\s+down)\b""", IC) to "decrease"
    )

    private val TENS = mapOf("twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90)
    private val TEENS = mapOf("ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14, "fifteen" to 15,
        "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19)
    private val ONES = mapOf("one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9)
    private val KEEP_WORDS = Regex("""\b(?:add|new|rename|name|named|called|call|note|notes|contact)\b""", IC)

    /** "fifty" -> 50, "twenty five" -> 25, "half" -> 50, "50 percent" -> 50. Sentences that add or name a dial, shortcut or note keep their words. */
    private fun numberWords(t: String): String {
        var s = Regex("""\b(\d+)\s*(?:percent|per\s+cent|%)""", IC).replace(t) { it.groupValues[1] }
        if (KEEP_WORDS.containsMatchIn(s)) return s
        s = Regex("""\b(?:(?:one|a)\s+)?hundred\b""", IC).replace(s, "100")
        s = Regex("""\b(twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)(?:[\s\-]+(one|two|three|four|five|six|seven|eight|nine))?\b""", IC).replace(s) { m ->
            val tens = TENS[m.groupValues[1].lowercase()] ?: 0
            val ones = if (m.groupValues[2].isEmpty()) 0 else (ONES[m.groupValues[2].lowercase()] ?: 0)
            (tens + ones).toString()
        }
        s = Regex("""\b(ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen)\b""", IC).replace(s) { m ->
            (TEENS[m.groupValues[1].lowercase()] ?: 0).toString()
        }
        s = Regex("""\b(to|at|by)\s+half\b""", IC).replace(s) { m -> m.groupValues[1] + " 50" }
        return s
    }

    /** "black" and "white" alone keep their old meaning (the dark / light look). */
    private val PLAIN = setOf("black", "white")

    private const val ITEMS = """(?:shortcuts?(?:\s+row)?|fast\s+dials?|dials?|clock|weather|date|where\s+to|maps?|inbox|pencil|mic|microphone|gear|refresh|clear\s+all|notes?(?:\s+button)?|title|count|badge)"""

    /**
     * Feelings and everyday wishes as the plain change they mean, or null. Only whole, clear complaints are turned into a change:
     * "make the text small" is still a command for smaller text, while "the text is too small" asks for bigger text.
     */
    private fun feeling(t: String): String? {
        val low = t.lowercase().replace("’", "'")
        fun has(p: String) = Regex(p, IC).containsMatchIn(low)
        val textish = has("""\b(?:text|texts|font|fonts|writing|letters?|words|labels?|print|numbers?)\b""")
        val widgetish = has("""\b(?:widget|background|home\s*screen|tiles?|panel)\b""")
        val animish = has("""\b(?:animations?|animated|motion|moving|movement|moves|effects?|wobbl\w*|bounc\w*|spinn?\w*)\b""")

        // undo / redo said in everyday words (whole sentence only)
        if (Regex("""^(?:go\s+back|revert(?:\s+(?:that|it))?|take\s+(?:it|that)\s+back|put\s+(?:it|that)\s+back|cancel\s+that|oops+|never\s?mind|that\s+was\s+wrong|not\s+like\s+that|i\s+(?:do\s+not|don't|dont)\s+like\s+(?:it|that)|bring\s+(?:it|that)\s+back)[.!\s]*$""", IC).containsMatchIn(low)) return "undo"
        if (Regex("""^(?:do\s+(?:it|that)\s+again|go\s+forward)[.!\s]*$""", IC).containsMatchIn(low)) return "redo"

        // text too small / too big / hard to read
        if (textish && has("""\btoo\s+(?:small|tiny|little|weak)\b|\b(?:is|are|looks?|seems?)\s+(?:so\s+|really\s+|very\s+)?(?:small|tiny)\b|\b(?:can'?t|cannot|can\s+not|couldn'?t|unable\s+to|hard\s+to|difficult\s+to|impossible\s+to|trouble)\s+(?:\w+\s+){0,2}?(?:read|see)\b|\bsquint\w*|\bunreadable\b|\bnot\s+readable\b""")) return "make the text size bigger"
        if (!textish && has("""\b(?:can'?t|cannot|can\s+not|hard\s+to|difficult\s+to)\s+(?:read|see)\s+(?:anything|a\s+thing|it|this|them|that)\b|\bhard\s+to\s+read\b|\bunreadable\b|\bsquint\w*""")) return "make the text size bigger"
        if (textish && has("""\btoo\s+(?:big|large|huge|giant|massive)\b|\b(?:is|are|looks?|seems?)\s+(?:so|too|really|very|way)\s+(?:big|large|huge|giant|massive)\b""")) return "make the text size smaller"
        if (has("""\b(?:easier\s+to\s+read|more\s+readable|readab\w+|more\s+legible|legib\w+)\b""")) return "make the text size bigger"
        val ev = Regex("""^(?:make\s+)?(?:everything|all\s+of\s+it|it\s+all|the\s+whole\s+thing)\s+(?:a\s+bit\s+|a\s+little\s+|much\s+|slightly\s+)?(bigger|larger|more\s+visible|clearer|smaller)\s*$""", IC).find(low)
        if (ev != null) return if (ev.groupValues[1].startsWith("smaller")) "make the text size smaller" else "make the text size bigger"

        // light and dark
        if (has("""\btoo\s+(?:bright|white|shiny)\b|\bblinding\b|\bglar\w*|\bhurts?\s+(?:my\s+)?eyes?\b|\beyes?\s+(?:hurt|burn|ache)s?\b|\bburns?\s+(?:my\s+)?eyes?\b|\bso\s+bright\b""")) return "dark look"
        if (has("""\btoo\s+(?:dark|gloomy|black|dim)\b|\bso\s+dark\b""")) return "light look"

        // widget background: wants to see the wallpaper / wants the widget to stand out
        if (has("""\btoo\s+(?:solid|opaque|heavy|thick|dense|blocky)\b|\b(?:blocks?|hides?|covers?|covering|hiding|blocking)\s+(?:my\s+|the\s+)?(?:wallpaper|background)\b|\bcan'?t\s+see\s+(?:my\s+|the\s+)?(?:wallpaper|background)\b""")) return "make the widget transparent"
        if (widgetish && has("""\btoo\s+(?:transparent|see[\s\-]?through|faded|invisible|ghosty|clear|weak)\b|\bwashed\s+out\b|\bblend(?:s|ing)?\s+(?:in|into)\b|\bdisappear\w*|\b(?:can'?t|cannot|hard\s+to|difficult\s+to)\s+see\b""")) return "make the widget solid"

        // animations
        if (animish) {
            if (has("""\btoo\s+(?:fast|quick)\b|\bso\s+fast\b|\brushed\b|\bfrantic\b""")) return "make the animation speed smaller"
            if (has("""\btoo\s+slow\b|\bso\s+slow\b|\bsluggish\b""")) return "make the animation speed bigger"
            if (has("""\btoo\s+(?:much|many|busy|strong|wild|crazy)\b|\bannoy\w*|\bdistract\w*|\bdizz\w*|\bnause\w*|\btired\s+of\b|\bsick\s+of\b|\bhate\b|\blagg?\w*|\bslows?\s+(?:down\s+)?my\s+phone\b|\bdrains?\s+(?:my\s+)?battery\b|\benough\b|\bno\s+more\b""")) return "turn animations off"
        }
        if (has("""\btoo\s+busy\b|\btoo\s+much\s+(?:movement|motion)\b|\beverything\s+(?:is\s+)?(?:moving|moves|jumping|jumps|wobbl\w*)\b""")) return "turn animations off"

        // spacing and order
        if (has("""\b(?:crowded|cluttered|cramped|squashed|squeezed|jammed|packed\s+together|too\s+(?:tight|close|packed|dense)|touching\s+each\s+other|stuck\s+together|no\s+space)\b""")) return "make the row gap bigger"
        if (has("""\b(?:untidy|uneven|unaligned|misaligned|not\s+aligned|not\s+(?:straight|even|neat)|crooked|messy|overlap\w*|jumbled|all\s+over\s+the\s+place)\b""")) return "tidy the spaces between rows"
        if (has("""\btoo\s+(?:far\s+apart|spread(?:\s+out)?|loose|spaced)\b|\btoo\s+much\s+(?:space|spacing|gap|gaps)\b|\bgaps?\s+(?:is\s+|are\s+)?too\s+(?:big|large|wide|much)\b|\btoo\s+many\s+gaps\b""")) return "make the row gap smaller"

        // wants something new to look at
        if (has("""\b(?:boring|dull|bland|ugly|plain|stale|meh|lifeless|outdated|old[\s\-]looking)\b|\blooks?\s+old\b|\bnew\s+(?:look|theme|style)\b|\bfresh\s+(?:look|theme)\b|\bsomething\s+(?:new|different|fresh|nice|pretty|cool|fun)\b|\bsurprise\s+me\b|\bchange\s+(?:the\s+)?(?:look|style|theme)\b|\b(?:different|another|other)\s+(?:look|theme|style)\b|\bspice\s+(?:it\s+)?up\b|\bfreshen\w*|\bmake\s+it\s+(?:pretty|prettier|nicer|cooler|fancy|fancier|beautiful)\b|\bbeautif\w+|\bprettier\b""")) return "random theme"

        // a colour wish: "make it blue", "I like red", "blue please" -> the accent colour
        Regex("""^(?:make|paint|colou?r|turn|set)\s+(?:it|everything|the\s+look|the\s+app|the\s+buttons?|buttons?|all)\s+(?:look\s+|to\s+be\s+|into\s+|to\s+)?([a-z]+)\s*$""", IC).find(low)?.let { m ->
            if (Helper.colourOf(m.groupValues[1]) != null && m.groupValues[1].lowercase() !in PLAIN) return "accent " + m.groupValues[1]
        }
        Regex("""^(?:(?:i\s+)?(?:like|love|prefer)\s+|use\s+|colou?r\s+)?(?:the\s+|a\s+|some\s+)?(?:colou?r\s+)?([a-z]+)(?:\s+(?:colou?r|theme|accent|one))?\s*$""", IC).find(low)?.let { m ->
            if (Helper.colourOf(m.groupValues[1]) != null && m.groupValues[1].lowercase() !in PLAIN) return "accent " + m.groupValues[1]
        }

        // hide / show a part in everyday words
        Regex("""^(?:get\s+rid\s+of|ditch|lose|kill|take\s+away|take\s+off|(?:i\s+)?(?:do\s+not|don't|dont)\s+(?:need|want|use|like))\s+(?:the\s+|my\s+)?($ITEMS)\s*$""", IC).find(low)?.let { m ->
            return "hide the " + m.groupValues[1]
        }
        Regex("""^(?:bring\s+back|put\s+back|i\s+miss|(?:i\s+)?want\s+back)\s+(?:the\s+|my\s+)?($ITEMS)\s*$""", IC).find(low)?.let { m ->
            return "show the " + m.groupValues[1]
        }
        return null
    }

    /** [input] put into plainer words the helper's rules and offline model already know. A note's text is never touched. */
    fun rewrite(input: String): String {
        val first = input.trim()
        if (first.isEmpty() || NOTE_ADD.containsMatchIn(first) || Regex("""^note\s*[:\-]""", IC).containsMatchIn(first)) return first
        var t = stripFiller(first)
        if (t.isEmpty()) return first
        t = fixTypos(t)
        feeling(t)?.let { return it }
        for ((re, to) in SWAPS) t = re.replace(t, to)
        return numberWords(t)
    }

    // ------------------------------------------------------------------ the Where to? icon

    private val PIN_EMOJI = listOf(
        "car" to "🚗", "taxi" to "🚕", "cab" to "🚕", "heart" to "❤️", "star" to "⭐", "home" to "🏠", "house" to "🏠", "flag" to "🚩",
        "compass" to "🧭", "globe" to "🌍", "world" to "🌍", "earth" to "🌍", "rocket" to "🚀", "fire" to "🔥", "target" to "🎯",
        "satellite" to "🛰️", "pushpin" to "📌", "finish" to "🏁", "bus" to "🚌", "plane" to "✈️", "airplane" to "✈️", "bike" to "🚲",
        "bicycle" to "🚲", "parking" to "🅿️", "suitcase" to "🧳", "luggage" to "🧳", "beach" to "🏖️", "mountain" to "🏔️", "coffee" to "☕"
    )
    private val ICONISH = setOf("icon", "icons", "picture", "image", "emoji", "symbol", "logo", "emblem", "pin", "marker", "pointer")
    private val NOT_PIN = setOf("app", "apps", "dial", "dials", "shortcut", "shortcuts", "note", "notes", "message", "messages", "tile", "tiles", "pinned", "chat", "chats")
    private val CHANGE = setOf("change", "set", "make", "use", "put", "replace", "switch", "turn", "pick", "choose", "want", "like", "to", "into", "as", "with")

    /**
     * Changes the icon of the Where to? box: "change the pin icon to a car", "use the red pin", "use the Maps icon again",
     * "Where to? icon 🚀", "pin icon from my gallery". With nothing clear to set it opens the list of icons (openSlot "pin").
     * Returns null when the sentence is not about this icon.
     */
    fun pinIcon(c: Context, input: String): Helper.Result? {
        val lower = input.lowercase()
        val w = lower.split(Regex("[^a-z]+")).filter { it.isNotEmpty() }
        if (w.isEmpty() || w.any { it in NOT_PIN }) return null
        if (w.any { it in setOf("size", "bigger", "smaller", "larger", "width", "height") }) return null
        val aboutPin = w.any { it == "pin" || it == "pins" || it == "marker" || it == "pointer" || it == "pushpin" } ||
            (w.contains("where") && w.any { it in setOf("icon", "icons", "emoji", "symbol", "logo") }) ||
            ((w.contains("maps") || w.contains("map") || w.contains("google")) && w.any { it in setOf("icon", "icons", "logo") })
        if (!aboutPin) return null
        val e = Helper.firstEmoji(input)
        val wantsRed = (w.contains("red") && w.any { it == "pin" || it == "pins" }) || w.any { it in setOf("classic", "builtin", "basic", "plain", "simple") }
        val wantsIcon = e != null || wantsRed || w.any { it in ICONISH && it != "pin" && it != "marker" && it != "pointer" } ||
            (w.any { it in CHANGE } && w.any { w1 -> PIN_EMOJI.any { it.first == w1 } })
        if (!wantsIcon) return null

        if (e != null) {
            Prefs.setEmojiOf(c, "pin", e)
            TilesWidget.rebuildAll(c.applicationContext)
            return Helper.Result("Where to? icon is now $e.", true, "")
        }
        val resetWords = setOf("normal", "default", "original", "standard", "google", "maps", "real", "usual", "again")
        val wantsMaps = w.any { it in resetWords } || (w.contains("back") && !w.contains("red"))
        val wantsPicture = w.any { it in setOf("gallery", "photo", "photos", "pic", "pics", "upload", "custom") } ||
            (w.contains("own") && w.any { it == "picture" || it == "image" })
        when {
            wantsRed -> {
                Prefs.setPinMode(c, 1)
                TilesWidget.rebuildAll(c.applicationContext)
                return Helper.Result("Where to? icon is now the red pin.", true, "")
            }
            wantsMaps -> {
                Prefs.setPinMode(c, 0)
                TilesWidget.rebuildAll(c.applicationContext)
                return Helper.Result("Where to? icon is back to the Google Maps icon.", true, "")
            }
            wantsPicture -> return Helper.Result("Choose the picture for the Where to? icon.", false, "", "pin_pic")
        }
        // "the pin to a car", "pin icon rocket"
        val named = w.firstNotNullOfOrNull { word -> PIN_EMOJI.firstOrNull { it.first == word }?.second }
        if (named != null && w.any { it in CHANGE }) {
            Prefs.setEmojiOf(c, "pin", named)
            TilesWidget.rebuildAll(c.applicationContext)
            return Helper.Result("Where to? icon is now $named.", true, "")
        }
        return Helper.Result("Pick the Where to? icon from the list.", false, "", "pin")
    }
}
