package com.example.notiftiles

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

// Look & colors: control panel background, the shadow (dim) behind it, and the widget background.
class SettingsActivity : Activity() {

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private lateinit var scroll: ScrollView
    private lateinit var col: LinearLayout
    private var scrimView: View? = null
    private var sheetView: View? = null
    private var widgetView: View? = null

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun c(v: Long) = v.toInt()

    private val bgColors = intArrayOf(
        0, c(0xFF000000), c(0xFF1C1C20), c(0xFFFFFFFF), c(0xFFF2F2F5), c(0xFF1E3A8A),
        c(0xFF0F766E), c(0xFF166534), c(0xFF7C3AED), c(0xFFB91C1C), c(0xFFEA580C), c(0xFFDB2777)
    )
    private val scrimColors = intArrayOf(
        c(0xFF000000), c(0xFF1C1C20), c(0xFF1E3A8A), c(0xFF7C3AED), c(0xFF0F766E), c(0xFF3E2723)
    )

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

    private fun rebuild() {
        val y = scroll.scrollY
        col.removeAllViews()
        build()
        scroll.post { scroll.scrollTo(0, y) }
    }

    private fun applyLook() {
        updatePreviews()
        TilesWidget.refresh(applicationContext)
    }

    private fun title(t: String, size: Float, topPad: Int) = TextView(this).apply {
        text = t
        textSize = size
        setTypeface(null, Typeface.BOLD)
        setPadding(0, dp(topPad), 0, dp(4))
    }

    private fun build() {
        col.addView(title("Look & colors", 22f, 0))

        // ---- Control panel background
        col.addView(title("Control panel background", 16f, 16))
        col.addView(panelPreview())
        col.addView(swatchRow(bgColors, Prefs.panelBg(this)) { Prefs.setPanelBg(this, it); applyLook(); rebuild() })
        col.addView(customHex { Prefs.setPanelBg(this, it); applyLook(); rebuild() })

        // ---- Shadow behind the panel
        col.addView(title("Shadow behind the panel", 16f, 20))
        col.addView(swatchRow(scrimColors, Prefs.scrimColor(this)) { Prefs.setScrimColor(this, it); applyLook(); rebuild() })
        col.addView(seek("Strength", Prefs.scrimAlpha(this)) { Prefs.setScrimAlpha(this, it); updatePreviews() })

        // ---- Widget background
        col.addView(title("Home-screen widget background", 16f, 20))
        col.addView(widgetPreview())
        col.addView(swatchRow(bgColors, Prefs.widgetBg(this)) { Prefs.setWidgetBg(this, it); applyLook(); rebuild() })
        col.addView(customHex { Prefs.setWidgetBg(this, it); applyLook(); rebuild() })
        col.addView(seek("Opacity", Prefs.widgetAlpha(this)) { Prefs.setWidgetAlpha(this, it); updatePreviews() })

        val reset = Button(this)
        reset.text = "Reset everything to automatic"
        reset.isAllCaps = false
        reset.setOnClickListener {
            Prefs.resetLook(this)
            applyLook()
            rebuild()
        }
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = dp(24)
        col.addView(reset, lp)

        updatePreviews()
    }

    private fun panelPreview(): View {
        val frame = FrameLayout(this)
        frame.setBackgroundColor(0xFF8A8F98.toInt())
        val scrim = View(this)
        frame.addView(scrim, FrameLayout.LayoutParams(MATCH, MATCH))
        val sheet = View(this)
        val slp = FrameLayout.LayoutParams(MATCH, dp(56), Gravity.BOTTOM)
        slp.setMargins(dp(10), 0, dp(10), dp(10))
        frame.addView(sheet, slp)
        scrimView = scrim
        sheetView = sheet
        frame.layoutParams = LinearLayout.LayoutParams(MATCH, dp(110))
        return frame
    }

    private fun widgetPreview(): View {
        val frame = FrameLayout(this)
        frame.setBackgroundColor(0xFF8A8F98.toInt())
        val w = View(this)
        val wlp = FrameLayout.LayoutParams(MATCH, dp(56))
        wlp.setMargins(dp(10), dp(10), dp(10), dp(10))
        frame.addView(w, wlp)
        widgetView = w
        frame.layoutParams = LinearLayout.LayoutParams(MATCH, dp(76))
        return frame
    }

    private fun rounded(color: Int, r: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(r).toFloat()
    }

    private fun updatePreviews() {
        scrimView?.setBackgroundColor(Prefs.scrimArgb(this))
        sheetView?.background = rounded(Prefs.panelBgColor(this), 16)
        widgetView?.background = rounded(Prefs.widgetBgArgb(this), 16)
    }

    private fun swatchRow(colors: IntArray, selected: Int, onPick: (Int) -> Unit): View {
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        hs.addView(row)
        for (color in colors) {
            val tv = TextView(this)
            tv.gravity = Gravity.CENTER
            tv.textSize = 11f
            val d = GradientDrawable()
            d.shape = GradientDrawable.OVAL
            if (color == 0) {
                d.setColor(0xFF9E9E9E.toInt())
                tv.text = "Auto"
                tv.setTextColor(0xFFFFFFFF.toInt())
            } else {
                d.setColor(color)
            }
            val on = color == selected
            d.setStroke(if (on) dp(3) else dp(1), if (on) 0xFF3B82F6.toInt() else 0x55888888)
            tv.background = d
            tv.setOnClickListener { onPick(color) }
            val lp = LinearLayout.LayoutParams(dp(44), dp(44))
            lp.setMargins(0, dp(8), dp(8), dp(8))
            row.addView(tv, lp)
        }
        return hs
    }

    private fun customHex(onColor: (Int) -> Unit): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val et = EditText(this)
        et.hint = "Custom colour, e.g. #1E90FF"
        et.setSingleLine(true)
        row.addView(et, LinearLayout.LayoutParams(0, WRAP, 1f))
        val b = Button(this)
        b.text = "Apply"
        b.isAllCaps = false
        b.setOnClickListener {
            try {
                val v = Color.parseColor(et.text.toString().trim())
                onColor(v or 0xFF000000.toInt())
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, "Use a colour like #1E90FF", Toast.LENGTH_SHORT).show()
            }
        }
        row.addView(b)
        return row
    }

    private fun seek(label: String, value: Int, onChange: (Int) -> Unit): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val t = TextView(this)
        t.text = "$label: $value%"
        t.setPadding(0, dp(8), 0, 0)
        box.addView(t)
        val sb = SeekBar(this)
        sb.max = 100
        sb.progress = value
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                t.text = "$label: $p%"
                if (fromUser) onChange(p)
            }

            override fun onStartTrackingTouch(s: SeekBar) {}

            override fun onStopTrackingTouch(s: SeekBar) {
                TilesWidget.refresh(applicationContext)
            }
        })
        box.addView(sb)
        return box
    }
}
