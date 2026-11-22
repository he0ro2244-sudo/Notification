package com.example.notiftiles

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A short diary of what happened when a button was pressed in the panel (app, button, what the app allowed, what failed
 * and why). Never stores message texts. Shown in Settings > Button log, to be copied and sent for fixing.
 */
object ActionLog {
    private const val MAX = 60

    private fun sp(c: Context) = c.getSharedPreferences("nt_log", Context.MODE_PRIVATE)

    @Synchronized
    fun add(c: Context, text: String) {
        try {
            val now = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            val old = sp(c).getString("log", "") ?: ""
            val lines = if (old.isEmpty()) ArrayList() else ArrayList(old.split("\n"))
            val line = "$now  $text"
            if (text.startsWith("card ") && lines.lastOrNull()?.endsWith(text) == true) return
            lines.add(line)
            while (lines.size > MAX) lines.removeAt(0)
            sp(c).edit().putString("log", lines.joinToString("\n")).apply()
        } catch (e: Exception) {
        }
    }

    fun text(c: Context): String = sp(c).getString("log", "") ?: ""

    fun clear(c: Context) {
        sp(c).edit().remove("log").apply()
    }

    fun show(a: Activity) {
        val t = text(a)
        val tv = TextView(a)
        tv.text = if (t.isEmpty()) "Nothing yet. Press buttons in the panel (x, Open, Reply, View, Like...) and come back here." else t
        tv.typeface = Typeface.MONOSPACE
        tv.textSize = 12f
        tv.setTextIsSelectable(true)
        tv.setPadding(40, 24, 40, 24)
        val sv = ScrollView(a)
        sv.addView(tv)
        AlertDialog.Builder(a).setTitle("Button log").setView(sv)
            .setPositiveButton("Close", null)
            .setNeutralButton("Copy") { _, _ ->
                val cm = a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Button log", text(a)))
                Toast.makeText(a, "Copied", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Clear") { _, _ -> clear(a) }
            .show()
    }

    /** What kind of thing a notification button starts. */
    fun kind(pi: android.app.PendingIntent?): String = when {
        pi == null -> "none"
        pi.isActivity -> "opens a screen"
        pi.isBroadcast -> "background broadcast"
        pi.isService -> "background service"
        else -> "other"
    }
}
