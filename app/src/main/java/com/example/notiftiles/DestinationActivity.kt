package com.example.notiftiles

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

// Opened by the Amman pin: type where you want to go, then Google Maps shows the route.
class DestinationActivity : Activity() {

    private lateinit var input: EditText
    private var dialog: AlertDialog? = null
    private var banner: TextView? = null

    private fun locationOn(): Boolean =
        (getSystemService(Context.LOCATION_SERVICE) as LocationManager).isLocationEnabled

    private fun openLocationSettings() {
        try {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Open Settings > Location", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        banner?.visibility = if (locationOn()) android.view.View.GONE else android.view.View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(pad, pad / 2, pad, 0)

        val b = TextView(this)
        b.text = "📍 Location is off. Tap to turn it on"
        b.textSize = 13f
        b.setTextColor(0xFFEA580C.toInt())
        b.setPadding(0, pad / 2, 0, pad / 2)
        b.setOnClickListener { openLocationSettings() }
        b.visibility = if (locationOn()) android.view.View.GONE else android.view.View.VISIBLE
        banner = b
        box.addView(b)

        input = EditText(this)
        input.hint = "Place or address (e.g. Abdali Mall)"
        input.setSingleLine(true)
        input.imeOptions = EditorInfo.IME_ACTION_GO
        input.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_GO) { go(input.text.toString()); true } else false
        }
        box.addView(input)

        val recents = recents()
        if (recents.isNotEmpty()) {
            val h = TextView(this)
            h.text = "Recent"
            h.textSize = 12f
            h.setPadding(0, pad, 0, pad / 4)
            box.addView(h)
            for (r in recents) {
                val t = TextView(this)
                t.text = "🕘  $r"
                t.textSize = 16f
                t.gravity = Gravity.CENTER_VERTICAL
                t.setPadding(0, pad / 2, 0, pad / 2)
                t.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                t.setOnClickListener { go(r) }
                box.addView(t)
            }
        }

        dialog = AlertDialog.Builder(this)
            .setTitle("Where to?")
            .setView(box)
            .setPositiveButton("Go", null)
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .create()
        dialog!!.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog!!.show()
        // Keep the dialog open if the box is empty.
        dialog!!.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { go(input.text.toString()) }
        input.requestFocus()

        // Android doesn't let an app switch Location on by itself, so ask once and jump straight to the switch.
        if (savedInstanceState == null && !locationOn()) {
            AlertDialog.Builder(this)
                .setTitle("Turn on location?")
                .setMessage("Maps needs location to start directions from where you are.")
                .setPositiveButton("Turn on") { _, _ -> openLocationSettings() }
                .setNegativeButton("Not now", null)
                .show()
        }
    }

    private fun go(raw: String) {
        val place = raw.trim()
        if (place.isEmpty()) {
            Toast.makeText(this, "Type where you want to go", Toast.LENGTH_SHORT).show()
            return
        }
        saveRecent(place)
        // Directions from your current location to the place; Maps lets you switch car/walk/transit.
        val uri = Uri.parse(
            "https://www.google.com/maps/dir/?api=1&travelmode=driving&destination=" + Uri.encode(place)
        )
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open Maps", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private fun sp() = getSharedPreferences("nt", Context.MODE_PRIVATE)

    private fun recents(): List<String> =
        (sp().getString("dest_recent", "") ?: "").split("\n").filter { it.isNotBlank() }

    private fun saveRecent(place: String) {
        val list = listOf(place) + recents().filter { !it.equals(place, ignoreCase = true) }
        sp().edit().putString("dest_recent", list.take(5).joinToString("\n")).apply()
    }

    override fun onDestroy() {
        dialog?.dismiss()
        super.onDestroy()
    }
}
