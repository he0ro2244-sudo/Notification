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

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun dpf(n: Float) = (n * resources.displayMetrics.density).toInt()
    private val density: Float get() = resources.displayMetrics.density

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
        hint.text = "Press and drag any item of the widget to any place, then let go. Every item (clock, date, weather, Where to?, each fast-dial circle, each shortcut circle, title, count, mic, + Note, Clear all, pencil, gear) stays exactly where you drop it. Drag the bar \"Messages start here\" to choose where the message tiles begin. A hidden item is shown again in Arrange & resize first."
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

        val back = Button(this)
        back.text = "Back to the old rows layout"
        back.isAllCaps = false
        back.setOnClickListener {
            Prefs.resetFree(this)
            TilesWidget.rebuildAll(applicationContext)
            render()
        }
        col.addView(back, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(16) })

        val done = Button(this)
        done.text = "Done"
        done.setOnClickListener { finish() }
        col.addView(done, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })

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
        if (Prefs.freeOn(this)) return Prefs.freePos(this).toMutableMap()
        Prefs.setFreeGridTop(this, measureGridTop())
        return measureAll()
    }

    /** The dragged item keeps the place where it was let go. */
    private fun drop(dragged: View, key: String, dxDp: Float, dyDp: Float) {
        val rl = rootLoc()
        val places = currentPlaces()
        val p0 = placeOf(dragged, rl)
        val old = places[key]
        val maxX = (widthDp - 24).toFloat().coerceAtLeast(0f)
        val maxY = (heightDp - 24).toFloat().coerceAtLeast(0f)
        val x = (p0.first + dxDp).coerceIn(0f, maxX)
        val y = (p0.second + dyDp).coerceIn(0f, maxY)
        val w = old?.w ?: (if (key == "maps") dragged.width / density else 0f)
        places[key] = Prefs.FreePos(x, y, w)
        Prefs.setFreePos(this, places)
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
                        val newTop = ((startTop + dy) / density).coerceIn(0f, maxTop)
                        val places = currentPlaces()
                        Prefs.setFreePos(this, places)
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
