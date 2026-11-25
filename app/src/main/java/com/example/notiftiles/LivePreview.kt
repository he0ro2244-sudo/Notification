package com.example.notiftiles

import android.animation.ValueAnimator
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A live copy of the widget pinned above a settings screen. It is drawn the same way the home screen gets the widget,
 * and draws itself again a moment after any setting changes, so you see the result without leaving the screen.
 * Tap the grey bar to change its size: Off / Small / Medium / Full.
 */
class LivePreview(private val act: Activity) {
    private val dm = act.resources.displayMetrics
    private fun dp(v: Float) = Math.round(v * dm.density)
    private val prefs = act.getSharedPreferences("nt", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private var mode = prefs.getInt("ui_preview", 1).coerceIn(0, 3)
    private val root = LinearLayout(act)
    private val bar = TextView(act)
    private val box = object : FrameLayout(act) {
        // The preview is only to look at: taps never reach the widget's buttons
        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true
    }
    private val again = Runnable { render() }
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != "ui_preview") refreshSoon()
    }

    /** Puts the preview above [scroll] and returns the screen to show. It also takes care of the system bars. */
    fun wrap(scroll: View): View {
        root.orientation = LinearLayout.VERTICAL
        root.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(0, b.top, 0, b.bottom)
            insets
        }
        bar.textSize = 13f
        bar.setPadding(dp(16f), dp(8f), dp(16f), dp(8f))
        bar.setBackgroundColor(0x24808080)
        bar.setOnClickListener {
            mode = (mode + 1) % 4
            prefs.edit().putInt("ui_preview", mode).apply()
            render()
        }
        box.layoutDirection = View.LAYOUT_DIRECTION_LTR
        box.clipChildren = true
        box.setBackgroundColor(0x14808080)
        root.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(box, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        updateBar()
        return root
    }

    fun start() {
        prefs.registerOnSharedPreferenceChangeListener(listener)
        render()
    }

    fun stop() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
        handler.removeCallbacks(again)
    }

    fun refreshSoon() {
        handler.removeCallbacks(again)
        handler.postDelayed(again, 250)
    }

    private fun updateBar() {
        val name = when (mode) { 0 -> "off"; 1 -> "small"; 2 -> "medium"; else -> "full size" }
        bar.text = if (mode == 0) "Live preview: off  (tap to show it)" else "Live preview: $name  (tap to change the size)"
    }

    private fun note(t: String) {
        val tv = TextView(act)
        tv.text = t
        tv.textSize = 13f
        tv.setPadding(dp(16f), dp(10f), dp(16f), dp(10f))
        box.addView(tv)
        box.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun render() {
        if (demoing) return
        box.removeAllViews()
        updateBar()
        if (mode == 0) {
            box.visibility = View.GONE
            return
        }
        box.visibility = View.VISIBLE
        val mgr = AppWidgetManager.getInstance(act)
        val ids = mgr.getAppWidgetIds(ComponentName(act, TilesWidget::class.java))
        if (ids.isEmpty()) {
            note("Add the NotifTiles widget to your home screen to see a live preview here.")
            return
        }
        try {
            val view = TilesWidget.build(act.applicationContext, ids[0]).apply(act, box)
            val opt = mgr.getAppWidgetOptions(ids[0])
            val screenW = Math.round(dm.widthPixels / dm.density)
            val wOpt = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val widthDp = (if (wOpt > 0) wOpt else 320).coerceIn(120, screenW)
            val hMax = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
            val hMin = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val heightDp = (if (hMax > 0) hMax else hMin).coerceAtLeast(260)
            val s = when (mode) { 1 -> 0.5f; 2 -> 0.75f; else -> 1f }
            view.findViewById<View>(R.id.grid)?.visibility = View.INVISIBLE
            view.pivotX = dp(widthDp.toFloat()) / 2f
            view.pivotY = 0f
            view.scaleX = s
            view.scaleY = s
            box.addView(view, FrameLayout.LayoutParams(dp(widthDp.toFloat()), dp(heightDp.toFloat()), Gravity.CENTER_HORIZONTAL))
            // Never take more than about a third of the screen: a tall widget is cropped, its top part is what you edit
            val cap = Math.round(dm.heightPixels * 0.36f)
            box.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, minOf(dp(heightDp * s), cap))
        } catch (e: Throwable) {
            android.util.Log.e("NotifTilesPreview", "Could not draw the live preview", e)
            note("The preview could not be drawn here.")
        }
    }

    // ---------------------------------------------------------------- 1.5 second demo of the animation you just tapped
    private var demoing = false
    private val endDemo = Runnable { demoing = false; box.setBackgroundColor(0x14808080); render() }

    /** Plays [key] in [style] (an entry of Alive.ELEMENTS, or "speed") for about 1.5 s in the preview area. */
    fun demo(key: String, style: String) {
        if (!Prefs.aliveOn(act)) return
        val T = { ms: Long -> Alive.t(act, ms) }
        val fso = android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)
        handler.removeCallbacks(endDemo); handler.removeCallbacks(again)
        demoing = true
        box.removeAllViews()
        box.visibility = View.VISIBLE
        val h = dp(120f)
        box.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h)
        val accent = Prefs.accent(act)
        fun tv(txt: String, sp: Float) = TextView(act).apply { text = txt; textSize = sp; gravity = Gravity.CENTER }
        fun put(v: View, w: Int, hh: Int) { box.addView(v, FrameLayout.LayoutParams(w, hh, Gravity.CENTER)) }
        fun dot(sz: Float, stroke: Boolean) = View(act).apply {
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                if (stroke) setStroke(dp(2f), accent) else setColor(accent)
            }
            layoutParams = FrameLayout.LayoutParams(dp(sz), dp(sz), Gravity.CENTER)
        }
        val icon = tv(when (key) { "note" -> "✏️"; "mic" -> "🎙️"; "refresh" -> "🔄"; "edit" -> "✏️"; "gear" -> "⚙️"; "title", "badge", "row" -> "📬"; "clear" -> "🧹"; else -> "📝" }, 34f)
        val cardView = FrameLayout(act).apply {
            background = android.graphics.drawable.GradientDrawable().apply { cornerRadius = dp(16f).toFloat(); setColor(accent and 0x00FFFFFF or 0x66000000) }
        }
        when (key) {
            "open", "close" -> {
                val card = cardView; put(card, dp(150f), dp(84f))
                val s = if (key == "close") Alive.chosen(act, "open") else style
                card.post {
                    card.pivotX = card.width * 0.5f; card.pivotY = card.height * 0.5f
                    when (s) {
                        "slide" -> { card.translationY = dp(40f).toFloat(); card.alpha = 0f; card.animate().translationY(0f).alpha(1f).setDuration(T(380)).setInterpolator(fso).start() }
                        "circle" -> { try { android.view.ViewAnimationUtils.createCircularReveal(card, card.width / 2, card.height / 2, 0f, card.width * 0.6f).setDuration(T(420)).start() } catch (e: Exception) { } }
                        "zoom" -> { card.scaleX = 0.9f; card.scaleY = 0.9f; card.alpha = 0f; box.setBackgroundColor(0x44000000); card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(T(400)).setInterpolator(android.view.animation.DecelerateInterpolator(1.6f)).start() }
                        "off" -> {}
                        else -> { card.scaleX = 0.86f; card.scaleY = 0.86f; card.alpha = 0f; card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(T(380)).setInterpolator(android.view.animation.OvershootInterpolator(1.1f)).start() }
                    }
                    // and back out, so the close half is seen too
                    handler.postDelayed({ if (demoing) card.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(T(300)).start() }, 1100)
                }
            }
            "press" -> { put(icon, dp(60f), dp(60f)); icon.animate().scaleX(0.92f).scaleY(0.92f).setDuration(T(110)).withEndAction { icon.animate().scaleX(1f).scaleY(1f).setDuration(T(380)).setInterpolator(android.view.animation.OvershootInterpolator(3f)).start() }.start() }
            "gear" -> { put(icon, dp(60f), dp(60f))
                if (style == "off") { }
                else if (style == "turn") icon.animate().rotation(200f).setDuration(T(600)).setInterpolator(android.view.animation.OvershootInterpolator(1.6f)).withEndAction { icon.animate().rotation(180f).setDuration(T(250)).start() }.start()
                else icon.animate().rotation(360f).setDuration(T(900)).setInterpolator(fso).start() }
            "refresh" -> { put(icon, dp(60f), dp(60f)); icon.animate().rotation(360f).setDuration(T(if (style == "arc") 1100 else 800)).setInterpolator(fso).start() }
            "mic" -> if (style == "rings") {
                put(icon, dp(60f), dp(60f))
                for (i in 0 until 3) { val r = dot(36f, true); box.addView(r, 0); r.alpha = 0f
                    r.animate().setStartDelay(T(i * 380L)).alpha(0.8f).scaleX(2.6f).scaleY(2.6f).setDuration(T(900)).withEndAction { r.alpha = 0f }.start() }
            } else {
                val row = LinearLayout(act); row.gravity = Gravity.CENTER; put(row, dp(90f), dp(60f))
                for (i in 0 until 5) { val b = View(act).apply { setBackgroundColor(accent) }; row.addView(b, LinearLayout.LayoutParams(dp(8f), dp(14f)).apply { setMargins(dp(3f), 0, dp(3f), 0) })
                    ValueAnimator.ofInt(dp(10f), dp(54f)).apply { duration = T(260L + i * 60L); repeatMode = ValueAnimator.REVERSE; repeatCount = 4
                        addUpdateListener { b.layoutParams = b.layoutParams.apply { height = it.animatedValue as Int }; b.requestLayout() } }.start() }
            }
            "note" -> { put(icon, dp(60f), dp(60f))
                if (style == "ink") ValueAnimator.ofFloat(-12f, 12f).apply { duration = 160; repeatMode = ValueAnimator.REVERSE; repeatCount = 7; addUpdateListener { icon.rotation = it.animatedValue as Float } }.start()
                else icon.animate().scaleX(1.35f).scaleY(1.35f).setDuration(T(140)).withEndAction { icon.animate().scaleX(1f).scaleY(1f).setDuration(T(300)).setInterpolator(android.view.animation.OvershootInterpolator(2f)).start() }.start() }
            "edit" -> { put(icon, dp(60f), dp(60f))
                if (style == "trail") icon.animate().translationX(dp(40f).toFloat()).translationY(-dp(18f).toFloat()).setDuration(T(500)).setInterpolator(fso).withEndAction { icon.animate().translationX(0f).translationY(0f).setDuration(T(400)).start() }.start()
                else { icon.animate().rotation(-28f).setDuration(T(300)).start()
                    for (i in 0 until 3) { val dd = dot(8f, false); dd.alpha = 0f; (dd.layoutParams as FrameLayout.LayoutParams).leftMargin = dp(60f + i * 16f); box.addView(dd); dd.animate().alpha(1f).setStartDelay(T(300L + i * 200L)).setDuration(T(160)).start() } } }
            else -> { put(icon, dp(60f), dp(60f)); icon.animate().translationY(-dp(14f).toFloat()).setDuration(T(200)).setInterpolator(android.view.animation.DecelerateInterpolator()).withEndAction { icon.animate().translationY(0f).setDuration(T(400)).setInterpolator(android.view.animation.OvershootInterpolator(2f)).start() }.start() }
        }
        handler.postDelayed(endDemo, 1600)
    }
}
