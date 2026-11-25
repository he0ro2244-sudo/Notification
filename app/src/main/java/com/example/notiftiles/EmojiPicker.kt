package com.example.notiftiles

import android.app.Activity
import android.app.AlertDialog
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

// Emoji in place of the built-in icons and words (notes, mic, reset, Clear all, pencil). Used by Settings and the pencil menu.
object EmojiPicker {
    class Slot(val key: String, val label: String, val builtIn: String, val choices: List<String>)

    val SLOTS = listOf(
        Slot("note", "Notes (+ Note)", "+", Look.EMOJI_NOTE),
        Slot("mic", "Mic (voice recorder)", "mic icon", Look.EMOJI_MIC),
        Slot("reset", "Reset / refresh", "↻ icon", Look.EMOJI_RESET),
        Slot("clear", "Clear all", "none", Look.EMOJI_CLEAR),
        Slot("edit", "Pencil", "✎", Look.EMOJI_EDIT),
        Slot("gear", "Settings (gear)", "gear icon", Look.EMOJI_GEAR),
        Slot("inbox", "Inbox title", "none", Look.EMOJI_INBOX),
        // The same places while the inbox is empty ("same" = the icon above stays; 📬 turns into 📭 by itself)
        Slot("inbox_e", "Inbox title when empty", "automatic (📬 → 📭)", listOf("📭", "📪") + Look.EMOJI_INBOX),
        Slot("clear_e", "Clear all when empty", "same", Look.EMOJI_CLEAR),
        Slot("mic_e", "Mic when empty", "same", Look.EMOJI_MIC),
        Slot("note_e", "Notes when empty", "same", Look.EMOJI_NOTE)
    )

    /** One-line summary of what is chosen now, for Settings. */
    fun summary(act: Activity): String =
        SLOTS.joinToString("   ") { s -> Prefs.emojiOf(act, s.key).ifEmpty { "·" } }

    /** First a list of the places, then the emoji to choose for the one tapped. [onDone] runs after a change. */
    fun show(act: Activity, onDone: () -> Unit) {
        val names = SLOTS.map { s ->
            val e = Prefs.emojiOf(act, s.key)
            s.label + "     " + (if (e.isEmpty()) s.builtIn else e)
        }.toTypedArray()
        AlertDialog.Builder(act)
            .setTitle("Emoji icons")
            .setItems(names) { _, i -> pick(act, SLOTS[i], onDone) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun pick(act: Activity, s: Slot, onDone: () -> Unit) {
        val d = act.resources.displayMetrics.density
        val box = LinearLayout(act)
        box.orientation = LinearLayout.VERTICAL
        val pad = (16 * d).toInt()
        box.setPadding(pad, pad / 2, pad, 0)
        var dlg: AlertDialog? = null

        fun choose(v: String) {
            Prefs.setEmojiOf(act, s.key, v)
            TilesWidget.rebuildAll(act.applicationContext)
            dlg?.dismiss()
            onDone()
        }

        var i = 0
        while (i < s.choices.size) {
            val row = LinearLayout(act)
            row.orientation = LinearLayout.HORIZONTAL
            for (k in i until minOf(i + 5, s.choices.size)) {
                val e = s.choices[k]
                val tv = TextView(act)
                tv.text = e
                tv.textSize = 28f
                tv.gravity = Gravity.CENTER
                tv.setOnClickListener { choose(e) }
                row.addView(tv, LinearLayout.LayoutParams(0, (56 * d).toInt(), 1f))
            }
            box.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            i += 5
        }

        val own = EditText(act)
        own.hint = "Or type / paste your own emoji"
        own.setSingleLine(true)
        box.addView(own)
        val use = Button(act)
        use.isAllCaps = false
        use.text = "Use my emoji"
        use.setOnClickListener {
            val v = own.text.toString().trim()
            if (v.isNotEmpty()) choose(v)
        }
        box.addView(use)

        val scroll = ScrollView(act)
        scroll.addView(box)
        val built = AlertDialog.Builder(act)
            .setTitle(s.label)
            .setView(scroll)
            .setNeutralButton("Normal icon") { _, _ -> choose("") }
            .setNegativeButton("Cancel", null)
            .create()
        dlg = built
        built.show()
    }
}
