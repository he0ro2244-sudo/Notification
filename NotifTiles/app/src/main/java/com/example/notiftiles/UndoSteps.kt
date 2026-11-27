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
            Prefs.pushSnapshot(ctx, scope, before, Prefs.describe(before, now))
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

    /** Takes back the last [n] steps. Returns false when there is nothing to take back. */
    fun undo(n: Int = 1): Boolean {
        flush()
        val ok = Prefs.undoLook(ctx, scope, n)
        resync()
        if (ok) TilesWidget.rebuildAll(ctx)
        return ok
    }

    /** Puts back the next [n] steps that were taken back. */
    fun redo(n: Int = 1): Boolean {
        flush()
        val ok = Prefs.redoLook(ctx, scope, n)
        resync()
        if (ok) TilesWidget.rebuildAll(ctx)
        return ok
    }
}

/**
 * The Undo / Redo / History buttons shown on Settings, Arrange & resize and Move.
 * History is a list of what was done (newest first, with the time): tap a line to go back to just before it,
 * or tap a line above "now" to go forward again.
 */
class UndoBar(private val c: android.app.Activity, private val scope: String, private val tracker: ChangeTracker, private val after: () -> Unit) {
    val view = LinearLayout(c)
    private val undoB = Button(c)
    private val redoB = Button(c)
    private val histB = Button(c)

    init {
        view.orientation = LinearLayout.HORIZONTAL
        for (b in listOf(undoB, redoB, histB)) {
            b.isAllCaps = false
            b.setSingleLine(true)
            view.addView(b, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }
        histB.text = "☰ History"
        undoB.setOnClickListener { done(tracker.undo(1), "Undone") }
        redoB.setOnClickListener { done(tracker.redo(1), "Redone") }
        histB.setOnClickListener { showHistory() }
        refresh()
    }

    fun refresh() {
        val u = Prefs.undoSteps(c, scope)
        val r = Prefs.redoSteps(c, scope)
        undoB.text = "↶ Undo ($u)"
        redoB.text = "↷ Redo ($r)"
        undoB.isEnabled = u > 0
        redoB.isEnabled = r > 0
    }

    private fun done(ok: Boolean, msg: String) {
        android.widget.Toast.makeText(c, if (ok) msg else "Nothing to do", android.widget.Toast.LENGTH_SHORT).show()
        refresh()
        after()
    }

    private fun showHistory() {
        tracker.flush()
        val fmt = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val redo = Prefs.redoList(c, scope)
        val undo = Prefs.undoList(c, scope)
        val lines = ArrayList<String>()
        val acts = ArrayList<() -> Boolean>()
        // Redo entries: farthest in the future first, so the list reads top (later) to bottom (earlier) like the undo part
        for (i in redo.indices.reversed()) {
            val e = redo[i]
            lines.add("↷  " + e.l + "  (" + fmt.format(java.util.Date(e.t)) + ")")
            val n = i + 1
            acts.add { tracker.redo(n) }
        }
        lines.add("—— now ——")
        acts.add { false }
        for (i in undo.indices) {
            val e = undo[i]
            lines.add("↶  " + e.l + "  (" + fmt.format(java.util.Date(e.t)) + ")")
            val n = i + 1
            acts.add { tracker.undo(n) }
        }
        if (undo.isEmpty() && redo.isEmpty()) {
            android.widget.Toast.makeText(c, "Nothing done yet", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(c)
            .setTitle("Tap a line: undo back to just before it (↶) or redo up to it (↷)")
            .setItems(lines.toTypedArray()) { _, which ->
                if (lines[which].startsWith("——")) return@setItems
                done(acts[which](), "Done")
            }
            .setNeutralButton("Clear history") { _, _ -> confirmClear() }
            .setNegativeButton("Close", null)
            .show()
    }

    /** Deletes the list of steps only; nothing on the widget changes. */
    private fun confirmClear() {
        android.app.AlertDialog.Builder(c)
            .setMessage("Clear the undo history? The widget stays as it is now, but you can no longer go back to earlier steps.")
            .setPositiveButton("Clear") { _, _ ->
                Prefs.clearHistory(c, scope)
                tracker.resync()
                android.widget.Toast.makeText(c, "History cleared", android.widget.Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        /** Pins [bar] under the live preview and above the scrolling content ([root] is what LivePreview.wrap returned). */
        fun pin(root: android.view.View, bar: UndoBar) {
            (root as LinearLayout).addView(bar.view, 2, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }
}
