package com.example.notiftiles

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator

/**
 * Alive icons: an icon moves when something happens that it stands for, and is still the rest of the time.
 *  - gear spins when you come back from Settings (and when tapped in the panel)
 *  - refresh turns while the widget refreshes
 *  - note shows a writing hand while you type a note
 *  - mic pulses (red) while recording
 *  - broom sweeps when you clear the inbox
 *  - mail box: the flag goes up when a new message arrives
 *  - pencil wiggles when its menu opens
 * Widget icons cannot run a real animation, so for a second or two they are redrawn about 7 times a second (only the
 * icons that are moving); then one last redraw puts them back. Panel icons use real animations.
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

    // ---------------------------------------------------------------- panel: real animations on tap
    private fun anim(v: View, prop: String, vararg values: Float, ms: Long, linear: Boolean = false, after: (() -> Unit)? = null) {
        val an = ObjectAnimator.ofFloat(v, prop, *values)
        an.duration = ms
        an.interpolator = if (linear) LinearInterpolator() else DecelerateInterpolator()
        if (after != null) an.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(a: Animator) { after() }
        })
        an.start()
    }

    private fun on(c: Context) = Prefs.aliveOn(c) && Prefs.alivePanel(c)

    /** A tap on a panel icon: the icon does its move, then [action] runs (at once when Alive icons are off). */
    fun tap(ctx: Context, v: View, kind: String, action: () -> Unit) {
        if (!on(ctx)) { action(); return }
        v.animate().cancel()
        val done = { v.rotation = 0f; v.scaleX = 1f; v.scaleY = 1f; action() }
        when (kind) {
            "gear" -> anim(v, "rotation", 0f, 540f, ms = 520) { done() }
            "refresh" -> anim(v, "rotation", 0f, 360f, ms = 450) { done() }
            "clear" -> { v.post { v.pivotX = v.width / 2f; v.pivotY = v.height.toFloat() }
                anim(v, "rotation", 0f, -20f, 20f, -20f, 20f, 0f, ms = 520, linear = true) { done() } }
            "edit" -> anim(v, "rotation", 0f, -25f, 25f, -15f, 0f, ms = 420, linear = true) { done() }
            "mic" -> { anim(v, "scaleX", 1f, 1.35f, 1f, ms = 360) { done() }; anim(v, "scaleY", 1f, 1.35f, 1f, ms = 360) }
            else -> action()
        }
    }

    /** A little wiggle (e.g. the note icon while a note is typed, a badge when a message arrives). */
    fun wiggle(ctx: Context, v: View) {
        if (!on(ctx) || v.rotation != 0f) return
        anim(v, "rotation", 0f, -12f, 12f, -8f, 0f, ms = 300, linear = true)
    }

    /** A little pop (e.g. the count badge when a message arrives). */
    fun pop(ctx: Context, v: View) {
        if (!on(ctx)) return
        anim(v, "scaleX", 1f, 1.5f, 1f, ms = 380)
        anim(v, "scaleY", 1f, 1.5f, 1f, ms = 380)
    }

    /** The mail box on the Inbox title: the flag goes up when a message arrives. */
    fun bounce(ctx: Context, v: View) {
        if (!on(ctx)) return
        anim(v, "translationY", 0f, -10f * ctx.resources.displayMetrics.density, 0f, ms = 420)
    }
}
