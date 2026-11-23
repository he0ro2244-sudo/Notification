package com.example.notiftiles

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** The in-app user manual (res/raw/user_manual.md), drawn as plain text rows at the bottom of Settings. */
object Manual {
    fun addTo(ctx: Context, col: LinearLayout) {
        val dm = ctx.resources.displayMetrics.density
        fun dp(v: Int) = Math.round(v * dm)
        val text = try {
            ctx.resources.openRawResource(R.raw.user_manual).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "The manual could not be loaded."
        }
        fun add(t: CharSequence, size: Float, bold: Boolean, top: Int, left: Int = 0, alpha: Float = 1f) {
            val tv = TextView(ctx)
            tv.text = t
            tv.textSize = size
            if (bold) tv.setTypeface(null, Typeface.BOLD)
            tv.alpha = alpha
            tv.setTextIsSelectable(true)
            tv.setPadding(dp(left), dp(top), 0, dp(2))
            col.addView(tv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        for (raw in text.lines()) {
            val line = raw.trimEnd()
            when {
                line.isBlank() -> {}
                line.startsWith("### ") -> add(line.drop(4), 15f, true, 12)
                line.startsWith("## ") -> add(line.drop(3), 17f, true, 18)
                line.startsWith("# ") -> add(line.drop(2), 20f, true, 6)
                line.startsWith("- ") -> add(bold("•  " + line.drop(2)), 14f, false, 2, 8)
                Regex("^\\d+\\. .*").matches(line) -> add(bold(line), 14f, false, 2, 8)
                else -> add(bold(line), 14f, false, 4, 0, 0.9f)
            }
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
