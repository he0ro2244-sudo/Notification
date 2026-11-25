package com.example.notiftiles

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.RemoteViews

/**
 * Alive icons.
 *  - State: icons change with the inbox (see Prefs.emojiFor).
 *  - Widget: when a new message arrives the icons blink one after another (a short wave). Widgets cannot run real
 *    animations, so this is a few small partial updates a third of a second apart that only change the opacity.
 *  - Panel: the icons pop one after another when it opens, and the inbox title and Clear all breathe while there are messages.
 */
object Alive {
    private val main = Handler(Looper.getMainLooper())
    private var seen = -1
    private var running = false

    // The widget views the wave visits, in order (a view that is not on this layout is simply skipped)
    private val WAVE = intArrayOf(R.id.hdr_title, R.id.count, R.id.wmic, R.id.wnote, R.id.wclear, R.id.wrefresh, R.id.wedit, R.id.wset)

    /** Called whenever the notifications changed: starts the wave when there are more messages than before. */
    fun onMessages(ctx: Context) {
        val n = try { Feed.items(ctx).size } catch (e: Exception) { return }
        val more = seen in 0 until n
        seen = n
        if (more && Prefs.aliveOn(ctx) && Prefs.aliveWave(ctx)) wave(ctx.applicationContext)
    }

    private fun wave(ctx: Context) {
        if (running) return
        running = true
        val mgr = AppWidgetManager.getInstance(ctx)
        val ids = mgr.getAppWidgetIds(ComponentName(ctx, TilesWidget::class.java))
        if (ids.isEmpty()) { running = false; return }
        fun frame(active: Int) {
            val rv = RemoteViews(ctx.packageName, R.layout.widget_tiles)
            for ((i, v) in WAVE.withIndex()) rv.setFloat(v, "setAlpha", if (i == active) 0.3f else 1f)
            try { mgr.partiallyUpdateAppWidget(ids, rv) } catch (e: Exception) { }
        }
        for (i in WAVE.indices) main.postDelayed({ frame(i) }, 330L * (i + 1))
        main.postDelayed({
            frame(-1) // everything back to full
            running = false
        }, 330L * (WAVE.size + 2))
    }

    /** Panel: the given icons pop one after another (call once when the panel opens). */
    fun pop(ctx: Context, views: List<View>) {
        if (!Prefs.aliveOn(ctx) || !Prefs.alivePanel(ctx)) return
        for ((i, v) in views.withIndex()) {
            if (v.visibility != View.VISIBLE) continue
            v.animate().cancel()
            v.animate().scaleX(1.35f).scaleY(1.35f).setStartDelay(120L * i).setDuration(160)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { v.animate().scaleX(1f).scaleY(1f).setStartDelay(0).setDuration(180).start() }
                .start()
        }
    }

    /** Panel: slow breathing while the view is on screen (call again to restart; [stop] ends it). */
    fun breathe(ctx: Context, v: View) {
        if (!Prefs.aliveOn(ctx) || !Prefs.alivePanel(ctx)) return
        v.animate().cancel()
        v.alpha = 1f
        fun down() { v.animate().alpha(0.55f).setDuration(1100).setStartDelay(0).withEndAction { up(v) }.start() }
        down()
    }

    private fun up(v: View) {
        v.animate().alpha(1f).setDuration(1100).withEndAction {
            if (v.isAttachedToWindow) v.animate().alpha(0.55f).setDuration(1100).withEndAction { up(v) }.start()
        }.start()
    }

    fun stop(v: View) {
        v.animate().cancel()
        v.alpha = 1f
        v.scaleX = 1f
        v.scaleY = 1f
    }
}
