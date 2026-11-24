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
        val circle: Int = 0, val bw: Int = 0, val bc: Int = 0,
        // optional emoji icons: note, mic, reset, clear all, pencil (null = leave the emoji as they are)
        val emoji: List<String>? = null
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
            icon = c(0xFFA78BFA), font = "sans-serif-condensed", shape = 7, circle = c(0xFF121212), bw = 1, bc = c(0xFFA78BFA)),
        Theme("Lavender", c(0xFFEDE9FE), c(0xFF000000), 30, c(0xFFEDE9FE), 92, c(0xFF7C3AED),
            icon = c(0xFF7C3AED), font = "sans-serif", shape = 1, circle = c(0xFFDDD6FE), bw = 1, bc = c(0xFF7C3AED)),
        Theme("Mint", c(0xFFD1FAE5), c(0xFF000000), 30, c(0xFFD1FAE5), 92, c(0xFF059669),
            icon = c(0xFF059669), font = "sans-serif-light", shape = 4, circle = c(0xFFA7F3D0), bw = 1, bc = c(0xFF059669)),
        Theme("Coffee", c(0xFF2B1D16), c(0xFF120B08), 55, c(0xFF2B1D16), 92, c(0xFFD6A77A),
            icon = c(0xFFD6A77A), font = "serif", shape = 1, circle = c(0xFF40291E), bw = 1, bc = c(0xFFD6A77A)),
        Theme("Neon", c(0xFF05010F), c(0xFF000000), 60, c(0xFF05010F), 96, c(0xFFFF2BD6),
            icon = c(0xFF00F5D4), font = "sans-serif-black", shape = 7, circle = c(0xFF1A0B2E), bw = 2, bc = c(0xFF00F5D4)),
        Theme("Candy", c(0xFFFFF1F2), c(0xFF000000), 25, c(0xFFFFF1F2), 94, c(0xFFFF5C8A),
            icon = c(0xFFFF5C8A), font = "casual", shape = 3, circle = c(0xFFFFD6E0), bw = 2, bc = c(0xFFFF5C8A),
            emoji = listOf("🍬", "🎤", "✨", "🧁", "🖍️")),
        Theme("Slate", c(0xFF1E293B), c(0xFF0B1220), 50, c(0xFF1E293B), 92, c(0xFF94A3B8),
            icon = c(0xFF94A3B8), font = "sans-serif-condensed", shape = 2, circle = c(0xFF334155), bw = 1, bc = c(0xFF94A3B8)),
        Theme("Crimson", c(0xFF1A0507), c(0xFF000000), 55, c(0xFF1A0507), 94, c(0xFFEF4444),
            icon = c(0xFFEF4444), font = "sans-serif-medium", shape = 5, circle = c(0xFF3B0D12), bw = 1, bc = c(0xFFEF4444)),
        Theme("Gold", c(0xFF14110A), c(0xFF000000), 55, c(0xFF14110A), 95, c(0xFFFFD700),
            icon = c(0xFFFFD700), font = "serif", shape = 7, circle = c(0xFF2A2412), bw = 2, bc = c(0xFFFFD700),
            emoji = listOf("⭐", "🎙️", "💫", "🧹", "🪄")),
        Theme("Sky", c(0xFFE0F2FE), c(0xFF000000), 30, c(0xFFE0F2FE), 92, c(0xFF0284C7),
            icon = c(0xFF0284C7), font = "sans-serif", shape = 0, circle = c(0xFFBAE6FD), bw = 1, bc = c(0xFF0284C7)),
        Theme("Sand", c(0xFFF5EBDD), c(0xFF000000), 30, c(0xFFF5EBDD), 93, c(0xFFB45309),
            icon = c(0xFFB45309), font = "serif", shape = 1, circle = c(0xFFE8D5B7), bw = 1, bc = c(0xFFB45309)),
        Theme("Cyber", c(0xFF0A0F1F), c(0xFF000000), 60, c(0xFF0A0F1F), 95, c(0xFF22D3EE),
            icon = c(0xFFA3E635), font = "monospace", shape = 6, circle = c(0xFF111B33), bw = 1, bc = c(0xFFA3E635)),
        Theme("Aurora", c(0xFF0B1B2B), c(0xFF04101B), 55, c(0xFF0B1B2B), 92, c(0xFF4ADE80),
            icon = c(0xFFA78BFA), font = "sans-serif-light", shape = 4, circle = c(0xFF12304A), bw = 1, bc = c(0xFFA78BFA)),
        Theme("Cherry", c(0xFF2A0A18), c(0xFF14040C), 55, c(0xFF2A0A18), 92, c(0xFFF472B6),
            icon = c(0xFFF472B6), font = "cursive", shape = 3, circle = c(0xFF4A1230), bw = 1, bc = c(0xFFF472B6),
            emoji = listOf("📝", "🎤", "🔄", "🗑️", "✏️")),
        Theme("Graphite", c(0xFF18181B), c(0xFF000000), 55, c(0xFF18181B), 93, c(0xFFFB923C),
            icon = c(0xFFFB923C), font = "sans-serif-medium", shape = 1, circle = c(0xFF27272A), bw = 1, bc = c(0xFFFB923C))
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
        Prefs.clearBorderParts(c)
        // themes that bring their own emoji set the note, mic, reset, clear all and pencil icons
        t.emoji?.let { e ->
            val keys = listOf("note", "mic", "reset", "clear", "edit")
            for ((i, k) in keys.withIndex()) Prefs.setEmojiOf(c, k, e.getOrElse(i) { "" })
        }
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
