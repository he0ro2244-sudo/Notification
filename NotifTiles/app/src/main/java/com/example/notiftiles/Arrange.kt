package com.example.notiftiles

import android.content.Context
import android.service.notification.StatusBarNotification

/** One tile on the widget or in the panel: one chat, or all the chats of one app when grouping is on. */
class Tile(val sbn: StatusBarNotification, val count: Int, val title: String, val text: String)

/** One of "My pages": a name, the apps whose messages show on it and the app shortcuts on it. */
data class CPage(val name: String, val apps: List<String>, val shortcuts: List<String>)

// Grouping, pages and the order of the message tiles. With every setting at its default nothing changes.
object Arrange {

    /** Number of tiles for [items] (one per chat, or one per app when grouped). */
    fun tileCount(c: Context, items: List<StatusBarNotification>): Int =
        if (Prefs.groupMode(c) == 1) items.map { it.packageName }.distinct().size else items.size

    /** [k] = how many message lines a tile shows. */
    fun tiles(c: Context, items: List<StatusBarNotification>, k: Int): List<Tile> {
        val perApp = items.groupingBy { it.packageName }.eachCount()
        if (Prefs.groupMode(c) == 0) {
            return items.map { Tile(it, perApp[it.packageName] ?: 1, Feed.title(it), Feed.recentText(c, it, k)) }
        }
        val out = ArrayList<Tile>()
        for ((_, list) in items.groupBy { it.packageName }) {
            val newest = list.maxByOrNull { it.postTime } ?: list[0]
            val title = list.sortedByDescending { it.postTime }
                .map { Feed.title(it) }.filter { it.isNotEmpty() }.distinct().joinToString(", ")
            val lines = ArrayList<String>()
            for (n in list.sortedByDescending { it.postTime }) {
                val last = Feed.lines(c, n, true).lastOrNull() ?: continue
                val who = Feed.title(n)
                lines.add(if (who.isNotEmpty() && list.size > 1 && !last.contains(who)) "$who: $last" else last)
                if (lines.size >= k) break
            }
            out.add(Tile(newest, list.size, title, lines.joinToString("\n")))
        }
        return out
    }

    // ---- Pages ----

    fun customOn(c: Context): Boolean = Prefs.scrollMode(c) == 2

    fun pageCount(c: Context, total: Int): Int {
        if (customOn(c)) return maxOf(1, Prefs.customPages(c).size)
        val size = Prefs.pageSize(c)
        return if (total <= 0) 1 else (total + size - 1) / size
    }

    /** The page shown now (0 = first) of "w" (widget) or "p" (panel), kept inside the real number of pages. */
    fun pageNow(c: Context, key: String, total: Int): Int =
        Prefs.page(c, key).coerceIn(0, pageCount(c, total) - 1)

    /** Only the tiles of the current page when pages are on; otherwise everything. */
    fun slice(c: Context, key: String, list: List<Tile>): List<Tile> {
        val mode = Prefs.scrollMode(c)
        if (mode == 0) return list
        if (mode == 2) {
            val pages = Prefs.customPages(c)
            if (pages.isEmpty()) return list
            val now = pageNow(c, key, list.size)
            val assigned = pages.flatMap { it.apps }.toSet()
            // An app that is on no page shows on page 1.
            return list.filter { val p = it.sbn.packageName; p in pages[now].apps || (now == 0 && p !in assigned) }
        }
        val size = Prefs.pageSize(c)
        return list.drop(pageNow(c, key, list.size) * size).take(size)
    }

    /** My pages: the app shortcuts of the current page. */
    fun shortcuts(c: Context, key: String): List<String> {
        if (!customOn(c)) return emptyList()
        val pages = Prefs.customPages(c)
        return pages.getOrNull(pageNow(c, key, 0))?.shortcuts ?: emptyList()
    }

    /** My pages: name of the current page (null when My pages is off). */
    fun pageName(c: Context, key: String): String? {
        if (!customOn(c)) return null
        return Prefs.customPages(c).getOrNull(pageNow(c, key, 0))?.name
    }

    fun addPage(c: Context, name: String) {
        val l = Prefs.customPages(c)
        val n = name.trim().ifEmpty { "Page ${l.size + 1}" }
        Prefs.setCustomPages(c, l + CPage(n, emptyList(), emptyList()))
    }

    /** Puts [pkg]'s messages on page [to] (an app is on one page at a time). */
    fun moveApp(c: Context, pkg: String, to: Int) {
        val l = Prefs.customPages(c)
        if (to !in l.indices) return
        Prefs.setCustomPages(c, l.mapIndexed { i, p ->
            val rest = p.apps.filter { it != pkg }
            if (i == to) p.copy(apps = rest + pkg) else p.copy(apps = rest)
        })
    }

    fun removeShortcut(c: Context, page: Int, pkg: String) {
        val l = Prefs.customPages(c)
        Prefs.setCustomPages(c, l.mapIndexed { i, p -> if (i == page) p.copy(shortcuts = p.shortcuts.filter { it != pkg }) else p })
    }

    /** Next page (+1) or previous page (-1); wraps around when endless scrolling is on. */
    fun move(c: Context, key: String, total: Int, delta: Int) {
        val n = pageCount(c, total)
        val p = pageNow(c, key, total) + delta
        val q = if (Prefs.loop(c)) ((p % n) + n) % n else p.coerceIn(0, n - 1)
        Prefs.setPage(c, key, q)
    }
}
