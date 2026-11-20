package com.example.notiftiles

import android.app.Activity
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.content.res.Configuration
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
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private var cBg = 0
    private var cCard = 0
    private var cField = 0
    private var cText = 0
    private var cSub = 0

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, r: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(r).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        cBg = if (dark) 0xFF1C1C20.toInt() else 0xFFF2F2F5.toInt()
        cCard = if (dark) 0xFF2B2B31.toInt() else 0xFFFFFFFF.toInt()
        cField = if (dark) 0xFF3A3A42.toInt() else 0xFFEDEDF1.toInt()
        cText = if (dark) 0xFFECECEF.toInt() else 0xFF1B1B1F.toInt()
        cSub = if (dark) 0xFFA8A8B0.toInt() else 0xFF5E5E66.toInt()

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
        sheet.background = rounded(cBg, 22)
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
        title.text = "Notifications"
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(cText)
        header.addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))
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

    override fun onResume() {
        super.onResume()
        Access.rebindIfNeeded(this)
        NotifListener.onChange = {
            runOnUiThread { if (!hasDraft()) refresh() }
        }
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
        val svc = NotifListener.instance
        if (svc == null) {
            list.addView(
                note(
                    if (Access.enabled(this)) "Connecting to notifications…"
                    else "Notification access is off. Open Notif Tiles setup and turn it on."
                )
            )
            return
        }
        val enabled = Prefs.enabled(this)
        val all = try {
            svc.activeNotifications?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList<StatusBarNotification>()
        }
        val withKids = all
            .filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
            .map { it.packageName to it.notification.group }
            .toSet()
        val items = all
            .filter { it.packageName in enabled && it.packageName != packageName }
            .filter { (it.notification.flags and Notification.FLAG_ONGOING_EVENT) == 0 }
            .filter {
                val summary = (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
                !summary || (it.packageName to it.notification.group) !in withKids
            }
            .sortedByDescending { it.postTime }

        if (items.isEmpty()) {
            list.addView(note("Nothing new from your apps."))
            return
        }
        val counts = items.groupingBy { it.packageName }.eachCount()
        for (sbn in items) list.addView(tile(sbn, counts[sbn.packageName] ?: 1))
    }

    private fun tile(sbn: StatusBarNotification, count: Int): View {
        val n = sbn.notification
        val ex = n.extras
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = (ex.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: ex.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
        val pm = packageManager
        val label = try {
            pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = rounded(cCard, 16)
        card.setPadding(dp(12), dp(10), dp(12), dp(10))
        val clp = LinearLayout.LayoutParams(MATCH, WRAP)
        clp.bottomMargin = dp(8)
        card.layoutParams = clp

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        try {
            val iv = ImageView(this)
            iv.setImageDrawable(pm.getApplicationIcon(sbn.packageName))
            head.addView(iv, LinearLayout.LayoutParams(dp(22), dp(22)))
        } catch (e: Exception) {
        }
        val name = TextView(this)
        name.text = if (count > 1) "$label · $count" else label
        name.textSize = 12f
        name.setTextColor(cSub)
        name.setPadding(dp(8), 0, 0, 0)
        head.addView(name, LinearLayout.LayoutParams(0, WRAP, 1f))
        val x = TextView(this)
        x.text = "×"
        x.textSize = 24f
        x.setTextColor(cSub)
        x.setPadding(dp(14), dp(2), dp(4), dp(2))
        x.setOnClickListener { NotifListener.instance?.cancelNotification(sbn.key) }
        head.addView(x)
        card.addView(head)

        val body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(0, dp(6), 0, dp(2))
        if (title.isNotEmpty()) {
            val t = TextView(this)
            t.text = title
            t.textSize = 15f
            t.setTypeface(null, Typeface.BOLD)
            t.setTextColor(cText)
            t.maxLines = 2
            t.ellipsize = TextUtils.TruncateAt.END
            body.addView(t)
        }
        if (text.isNotEmpty()) {
            val t = TextView(this)
            t.text = text
            t.textSize = 14f
            t.setTextColor(cText)
            t.maxLines = 4
            t.ellipsize = TextUtils.TruncateAt.END
            body.addView(t)
        }
        body.setOnClickListener { open(sbn) }
        card.addView(body)

        val actions = n.actions?.toList() ?: emptyList()
        val reply = actions.firstOrNull { a ->
            a.actionIntent != null && a.remoteInputs?.any { it.allowFreeFormInput } == true
        }

        if (reply != null) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(0, dp(8), 0, 0)
            val et = EditText(this)
            et.hint = "Reply…"
            et.setHintTextColor(cSub)
            et.setTextColor(cText)
            et.textSize = 14f
            et.maxLines = 3
            et.background = rounded(cField, 10)
            et.setPadding(dp(10), dp(8), dp(10), dp(8))
            row.addView(et, LinearLayout.LayoutParams(0, WRAP, 1f))
            val send = Button(this, null, android.R.attr.borderlessButtonStyle)
            send.text = "Send"
            send.isAllCaps = false
            send.setOnClickListener { sendReply(reply, et) }
            row.addView(send)
            card.addView(row)
        }

        val others = actions.filter {
            it !== reply && it.actionIntent != null &&
                (it.remoteInputs == null || it.remoteInputs.isEmpty())
        }.take(3)
        if (others.isNotEmpty()) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0, dp(4), 0, 0)
            for (a in others) {
                val b = Button(this, null, android.R.attr.borderlessButtonStyle)
                b.text = a.title
                b.isAllCaps = false
                b.setOnClickListener { runAction(a) }
                row.addView(b)
            }
            card.addView(row)
        }
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

    private fun clearAll() {
        val svc = NotifListener.instance ?: return
        val enabled = Prefs.enabled(this)
        val keys = try {
            svc.activeNotifications
                .filter { it.packageName in enabled && it.packageName != packageName && it.isClearable }
                .map { it.key }
                .toTypedArray()
        } catch (e: Exception) {
            return
        }
        if (keys.isNotEmpty()) svc.cancelNotifications(keys)
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
