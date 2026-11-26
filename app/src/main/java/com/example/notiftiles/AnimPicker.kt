package com.example.notiftiles

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * The animation picker, laid out like the emoji panel of a keyboard: tabs along the bottom (Panel, Touch, Messages,
 * Notes), a grid of styles above them. Tap a style to choose it; it plays at once in the live preview above the panel.
 * It sits at the bottom of the screen so the preview stays in view. The old list of choices in Settings still works.
 */
object AnimPicker {
    /** [onPlay] shows the style in the live preview; [onDone] runs when the panel closes (to redraw Settings). */
    fun show(act: Activity, onPlay: (key: String, style: String) -> Unit, onDone: () -> Unit) {
        val d = act.resources.displayMetrics.density
        fun dp(v: Float) = (v * d).toInt()
        fun attr(a: Int, fallback: Int): Int {
            val tv = TypedValue()
            return if (act.theme.resolveAttribute(a, tv, true) && tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) tv.data else fallback
        }
        val bg = attr(android.R.attr.colorBackground, Color.WHITE)
        val fg = attr(android.R.attr.textColorPrimary, Color.BLACK)
        val accent = Prefs.accent(act)
        var changed = false

        val dlg = Dialog(act)
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val root = LinearLayout(act)
        root.orientation = LinearLayout.VERTICAL
        root.background = GradientDrawable().apply {
            setColor(bg); cornerRadii = floatArrayOf(dp(20f).toFloat(), dp(20f).toFloat(), dp(20f).toFloat(), dp(20f).toFloat(), 0f, 0f, 0f, 0f)
        }

        // top line: name of the tab + close
        val top = LinearLayout(act)
        top.gravity = Gravity.CENTER_VERTICAL
        top.setPadding(dp(16f), dp(10f), dp(8f), 0)
        val heading = TextView(act)
        heading.textSize = 15f
        heading.setTextColor(fg)
        heading.typeface = android.graphics.Typeface.DEFAULT_BOLD
        top.addView(heading, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val close = TextView(act)
        close.text = "✕"
        close.textSize = 18f
        close.setTextColor(fg)
        close.setPadding(dp(14f), dp(6f), dp(14f), dp(6f))
        close.setOnClickListener { dlg.dismiss() }
        top.addView(close)
        root.addView(top)

        val scroll = ScrollView(act)
        val grid = LinearLayout(act)
        grid.orientation = LinearLayout.VERTICAL
        grid.setPadding(dp(12f), dp(4f), dp(12f), dp(8f))
        scroll.addView(grid)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val tabs = LinearLayout(act)
        tabs.orientation = LinearLayout.HORIZONTAL
        tabs.setBackgroundColor(0x1A808080)
        val tabViews = ArrayList<TextView>()
        root.addView(HorizontalScrollView(act).apply { isHorizontalScrollBarEnabled = false; addView(tabs) },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        fun card(label: String, selected: Boolean, onTap: () -> Unit): TextView {
            val tv = TextView(act)
            tv.text = label
            tv.textSize = 13f
            tv.gravity = Gravity.CENTER
            tv.setTextColor(fg)
            tv.setPadding(dp(6f), dp(10f), dp(6f), dp(10f))
            tv.minHeight = dp(52f)
            tv.background = GradientDrawable().apply {
                cornerRadius = dp(12f).toFloat()
                setColor(if (selected) (accent and 0x00FFFFFF) or 0x33000000 else 0x14808080)
                if (selected) setStroke(dp(2f), accent)
            }
            tv.setOnClickListener { onTap() }
            return tv
        }

        var current = 0
        fun showTab(i: Int, toTop: Boolean = true) {
            current = i
            val tab = FxStyles.TABS[i]
            heading.text = tab.icon + "  " + tab.name
            for ((k, v) in tabViews.withIndex()) {
                v.background = if (k == i) GradientDrawable().apply { cornerRadius = dp(14f).toFloat(); setColor((accent and 0x00FFFFFF) or 0x40000000) } else null
            }
            grid.removeAllViews()
            for (key in tab.keys) {
                val e = Alive.ELEMENTS.firstOrNull { it.key == key } ?: continue
                val sel = Alive.chosen(act, key)
                val label = TextView(act)
                label.text = e.label
                label.textSize = 12.5f
                label.setTextColor(fg)
                label.alpha = 0.75f
                label.setPadding(dp(4f), dp(12f), dp(4f), dp(6f))
                grid.addView(label)
                val options = e.styles + listOf("off" to "Off")
                var n = 0
                while (n < options.size) {
                    val row = LinearLayout(act)
                    row.orientation = LinearLayout.HORIZONTAL
                    for (k in n until minOf(n + 3, options.size)) {
                        val (id, name) = options[k]
                        val c = card(name, id == sel) {
                            Prefs.setAnimStyle(act, key, id)
                            changed = true
                            showTab(current, false)
                            onPlay(key, id)
                        }
                        row.addView(c, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(3f), dp(3f), dp(3f), dp(3f)) })
                    }
                    // keep the last row's cards the same width as the rest
                    for (pad in 0 until (3 - minOf(3, options.size - n))) {
                        row.addView(View(act), LinearLayout.LayoutParams(0, 1, 1f).apply { setMargins(dp(3f), 0, dp(3f), 0) })
                    }
                    grid.addView(row)
                    n += 3
                }
            }
            if (toTop) scroll.scrollTo(0, 0)
        }

        for ((i, tab) in FxStyles.TABS.withIndex()) {
            val tv = TextView(act)
            tv.text = tab.icon
            tv.textSize = 22f
            tv.gravity = Gravity.CENTER
            tv.setPadding(dp(18f), dp(8f), dp(18f), dp(8f))
            tv.setOnClickListener { showTab(i) }
            tabViews.add(tv)
            tabs.addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(dp(4f), dp(4f), dp(4f), dp(4f)) })
        }
        showTab(0)

        dlg.setContentView(root)
        dlg.window?.let { w ->
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setGravity(Gravity.BOTTOM)
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, (act.resources.displayMetrics.heightPixels * 0.52f).toInt())
            w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            // the live preview above stays visible and tappable
            w.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        }
        dlg.setOnDismissListener { if (changed) onDone() }
        dlg.show()
    }
}
