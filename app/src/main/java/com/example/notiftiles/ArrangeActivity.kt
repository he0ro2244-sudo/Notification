package com.example.notiftiles

import android.app.Activity
import android.app.AlertDialog
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

        // ---- 1. Sections of the widget
        col.addView(title("Sections (top to bottom)", 16f, 16))
        col.addView(hint("The message tiles always fill the space under these sections."))
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
            box.addView(sizeBar(Prefs.blockScale(this, key)) { Prefs.setBlockScale(this, key, it) })
            box
        }) { l -> Prefs.setBlockOrder(this, l); redraw(); rebuild() })

        // ---- Inbox row items: show / hide and size of each
        col.addView(title("Inbox row items", 16f, 20))
        col.addView(hint("Show or hide each item and make it bigger or smaller (on top of the section size above)."))
        val hdrItems = listOf("w_title" to "Title (Inbox)", "w_count" to "Count badge", "w_mic" to "Mic", "w_note" to "+ Note",
            "w_clear" to "Clear all", "w_gear" to "Settings gear")
        for ((k, label) in hdrItems) {
            val box = LinearLayout(this)
            box.orientation = LinearLayout.VERTICAL
            val top = LinearLayout(this)
            top.orientation = LinearLayout.HORIZONTAL
            top.gravity = Gravity.CENTER_VERTICAL
            val nm = TextView(this)
            nm.text = label
            nm.textSize = 15f
            top.addView(nm, LinearLayout.LayoutParams(0, WRAP, 1f))
            top.addView(smallSwitch(Prefs.showItem(this, k)) { Prefs.setShowItem(this, k, it); redraw() })
            box.addView(top)
            if (k in Prefs.ITEM_KEYS) box.addView(sizeBar(Prefs.itemScale(this, k), 50, 250) { Prefs.setItemScale(this, k, it) })
            col.addView(box)
        }
        val pen = android.widget.Switch(this)
        pen.text = "Show a ✎ button on the widget that opens this screen"
        pen.isChecked = Prefs.editBtn(this)
        pen.setOnCheckedChangeListener { _, v -> Prefs.setEditBtn(this, v); redraw() }
        col.addView(pen)

        // ---- 2. Items of the top row
        col.addView(title("Top row items (left to right)", 16f, 20))
        col.addView(hint("Move \"Where to?\" between the circles, or reorder the circles."))
        val circles = Prefs.order(this).toMutableList()
        val row = circles.toMutableList()
        row.add(Prefs.pillPos(this).coerceIn(0, 3), "maps")
        val itemNames = mapOf("clock" to "Clock", "weather" to "Weather", "date" to "Date", "maps" to "Where to? box")
        col.addView(dragList(row, { key ->
            val h = LinearLayout(this)
            h.orientation = LinearLayout.HORIZONTAL
            h.gravity = Gravity.CENTER_VERTICAL
            val name = TextView(this)
            name.text = itemNames[key]
            name.textSize = 15f
            h.addView(name, LinearLayout.LayoutParams(0, WRAP, 1f))
            h.addView(smallSwitch(Prefs.show(this, key)) { Prefs.setShow(this, key, it); redraw() })
            h
        }) { l ->
            Prefs.setOrder(this, l.filter { it != "maps" })
            Prefs.setPillPos(this, l.indexOf("maps"))
            redraw(); rebuild()
        })

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

    private fun smallSwitch(on: Boolean, change: (Boolean) -> Unit): View {
        val sw = Switch(this)
        sw.isChecked = on
        sw.setOnCheckedChangeListener { _, v -> change(v) }
        return sw
    }

    private fun sizeBar(value: Int, min: Int = 50, max: Int = 200, set: (Int) -> Unit): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val t = TextView(this)
        t.text = "Size: $value%"
        t.textSize = 12f
        t.alpha = 0.7f
        box.addView(t)
        val sb = SeekBar(this)
        sb.max = max - min
        sb.progress = (value - min).coerceIn(0, max - min)
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                t.text = "Size: ${p + min}%"
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
