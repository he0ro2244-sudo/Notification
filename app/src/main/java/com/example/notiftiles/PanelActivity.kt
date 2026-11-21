package com.example.notiftiles

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class PanelActivity : Activity() {

    private lateinit var list: LinearLayout
    private lateinit var badge: TextView
    private var expanded: String? = null
    private var dark = false

    private var cBg = 0
    private var cText = 0
    private var cSub = 0

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, r: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(r).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark = Feed.isDark(this)
        cBg = if (dark) 0xFF1C1C20.toInt() else 0xFFF2F2F5.toInt()
        cText = if (dark) 0xFFECECEF.toInt() else 0xFF1B1B1F.toInt()
        cSub = if (dark) 0xFFA8A8B0.toInt() else 0xFF5E5E66.toInt()
        expanded = intent?.getStringExtra("key")

        val root = FrameLayout(this)
        root.setBackgroundColor(0x66000000)
        root.setOnClickListener { finish() }
        root.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
            v.setPadding(0, b.top, 0, b.bottom)
            insets
        }

        val sheet = LinearLayout(this)
        sheet.orientation = LinearLayout.VERTICAL
        sheet.background = rounded(cBg, 24)
        sheet.setPadding(dp(12), dp(12), dp(12), dp(8))
        sheet.setOnClickListener { }
        val lp = FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM)
        lp.setMargins(dp(8), dp(8), dp(8), dp(8))
        root.addView(sheet, lp)

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(dp(4), 0, dp(4), dp(8))
        val title = TextView(this)
        title.text = "Inbox"
        title.textSize = 20f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(cText)
        header.addView(title)
        badge = TextView(this)
        badge.textSize = 12f
        badge.setTypeface(null, Typeface.BOLD)
        badge.setTextColor(0xFFFFFFFF.toInt())
        badge.background = rounded(0xFF3B82F6.toInt(), 10)
        badge.setPadding(dp(8), dp(2), dp(8), dp(2))
        val blp = LinearLayout.LayoutParams(WRAP, WRAP)
        blp.marginStart = dp(8)
        header.addView(badge, blp)
        val spacer = View(this)
        header.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))
        val clear = TextView(this)
        clear.text = "Clear all"
        clear.textSize = 14f
        clear.setTextColor(cSub)
        clear.setPadding(dp(8), dp(6), dp(8), dp(6))
        clear.setOnClickListener { clearAll() }
        header.addView(clear)
        sheet.addView(header)

        val scroll = ScrollView(this)
        list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        scroll.addView(list)
        sheet.addView(scroll, LinearLayout.LayoutParams(MATCH, WRAP))

        setContentView(root)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        expanded = intent.getStringExtra("key")
        refresh()
    }

    override fun onResume() {
        super.onResume()
        Access.rebindIfNeeded(this)
        NotifListener.onChange = { runOnUiThread { if (!hasDraft()) refresh() } }
        refresh()
    }

    override fun onPause() {
        super.onPause()
        NotifListener.onChange = null
    }

    private fun hasDraft(): Boolean {
        fun scan(v: View): Boolean = when (v) {
            is EditText -> v.text.isNotEmpty()
            is ViewGroup -> (0 until v.childCount).any { scan(v.getChildAt(it)) }
            else -> false
        }
        return scan(list)
    }

    private fun note(msg: String): TextView {
        val t = TextView(this)
        t.text = msg
        t.textSize = 14f
        t.setTextColor(cSub)
        t.setPadding(dp(8), dp(12), dp(8), dp(16))
        return t
    }

    private fun refresh() {
        list.removeAllViews()
        if (NotifListener.instance == null) {
            badge.visibility = View.GONE
            list.addView(
                note(
                    if (Access.enabled(this)) "Connecting to notifications…"
                    else "Notification access is off. Open Notif Tiles setup and turn it on."
                )
            )
            return
        }
        val items = Feed.items(this)
        badge.text = items.size.toString()
        badge.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        if (items.isEmpty()) {
            list.addView(note("Nothing new from your apps."))
            return
        }
        val counts = items.groupingBy { it.packageName }.eachCount()
        if (expanded != null && items.none { it.key == expanded }) expanded = null

        var pending: StatusBarNotification? = null
        for (sbn in items) {
            val p = pending
            if (sbn.key == expanded) {
                if (p != null) list.addView(row(p, null, counts))
                pending = null
                list.addView(expandedCard(sbn, counts[sbn.packageName] ?: 1))
            } else if (p == null) {
                pending = sbn
            } else {
                list.addView(row(p, sbn, counts))
                pending = null
            }
        }
        val last = pending
        if (last != null) list.addView(row(last, null, counts))
    }

    private fun row(a: StatusBarNotification, b: StatusBarNotification?, counts: Map<String, Int>): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        val rlp = LinearLayout.LayoutParams(MATCH, WRAP)
        rlp.bottomMargin = dp(8)
        r.layoutParams = rlp
        r.addView(compact(a, counts[a.packageName] ?: 1), cell(true))
        if (b != null) r.addView(compact(b, counts[b.packageName] ?: 1), cell(false))
        else r.addView(View(this), cell(false))
        return r
    }

    private fun cell(first: Boolean): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(0, MATCH, 1f)
        if (first) lp.marginEnd = dp(4) else lp.marginStart = dp(4)
        return lp
    }

    private fun appIcon(pkg: String, size: Int): ImageView? = try {
        val iv = ImageView(this)
        iv.setImageDrawable(packageManager.getApplicationIcon(pkg))
        iv.layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
        iv
    } catch (e: Exception) {
        null
    }

    private fun countBadge(count: Int, bg: Int, fg: Int): TextView {
        val t = TextView(this)
        t.text = count.toString()
        t.textSize = 11f
        t.setTypeface(null, Typeface.BOLD)
        t.setTextColor(bg)
        t.background = rounded(fg, 10)
        t.setPadding(dp(7), dp(1), dp(7), dp(1))
        return t
    }

    private fun compact(sbn: StatusBarNotification, count: Int): View {
        val st = Feed.style(sbn.packageName)
        val bg = st.bg(dark)
        val fg = st.fg(dark)
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = rounded(bg, 16)
        card.setPadding(dp(10), dp(10), dp(10), dp(10))
        card.minimumHeight = dp(104)

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val ic = appIcon(sbn.packageName, 22)
        if (ic != null) head.addView(ic)
        head.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        head.addView(countBadge(count, bg, fg))
        card.addView(head)

        val t = TextView(this)
        t.text = Feed.title(sbn)
        t.textSize = 14f
        t.setTypeface(null, Typeface.BOLD)
        t.setTextColor(fg)
        t.maxLines = 1
        t.ellipsize = TextUtils.TruncateAt.END
        t.setPadding(0, dp(8), 0, 0)
        card.addView(t)

        val m = TextView(this)
        m.text = Feed.text(sbn)
        m.textSize = 13f
        m.setTextColor(Feed.alpha(fg, 0xDD))
        m.maxLines = 2
        m.ellipsize = TextUtils.TruncateAt.END
        card.addView(m)

        card.setOnClickListener {
            expanded = sbn.key
            refresh()
        }
        return card
    }

    private fun pill(label: String, fg: Int, bg: Int, filled: Boolean, onClick: () -> Unit): Button {
        val b = Button(this, null, android.R.attr.borderlessButtonStyle)
        b.text = label
        b.isAllCaps = false
        b.textSize = 13f
        b.minHeight = 0
        b.minimumHeight = 0
        b.setPadding(dp(14), dp(8), dp(14), dp(8))
        if (filled) {
            b.setTextColor(bg)
            b.background = rounded(fg, 12)
        } else {
            b.setTextColor(fg)
            b.background = rounded(Feed.alpha(fg, 0x26), 12)
        }
        b.setOnClickListener { onClick() }
        return b
    }

    private fun expandedCard(sbn: StatusBarNotification, count: Int): View {
        val n = sbn.notification
        val st = Feed.style(sbn.packageName)
        val bg = st.bg(dark)
        val fg = st.fg(dark)

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = rounded(bg, 18)
        card.setPadding(dp(12), dp(10), dp(12), dp(12))
        val clp = LinearLayout.LayoutParams(MATCH, WRAP)
        clp.bottomMargin = dp(8)
        card.layoutParams = clp

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val ic = appIcon(sbn.packageName, 24)
        if (ic != null) head.addView(ic)
        val name = TextView(this)
        val label = Feed.label(this, sbn.packageName)
        name.text = if (count > 1) "$label · $count" else label
        name.textSize = 13f
        name.setTextColor(Feed.alpha(fg, 0xCC))
        name.setPadding(dp(8), 0, 0, 0)
        head.addView(name, LinearLayout.LayoutParams(0, WRAP, 1f))
        val x = TextView(this)
        x.text = "×"
        x.textSize = 26f
        x.setTextColor(fg)
        x.setPadding(dp(14), 0, dp(2), 0)
        x.setOnClickListener {
            expanded = null
            NotifListener.instance?.cancelNotification(sbn.key)
            refresh()
        }
        head.addView(x)
        head.setOnClickListener {
            expanded = null
            refresh()
        }
        card.addView(head)

        val tt = Feed.title(sbn)
        if (tt.isNotEmpty()) {
            val t = TextView(this)
            t.text = tt
            t.textSize = 16f
            t.setTypeface(null, Typeface.BOLD)
            t.setTextColor(fg)
            t.setPadding(0, dp(8), 0, 0)
            card.addView(t)
        }
        val tx = Feed.text(sbn)
        if (tx.isNotEmpty()) {
            val m = TextView(this)
            m.text = tx
            m.textSize = 14f
            m.setTextColor(Feed.alpha(fg, 0xEE))
            m.maxLines = 8
            m.ellipsize = TextUtils.TruncateAt.END
            m.setPadding(0, dp(2), 0, 0)
            card.addView(m)
        }

        val actions = n.actions?.toList() ?: emptyList()
        val reply = actions.firstOrNull { a ->
            a.actionIntent != null && a.remoteInputs?.any { it.allowFreeFormInput } == true
        }

        if (reply != null) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(0, dp(10), 0, 0)
            val et = EditText(this)
            et.hint = "Reply…"
            et.setHintTextColor(Feed.alpha(fg, 0x99))
            et.setTextColor(fg)
            et.textSize = 14f
            et.maxLines = 3
            et.background = rounded(if (dark) 0x33FFFFFF else 0xAAFFFFFF.toInt(), 12)
            et.setPadding(dp(12), dp(10), dp(12), dp(10))
            row.addView(et, LinearLayout.LayoutParams(0, WRAP, 1f))
            val send = pill("Send", fg, bg, true) { sendReply(reply, et) }
            val slp = LinearLayout.LayoutParams(WRAP, WRAP)
            slp.marginStart = dp(8)
            row.addView(send, slp)
            card.addView(row)
        }

        val others = actions.filter {
            it !== reply && it.actionIntent != null &&
                (it.remoteInputs == null || it.remoteInputs.isEmpty())
        }.take(3)

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.setPadding(0, dp(10), 0, 0)
        val blp = { LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = dp(6) } }
        for (a in others) {
            bar.addView(pill(a.title.toString(), fg, bg, false) { runAction(a) }, blp())
        }
        bar.addView(pill("Open", fg, bg, false) { open(sbn) }, blp())
        if (count > 1) {
            bar.addView(pill("Clear $count", fg, bg, false) { clearApp(sbn.packageName) }, blp())
        }
        card.addView(bar)
        return card
    }

    private fun sendReply(a: Notification.Action, et: EditText) {
        val msg = et.text.toString().trim()
        if (msg.isEmpty()) {
            toast("Type a reply first")
            return
        }
        val inputs = a.remoteInputs ?: return
        val results = Bundle()
        for (ri in inputs) results.putCharSequence(ri.resultKey, msg)
        val intent = Intent()
        RemoteInput.addResultsToIntent(inputs, intent, results)
        RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_FREE_FORM_INPUT)
        try {
            a.actionIntent.send(this, 0, intent)
            et.setText("")
            toast("Sent")
        } catch (e: PendingIntent.CanceledException) {
            toast("Couldn't send. Open the app to reply.")
        }
    }

    private fun runAction(a: Notification.Action) {
        try {
            a.actionIntent.send()
            if (a.actionIntent.isActivity) finish()
        } catch (e: PendingIntent.CanceledException) {
            toast("Couldn't run that action")
        }
    }

    private fun open(sbn: StatusBarNotification) {
        try {
            val pi = sbn.notification.contentIntent
            if (pi != null) {
                pi.send()
                if ((sbn.notification.flags and Notification.FLAG_AUTO_CANCEL) != 0) {
                    NotifListener.instance?.cancelNotification(sbn.key)
                }
            } else {
                packageManager.getLaunchIntentForPackage(sbn.packageName)?.let { startActivity(it) }
            }
        } catch (e: Exception) {
        }
        finish()
    }

    private fun clearApp(pkg: String) {
        val svc = NotifListener.instance ?: return
        val keys = Feed.items(this).filter { it.packageName == pkg && it.isClearable }
            .map { it.key }.toTypedArray()
        expanded = null
        if (keys.isNotEmpty()) svc.cancelNotifications(keys)
    }

    private fun clearAll() {
        val svc = NotifListener.instance ?: return
        val keys = Feed.items(this).filter { it.isClearable }.map { it.key }.toTypedArray()
        if (keys.isNotEmpty()) svc.cancelNotifications(keys)
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
