package com.example.notiftiles

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** What the pencil on the widget does: Move (drag items to another place), Arrange & resize (sections, sizes, colours) Saved layouts, or Reset (everything back to default). */
class EditMenuActivity : Activity() {

    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(24), dp(16), dp(24))

        val title = TextView(this)
        title.text = "Pencil ✎"
        title.textSize = 20f
        title.setTypeface(title.typeface, Typeface.BOLD)
        col.addView(title)

        val hint = TextView(this)
        hint.text = "What do you want to do with the widget?"
        hint.textSize = 13f
        hint.alpha = 0.7f
        hint.setPadding(0, dp(2), 0, dp(4))
        col.addView(hint)

        col.addView(choice("Move", "Press any item of the widget (clock, date, weather, Where to?, each fast-dial circle, each shortcut circle, title, count, mic, + Note, Clear all, pencil, gear) and drag it to any place. No rows.") {
            startActivity(Intent(this, MoveActivity::class.java))
        })
        col.addView(choice("Arrange & resize", "Order of the sections, sizes, colours and what is shown.") {
            startActivity(Intent(this, ArrangeActivity::class.java))
        })
        col.addView(choice("Saved layouts", "Save the widget as it looks now under a name, and load it again later.") {
            showLayouts()
        })
        col.addView(choice("Reset", "Back to the standard widget: top row, fast-dial circles, shortcuts row and Inbox row in their normal places, no changes to colours, fonts, sizes or shapes. Your apps, contacts, shortcuts and notes are kept.") {
            AlertDialog.Builder(this)
                .setMessage("Reset the widget to the standard layout (top row, fast-dial, shortcuts row, Inbox row) and remove ALL themes, colours, fonts, sizes and moved places? Your apps, contacts, shortcuts and notes are kept. You can undo this from this menu.")
                .setPositiveButton("Reset all") { _, _ ->
                    Prefs.resetAllLook(this, Prefs.SCOPE_PENCIL)
                    // Full redraw, the same as Settings does. refresh() only updates the texts, so the old moved places stayed on screen and piled up.
                    TilesWidget.rebuildAll(applicationContext)
                    android.widget.Toast.makeText(this, "Everything is back to default", android.widget.Toast.LENGTH_SHORT).show()
                    recreate()
                }
                .setNegativeButton("Cancel", null)
                .show()
        })
        if (Prefs.hasUndo(this, Prefs.SCOPE_PENCIL)) {
            col.addView(choice("Undo last change (${Prefs.undoSteps(this, Prefs.SCOPE_PENCIL)})", "Takes back the last thing done with the pencil (a move, a resize, a colour, a Reset or a loaded layout), one step at a time. Settings has its own separate Undo. Custom background pictures are not brought back.") {
                val ok = Prefs.undoLook(this, Prefs.SCOPE_PENCIL)
                TilesWidget.rebuildAll(applicationContext)
                android.widget.Toast.makeText(this, if (ok) "Brought back" else "Nothing to undo", android.widget.Toast.LENGTH_SHORT).show()
                recreate()
            })
        }
        setContentView(col)
    }

    private fun showLayouts() {
        val names = Prefs.presetNames(this)
        val items = arrayOf("+ Save the widget as it is now...") + names.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Saved layouts")
            .setItems(items) { _, i ->
                if (i == 0) askName() else openLayout(names[i - 1])
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun askName() {
        val input = android.widget.EditText(this)
        input.hint = "Name, for example Work"
        input.setSingleLine()
        AlertDialog.Builder(this)
            .setTitle("Save this layout")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val ok = Prefs.savePreset(this, input.text.toString())
                android.widget.Toast.makeText(this, if (ok) "Saved" else "Type a name first", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openLayout(name: String) {
        AlertDialog.Builder(this)
            .setTitle(name)
            .setMessage("Load this layout? It replaces the current look and places (you can undo it from this menu).")
            .setPositiveButton("Load") { _, _ ->
                val ok = Prefs.loadPreset(this, name, Prefs.SCOPE_PENCIL)
                TilesWidget.rebuildAll(applicationContext)
                android.widget.Toast.makeText(this, if (ok) "Loaded" else "Could not load it", android.widget.Toast.LENGTH_SHORT).show()
                recreate()
            }
            .setNeutralButton("Delete") { _, _ ->
                Prefs.deletePreset(this, name)
                android.widget.Toast.makeText(this, "Deleted", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun choice(name: String, sub: String, onTap: () -> Unit): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(14), dp(12), dp(14), dp(12))
        val bg = GradientDrawable()
        bg.cornerRadius = dp(12).toFloat()
        bg.setColor(0x22888888)
        box.background = bg
        val t = TextView(this)
        t.text = name
        t.textSize = 16f
        t.setTypeface(t.typeface, Typeface.BOLD)
        box.addView(t)
        val s = TextView(this)
        s.text = sub
        s.textSize = 13f
        s.alpha = 0.7f
        box.addView(s)
        box.setOnClickListener { onTap() }
        box.layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) }
        return box
    }
}
