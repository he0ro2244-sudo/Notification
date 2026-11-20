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
        help.text = "Then add the \"Notif Tiles\" icon (not \"setup\") to your Nova home screen. " +
            "Tap it any time to open the panel. If an app is missing below, make it send a " +
            "notification once and it will appear here."
        help.textSize = 14f
        help.setPadding(0, dp(12), 0, dp(12))
        col.addView(help)

        listBox = LinearLayout(this)
        listBox.orientation = LinearLayout.VERTICAL
        col.addView(listBox)
    }

    override fun onResume() {
        super.onResume()
        status.text = if (Access.enabled(this)) "Notification access: on" else "Notification access: OFF"
        Access.rebindIfNeeded(this)
        renderList()
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

    private fun labelOf(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

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

        for ((pkg, label) in map.entries.sortedBy { it.value.lowercase() }) {
            val sw = Switch(this)
            sw.text = if (label == pkg) pkg else label
            sw.textSize = 16f
            sw.isChecked = pkg in enabled
            sw.setPadding(0, dp(10), 0, dp(10))
            sw.setOnCheckedChangeListener { _, on -> Prefs.setEnabled(this, pkg, on) }
            listBox.addView(sw)
        }
    }
}
