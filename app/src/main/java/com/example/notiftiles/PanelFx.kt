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
 * The panel's animations. Each part has its own styles (Settings > Animation); "off" keeps it still.
 * Parts: Note, Clear all, Gear, Refresh, Mic, Pencil, the Inbox title, the count badge, the header when the panel
 * opens, and a new message tile. The speed (Slow / Normal / Fast) changes the length of all of them.
 */
object PanelFx {
    private val main = Handler(Looper.getMainLooper())
    private fun st(c: Context, key: String) = Alive.style(c, key)
    private fun t(c: Context, ms: Long) = Alive.t(c, ms)
    private fun dpf(c: Context, v: Float) = v * c.resources.displayMetrics.density
    private fun gone(c: Context): Boolean = (c as? Activity)?.let { it.isFinishing || it.isDestroyed } ?: false

    /** Runs [r] after [ms] (speed applied), unless the panel has closed in the meantime. */
    private fun at(c: Context, ms: Long, r: () -> Unit) {
        main.postDelayed({ if (!gone(c)) r() }, t(c, ms))
    }

    // Steps for [wave]: each one sets a value on the view over [ms] milliseconds
    private val ROT: (View, Float, Long) -> Unit = { v, x, ms -> v.animate().rotation(x).setDuration(ms).start() }
    private val MOVE_X: (View, Float, Long) -> Unit = { v, x, ms -> v.animate().translationX(x).setDuration(ms).start() }
    private val MOVE_Y: (View, Float, Long) -> Unit = { v, x, ms -> v.animate().translationY(x).setDuration(ms).start() }
    private val SIZE: (View, Float, Long) -> Unit = { v, x, ms -> v.animate().scaleX(x).scaleY(x).setDuration(ms).start() }

    /** Moves one property of [v] through [vals], one value every [step] ms (speed applied). */
    private fun wave(c: Context, v: View, step: Long, vals: FloatArray, prop: (View, Float, Long) -> Unit) {
        for ((i, x) in vals.withIndex()) at(c, i * step) { prop(v, x, t(c, step)) }
    }

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

    private fun bump(c: Context, v: View, to: Float = 1.25f) {
        v.animate().scaleX(to).scaleY(to).setDuration(t(c, 90)).withEndAction {
            v.animate().scaleX(1f).scaleY(1f).setDuration(t(c, 130)).start()
        }.start()
    }

    private fun popIn(c: Context, v: View) {
        v.scaleX = 0.6f; v.scaleY = 0.6f
        v.animate().scaleX(1f).scaleY(1f).setDuration(t(c, 320)).setInterpolator(OvershootInterpolator(3f)).start()
    }

    private fun center(root: View, v: View): FloatArray {
        val a = IntArray(2); val b = IntArray(2)
        v.getLocationInWindow(a); root.getLocationInWindow(b)
        return floatArrayOf(a[0] - b[0] + v.width / 2f, a[1] - b[1] + v.height / 2f)
    }

    /** A loose emoji floating above the panel, centred on (cx, cy). */
    private fun ghost(c: Context, root: ViewGroup, text: String, sp: Float, cx: Float, cy: Float): TextView {
        val g = TextView(c)
        g.text = text
        g.textSize = sp
        root.addView(g, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        g.measure(0, 0)
        g.x = cx - g.measuredWidth / 2f
        g.y = cy - g.measuredHeight / 2f
        return g
    }

    // ------------------------------------------------------------------ Note: writing while you type, check when saved
    private val writeEnd = HashMap<TextView, Long>()

    /** Press Note and type: the icon writes. Call again on every key to keep it writing. */
    fun writing(ctx: Context, tv: TextView, label: String, ms: Long = 1800) {
        val style = st(ctx, "note")
        if (style == "off") return
        val running = writeEnd.containsKey(tv)
        writeEnd[tv] = SystemClock.uptimeMillis() + t(ctx, ms)
        if (running) return
        orig(tv)
        val d = dpf(ctx, 1f)
        var i = 0
        val step = object : Runnable {
            override fun run() {
                val end = writeEnd[tv] ?: return
                if (SystemClock.uptimeMillis() >= end) { writeEnd.remove(tv); restore(tv); return }
                when (style) {
                    "bounce" -> {
                        val up = i % 2 == 0
                        tv.text = (if (up) "📝" else "🖊️") + " " + label
                        tv.translationY = if (up) 0f else -4f * d
                        tv.rotation = if (up) -6f else 6f
                    }
                    "sparkle" -> {
                        tv.text = (if (i % 2 == 0) "✍️" else "📝") + " " + label
                        val root = tv.rootView as? ViewGroup
                        if (i % 3 == 0 && root != null) {
                            val c = center(root, tv)
                            val g = ghost(ctx, root, "✨", 12f, c[0] + 14f * d, c[1] - 10f * d)
                            g.animate().alpha(0f).y(g.y - 12f * d).setDuration(t(ctx, 420)).withEndAction { root.removeView(g) }.start()
                        }
                    }
                    else -> {
                        // the writing hand: travels along the line and bobs
                        val s = Math.sin(i * 0.9).toFloat()
                        tv.text = listOf("✍️", "✍️", "🖊️", "✍️")[i % 4] + " " + label
                        tv.translationX = s * 3f * d
                        tv.translationY = if (i % 2 == 0) 0f else d
                        tv.rotation = s * 7f
                    }
                }
                i++
                main.postDelayed(this, t(ctx, 120))
            }
        }
        main.post(step)
    }

    /** The note was saved: a check mark pops in, then the normal icon returns. */
    fun saved(act: Activity, tv: TextView, label: String) {
        val style = st(act, "note")
        if (style == "off") return
        writeEnd.remove(tv)
        orig(tv)
        calm(tv)
        tv.text = "✅ " + label
        popIn(act, tv)
        val root = tv.rootView as? ViewGroup
        if (style == "sparkle" && root != null) {
            val c = center(root, tv)
            val g = ghost(act, root, "✨", 16f, c[0], c[1] - 16f * dpf(act, 1f))
            g.scaleX = 0.3f; g.scaleY = 0.3f
            g.animate().scaleX(1.3f).scaleY(1.3f).alpha(0f).setDuration(t(act, 500)).withEndAction { root.removeView(g) }.start()
        }
        at(act, 900) { restore(tv) }
    }

    // ------------------------------------------------------------------ Clear all
    fun clearAll(act: Activity, root: ViewGroup, tv: TextView, label: String, action: () -> Unit) {
        when (st(act, "clear")) {
            "off" -> action()
            "broom" -> clearBroom(act, root, tv, label, action)
            "poof" -> clearPoof(act, root, tv, label, action)
            else -> clearBasket(act, root, tv, label, action)
        }
    }

    /** Basket: the messages drop in, the basket tips over and empties, then a check mark. */
    private fun clearBasket(act: Activity, root: ViewGroup, tv: TextView, label: String, action: () -> Unit) {
        orig(tv)
        calm(tv)
        val d = dpf(act, 1f)
        tv.text = "🧺 " + label
        val c = center(root, tv)
        val things = listOf("📨", "💬", "📩")
        // 1. messages drop into the basket, it bumps each time
        for ((i, e) in things.withIndex()) at(act, i * 230L) {
            val g = ghost(act, root, e, 17f, c[0] - 14f * d + i * 14f * d, c[1] - 52f * d)
            g.animate().y(c[1] - g.measuredHeight / 2f).alpha(0.35f).setDuration(t(act, 260)).setInterpolator(AccelerateInterpolator())
                .withEndAction { root.removeView(g); bump(act, tv) }.start()
        }
        // 2. the basket tips over, everything pours out and the list clears
        at(act, 950) {
            tv.pivotX = tv.width / 2f; tv.pivotY = tv.height.toFloat()
            tv.animate().rotation(-38f).setDuration(t(act, 220)).start()
            for ((i, e) in things.withIndex()) {
                val g = ghost(act, root, e, 15f, c[0] - 6f * d, c[1] - 4f * d)
                g.animate().x(g.x - (30f + i * 14f) * d).y(g.y + (14f + i * 8f) * d).rotation(-50f - i * 25f).alpha(0f)
                    .setDuration(t(act, 520)).setInterpolator(DecelerateInterpolator()).withEndAction { root.removeView(g) }.start()
            }
            action()
        }
        // 3. upright, empty, and a check mark pops in
        at(act, 1350) {
            tv.animate().rotation(0f).setDuration(t(act, 200)).start()
            tv.text = "✅ " + label
            popIn(act, tv)
        }
        at(act, 2100) { restore(tv) }
    }

    /** Broom: the label sweeps side to side and dust puffs out, then a check mark. */
    private fun clearBroom(act: Activity, root: ViewGroup, tv: TextView, label: String, action: () -> Unit) {
        orig(tv)
        calm(tv)
        val d = dpf(act, 1f)
        val c = center(root, tv)
        tv.text = "🧹 " + label
        tv.pivotX = tv.width / 2f; tv.pivotY = tv.height.toFloat()
        wave(act, tv, 110L, floatArrayOf(-20f, 20f, -20f, 20f, 0f), ROT)
        for (i in 0..3) at(act, i * 120L) {
            val g = ghost(act, root, if (i % 2 == 0) "🫧" else "✨", 14f, c[0] + (i * 6f - 9f) * d, c[1])
            g.animate().y(g.y - 26f * d).alpha(0f).setDuration(t(act, 420)).withEndAction { root.removeView(g) }.start()
        }
        at(act, 600) {
            action()
            tv.text = "✅ " + label
            popIn(act, tv)
        }
        at(act, 1300) { restore(tv) }
    }

    /** Poof: the basket shrinks, a little cloud bursts out, then it comes back with a check mark. */
    private fun clearPoof(act: Activity, root: ViewGroup, tv: TextView, label: String, action: () -> Unit) {
        orig(tv)
        calm(tv)
        val d = dpf(act, 1f)
        val c = center(root, tv)
        tv.text = "🧺 " + label
        tv.animate().scaleX(0.3f).scaleY(0.3f).alpha(0.3f).setDuration(t(act, 180)).setInterpolator(AccelerateInterpolator()).start()
        at(act, 200) {
            val dirs = listOf(-26f to -12f, 0f to -28f, 26f to -12f)
            for ((i, dxy) in dirs.withIndex()) {
                val g = ghost(act, root, if (i == 1) "✨" else "💨", 16f, c[0], c[1])
                g.animate().x(g.x + dxy.first * d).y(g.y + dxy.second * d).alpha(0f)
                    .setDuration(t(act, 480)).setInterpolator(DecelerateInterpolator()).withEndAction { root.removeView(g) }.start()
            }
            action()
            tv.text = "✅ " + label
            tv.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(t(act, 200)).start()
        }
        at(act, 1100) { restore(tv) }
    }

    // ------------------------------------------------------------------ Gear: before Settings opens
    fun gear(act: Activity, v: View, action: () -> Unit) {
        when (st(act, "gear")) {
            "off" -> action()
            "turn" -> {
                calm(v)
                v.animate().rotation(180f).scaleX(1.1f).scaleY(1.1f).setDuration(t(act, 380)).setInterpolator(DecelerateInterpolator(1.4f))
                    .withEndAction { calm(v); action() }.start()
            }
            "wobble" -> {
                calm(v)
                wave(act, v, 90L, floatArrayOf(-18f, 18f, -12f, 12f, -5f, 0f), ROT)
                at(act, 6 * 90L) { calm(v); action() }
            }
            else -> {
                calm(v)
                v.animate().rotation(540f).scaleX(1.25f).scaleY(1.25f).setDuration(t(act, 650)).setInterpolator(DecelerateInterpolator(1.6f))
                    .withEndAction { calm(v); action() }.start()
            }
        }
    }

    // ------------------------------------------------------------------ Refresh: while it refreshes
    fun refresh(act: Activity, tv: TextView, action: () -> Unit) {
        when (st(act, "refresh")) {
            "off" -> action()
            "pulse" -> {
                calm(tv)
                wave(act, tv, 160L, floatArrayOf(1.3f, 1f, 1.3f, 1f), SIZE)
                at(act, 4 * 160L) { calm(tv); action(); popIn(act, tv) }
            }
            "one" -> {
                calm(tv)
                tv.animate().rotation(360f).setDuration(t(act, 450)).setInterpolator(DecelerateInterpolator(1.4f))
                    .withEndAction { calm(tv); action(); popIn(act, tv) }.start()
            }
            else -> {
                calm(tv)
                tv.animate().rotation(720f).setDuration(t(act, 750)).setInterpolator(DecelerateInterpolator(1.4f))
                    .withEndAction { calm(tv); action(); popIn(act, tv) }.start()
            }
        }
    }

    // ------------------------------------------------------------------ Mic: before the recorder opens
    fun mic(act: Activity, root: ViewGroup, tv: TextView, action: () -> Unit) {
        when (st(act, "mic")) {
            "off" -> action()
            "pulse" -> {
                calm(tv)
                wave(act, tv, 220L, floatArrayOf(1.35f, 1f, 1.35f, 1f), SIZE)
                at(act, 4 * 220L) { calm(tv); action() }
            }
            "wave" -> {
                calm(tv)
                val d = dpf(act, 1f)
                wave(act, tv, 120L, floatArrayOf(-4f * d, 4f * d, -4f * d, 4f * d, 0f), MOVE_Y)
                at(act, 5 * 120L) { calm(tv); action() }
            }
            else -> redRings(act, root, tv, action)
        }
    }

    /** Live red rings spreading out from the mic. */
    private fun redRings(act: Activity, root: ViewGroup, tv: TextView, action: () -> Unit) {
        calm(tv)
        val d = dpf(act, 1f)
        val c = center(root, tv)
        bump(act, tv, 1.35f)
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
            ring.animate().scaleX(1.9f).scaleY(1.9f).alpha(0f).setDuration(t(act, 600)).setInterpolator(DecelerateInterpolator())
                .withEndAction { root.removeView(ring) }.start()
        }
        at(act, 700) { action() }
    }

    // ------------------------------------------------------------------ Pencil: before the edit menu opens
    fun pencil(act: Activity, root: ViewGroup, tv: TextView, action: () -> Unit) {
        val style = st(act, "edit")
        if (style == "off") { action(); return }
        orig(tv)
        calm(tv)
        val d = dpf(act, 1f)
        val c = center(root, tv)
        if (style == "wiggle" || style == "sparkle") {
            if (style == "sparkle") {
                at(act, 60) {
                    val g = ghost(act, root, "✨", 16f, c[0], c[1] - 18f * d)
                    g.scaleX = 0.3f; g.scaleY = 0.3f
                    g.animate().scaleX(1.3f).scaleY(1.3f).alpha(0f).y(g.y - 10f * d).setDuration(t(act, 500)).withEndAction { root.removeView(g) }.start()
                }
            }
            wave(act, tv, 90L, floatArrayOf(-14f, 14f, -10f, 10f, 0f), ROT)
            at(act, 6 * 90L) { restore(tv); action() }
            return
        }
        // write: the pencil writes a line, then a sparkle
        tv.text = "✏️"
        val xs = floatArrayOf(-6f, 6f, -6f, 6f, 0f)
        for ((i, x) in xs.withIndex()) at(act, i * 110L) {
            tv.animate().translationX(x * d).rotation(if (i % 2 == 0) -22f else 12f).setDuration(t(act, 110)).start()
        }
        at(act, 580) {
            val g = ghost(act, root, "✨", 16f, c[0], c[1] - 18f * d)
            g.scaleX = 0.3f; g.scaleY = 0.3f
            g.animate().scaleX(1.3f).scaleY(1.3f).alpha(0f).y(g.y - 10f * d).setDuration(t(act, 500)).withEndAction { root.removeView(g) }.start()
        }
        at(act, 700) { restore(tv); action() }
    }

    // ------------------------------------------------------------------ Inbox title: when a message arrives
    fun mail(act: Activity, tv: TextView, label: String) {
        val style = st(act, "title")
        if (style == "off") return
        orig(tv)
        calm(tv)
        val d = dpf(act, 1f)
        when (style) {
            "bounce" -> {
                wave(act, tv, 90L, floatArrayOf(-10f * d, 0f, -6f * d, 0f), MOVE_Y)
                at(act, 420) { restore(tv) }
            }
            "shake" -> {
                wave(act, tv, 70L, floatArrayOf(-7f * d, 7f * d, -7f * d, 7f * d, 0f), MOVE_X)
                at(act, 420) { restore(tv) }
            }
            else -> {
                // the flag goes up
                val frames = listOf("📪", "📫", "📬")
                for ((i, e) in frames.withIndex()) at(act, i * 230L) {
                    tv.text = Look.withEmoji(e, label)
                    bump(act, tv, 1.18f)
                }
                at(act, 1100) { restore(tv) }
            }
        }
    }

    // ------------------------------------------------------------------ Count badge: when a message arrives
    fun badge(c: Context, v: View) {
        when (st(c, "badge")) {
            "off" -> Unit
            "grow" -> v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(t(c, 400)).withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(t(c, 400)).start()
            }.start()
            "wiggle" -> {
                calm(v)
                wave(c, v, 80L, floatArrayOf(-12f, 12f, -8f, 8f, 0f), ROT)
            }
            else -> bump(c, v, 1.5f)
        }
    }

    // ------------------------------------------------------------------ Panel opens: the header icons come in one by one
    fun open(c: Context, header: ViewGroup) {
        val style = st(c, "open")
        if (style == "off") return
        val d = dpf(c, 1f)
        val views = (0 until header.childCount).map { header.getChildAt(it) }.filter { it.visibility == View.VISIBLE }
        for ((i, v) in views.withIndex()) {
            when (style) {
                "slide" -> { v.translationY = -18f * d; v.alpha = 0f }
                "fade" -> { v.alpha = 0f }
                else -> { v.scaleX = 0.4f; v.scaleY = 0.4f }
            }
            val a = v.animate().setStartDelay(t(c, i * 90L)).setDuration(t(c, 320))
            when (style) {
                "slide" -> a.translationY(0f).alpha(1f).setInterpolator(DecelerateInterpolator())
                "fade" -> a.alpha(1f)
                else -> a.scaleX(1f).scaleY(1f).setInterpolator(OvershootInterpolator(3f))
            }.start()
        }
    }

    // ------------------------------------------------------------------ New message tile: comes in
    fun newRow(c: Context, v: View) {
        val d = dpf(c, 1f)
        when (st(c, "row")) {
            "off" -> Unit
            "fade" -> { v.alpha = 0f; v.animate().alpha(1f).setDuration(t(c, 420)).start() }
            "drop" -> {
                v.alpha = 0f; v.translationY = -26f * d
                v.animate().alpha(1f).translationY(0f).setDuration(t(c, 420)).setInterpolator(DecelerateInterpolator()).start()
            }
            else -> {
                v.alpha = 0f; v.translationX = 90f * d
                v.animate().alpha(1f).translationX(0f).setDuration(t(c, 380)).setInterpolator(DecelerateInterpolator()).start()
            }
        }
    }
}
