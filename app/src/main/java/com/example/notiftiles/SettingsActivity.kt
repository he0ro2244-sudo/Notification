package com.example.notiftiles

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

// Look & colors: control panel background, the shadow (dim) behind it, and the widget background.
class SettingsActivity : Activity() {

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private lateinit var scroll: ScrollView
    private lateinit var col: LinearLayout
    private var scrimView: View? = null
    private var sheetView: View? = null
    private var widgetView: View? = null

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun c(v: Long) = v.toInt()

    private val bgColors = intArrayOf(
        0, c(0xFF000000), c(0xFF1C1C20), c(0xFFFFFFFF), c(0xFFF2F2F5), c(0xFF1E3A8A),
        c(0xFF0F766E), c(0xFF166534), c(0xFF7C3AED), c(0xFFB91C1C), c(0xFFEA580C), c(0xFFDB2777)
    )
    private val borderColors = intArrayOf(
        0, c(0xFFFFFFFF), c(0xFF000000), c(0xFF3B82F6), c(0xFF22D3EE), c(0xFF4ADE80),
        c(0xFFFACC15), c(0xFFFB923C), c(0xFFEF4444), c(0xFFDB2777), c(0xFFA78BFA)
    )
    private val scrimColors = intArrayOf(
        c(0xFF000000), c(0xFF1C1C20), c(0xFF1E3A8A), c(0xFF7C3AED), c(0xFF0F766E), c(0xFF3E2723)
    )

    private val accents = intArrayOf(
        c(0xFF3B82F6), c(0xFF22D3EE), c(0xFF4ADE80), c(0xFFFACC15),
        c(0xFFFB923C), c(0xFFEF4444), c(0xFFDB2777), c(0xFFA78BFA)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scroll = ScrollView(this)
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars())
            v.setPadding(0, b.top, 0, b.bottom)
            insets
        }
        col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(16), dp(16), dp(32))
        scroll.addView(col)
        setContentView(scroll)
        build()
    }

    override fun onRestart() {
        super.onRestart()
        rebuild()
    }

    private fun rebuild() {
        val y = scroll.scrollY
        col.removeAllViews()
        build()
        scroll.post { scroll.scrollTo(0, y) }
    }

    private fun refreshWeatherNow() {
        val app = applicationContext
        Thread {
            Weather.fetch(app)
            TilesWidget.refresh(app)
        }.start()
    }

    private fun applyLook() {
        updatePreviews()
        TilesWidget.refresh(applicationContext)
    }

    private fun title(t: String, size: Float, topPad: Int) = TextView(this).apply {
        text = t
        textSize = size
        setTypeface(null, Typeface.BOLD)
        setPadding(0, dp(topPad), 0, dp(4))
    }

    private val openSections = mutableSetOf("Themes & accent")

    private fun section(name: String, body: () -> Unit) {
        val open = name in openSections
        val h = TextView(this)
        h.text = (if (open) "▾  " else "▸  ") + name
        h.textSize = 17f
        h.setTypeface(null, Typeface.BOLD)
        h.setPadding(dp(14), dp(14), dp(14), dp(14))
        h.background = rounded(0x1F808080, 12)
        h.setOnClickListener {
            if (open) openSections.remove(name) else openSections.add(name)
            rebuild()
        }
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = dp(12)
        col.addView(h, lp)
        if (open) body()
    }

    private fun build() {
        col.addView(title("Look & colors", 22f, 0))
        try {
            val vn = packageManager.getPackageInfo(packageName, 0).versionName
            col.addView(hint("Notif Tiles version $vn"))
        } catch (e: Exception) {
        }

        col.addView(hint("Tap a group to open or close it. Everything is still here, just grouped."))

        section("Themes & accent") {
        // ---- Themes
        col.addView(title("Themes", 16f, 12))
        col.addView(themeRow())

        // ---- Accent colour
        col.addView(title("Accent colour (buttons, badge)", 16f, 20))
        col.addView(swatchRow(accents, Prefs.accent(this)) { Prefs.setAccent(this, it); applyLook(); rebuild() })
        col.addView(title("Icons & Clear all colour (mic, settings, refresh)", 16f, 20))
        col.addView(hint("Themes set this too. Auto = the normal grey. A colour picked for one part under Text colour per part still wins."))
        col.addView(swatchRow(intArrayOf(0) + accents, Prefs.iconColor(this)) { Prefs.setIconColor(this, it); applyLook(); rebuild() })

        // ---- Automatic day / night theme
        col.addView(title("Auto theme (day / night)", 16f, 20))
        col.addView(hint("Switches theme by itself at the times below. Uses one small alarm per change, nothing runs in the background."))
        col.addView(toggleRaw("Switch theme automatically", Prefs.autoTheme(this)) {
            Prefs.setAutoTheme(this, it); Prefs.setAutoLast(this, -1); Extras.tick(this); applyLook(); rebuild()
        })
        if (Prefs.autoTheme(this)) {
            val names = Themes.LIST.mapIndexed { i, t -> i.toString() to t.name }
            col.addView(hint("Day theme"))
            col.addView(choiceRow(names, Prefs.autoDayTheme(this).toString(), false) {
                Prefs.setAutoDayTheme(this, it.toInt()); Prefs.setAutoLast(this, -1); Extras.tick(this); applyLook(); rebuild()
            })
            col.addView(hint("Night theme"))
            col.addView(choiceRow(names, Prefs.autoNightTheme(this).toString(), false) {
                Prefs.setAutoNightTheme(this, it.toInt()); Prefs.setAutoLast(this, -1); Extras.tick(this); applyLook(); rebuild()
            })
            col.addView(timeRow("Day starts", Prefs.autoDayStart(this)) {
                Prefs.setAutoDayStart(this, it); Prefs.setAutoLast(this, -1); Extras.tick(this); applyLook(); rebuild()
            })
            col.addView(timeRow("Night starts", Prefs.autoNightStart(this)) {
                Prefs.setAutoNightStart(this, it); Prefs.setAutoLast(this, -1); Extras.tick(this); applyLook(); rebuild()
            })
        }

        }

        section("Backgrounds & pictures") {
        // ---- Control panel background
        col.addView(title("Control panel background", 16f, 16))
        col.addView(panelPreview())
        col.addView(swatchRow(bgColors, Prefs.panelBg(this)) { Prefs.setPanelBg(this, it); applyLook(); rebuild() })
        col.addView(customHex { Prefs.setPanelBg(this, it); applyLook(); rebuild() })

        // ---- Shadow behind the panel
        col.addView(title("Shadow behind the panel", 16f, 20))
        col.addView(swatchRow(scrimColors, Prefs.scrimColor(this)) { Prefs.setScrimColor(this, it); applyLook(); rebuild() })
        col.addView(seek("Strength", Prefs.scrimAlpha(this)) { Prefs.setScrimAlpha(this, it); updatePreviews() })

        // ---- Widget background
        col.addView(title("Home-screen widget background", 16f, 20))
        col.addView(widgetPreview())
        col.addView(swatchRow(bgColors, Prefs.widgetBg(this)) { Prefs.setWidgetBg(this, it); applyLook(); rebuild() })
        col.addView(customHex { Prefs.setWidgetBg(this, it); applyLook(); rebuild() })
        col.addView(seek("Opacity", Prefs.widgetAlpha(this)) { Prefs.setWidgetAlpha(this, it); updatePreviews() })

        // ---- Background pictures
        col.addView(title("Background pictures", 16f, 20))
        col.addView(hint("Pick a photo from your gallery. Lower the strength to let the colour show through."))
        col.addView(imageRow("Widget background", "widget"))
        col.addView(imageRow("Fast-dial background", "dial"))
        col.addView(title("Fast-dial strip colour (behind the circles)", 14f, 12))
        col.addView(swatchRow(bgColors, Prefs.dialBg(this)) { Prefs.setDialBg(this, it); applyLook(); rebuild() })
        col.addView(seekRange("Strip opacity", Prefs.dialBgAlpha(this), 0, 100, "%") { Prefs.setDialBgAlpha(this, it) })
        col.addView(imageRow("Panel background", "panel"))

        }

        section("Text, fonts & text colours") {
        // ---- Message time stamp
        col.addView(title("Message time stamp", 16f, 20))
        col.addView(hint("The [time] in front of each message line. Smart: today shows only the time, yesterday shows Yesterday, older shows the date. It uses the same font, size and colour as the message text below."))
        col.addView(choiceRow(listOf("0" to "Smart", "1" to "Date + time", "2" to "Time only", "3" to "None"), Prefs.stampStyle(this).toString(), false) {
            Prefs.setStampStyle(this, it.toInt()); applyLook(); rebuild()
        })
        // ---- Fonts
        col.addView(title("Font", 16f, 20))
        col.addView(hint("Applies to the widget, the panel and the Where to? box."))
        col.addView(choiceRow(Look.FONTS, Prefs.fontFamily(this), true) {
            Prefs.setFontFamily(this, it); applyLook(); rebuild()
        })
        col.addView(hint("Weight"))
        col.addView(choiceRow(listOf("0" to "As designed", "1" to "All bold", "2" to "No bold"), Prefs.boldMode(this).toString(), false) {
            Prefs.setBoldMode(this, it.toInt()); applyLook(); rebuild()
        })
        col.addView(toggleRaw("Italic", Prefs.italic(this)) { Prefs.setItalic(this, it); applyLook(); updateFontPreview() })
        col.addView(hint("Letter case (widget text)"))
        col.addView(choiceRow(listOf("0" to "As typed", "1" to "UPPERCASE", "2" to "lowercase", "3" to "Each Word"), Prefs.textCase(this).toString(), false) {
            Prefs.setTextCase(this, it.toInt()); applyLook(); rebuild()
        })
        col.addView(seekRange("Text size", Prefs.fontScale(this), 70, 160, "%") { Prefs.setFontScale(this, it); updateFontPreview() })
        val sample = TextView(this)
        sample.text = "Aa  12:45  The quick brown fox"
        sample.textSize = 18f
        sample.setPadding(0, dp(8), 0, dp(8))
        Look.applyTree(sample, this)
        fontPreview = sample
        col.addView(sample)

        col.addView(title("Text size per part of the panel", 14f, 12))
        col.addView(hint("On top of the overall text size above. 100% = as designed."))
        for ((g, label) in Prefs.PANEL_GROUPS) {
            col.addView(seekRange(label, Prefs.grpSize(this, g), 60, 220, "%") { Prefs.setGrpSize(this, g, it) })
        }

        // ---- Wording of the widget
        col.addView(title("Widget wording", 16f, 20))
        col.addView(hint("Change the text shown when there are no messages, and the word before the weather update time. Empty + Save = original wording. Colours: see Text colour per part below."))
        col.addView(toggleRaw("Show the \"Nothing new\" message in the widget", Prefs.showEmptyText(this)) { Prefs.setShowEmptyText(this, it); applyLook() })
        col.addView(textPref("Empty message text", Prefs.emptyText(this)) { Prefs.setEmptyText(this, it) })
        col.addView(textPref("Update word (before the time)", Prefs.updatedLabel(this)) { Prefs.setUpdatedLabel(this, it) })

        // ---- Colours of text and shapes
        col.addView(title("Text colour (widget)", 16f, 20))
        col.addView(swatchRow(textColors, Prefs.textColor(this)) { Prefs.setTextColor(this, it); applyLook(); rebuild() })
        col.addView(title("Text colour per part", 14f, 12))
        col.addView(hint("Tap a row to pick its colour (big palette, sliders or a hex code). The grey Auto dot follows the overall text colour above; the Auto button in the picker puts a part back."))
        for ((k, label) in Prefs.WIDGET_PARTS) {
            col.addView(colorRow(label, Prefs.partColor(this, k)) { Prefs.setPartColor(this, k, it); applyLook(); rebuild() })
        }
        for ((g, label) in Prefs.PANEL_GROUPS) {
            col.addView(colorRow("Panel: $label", Prefs.grpColor(this, g)) { Prefs.setGrpColor(this, g, it); applyLook(); rebuild() })
        }
        }

        section("Shapes, circles & borders") {
        // ---- Sizes and shape
        col.addView(title("Sizes & shape", 16f, 20))
        col.addView(choiceRow(Look.SHAPES, Prefs.shape(this).toString(), false) {
            Prefs.setShape(this, it.toInt()); applyLook(); rebuild()
        })
        col.addView(seekRange("Clock / weather / date circles", Prefs.topSize(this), 36, 72, " dp") { Prefs.setTopSize(this, it) })
        col.addView(seekRange("Widget settings icon", Prefs.gearSize(this), 14, 40, " dp") { Prefs.setGearSize(this, it) })
        col.addView(toggleRaw("Refresh icon (centre of the widget)", Prefs.refreshShow(this)) { Prefs.setRefreshShow(this, it); applyLook() })
        col.addView(seekRange("Widget refresh icon", Prefs.refreshSize(this), 10, 32, " dp") { Prefs.setRefreshSize(this, it) })
        col.addView(seekRange("Widget mic icon", Prefs.micSize(this), 14, 40, " dp") { Prefs.setMicSize(this, it) })
        col.addView(toggleRaw("3D icons (widget mic, settings, refresh)", Prefs.icons3d(this)) { Prefs.setIcons3d(this, it); applyLook() })
        col.addView(seekRange("Fast-dial circles", Prefs.dialSize(this), 30, 64, " dp") { Prefs.setDialSize(this, it) })
        col.addView(toggleRaw("Names under fast-dial circles", Prefs.dialNames(this)) { Prefs.setDialNames(this, it); applyLook() })

        col.addView(title("Circle colour (clock, weather, date, Where to?)", 14f, 12))
        col.addView(swatchRow(bgColors, Prefs.circleColor(this)) { Prefs.setCircleColor(this, it); applyLook(); rebuild() })
        col.addView(title("Fast-dial circle colour", 14f, 12))
        col.addView(swatchRow(bgColors, Prefs.dialCircle(this)) { Prefs.setDialCircle(this, it); applyLook(); rebuild() })

        // ---- Borders
        col.addView(title("Borders", 16f, 20))
        col.addView(hint("A line around the panel and the message tiles. Thickness 0 = no border."))
        col.addView(seekRange("Thickness", Prefs.borderWidth(this), 0, 6, " dp") { Prefs.setBorderWidth(this, it) })
        col.addView(hint("Colour (first one = automatic)"))
        col.addView(swatchRow(borderColors, Prefs.borderColor(this)) { Prefs.setBorderColor(this, it); rebuild() })
        col.addView(toggleRaw("Border around the panel", Prefs.borderPanel(this)) { Prefs.setBorderPanel(this, it) })
        col.addView(toggleRaw("Border around message tiles", Prefs.borderTiles(this)) { Prefs.setBorderTiles(this, it) })

        }

        section("Widget layout, clock & weather") {
        // ---- Arrange the widget
        col.addView(title("Arrange the widget", 16f, 20))
        col.addView(hint("Use ▲ ▼ to move the circles. Switch an item off to hide it."))
        val labels = mapOf("clock" to "Clock", "weather" to "Weather", "date" to "Date")
        val order = Prefs.order(this)
        for ((i, key) in order.withIndex()) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val sw = Switch(this)
            sw.text = "${i + 1}.  ${labels[key]}"
            sw.isChecked = Prefs.show(this, key)
            sw.setOnCheckedChangeListener { _, v -> Prefs.setShow(this, key, v); applyLook() }
            row.addView(sw, LinearLayout.LayoutParams(0, WRAP, 1f))
            row.addView(moveButton("▲", i > 0) { swap(order, i, i - 1) })
            row.addView(moveButton("▼", i < order.size - 1) { swap(order, i, i + 1) })
            col.addView(row)
        }
        col.addView(toggle("Maps search box", "maps"))
        col.addView(toggleRaw("Maps search box at the start (left)", Prefs.pillFirst(this)) { Prefs.setPillFirst(this, it); applyLook() })
        col.addView(toggle("Fast-dial circles", "dial"))

        // ---- Clock, date, weather
        col.addView(title("Clock, date & weather", 16f, 20))
        col.addView(hint("Clock"))
        col.addView(choiceRow(listOf("0" to "Phone setting", "1" to "12-hour", "2" to "24-hour"), Prefs.clockFmt(this).toString(), false) {
            Prefs.setClockFmt(this, it.toInt()); applyLook(); rebuild()
        })
        col.addView(hint("Date"))
        col.addView(choiceRow(Prefs.DATE_FORMATS.mapIndexed { i, p -> i.toString() to p.second }, Prefs.dateFmt(this).toString(), false) {
            Prefs.setDateFmt(this, it.toInt()); applyLook(); rebuild()
        })
        col.addView(hint("Weather city: ${Weather.city(this)}"))
        col.addView(cityRow())
        col.addView(choiceRow(listOf("c" to "°C", "f" to "°F"), Weather.unit(this), false) {
            Weather.setUnit(this, it); applyLook(); rebuild()
        })
        col.addView(hint("Weather mode"))
        col.addView(choiceRow(listOf("both" to "Both (recommended)", "app" to "Phone app only", "online" to "Online only"), Weather.mode(this), false) {
            Weather.setMode(this, it); applyLook(); rebuild()
        })
        col.addView(hint(
            "Both: whenever you copy your phone weather app's number (tap the weather circle on the widget: your weather app opens and the number is copied), the widget shows that exact number. The small refresh icon never opens the app, it only refreshes the online number. " +
                "Afterwards it keeps updating itself from the online source below and adds the difference it learned, so it stays close without opening the app. " +
                "If the phone app was never copied (or the helper is OFF) it uses the online number; if the online service cannot be reached it keeps the last number. Phone app only: always the last number copied. Online only: ignores the phone app. Copying needs the Accessibility helper. " +
                (if (RecorderTapService.enabled(this)) "Helper is ON. " else "Helper is OFF. ") +
                (if (Weather.appTime(this) > 0L) "Last copied: " + Weather.appTemp(this) + "° at " +
                    java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(Weather.appTime(this))) + ". " else "Nothing copied yet. ") +
                (if (Weather.gapKnown(this)) "Learned difference: " + (if (Weather.gap(this) > 0) "+" else "") + Weather.gap(this) + "°." else "")
        ))
        val accBtn2 = Button(this)
        accBtn2.text = "Open Accessibility settings"
        accBtn2.isAllCaps = false
        accBtn2.setOnClickListener {
            try {
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
            } catch (e: Exception) {
            }
        }
        col.addView(accBtn2)
        col.addView(hint("Weather source (try another if the number differs from your phone's weather app)"))
        col.addView(choiceRow(Weather.SOURCES, Weather.source(this), false) {
            Weather.setSource(this, it); refreshWeatherNow(); rebuild()
        })
        val cmp = Button(this)
        cmp.text = "Compare all sources now"
        cmp.isAllCaps = false
        cmp.setOnClickListener {
            Toast.makeText(this, "Checking every source…", Toast.LENGTH_SHORT).show()
            Thread {
                val res = Weather.compare(applicationContext)
                runOnUiThread {
                    val labels = res.map { (_, name, t) -> name + ":  " + (if (t != null) "$t°" else "no data") }.toTypedArray()
                    AlertDialog.Builder(this)
                        .setTitle("Pick the one that matches your phone")
                        .setItems(labels) { _, i ->
                            Weather.setSource(this, res[i].first)
                            refreshWeatherNow()
                            rebuild()
                        }
                        .setNegativeButton("Close", null)
                        .show()
                }
            }.start()
        }
        col.addView(cmp)
        col.addView(seekRange("Temperature adjust", Weather.offset(this), -6, 6, "°") { Weather.setOffset(this, it); applyLook() })
        col.addView(toggleRaw("Show \"Updated\" time in the widget", Weather.showUpdated(this)) { Weather.setShowUpdated(this, it); applyLook() })

        }

        section("Fast-dial contacts") {
        // ---- Fast dial contacts
        col.addView(title("Fast-dial contacts", 16f, 20))
        col.addView(toggleRaw("Ask before calling (Call / Change / Remove)", Prefs.dialAsk(this)) { Prefs.setDialAsk(this, it) })
        col.addView(hint("Put a wrong person on a circle? Change or clear it here. With the switch above on, tapping a circle on the widget also offers Change and Remove."))
        for (i in 0 until Prefs.DIAL_SLOTS) {
            val nm = Prefs.dialName(this, i)
            val num = Prefs.dialNumber(this, i)
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val label = TextView(this)
            label.text = "${i + 1}.  " + (if (num.isEmpty()) "Empty" else if (nm.isNotBlank()) nm else num)
            label.textSize = 15f
            row.addView(label, LinearLayout.LayoutParams(0, WRAP, 1f))
            val ch = Button(this)
            ch.isAllCaps = false
            ch.text = if (num.isEmpty()) "Pick" else "Change"
            ch.setOnClickListener {
                startActivity(Intent(this, DialActivity::class.java).putExtra("slot", i).putExtra("pick", true))
            }
            row.addView(ch)
            if (num.isNotEmpty()) {
                val cl = Button(this)
                cl.isAllCaps = false
                cl.text = "Clear"
                cl.setOnClickListener {
                    Prefs.clearDial(this, i)
                    applyLook()
                    rebuild()
                }
                row.addView(cl)
            }
            col.addView(row)
            col.addView(hint("Circle ${i + 1}: background colour"))
            col.addView(swatchRow(bgColors, Prefs.dialCircleOf(this, i)) { Prefs.setDialCircleOf(this, i, it); applyLook(); rebuild() })
            col.addView(hint("Circle ${i + 1}: text colour (letter and name)"))
            col.addView(swatchRow(textColors, Prefs.dialTextOf(this, i)) { Prefs.setDialTextOf(this, i, it); applyLook(); rebuild() })
        }

        }

        section("\"Where to?\" search") {
        // ---- "Where to?" search
        col.addView(title("\"Where to?\" search", 16f, 20))
        col.addView(hint("Travel mode used when you search"))
        col.addView(modeRow())
        col.addView(toggleRaw("Drive: start navigation straight away", Prefs.navNow(this)) { Prefs.setNavNow(this, it) })
        col.addView(toggleRaw("Turn on Location when I open it", Prefs.autoLocation(this)) { Prefs.setAutoLocation(this, it) })
        col.addView(toggleRaw("Where to? goes straight to the Google Maps search screen", Prefs.mapsSearchDirect(this)) { Prefs.setMapsSearchDirect(this, it) })
        col.addView(hint("Opens Google Maps and, with the Accessibility helper ON, taps its search bar for you, so you get Maps' own suggestions, Home, Work and Recent. Helper OFF: Maps opens and you tap its search bar yourself. Off: the Notif Tiles box below is used."))
        col.addView(toggleRaw("Closing the Maps pop-up also exits navigation", Prefs.exitOnPopupClose(this)) { Prefs.setExitOnPopupClose(this, it) })
        col.addView(hint("Needs the Accessibility helper ON. When you close the small Google Maps window, Notif Tiles presses Exit navigation for you (Location then switches off too, if that option is set up). Opening the pop-up to full screen does not end it."))
        col.addView(toggleRaw("Switch Location off again when navigation ends", Prefs.autoLocationOff(this)) { Prefs.setAutoLocationOff(this, it) })
        col.addView(hint(if (LocationAuto.canWrite(this)) "Permission granted: Location is switched on and off by Notif Tiles with no pop-up. Location is switched off again once the Google Maps navigation notification is gone (if it was already on before, only with the next switch on)." else "Needs a one-time permission. Without it Android always shows its Turn on pop-up and Location stays on until you switch it off yourself. In Termux, with adb connected to this phone, run: adb shell pm grant com.example.notiftiles android.permission.WRITE_SECURE_SETTINGS"))
        col.addView(toggleRaw("...even if Location was already on before", Prefs.locOffAlways(this)) { Prefs.setLocOffAlways(this, it) })
        val locTest = Button(this)
        locTest.isAllCaps = false
        locTest.text = "Test: switch Location off now"
        locTest.setOnClickListener {
            LocationAuto.testOff(this) { msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
        }
        col.addView(locTest)
        val selfCheck = Button(this)
        selfCheck.isAllCaps = false
        selfCheck.text = "Self-check: what works and what does not"
        selfCheck.setOnClickListener {
            AlertDialog.Builder(this).setTitle("Where to? / Exit / Location").setMessage(LocationAuto.report(this)).setPositiveButton("OK", null).show()
        }
        col.addView(selfCheck)
        val exitTest = Button(this)
        exitTest.isAllCaps = false
        exitTest.text = "Test: exit navigation now"
        exitTest.setOnClickListener {
            LocationAuto.exitSolid(this) { msg -> Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show() }
        }
        col.addView(exitTest)
        col.addView(toggleRaw("Voice (mic) button", Prefs.voiceButton(this)) { Prefs.setVoiceButton(this, it) })
        col.addView(toggleRaw("Place suggestions while typing", Prefs.suggestions(this)) { Prefs.setSuggestions(this, it) })
        col.addView(toggleRaw("Remember recent places", Prefs.rememberPlaces(this)) { Prefs.setRememberPlaces(this, it) })
        col.addView(placeField("Home address", "home"))
        col.addView(placeField("Work address", "work"))
        val clr = Button(this)
        clr.text = "Clear recent places"
        clr.isAllCaps = false
        clr.setOnClickListener {
            Prefs.clearRecents(this)
            Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show()
        }
        col.addView(clr)

        }

        section("Notes") {
        // ---- Notes app
        col.addView(title("Notes", 16f, 20))
        col.addView(hint("Where \"+ Note\" saves what you type."))
        col.addView(toggleRaw("+ Note opens the notes app straight away (no box here)", Notes.direct(this)) { Notes.setDirect(this, it) })
        col.addView(hint("Off: you type in a box here, then tap Send to notes app. On: your notes app opens with a new empty note and you type there. The app below is chosen once and used every time."))
        val noteOptions = ArrayList<Pair<String, String>>()
        noteOptions.add("auto" to "Phone default")
        for (a in Notes.installedApps(this)) noteOptions.add(a)
        val cur = Notes.mode(this)
        if (cur !in listOf("auto", "ask", "inapp") && noteOptions.none { it.first == cur }) {
            noteOptions.add(cur to Notes.label(this, cur))
        }
        noteOptions.add("ask" to "Ask each time")
        noteOptions.add("inapp" to "Inside Notif Tiles")
        col.addView(choiceRow(noteOptions, cur, false) { Notes.setMode(this, it); rebuild() })
        val other = Button(this)
        other.isAllCaps = false
        other.text = "Choose any other app…"
        other.setOnClickListener { Notes.pickDialog(this) { rebuild() } }
        col.addView(other)

        }

        section("Voice recorder") {
        // ---- Voice recorder (mic next to + Note)
        col.addView(title("Voice recorder", 16f, 20))
        col.addView(hint("The blue mic at the start of the widget header starts recording. With the Accessibility helper ON it uses your phone's recorder; with the helper OFF it records inside this app (switch below). While recording, the widget mic turns into a red stop square and the phone buzzes: tap it again to stop and save. Recordings are saved as m4a in Internal storage > Recordings > NotifTiles."))
        val recOptions = ArrayList<Pair<String, String>>()
        recOptions.add("auto" to "Phone's recorder")
        for (a in Recorder.installedApps(this)) recOptions.add(a)
        val recCur = Recorder.mode(this)
        if (recCur != "auto" && recOptions.none { it.first == recCur }) recOptions.add(recCur to Recorder.label(this, recCur))
        col.addView(choiceRow(recOptions, recCur, false) { Recorder.setMode(this, it); rebuild() })
        val recOther = Button(this)
        recOther.isAllCaps = false
        recOther.text = "Choose any other app…"
        recOther.setOnClickListener { Recorder.pickDialog(this, { rebuild() }) }
        col.addView(recOther)
        col.addView(hint("How the mic starts recording. If the mic only opens the Recorder, try another one and use the test button."))
        col.addView(choiceRow(Recorder.START_ACTIONS, Recorder.startAction(this), false) { Recorder.setStartAction(this, it); rebuild() })
        col.addView(toggleRaw("Helper OFF: record inside this app (saved in Recordings/NotifTiles)", Recorder.builtinOn(this)) { Recorder.setBuiltinOn(this, it) })
        val recTest = Button(this)
        recTest.isAllCaps = false
        recTest.text = "Test: start recording now"
        recTest.setOnClickListener { startActivity(Intent(this, RecordActivity::class.java)) }
        col.addView(recTest)
        col.addView(title("Auto-press the red button", 14f, 12))
        col.addView(hint("This phone's Recorder ignores start requests from other apps. This helper opens the Recorder and presses its red record button for you. Turn on \"Notif Tiles recorder button\" in Accessibility. If the switch is greyed out: App info > ⋮ > Allow restricted settings."))
        col.addView(hint("Helper is " + (if (RecorderTapService.enabled(this)) "ON" else "OFF")))
        val accBtn = Button(this)
        accBtn.isAllCaps = false
        accBtn.text = "Open Accessibility settings"
        accBtn.setOnClickListener { startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        col.addView(accBtn)
        col.addView(hint(Recorder.learnedSummary(this)))
        val learnBtn = Button(this)
        learnBtn.isAllCaps = false
        learnBtn.text = "Learn the red button"
        learnBtn.setOnClickListener {
            if (!RecorderTapService.enabled(this)) {
                Toast.makeText(this, "Turn the helper on in Accessibility first", Toast.LENGTH_LONG).show()
            } else {
                RecorderTapService.learnUntil = System.currentTimeMillis() + 60000
                Recorder.open(this, true)
                Toast.makeText(this, "Now press the red record button once, then stop the recording", Toast.LENGTH_LONG).show()
            }
        }
        col.addView(learnBtn)
        col.addView(seekRange("Red button position: across", Recorder.tapX(this), 20, 80, "%") { Recorder.setTapX(this, it) })
        col.addView(seekRange("Red button position: down", Recorder.tapY(this), 60, 95, "%") { Recorder.setTapY(this, it) })
        val recCheck = Button(this)
        recCheck.isAllCaps = false
        recCheck.text = "Check recorder entry points"
        recCheck.setOnClickListener {
            val info = Recorder.diagnose(this)
            android.app.AlertDialog.Builder(this)
                .setTitle("Recorder")
                .setMessage(info)
                .setPositiveButton("Copy") { _, _ ->
                    val cm = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("recorder", info))
                    Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Close", null)
                .show()
        }
        col.addView(recCheck)

        }

        section("Quiet & snooze") {
        col.addView(title("Snooze", 16f, 20))
        col.addView(hint(Extras.quietText(this)?.let { "$it. The widget stays empty until then; the panel still lists your messages." }
            ?: "While snoozed the widget and its count stay empty. Your messages stay in the panel and in the phone's notification shade."))
        col.addView(choiceRow(listOf("30" to "30 min", "60" to "1 hour", "180" to "3 hours", "tom" to "Until 7:00", "0" to "Cancel"), "", false) {
            val until = when (it) {
                "0" -> 0L
                "tom" -> Extras.nextAt(7 * 60)
                else -> System.currentTimeMillis() + it.toLong() * 60000L
            }
            Prefs.setSnoozeUntil(this, until); Extras.tick(this); applyLook(); rebuild()
        })
        col.addView(title("Quiet hours", 16f, 20))
        col.addView(toggleRaw("Keep the widget quiet during these hours", Prefs.quietOn(this)) {
            Prefs.setQuietOn(this, it); Extras.tick(this); applyLook(); rebuild()
        })
        if (Prefs.quietOn(this)) {
            col.addView(timeRow("From", Prefs.quietStart(this)) { Prefs.setQuietStart(this, it); Extras.tick(this); applyLook(); rebuild() })
            col.addView(timeRow("Until", Prefs.quietEnd(this)) { Prefs.setQuietEnd(this, it); Extras.tick(this); applyLook(); rebuild() })
        }

        }

        section("Show / hide items") {
        col.addView(hint("Switch off anything you do not want to see. Nothing is deleted: switch it on again any time. You can always open Look & colors from the Notif Tiles app itself."))
        col.addView(title("Widget top row and boxes", 16f, 12))
        col.addView(toggle("Clock circle", "clock"))
        col.addView(toggle("Weather circle", "weather"))
        col.addView(toggle("Date circle", "date"))
        col.addView(toggle("Maps \"Where to?\" box", "maps"))
        col.addView(toggle("Fast-dial strip", "dial"))
        col.addView(toggleRaw("Names under fast-dial circles", Prefs.dialNames(this)) { Prefs.setDialNames(this, it); applyLook() })
        col.addView(toggleRaw("Refresh icon (centre of the widget)", Prefs.refreshShow(this)) { Prefs.setRefreshShow(this, it); applyLook() })
        col.addView(toggleRaw("\"Updated\" time under the refresh icon", Weather.showUpdated(this)) { Weather.setShowUpdated(this, it); applyLook() })
        col.addView(toggleRaw("\"Nothing new\" message", Prefs.showEmptyText(this)) { Prefs.setShowEmptyText(this, it); applyLook() })
        col.addView(title("Widget header, tiles and panel", 16f, 16))
        for ((k, label) in Prefs.SHOW_ITEMS) {
            col.addView(toggleRaw(label, Prefs.showItem(this, k)) { Prefs.setShowItem(this, k, it); applyLook() })
        }

        }

        section("Scroll, pages & grouping") {
        col.addView(title("Scrolling", 16f, 12))
        col.addView(hint("Scroll = the list moves up and down (as before). Pages = a few tiles at a time: ‹ › buttons under the widget; in the panel you can also swipe left or right. (A home-screen widget cannot be swiped sideways, that is an Android limit.)"))
        col.addView(choiceRow(listOf("0" to "Scroll up / down", "1" to "Pages"), Prefs.scrollMode(this).toString(), false) {
            Prefs.setScrollMode(this, it.toInt()); Prefs.setPage(this, "w", 0); Prefs.setPage(this, "p", 0); applyLook(); rebuild()
        })
        if (Prefs.scrollMode(this) == 1) {
            col.addView(seekRange("Tiles per page", Prefs.pageSize(this), 2, 20, "") {
                Prefs.setPageSize(this, it); Prefs.setPage(this, "w", 0); Prefs.setPage(this, "p", 0)
            })
        }
        col.addView(toggleRaw("Endless: pages wrap around, and the widget list repeats", Prefs.loop(this)) { Prefs.setLoop(this, it); applyLook() })

        col.addView(title("Group messages", 16f, 20))
        col.addView(hint("One tile per chat (as before), or one tile per app that shows all its chats together. Tapping a grouped tile opens its newest chat."))
        col.addView(choiceRow(listOf("0" to "One tile per chat", "1" to "One tile per app"), Prefs.groupMode(this).toString(), false) {
            Prefs.setGroupMode(this, it.toInt()); Prefs.setPage(this, "w", 0); Prefs.setPage(this, "p", 0); applyLook(); rebuild()
        })

        col.addView(title("Order of the tiles", 16f, 20))
        col.addView(hint("Pinned apps (Apps: pin, mute, colour) always stay first."))
        col.addView(choiceRow(listOf("0" to "Newest first", "1" to "Oldest first", "2" to "My own order", "3" to "App name A-Z"), Prefs.sortMode(this).toString(), false) {
            Prefs.setSortMode(this, it.toInt()); applyLook(); rebuild()
        })
        if (Prefs.sortMode(this) == 2) {
            col.addView(hint("Use ▲ ▼ to move an app up or down."))
            val enabledApps = Prefs.enabled(this).toList()
            val saved = Prefs.appOrder(this).filter { it in enabledApps }
            val mine = saved + enabledApps.filter { it !in saved }.sortedBy { Feed.label(this, it).lowercase() }
            for ((i, pkg) in mine.withIndex()) {
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = Gravity.CENTER_VERTICAL
                val name = TextView(this)
                name.text = "${i + 1}.  ${Feed.label(this, pkg)}"
                name.textSize = 15f
                row.addView(name, LinearLayout.LayoutParams(0, WRAP, 1f))
                row.addView(moveButton("▲", i > 0) { moveApp(mine, i, i - 1) })
                row.addView(moveButton("▼", i < mine.size - 1) { moveApp(mine, i, i + 1) })
                col.addView(row)
            }
        }

        }

        section("Apps: pin, mute, colour") {
        col.addView(hint("Pinned apps are listed first. Muted apps are hidden from the widget, the panel and the counts. A colour changes that app's tiles."))
        val pkgs = Prefs.enabled(this).sortedBy { Feed.label(this, it).lowercase() }
        if (pkgs.isEmpty()) col.addView(hint("No apps chosen yet. Choose them on the main screen first."))
        for (p in pkgs) {
            col.addView(title(Feed.label(this, p), 14f, 12))
            col.addView(toggleRaw("Pin to top", p in Prefs.pinned(this)) { Prefs.setPinned(this, p, it); applyLook() })
            col.addView(toggleRaw("Mute", p in Prefs.muted(this)) { Prefs.setMuted(this, p, it); applyLook() })
            col.addView(swatchRow(bgColors, Prefs.appColor(this, p)) { Prefs.setAppColor(this, p, it); applyLook(); rebuild() })
        }

        }

        section("Counts") {
        // ---- Counts
        col.addView(title("Counts", 16f, 20))
        col.addView(toggleRaw("Count drops after I read a message here", Prefs.countRead(this)) { Prefs.setCountRead(this, it) })
        col.addView(toggleRaw("Count drops after I reply here", Prefs.countReply(this)) { Prefs.setCountReply(this, it) })
        col.addView(toggleRaw("Also remove it from the phone's notification panel", Prefs.dismissHandled(this)) { Prefs.setDismissHandled(this, it) })
        col.addView(hint("The Clear all button (widget and panel) does"))
        col.addView(choiceRow(listOf("0" to "Clear all", "1" to "Mark all read"), Prefs.clearMode(this).toString(), false) {
            Prefs.setClearMode(this, it.toInt()); applyLook(); rebuild()
        })
        col.addView(hint("Mark all read counts every message as read and, with the option above, clears it from the phone's shade, the same as opening it here. The button then says Read all.\n\nWhen you read, reply to or mark a message as read here, its notification is cleared from the phone's shade too. A new message from the same chat counts again."))

        }

        section("Backup & restore") {
            col.addView(hint("Saves all your Look & colors choices, fast-dial contacts, notes and background pictures into one file, so a reinstall does not lose them."))
            val ex = Button(this)
            ex.text = "Export settings to a file"
            ex.isAllCaps = false
            ex.setOnClickListener {
                try {
                    val i = Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/json").putExtra(Intent.EXTRA_TITLE, "notiftiles-settings.json")
                    startActivityForResult(i, 110)
                } catch (e: Exception) {
                    Toast.makeText(this, "No file app found", Toast.LENGTH_SHORT).show()
                }
            }
            col.addView(ex)
            val im = Button(this)
            im.text = "Import settings from a file"
            im.isAllCaps = false
            im.setOnClickListener {
                try {
                    val i = Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*")
                    startActivityForResult(i, 111)
                } catch (e: Exception) {
                    Toast.makeText(this, "No file app found", Toast.LENGTH_SHORT).show()
                }
            }
            col.addView(im)
        }

        val reset = Button(this)
        reset.text = "Reset everything to automatic"
        reset.isAllCaps = false
        reset.setOnClickListener {
            Prefs.resetLook(this)
            applyLook()
            rebuild()
        }
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = dp(24)
        col.addView(reset, lp)

        updatePreviews()
    }

    private fun modeRow(): View {
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        hs.addView(row)
        for ((key, label) in DestinationActivity.MODES) {
            val b = Button(this)
            b.isAllCaps = false
            b.text = if (key == Prefs.travelMode(this)) "✓ $label" else label
            b.setOnClickListener {
                Prefs.setTravelMode(this, key)
                rebuild()
            }
            row.addView(b)
        }
        return hs
    }

    private fun textPref(label: String, current: String, onSave: (String) -> Unit): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val et = EditText(this)
        et.hint = label
        et.setSingleLine(true)
        et.setText(current)
        row.addView(et, LinearLayout.LayoutParams(0, WRAP, 1f))
        val b = Button(this)
        b.text = "Save"
        b.isAllCaps = false
        b.setOnClickListener {
            onSave(et.text.toString())
            applyLook()
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }
        row.addView(b)
        return row
    }

    private fun timeRow(label: String, minutes: Int, onPick: (Int) -> Unit): View {
        val b = Button(this)
        b.isAllCaps = false
        b.text = "$label: ${Extras.fmt(minutes)}"
        b.setOnClickListener {
            android.app.TimePickerDialog(this, { _, h, m -> onPick(h * 60 + m) }, minutes / 60, minutes % 60, true).show()
        }
        return b
    }

    private fun placeField(label: String, key: String): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val et = EditText(this)
        et.hint = label
        et.setSingleLine(true)
        et.setText(Prefs.fav(this, key))
        row.addView(et, LinearLayout.LayoutParams(0, WRAP, 1f))
        val b = Button(this)
        b.text = "Save"
        b.isAllCaps = false
        b.setOnClickListener {
            Prefs.setFav(this, key, et.text.toString())
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
        }
        row.addView(b)
        return row
    }

    private var fontPreview: TextView? = null

    private fun updateFontPreview() {
        fontPreview?.let { Look.applyTree(it, this) }
    }

    private val textColors = intArrayOf(
        0, c(0xFF000000), c(0xFFFFFFFF), c(0xFF1B1B1F), c(0xFFF2F2F5),
        c(0xFFFACC15), c(0xFF22D3EE), c(0xFF4ADE80), c(0xFFFB923C), c(0xFFF472B6)
    )

    private val KINDS = listOf("widget", "dial", "panel")

    private fun choiceRow(options: List<Pair<String, String>>, selected: String, preview: Boolean, onPick: (String) -> Unit): View {
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        hs.addView(row)
        for ((key, label) in options) {
            val b = Button(this)
            b.isAllCaps = false
            b.text = if (key == selected) "✓ $label" else label
            if (preview) b.typeface = Typeface.create(key, Typeface.NORMAL)
            b.setOnClickListener { onPick(key) }
            row.addView(b)
        }
        return hs
    }

    private fun seekRange(label: String, value: Int, min: Int, max: Int, unit: String, onChange: (Int) -> Unit): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val t = TextView(this)
        t.text = "$label: $value$unit"
        t.setPadding(0, dp(8), 0, 0)
        box.addView(t)
        val sb = SeekBar(this)
        sb.max = max - min
        sb.progress = (value - min).coerceIn(0, max - min)
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                t.text = "$label: ${p + min}$unit"
                if (fromUser) onChange(p + min)
            }

            override fun onStartTrackingTouch(s: SeekBar) {}

            override fun onStopTrackingTouch(s: SeekBar) {
                TilesWidget.refresh(applicationContext)
            }
        })
        box.addView(sb)
        return box
    }

    private fun imageRow(label: String, kind: String): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val has = Look.hasImage(this, kind)
        box.addView(hint(if (has) "$label: picture set" else "$label: none"))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val pick = Button(this)
        pick.isAllCaps = false
        pick.text = if (has) "Change picture" else "Choose picture"
        pick.setOnClickListener { pickImage(kind) }
        row.addView(pick)
        if (has) {
            val rm = Button(this)
            rm.isAllCaps = false
            rm.text = "Remove"
            rm.setOnClickListener {
                Look.clearImage(this, kind)
                applyLook()
                rebuild()
            }
            row.addView(rm)
        }
        box.addView(row)
        if (has) {
            box.addView(seekRange("Picture strength", Prefs.imgAlpha(this, kind), 10, 100, "%") { Prefs.setImgAlpha(this, kind, it) })
        }
        return box
    }

    private fun pickImage(kind: String) {
        try {
            val i = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
            startActivityForResult(Intent.createChooser(i, "Choose picture"), 100 + KINDS.indexOf(kind))
        } catch (e: Exception) {
            Toast.makeText(this, "No gallery app found", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode == 110 && resultCode == RESULT_OK && uri != null) {
            Thread {
                val ok = try {
                    contentResolver.openOutputStream(uri, "wt")?.use { it.write(Backup.export(applicationContext).toByteArray()) }
                    true
                } catch (e: Exception) {
                    false
                }
                runOnUiThread { Toast.makeText(this, if (ok) "Settings saved" else "Could not save the file", Toast.LENGTH_SHORT).show() }
            }.start()
        }
        if (requestCode == 111 && resultCode == RESULT_OK && uri != null) {
            Thread {
                val n = try {
                    val text = contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                    Backup.import(applicationContext, text)
                } catch (e: Exception) {
                    -1
                }
                runOnUiThread {
                    Toast.makeText(this, if (n >= 0) "Restored $n settings" else "That file is not a Notif Tiles backup", Toast.LENGTH_LONG).show()
                    applyLook()
                    rebuild()
                }
            }.start()
        }
        if (requestCode in 100..102 && resultCode == RESULT_OK && uri != null) {
            val kind = KINDS[requestCode - 100]
            Thread {
                val ok = Look.importImage(applicationContext, uri, kind)
                runOnUiThread {
                    Toast.makeText(this, if (ok) "Picture set" else "Could not use that picture", Toast.LENGTH_SHORT).show()
                    applyLook()
                    rebuild()
                }
            }.start()
        }
    }

    private fun cityRow(): View {
        val act = this
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val et = EditText(this)
        et.hint = "City, e.g. Riyadh"
        et.setSingleLine(true)
        et.setText(Weather.city(this))
        et.setSelectAllOnFocus(true)
        row.addView(et, LinearLayout.LayoutParams(0, WRAP, 1f))
        val b = Button(this)
        b.text = "Set"
        b.isAllCaps = false
        b.setOnClickListener {
            val name = et.text.toString().trim()
            if (name.isEmpty() || name == Weather.city(act)) return@setOnClickListener
            Toast.makeText(this, "Looking up…", Toast.LENGTH_SHORT).show()
            Thread {
                val r = Weather.lookup(applicationContext, name)
                runOnUiThread {
                    Toast.makeText(this, r ?: "City not found", Toast.LENGTH_SHORT).show()
                    if (r != null) {
                        TilesWidget.refresh(applicationContext)
                        rebuild()
                    }
                }
            }.start()
        }
        row.addView(b)
        box.addView(row)

        // Small list of matching places that appears while typing; tap one to use it.
        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        box.addView(list)
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var token = 0
        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s?.toString()?.trim() ?: ""
                token++
                val mine = token
                list.removeAllViews()
                if (q.length < 2 || q == Weather.city(act)) return
                handler.postDelayed({
                    if (mine != token) return@postDelayed
                    Thread {
                        val hits = Weather.search(q)
                        act.runOnUiThread {
                            if (mine != token) return@runOnUiThread
                            list.removeAllViews()
                            for (h in hits) {
                                val t = TextView(act)
                                t.text = h.label
                                t.textSize = 15f
                                t.setPadding(act.dp(12), act.dp(10), act.dp(12), act.dp(10))
                                val bg = GradientDrawable()
                                bg.setColor(0x22808080)
                                bg.cornerRadius = act.dp(8).toFloat()
                                t.background = bg
                                val lp = LinearLayout.LayoutParams(MATCH, WRAP)
                                lp.topMargin = act.dp(4)
                                t.layoutParams = lp
                                t.setOnClickListener {
                                    Weather.setPlace(act.applicationContext, h)
                                    Toast.makeText(act, h.label, Toast.LENGTH_SHORT).show()
                                    TilesWidget.refresh(act.applicationContext)
                                    act.rebuild()
                                }
                                list.addView(t)
                            }
                        }
                    }.start()
                }, 350)
            }
        })
        return box
    }

    private fun panelPreview(): View {
        val frame = FrameLayout(this)
        frame.setBackgroundColor(0xFF8A8F98.toInt())
        val scrim = View(this)
        frame.addView(scrim, FrameLayout.LayoutParams(MATCH, MATCH))
        val sheet = View(this)
        val slp = FrameLayout.LayoutParams(MATCH, dp(56), Gravity.BOTTOM)
        slp.setMargins(dp(10), 0, dp(10), dp(10))
        frame.addView(sheet, slp)
        scrimView = scrim
        sheetView = sheet
        frame.layoutParams = LinearLayout.LayoutParams(MATCH, dp(110))
        return frame
    }

    private fun widgetPreview(): View {
        val frame = FrameLayout(this)
        frame.setBackgroundColor(0xFF8A8F98.toInt())
        val w = View(this)
        val wlp = FrameLayout.LayoutParams(MATCH, dp(56))
        wlp.setMargins(dp(10), dp(10), dp(10), dp(10))
        frame.addView(w, wlp)
        widgetView = w
        frame.layoutParams = LinearLayout.LayoutParams(MATCH, dp(76))
        return frame
    }

    private fun rounded(color: Int, r: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(r).toFloat()
    }

    private fun updatePreviews() {
        scrimView?.setBackgroundColor(Prefs.scrimArgb(this))
        sheetView?.background = rounded(Prefs.panelBgColor(this), 16)
        widgetView?.background = rounded(Prefs.widgetBgArgb(this), 16)
    }

    private fun hint(t: String) = TextView(this).apply {
        text = t
        textSize = 12f
        alpha = 0.7f
        setPadding(0, 0, 0, dp(6))
    }

    private fun themeRow(): View {
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        hs.addView(row)
        for (t in Themes.LIST) {
            val b = Button(this)
            b.text = t.name
            b.isAllCaps = false
            b.setOnClickListener {
                Themes.apply(this, t)
                applyLook()
                rebuild()
            }
            row.addView(b)
        }
        return hs
    }

    private fun moveButton(label: String, enabled: Boolean, action: () -> Unit): View {
        val b = Button(this)
        b.text = label
        b.minWidth = 0
        b.minimumWidth = 0
        b.isEnabled = enabled
        b.setOnClickListener { action() }
        return b
    }

    private fun moveApp(order: List<String>, a: Int, b: Int) {
        val l = order.toMutableList()
        val t = l[a]
        l[a] = l[b]
        l[b] = t
        Prefs.setAppOrder(this, l)
        applyLook()
        rebuild()
    }

    // One compact row per colour: the label, and a dot showing the colour. Tap it to open the picker.
    private fun colorRow(label: String, current: Int, onPick: (Int) -> Unit): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(4), 0, dp(4))
        val name = TextView(this)
        name.text = label
        name.textSize = 15f
        row.addView(name, LinearLayout.LayoutParams(0, WRAP, 1f))
        val dot = TextView(this)
        dot.gravity = Gravity.CENTER
        dot.textSize = 11f
        val d = GradientDrawable()
        d.shape = GradientDrawable.OVAL
        if (current == 0) {
            d.setColor(0xFF9E9E9E.toInt())
            dot.text = "Auto"
            dot.setTextColor(0xFFFFFFFF.toInt())
        } else {
            d.setColor(current)
        }
        d.setStroke(dp(1), 0x55888888)
        dot.background = d
        row.addView(dot, LinearLayout.LayoutParams(dp(44), dp(44)))
        row.setOnClickListener { ColorPicker.show(this, label, current, true) { v -> onPick(v) } }
        return row
    }

    private fun swap(order: List<String>, a: Int, b: Int) {
        val l = order.toMutableList()
        val t = l[a]
        l[a] = l[b]
        l[b] = t
        Prefs.setOrder(this, l)
        applyLook()
        rebuild()
    }

    private fun toggle(label: String, key: String): View =
        toggleRaw(label, Prefs.show(this, key)) { Prefs.setShow(this, key, it); applyLook() }

    private fun toggleRaw(label: String, on: Boolean, onChange: (Boolean) -> Unit): View {
        val sw = Switch(this)
        sw.text = label
        sw.isChecked = on
        sw.setPadding(0, dp(6), 0, dp(6))
        sw.setOnCheckedChangeListener { _, v -> onChange(v) }
        return sw
    }

    private fun swatchRow(colors: IntArray, selected: Int, onPick: (Int) -> Unit): View {
        val hs = HorizontalScrollView(this)
        hs.isHorizontalScrollBarEnabled = false
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        hs.addView(row)
        for (color in colors) {
            val tv = TextView(this)
            tv.gravity = Gravity.CENTER
            tv.textSize = 11f
            val d = GradientDrawable()
            d.shape = GradientDrawable.OVAL
            if (color == 0) {
                d.setColor(0xFF9E9E9E.toInt())
                tv.text = "Auto"
                tv.setTextColor(0xFFFFFFFF.toInt())
            } else {
                d.setColor(color)
            }
            val on = color == selected
            d.setStroke(if (on) dp(3) else dp(1), if (on) 0xFF3B82F6.toInt() else 0x55888888)
            tv.background = d
            tv.setOnClickListener { onPick(color) }
            val lp = LinearLayout.LayoutParams(dp(44), dp(44))
            lp.setMargins(0, dp(8), dp(8), dp(8))
            row.addView(tv, lp)
        }
        // "+" = every other colour (palette, sliders, hex code)
        val more = TextView(this)
        more.gravity = Gravity.CENTER
        more.textSize = 20f
        more.text = "+"
        more.setTextColor(0xFFFFFFFF.toInt())
        val md = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(c(0xFFEF4444), c(0xFFFACC15), c(0xFF4ADE80), c(0xFF22D3EE), c(0xFFA78BFA))
        )
        md.shape = GradientDrawable.OVAL
        more.background = md
        more.setOnClickListener { ColorPicker.show(this, "Pick a colour", selected, colors.contains(0)) { v -> onPick(v) } }
        val mlp = LinearLayout.LayoutParams(dp(44), dp(44))
        mlp.setMargins(0, dp(8), dp(8), dp(8))
        row.addView(more, mlp)
        return hs
    }

    private fun customHex(onColor: (Int) -> Unit): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val et = EditText(this)
        et.hint = "Custom colour, e.g. #1E90FF"
        et.setSingleLine(true)
        row.addView(et, LinearLayout.LayoutParams(0, WRAP, 1f))
        val b = Button(this)
        b.text = "Apply"
        b.isAllCaps = false
        b.setOnClickListener {
            try {
                val v = Color.parseColor(et.text.toString().trim())
                onColor(v or 0xFF000000.toInt())
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, "Use a colour like #1E90FF", Toast.LENGTH_SHORT).show()
            }
        }
        row.addView(b)
        return row
    }

    private fun seek(label: String, value: Int, onChange: (Int) -> Unit): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val t = TextView(this)
        t.text = "$label: $value%"
        t.setPadding(0, dp(8), 0, 0)
        box.addView(t)
        val sb = SeekBar(this)
        sb.max = 100
        sb.progress = value
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                t.text = "$label: $p%"
                if (fromUser) onChange(p)
            }

            override fun onStartTrackingTouch(s: SeekBar) {}

            override fun onStopTrackingTouch(s: SeekBar) {
                TilesWidget.refresh(applicationContext)
            }
        })
        box.addView(sb)
        return box
    }
}
