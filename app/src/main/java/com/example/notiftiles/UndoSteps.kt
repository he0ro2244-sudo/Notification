package com.example.notiftiles

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout

/**
 * Remembers the widget's look one step at a time, so "Undo" can take back each change in turn.
 * The pencil screens and Settings each have their own list ([scope]) and never touch the other's.
 *
 * While a screen is open it watches the look / layout settings. Changes that come close together (a slider being dragged)
 * count as one step: the copy from just before the first of them is kept.
 */
class ChangeTracker(context: Context, private val scope: String, private val onChange: () -> Unit = {}) {
    private companion object {
        // Trackers that are running now. Two screens of the same scope can run together (Settings and the Arrange screen opened from it).
        val active = ArrayList<ChangeTracker>()
    }

    private val ctx: Context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private var shadow = ""
    private var pendingBefore: String? = null
    private var running = false

    private val commit = Runnable { flush() }

    // Kept in a field: the preferences only hold a listener weakly
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key != null && Prefs.isTrackedKey(key)) {
            if (pendingBefore == null) pendingBefore = shadow
            handler.removeCallbacks(commit)
            handler.postDelayed(commit, 800)
        }
    }

    fun start() {
        if (running) return
        running = true
        shadow = Prefs.fullJson(ctx)
        pendingBefore = null
        active.add(this)
        Prefs.store(ctx).registerOnSharedPreferenceChangeListener(listener)
    }

    fun stop() {
        if (!running) return
        flush()
        running = false
        active.remove(this)
        Prefs.store(ctx).unregisterOnSharedPreferenceChangeListener(listener)
    }

    /** Closes the step in progress now (before an Undo, a Reset or leaving the screen). */
    fun flush() {
        handler.removeCallbacks(commit)
        val before = pendingBefore ?: return
        pendingBefore = null
        val now = Prefs.fullJson(ctx)
        if (now != before) {
            Prefs.pushSnapshot(ctx, scope, before)
            onChange()
        }
        shadow = now
    }

    /** Call after the app itself changed things (Undo, Reset, loading a layout): that is not a new step. */
    fun resync() {
        quietResync()
        for (t in ArrayList(active)) if (t !== this && t.scope == scope) t.quietResync()
        onChange()
    }

    private fun quietResync() {
        handler.removeCallbacks(commit)
        pendingBefore = null
        shadow = Prefs.fullJson(ctx)
    }

    /** Takes back one step. Returns false when there is nothing to take back. */
    fun undo(): Boolean {
        flush()
        val ok = Prefs.undoLook(ctx, scope)
        resync()
        if (ok) TilesWidget.rebuildAll(ctx)
        return ok
    }
}

/** The "Undo" button shown on Settings, Arrange & resize and Move. Its text shows how many steps can be taken back. */
object UndoBar {
    fun label(c: Context, scope: String): String {
        val n = Prefs.undoSteps(c, scope)
        return if (n > 0) "↶  Undo last change  ($n)" else "↶  Undo last change  (nothing to undo)"
    }

    fun make(c: Context, scope: String, tracker: ChangeTracker, after: () -> Unit): Button {
        val b = Button(c)
        b.isAllCaps = false
        b.text = label(c, scope)
        b.setOnClickListener {
            val ok = tracker.undo()
            android.widget.Toast.makeText(c, if (ok) "Brought back" else "Nothing to undo", android.widget.Toast.LENGTH_SHORT).show()
            after()
        }
        return b
    }

    /** Pins [bar] under the live preview and above the scrolling content ([root] is what LivePreview.wrap returned). */
    fun pin(root: android.view.View, bar: Button) {
        (root as LinearLayout).addView(bar, 2, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }
}
