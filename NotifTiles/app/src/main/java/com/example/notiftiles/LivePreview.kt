package com.example.notiftiles

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
    // Shrinks while the settings list is scrolled down, so the preview stays in view without taking the screen
    private var mini = false
    fun autoShrink(): Boolean = prefs.getBoolean("ui_preview_mini", true)
    fun setAutoShrink(v: Boolean) { prefs.edit().putBoolean("ui_preview_mini", v).apply(); if (!v) setMini(false, true) }

    /** Makes the preview small ([on]) or normal again, with a short animation. Ignored while a change is still settling. */
    private var holdUntil = 0L     // after it grew because of a change, it stays big for a moment so it does not jump back at once
    private var settleUntil = 0L   // while it animates (and a moment after) scroll events are ignored: they come from the resize itself
    private var frac = if (mini) 1f else 0f   // 0 = full size, 1 = small strip
    private var animator: ValueAnimator? = null
    private var barH = 0
    private var contentView: View? = null
    private var fullH = 0
    private var miniH = 0
    private var sFull = 1f
    private var sMini = 1f

    /** True while the preview is resizing: the scroll listener must not react to scroll changes caused by the resize. */
    fun isSettling(): Boolean = android.os.SystemClock.uptimeMillis() < settleUntil

    /** Brings the preview back on request (a tap on the screen title or the top edge), even while it is still settling. */
    fun reveal() {
        if (mode == 0 || (!mini && frac == 0f)) return
        holdUntil = android.os.SystemClock.uptimeMillis() + 800   // just long enough for the list to settle; the next scroll hides it again
        setMini(false, true)
    }

    /** Hides it at once (the search box was tapped), whatever else is going on. */
    fun hideNow() {
        if (!autoShrink() || mode == 0 || mini) return
        setMini(true, true)
    }

    fun shrink(on: Boolean) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now < settleUntil) return
        if (on && now < holdUntil) return
        val want = on && autoShrink() && mode != 0
        if (want == mini) return
        setMini(want, true)
    }

    private fun setMini(want: Boolean, animate: Boolean) {
        mini = want
        updateBar()
        animator?.cancel()
        val target = if (want) 1f else 0f
        if (!animate || contentView == null) { applyFrac(target); return }
        settleUntil = android.os.SystemClock.uptimeMillis() + 200 + 250
        animator = ValueAnimator.ofFloat(frac, target).apply {
            duration = 200
            interpolator = android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)
            addUpdateListener { applyFrac(it.animatedValue as Float) }
            start()
        }
    }

    /** Sets how small the preview is (0..1) by scaling the already drawn widget: no rebuild, so nothing flashes. */
    private fun applyFrac(f: Float) {
        frac = f
        if (barH == 0) {
            bar.measure(View.MeasureSpec.makeMeasureSpec(dm.widthPixels, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
            barH = bar.measuredHeight
        }
        val bl = bar.layoutParams as LinearLayout.LayoutParams
        if (mode == 0) {
            bl.height = ViewGroup.LayoutParams.WRAP_CONTENT; bar.alpha = 1f; bar.visibility = View.VISIBLE
        } else {
            bl.height = (barH * (1f - f)).toInt(); bar.alpha = 1f - f
            bar.visibility = if (f >= 1f) View.GONE else View.VISIBLE
        }
        bar.layoutParams = bl
        val v = contentView ?: return
        val s = sFull + (sMini - sFull) * f
        v.scaleX = s; v.scaleY = s
        val h = (fullH + (miniH - fullH) * f).toInt()
        v.translationY = -(fullH - h).toFloat()   // slides up out of view
        val bp = box.layoutParams
        bp.height = h
        box.layoutParams = bp
        box.visibility = if (h <= 0) View.GONE else View.VISIBLE
    }
    private val root = LinearLayout(act)
    private val bar = TextView(act)
    private val box = object : FrameLayout(act) {
        // The preview is only to look at: taps never reach the widget's buttons
        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true
    }
    private val again = Runnable { render() }
    // Keys the app itself writes in the background (weather refresh, location): they must not grow the preview
    private val quietKeys = setOf("w_try", "w_time", "w_gaptime", "w_apptime", "w_temp", "w_code", "w_off", "w_gap", "w_day",
        "w_apptemp", "w_appcode", "w_has", "w_city", "w_lat", "w_lon", "note_app")
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && key.startsWith("ui_")) return@OnSharedPreferenceChangeListener
        // Something was changed while the preview was small: it grows so the change can be seen (not for background writes)
        if (mini && (key == null || key !in quietKeys)) {
            holdUntil = android.os.SystemClock.uptimeMillis() + 2500
            setMini(false, true)
        }
        refreshSoon()
    }

    /** Puts the preview above [scroll] and returns the screen to show. It also takes care of the system bars. */
    fun wrap(scroll: View): View {
        root.orientation = LinearLayout.VERTICAL
        root.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(0, b.top, 0, b.bottom)
            insets
        }
        root.setOnClickListener { reveal() }   // the empty strip at the very top
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
        bar.text = if (mode == 0) "Live preview: off  (tap to show it)"
        else if (mini) "Live preview: hidden while you scroll  (tap to change the size)"
        else "Live preview: $name  (tap to change the size)"
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
        contentView = null
        updateBar()
        if (mode == 0) {
            box.visibility = View.GONE
            applyFrac(0f)
            return
        }
        box.visibility = View.VISIBLE
        val mgr = AppWidgetManager.getInstance(act)
        val ids = mgr.getAppWidgetIds(ComponentName(act, TilesWidget::class.java))
        if (ids.isEmpty()) {
            note("Add the NotifTiles widget to your home screen to see a live preview here.")
            applyFrac(frac)
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
            sFull = when (mode) { 1 -> 0.5f; 2 -> 0.75f; else -> 1f }
            sMini = sFull   // hidden, not scaled: it just slides away
            view.findViewById<View>(R.id.grid)?.visibility = View.INVISIBLE
            view.pivotX = dp(widthDp.toFloat()) / 2f
            view.pivotY = 0f
            box.addView(view, FrameLayout.LayoutParams(dp(widthDp.toFloat()), dp(heightDp.toFloat()), Gravity.CENTER_HORIZONTAL))
            contentView = view
            // Never take more than about a third of the screen: a tall widget is cropped, its top part is what you edit
            fullH = minOf(dp(heightDp * sFull), Math.round(dm.heightPixels * 0.36f))
            miniH = 0   // scrolled down: hidden completely
            box.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, fullH)
            applyFrac(frac)   // keeps a running shrink/grow going instead of jumping
        } catch (e: Throwable) {
            android.util.Log.e("NotifTilesPreview", "Could not draw the live preview", e)
            note("The preview could not be drawn here.")
            applyFrac(frac)
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
        contentView = null
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
        if (key != "open" && FxStyles.isNew(key, style)) {
            if (key == "list") {
                // three tiles come in one after another
                val colv = LinearLayout(act).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
                put(colv, dp(150f), dp(100f))
                for (i in 0 until 3) {
                    val tile = View(act).apply { background = cardView.background.constantState?.newDrawable()?.mutate() ; alpha = 0f }
                    colv.addView(tile, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24f)).apply { setMargins(0, dp(3f), 0, dp(3f)) })
                    FxStyles.listIn(act, tile, style, T(i * 110L), i)
                }
            } else {
                put(icon, dp(60f), dp(60f))
                icon.post {
                    FxStyles.play(act, icon, key, style)
                    if (key == "press") handler.postDelayed({ if (demoing) FxStyles.release(act, icon) }, 500)
                }
            }
        } else when (key) {
            "open", "close" -> {
                val card = cardView; put(card, dp(150f), dp(84f))
                val s = if (key == "close") Alive.chosen(act, "open") else style
                card.post {
                    card.pivotX = card.width * 0.5f; card.pivotY = card.height * 0.5f
                    when (s) {
                        "flip", "drop", "unfold", "swing", "tiltup", "burst" -> FxStyles.open(act, card, s)
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
                // three concentric rings spread out from the mic while it "listens"
                put(icon, dp(60f), dp(60f))
                for (i in 0 until 3) { val r = dot(36f, true); box.addView(r, 0); r.alpha = 0f
                    r.animate().setStartDelay(T(i * 300L)).alpha(0.8f).scaleX(2.6f).scaleY(2.6f).setDuration(T(800)).withEndAction { r.alpha = 0f }.start() }
                icon.animate().scaleX(1.12f).scaleY(1.12f).setDuration(T(160)).withEndAction { icon.animate().scaleX(1f).scaleY(1f).setDuration(T(240)).start() }.start()
            } else {
                // equalizer bars bounce with scaleY (hardware accelerated, no layout work)
                val row = LinearLayout(act); row.gravity = Gravity.CENTER; put(row, dp(110f), dp(60f))
                for (i in 0 until 7) { val b = View(act).apply { setBackgroundColor(accent) }
                    row.addView(b, LinearLayout.LayoutParams(dp(8f), dp(48f)).apply { setMargins(dp(3f), 0, dp(3f), 0) })
                    b.scaleY = 0.2f
                    ValueAnimator.ofFloat(0.2f, 1f).apply { duration = T(190L + i * 45L); repeatMode = ValueAnimator.REVERSE; repeatCount = 6
                        interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                        addUpdateListener { b.scaleY = it.animatedValue as Float }
                        addListener(object : AnimatorListenerAdapter() { override fun onAnimationEnd(a: Animator) { b.animate().scaleY(0.2f).setDuration(T(160)).start() } }) }.start() }
            }
            "note" -> { put(icon, dp(60f), dp(60f))
                if (style == "ink") {
                    // the pen tilts and scribbles a short stroke while you "type", then settles back
                    val stroke = ScribbleView(act, accent)
                    box.addView(stroke, FrameLayout.LayoutParams(dp(90f), dp(14f), Gravity.CENTER).apply { topMargin = dp(46f) })
                    stroke.play(T(1200))
                    ValueAnimator.ofFloat(-14f, 14f).apply { duration = T(170); repeatMode = ValueAnimator.REVERSE; repeatCount = 6
                        addUpdateListener { icon.rotation = it.animatedValue as Float }
                        addListener(object : AnimatorListenerAdapter() { override fun onAnimationEnd(a: Animator) { icon.animate().rotation(0f).setDuration(T(200)).start() } }) }.start()
                } else if (style == "ripple") {
                    // one crisp stamp: the icon presses down, a ring spreads out
                    val ring = dot(36f, true); box.addView(ring, 0); ring.alpha = 0f
                    ring.animate().alpha(0.8f).scaleX(2.4f).scaleY(2.4f).setDuration(T(520)).withEndAction { ring.alpha = 0f }.start()
                    icon.animate().scaleX(0.8f).scaleY(0.8f).setDuration(T(110)).withEndAction { icon.animate().scaleX(1f).scaleY(1f).setDuration(T(380)).setInterpolator(android.view.animation.OvershootInterpolator(3f)).start() }.start()
                } }
            "edit" -> { put(icon, dp(60f), dp(60f))
                if (style == "trail") { val tr = ScribbleView(act, accent); box.addView(tr, 0, FrameLayout.LayoutParams(dp(90f), dp(14f), Gravity.CENTER).apply { topMargin = dp(40f) }); tr.play(T(900))
                    icon.animate().translationX(dp(40f).toFloat()).translationY(-dp(18f).toFloat()).setDuration(T(500)).setInterpolator(fso).withEndAction { icon.animate().translationX(0f).translationY(0f).setDuration(T(400)).start() }.start() }
                else { icon.animate().rotation(-28f).setDuration(T(300)).start()
                    for (i in 0 until 3) { val dd = dot(8f, false); dd.alpha = 0f; (dd.layoutParams as FrameLayout.LayoutParams).leftMargin = dp(60f + i * 16f); box.addView(dd); dd.animate().alpha(1f).setStartDelay(T(300L + i * 200L)).setDuration(T(160)).start() } } }
            else -> { put(icon, dp(60f), dp(60f)); icon.animate().translationY(-dp(14f).toFloat()).setDuration(T(200)).setInterpolator(android.view.animation.DecelerateInterpolator()).withEndAction { icon.animate().translationY(0f).setDuration(T(400)).setInterpolator(android.view.animation.OvershootInterpolator(2f)).start() }.start() }
        }
        handler.postDelayed(endDemo, 1600)
    }
}

/** A short scribbled stroke that draws itself, then fades. Used by the demo of the pen and pencil styles. */
private class ScribbleView(c: Context, col: Int) : View(c) {
    private var p = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = col; style = Paint.Style.STROKE
        strokeWidth = 3f * c.resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    fun play(ms: Long) {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ms
            addUpdateListener { p = it.animatedValue as Float; invalidate() }
        }.start()
    }
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val n = 40
        val end = (p / 0.8f).coerceAtMost(1f)
        val path = Path()
        for (k in 0..(n * end).toInt()) {
            val u = k / n.toFloat()
            val x = w * u
            val y = h / 2f + Math.sin(u * Math.PI * 8.0).toFloat() * h * 0.35f
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        paint.alpha = (255 * (1f - ((p - 0.8f) / 0.2f).coerceIn(0f, 1f))).toInt()
        cv.drawPath(path, paint)
    }
}
