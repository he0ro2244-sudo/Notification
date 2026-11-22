package com.example.notiftiles

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

// Behind the "Where to?" pill: makes sure Location is on, then shows its own search box
// (keyboard already open, voice, favourites, recents, suggestions) and hands the place to Google Maps.
class DestinationActivity : Activity() {

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    private val ui = Handler(Looper.getMainLooper())

    private lateinit var input: EditText
    private lateinit var list: LinearLayout
    private val chips = ArrayList<Pair<String, TextView>>()
    private var mode = "driving"
    private var fg = Color.WHITE
    private var accent = 0xFF3B82F6.toInt()
    private var pending: Runnable? = null

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun locationOn(): Boolean =
        (getSystemService(Context.LOCATION_SERVICE) as LocationManager).isLocationEnabled

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
        if (savedInstanceState != null || locationOn() || !Prefs.autoLocation(this)) {
            showSearch()
            return
        }
        // With the one-time permission granted, Location is switched on silently (and off again after navigation).
        if (LocationAuto.turnOn(this)) {
            showSearch()
            return
        }
        // Location is off: show Google's one-tap "turn on location" prompt, then the search box.
        try {
            val req = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000L).build()
            val settings = LocationSettingsRequest.Builder().addLocationRequest(req).setAlwaysShow(true).build()
            LocationServices.getSettingsClient(this).checkLocationSettings(settings)
                .addOnSuccessListener { showSearch() }
                .addOnFailureListener { e ->
                    if (e is ResolvableApiException) {
                        try {
                            e.startResolutionForResult(this, REQ_LOCATION)
                        } catch (x: IntentSender.SendIntentException) {
                            fallback()
                        }
                    } else {
                        fallback()
                    }
                }
        } catch (e: Exception) {
            fallback()
        }
    }

    // No Google Play services prompt: open the Location switch, then the search box when you come back.
    private fun fallback() {
        try {
            startActivityForResult(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQ_LOCATION)
        } catch (e: Exception) {
            showSearch()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_LOCATION -> showSearch()
            REQ_VOICE -> {
                val said = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                if (resultCode == RESULT_OK && !said.isNullOrBlank()) go(said)
            }
        }
    }

    private fun rounded(color: Int, r: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(r).toFloat()
    }

    private fun alphaOf(color: Int, a: Int) = (a shl 24) or (color and 0xFFFFFF)

    // Option "Where to? opens the Google Maps search screen": launch Maps and let the helper tap its search bar.
    // Not while navigating, so the red "Exit navigation" button stays reachable here.
    private fun openMapsStraight(): Boolean {
        if (!Prefs.mapsSearchDirect(this) || LocationAuto.navNotification() != null) return false
        val i = packageManager.getLaunchIntentForPackage(MAPS) ?: return false
        if (RecorderTapService.enabled(this)) RecorderTapService.mapsSearchUntil = System.currentTimeMillis() + 8000L
        startActivity(i)
        finish()
        return true
    }

    private fun showSearch() {
        if (openMapsStraight()) return
        val bg = Prefs.panelBgColor(this)
        val lum = 0.299 * Color.red(bg) + 0.587 * Color.green(bg) + 0.114 * Color.blue(bg)
        fg = if (lum > 140) 0xFF111111.toInt() else 0xFFFFFFFF.toInt()
        accent = Prefs.accent(this)
        mode = Prefs.travelMode(this)

        val root = FrameLayout(this)
        root.setBackgroundColor(Prefs.scrimArgb(this))
        root.setOnClickListener { finish() }

        val sheet = LinearLayout(this)
        sheet.orientation = LinearLayout.VERTICAL
        sheet.background = Look.panelBackground(this, bg, dp(22))
        sheet.setPadding(dp(16), dp(16), dp(16), dp(12))
        sheet.isClickable = true
        val slp = FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM)
        slp.setMargins(dp(8), 0, dp(8), dp(8))
        root.addView(sheet, slp)

        // While Google Maps is navigating: one tap ends it (the same as "Exit navigation" in its notification).
        if (LocationAuto.navNotification() != null) {
            val ex = TextView(this)
            ex.text = "⛔  Exit navigation"
            ex.textSize = 16f
            ex.gravity = Gravity.CENTER
            ex.setTextColor(0xFFFFFFFF.toInt())
            ex.background = rounded(0xFFD93025.toInt(), 14)
            ex.setPadding(dp(12), dp(12), dp(12), dp(12))
            ex.setOnClickListener {
                val ok = LocationAuto.exitNavigation()
                Toast.makeText(this, if (ok) "Navigation ended" else "Open Google Maps to end navigation", Toast.LENGTH_SHORT).show()
                if (ok) finish()
            }
            val elp = LinearLayout.LayoutParams(MATCH, WRAP)
            elp.bottomMargin = dp(10)
            sheet.addView(ex, elp)
        }

        // Search row: text box + mic
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        input = EditText(this)
        input.hint = "Where to?"
        input.setHintTextColor(alphaOf(fg, 0x88))
        input.setTextColor(fg)
        input.textSize = 18f
        input.setSingleLine(true)
        input.imeOptions = EditorInfo.IME_ACTION_GO
        input.background = rounded(alphaOf(fg, 0x1F), 14)
        input.setPadding(dp(14), dp(12), dp(14), dp(12))
        input.setOnEditorActionListener { _, a, _ ->
            if (a == EditorInfo.IME_ACTION_GO) {
                go(input.text.toString())
                true
            } else false
        }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = onTyped(s?.toString().orEmpty())
        })
        row.addView(input, LinearLayout.LayoutParams(0, WRAP, 1f))
        if (Prefs.voiceButton(this)) {
            val mic = TextView(this)
            mic.text = "🎤"
            mic.textSize = 22f
            mic.gravity = Gravity.CENTER
            mic.setOnClickListener { listen() }
            val mlp = LinearLayout.LayoutParams(dp(48), dp(48))
            mlp.leftMargin = dp(8)
            row.addView(mic, mlp)
        }
        sheet.addView(row)

        // Travel mode chips
        val modes = LinearLayout(this)
        modes.orientation = LinearLayout.HORIZONTAL
        modes.setPadding(0, dp(10), 0, dp(4))
        chips.clear()
        for ((key, label) in MODES) {
            val t = TextView(this)
            t.text = label
            t.textSize = 14f
            t.gravity = Gravity.CENTER
            t.setPadding(dp(8), dp(8), dp(8), dp(8))
            t.setOnClickListener {
                mode = key
                Prefs.setTravelMode(this, key)
                paintChips()
            }
            chips.add(key to t)
            val lp = LinearLayout.LayoutParams(0, WRAP, 1f)
            lp.rightMargin = dp(6)
            modes.addView(t, lp)
        }
        sheet.addView(modes)
        paintChips()

        list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        sheet.addView(list, LinearLayout.LayoutParams(MATCH, WRAP))

        setContentView(root)
        Look.applyTree(sheet, this)
        showDefaults()
        input.requestFocus()
        input.postDelayed({
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }, 250)
    }

    private fun paintChips() {
        for ((key, t) in chips) {
            val on = key == mode
            t.background = rounded(if (on) accent else alphaOf(fg, 0x1F), 18)
            t.setTextColor(if (on) 0xFFFFFFFF.toInt() else fg)
        }
    }

    private fun row(icon: String, text: String, onClick: () -> Unit): View {
        val t = TextView(this)
        t.text = "$icon   $text"
        t.textSize = 16f
        t.setTextColor(fg)
        t.setSingleLine(true)
        t.setPadding(dp(6), dp(12), dp(6), dp(12))
        t.setOnClickListener { onClick() }
        Look.applyTree(t, this)
        return t
    }

    // Empty box: Home / Work and recent places.
    private fun showDefaults() {
        list.removeAllViews()
        list.addView(row("🗺", "Open Google Maps (its own search and suggestions)") { openMaps() })
        val home = Prefs.fav(this, "home")
        val work = Prefs.fav(this, "work")
        if (home.isNotEmpty()) list.addView(row("🏠", "Home  ·  $home") { go(home) })
        if (work.isNotEmpty()) list.addView(row("💼", "Work  ·  $work") { go(work) })
        if (Prefs.rememberPlaces(this)) {
            for (r in Prefs.recents(this).take(5)) list.addView(row("🕘", r) { go(r) })
        }
    }

    @Suppress("DEPRECATION")
    private fun onTyped(text: String) {
        pending?.let { ui.removeCallbacks(it) }
        val q = text.trim()
        if (q.isEmpty()) {
            showDefaults()
            return
        }
        list.removeAllViews()
        list.addView(row("🔎", "Search “$q”") { go(q) })
        list.addView(row("🗺", "Show “$q” in Google Maps (all matches)") { openMapsSearch(q) })
        if (!Prefs.suggestions(this) || q.length < 3) return
        val r = Runnable {
            Thread {
                val found = try {
                    if (Geocoder.isPresent()) {
                        Geocoder(this).getFromLocationName(q, 5)
                            ?.mapNotNull { it.getAddressLine(0) }
                            ?.distinct()
                            .orEmpty()
                    } else emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
                ui.post {
                    if (isFinishing || input.text.toString().trim() != q) return@post
                    for (s in found) list.addView(row("📍", s) { go(s) })
                }
            }.start()
        }
        pending = r
        ui.postDelayed(r, 400)
    }

    // Google Maps' own search results for the typed words (pick the right place there, then Directions).
    private fun openMapsSearch(q: String) {
        val enc = Uri.encode(q)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$enc")).setPackage(MAPS))
            finish()
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$enc")))
                finish()
            } catch (x: Exception) {
                Toast.makeText(this, "No maps app found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Straight into the Google Maps app, to use its own search box.
    private fun openMaps() {
        val i = packageManager.getLaunchIntentForPackage(MAPS)
        if (i != null) {
            startActivity(i)
            finish()
        } else {
            Toast.makeText(this, "Google Maps isn't installed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun listen() {
        try {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_PROMPT, "Where to?")
            startActivityForResult(i, REQ_VOICE)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input isn't available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun go(raw: String) {
        val q = raw.trim()
        if (q.isEmpty()) return
        if (Prefs.rememberPlaces(this)) Prefs.addRecent(this, q)
        val enc = Uri.encode(q)
        val uri = if (mode == "driving" && Prefs.navNow(this)) {
            Uri.parse("google.navigation:q=$enc&mode=d")
        } else {
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$enc&travelmode=$mode")
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage(MAPS))
        } catch (e: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$enc")))
            } catch (x: Exception) {
                Toast.makeText(this, "No maps app found", Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }

    companion object {
        private const val REQ_LOCATION = 3
        private const val REQ_VOICE = 4
        private const val MAPS = "com.google.android.apps.maps"
        val MODES = listOf(
            "driving" to "🚗 Drive",
            "walking" to "🚶 Walk",
            "bicycling" to "🚲 Bike",
            "transit" to "🚌 Transit"
        )
    }
}
