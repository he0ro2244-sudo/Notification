package com.example.notiftiles

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var listBox: LinearLayout
    private lateinit var dialBox: LinearLayout

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(16), dp(16), dp(24))
        scroll.addView(col)
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            val s = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(s.left, s.top, s.right, s.bottom)
            insets
        }
        setContentView(scroll)

        val title = TextView(this)
        title.text = "Notif Tiles setup"
        title.textSize = 22f
        col.addView(title)

        status = TextView(this)
        status.textSize = 15f
        status.setPadding(0, dp(8), 0, dp(8))
        col.addView(status)

        col.addView(btn("1. Grant notification access") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        col.addView(btn("2. Battery settings (set Notif Tiles to unrestricted)") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })
        col.addView(btn("3. Turn on my usual apps") {
            Prefs.enableAll(this, Prefs.USUAL)
            renderList()
            Toast.makeText(this, "Turned on", Toast.LENGTH_SHORT).show()
        })
        col.addView(btn("Open panel") {
            startActivity(Intent(this, PanelActivity::class.java))
        })

        val help = TextView(this)
        help.text = "Home screen: long-press Nova > Widgets > Notif Tiles, and drop the widget " +
            "on an empty page. Tapping a tile opens the panel with reply and action buttons. " +
            "You can also add the \"Notif Tiles\" app icon (not \"setup\") as a shortcut.\n\n" +
            "Your apps are listed first below. If an app is missing, make it send a " +
            "notification once and it will appear."
        help.textSize = 14f
        help.setPadding(0, dp(12), 0, dp(12))
        col.addView(help)

        col.addView(heading("Fast dial (5 circles on the widget)"))
        dialBox = LinearLayout(this)
        dialBox.orientation = LinearLayout.VERTICAL
        col.addView(dialBox)

        listBox = LinearLayout(this)
        listBox.orientation = LinearLayout.VERTICAL
        col.addView(listBox)
    }

    override fun onResume() {
        super.onResume()
        status.text = if (Access.enabled(this)) "Notification access: on" else "Notification access: OFF"
        Access.rebindIfNeeded(this)
        renderDial()
        renderList()
    }

    private fun renderDial() {
        dialBox.removeAllViews()
        for (i in 0 until Prefs.DIAL_SLOTS) {
            val name = Prefs.dialName(this, i)
            val num = Prefs.dialNumber(this, i)
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = android.view.Gravity.CENTER_VERTICAL
            val t = TextView(this)
            t.text = "${i + 1}. " + if (num.isEmpty()) "empty" else "$name  $num"
            t.textSize = 15f
            t.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            row.addView(t)
            val set = Button(this)
            set.text = if (num.isEmpty()) "Set" else "Change"
            set.isAllCaps = false
            set.setOnClickListener {
                startActivity(
                    Intent(this, DialActivity::class.java).putExtra("slot", i).putExtra("pick", true)
                )
            }
            row.addView(set)
            if (num.isNotEmpty()) {
                val clr = Button(this)
                clr.text = "Clear"
                clr.isAllCaps = false
                clr.setOnClickListener {
                    Prefs.clearDial(this, i)
                    TilesWidget.refresh(applicationContext)
                    renderDial()
                }
                row.addView(clr)
            }
            dialBox.addView(row)
        }
    }

    private fun btn(label: String, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = label
        b.isAllCaps = false
        b.setOnClickListener { onClick() }
        b.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return b
    }

    private fun heading(t: String): TextView {
        val h = TextView(this)
        h.text = t
        h.textSize = 13f
        h.setPadding(0, dp(12), 0, dp(4))
        return h
    }

    private fun labelOf(pkg: String): String = Feed.label(this, pkg)

    private fun renderList() {
        listBox.removeAllViews()
        val pm = packageManager
        val enabled = Prefs.enabled(this)
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val map = HashMap<String, String>()
        pm.queryIntentActivities(q, 0).forEach {
            map[it.activityInfo.packageName] = it.loadLabel(pm).toString()
        }
        (Prefs.seen(this) + enabled).forEach { if (!map.containsKey(it)) map[it] = labelOf(it) }
        map.remove(packageName)

        val sorted = map.entries.sortedWith(
            compareByDescending<Map.Entry<String, String>> { it.key in enabled }
                .thenBy { it.value.lowercase() }
        )
        var sectionShown = ""
        for ((pkg, label) in sorted) {
            val on = pkg in enabled
            val section = if (on) "Your apps" else "Other apps"
            if (section != sectionShown) {
                listBox.addView(heading(section))
                sectionShown = section
            }
            val sw = Switch(this)
            sw.text = if (label == pkg) pkg else label
            sw.textSize = 16f
            sw.isChecked = on
            sw.setPadding(0, dp(10), 0, dp(10))
            sw.setOnCheckedChangeListener { _, checked -> Prefs.setEnabled(this, pkg, checked) }
            listBox.addView(sw)
        }
    }
}
