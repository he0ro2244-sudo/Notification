package com.example.notiftiles

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import java.util.WeakHashMap

/**
 * The look of the Settings screen itself (not the panel or the widget): light / dark / AMOLED black / match the panel theme,
 * your own colours (background, text, buttons and switches, section bars), text size, font and corner roundness.
 * "Auto" (0) for a colour means "as the phone's theme draws it". It is kept apart from the panel themes, and Reset
 * (which clears the widget look) leaves it alone.
 */
object SettingsTheme {
    const val MODE_PHONE = 0
    const val MODE_LIGHT = 1
    const val MODE_DARK = 2
    const val MODE_AMOLED = 3
    const val MODE_PANEL = 4

    private fun sp(c: Context) = c.getSharedPreferences("nt", Context.MODE_PRIVATE)

    fun mode(c: Context): Int = sp(c).getInt("ui_s_mode", MODE_PHONE).coerceIn(0, 4)
    fun setMode(c: Context, v: Int) = sp(c).edit().putInt("ui_s_mode", v).apply()
    fun bgOwn(c: Context): Int = sp(c).getInt("ui_s_bg", 0)
    fun setBg(c: Context, v: Int) = sp(c).edit().putInt("ui_s_bg", v).apply()
    fun textOwn(c: Context): Int = sp(c).getInt("ui_s_text", 0)
    fun setText(c: Context, v: Int) = sp(c).edit().putInt("ui_s_text", v).apply()
    fun accentOwn(c: Context): Int = sp(c).getInt("ui_s_accent", 0)
    fun setAccent(c: Context, v: Int) = sp(c).edit().putInt("ui_s_accent", v).apply()
    fun cardOwn(c: Context): Int = sp(c).getInt("ui_s_card", 0)
    fun setCard(c: Context, v: Int) = sp(c).edit().putInt("ui_s_card", v).apply()
    /** Text size in percent (100 = as designed). */
    fun size(c: Context): Int = sp(c).getInt("ui_s_size", 100).coerceIn(70, 150)
    fun setSize(c: Context, v: Int) = sp(c).edit().putInt("ui_s_size", v).apply()
    /** Corner roundness of the section bars and the search box in dp (-1 = as designed). */
    fun radius(c: Context): Int = sp(c).getInt("ui_s_radius", -1)
    fun setRadius(c: Context, v: Int) = sp(c).edit().putInt("ui_s_radius", v).apply()
    /** Font family ("" = the phone's own). */
    fun font(c: Context): String = sp(c).getString("ui_s_font", "") ?: ""
    fun setFont(c: Context, v: String) = sp(c).edit().putString("ui_s_font", v).apply()

    fun reset(c: Context) = sp(c).edit().remove("ui_s_mode").remove("ui_s_bg").remove("ui_s_text").remove("ui_s_accent")
        .remove("ui_s_card").remove("ui_s_size").remove("ui_s_radius").remove("ui_s_font").apply()

    // ---------------------------------------------------------------- what is in force now
    /** Night (true) or day (false) look forced on the screen; null = follow the phone. */
    fun night(c: Context): Boolean? = when (mode(c)) {
        MODE_LIGHT -> false
        MODE_DARK, MODE_AMOLED -> true
        MODE_PANEL -> Prefs.panelBg(c).let { if (it == 0) null else Color.luminance(it or 0xFF000000.toInt()) < 0.5f }
        else -> null
    }

    /** Background colour (opaque), or 0 for the phone's own. */
    fun bg(c: Context): Int {
        val own = bgOwn(c)
        if (own != 0) return own or 0xFF000000.toInt()
        return when (mode(c)) {
            MODE_AMOLED -> 0xFF000000.toInt()
            MODE_PANEL -> Prefs.panelBg(c).let { if (it == 0) 0 else it or 0xFF000000.toInt() }
            else -> 0
        }
    }

    fun text(c: Context): Int {
        val own = textOwn(c)
        if (own != 0) return own or 0xFF000000.toInt()
        val b = bg(c)
        // a background of your own (or the panel's) decides whether the text is light or dark
        return if (b != 0 && (bgOwn(c) != 0 || mode(c) == MODE_PANEL)) Look.contrast(b) else 0
    }

    fun accent(c: Context): Int {
        val own = accentOwn(c)
        if (own != 0) return own or 0xFF000000.toInt()
        return if (mode(c) == MODE_PANEL) Prefs.accent(c) or 0xFF000000.toInt() else 0
    }

    private fun cardColor(c: Context): Int {
        val own = cardOwn(c)
        if (own != 0) return own
        val b = bg(c)
        // on a coloured background the bars are a soft tint of the text colour
        val t = text(c)
        return if (b != 0 && t != 0) (0x26 shl 24) or (t and 0xFFFFFF) else 0x1F808080
    }

    /** The rounded bar behind a section name (and the search box). */
    fun card(c: Context, dpR: Int): GradientDrawable = GradientDrawable().apply {
        setColor(cardColor(c))
        val r = radius(c)
        cornerRadius = (if (r >= 0) r else dpR) * c.resources.displayMetrics.density
    }

    /** True when any part of this look differs from the plain phone theme. */
    fun custom(c: Context): Boolean = mode(c) != MODE_PHONE || bgOwn(c) != 0 || textOwn(c) != 0 || accentOwn(c) != 0 ||
        cardOwn(c) != 0 || size(c) != 100 || font(c).isNotEmpty() || radius(c) >= 0

    /** Gives [base] the forced day / night look (used in the screen's attachBaseContext, so dialogs follow too). */
    fun wrap(base: Context): Context {
        val n = night(base) ?: return base
        val cfg = Configuration(base.resources.configuration)
        cfg.uiMode = (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            (if (n) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        return base.createConfigurationContext(cfg)
    }

    // ---------------------------------------------------------------- painting the screen
    private val baseSize = WeakHashMap<TextView, Float>()   // the size a text had before it was scaled, so scaling never compounds

    /** Colours the window and the whole screen [root]. Call it after every (re)build. */
    fun applyWindow(act: Activity, screen: View) {
        val b = bg(act)
        if (b != 0) {
            act.window.setBackgroundDrawable(ColorDrawable(b))
            screen.setBackgroundColor(b)
        } else {
            screen.background = null
            // the theme's own window background comes back after a recreate; nothing to do here
        }
    }

    /** Applies colours, tints, size and font to every view under [root]. */
    fun applyTree(c: Context, root: View) {
        if (!custom(c)) return
        val text = text(c)
        val accent = accent(c)
        val scale = size(c) / 100f
        val fam = font(c)
        val tint = if (accent != 0) ColorStateList.valueOf(accent) else null
        fun walk(v: View) {
            when (v) {
                is Button -> {
                    if (tint != null) { v.backgroundTintList = tint; v.setTextColor(Look.contrast(accent)) }
                    else if (text != 0) v.setTextColor(text)
                    style(v, scale, fam)
                }
                is EditText -> { if (text != 0) v.setTextColor(text); style(v, scale, fam) }
                is CompoundButton -> {
                    if (text != 0) v.setTextColor(text)
                    if (tint != null && v is Switch) {
                        v.thumbTintList = tint
                        v.trackTintList = ColorStateList.valueOf((accent and 0xFFFFFF) or (0x80 shl 24))
                    }
                    style(v, scale, fam)
                }
                is TextView -> { if (text != 0) v.setTextColor(text); style(v, scale, fam) }
                is SeekBar -> if (tint != null) { v.progressTintList = tint; v.thumbTintList = tint }
            }
            if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(root)
    }

    private fun style(tv: TextView, scale: Float, fam: String) {
        if (scale != 1f) {
            val base = baseSize.getOrPut(tv) { tv.textSize }
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, base * scale)
        }
        if (fam.isNotEmpty()) {
            val st = tv.typeface?.style ?: Typeface.NORMAL
            tv.typeface = Typeface.create(fam, st)
        }
    }
}
