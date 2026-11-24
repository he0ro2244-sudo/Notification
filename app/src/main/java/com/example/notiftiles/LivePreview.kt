package com.example.notiftiles

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
}
