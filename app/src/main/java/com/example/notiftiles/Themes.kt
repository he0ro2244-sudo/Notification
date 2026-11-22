package com.example.notiftiles

import android.content.Context

// The built-in themes (colours, icons, font, shape, circles, borders) and the automatic day / night switch.
object Themes {
    private fun c(v: Long) = v.toInt()

    class Theme(
        val name: String, val panel: Int, val scrim: Int, val scrimA: Int,
        val widget: Int, val widgetA: Int, val accent: Int,
        // extras: icon colour, font, shape (index in Look.SHAPES), circle colour, border thickness + colour
        val icon: Int = 0, val font: String = "sans-serif", val shape: Int = 0,
        val circle: Int = 0, val bw: Int = 0, val bc: Int = 0
    )

    val LIST = listOf(
        Theme("Auto", 0, c(0xFF000000), 40, 0, 93, c(0xFF3B82F6)),
        Theme("Midnight", c(0xFF0B1020), c(0xFF000000), 55, c(0xFF0B1020), 90, c(0xFF60A5FA),
            icon = c(0xFF60A5FA), font = "sans-serif-medium", shape = 1, circle = c(0xFF16203A), bw = 1, bc = c(0xFF60A5FA)),
        Theme("Ocean", c(0xFF0F3D4C), c(0xFF04202A), 50, c(0xFF0F3D4C), 90, c(0xFF22D3EE),
            icon = c(0xFF22D3EE), font = "sans-serif-light", shape = 6, circle = c(0xFF14566A), bw = 1, bc = c(0xFF22D3EE)),
        Theme("Forest", c(0xFF14301F), c(0xFF05140A), 50, c(0xFF14301F), 90, c(0xFF4ADE80),
            icon = c(0xFF4ADE80), font = "serif", shape = 4, circle = c(0xFF1E4A2E), bw = 1, bc = c(0xFF4ADE80)),
        Theme("Sunset", c(0xFF3B1D2A), c(0xFF1A0A12), 50, c(0xFF3B1D2A), 90, c(0xFFFB923C),
            icon = c(0xFFFB923C), font = "casual", shape = 3, circle = c(0xFF5A2D40)),
        Theme("Rose", c(0xFFFCE7F3), c(0xFF000000), 30, c(0xFFFCE7F3), 92, c(0xFFDB2777),
            icon = c(0xFFDB2777), font = "sans-serif", shape = 0, circle = c(0xFFF9C9E1), bw = 1, bc = c(0xFFDB2777)),
        Theme("Light", c(0xFFFFFFFF), c(0xFF000000), 30, c(0xFFFFFFFF), 92, c(0xFF3B82F6),
            icon = c(0xFF3B82F6), font = "sans-serif", shape = 1, circle = c(0xFFEEF2F7), bw = 1, bc = c(0xFFD0D5DD)),
        Theme("AMOLED", c(0xFF000000), c(0xFF000000), 60, c(0xFF000000), 100, c(0xFFA78BFA),
            icon = c(0xFFA78BFA), font = "sans-serif-condensed", shape = 7, circle = c(0xFF121212), bw = 1, bc = c(0xFFA78BFA))
    )

    fun apply(c: Context, t: Theme) {
        Prefs.setPanelBg(c, t.panel)
        Prefs.setScrimColor(c, t.scrim)
        Prefs.setScrimAlpha(c, t.scrimA)
        Prefs.setWidgetBg(c, t.widget)
        Prefs.setWidgetAlpha(c, t.widgetA)
        Prefs.setAccent(c, t.accent)
        // The theme also sets icons, font, shape, circles and borders
        Prefs.setIconColor(c, t.icon)
        Prefs.setFontFamily(c, t.font)
        Prefs.setShape(c, t.shape)
        Prefs.setCircleColor(c, t.circle)
        Prefs.setDialCircle(c, t.circle)
        Prefs.setBorderWidth(c, t.bw)
        Prefs.setBorderColor(c, t.bc)
        // own colours of the icon parts would hide the theme icon colour, so they go back to automatic
        for (k in listOf("mic", "clear", "gear", "refresh")) Prefs.setPartColor(c, k, 0)
        for (g in listOf("clear", "gear")) Prefs.setGrpColor(c, g, 0)
    }

    /** Switches to the day or night theme when the time of day calls for it (only when the change is due). */
    fun applyAuto(c: Context) {
        if (!Prefs.autoTheme(c)) return
        val day = Extras.inWindow(Extras.minutesNow(), Prefs.autoDayStart(c), Prefs.autoNightStart(c))
        val phase = if (day) 1 else 0
        if (Prefs.autoLast(c) == phase) return
        val idx = (if (day) Prefs.autoDayTheme(c) else Prefs.autoNightTheme(c)).coerceIn(0, LIST.size - 1)
        apply(c, LIST[idx])
        Prefs.setAutoLast(c, phase)
    }
}
