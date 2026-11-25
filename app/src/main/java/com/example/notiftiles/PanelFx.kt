package com.example.notiftiles

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView

/**
 * The panel's icon stories. Every icon has its own, and they play when you press the icon:
 *  - Note: becomes a writing hand that moves like writing while you type; a check mark when saved
 *  - Clear all: a basket catches the messages, tips over and empties, then a check mark
 *  - Gear: spins up and slows down, then Settings opens
 *  - Refresh: turns twice and slows down while it refreshes
 *  - Mic: red rings spread out like a live recording
 *  - Pencil: writes a line and finishes with a sparkle
 *  - Mail box (Inbox title): the flag goes up when a message arrives
 */
object PanelFx {
    private val main = Handler(Looper.getMainLooper())
    private fun on(c: Context) = Prefs.aliveOn(c) && Prefs.alivePanel(c)
    private fun dpf(c: Context, v: Float) = v * c.resources.displayMetrics.density
    private fun at(act: Activity, ms: Long, r: () -> Unit) { main.postDelayed({ if (!act.isFinishing && !act.isDestroyed) r() }, ms) }

    // The text the view had before a story started, so it can be put back exactly
    private fun orig(tv: TextView): CharSequence {
        val t = tv.getTag(R.id.nt_alive)
        if (t is CharSequence) return t
        tv.setTag(R.id.nt_alive, tv.text)
        return tv.text
    }

    private fun restore(tv: TextView) {
        (tv.getTag(R.id.nt_alive) as? CharSequence)?.let { tv.text = it }
        tv.setTag(R.id.nt_alive, null)
        calm(tv)
    }

    private fun calm(v: View) {
        v.animate().cancel()
        v.rotation = 0f; v.translationX = 0f; v.translationY = 0f; v.scaleX = 1f; v.scaleY = 1f; v.alpha = 1f
    }

    private fun bump(v: View, to: Float = 1.25f) {
        v.animate().scaleX(to).scaleY(to).setDuration(90).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(130).start()
        }.start()
    }

    private fun pop(v: View) {
        v.scaleX = 0.6f; v.scaleY = 0.6f
        v.animate().scaleX(1f).scaleY(1f).setDuration(320).setInterpolator(OvershootInterpolator(3f)).start()
    }

    private fun center(root: View, v: View): FloatArray {
        val a = IntArray(2); val b = IntArray(2)
        v.getLocationInWindow(a); root.getLocationInWindow(b)
        return floatArrayOf(a[0] - b[0] + v.width / 2f, a[1] - b[1] + v.height / 2f)
    }

    /** A loose emoji floating above the panel, centred on (cx, cy). */
    private fun ghost(act: Activity, root: ViewGroup, text: String, sp: Float, cx: Float, cy: Float): TextView {
        val g = TextView(act)
        g.text = text
        g.textSize = sp
        root.addView(g, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        g.measure(0, 0)
        g.x = cx - g.measuredWidth / 2f
        g.y = cy - g.measuredHeight / 2f
        return g
    }

    // ------------------------------------------------------------------ Note: hand writing
    private val writeEnd = HashMap<TextView, Long>()

    /** Press Note and type: the icon becomes a hand that writes. Call again on every key to keep it writing. */
    fun writing(ctx: Context, tv: TextView, label: String, ms: Long = 1800) {
        if (!on(ctx)) return
        val running = writeEnd.containsKey(tv)
        writeEnd[tv] = SystemClock.uptimeMillis() + ms
        if (running) return
        orig(tv)
        val d = dpf(ctx, 1f)
        var i = 0
        val poses = listOf("✍️", "✍️", "🖊️", "✍️")
        val step = object : Runnable {
            override fun run() {
                val end = writeEnd[tv] ?: return
                if (SystemClock.uptimeMillis() >= end) { writeEnd.remove(tv); restore(tv); return }
                val s = Math.sin(i * 0.9).toFloat()
                tv.text = poses[i % 4] + " " + label
                tv.translationX = s * 3f * d     // the hand travels along the line
                tv.translationY = if (i % 2 == 0) 0f else d
                tv.rotation = s * 7f
                i++
                main.postDelayed(this, 120)
            }
        }
        main.post(step)
    }

    /** The note was saved: a check mark pops in, then the normal icon returns. */
    fun saved(act: Activity, tv: TextView, label: String) {
        if (!on(act)) return
        writeEnd.remove(tv)
        orig(tv)
        calm(tv)
        tv.text = "✅ " + label
        pop(tv)
        at(act, 900) { restore(tv) }
    }

    // ------------------------------------------------------------------ Clear all: basket fills, tips, empties, check
    fun clearAll(act: Activity, root: ViewGroup, tv: TextView, label: String, action: () -> Unit) {
        if (!on(act)) { action(); return }
        orig(tv)
        calm(tv)
        val d = dpf(act, 1f)
        tv.text = "🧺 " + label
        val c = center(root, tv)
        val things = listOf("📨", "💬", "📩")
        // 1. messages drop into the basket, it bumps each time
        for ((i, e) in things.withIndex()) at(act, i * 230L) {
            val g = ghost(act, root, e, 17f, c[0] - 14f * d + i * 14f * d, c[1] - 52f * d)
            g.animate().y(c[1] - g.measuredHeight / 2f).alpha(0.35f).setDuration(260).setInterpolator(AccelerateInterpolator())
                .withEndAction { root.removeView(g); bump(tv) }.start()
        }
        // 2. the basket tips over, everything pours out and the list clears
        at(act, 950) {
            tv.pivotX = tv.width / 2f; tv.pivotY = tv.height.toFloat()
            tv.animate().rotation(-38f).setDuration(220).start()
            for ((i, e) in things.withIndex()) {
                val g = ghost(act, root, e, 15f, c[0] - 6f * d, c[1] - 4f * d)
                g.animate().x(g.x - (30f + i * 14f) * d).y(g.y + (14f + i * 8f) * d).rotation(-50f - i * 25f).alpha(0f)
                    .setDuration(520).setInterpolator(DecelerateInterpolator()).withEndAction { root.removeView(g) }.start()
            }
            action()
        }
        // 3. upright, empty, and a check mark pops in
        at(act, 1350) {
            tv.animate().rotation(0f).setDuration(200).start()
            tv.text = "✅ " + label
            pop(tv)
        }
        at(act, 2100) { restore(tv) }
    }

    // ------------------------------------------------------------------ Gear: spins up and slows down
    fun gear(act: Activity, v: View, action: () -> Unit) {
        if (!on(act)) { action(); return }
        calm(v)
        v.animate().rotation(540f).scaleX(1.25f).scaleY(1.25f).setDuration(650).setInterpolator(DecelerateInterpolator(1.6f))
            .withEndAction { calm(v); action() }.start()
    }

    // ------------------------------------------------------------------ Refresh: turns twice while it refreshes
    fun refresh(act: Activity, tv: TextView, action: () -> Unit) {
        if (!on(act)) { action(); return }
        calm(tv)
        tv.animate().rotation(720f).setDuration(750).setInterpolator(DecelerateInterpolator(1.4f))
            .withEndAction { calm(tv); action(); pop(tv) }.start()
    }

    // ------------------------------------------------------------------ Mic: live red rings
    fun mic(act: Activity, root: ViewGroup, tv: TextView, action: () -> Unit) {
        if (!on(act)) { action(); return }
        calm(tv)
        val d = dpf(act, 1f)
        val c = center(root, tv)
        bump(tv, 1.35f)
        for (i in 0..2) at(act, i * 200L) {
            val ring = View(act)
            val g = GradientDrawable()
            g.shape = GradientDrawable.OVAL
            g.setStroke((2 * d).toInt().coerceAtLeast(2), Color.parseColor("#E53935"))
            g.setColor(Color.TRANSPARENT)
            ring.background = g
            val sz = (34 * d).toInt()
            root.addView(ring, FrameLayout.LayoutParams(sz, sz))
            ring.x = c[0] - sz / 2f; ring.y = c[1] - sz / 2f
            ring.scaleX = 0.4f; ring.scaleY = 0.4f; ring.alpha = 0.9f
            ring.animate().scaleX(1.9f).scaleY(1.9f).alpha(0f).setDuration(600).setInterpolator(DecelerateInterpolator())
                .withEndAction { root.removeView(ring) }.start()
        }
        at(act, 700) { action() }
    }

    // ------------------------------------------------------------------ Pencil: writes a line, then a sparkle
    fun pencil(act: Activity, root: ViewGroup, tv: TextView, action: () -> Unit) {
        if (!on(act)) { action(); return }
        orig(tv)
        calm(tv)
        val d = dpf(act, 1f)
        val c = center(root, tv)
        tv.text = "✏️"
        val xs = floatArrayOf(-6f, 6f, -6f, 6f, 0f)
        for ((i, x) in xs.withIndex()) at(act, i * 110L) {
            tv.animate().translationX(x * d).rotation(if (i % 2 == 0) -22f else 12f).setDuration(110).start()
        }
        at(act, 580) {
            val g = ghost(act, root, "✨", 16f, c[0], c[1] - 18f * d)
            g.scaleX = 0.3f; g.scaleY = 0.3f
            g.animate().scaleX(1.3f).scaleY(1.3f).alpha(0f).y(g.y - 10f * d).setDuration(500).withEndAction { root.removeView(g) }.start()
        }
        at(act, 700) { restore(tv); action() }
    }

    // ------------------------------------------------------------------ Mail box: the flag goes up
    fun mail(act: Activity, tv: TextView, label: String) {
        if (!on(act)) return
        orig(tv)
        calm(tv)
        val frames = listOf("📪", "📫", "📬")
        for ((i, e) in frames.withIndex()) at(act, i * 230L) {
            tv.text = Look.withEmoji(e, label)
            bump(tv, 1.18f)
        }
        at(act, 1100) { restore(tv) }
    }
}
