package com.example.notiftiles

import android.animation.TimeInterpolator
import android.content.Context
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator

/**
 * The extra animation styles. They only use view properties (scale, rotation, translation, alpha), so they run on the
 * render thread and allocate nothing per frame. PanelFx and the settings preview both play them from here, so what you
 * see in the picker is exactly what the panel does.
 * Every new style is added after the old ones, so nothing you already chose changes.
 */
object FxStyles {
    private val smooth = PathInterpolator(0.4f, 0f, 0.2f, 1f)

    /** Styles added in this release, by part. The key is the same as in Alive.ELEMENTS. */
    val NEW: Map<String, List<Pair<String, String>>> = mapOf(
        "open" to listOf("flip" to "Flip up", "drop" to "Drop in with a bounce", "unfold" to "Unfold from the top",
            "swing" to "Swing down like a door", "tiltup" to "Rise and tilt up (3D)", "burst" to "Burst out with a pop"),
        "gear" to listOf("wobble" to "Wobble and settle", "bounce" to "Hop and half turn",
            "tada" to "Wind up, spin and grow", "jelly" to "Jelly squash and spin"),
        "refresh" to listOf("pulse" to "Pulse and turn", "flip" to "Flip over",
            "spiral" to "Spiral in and out", "swirl" to "Wind back, then swirl"),
        "mic" to listOf("blink" to "Red dot blinks", "pulse" to "Heartbeat", "jump" to "Jump and shake"),
        "title" to listOf("shake" to "Shake", "flip" to "Flip", "heart" to "Heartbeat", "tada" to "Wiggle and shine"),
        "badge" to listOf("shake" to "Shake", "flip" to "Flip over", "heart" to "Heartbeat", "tada" to "Wiggle and shine"),
        "row" to listOf("pop" to "Pop in", "drop" to "Drop from the top",
            "spin" to "Spin in", "swing" to "Swing in from the side"),
        "press" to listOf("pulse" to "Grow a little", "tilt" to "Tilt",
            "jelly" to "Jelly (squash and wobble back)", "lift" to "Lift up"),
        "list" to listOf("slide" to "Slide in from the left", "zoom" to "Zoom in one by one",
            "wave" to "Wave in from both sides", "flipin" to "Flip in one by one", "bounce" to "Bounce up one by one")
    )

    /** One step of a chained animation: every value is where the view should be at the end of the step. */
    private class Step(
        val ms: Long, val rot: Float = 0f, val sx: Float = 1f, val sy: Float = 1f,
        val tx: Float = 0f, val ty: Float = 0f, val a: Float = 1f, val rx: Float = 0f, val ry: Float = 0f,
        val ip: TimeInterpolator = PathInterpolator(0.4f, 0f, 0.2f, 1f)
    )

    private fun chain(v: View, steps: List<Step>, i: Int = 0, end: () -> Unit) {
        if (i >= steps.size) { end(); return }
        val s = steps[i]
        v.animate().setStartDelay(0).rotation(s.rot).rotationX(s.rx).rotationY(s.ry).scaleX(s.sx).scaleY(s.sy)
            .translationX(s.tx).translationY(s.ty).alpha(s.a).setDuration(s.ms).setInterpolator(s.ip)
            .withEndAction { chain(v, steps, i + 1, end) }.start()
    }

    fun isNew(key: String, style: String): Boolean = NEW[key]?.any { it.first == style } == true

    /** The picker's pages (like the emoji keyboard's tabs): a tab icon, its name, and the parts it holds. */
    class Tab(val icon: String, val name: String, val keys: List<String>)
    val TABS = listOf(
        Tab("🪟", "Panel", listOf("open", "close", "list")),
        Tab("👆", "Touch", listOf("press", "gear", "refresh", "mic", "edit")),
        Tab("📬", "Messages", listOf("row", "title", "badge", "clear")),
        Tab("📝", "Notes", listOf("note"))
    )

    // ---------------------------------------------------------------- a small icon-like view
    /** Plays [style] on [v] (a gear, a refresh arrow, the title, the badge, a tile...). [then] runs when it is over. */
    fun play(c: Context, v: View, key: String, style: String, then: (() -> Unit)? = null) {
        val T = { ms: Long -> Alive.t(c, ms) }
        val d = c.resources.displayMetrics.density
        v.animate().cancel()
        v.pivotX = v.width / 2f; v.pivotY = v.height / 2f
        fun reset() { v.rotation = 0f; v.rotationX = 0f; v.rotationY = 0f; v.scaleX = 1f; v.scaleY = 1f; v.translationX = 0f; v.translationY = 0f; v.alpha = 1f }
        fun done() { reset(); then?.invoke() }
        v.cameraDistance = 8000f * d
        when (key + ":" + style) {
            "gear:wobble" -> v.animate().rotation(38f).setDuration(T(160)).setInterpolator(DecelerateInterpolator()).withEndAction {
                v.animate().rotation(-26f).setDuration(T(200)).withEndAction {
                    v.animate().rotation(14f).setDuration(T(160)).withEndAction {
                        v.animate().rotation(0f).setDuration(T(160)).withEndAction { done() }.start()
                    }.start()
                }.start()
            }.start()
            "gear:bounce" -> v.animate().translationY(-9f * d).rotation(180f).setDuration(T(260)).setInterpolator(DecelerateInterpolator()).withEndAction {
                v.animate().translationY(0f).rotation(360f).setDuration(T(420)).setInterpolator(BounceInterpolator()).withEndAction { done() }.start()
            }.start()
            "refresh:pulse" -> v.animate().scaleX(1.25f).scaleY(1.25f).rotation(180f).setDuration(T(300)).setInterpolator(DecelerateInterpolator()).withEndAction {
                v.animate().scaleX(1f).scaleY(1f).rotation(360f).setDuration(T(380)).setInterpolator(OvershootInterpolator(2f)).withEndAction { done() }.start()
            }.start()
            "refresh:flip" -> v.animate().rotationY(360f).setDuration(T(650)).setInterpolator(smooth).withEndAction { done() }.start()
            "mic:blink" -> v.animate().scaleX(1.18f).scaleY(1.18f).alpha(0.35f).setDuration(T(150)).withEndAction {
                v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(T(150)).withEndAction {
                    v.animate().alpha(0.35f).setDuration(T(150)).withEndAction {
                        v.animate().alpha(1f).setDuration(T(150)).withEndAction { done() }.start()
                    }.start()
                }.start()
            }.start()
            "title:shake", "badge:shake" -> v.animate().translationX(-6f * d).setDuration(T(60)).withEndAction {
                v.animate().translationX(6f * d).setDuration(T(100)).withEndAction {
                    v.animate().translationX(-4f * d).setDuration(T(90)).withEndAction {
                        v.animate().translationX(0f).setDuration(T(120)).setInterpolator(OvershootInterpolator(2f)).withEndAction { done() }.start()
                    }.start()
                }.start()
            }.start()
            "title:flip", "badge:flip" -> v.animate().rotationX(360f).setDuration(T(520)).setInterpolator(smooth).withEndAction { done() }.start()
            "press:pulse" -> v.animate().scaleX(1.08f).scaleY(1.08f).setDuration(T(110)).start()
            "press:tilt" -> v.animate().rotation(-4f).scaleX(0.96f).scaleY(0.96f).setDuration(T(110)).start()
            "row:pop" -> { v.scaleX = 0.7f; v.scaleY = 0.7f; v.alpha = 0f
                v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(T(380)).setInterpolator(OvershootInterpolator(2.2f)).withEndAction { then?.invoke() }.start() }
            "row:drop" -> { v.translationY = -36f * d; v.alpha = 0f
                v.animate().translationY(0f).alpha(1f).setDuration(T(520)).setInterpolator(BounceInterpolator()).withEndAction { then?.invoke() }.start() }
            "gear:tada" -> chain(v, listOf(
                Step(T(150), rot = -10f, sx = 0.8f, sy = 0.8f),
                Step(T(420), rot = 300f, sx = 1.3f, sy = 1.3f, ip = OvershootInterpolator(2f)),
                Step(T(260), rot = 360f, ip = DecelerateInterpolator()))) { done() }
            "gear:jelly" -> chain(v, listOf(
                Step(T(110), sx = 1.25f, sy = 0.75f),
                Step(T(130), rot = 120f, sx = 0.8f, sy = 1.2f),
                Step(T(130), rot = 240f, sx = 1.12f, sy = 0.9f),
                Step(T(240), rot = 360f, ip = OvershootInterpolator(3f)))) { done() }
            "refresh:spiral" -> chain(v, listOf(
                Step(T(420), rot = 360f, sx = 0.55f, sy = 0.55f, a = 0.7f, ip = AccelerateInterpolator()),
                Step(T(420), rot = 720f, ip = OvershootInterpolator(2.2f)))) { done() }
            "refresh:swirl" -> chain(v, listOf(
                Step(T(220), rot = -50f, sx = 1.15f, sy = 1.15f),
                Step(T(520), rot = 400f, sx = 1.2f, sy = 1.2f, ip = AccelerateInterpolator(1.2f)),
                Step(T(220), rot = 360f, ip = OvershootInterpolator(2.5f)))) { done() }
            "mic:pulse", "title:heart", "badge:heart" -> chain(v, listOf(
                Step(T(110), sx = 1.3f, sy = 1.3f),
                Step(T(110), sx = 1f, sy = 1f),
                Step(T(110), sx = 1.3f, sy = 1.3f),
                Step(T(240), sx = 1f, sy = 1f, ip = OvershootInterpolator(2f)))) { done() }
            "mic:jump" -> chain(v, listOf(
                Step(T(160), ty = -10f * d, sx = 0.9f, sy = 1.15f, ip = DecelerateInterpolator()),
                Step(T(300), ty = 0f, ip = BounceInterpolator()),
                Step(T(90), rot = -12f), Step(T(90), rot = 12f), Step(T(120), rot = 0f))) { done() }
            "title:tada", "badge:tada" -> chain(v, listOf(
                Step(T(120), rot = -5f, sx = 0.92f, sy = 0.92f),
                Step(T(120), rot = 5f, sx = 1.12f, sy = 1.12f),
                Step(T(120), rot = -5f, sx = 1.12f, sy = 1.12f),
                Step(T(120), rot = 5f, sx = 1.12f, sy = 1.12f),
                Step(T(220), ip = OvershootInterpolator(2f)))) { done() }
            "row:spin" -> { v.scaleX = 0.2f; v.scaleY = 0.2f; v.rotation = -180f; v.alpha = 0f
                v.animate().scaleX(1f).scaleY(1f).rotation(0f).alpha(1f).setDuration(T(520)).setInterpolator(OvershootInterpolator(1.6f)).withEndAction { then?.invoke() }.start() }
            "row:swing" -> { v.pivotX = 0f; v.rotationY = -80f; v.alpha = 0f
                v.animate().rotationY(0f).alpha(1f).setDuration(T(520)).setInterpolator(OvershootInterpolator(1.3f)).withEndAction { v.pivotX = v.width / 2f; then?.invoke() }.start() }
            "press:jelly" -> v.animate().scaleX(0.88f).scaleY(1.06f).setDuration(T(100)).start()
            "press:lift" -> v.animate().scaleX(1.1f).scaleY(1.1f).translationY(-3f * d).setDuration(T(120)).start()
            else -> then?.invoke()
        }
    }

    /** Touch feedback release for the two new touch styles (the old ones are handled in PanelFx.press). */
    fun release(c: Context, v: View) {
        val T = { ms: Long -> Alive.t(c, ms) }
        v.animate().setStartDelay(0).rotation(0f).scaleX(1f).scaleY(1f).translationY(0f).setDuration(T(320)).setInterpolator(OvershootInterpolator(2.5f)).start()
    }

    // ---------------------------------------------------------------- the panel sheet
    /** Opens [sheet] with a new style. Returns false when [style] is not one of the new ones. */
    fun open(c: Context, sheet: View, style: String): Boolean {
        val T = { ms: Long -> Alive.t(c, ms) }
        val d = c.resources.displayMetrics.density
        sheet.cameraDistance = 9000f * d
        when (style) {
            "flip" -> {
                sheet.pivotY = sheet.height.toFloat()
                sheet.rotationX = -70f; sheet.alpha = 0f
                sheet.animate().rotationX(0f).alpha(1f).setDuration(T(440)).setInterpolator(smooth).start()
            }
            "drop" -> {
                sheet.translationY = -sheet.height * 0.35f; sheet.alpha = 0f
                sheet.animate().translationY(0f).alpha(1f).setDuration(T(620)).setInterpolator(BounceInterpolator()).start()
            }
            "unfold" -> {
                sheet.pivotY = 0f
                sheet.scaleY = 0.05f; sheet.alpha = 0f
                sheet.animate().scaleY(1f).alpha(1f).setDuration(T(420)).setInterpolator(smooth).start()
            }
            "swing" -> {
                sheet.pivotY = 0f
                sheet.rotationX = 85f; sheet.alpha = 0f
                sheet.animate().rotationX(0f).alpha(1f).setDuration(T(520)).setInterpolator(OvershootInterpolator(1.2f)).start()
            }
            "tiltup" -> {
                sheet.pivotY = sheet.height.toFloat()
                sheet.translationY = sheet.height * 0.18f; sheet.rotationX = 25f; sheet.scaleX = 0.92f; sheet.scaleY = 0.92f; sheet.alpha = 0f
                sheet.animate().translationY(0f).rotationX(0f).scaleX(1f).scaleY(1f).alpha(1f).setDuration(T(480)).setInterpolator(smooth).start()
            }
            "burst" -> {
                sheet.scaleX = 0.2f; sheet.scaleY = 0.2f; sheet.rotation = -6f; sheet.alpha = 0f
                sheet.animate().scaleX(1f).scaleY(1f).rotation(0f).alpha(1f).setDuration(T(520)).setInterpolator(OvershootInterpolator(2.4f)).start()
            }
            else -> return false
        }
        return true
    }

    /** Closes [sheet] the way [style] opened it. Returns false when it is not one of the new ones. */
    fun close(c: Context, sheet: View, style: String, done: () -> Unit): Boolean {
        val T = { ms: Long -> Alive.t(c, ms) }
        when (style) {
            "flip" -> { sheet.pivotY = sheet.height.toFloat(); sheet.animate().rotationX(-70f).alpha(0f).setDuration(T(260)).setInterpolator(AccelerateInterpolator(1.2f)).withEndAction { done() }.start() }
            "drop" -> sheet.animate().translationY(-sheet.height * 0.35f).alpha(0f).setDuration(T(260)).setInterpolator(AccelerateInterpolator(1.3f)).withEndAction { done() }.start()
            "unfold" -> { sheet.pivotY = 0f; sheet.animate().scaleY(0.05f).alpha(0f).setDuration(T(260)).setInterpolator(smooth).withEndAction { done() }.start() }
            "swing" -> { sheet.pivotY = 0f; sheet.animate().rotationX(85f).alpha(0f).setDuration(T(280)).setInterpolator(AccelerateInterpolator(1.2f)).withEndAction { done() }.start() }
            "tiltup" -> { sheet.pivotY = sheet.height.toFloat(); sheet.animate().translationY(sheet.height * 0.18f).rotationX(25f).alpha(0f).setDuration(T(280)).setInterpolator(AccelerateInterpolator(1.2f)).withEndAction { done() }.start() }
            "burst" -> sheet.animate().scaleX(0.2f).scaleY(0.2f).alpha(0f).setDuration(T(260)).setInterpolator(AnticipateOvershootInterpolator(1.5f)).withEndAction { done() }.start()
            else -> return false
        }
        return true
    }

    /** A tile of the list appears (Tiles appear). Returns false when [style] is not a new one. */
    fun listIn(c: Context, v: View, style: String, delay: Long, index: Int = 0): Boolean {
        val T = { ms: Long -> Alive.t(c, ms) }
        val d = c.resources.displayMetrics.density
        when (style) {
            "slide" -> { v.translationX = -48f * d
                v.animate().alpha(1f).translationX(0f).setStartDelay(delay).setDuration(T(380)).setInterpolator(smooth).start() }
            "zoom" -> { v.scaleX = 0.6f; v.scaleY = 0.6f
                v.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(delay).setDuration(T(380)).setInterpolator(OvershootInterpolator(1.6f)).start() }
            "wave" -> { val side = if (index % 2 == 0) -1f else 1f
                v.translationX = side * 52f * d; v.rotation = side * 3f
                v.animate().alpha(1f).translationX(0f).rotation(0f).setStartDelay(delay).setDuration(T(460)).setInterpolator(OvershootInterpolator(1.2f)).start() }
            "flipin" -> { v.cameraDistance = 9000f * d; v.pivotY = 0f; v.rotationX = -90f
                v.animate().alpha(1f).rotationX(0f).setStartDelay(delay).setDuration(T(440)).setInterpolator(smooth).withEndAction { v.pivotY = v.height / 2f }.start() }
            "bounce" -> { v.translationY = 70f * d
                v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(T(640)).setInterpolator(BounceInterpolator()).start() }
            else -> return false
        }
        return true
    }
}
