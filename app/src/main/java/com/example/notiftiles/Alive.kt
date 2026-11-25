package com.example.notiftiles

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.PowerManager

/**
 * Alive icons: the widget icons move when something happens that they stand for, and are still the rest of the time.
 *  - gear spins when you come back from Settings, refresh turns while the widget refreshes, note shows a writing hand
 *    while you type, mic pulses (red) while recording, broom sweeps when you clear the inbox, mail flag goes up on a
 *    new message, pencil wiggles when its menu opens.
 * Widget icons cannot run a real animation, so for a second or two they are redrawn about 7 times a second (only the
 * icons that are moving); then one last redraw puts them back.
 * The panel's animations (styles, speed, on or off) are in Settings > Animation and played by PanelFx.
 */
object Alive {
    private val main = Handler(Looper.getMainLooper())
    private const val STEP = 140L

    /** A running widget event: [f] = frames since it started, [p] = how far it is (0 to 1) when it has a set length. */
    class Ev(val f: Int, val p: Float)

    // ---------------------------------------------------------------- poses
    private val FRAMES: Map<String, List<String>> = mapOf(
        "📝" to listOf("📝", "✍️", "📝", "🖊️"), "✍️" to listOf("✍️", "📝", "✍️", "🖋️"),
        "🗒️" to listOf("🗒️", "📝", "🗒️", "✍️"), "📓" to listOf("📓", "📔", "📒", "📔"),
        "📋" to listOf("📋", "✅", "📋", "📝"), "✏️" to listOf("✏️", "🖊️", "✏️", "✍️"),
        "🖊️" to listOf("🖊️", "✏️", "🖊️", "✍️"), "✎" to listOf("✎", "✐", "✎", "✏"),
        "📬" to listOf("📬", "📫"), "📫" to listOf("📫", "📬"), "📥" to listOf("📥", "📬", "📥", "📨"),
        "✉️" to listOf("✉️", "📨", "📩", "📨"), "📨" to listOf("📨", "📩"), "📩" to listOf("📩", "📨"),
        "💌" to listOf("💌", "💖", "💌", "💗"), "💬" to listOf("💬", "🗨️"), "🗨️" to listOf("🗨️", "💬"),
        "📱" to listOf("📱", "📳"), "🔔" to listOf("🔔", "🛎️", "🔔", "🛎️"),
        "🧹" to listOf("🧹", "🧼", "🧹", "✨"), "🧼" to listOf("🧼", "🫧", "🧼", "✨"),
        "🗑️" to listOf("🗑️", "🚮"), "🪄" to listOf("🪄", "✨", "🪄", "⭐"), "🧽" to listOf("🧽", "🫧"),
        "🎙️" to listOf("🎙️", "🎤", "🎙️", "🔴"), "🎤" to listOf("🎤", "🎙️"), "🔴" to listOf("🔴", "⭕"),
        "⚙️" to listOf("⚙️", "🔧", "⚙️", "🛠️"), "🔧" to listOf("🔧", "🔩"),
        "🔄" to listOf("🔄", "🔃", "🔄", "🔁"), "🔃" to listOf("🔃", "🔄"), "🔁" to listOf("🔁", "🔂"),
        "⭐" to listOf("⭐", "🌟", "✨", "🌟"), "🌟" to listOf("🌟", "✨", "⭐", "✨"),
        "❤️" to listOf("❤️", "💖", "❤️", "💗"), "🔥" to listOf("🔥", "💥", "🔥", "✨"),
        "✨" to listOf("✨", "💫", "⭐", "💫")
    )
    fun has(e: String): Boolean = e.isNotEmpty() && (FRAMES[e]?.size ?: 0) > 1
    fun sw(e: String, f: Int): String = FRAMES[e]?.let { it[f % it.size] } ?: e

    private val NOTE_ICONS = setOf("", "📝", "🗒️", "📓", "📔", "📒", "📋", "✍️", "✏️", "🖊️", "🖋️", "📃", "📄")
    private val CLEAN_ICONS = setOf("", "🧹", "🧼", "🧽", "🪄", "🗑️", "🚮", "✨")
    private val MAIL_ICONS = setOf("", "📬", "📫", "📭", "📪")

    /** The writing hand: used for the Note icon while a note is typed. */
    fun writePose(base: String, f: Int): String =
        if (base in NOTE_ICONS) listOf("✍️", "📝", "✍️", "🖊️", "✍️")[(f / 2) % 5] else sw(base, f / 2)

    /** The broom sweeping: used for Clear all while the inbox is cleared. */
    fun sweepPose(base: String, f: Int): String =
        if (base in CLEAN_ICONS) listOf("🧹", "🧹", "🧼", "🫧", "✨")[(f / 2) % 5] else sw(base, f / 2)

    /** Mail arriving: the flag goes up. */
    fun mailPose(base: String, f: Int): String =
        if (base in MAIL_ICONS) listOf("📪", "📫", "📬")[(f / 3).coerceAtMost(2)] else sw(base, f / 2)

    /** A bitmap turned by [deg] and scaled by [scale] around its centre (same size). */
    fun xform(b: Bitmap, deg: Float, scale: Float = 1f): Bitmap {
        if (deg == 0f && scale == 1f) return b
        val out = Bitmap.createBitmap(b.width, b.height, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.rotate(deg, b.width / 2f, b.height / 2f)
        c.scale(scale, scale, b.width / 2f, b.height / 2f)
        c.drawBitmap(b, 0f, 0f, null)
        return out
    }

    /** A built-in icon (gear, refresh, mic) as a coloured bitmap, flat or 3D like the widget draws it now. */
    fun glyph(ctx: Context, res: Int, color: Int, dp: Float): Bitmap? {
        if (Prefs.icons3d(ctx)) return Look.icon3d(ctx, res, color, dp)
        return try {
            val d = ctx.getDrawable(res)?.mutate() ?: return null
            val px = (dp * ctx.resources.displayMetrics.density).toInt().coerceIn(32, 192)
            val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            val pad = (px * 0.08f).toInt()
            d.setBounds(pad, pad, px - pad, px - pad)
            d.setTint(color or 0xFF000000.toInt())
            d.draw(Canvas(b))
            b
        } catch (e: Exception) { null }
    }

    // ---------------------------------------------------------------- widget events
    private var appCtx: Context? = null
    private var looping = false
    private val startAt = HashMap<String, Long>()
    private val endAt = HashMap<String, Long>()
    private var seen = -1

    private fun enabled(c: Context) = Prefs.aliveOn(c) && Prefs.aliveWave(c)

    /** Something happened that [kind] stands for: gear, refresh, note, clear, mic, edit or inbox. It moves for [ms]. */
    fun go(ctx: Context, kind: String, ms: Long) {
        val c = ctx.applicationContext
        if (!enabled(c)) return
        appCtx = c
        val now = System.currentTimeMillis()
        if ((endAt[kind] ?: 0L) < now) startAt[kind] = now
        endAt[kind] = now + ms
        if (!looping) { looping = true; main.post(tick) }
    }

    /** The thing [kind] stands for is over (it keeps moving for at least [minMs] so it is seen). */
    fun end(ctx: Context, kind: String, minMs: Long = 1200) {
        val s = startAt[kind] ?: return
        endAt[kind] = maxOf(System.currentTimeMillis() + 150, s + minMs)
    }

    private val tick = object : Runnable {
        override fun run() {
            val c = appCtx
            if (c == null) { looping = false; return }
            val now = System.currentTimeMillis()
            if (RecordService.running()) go(c, "mic", 700)
            val act = HashMap<String, Ev>()
            val rest = HashSet<String>()
            for ((k, e) in endAt.toMap()) {
                val s = startAt[k] ?: now
                if (now < e) act[k] = Ev(((now - s) / STEP).toInt(), ((now - s).toFloat() / (e - s).coerceAtLeast(1)).coerceIn(0f, 1f))
                else { rest.add(k); endAt.remove(k); startAt.remove(k) }
            }
            if (act.isEmpty() && rest.isEmpty()) { looping = false; return }
            val pm = c.getSystemService(Context.POWER_SERVICE) as PowerManager
            val mgr = AppWidgetManager.getInstance(c)
            val ids = mgr.getAppWidgetIds(ComponentName(c, TilesWidget::class.java))
            if (ids.isEmpty()) { endAt.clear(); startAt.clear(); looping = false; return }
            if (pm.isInteractive) try { mgr.partiallyUpdateAppWidget(ids, TilesWidget.aliveFrame(c, act, rest)) } catch (e: Exception) { }
            main.postDelayed(this, STEP)
        }
    }

    /** Called when the notifications changed: a new message makes the mail box flag go up. */
    fun onMessages(ctx: Context) {
        val n = try { Feed.items(ctx).size } catch (e: Exception) { return }
        val more = seen in 0 until n
        seen = n
        if (more) go(ctx, "inbox", 1500)
    }

    // ---------------------------------------------------------------- Animation settings (one section in Settings)
    /** One part of the panel that can animate. [styles] are the choices; the first one is the default. */
    class Elem(val key: String, val label: String, val styles: List<Pair<String, String>>)

    val ELEMENTS: List<Elem> = listOf(
        Elem("note", "Note (the pen writes as you type)",
            listOf("ink" to "Pen writes", "ripple" to "Stamp only")),
        Elem("clear", "Clear all",
            listOf("bin" to "Messages drop into the bin", "sweep" to "Band sweeps the list")),
        Elem("gear", "Gear (before Settings opens)",
            listOf("spin" to "One smooth turn", "turn" to "Turn and settle")),
        Elem("refresh", "Refresh",
            listOf("arc" to "Arrow draws a loop", "spin" to "Smooth turn")),
        Elem("mic", "Mic (before the recorder opens)",
            listOf("bars" to "Live sound bars", "rings" to "Rings and pulse")),
        Elem("edit", "Pencil (before the edit menu opens)",
            listOf("trail" to "Pencil draws a trail", "tilt" to "Tilt and dots")),
        Elem("title", "Inbox title (when a message arrives)",
            listOf("envelope" to "Envelope drops in", "bounce" to "Bounce")),
        Elem("badge", "Count badge (when a message arrives)",
            listOf("roll" to "Number rolls up", "pop" to "Pop")),
        Elem("open", "Panel opens",
            listOf("rise" to "Sheet rises", "fade" to "Fade in")),
        Elem("row", "New message tile (when it arrives)",
            listOf("slide" to "Slide in with highlight", "fade" to "Fade in")),
        Elem("press", "Touch feedback (tiles and buttons)",
            listOf("squish" to "Squish and bounce back", "dim" to "Soft dim")),
        Elem("list", "Tiles appear (panel opens, page or message changes)",
            listOf("cascade" to "Rise one by one", "fade" to "Fade in")),
        Elem("close", "Panel closes (swipe down or tap outside)",
            listOf("drop" to "Sheet follows your finger and drops", "fade" to "Fade out")),
    )

    /** The style picked for [key] in Settings, or the default one when nothing was picked. */
    fun chosen(c: Context, key: String): String {
        val e = ELEMENTS.firstOrNull { it.key == key } ?: return "off"
        val s = Prefs.animStyle(c, key)
        return if (s == "off" || e.styles.any { it.first == s }) s else e.styles[0].first
    }

    /** The style the panel plays for [key]: "off" when Animation or Panel animations are switched off. */
    fun style(c: Context, key: String): String =
        if (!Prefs.aliveOn(c) || !Prefs.alivePanel(c)) "off" else chosen(c, key)

    /** How long a panel animation takes: Slow is longer, Fast is shorter, Normal is as designed. */
    fun t(c: Context, ms: Long): Long {
        val f = when (Prefs.animSpeed(c)) { 0 -> 1.6f; 2 -> 0.6f; else -> 1f }
        return (ms * f).toLong().coerceAtLeast(1L)
    }
}
