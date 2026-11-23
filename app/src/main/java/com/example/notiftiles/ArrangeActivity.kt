package com.example.notiftiles

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/**
 * Arrange & resize: drag the sections of the widget (top row, fast dial, shortcuts row, Inbox row) and the items of the
 * top row (clock, weather, date, Where to?) into the order you want, hide them, and make each section bigger or smaller.
 * Also drag the apps into your own tile order. The widget on the home screen is the live preview.
 */
class ArrangeActivity : Activity() {

    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private lateinit var scroll: ScrollView
    private lateinit var col: LinearLayout

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scroll = ScrollView(this)
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(0, b.top, 0, b.bottom)
            insets
        }
        col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(16), dp(16), dp(32))
        scroll.addView(col)
        setContentView(scroll)
        build()
    }

    private fun redraw() {
        TilesWidget.rebuildAll(applicationContext)
    }

    private fun rebuild() {
        val y = scroll.scrollY
        col.removeAllViews()
        build()
        scroll.post { scroll.scrollTo(0, y) }
    }

    private fun title(t: String, size: Float, top: Int) = TextView(this).apply {
        text = t
        textSize = size
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(top), 0, dp(4))
    }

    private fun hint(t: String) = TextView(this).apply {
        text = t
        textSize = 13f
        alpha = 0.7f
        setPadding(0, dp(2), 0, dp(6))
    }

    private fun build() {
        col.addView(title("Arrange & resize the widget", 20f, 0))
        col.addView(hint("Hold the ☰ handle of a row and drag it up or down. The widget on your home screen follows at once. Switches hide a part, sliders make a part bigger or smaller (100% = as before)."))
        val moveBtn = Button(this)
        moveBtn.text = "Move items (drag every item to any place)…"
        moveBtn.setOnClickListener { startActivity(Intent(this, MoveActivity::class.java)) }
        col.addView(moveBtn, LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(8) })

        val free = Prefs.freeOn(this)
        if (free) addFreeSection()

        // ---- 1. Sections of the widget (rows layout only)
        if (!free) {
        col.addView(title("Sections (top to bottom)", 16f, 16))
        col.addView(hint("The message tiles always fill the space under these sections. (Once you have moved an item with Move, items sit where you put them and these sections are not used; Move > 'Back to the old rows layout' brings them back.)"))
        val names = mapOf("top" to "Top row (clock, weather, date, Where to?)", "dial" to "Fast-dial circles",
            "quick" to "Shortcuts row", "head" to "Inbox row (title, mic, + Note, Clear all, gear)")
        col.addView(dragList(Prefs.blockOrder(this), { key ->
            val box = LinearLayout(this)
            box.orientation = LinearLayout.VERTICAL
            val head = LinearLayout(this)
            head.orientation = LinearLayout.HORIZONTAL
            head.gravity = Gravity.CENTER_VERTICAL
            val name = TextView(this)
            name.text = names[key]
            name.textSize = 15f
            head.addView(name, LinearLayout.LayoutParams(0, WRAP, 1f))
            when (key) {
                "dial" -> head.addView(smallSwitch(Prefs.show(this, "dial")) { Prefs.setShow(this, "dial", it); redraw() })
                "quick" -> head.addView(smallSwitch(Prefs.quickOn(this)) { Prefs.setQuickOn(this, it); redraw() })
            }
            box.addView(head)
            box.addView(sizeBar(Prefs.blockScale(this, key), 50, 200, "Whole section size") { Prefs.setBlockScale(this, key, it) })
            box
        }) { l -> Prefs.setBlockOrder(this, l); redraw(); rebuild() })
        }

        // ---- Every item: show / hide, size and colour of each one on its own
        col.addView(title("Every item", 16f, 20))
        col.addView(hint("Each item has its own size (100% = as before); circles, shapes and boxes also have Shape width / Shape height sliders that stretch only the shape, not the text. Switches hide an item. Tap Colour to change it (Auto = the normal colour)."))
        // key, label, show getter/setter (null = no switch), colour key (null = none)
        class Spec(val key: String, val label: String, val on: (() -> Boolean)?, val setOn: ((Boolean) -> Unit)?, val color: String?)
        fun hdr(k: String, label: String, color: String?) =
            Spec(k, label, { Prefs.showItem(this, k) }, { v -> Prefs.setShowItem(this, k, v) }, color)
        fun top(k: String, shown: String, label: String, color: String?) =
            Spec(k, label, { Prefs.show(this, shown) }, { v -> Prefs.setShow(this, shown, v) }, color)
        val specs = ArrayList<Pair<String, List<Spec>>>()
        specs.add("Top row" to listOf(
            top("w_clock", "clock", "Clock", "clock"), top("w_weather", "weather", "Weather", "weather"),
            top("w_date", "date", "Date", "date"), Spec("w_merge", "Merged shape", null, null, null),
            top("w_where", "maps", "Where to? box", "where")))
        specs.add("Fast-dial circles" to (0 until Prefs.DIAL_SLOTS).map { Spec("d$it", "Circle ${it + 1}", null, null, null) })
        specs.add("Shortcuts row circles" to (0 until Prefs.QUICK_SLOTS).map { Spec("q$it", "Circle ${it + 1}", null, null, null) })
        specs.add("Inbox row" to listOf(
            hdr("w_title", "Title (Inbox)", "title"), hdr("w_count", "Count badge", null), hdr("w_mic", "Mic", "mic"),
            hdr("w_note", "+ Note", "note"), hdr("w_clear", "Clear all", "clear"), hdr("w_gear", "Settings gear", "gear"),
            Spec("w_edit", "Pencil ✎ (opens this screen)", { Prefs.editBtn(this) }, { v -> Prefs.setEditBtn(this, v) }, "edit"),
            Spec("w_refresh", "Refresh icon", { Prefs.refreshShow(this) }, { v -> Prefs.setRefreshShow(this, v) }, "refresh")))
        for ((groupName, list) in specs) {
            col.addView(title(groupName, 14f, 12))
            for (sp in list) {
                val box = LinearLayout(this)
                box.orientation = LinearLayout.VERTICAL
                val rowTop = LinearLayout(this)
                rowTop.orientation = LinearLayout.HORIZONTAL
                rowTop.gravity = Gravity.CENTER_VERTICAL
                val nm = TextView(this)
                nm.text = sp.label
                nm.textSize = 15f
                rowTop.addView(nm, LinearLayout.LayoutParams(0, WRAP, 1f))
                if (sp.color != null) {
                    val cb = TextView(this)
                    cb.text = "Colour"
                    cb.textSize = 13f
                    cb.setPadding(dp(12), dp(8), dp(12), dp(8))
                    val cd = GradientDrawable()
                    cd.cornerRadius = dp(8).toFloat()
                    cd.setStroke(dp(1), 0x66888888)
                    val cur = Prefs.partColor(this, sp.color)
                    if (cur != 0) { cd.setColor(cur); cb.setTextColor(0xFF000000.toInt() or (cur.inv() and 0xFFFFFF)) }
                    cb.background = cd
                    cb.setOnClickListener {
                        ColorPicker.show(this, sp.label, Prefs.partColor(this, sp.color), true) { v ->
                            Prefs.setPartColor(this, sp.color, v); redraw(); rebuild()
                        }
                    }
                    rowTop.addView(cb)
                }
                if (sp.on != null && sp.setOn != null) rowTop.addView(smallSwitch(sp.on.invoke()) { sp.setOn.invoke(it); redraw() })
                box.addView(rowTop)
                box.addView(sizeBar(Prefs.itemScale(this, sp.key), 50, 250) { Prefs.setItemScale(this, sp.key, it) })
                val isBox = sp.key in setOf("w_clock", "w_weather", "w_date", "w_merge") || sp.key.startsWith("d") || sp.key.startsWith("q")
                if (isBox) box.addView(sizeBar(Prefs.itemW(this, sp.key), 50, 250, "Shape width only (text size stays)") { Prefs.setItemW(this, sp.key, it) })
                if (sp.key == "w_where" && !free) box.addView(whereWidthBar())
                if (isBox || sp.key == "w_where") box.addView(sizeBar(Prefs.itemH(this, sp.key), 50, 250, "Shape height only (text size stays)") { Prefs.setItemH(this, sp.key, it) })
                col.addView(box)
            }
        }

        // ---- 2. Items of the top row (any item can be moved to the other row with Move)
        val itemNames = mapOf("clock" to "Clock", "weather" to "Weather", "date" to "Date", "maps" to "Where to? box",
            "title" to "Title (Inbox)", "count" to "Count badge", "mic" to "Mic", "note" to "+ Note",
            "clear" to "Clear all", "edit" to "Pencil ✎", "gear" to "Settings gear")
        if (!free) {
        col.addView(title("Top row items (left to right)", 16f, 20))
        col.addView(hint("Drag to change the order. To move an item into the Inbox row, or anywhere else, use Move (the button above, or the pencil ✎ on the widget)."))
        col.addView(dragList(Prefs.rows(this)["top"] ?: emptyList(), { key ->
            val h = LinearLayout(this)
            h.orientation = LinearLayout.HORIZONTAL
            h.gravity = Gravity.CENTER_VERTICAL
            val name = TextView(this)
            name.text = itemNames[key] ?: key
            name.textSize = 15f
            h.addView(name, LinearLayout.LayoutParams(0, WRAP, 1f))
            h.addView(smallSwitch(Prefs.itemOn(this, key)) { Prefs.setItemOn(this, key, it); redraw() })
            h
        }) { l ->
            Prefs.setRows(this, l, Prefs.rows(this)["head"] ?: emptyList())
            redraw(); rebuild()
        })
        }

        col.addView(title("Merge into one shape", 16f, 20))
        col.addView(hint("Tick two or more to show them together in one shape (for example clock + date). The shape sits where the first of them is."))
        val mergedNow = Prefs.merged(this)
        for (k in listOf("clock", "weather", "date")) {
            val cb = android.widget.CheckBox(this)
            cb.text = itemNames[k]
            cb.isChecked = k in mergedNow
            cb.setOnCheckedChangeListener { _, v ->
                Prefs.setMerged(this, if (v) Prefs.merged(this) + k else Prefs.merged(this) - k)
                redraw()
            }
            col.addView(cb)
        }
        val horiz = android.widget.CheckBox(this)
        horiz.text = "Side by side (one line) instead of one under the other"
        horiz.isChecked = Prefs.mergeHoriz(this)
        horiz.setOnCheckedChangeListener { _, v -> Prefs.setMergeHoriz(this, v); redraw() }
        col.addView(horiz)

        // ---- 3. Order of the message tiles (apps)
        col.addView(title("Order of the message tiles (apps)", 16f, 20))
        col.addView(hint("Drag an app to where you want its tiles. Pinned apps still come first. Dragging switches \"Order of the tiles\" to My own order."))
        val enabledApps = Prefs.enabled(this).toList()
        if (enabledApps.isEmpty()) {
            col.addView(hint("No apps chosen yet. Choose them on the main screen first."))
        } else {
            val saved = Prefs.appOrder(this).filter { it in enabledApps }
            val mine = saved + enabledApps.filter { it !in saved }.sortedBy { Feed.label(this, it).lowercase() }
            col.addView(dragList(mine, { pkg ->
                val name = TextView(this)
                name.text = Feed.label(this, pkg)
                name.textSize = 15f
                name
            }) { l ->
                Prefs.setAppOrder(this, l)
                Prefs.setSortMode(this, 2)
                TilesWidget.refresh(applicationContext)
                rebuild()
            })
        }

        val reset = Button(this)
        reset.text = "Reset widget arrangement"
        reset.setOnClickListener {
            AlertDialog.Builder(this).setMessage("Put the sections, the top row and all sizes back to how they were?")
                .setPositiveButton("Reset") { _, _ -> Prefs.resetArrange(this); redraw(); rebuild() }
                .setNegativeButton("Cancel", null).show()
        }
        col.addView(reset, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(20) })
    }

    /** Shown instead of the sections list while the free layout is on: what it is, snap, Where to? width, back to rows. */
    private fun addFreeSection() {
        col.addView(title("Free layout is ON", 16f, 16))
        col.addView(hint("Every item sits where you put it with Move, so the sections list and the top-row order are not used. Sizes, colours, switches and Merge below still work."))
        val sw = Switch(this)
        sw.text = "Snap to a small grid while moving"
        sw.isChecked = Prefs.freeSnap(this)
        sw.setOnCheckedChangeListener { _, v -> Prefs.setFreeSnap(this, v) }
        col.addView(sw, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

        val wp = Prefs.freePos(this)["maps"]
        if (wp != null) {
            val cur = (if (wp.w > 0f) wp.w else 150f).toInt().coerceIn(80, 320)
            val t = TextView(this)
            t.text = "Where to? box width: $cur dp"
            t.textSize = 12f
            t.alpha = 0.7f
            t.setPadding(0, dp(12), 0, 0)
            col.addView(t)
            val sb = SeekBar(this)
            sb.max = 240
            sb.progress = cur - 80
            sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                    t.text = "Where to? box width: ${p + 80} dp"
                    if (fromUser) Prefs.setFreeWidth(this@ArrangeActivity, "maps", (p + 80).toFloat())
                }
                override fun onStartTrackingTouch(s: SeekBar) {}
                override fun onStopTrackingTouch(s: SeekBar) { redraw() }
            })
            col.addView(sb)
        }
        val back = Button(this)
        back.text = "Back to the old rows layout"
        back.isAllCaps = false
        back.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("Forget the places of all items and use the rows and sections again?")
                .setPositiveButton("Yes") { _, _ -> Prefs.resetFree(this); redraw(); rebuild() }
                .setNegativeButton("No", null)
                .show()
        }
        col.addView(back, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })
    }

    private fun smallSwitch(on: Boolean, change: (Boolean) -> Unit): View {
        val sw = Switch(this)
        sw.isChecked = on
        sw.setOnCheckedChangeListener { _, v -> change(v) }
        return sw
    }

    /** Width of the Where to? box in the rows layout: far left = fills the row (as before), otherwise a width in dp. */
    private fun whereWidthBar(): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val t = TextView(this)
        t.textSize = 12f
        t.alpha = 0.7f
        fun label(w: Int) = if (w <= 0) "Box width: fills the row (default)" else "Box width: $w dp"
        val cur = Prefs.whereWidth(this)
        t.text = label(cur)
        box.addView(t)
        val sb = SeekBar(this)
        sb.max = 280
        sb.progress = if (cur <= 0) 0 else (cur - 79).coerceIn(1, 280)
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                val w = if (p <= 0) 0 else p + 79
                t.text = label(w)
                if (fromUser) Prefs.setWhereWidth(this@ArrangeActivity, w)
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) { redraw() }
        })
        box.addView(sb)
        return box
    }

    private fun sizeBar(value: Int, min: Int = 50, max: Int = 200, label: String = "Size", set: (Int) -> Unit): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val t = TextView(this)
        t.text = "$label: $value%"
        t.textSize = 12f
        t.alpha = 0.7f
        box.addView(t)
        val sb = SeekBar(this)
        sb.max = max - min
        sb.progress = (value - min).coerceIn(0, max - min)
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                t.text = "$label: ${p + min}%"
                if (fromUser) set(p + min)
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) { redraw() }
        })
        box.addView(sb)
        return box
    }

    /**
     * A column of rows that can be re-ordered by dragging the ☰ handle. [content] makes the part right of the handle.
     * When a drag ends in a new place, [onMoved] gets the new order of [items] (it should redraw the screen).
     */
    private fun dragList(items: List<String>, content: (String) -> View, onMoved: (List<String>) -> Unit): LinearLayout {
        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        val n = items.size
        val gap = dp(6)
        for ((idx, key) in items.withIndex()) {
            val rowView = LinearLayout(this)
            rowView.orientation = LinearLayout.HORIZONTAL
            rowView.gravity = Gravity.CENTER_VERTICAL
            val bg = GradientDrawable()
            bg.cornerRadius = dp(10).toFloat()
            bg.setColor(0x22888888)
            rowView.background = bg
            rowView.setPadding(dp(4), dp(4), dp(10), dp(4))

            val handle = TextView(this)
            handle.text = "☰"
            handle.textSize = 22f
            handle.gravity = Gravity.CENTER
            rowView.addView(handle, LinearLayout.LayoutParams(dp(52), dp(52)))
            rowView.addView(content(key), LinearLayout.LayoutParams(0, WRAP, 1f))

            var startY = 0f
            var from = idx
            var target = idx
            var tops = IntArray(n)
            var hs = IntArray(n)
            handle.setOnTouchListener { v, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        from = list.indexOfChild(rowView)
                        target = from
                        tops = IntArray(n) { list.getChildAt(it).top }
                        hs = IntArray(n) { list.getChildAt(it).height }
                        startY = ev.rawY
                        rowView.elevation = dp(8).toFloat()
                        rowView.alpha = 0.9f
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dy = ev.rawY - startY
                        rowView.translationY = dy
                        val center = tops[from] + hs[from] / 2f + dy
                        var t = from
                        for (j in 0 until n) if (center >= tops[j] && center < tops[j] + hs[j] + gap) t = j
                        if (center < tops[0]) t = 0
                        if (center >= tops[n - 1] + hs[n - 1]) t = n - 1
                        target = t
                        val shift = (hs[from] + gap).toFloat()
                        for (j in 0 until n) {
                            if (j == from) continue
                            list.getChildAt(j).translationY = when {
                                from < t && j in (from + 1)..t -> -shift
                                from > t && j in t until from -> shift
                                else -> 0f
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        for (j in 0 until n) {
                            val ch = list.getChildAt(j)
                            ch.translationY = 0f
                            ch.elevation = 0f
                            ch.alpha = 1f
                        }
                        if (ev.actionMasked == MotionEvent.ACTION_UP && target != from) {
                            val l = items.toMutableList()
                            val item = l.removeAt(from)
                            l.add(target, item)
                            list.post { onMoved(l) }
                        }
                        true
                    }
                    else -> true
                }
            }
            list.addView(rowView, LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = gap })
        }
        return list
    }
}
