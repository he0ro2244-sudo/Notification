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

    private fun failedOnly(t: String): String = t.split("\n").filter { it.contains("fail", ignoreCase = true) }.joinToString("\n")

    fun show(a: Activity) {
        val all = text(a)
        var only = true
        fun shown(): String {
            val t = if (only) failedOnly(all) else all
            return if (t.isEmpty()) (if (all.isEmpty()) "Nothing yet. Press buttons in the panel (x, Open, Reply, View, Like...) and come back here." else "No failures. Untick \"Failed only\" to see everything.") else t
        }
        val tv = TextView(a)
        tv.text = shown()
        tv.typeface = Typeface.MONOSPACE
        tv.textSize = 12f
        tv.setTextIsSelectable(true)
        tv.setPadding(40, 24, 40, 24)
        val cb = android.widget.CheckBox(a)
        cb.text = "Failed only"
        cb.isChecked = true
        cb.setPadding(40, 16, 40, 0)
        cb.setOnCheckedChangeListener { _, on -> only = on; tv.text = shown() }
        val sv = ScrollView(a)
        sv.addView(tv)
        val box = android.widget.LinearLayout(a)
        box.orientation = android.widget.LinearLayout.VERTICAL
        box.addView(cb)
        box.addView(sv)
        AlertDialog.Builder(a).setTitle("Button log").setView(box)
            .setPositiveButton("Close", null)
            .setNeutralButton("Copy") { _, _ ->
                val cm = a.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Button log", if (only) failedOnly(all) else all))
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
