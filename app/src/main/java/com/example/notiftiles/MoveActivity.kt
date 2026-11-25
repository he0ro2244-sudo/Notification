package com.example.notiftiles

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.abs

/**
 * Move: shows the real widget and lets you press and drag ANY item (clock, date, weather, Where to?, every fast-dial
 * circle, every shortcut circle, title, count, mic, + Note, Clear all, pencil, gear) to any place. There are no rows:
 * each item keeps exactly the place where you let go. The line "Messages start here" can be dragged too.
 *
 * Nothing changes until the first item is moved: at that moment the places of all items are read from the widget as it
 * looks now (so everything stays where it is) and the free layout is switched on.
 */
class MoveActivity : Activity() {

    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private lateinit var holder: FrameLayout
    private lateinit var note: TextView
    private var root: View? = null
    private var widthDp = 250
    private var heightDp = 300
    private val scope: String get() = intent.getStringExtra("scope") ?: Prefs.SCOPE_PENCIL
    private lateinit var tracker: ChangeTracker
    private var undoBar: UndoBar? = null
    private var optW = 0f // the widget width (dp) the home screen reports; the places are saved for it

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun dpf(n: Float) = (n * resources.displayMetrics.density).toInt()
    private val density: Float get() = resources.displayMetrics.density

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tracker = ChangeTracker(this, scope) { undoBar?.refresh() }
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(16), dp(16), dp(32))

        val heading = TextView(this)
        heading.text = "Move items"
        heading.textSize = 20f
        heading.setTypeface(heading.typeface, Typeface.BOLD)
        col.addView(heading)

        val hint = TextView(this)
        hint.text = "Press and drag any item of the widget to any place, then let go. Every item (clock, date, weather, Where to?, each fast-dial circle, each shortcut circle, title, count, mic, + Note, Clear all, pencil, gear) stays exactly where you drop it. Drag the bar \"Messages start here\" to choose where the message tiles begin. While you drag, an item clicks into line with its neighbours (same height, same left or right edge) when it is close. \"Tidy up\" lines up whole rows for you. A hidden item is shown again in Arrange & resize first."
        hint.textSize = 13f
        hint.alpha = 0.7f
        hint.setPadding(0, dp(2), 0, dp(6))
        col.addView(hint)

        note = TextView(this)
        note.textSize = 13f
        note.setPadding(0, dp(6), 0, dp(6))
        note.visibility = View.GONE
        col.addView(note)

        // The preview is as wide as the real widget, so a place here is the same place on the home screen
        holder = FrameLayout(this)
        holder.layoutDirection = View.LAYOUT_DIRECTION_LTR
        holder.clipChildren = false
        col.addView(holder, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

        val alignBtn = Button(this)
        alignBtn.text = "Tidy up: align items in each row"
        alignBtn.isAllCaps = false
        alignBtn.setOnClickListener { tidy(0) }
        col.addView(alignBtn, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(16) })

        val reflowBtn = Button(this)
        reflowBtn.text = "Fix layout: close gaps and line everything up"
        reflowBtn.isAllCaps = false
        reflowBtn.setOnClickListener { reflowAll(false) }
        col.addView(reflowBtn, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(16) })

        val fixBtn = Button(this)
        fixBtn.text = "Tidy up: fix overlapping items (smart)"
        fixBtn.isAllCaps = false
        fixBtn.setOnClickListener { fixOverlaps(false) }
        col.addView(fixBtn, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

        val spaceBtn = Button(this)
        spaceBtn.text = "Tidy up: align and space evenly"
        spaceBtn.isAllCaps = false
        spaceBtn.setOnClickListener { tidy(1) }
        col.addView(spaceBtn, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

        // Pack closer: items of a row sit next to each other with a small gap you choose (no big empty spaces)
        val gapLabel = TextView(this)
        gapLabel.textSize = 13f
        gapLabel.setPadding(0, dp(12), 0, 0)
        gapLabel.text = "Gap between items when packing: ${Prefs.freeGap(this)} dp"
        col.addView(gapLabel)
        val gapBar = android.widget.SeekBar(this)
        gapBar.max = 40
        gapBar.progress = Prefs.freeGap(this)
        gapBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: android.widget.SeekBar, p: Int, fromUser: Boolean) {
                gapLabel.text = "Gap between items when packing: $p dp"
                if (fromUser) Prefs.setFreeGap(this@MoveActivity, p)
            }
            override fun onStartTrackingTouch(s: android.widget.SeekBar) {}
            override fun onStopTrackingTouch(s: android.widget.SeekBar) {}
        })
        col.addView(gapBar)
        val centre = android.widget.CheckBox(this)
        centre.text = "Centre each packed row in the widget"
        centre.isChecked = Prefs.freePackCenter(this)
        centre.setOnCheckedChangeListener { _, v -> Prefs.setFreePackCenter(this, v) }
        col.addView(centre)
        val packBtn = Button(this)
        packBtn.text = "Tidy up: pack items closer"
        packBtn.isAllCaps = false
        packBtn.setOnClickListener { tidy(2) }
        col.addView(packBtn, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(4) })

        // Same undo as in Arrange & resize: every move, tidy-up or change takes back one step at a time (the list of whoever opened Move)
        val ub = UndoBar(this, scope, tracker) { holder.post { render() } }
        undoBar = ub
        col.addView(ub.view, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

        val back = Button(this)
        back.text = "Back to the old rows layout"
        back.isAllCaps = false
        back.setOnClickListener {
            Prefs.resetFree(this)
            TilesWidget.rebuildAll(applicationContext)
            render()
        }
        col.addView(back, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

        val arr = Button(this)
        arr.text = "Arrange & resize (sizes, colours, show / hide)…"
        arr.isAllCaps = false
        arr.setOnClickListener { startActivity(android.content.Intent(this, ArrangeActivity::class.java).putExtra("scope", scope)) }
        col.addView(arr, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

        val done = Button(this)
        done.text = "Done"
        done.setOnClickListener { finish() }
        col.addView(done, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

        val scroll = ScrollView(this)
        scroll.addView(col)
        setContentView(scroll)
        render()
    }

    // Sizes or switches may have changed in Arrange & resize: draw the preview again when coming back
    private var firstResume = true

    override fun onResume() {
        super.onResume()
        tracker.start()
        undoBar?.refresh()
        if (firstResume) { firstResume = false; return }
        render()
    }

    override fun onPause() {
        tracker.stop()
        super.onPause()
    }

    private fun showNote(text: String) {
        note.text = text
        note.visibility = View.VISIBLE
    }

    /** Draws the real widget (built the same way the home screen gets it) and makes its items draggable. */
    private fun render() {
        holder.removeAllViews()
        root = null
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, TilesWidget::class.java))
        if (ids.isEmpty()) {
            showNote("Add the NotifTiles widget to your home screen first, then come back here.")
            return
        }
        val view: View
        try {
            view = TilesWidget.build(applicationContext, ids[0]).apply(this, holder)
        } catch (e: Throwable) {
            android.util.Log.e("NotifTilesMove", "Could not draw the widget preview", e)
            showNote("The widget could not be drawn here: $e")
            return
        }
        val opt = mgr.getAppWidgetOptions(ids[0])
        val screenW = (resources.displayMetrics.widthPixels / density).toInt() - 32
        val wOpt = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        optW = wOpt.toFloat()
        widthDp = (if (wOpt > 0) wOpt else 320).coerceIn(120, screenW.coerceAtLeast(120))
        val hMax = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        val hMin = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        heightDp = (if (hMax > 0) hMax else hMin).coerceAtLeast(260)
        root = view
        holder.addView(view, FrameLayout.LayoutParams(dp(widthDp), dp(heightDp)))
        note.visibility = View.GONE
        // Each step is separate, so a problem in one never hides the widget itself
        try {
            clearClicks(view)
            view.findViewById<View>(R.id.grid)?.visibility = View.INVISIBLE
        } catch (e: Throwable) {
            android.util.Log.e("NotifTilesMove", "Preview setup failed", e)
        }
        // Wait until the widget has been measured: the places are read from it
        holder.post {
            try {
                setupDrag(view)
            } catch (e: Throwable) {
                android.util.Log.e("NotifTilesMove", "Could not make the items draggable", e)
            }
            if (intent.getBooleanExtra("fix_all", false) && !fixAllDone) {
                fixAllDone = true
                Prefs.setTidyPending(this, false)
                holder.postDelayed({ reflowAll(true) }, 500)
            } else if (Prefs.tidyPending(this) && Prefs.freeOn(this)) {
                Prefs.setTidyPending(this, false)
                holder.postDelayed({ fixOverlaps(true) }, 500)
            } else {
                closeIfAuto()
            }
        }
    }

    private fun setupDrag(view: View) {
        for ((key, id) in TilesWidget.FREE_VIEW) {
            try {
                val item = view.findViewById<View>(id) ?: continue
                if (item.visibility == View.VISIBLE && item.width > 0) attachDrag(item, key)
            } catch (e: Throwable) {
                android.util.Log.e("NotifTilesMove", "Could not make $key draggable", e)
            }
        }
        addGridBar()
    }

    /** The preview only shows the layout: taps must not open the app or the panel. */
    private fun clearClicks(v: View) {
        v.setOnClickListener(null)
        v.isClickable = false
        v.isLongClickable = false
        if (v is ViewGroup) for (i in 0 until v.childCount) clearClicks(v.getChildAt(i))
    }

    // ---- Places ----

    private fun rootLoc(): IntArray {
        val loc = IntArray(2)
        root?.getLocationOnScreen(loc)
        return loc
    }

    /** The place of a view now, in dp from the top-left corner of the widget. */
    private fun placeOf(v: View, rl: IntArray): Pair<Float, Float> {
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)
        return Pair((loc[0] - rl[0]) / density, (loc[1] - rl[1]) / density)
    }

    /** The places of every item on show, as the widget looks now (used the first time an item is moved). */
    private fun measureAll(): MutableMap<String, Prefs.FreePos> {
        val out = LinkedHashMap<String, Prefs.FreePos>()
        val r = root ?: return out
        val rl = rootLoc()
        for ((key, id) in TilesWidget.FREE_VIEW) {
            val v = r.findViewById<View>(id) ?: continue
            if (v.visibility == View.GONE || v.width <= 0) continue
            val p = placeOf(v, rl)
            out[key] = Prefs.FreePos(p.first, p.second, if (key == "maps") v.width / density else 0f)
        }
        return out
    }

    /** Where the message tiles start now, in dp from the top of the widget. */
    private fun measureGridTop(): Float {
        val r = root ?: return Prefs.freeGridTop(this)
        val g = r.findViewById<View>(R.id.gridwrap) ?: return Prefs.freeGridTop(this)
        return placeOf(g, rootLoc()).second
    }

    /** Width and height in dp of every item on show, read from the preview. */
    private fun sizes(): Map<String, Pair<Float, Float>> {
        val out = HashMap<String, Pair<Float, Float>>()
        val r = root ?: return out
        for ((key, id) in TilesWidget.FREE_VIEW) {
            val v = r.findViewById<View>(id) ?: continue
            if (v.visibility == View.GONE || v.width <= 0) continue
            out[key] = Pair(v.width / density, v.height / density)
        }
        return out
    }

    /**
     * Lines items up. Items whose middles are within about 14dp of each other form a row and are put on one line;
     * with [space] the items of a row (3 or more) also get equal gaps between the first and the last.
     */
    private fun tidy(mode: Int) {
        val space = mode == 1
        if (!Prefs.freeOn(this)) {
            showNote("Move an item first, then Tidy up lines the items up.")
            return
        }
        val sz = sizes()
        val places = currentPlaces()
        Prefs.saveFreePrev(this)
        val keys = places.keys.filter { it in sz }.sortedBy { places.getValue(it).y + sz.getValue(it).second / 2f }
        val rows = ArrayList<MutableList<String>>()
        for (k in keys) {
            val c = places.getValue(k).y + sz.getValue(k).second / 2f
            val row = rows.lastOrNull()
            if (row != null) {
                val mean = row.map { places.getValue(it).y + sz.getValue(it).second / 2f }.average().toFloat()
                if (kotlin.math.abs(c - mean) <= 14f) { row.add(k); continue }
            }
            rows.add(mutableListOf(k))
        }
        for (row in rows) {
            if (row.size < 2) continue
            val mid = row.map { places.getValue(it).y + sz.getValue(it).second / 2f }.average().toFloat()
            for (k in row) {
                val p = places.getValue(k)
                places[k] = Prefs.FreePos(p.x, (mid - sz.getValue(k).second / 2f).coerceAtLeast(0f), p.w)
            }
            if (mode == 2) {
                // Pack: the items of the row sit side by side with the chosen gap, from the first item's place or centred
                val gap = Prefs.freeGap(this).toFloat()
                val byX = row.sortedBy { places.getValue(it).x }
                val total = byX.sumOf { sz.getValue(it).first.toDouble() }.toFloat() + gap * (byX.size - 1)
                val full = if (optW > 0f) optW else widthDp.toFloat()
                var x = if (Prefs.freePackCenter(this)) ((full - total) / 2f).coerceAtLeast(0f) else places.getValue(byX.first()).x
                for (k in byX) {
                    val p = places.getValue(k)
                    places[k] = Prefs.FreePos(x, p.y, p.w)
                    x += sz.getValue(k).first + gap
                }
            }
            if (space && row.size >= 3) {
                val byX = row.sortedBy { places.getValue(it).x }
                val first = places.getValue(byX.first()).x
                val last = byX.last()
                val end = places.getValue(last).x + sz.getValue(last).first
                val total = byX.sumOf { sz.getValue(it).first.toDouble() }.toFloat()
                val gap = (end - first - total) / (byX.size - 1)
                if (gap >= 0f) {
                    var x = first
                    for (k in byX) {
                        val p = places.getValue(k)
                        places[k] = Prefs.FreePos(x, p.y, p.w)
                        x += sz.getValue(k).first + gap
                    }
                }
            }
        }
        Prefs.setFreePos(this, places)
        Prefs.setFreeRefW(this, optW)
        TilesWidget.rebuildAll(applicationContext)
        holder.post { render() }
    }

    // Opened by itself after something was switched on: close again once the overlaps are fixed
    private fun closeIfAuto() {
        if (intent.getBooleanExtra("auto_tidy", false)) holder.postDelayed({ if (!isFinishing) finish() }, 900)
    }

    private var fixAllDone = false

    /**
     * One-tap "Fix layout": everything below the clock / fast-dial rows (shortcut circles, title, count, mic, + Note,
     * Clear all, pencil, gear) is laid out again in reading order, side by side with a small gap, a new line when a row is full.
     * Gaps left by deleted items close, and the line "Messages start here" moves up or down to sit just under them.
     */
    private fun reflowAll(auto: Boolean) {
        if (!Prefs.freeOn(this)) {
            android.widget.Toast.makeText(this, "The widget is in the normal rows layout: nothing to fix. Use Move first.", android.widget.Toast.LENGTH_LONG).show()
            closeIfAuto()
            return
        }
        val sz = sizes()
        val places = currentPlaces()
        val topKeys = setOf("clock", "weather", "date", "merge", "maps", "d0", "d1", "d2", "d3", "d4")
        val group = places.keys.filter { it in sz && it !in topKeys }
        if (group.isEmpty()) {
            closeIfAuto()
            return
        }
        val full = if (optW > 0f) optW else widthDp.toFloat()
        val gap = Prefs.freeGap(this).toFloat().coerceAtLeast(6f)
        fun cy(k: String) = places.getValue(k).y + sz.getValue(k).second / 2f
        // Order: shortcut circles (left to right), then + Note, mic, pencil, gear; Inbox, Clear all and the count always go last,
        // on their own line just above the messages.
        val bottomKeys = listOf("title", "clear", "count").filter { it in group }
        val buttonKeys = listOf("note", "mic", "edit", "gear").filter { it in group }
        val circles = group.filter { it.startsWith("q") }.sortedBy { places.getValue(it).x }
        val rest = group.filter { it !in bottomKeys && it !in buttonKeys && it !in circles }
        val main = circles + buttonKeys + rest
        val topBottom = places.keys.filter { it in sz && it in topKeys && it != "merge" }.maxOfOrNull { places.getValue(it).y + sz.getValue(it).second } ?: 0f
        val left = group.minOf { places.getValue(it).x }.coerceIn(0f, 16f)
        var y = maxOf(group.minOf { places.getValue(it).y }, topBottom + 4f)
        val out = LinkedHashMap(places)
        var bottom = y
        fun flow(order: List<String>) {
            var i = 0
            while (i < order.size) {
                val line = ArrayList<String>()
                var x = left
                while (i < order.size) {
                    val w = sz.getValue(order[i]).first
                    if (line.isNotEmpty() && x + w > full) break
                    line.add(order[i])
                    x += w + gap
                    i++
                }
                val rowH = line.maxOf { sz.getValue(it).second }
                var px = left
                for (k in line) {
                    val (w, h) = sz.getValue(k)
                    val p = places.getValue(k)
                    out[k] = Prefs.FreePos(px, y + (rowH - h) / 2f, p.w)
                    px += w + gap
                }
                y += rowH + 4f
                bottom = y
            }
        }
        flow(main)
        flow(bottomKeys)
        Prefs.saveFreePrev(this)
        Prefs.setFreePos(this, out)
        Prefs.setFreeRefW(this, optW)
        Prefs.setFreeGridTop(this, bottom + 4f)
        Prefs.setRefreshTop(this, true) // the refresh icon sits right under the items, not in the middle
        TilesWidget.rebuildAll(applicationContext)
        holder.post {
            render()
            showNote("Layout fixed: gaps closed and items lined up. Undo is below if you do not like it.")
            closeIfAuto()
        }
    }

    private class Box(val key: String, var x: Float, var y: Float, val w: Float, val h: Float)

    /**
     * Smart fix after something was added or switched on again: items that sit on top of each other are separated.
     * The item that comes later (further down, then further right) is the one that moves. In order:
     *  1. it goes to a free gap on its own row, the one closest to where it was (the end of the row counts too);
     *  2. else the items of that row are packed side by side in their order, the moved item last, if they all fit;
     *  3. else it goes to the right of what it overlaps, or to the next line.
     */
    private fun fixOverlaps(auto: Boolean) {
        if (!Prefs.freeOn(this)) {
            if (!auto) showNote("Move an item first, then Tidy up can fix overlaps.")
            return
        }
        val sz = sizes()
        val places = currentPlaces()
        val keys = places.keys.filter { it in sz && it != "merge" }
            .sortedWith(compareBy({ places.getValue(it).y }, { places.getValue(it).x }))
        val full = if (optW > 0f) optW else widthDp.toFloat()
        val m = 4f // smallest space kept between two items
        val placed = ArrayList<Box>()
        val moved = LinkedHashSet<String>()
        fun hits(x: Float, y: Float, w: Float, h: Float, o: Box) =
            x < o.x + o.w - 2f && x + w > o.x + 2f && y < o.y + o.h - 2f && y + h > o.y + 2f
        fun vOverlap(y: Float, h: Float, o: Box) = minOf(y + h, o.y + o.h) - maxOf(y, o.y)

        for (k in keys) {
            val p = places.getValue(k)
            val (w, h) = sz.getValue(k)
            if (placed.none { hits(p.x, p.y, w, h, it) }) {
                placed.add(Box(k, p.x, p.y, w, h))
                continue
            }
            // 1. a free gap on its own row
            val row = placed.filter { vOverlap(p.y, h, it) > 4f }.sortedBy { it.x }
            var bestX = Float.NaN
            var bestD = Float.MAX_VALUE
            var edge = 0f
            val gaps = ArrayList<Pair<Float, Float>>()
            for (o in row) {
                gaps.add(Pair(edge, o.x - m))
                edge = o.x + o.w + m
            }
            gaps.add(Pair(edge, full))
            for ((a, b) in gaps) {
                if (b - a < w) continue
                val x = p.x.coerceIn(a, b - w)
                val d = kotlin.math.abs(x - p.x)
                if (d < bestD) { bestD = d; bestX = x }
            }
            if (!bestX.isNaN() && placed.none { hits(bestX, p.y, w, h, it) }) {
                placed.add(Box(k, bestX, p.y, w, h))
                moved.add(k)
                continue
            }
            // 2. pack the row: the items already there keep their order, this one goes last
            val cluster = placed.filter { vOverlap(p.y, h, it) >= 0.3f * minOf(h, it.h) }.sortedBy { it.x }
            val total = cluster.sumOf { it.w.toDouble() }.toFloat() + w + m * cluster.size
            if (cluster.isNotEmpty() && total <= full) {
                var x = cluster.first().x
                if (x + total > full) x = (full - total).coerceAtLeast(0f)
                val snapshot = cluster.map { Pair(it.x, it.y) }
                for (o in cluster) {
                    o.x = x
                    x += o.w + m
                }
                val inRow = cluster.map { c -> c.key }
                val ok = placed.none { o -> o.key !in inRow && hits(x, p.y, w, h, o) }
                if (ok) {
                    for ((i, o) in cluster.withIndex()) if (o.x != snapshot[i].first) moved.add(o.key)
                    placed.add(Box(k, x, p.y, w, h))
                    moved.add(k)
                    continue
                }
                for ((i, o) in cluster.withIndex()) o.x = snapshot[i].first // did not work out: put them back
            }
            // 3. the row is full: a new line just under it. Wrapped items line up there from the left, in order.
            val band = placed.filter { vOverlap(p.y, h, it) > 4f }
            var y = (band.maxOfOrNull { it.y + it.h } ?: (p.y + h)) + m
            var nx = Float.NaN
            var guard = 0
            while (guard++ < 40 && nx.isNaN()) {
                val cy = y
                val line = placed.filter { vOverlap(cy, h, it) > 4f }.sortedBy { it.x }
                var edge = 0f
                val free = ArrayList<Pair<Float, Float>>()
                for (o in line) {
                    free.add(Pair(edge, o.x - m))
                    edge = o.x + o.w + m
                }
                free.add(Pair(edge, full))
                val spot = free.firstOrNull { it.second - it.first >= w }
                if (spot != null) nx = spot.first else if (line.isEmpty()) nx = 0f else y = line.maxOf { it.y + it.h } + m
            }
            if (nx.isNaN()) nx = p.x
            placed.add(Box(k, nx, y, w, h))
            moved.add(k)
        }
        val out = LinkedHashMap(places)
        var count = 0
        for (b in placed) {
            val p = places.getValue(b.key)
            if (b.x != p.x || b.y != p.y) {
                out[b.key] = Prefs.FreePos(b.x, b.y, p.w)
                count++
            }
        }
        if (count == 0) {
            if (!auto) showNote("Nothing overlaps.")
            closeIfAuto()
            return
        }
        Prefs.saveFreePrev(this)
        Prefs.setFreePos(this, out)
        Prefs.setFreeRefW(this, optW)
        // Items that wrapped to a new line must not end up on top of the messages: they start lower then
        val lowest = placed.maxOfOrNull { it.y + it.h } ?: 0f
        if (lowest + 8f > Prefs.freeGridTop(this)) Prefs.setFreeGridTop(this, lowest + 8f)
        TilesWidget.rebuildAll(applicationContext)
        holder.post {
            render()
            showNote("Moved $count item(s) so nothing overlaps. Undo is below if you do not like it.")
            closeIfAuto()
        }
    }

    // ---- Dragging ----

    private fun attachDrag(item: View, key: String) {
        val slop = dp(8).toFloat()
        var downX = 0f
        var downY = 0f
        var moving = false
        item.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.rawX
                    downY = ev.rawY
                    moving = false
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY
                    if (!moving && (abs(dx) > slop || abs(dy) > slop)) {
                        moving = true
                        v.elevation = dp(6).toFloat()
                        v.alpha = 0.85f
                    }
                    if (moving) {
                        v.translationX = dx
                        v.translationY = dy
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val didMove = moving
                    moving = false
                    val dx = ev.rawX - downX
                    val dy = ev.rawY - downY
                    v.translationX = 0f
                    v.translationY = 0f
                    v.elevation = 0f
                    v.alpha = 1f
                    if (didMove && ev.actionMasked == MotionEvent.ACTION_UP) drop(v, key, dx / density, dy / density)
                    true
                }
                else -> true
            }
        }
    }

    /** The places of every item: the saved ones, or (the first time) read from the widget as it looks now. */
    private fun currentPlaces(): MutableMap<String, Prefs.FreePos> {
        if (Prefs.freeOn(this)) {
            // Saved places are stretched to the current widget width first, the same way the widget draws them
            val s = Prefs.freeScaleFor(this, optW)
            val m = LinkedHashMap<String, Prefs.FreePos>()
            for ((k, p) in Prefs.freePos(this)) m[k] = if (s == 1f) p else Prefs.FreePos(p.x * s, p.y, p.w * s)
            return m
        }
        Prefs.setFreeGridTop(this, measureGridTop())
        return measureAll()
    }

    /** Rounds a place to a 4dp grid when snapping is on. */
    private fun snap(v: Float): Float = if (Prefs.freeSnap(this)) Math.round(v / 4f) * 4f else v

    /** The dragged item keeps the place where it was let go. */
    private fun drop(dragged: View, key: String, dxDp: Float, dyDp: Float) {
        val rl = rootLoc()
        val places = currentPlaces()
        Prefs.saveFreePrev(this)
        val p0 = placeOf(dragged, rl)
        val old = places[key]
        val maxX = (widthDp - 24).toFloat().coerceAtLeast(0f)
        val maxY = (heightDp - 24).toFloat().coerceAtLeast(0f)
        var x = snap(p0.first + dxDp).coerceIn(0f, maxX)
        var y = snap(p0.second + dyDp).coerceIn(0f, maxY)
        // Guides: click into line with a neighbour (same middle height, same left or right edge) when it is close
        if (Prefs.freeSnap(this)) {
            val sz = sizes()
            val mw = dragged.width / density
            val mh = dragged.height / density
            var bestY = 9f
            var bestX = 7f
            val rawX = p0.first + dxDp
            val rawY = p0.second + dyDp
            for ((ok, op) in places) {
                if (ok == key) continue
                val s = sz[ok] ?: continue
                val dyc = (op.y + s.second / 2f) - (rawY + mh / 2f)
                if (kotlin.math.abs(dyc) < bestY) { bestY = kotlin.math.abs(dyc); y = (op.y + s.second / 2f - mh / 2f).coerceIn(0f, maxY) }
                val dl = op.x - rawX
                val dr = (op.x + s.first) - (rawX + mw)
                if (kotlin.math.abs(dl) < bestX) { bestX = kotlin.math.abs(dl); x = op.x.coerceIn(0f, maxX) }
                if (kotlin.math.abs(dr) < bestX) { bestX = kotlin.math.abs(dr); x = (op.x + s.first - mw).coerceIn(0f, maxX) }
            }
        }
        val w = old?.w ?: (if (key == "maps") dragged.width / density else 0f)
        places[key] = Prefs.FreePos(x, y, w)
        Prefs.setFreePos(this, places)
        Prefs.setFreeRefW(this, optW)
        Prefs.setFreeOn(this, true)
        TilesWidget.rebuildAll(applicationContext)
        holder.post { render() }
    }

    // ---- The bar where the message tiles start ----

    private fun addGridBar() {
        val bar = TextView(this)
        bar.text = "▼  Messages start here  (drag)"
        bar.textSize = 11f
        bar.gravity = Gravity.CENTER
        bar.setTextColor(0xFFFFFFFF.toInt())
        bar.setBackgroundColor(0xB0000000.toInt() or (Prefs.accent(this) and 0xFFFFFF))
        val top = if (Prefs.freeOn(this)) Prefs.freeGridTop(this) else measureGridTop()
        val lp = FrameLayout.LayoutParams(MATCH, dp(22))
        lp.topMargin = dpf(top.coerceIn(0f, (heightDp - 22).toFloat().coerceAtLeast(0f)))
        holder.addView(bar, lp)
        val slop = dp(8).toFloat()
        var downY = 0f
        var startTop = 0f
        var moving = false
        bar.setOnTouchListener { v, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downY = ev.rawY
                    startTop = (v.layoutParams as FrameLayout.LayoutParams).topMargin.toFloat()
                    moving = false
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dy = ev.rawY - downY
                    if (!moving && abs(dy) > slop) moving = true
                    if (moving) v.translationY = dy
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val didMove = moving
                    moving = false
                    val dy = ev.rawY - downY
                    v.translationY = 0f
                    if (didMove && ev.actionMasked == MotionEvent.ACTION_UP) {
                        val maxTop = (heightDp - 22).toFloat().coerceAtLeast(0f)
                        val newTop = snap((startTop + dy) / density).coerceIn(0f, maxTop)
                        val places = currentPlaces()
                        Prefs.setFreePos(this, places)
        Prefs.setFreeRefW(this, optW)
                        Prefs.setFreeGridTop(this, newTop)
                        Prefs.setFreeOn(this, true)
                        TilesWidget.rebuildAll(applicationContext)
                        holder.post { render() }
                    }
                    true
                }
                else -> true
            }
        }
    }
}
