package com.example.notiftiles

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** What the pencil on the widget does: Move (drag items to another place) or Arrange & resize (sections, sizes, colours). */
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
        setContentView(col)
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
