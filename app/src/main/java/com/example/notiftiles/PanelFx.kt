package com.example.notiftiles

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.animation.ValueAnimator
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.ViewConfiguration
import android.widget.EditText
import android.widget.LinearLayout
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** One drawn animation on the panel overlay. [draw] gets the progress 0..1 for the current time. */
class FxAnim(var start: Long, var end: Long, val draw: (Canvas, Float) -> Unit, val onEnd: (() -> Unit)?)

/** The overlay over the whole panel. It draws the animations and lets every touch pass through to what is under it. */
class FxLayer(c: Context) : View(c) {
    private val anims = ArrayList<FxAnim>()
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            val now = SystemClock.uptimeMillis()
            val done = anims.filter { now >= it.end }
            anims.removeAll(done)
            invalidate()
            for (a in done) a.onEnd?.invoke()
            if (anims.isNotEmpty()) postOnAnimation(this) else running = false
        }
    }

    init { setWillNotDraw(false) }

    fun add(a: FxAnim) {
        anims.add(a)
        if (!running) { running = true; postOnAnimation(tick) }
        invalidate()
    }

    override fun onDraw(cv: Canvas) {
        val now = SystemClock.uptimeMillis()
        for (a in anims.toList()) {
            if (now < a.start) continue
            val p = ((now - a.start).toFloat() / (a.end - a.start).coerceAtLeast(1L)).coerceIn(0f, 1f)
            a.draw(cv, p)
        }
    }
}

/**
 * The panel's animations, drawn on an overlay over the panel. Each one is tied to what the item does
 * (a pen writes, messages drop into the bin, the envelope drops into the Inbox title, a sound bar pulses while
 * recording). The style of each part is chosen in Settings > Animation; "off" keeps it still.
 * The icon itself is copied into the overlay, so it keeps the emoji and colour you chose.
 */
object PanelFx {
    private val main = Handler(Looper.getMainLooper())
    private var layer: FxLayer? = null
    private val PI_F = Math.PI.toFloat()
    private val RED = Color.parseColor("#E53935")

    /** Adds the overlay on top of the panel. Call it once the panel's views are in place. */
    fun attach(c: Context, root: ViewGroup) {
        val l = FxLayer(c)
        root.addView(l, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        layer = l
    }

    private fun st(c: Context, key: String) = Alive.style(c, key)
    private fun t(c: Context, ms: Long) = Alive.t(c, ms)
    private fun dp(c: Context, v: Float) = v * c.resources.displayMetrics.density
    private fun gone(c: Context): Boolean = (c as? Activity)?.let { it.isFinishing || it.isDestroyed } ?: false

    /** Runs [r] after [ms] (speed applied), unless the panel has closed. */
    private fun later(c: Context, ms: Long, r: () -> Unit) {
        main.postDelayed({ if (!gone(c)) r() }, t(c, ms))
    }

    /** Starts a drawn animation on the overlay after [delay] ms. [end] runs when it is over. */
    private fun anim(c: Context, L: FxLayer, ms: Long, delay: Long = 0L, draw: (Canvas, Float) -> Unit, end: (() -> Unit)? = null) {
        val s = SystemClock.uptimeMillis() + t(c, delay)
        L.add(FxAnim(s, s + t(c, ms), draw, end))
    }

    private fun hide(v: View) { v.alpha = 0f }
    private fun show(v: View) { v.alpha = 1f }

    /** Where [v] is, in the overlay's coordinates. */
    private fun rectIn(base: View, v: View): RectF {
        val a = IntArray(2); val b = IntArray(2)
        v.getLocationInWindow(a); base.getLocationInWindow(b)
        val x = (a[0] - b[0]).toFloat(); val y = (a[1] - b[1]).toFloat()
        return RectF(x, y, x + v.width, y + v.height)
    }

    /** A picture of [v] as it looks now (its emoji, its colour, its size). */
    private fun snap(v: View): Bitmap? {
        if (v.width <= 0 || v.height <= 0) return null
        val b = Bitmap.createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        v.draw(Canvas(b))
        return b
    }

    /** Draws a picture turned by [deg], scaled by [scale] and faded by [alpha], around the centre of [r]. */
    private fun drawSnap(cv: Canvas, bmp: Bitmap, r: RectF, deg: Float, scale: Float, alpha: Int = 255) {
        val bp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        bp.alpha = alpha.coerceIn(0, 255)
        cv.save()
        cv.translate(r.centerX(), r.centerY())
        cv.rotate(deg)
        cv.scale(scale, scale)
        cv.drawBitmap(bmp, -bmp.width / 2f, -bmp.height / 2f, bp)
        cv.restore()
    }

    private fun paint(color: Int, alpha: Int = 255, style: Paint.Style = Paint.Style.FILL, w: Float = 0f): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
            this.style = style
            strokeWidth = w
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

    // easing curves
    private fun seg(p: Float, a: Float, b: Float): Float = ((p - a) / (b - a)).coerceIn(0f, 1f)
    private fun outCubic(p: Float): Float = 1f - (1f - p) * (1f - p) * (1f - p)
    private fun inOutCubic(p: Float): Float = if (p < 0.5f) 4f * p * p * p else 1f - (-2f * p + 2f).let { it * it * it } / 2f
    private fun backOut(p: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1f
        val q = p - 1f
        return 1f + c3 * q * q * q + c1 * q * q
    }

    /** A small pop of a plain view (count badge, pencil ink). */
    private fun bumpView(c: Context, v: View, to: Float = 1.4f) {
        v.animate().scaleX(to).scaleY(to).setDuration(t(c, 140)).setInterpolator(DecelerateInterpolator()).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(t(c, 260)).setInterpolator(OvershootInterpolator(2f)).start()
        }.start()
    }

    // ------------------------------------------------------------------ Note: a pen writes as you type, a stamp when saved
    private class Ink(val color: Int) {
        var len = 0f
        var anim: FxAnim? = null
    }
    private val inks = HashMap<TextView, Ink>()

    /** Press Note and type: a pen draws a line under the word as you type. Call it on every key. */
    fun writing(ctx: Context, tv: TextView, label: String, ms: Long = 1200) {
        val L = layer ?: return
        if (st(ctx, "note") != "ink") return
        val d = dp(ctx, 1f)
        val now = SystemClock.uptimeMillis()
        val old = inks[tv]
        val live = old?.anim?.let { it.end > now } ?: false
        if (old != null && live) {
            old.len += 7f * d
            if (old.len > tv.width.toFloat()) old.len = 7f * d   // start a new line of ink
            old.anim?.end = now + ms
            return
        }
        val ink = Ink(Prefs.accent(ctx))
        ink.len = 7f * d
        inks[tv] = ink
        val r0 = rectIn(L, tv)
        val a = FxAnim(now, now + ms, { cv, p -> drawInk(cv, ink, r0, d, p) }, null)
        ink.anim = a
        L.add(a)
    }

    private fun drawInk(cv: Canvas, ink: Ink, r: RectF, d: Float, p: Float) {
        val fade = 1f - seg(p, 0.82f, 1f)
        val x0 = r.left
        val y0 = r.bottom + 3f * d
        fun waveY(x: Float) = y0 + sin((x - x0) / (3f * d)) * 1.6f * d
        val path = Path()
        path.moveTo(x0, y0)
        var x = x0
        while (x < x0 + ink.len) {
            x = minOf(x + 2f * d, x0 + ink.len)
            path.lineTo(x, waveY(x))
        }
        val a = (210 * fade).toInt()
        cv.drawPath(path, paint(ink.color, a, Paint.Style.STROKE, 1.6f * d))
        // the pen at the end of the line
        val tx = x0 + ink.len
        val ty = waveY(tx)
        cv.save()
        cv.translate(tx, ty)
        cv.rotate(-35f + 9f * sin(SystemClock.uptimeMillis() / 55f))   // the pen tilts as it scribbles
        cv.drawRoundRect(RectF(-2.6f * d, -16f * d, 2.6f * d, 0f), 2.6f * d, 2.6f * d, paint(ink.color, (255 * fade).toInt()))
        val nib = Path().apply { moveTo(-2.6f * d, -2f * d); lineTo(2.6f * d, -2f * d); lineTo(0f, 5f * d); close() }
        cv.drawPath(nib, paint(ink.color, (255 * fade).toInt()))
        cv.restore()
    }

    /** The note was saved: a ring spreads out and a tick stamps in over the label. */
    fun saved(act: Activity, tv: TextView, label: String) {
        val L = layer ?: return
        if (st(act, "note") == "off") return
        val old = inks.remove(tv)
        old?.anim?.end = SystemClock.uptimeMillis()
        val d = dp(act, 1f)
        val r = rectIn(L, tv)
        val cx = r.centerX()
        val cy = r.centerY()
        val col = Prefs.accent(act)
        hide(tv)
        anim(act, L, 900, draw = { cv, p ->
            val rp = seg(p, 0f, 0.7f)
            cv.drawCircle(cx, cy, (12f + 30f * outCubic(rp)) * d, paint(col, (150 * (1f - rp)).toInt(), Paint.Style.STROKE, 2f * d))
            val s = backOut(seg(p, 0f, 0.3f))
            val fade = 1f - seg(p, 0.75f, 1f)
            cv.drawCircle(cx, cy, 12f * d * s, paint(col, (255 * fade).toInt()))
            cv.save()
            cv.translate(cx, cy)
            cv.scale(s, s)
            val tick = Path().apply { moveTo(-5f * d, 0.5f * d); lineTo(-1.5f * d, 4f * d); lineTo(5f * d, -3.5f * d) }
            cv.drawPath(tick, paint(Color.WHITE, (255 * fade).toInt(), Paint.Style.STROKE, 2.4f * d))
            cv.restore()
        }, end = { show(tv) })
    }

    // ------------------------------------------------------------------ Clear all: the messages go into the bin
    fun clearAll(act: Activity, root: ViewGroup, tv: TextView, label: String, action: () -> Unit) {
        val L = layer
        val style = st(act, "clear")
        if (L == null || style == "off") { action(); return }
        if (style == "sweep") { clearSweep(act, L, tv, action); return }
        val d = dp(act, 1f)
        val r = rectIn(L, tv)
        val cx = r.centerX()
        val cy = r.centerY()
        val ink = tv.currentTextColor
        hide(tv)
        // three messages fly into the bin, one after the other
        for (i in 0..2) {
            anim(act, L, 420, delay = 120L * i, draw = { cv, p ->
                val e = p * p
                val sx = cx + (i - 1) * 22f * d
                val sy = cy - 46f * d
                val x = sx + (cx - sx) * e
                val y = sy + (cy - 2f * d - sy) * e
                val s = 1f - 0.6f * p
                val fade = 1f - seg(p, 0.8f, 1f)
                cv.save()
                cv.translate(x, y)
                cv.scale(s, s)
                cv.drawRoundRect(RectF(-6.5f * d, -4.5f * d, 6.5f * d, 4.5f * d), 2f * d, 2f * d, paint(ink, (255 * fade).toInt()))
                cv.restore()
            })
        }
        // the bin: the lid lifts, the bin bounces as they land, the lid closes
        anim(act, L, 1100, draw = { cv, p ->
            val lid = -32f * minOf(seg(p, 0.2f, 0.4f), 1f - seg(p, 0.75f, 0.9f))
            val bounce = 1f + 0.12f * sin(seg(p, 0.5f, 0.9f) * PI_F)
            cv.save()
            cv.translate(cx, cy + 2f * d)
            cv.scale(bounce, bounce)
            val body = Path().apply {
                moveTo(-6.5f * d, -3f * d); lineTo(6.5f * d, -3f * d); lineTo(5f * d, 8f * d); lineTo(-5f * d, 8f * d); close()
            }
            cv.drawPath(body, paint(ink, 230))
            cv.save()
            cv.translate(-8f * d, -3f * d)
            cv.rotate(lid)
            cv.drawRoundRect(RectF(0f, -3.2f * d, 16f * d, 0f), 1.5f * d, 1.5f * d, paint(ink))
            cv.restore()
            cv.restore()
        }, end = { show(tv) })
        // the list is cleared as the messages land
        later(act, 620) { action() }
    }

    /** A soft band sweeps across the list and takes the messages with it. */
    private fun clearSweep(act: Activity, L: FxLayer, tv: TextView, action: () -> Unit) {
        val d = dp(act, 1f)
        val r = rectIn(L, tv)
        val col = tv.currentTextColor
        val area = RectF(8f * d, r.bottom + 24f * d, L.width.toFloat() - 8f * d, L.height * 0.85f)
        hide(tv)
        anim(act, L, 760, draw = { cv, p ->
            val x = area.left - 40f * d + (area.width() + 40f * d) * outCubic(seg(p, 0f, 0.6f))
            val fade = 1f - seg(p, 0.6f, 1f)
            cv.save()
            cv.clipRect(area)
            cv.drawRect(RectF(x - 70f * d, area.top, x, area.bottom), paint(col, (30 * fade).toInt()))
            cv.drawRect(RectF(x - 1.5f * d, area.top, x, area.bottom), paint(col, (140 * fade).toInt()))
            cv.restore()
        }, end = { show(tv) })
        later(act, 380) { action() }
    }

    // ------------------------------------------------------------------ Gear: one turn that settles
    fun gear(act: Activity, v: View, action: () -> Unit) {
        val L = layer
        val style = st(act, "gear")
        val bmp = if (L != null && style != "off") snap(v) else null
        if (L == null || bmp == null) { action(); return }
        val d = dp(act, 1f)
        val r = rectIn(L, v)
        val glow = Prefs.accent(act)
        hide(v)
        anim(act, L, 760, draw = { cv, p ->
            val turn = if (style == "turn") 360f * backOut(p) else 360f * outCubic(p)
            val s = 1f + 0.1f * sin(p * PI_F)
            cv.drawCircle(r.centerX(), r.centerY(), r.width() / 2f + 6f * d, paint(glow, (70f * sin(p * PI_F)).toInt()))
            drawSnap(cv, bmp, r, turn, s)
        }, end = { show(v); action() })
    }

    // ------------------------------------------------------------------ Refresh: an arrow draws a loop
    fun refresh(act: Activity, tv: TextView, action: () -> Unit) {
        val L = layer
        val style = st(act, "refresh")
        val bmp = if (L != null && style != "off") snap(tv) else null
        if (L == null || bmp == null) { action(); return }
        val d = dp(act, 1f)
        val r = rectIn(L, tv)
        val col = Prefs.accent(act)
        val rad = max(r.width(), r.height()) / 2f + 5f * d
        hide(tv)
        anim(act, L, 800, draw = { cv, p ->
            if (style == "spin") {
                drawSnap(cv, bmp, r, 360f * inOutCubic(p), 1f)
            } else {
                drawSnap(cv, bmp, r, 360f * inOutCubic(p), 1f)
                val sweep = 360f * outCubic(seg(p, 0f, 0.85f))
                val ring = RectF(r.centerX() - rad, r.centerY() - rad, r.centerX() + rad, r.centerY() + rad)
                val a = (255 * (1f - seg(p, 0.8f, 1f))).toInt()
                cv.drawArc(ring, -90f, sweep, false, paint(col, a, Paint.Style.STROKE, 2.2f * d))
                // the arrowhead at the front of the loop
                val ang = Math.toRadians((-90f + sweep).toDouble())
                val tx = r.centerX() + rad * cos(ang).toFloat()
                val ty = r.centerY() + rad * sin(ang).toFloat()
                cv.save()
                cv.translate(tx, ty)
                cv.rotate(Math.toDegrees(ang).toFloat() + 90f)
                val head = Path().apply { moveTo(6f * d, 0f); lineTo(-2f * d, -4.5f * d); lineTo(-2f * d, 4.5f * d); close() }
                cv.drawPath(head, paint(col, a))
                cv.restore()
            }
        }, end = { show(tv); action() })
    }

    // ------------------------------------------------------------------ Mic: sound bars while recording
    fun mic(act: Activity, root: ViewGroup, tv: TextView, action: () -> Unit) {
        val L = layer
        val style = st(act, "mic")
        if (L == null || style == "off") { action(); return }
        val d = dp(act, 1f)
        val r = rectIn(L, tv)
        val cx = r.centerX()
        val cy = r.centerY()
        if (style == "rings") {
            tv.animate().scaleX(1.12f).scaleY(1.12f).setDuration(t(act, 160)).withEndAction {
                tv.animate().scaleX(1f).scaleY(1f).setDuration(t(act, 240)).start()
            }.start()
            anim(act, L, 800, draw = { cv, p ->
                for (k in 0..2) {
                    val rp = seg(p, k * 0.16f, k * 0.16f + 0.6f)
                    if (rp > 0f) cv.drawCircle(cx, cy, (10f + 26f * outCubic(rp)) * d, paint(RED, (180 * (1f - rp)).toInt(), Paint.Style.STROKE, 2f * d))
                }
            })
        } else {
            // live audio: the icon turns into bars that move like a voice
            hide(tv)
            anim(act, L, 900, draw = { cv, p ->
                val bw = 3f * d
                val gap = 2.6f * d
                val bars = 7
                val total = bars * bw + (bars - 1) * gap
                val env = max(sin(p * PI_F), 0.2f)
                for (i in 0 until bars) {
                    val level = 0.5f + 0.5f * sin(p * (12f + i * 2.3f) + i * 1.7f)
                    val h = (4f + 18f * level) * d * env
                    val x = cx - total / 2f + i * (bw + gap)
                    cv.drawRoundRect(RectF(x, cy - h / 2f, x + bw, cy + h / 2f), bw / 2f, bw / 2f, paint(RED))
                }
            }, end = { show(tv) })
        }
        later(act, 700) { action() }
    }

    // ------------------------------------------------------------------ Pencil: draws a trail, or tilts and taps
    fun pencil(act: Activity, root: ViewGroup, tv: TextView, action: () -> Unit) {
        val L = layer
        val style = st(act, "edit")
        val bmp = if (L != null && style != "off") snap(tv) else null
        if (L == null || bmp == null) { action(); return }
        val d = dp(act, 1f)
        val r = rectIn(L, tv)
        val col = Prefs.accent(act)
        hide(tv)
        if (style == "tilt") {
            anim(act, L, 620, draw = { cv, p ->
                val wob = sin(p * PI_F * 4f) * (1f - p) * 22f
                drawSnap(cv, bmp, r, wob, 1f)
                for (k in 0..2) {
                    val a = seg(p, 0.15f + 0.15f * k, 0.45f + 0.15f * k) * (1f - seg(p, 0.75f, 1f))
                    cv.drawCircle(r.right + (6f + 7f * k) * d, r.centerY(), 1.9f * d, paint(col, (255 * a).toInt()))
                }
            }, end = { show(tv); action() })
        } else {
            val x0 = r.left - 4f * d
            val x1 = r.right + 26f * d
            val y0 = r.bottom + 8f * d
            fun waveY(u: Float) = y0 - sin(u * PI_F * 2f) * 4f * d
            anim(act, L, 1000, draw = { cv, p ->
                val e = inOutCubic(seg(p, 0f, 0.7f))
                val fade = 1f - seg(p, 0.75f, 1f)
                val trail = Path()
                val steps = 24
                for (k in 0..steps) {
                    val u = e * k / steps
                    val x = x0 + (x1 - x0) * u
                    if (k == 0) trail.moveTo(x, waveY(u)) else trail.lineTo(x, waveY(u))
                }
                cv.drawPath(trail, paint(col, (200 * fade).toInt(), Paint.Style.STROKE, 1.6f * d))
                // the pencil rides the tip of the trail
                val tipX = x0 + (x1 - x0) * e
                val tipY = waveY(e)
                val at = RectF(tipX - r.width() / 2f, tipY - r.height() / 2f, tipX + r.width() / 2f, tipY + r.height() / 2f)
                val pa = (255 * (1f - seg(p, 0.85f, 1f))).toInt()
                drawSnap(cv, bmp, at, -35f * (1f - seg(p, 0.85f, 1f)), 1f, pa)
            }, end = { show(tv); action() })
        }
    }

    // ------------------------------------------------------------------ Inbox title: an envelope drops into the title
    fun mail(act: Activity, tv: TextView, label: String) {
        val L = layer
        val style = st(act, "title")
        if (L == null || style == "off") return
        val d = dp(act, 1f)
        if (style == "bounce") {
            tv.animate().translationY(-5f * d).setDuration(t(act, 120)).withEndAction {
                tv.animate().translationY(0f).setDuration(t(act, 380)).setInterpolator(OvershootInterpolator(2.5f)).start()
            }.start()
            return
        }
        val r = rectIn(L, tv)
        val col = Prefs.accent(act)
        val ex = r.right + 10f * d
        val ey = r.centerY()
        anim(act, L, 900, draw = { cv, p ->
            val drop = -26f * d * (1f - backOut(seg(p, 0f, 0.4f)))
            val fade = 1f - seg(p, 0.7f, 1f)
            val box = RectF(ex, ey - 5.5f * d + drop, ex + 15f * d, ey + 5.5f * d + drop)
            cv.drawRoundRect(box, 2f * d, 2f * d, paint(col, (255 * fade).toInt()))
            val flap = Path().apply { moveTo(box.left, box.top); lineTo(box.centerX(), box.centerY() + 0.5f * d); lineTo(box.right, box.top) }
            cv.drawPath(flap, paint(Color.WHITE, (255 * fade).toInt(), Paint.Style.STROKE, 1.4f * d))
        })
        tv.animate().translationY(-3f * d).setDuration(t(act, 150)).withEndAction {
            tv.animate().translationY(0f).setDuration(t(act, 300)).setInterpolator(OvershootInterpolator(2f)).start()
        }.start()
    }

    // ------------------------------------------------------------------ Count badge: the number rolls up
    /** [prev] is the number before this message arrived. */
    fun badge(c: Context, v: TextView, prev: String) {
        val L = layer
        val style = st(c, "badge")
        if (style == "off") return
        val next = v.text.toString()
        if (L == null || style == "pop" || prev.toIntOrNull() == null || next.toIntOrNull() == null) {
            bumpView(c, v, 1.5f)
            return
        }
        val d = dp(c, 1f)
        val r = rectIn(L, v)
        val col = Prefs.accent(c)
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = v.textSize
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        hide(v)
        anim(c, L, 480, draw = { cv, p ->
            val fm = tp.fontMetrics
            val base = r.centerY() - (fm.ascent + fm.descent) / 2f
            val h = r.height()
            cv.save()
            cv.clipRect(r)
            cv.drawRoundRect(r, 10f * d, 10f * d, paint(col))
            cv.drawText(prev, r.centerX(), base - h * outCubic(seg(p, 0f, 0.55f)), tp)
            cv.drawText(next, r.centerX(), base + h * (1f - outCubic(seg(p, 0.25f, 1f))), tp)
            cv.restore()
        }, end = { show(v) })
    }

    // ------------------------------------------------------------------ Panel opens: the sheet rises, the scrim fades in, the header follows
    /** Moves the scale pivot of [sheet] to the tapped point [o] (screen coordinates); the middle when unknown. */
    private fun pivotAt(sheet: View, o: android.graphics.Rect?) {
        val loc = IntArray(2); sheet.getLocationOnScreen(loc)
        val px = if (o != null) o.exactCenterX() - loc[0] else sheet.width / 2f
        val py = if (o != null) o.exactCenterY() - loc[1] else sheet.height / 2f
        sheet.pivotX = px.coerceIn(0f, sheet.width.toFloat())
        sheet.pivotY = py.coerceIn(0f, sheet.height.toFloat())
    }

    fun open(c: Context, sheet: View, header: ViewGroup, root: View? = null, origin: android.graphics.Rect? = null) {
        val style = st(c, "open")
        if (style == "off") return
        val d = dp(c, 1f)
        sheet.alpha = 0f
        sheet.post {
            pivotAt(sheet, origin)
            when (style) {
                "slide" -> {
                    sheet.translationY = 56f * d
                    sheet.alpha = 0f
                    sheet.animate().translationY(0f).alpha(1f).setDuration(t(c, 380))
                        .setInterpolator(android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)).start()
                }
                "circle" -> {
                    sheet.alpha = 1f
                    val cx = sheet.pivotX.toInt(); val cy = sheet.pivotY.toInt()
                    val r = Math.hypot(maxOf(cx, sheet.width - cx).toDouble(), maxOf(cy, sheet.height - cy).toDouble()).toFloat()
                    try {
                        android.view.ViewAnimationUtils.createCircularReveal(sheet, cx, cy, 0f, r).apply {
                            duration = t(c, 420); interpolator = android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)
                        }.start()
                    } catch (e: Exception) { sheet.alpha = 1f }
                }
                "zoom" -> {
                    sheet.scaleX = 0.9f; sheet.scaleY = 0.9f; sheet.alpha = 0f
                    sheet.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(t(c, 400))
                        .setInterpolator(DecelerateInterpolator(1.6f)).start()
                    // parallax: the content moves a little slower than the panel, so it has depth
                    (sheet as? ViewGroup)?.let { g ->
                        if (g.childCount > 1) g.getChildAt(g.childCount - 1).let { body ->
                            body.translationY = 18f * d
                            body.animate().translationY(0f).setStartDelay(t(c, 40)).setDuration(t(c, 520))
                                .setInterpolator(android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)).start()
                        }
                    }
                }
                else -> { // spring
                    sheet.scaleX = 0.86f; sheet.scaleY = 0.86f; sheet.alpha = 0f
                    sheet.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(t(c, 380))
                        .setInterpolator(OvershootInterpolator(1.1f)).start()
                }
            }
        }
        root?.background?.let { bg ->
            val col = (bg as? android.graphics.drawable.ColorDrawable)?.color
            if (col != null) {
                val a0 = Color.alpha(col)
                val aT = if (style == "zoom") maxOf(a0, 0x70) else a0   // Zoom & Parallax: the background dims softly
                val va = ValueAnimator.ofInt(0, aT)
                va.duration = t(c, if (style == "zoom") 400 else 320)
                va.addUpdateListener { root.setBackgroundColor(Color.argb(it.animatedValue as Int, Color.red(col), Color.green(col), Color.blue(col))) }
                va.start()
            }
        }
        val views = (0 until header.childCount).map { header.getChildAt(it) }.filter { it.visibility == View.VISIBLE }
        for ((i, v) in views.withIndex()) {
            v.alpha = 0f
            v.translationY = 8f * d
            v.animate().alpha(1f).translationY(0f).setStartDelay(t(c, 140L + i * 55L)).setDuration(t(c, 300)).setInterpolator(DecelerateInterpolator()).start()
        }
    }

    // ------------------------------------------------------------------ Touch feedback: every tile and button squishes under the finger
    /** Gives [v] touch feedback without taking the touch away from its own click. Safe to call more than once. */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    fun press(c: Context, v: View) {
        if (v is EditText) return
        if (v.getTag(R.id.nt_press) != null) return
        v.setTag(R.id.nt_press, true)
        v.setOnTouchListener { view, ev ->
            val style = st(c, "press")
            if (style != "off") when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (style == "dim") view.animate().setStartDelay(0).alpha(0.6f).setDuration(t(c, 90)).start()
                    else view.animate().setStartDelay(0).scaleX(0.92f).scaleY(0.92f).setDuration(t(c, 110)).setInterpolator(DecelerateInterpolator()).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (style == "dim") view.animate().setStartDelay(0).alpha(1f).setDuration(t(c, 200)).start()
                    else view.animate().setStartDelay(0).scaleX(1f).scaleY(1f).setDuration(t(c, 380)).setInterpolator(OvershootInterpolator(3f)).start()
                }
            }
            false
        }
    }

    /** Touch feedback for every tappable thing inside [g] (tiles, buttons, page arrows). */
    fun pressAll(c: Context, g: View) {
        if (st(c, "press") == "off") return
        if (g.isClickable && g !is EditText) press(c, g)
        if (g is ViewGroup) for (i in 0 until g.childCount) pressAll(c, g.getChildAt(i))
    }

    // ------------------------------------------------------------------ Tiles appear: they rise one after another
    fun cascade(c: Context, list: ViewGroup) {
        val style = st(c, "list")
        if (style == "off") return
        val d = dp(c, 1f)
        for (i in 0 until list.childCount) {
            val v = list.getChildAt(i)
            val delay = t(c, 40L + i * 55L)
            v.animate().cancel()
            v.alpha = 0f
            if (style == "fade") {
                v.animate().alpha(1f).setStartDelay(delay).setDuration(t(c, 260)).start()
            } else {
                v.translationY = 22f * d
                v.scaleX = 0.96f
                v.scaleY = 0.96f
                v.animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f).setStartDelay(delay)
                    .setDuration(t(c, 420)).setInterpolator(OvershootInterpolator(1.1f)).start()
            }
        }
    }

    /** Clear all: the rows leave one after another (the last one first) before the list is cleared. */
    fun rowsOut(c: Context, list: ViewGroup) {
        if (st(c, "clear") == "off") return
        val d = dp(c, 1f)
        val n = list.childCount
        for (i in 0 until n) {
            val v = list.getChildAt(i)
            v.animate().alpha(0f).translationX(if (i % 2 == 0) -48f * d else 48f * d)
                .setStartDelay(t(c, 50L * (n - 1 - i))).setDuration(t(c, 260)).setInterpolator(DecelerateInterpolator()).start()
        }
    }

    // ------------------------------------------------------------------ Panel closes
    /** Closes the panel with the chosen style, then runs [done] (finish). */
    fun close(c: Context, sheet: View, root: View, from: Float = 0f, origin: android.graphics.Rect? = null, done: () -> Unit) {
        val style = st(c, "close")
        if (style == "off") { done(); return }
        val d = dp(c, 1f)
        val col = (root.background as? android.graphics.drawable.ColorDrawable)?.color
        if (col != null) {
            val a0 = Color.alpha(col)
            val va = ValueAnimator.ofInt(a0, 0)
            va.duration = t(c, 220)
            va.addUpdateListener { root.setBackgroundColor(Color.argb(it.animatedValue as Int, Color.red(col), Color.green(col), Color.blue(col))) }
            va.start()
        }
        val fso = android.view.animation.PathInterpolator(0.4f, 0f, 0.2f, 1f)
        val open = st(c, "open")
        if (style == "fade") {
            sheet.animate().alpha(0f).setDuration(t(c, 200)).withEndAction { done() }.start()
        } else if (from > 0f || open == "off") {
            sheet.animate().translationY(sheet.height + 40f * d).alpha(0.4f)
                .setDuration(t(c, 260)).setInterpolator(android.view.animation.AccelerateInterpolator(1.3f)).withEndAction { done() }.start()
        } else when (open) {
            "slide" -> sheet.animate().translationY(56f * d).alpha(0f).setDuration(t(c, 260)).setInterpolator(fso).withEndAction { done() }.start()
            "circle" -> try {
                pivotAt(sheet, origin)
                val cx = sheet.pivotX.toInt(); val cy = sheet.pivotY.toInt()
                val r = Math.hypot(maxOf(cx, sheet.width - cx).toDouble(), maxOf(cy, sheet.height - cy).toDouble()).toFloat()
                android.view.ViewAnimationUtils.createCircularReveal(sheet, cx, cy, r, 0f).apply {
                    duration = t(c, 300); interpolator = fso
                    addListener(object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(a: android.animation.Animator) { sheet.visibility = View.INVISIBLE; done() }
                    })
                }.start()
            } catch (e: Exception) { done() }
            "zoom" -> { pivotAt(sheet, origin); sheet.animate().scaleX(0.92f).scaleY(0.92f).alpha(0f).setDuration(t(c, 240)).setInterpolator(DecelerateInterpolator()).withEndAction { done() }.start() }
            else -> { pivotAt(sheet, origin); sheet.animate().scaleX(0.86f).scaleY(0.86f).alpha(0f).setDuration(t(c, 240)).setInterpolator(android.view.animation.AccelerateInterpolator(1.2f)).withEndAction { done() }.start() }
        }
    }

    // ------------------------------------------------------------------ The sheet follows a downward drag and drops away when pulled far or flicked
    class SwipeSheet(val ctx: Context, val canDrag: () -> Boolean, val onDismiss: (Float) -> Unit) : LinearLayout(ctx) {
        private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
        private var downX = 0f
        private var downY = 0f
        private var dragging = false
        private var vt: VelocityTracker? = null

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            if (st2(ctx) == "off") return false
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = ev.rawX; downY = ev.rawY; dragging = false; vt?.recycle(); vt = VelocityTracker.obtain(); vt?.addMovement(ev) }
                MotionEvent.ACTION_MOVE -> {
                    vt?.addMovement(ev)
                    val dy = ev.rawY - downY
                    val dx = ev.rawX - downX
                    if (!dragging && dy > slop * 2 && dy > Math.abs(dx) * 1.6f && canDrag()) {
                        dragging = true
                        parent?.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> { vt?.recycle(); vt = null }
            }
            return false
        }

        @android.annotation.SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(ev: MotionEvent): Boolean {
            if (!dragging) return super.onTouchEvent(ev)
            vt?.addMovement(ev)
            when (ev.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    val dy = (ev.rawY - downY).coerceAtLeast(0f)
                    translationY = dy
                    alpha = 1f - 0.5f * (dy / height.coerceAtLeast(1)).coerceIn(0f, 1f)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    vt?.computeCurrentVelocity(1000)
                    val vy = vt?.yVelocity ?: 0f
                    vt?.recycle(); vt = null
                    dragging = false
                    val far = translationY > height * 0.3f || vy > 1400f
                    if (far && ev.actionMasked == MotionEvent.ACTION_UP) {
                        onDismiss(translationY)
                    } else {
                        animate().translationY(0f).alpha(1f).setDuration(Alive.t(ctx, 380)).setInterpolator(OvershootInterpolator(1.6f)).start()
                    }
                }
            }
            return true
        }

        private fun st2(c: Context) = Alive.style(c, "close")
    }

    // ------------------------------------------------------------------ New message: slides in with a highlight
    fun newRow(c: Context, v: View) {
        val style = st(c, "row")
        if (style == "off") return
        val d = dp(c, 1f)
        v.alpha = 0f
        if (style == "fade") {
            v.animate().alpha(1f).setDuration(t(c, 420)).start()
            return
        }
        v.translationX = 56f * d
        v.animate().alpha(1f).translationX(0f).setDuration(t(c, 460)).setInterpolator(OvershootInterpolator(1.1f)).start()
        val L = layer ?: return
        v.post {
            val r = rectIn(L, v)
            val col = Prefs.accent(c)
            anim(c, L, 900, draw = { cv, p ->
                val fade = 1f - seg(p, 0.1f, 1f)
                cv.drawRoundRect(RectF(r.left - 2f * d, r.top - 2f * d, r.right + 2f * d, r.bottom + 2f * d), 14f * d, 14f * d,
                    paint(col, (200 * fade).toInt(), Paint.Style.STROKE, 2f * d))
            })
        }
    }
}
