package com.example.notiftiles

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** The in-app user manual (res/raw/user_manual.md), drawn as plain text rows at the bottom of Settings, with a search box. */
object Manual {
    /** One row of the manual: its text, the heading group it belongs to, and whether it is a heading itself. */
    private class Row(val view: TextView, val group: Int, val src: String, val heading: Boolean)

    fun addTo(ctx: Context, col: LinearLayout) {
        val dm = ctx.resources.displayMetrics.density
        fun dp(v: Int) = Math.round(v * dm)
        val text = try {
            ctx.resources.openRawResource(R.raw.user_manual).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "The manual could not be loaded."
        }
        val rows = ArrayList<Row>()
        var group = 0

        fun add(src: String, shown: CharSequence, size: Float, bold: Boolean, top: Int, left: Int, alpha: Float, heading: Boolean) {
            val tv = TextView(ctx)
            tv.text = shown
            tv.textSize = size
            if (bold) tv.setTypeface(null, Typeface.BOLD)
            tv.alpha = alpha
            tv.setTextIsSelectable(true)
            tv.setPadding(dp(left), dp(top), 0, dp(2))
            col.addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            rows.add(Row(tv, group, src, heading))
        }

        // Search box: shows only the lines that match (with their heading)
        val search = EditText(ctx)
        search.hint = "Search the manual"
        search.setSingleLine(true)
        col.addView(search, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(8) })

        for (raw in text.lines()) {
            val line = raw.trimEnd()
            when {
                line.isBlank() -> {}
                line.startsWith("### ") -> { group++; add(line, line.drop(4), 15f, true, 12, 0, 1f, true) }
                line.startsWith("## ") -> { group++; add(line, line.drop(3), 17f, true, 18, 0, 1f, true) }
                line.startsWith("# ") -> { group++; add(line, line.drop(2), 20f, true, 6, 0, 1f, true) }
                line.startsWith("- ") -> add(line, bold("•  " + line.drop(2)), 14f, false, 2, 8, 1f, false)
                Regex("^\\d+\\. .*").matches(line) -> add(line, bold(line), 14f, false, 2, 8, 1f, false)
                else -> add(line, bold(line), 14f, false, 4, 0, 0.9f, false)
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = filter(rows, s?.toString()?.trim() ?: "")
        })
    }

    /** Shows the rows that match [query]; a heading that matches shows its whole section, and a matching line shows its heading. */
    private fun filter(rows: List<Row>, query: String) {
        val q = query.lowercase()
        if (q.isEmpty()) {
            for (r in rows) r.view.visibility = View.VISIBLE
            return
        }
        val headHit = HashSet<Int>()
        val lineHit = HashSet<Int>()
        for (r in rows) {
            if (r.src.lowercase().contains(q)) {
                if (r.heading) headHit.add(r.group) else lineHit.add(r.group)
            }
        }
        for (r in rows) {
            val show = if (r.heading) {
                r.group in headHit || r.group in lineHit
            } else {
                r.group in headHit || r.src.lowercase().contains(q)
            }
            r.view.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    /** **bold** pieces of a line shown in bold. */
    private fun bold(s: String): CharSequence {
        val sb = SpannableStringBuilder()
        val parts = s.split("**")
        for ((i, p) in parts.withIndex()) {
            val st = sb.length
            sb.append(p)
            if (i % 2 == 1 && p.isNotEmpty()) sb.setSpan(StyleSpan(Typeface.BOLD), st, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sb
    }
}
