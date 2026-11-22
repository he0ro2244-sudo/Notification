package com.example.notiftiles

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

// One colour picker for the whole app: a big palette, hue / saturation / brightness sliders, a hex box and Auto.
object ColorPicker {
    private fun c(v: Long) = v.toInt()

    val PALETTE: IntArray = intArrayOf(
        c(0xFF000000), c(0xFF1B1B1F), c(0xFF3C3C43), c(0xFF6B6B73), c(0xFF9E9E9E), c(0xFFD0D0D5), c(0xFFF2F2F5), c(0xFFFFFFFF),
        c(0xFFB91C1C), c(0xFFEF4444), c(0xFFFB7185), c(0xFFEA580C), c(0xFFFB923C), c(0xFFFACC15), c(0xFFFDE68A), c(0xFFA16207),
        c(0xFF166534), c(0xFF22C55E), c(0xFF4ADE80), c(0xFFA3E635), c(0xFF0F766E), c(0xFF14B8A6), c(0xFF22D3EE), c(0xFF0891B2),
        c(0xFF1E3A8A), c(0xFF3B82F6), c(0xFF60A5FA), c(0xFF93C5FD), c(0xFF6D28D9), c(0xFF7C3AED), c(0xFFA78BFA), c(0xFFC4B5FD),
        c(0xFFDB2777), c(0xFFF472B6), c(0xFFF9A8D4), c(0xFFFFE4E6), c(0xFF3E2723), c(0xFF8D6E63), c(0xFFD7CCC8), c(0xFFFFD700)
    )

    /**
     * Opens the picker. [current] 0 = Auto. [allowAuto] shows the Auto button (gives 0 back).
     * [onPick] is called with the chosen ARGB colour (or 0 for Auto).
     */
    fun show(act: Activity, label: String, current: Int, allowAuto: Boolean, onPick: (Int) -> Unit) {
        val density = act.resources.displayMetrics.density
        val hsv = FloatArray(3)
        val start = if (current != 0) current else c(0xFF3B82F6)
        Color.colorToHSV(start, hsv)
        var chosen = start or c(0xFF000000)

        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        val pad = (16 * density).toInt()
        box.setPadding(pad, pad / 2, pad, 0)

        val preview = TextView(act)
        preview.gravity = Gravity.CENTER
        preview.textSize = 16f
        preview.setTypeface(null, Typeface.BOLD)
        val hex = EditText(act)
        hex.hint = "#RRGGBB"
        hex.setSingleLine(true)

        val hue = SeekBar(act)
        val sat = SeekBar(act)
        val bri = SeekBar(act)
        hue.max = 360
        sat.max = 100
        bri.max = 100

        fun refreshPreview() {
            chosen = Color.HSVToColor(hsv)
            val g = GradientDrawable()
            g.cornerRadius = 14 * density
            g.setColor(chosen)
            g.setStroke((1 * density).toInt().coerceAtLeast(1), 0x55888888)
            preview.background = g
            preview.setTextColor(Look.contrast(chosen))
            val txt = String.format("#%06X", chosen and 0xFFFFFF)
            preview.text = txt
        }

        fun syncSliders() {
            hue.progress = hsv[0].toInt().coerceIn(0, 360)
            sat.progress = (hsv[1] * 100).toInt().coerceIn(0, 100)
            bri.progress = (hsv[2] * 100).toInt().coerceIn(0, 100)
        }

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                if (!fromUser) return
                hsv[0] = hue.progress.toFloat()
                hsv[1] = sat.progress / 100f
                hsv[2] = bri.progress / 100f
                refreshPreview()
            }

            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        }
        hue.setOnSeekBarChangeListener(listener)
        sat.setOnSeekBarChangeListener(listener)
        bri.setOnSeekBarChangeListener(listener)

        syncSliders()
        refreshPreview()
        box.addView(preview, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (56 * density).toInt()))

        fun small(t: String): TextView {
            val tv = TextView(act)
            tv.text = t
            tv.textSize = 12f
            tv.alpha = 0.7f
            tv.setPadding(0, (8 * density).toInt(), 0, 0)
            return tv
        }

        // palette: rows of 8
        box.addView(small("Colours"))
        var i = 0
        while (i < PALETTE.size) {
            val row = LinearLayout(act)
            row.orientation = LinearLayout.HORIZONTAL
            for (k in i until minOf(i + 8, PALETTE.size)) {
                val color = PALETTE[k]
                val dot = TextView(act)
                val d = GradientDrawable()
                d.shape = GradientDrawable.OVAL
                d.setColor(color)
                d.setStroke((1 * density).toInt().coerceAtLeast(1), 0x55888888)
                dot.background = d
                dot.setOnClickListener {
                    Color.colorToHSV(color, hsv)
                    syncSliders()
                    refreshPreview()
                }
                val lp = LinearLayout.LayoutParams(0, (36 * density).toInt(), 1f)
                lp.setMargins(2, 4, 2, 4)
                row.addView(dot, lp)
            }
            box.addView(row)
            i += 8
        }

        box.addView(small("Hue"))
        box.addView(hue)
        box.addView(small("Strength (saturation)"))
        box.addView(sat)
        box.addView(small("Light / dark"))
        box.addView(bri)

        box.addView(small("Or type a colour"))
        val hexRow = LinearLayout(act)
        hexRow.orientation = LinearLayout.HORIZONTAL
        hexRow.gravity = Gravity.CENTER_VERTICAL
        hexRow.addView(hex, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val use = Button(act)
        use.text = "Use"
        use.isAllCaps = false
        use.setOnClickListener {
            try {
                val raw = hex.text.toString().trim()
                val v = Color.parseColor(if (raw.startsWith("#")) raw else "#$raw")
                Color.colorToHSV(v, hsv)
                syncSliders()
                refreshPreview()
            } catch (e: IllegalArgumentException) {
                Toast.makeText(act, "Use a colour like #1E90FF", Toast.LENGTH_SHORT).show()
            }
        }
        hexRow.addView(use)
        box.addView(hexRow)

        val scroll = ScrollView(act)
        scroll.addView(box)

        val b = AlertDialog.Builder(act)
        b.setTitle(label)
        b.setView(scroll)
        b.setPositiveButton("Apply") { _, _ -> onPick(chosen or c(0xFF000000)) }
        b.setNegativeButton("Cancel", null)
        if (allowAuto) b.setNeutralButton("Auto") { _, _ -> onPick(0) }
        b.show()
    }
}
