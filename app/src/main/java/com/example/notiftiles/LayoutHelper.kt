package com.example.notiftiles

import android.content.Context

/**
 * Layout sentences for the widget and panel: gaps, item sizes, which row an item sits in, merging the top items,
 * and showing or hiding items. Each one saves a value the app already uses. Free drag positions are not handled here.
 */
object LayoutHelper {
    private val ITEM_WORDS = mapOf(
        "clock" to "clock", "time" to "clock",
        "weather" to "weather", "temperature" to "weather",
        "date" to "date", "day" to "date", "calendar" to "date",
        "maps" to "maps", "map" to "maps", "location" to "maps",
        "title" to "title", "inbox" to "title",
        "count" to "count", "badge" to "count",
        "mic" to "mic", "microphone" to "mic", "recorder" to "mic",
        "note" to "note", "notes" to "note",
        "clear" to "clear",
        "edit" to "edit", "pencil" to "edit",
        "gear" to "gear", "settings" to "gear", "setting" to "gear", "cog" to "gear"
    )
    /** The item a word names, also for its plural ("clocks" -> clock). */
    private fun itemWord(w: String): String? = ITEM_WORDS[w] ?: ITEM_WORDS[w.removeSuffix("s")]
        ?: ITEM_WORDS.keys.firstOrNull { k -> k.length >= 4 && Helper.near(w, k) }?.let { ITEM_WORDS[it] }

    private val MERGEABLE = setOf("clock", "weather", "date")
    private val TOP_ITEMS = setOf("clock", "weather", "date", "maps")
    private val UP = setOf("bigger", "larger", "more", "increase", "wider", "taller", "raise", "up")
    private val DOWN = setOf("smaller", "less", "decrease", "narrower", "shorter", "lower", "down")

    /** The setting item named in the sentence (clock, weather, ...), or null. */
    fun itemOf(input: String): String? = input.lowercase().split(Regex("[^a-z]+")).firstNotNullOfOrNull { itemWord(it) }

    private fun cap(s: String): String = s.replaceFirstChar { it.uppercase() }

    /** The value to save: the number said, or the current value moved one step up or down, kept inside the range. */
    private fun pick(n: Int?, current: Int, up: Boolean, down: Boolean, min: Int, max: Int, small: Int): Int = when {
        n != null -> n.coerceIn(min, max)
        up -> (current + small).coerceIn(min, max)
        down -> (current - small).coerceIn(min, max)
        else -> current
    }

    private fun redraw(c: Context) = TilesWidget.rebuildAll(c.applicationContext)

    /** Handles a layout sentence. Returns null when the sentence is not about the layout. */
    fun handle(c: Context, input: String): Helper.Result? {
        val words = input.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }.map { Helper.canon(it) }
        val n = Helper.numberIn(input)
        val item = words.firstNotNullOfOrNull { itemWord(it) }
        val up = words.any { it in UP }
        val down = words.any { it in DOWN }

        // Un-merge the top items
        if (words.contains("unmerge") || words.contains("split")) {
            Prefs.setMerged(c, emptySet())
            redraw(c)
            return Helper.Result("The top items are separate again.", true, "")
        }

        // Merge two or more of clock, weather and date
        if (words.contains("merge") || words.contains("combine")) {
            val parts = words.mapNotNull { itemWord(it) }.filter { it in MERGEABLE }.distinct()
            if (parts.size < 2) return Helper.Result("Name two of clock, weather or date to merge, for example: merge the clock and the date.", false, "")
            Prefs.setMerged(c, parts.toSet())
            redraw(c)
            return Helper.Result("Merged: ${parts.joinToString(" and ")}.", true, "")
        }

        // Gaps between the rows and the free-layout items
        if (words.contains("gap") || words.contains("spacing") || (words.contains("space") && words.contains("between"))) {
            if (words.contains("free")) {
                val v = pick(n, Prefs.freeGap(c), up, down, 0, 40, 2)
                Prefs.setFreeGap(c, v)
                redraw(c)
                return Helper.Result("Free layout gap is now $v.", true, "")
            }
            val v = pick(n, Prefs.rowGap(c), up, down, 0, 30, 2)
            Prefs.setRowGap(c, v)
            redraw(c)
            return Helper.Result("Row gap is now $v.", true, "")
        }

        if (item == null) return null

        // Order inside a row: "move the clock before the weather", "after", "left of", "right of"
        val pair = words.mapNotNull { itemWord(it) }.distinct()
        if (pair.size >= 2 && !words.contains("top") && (words.contains("before") || words.contains("after") || words.contains("left") || words.contains("right"))) {
            val x = pair[0]
            val y = pair[1]
            val before = words.contains("before") || words.contains("left")
            val rows = Prefs.rows(c)
            val top = (rows["top"] ?: emptyList()).filter { it != x }.toMutableList()
            val head = (rows["head"] ?: emptyList()).filter { it != x }.toMutableList()
            val list = if (top.contains(y)) top else head
            val at = list.indexOf(y) + (if (before) 0 else 1)
            list.add(at.coerceIn(0, list.size), x)
            Prefs.setRows(c, top, head)
            redraw(c)
            return Helper.Result("${cap(x)} moved ${if (before) "before" else "after"} ${cap(y)}.", true, "")
        }

        // Free layout: "move the clock left / right / up / down" nudges the saved place (by 10, or the number said)
        val dir = words.firstOrNull { it == "left" || it == "right" || it == "up" || it == "down" }
        if (dir != null && (words.contains("move") || words.contains("put")) && !words.contains("of") &&
            !words.contains("top") && !words.contains("bottom") && !words.contains("below") && !words.contains("under")) {
            val step = n ?: 10
            val pos = Prefs.freePos(c).toMutableMap()
            val cur = pos[item] ?: return Helper.Result("${cap(item)} has no free place yet. Drag it once in Move, then say this again.", false, "")
            val dx = when (dir) { "left" -> -step; "right" -> step; else -> 0 }
            val dy = when (dir) { "up" -> -step; "down" -> step; else -> 0 }
            pos[item] = Prefs.FreePos(cur.x + dx.toFloat(), cur.y + dy.toFloat(), cur.w)
            Prefs.setFreePos(c, pos)
            redraw(c)
            return Helper.Result("${cap(item)} moved $dir.", true, "")
        }

        // Move an item to the top row or the bottom (Inbox) row
        if (words.contains("move") || words.contains("put")) {
            val toTop = words.contains("top")
            val toBottom = words.any { it == "bottom" || it == "under" || it == "below" || it == "inbox" }
            if (toTop || toBottom) {
                val rows = Prefs.rows(c)
                val top = (rows["top"] ?: emptyList()).filter { it != item }.toMutableList()
                val head = (rows["head"] ?: emptyList()).filter { it != item }.toMutableList()
                val dest = if (toTop) top else head
                if (words.contains("first") || words.contains("start")) dest.add(0, item) else dest.add(item)
                Prefs.setRows(c, top, head)
                redraw(c)
                return Helper.Result("${cap(item)} moved to the ${if (toTop) "top" else "bottom"} row.", true, "")
            }
            return null
        }

        // Show or hide an item
        if (words.contains("hide") || words.contains("show")) {
            val on = words.contains("show")
            if (item in TOP_ITEMS) Prefs.setShow(c, item, on) else Prefs.setShowItem(c, item, on)
            redraw(c)
            return Helper.Result("${cap(item)} is now ${if (on) "shown" else "hidden"}.", true, "")
        }

        // Size of one item (a percentage, 50 to 250)
        if (up || down || words.any { it in setOf("width", "wider", "narrower", "height", "taller", "shorter", "wide", "tall", "bigger", "smaller") }) {
            val wide = words.any { it == "width" || it == "wider" || it == "narrower" || it == "wide" }
            val tall = words.any { it == "height" || it == "taller" || it == "shorter" || it == "tall" }
            val both = !wide && !tall
            val parts = ArrayList<String>()
            if (wide || both) {
                val v = pick(n, Prefs.itemW(c, item), up, down, 50, 250, 10)
                Prefs.setItemW(c, item, v)
                parts.add("width $v%")
            }
            if (tall || both) {
                val v = pick(n, Prefs.itemH(c, item), up, down, 50, 250, 10)
                Prefs.setItemH(c, item, v)
                parts.add("height $v%")
            }
            redraw(c)
            return Helper.Result("${cap(item)}: ${parts.joinToString(", ")}.", true, "")
        }
        return null
    }
}
