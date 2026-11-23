package com.example.notiftiles

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Typeface
import android.os.Bundle
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
 * Move: shows the real widget and lets you press and drag any item (clock, date, weather, Where to?, title, count,
 * mic, + Note, Clear all, pencil, gear) into either row and reorder it between existing items. The item goes where you let go, the
 * change is saved at once and the widget on the home screen follows.
 */
class MoveActivity : Activity() {

    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private lateinit var holder: FrameLayout
    private lateinit var note: TextView
    private var hiRow: View? = null

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(16), dp(16), dp(32))

        val heading = TextView(this)
        heading.text = "Move items"
        heading.textSize = 20f
        heading.setTypeface(heading.typeface, Typeface.BOLD)
        col.addView(heading)

        val hint = TextView(this)
        hint.text = "Drag any item into either row. Drop between items to choose its exact left-to-right position; items can be moved and reordered freely between the Top row and Inbox row. Changes save immediately. Hidden items must be shown in Arrange & resize first."
        hint.textSize = 13f
        hint.alpha = 0.7f
        hint.setPadding(0, dp(2), 0, dp(6))
        col.addView(hint)

        note = TextView(this)
        note.textSize = 13f
        note.setPadding(0, dp(6), 0, dp(6))
        note.visibility = View.GONE
        col.addView(note)

        holder = FrameLayout(this)
        col.addView(holder, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

        val done = Button(this)
        done.text = "Done"
        done.setOnClickListener { finish() }
        col.addView(done, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(16) })

        val scroll = ScrollView(this)
        scroll.addView(col)
        setContentView(scroll)
        render()
    }

    private fun showNote(text: String) {
        note.text = text
        note.visibility = View.VISIBLE
    }

    /** Draws the real widget (built the same way the home screen gets it) and makes its items draggable. */
    private fun render() {
        holder.removeAllViews()
        hiRow = null
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, TilesWidget::class.java))
        if (ids.isEmpty()) {
            showNote("Add the NotifTiles widget to your home screen first, then come back here.")
            return
        }
        val view: View
        try {
            view = TilesWidget.build(this, ids[0]).apply(this, holder)
        } catch (e: Throwable) {
            android.util.Log.e("NotifTilesMove", "Could not draw the widget preview", e)
            showNote("The widget could not be drawn here: $e")
            return
        }
        val h = mgr.getAppWidgetOptions(ids[0]).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0).coerceAtLeast(260)
        holder.addView(view, FrameLayout.LayoutParams(MATCH, dp(h)))
        note.visibility = View.GONE
        // Each step is separate, so a problem in one never hides the widget itself
        try {
            clearClicks(view)
            view.findViewById<View>(R.id.grid)?.visibility = View.INVISIBLE
        } catch (e: Throwable) {
            android.util.Log.e("NotifTilesMove", "Preview setup failed", e)
        }
        for ((key, id) in TilesWidget.ITEM_VIEW) {
            try {
                val item = view.findViewById<View>(id) ?: continue
                if (item.visibility == View.VISIBLE) attachDrag(item, key)
            } catch (e: Throwable) {
                android.util.Log.e("NotifTilesMove", "Could not make $key draggable", e)
            }
        }
    }

    /** The preview only shows the layout: taps must not open the app or the panel. */
    private fun clearClicks(v: View) {
        v.setOnClickListener(null)
        v.isClickable = false
        v.isLongClickable = false
        if (v is ViewGroup) for (i in 0 until v.childCount) clearClicks(v.getChildAt(i))
    }

    private fun keyOf(v: View): String? = TilesWidget.ITEM_VIEW.entries.firstOrNull { it.value == v.id }?.key

    private fun rowViews(): Map<String, View> {
        val m = LinkedHashMap<String, View>()
        holder.findViewById<View>(R.id.top_row)?.let { m["top"] = it }
        holder.findViewById<View>(R.id.header)?.let { m["head"] = it }
        return m
    }

    /** The row under a screen height (within reach of a row), or null when the finger is far from both rows. */
    private fun rowAt(y: Float): String? {
        var best: String? = null
        var bestD = Float.MAX_VALUE
        for ((name, row) in rowViews()) {
            val loc = IntArray(2)
            row.getLocationOnScreen(loc)
            val top = loc[1].toFloat()
            val bottom = top + row.height
            val d = when {
                y < top -> top - y
                y > bottom -> y - bottom
                else -> 0f
            }
            if (d < bestD) {
                bestD = d
                best = name
            }
        }
        return if (bestD <= dp(48)) best else null
    }

    /** Lights up the row an item would be dropped in. */
    private fun highlight(name: String?) {
        val row = name?.let { rowViews()[it] }
        if (row === hiRow) return
        hiRow?.background = null
        hiRow = row
        row?.setBackgroundColor(0x40000000 or (Prefs.accent(this) and 0xFFFFFF))
    }

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
                        highlight(rowAt(ev.rawY))
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val didMove = moving
                    moving = false
                    val loc = IntArray(2)
                    v.getLocationOnScreen(loc)
                    val cx = loc[0] + v.width / 2f
                    val cy = loc[1] + v.height / 2f
                    v.translationX = 0f
                    v.translationY = 0f
                    v.elevation = 0f
                    v.alpha = 1f
                    highlight(null)
                    if (didMove && ev.actionMasked == MotionEvent.ACTION_UP) drop(v, key, cx, cy)
                    true
                }
                else -> true
            }
        }
    }

    /** Puts the dragged item (or the whole merged shape) into the row and place it was dropped on, and saves it. */
    private fun drop(dragged: View, key: String, cx: Float, cy: Float) {
        val target = rowAt(cy) ?: return
        val group = rowViews()[target] as? ViewGroup ?: return
        // The items on show in that row, left to right, not counting the one being moved
        val kids = ArrayList<Pair<String, Float>>()
        for (i in 0 until group.childCount) {
            val c = group.getChildAt(i)
            if (c === dragged || c.visibility != View.VISIBLE) continue
            val k = keyOf(c) ?: continue
            val loc = IntArray(2)
            c.getLocationOnScreen(loc)
            kids.add(k to (loc[0] + c.width / 2f))
        }
        // Pick the insertion point by the nearest visible item's center. This makes drops
        // between items deterministic, including when the widget is scaled in this preview.
        val index = kids.indexOfFirst { it.second >= cx }.let { if (it < 0) kids.size else it }

        val rows = Prefs.rows(this)
        val oldTop = rows["top"] ?: emptyList()
        val merged = Prefs.merged(this)
        // The merged shape moves as one: all of its items
        val moving = if (key == "merge") oldTop.filter { it in merged } else listOf(key)
        if (moving.isEmpty()) return

        val top = oldTop.filter { it !in moving }.toMutableList()
        val head = (rows["head"] ?: emptyList()).filter { it !in moving }.toMutableList()
        val list = if (target == "top") top else head
        // Insert before the item that is on show at that place (the merged shape counts as its first item)
        val anchor = kids.getOrNull(index)?.first
        val anchorKey = if (anchor == "merge") oldTop.firstOrNull { it in merged } else anchor
        val at = if (anchorKey != null) list.indexOf(anchorKey) else -1
        if (at >= 0) list.addAll(at, moving) else list.addAll(moving)

        // Items that leave the top row are no longer merged with the others
        Prefs.setRows(this, top, head)
        Prefs.setMerged(this, merged.filter { it in top }.toSet())
        TilesWidget.rebuildAll(applicationContext)
        holder.post { render() }
    }
}
