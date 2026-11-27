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
    private var helperBox: EditText? = null
    /** What the helper changed last, so "it" and "that" can mean it. */
    private var lastTarget: String? = null
    private val REQ_VOICE = 4711
    private val REQ_PIN_PIC = 4712
    private lateinit var live: LivePreview
    private var scrimView: View? = null
    private var sheetView: View? = null
    private var widgetView: View? = null

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun c(v: Long) = v.toInt()

    private val bgColors = intArrayOf(
        0, c(0xFF000000), c(0xFF1C1C20), c(0xFFFFFFFF), c(0xFFF2F2F5), c(0xFF1E3A8A),
        c(0xFF0F766E), c(0xFF166534), c(0xFF7C3AED), c(0xFFB91C1C), c(0xFFEA580C), c(0xFFDB2777),
        c(0xFF0B1020), c(0xFF0F3D4C), c(0xFF14301F), c(0xFF3B1D2A), c(0xFF2B1D16),
        c(0xFFFCE7F3), c(0xFFE0F2FE), c(0xFFF5EBDD), c(0xFFEDE9FE), c(0xFFD1FAE5)
    )
    private val borderColors = intArrayOf(
        0, c(0xFFFFFFFF), c(0xFF000000), c(0xFF3B82F6), c(0xFF22D3EE), c(0xFF4ADE80),
        c(0xFFFACC15), c(0xFFFB923C), c(0xFFEF4444), c(0xFFDB2777), c(0xFFA78BFA),
        c(0xFFFFD700), c(0xFF00F5D4), c(0xFFFF2BD6), c(0xFF84CC16), c(0xFF6366F1), c(0xFF40E0D0)
    )
    private val scrimColors = intArrayOf(
        c(0xFF000000), c(0xFF1C1C20), c(0xFF1E3A8A), c(0xFF7C3AED), c(0xFF0F766E), c(0xFF3E2723),
        c(0xFF0B1020), c(0xFF14301F), c(0xFF3B1D2A)
    )

    private val accents = intArrayOf(
        c(0xFF3B82F6), c(0xFF22D3EE), c(0xFF4ADE80), c(0xFFFACC15),
        c(0xFFFB923C), c(0xFFEF4444), c(0xFFDB2777), c(0xFFA78BFA),
        c(0xFFFF2BD6), c(0xFF00F5D4), c(0xFFFF7F50), c(0xFF40E0D0),
        c(0xFF84CC16), c(0xFF6366F1), c(0xFFFFD700), c(0xFFFFFFFF)
    )

    // The Settings screen's own look (light / dark / AMOLED / your colours): see SettingsTheme
    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(SettingsTheme.wrap(newBase))
    }

    private lateinit var screenView: View
    private companion object {
        // Changing the look recreates the screen; these keep the open groups and the scroll place across that
        var keepOpen: Set<String> = emptySet()
        var keepScroll: Int = 0
    }

    /** Re-creates the screen so a new day / night look or background takes hold, keeping the open groups and the place. */
    private fun restyle() {
        keepOpen = HashSet(openSections)
        keepScroll = scroll.scrollY
        recreate()
    }

    private fun applyTheme() {
        SettingsTheme.applyWindow(this, screenView)
        SettingsTheme.applyTree(this, col)
        if (::undoBar.isInitialized) SettingsTheme.applyTree(this, undoBar.view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scroll = ScrollView(this)
        col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(16), dp(16), dp(16), dp(32))
        scroll.addView(col)
        live = LivePreview(this)
        val screen = live.wrap(scroll)
        screenView = screen
        tracker = ChangeTracker(this, Prefs.SCOPE_SETTINGS) { if (::undoBar.isInitialized) undoBar.refresh() }
        undoBar = UndoBar(this, Prefs.SCOPE_SETTINGS, tracker) { applyLook(); rebuild() }
        UndoBar.pin(screen, undoBar)
        setContentView(screen)
        // Scroll down: the live preview shrinks so it stays in view; scroll back up (or to the top): it grows again
        var travel = 0
        scroll.setOnScrollChangeListener { _, _, y, _, oldY ->
            // The preview resizing moves the list too (it is clamped at the bottom): that is not the finger, so it is ignored
            if (live.isSettling() || editing) { travel = 0; return@setOnScrollChangeListener }
            val dy = y - oldY
            if (y < dp(40)) { travel = 0; return@setOnScrollChangeListener }   // near the search box and undo bar: the preview never pops in or out
            if ((dy > 0) != (travel > 0)) travel = 0
            travel += dy
            // Scrolling either way only hides it; it comes back by pressing a section, changing a setting, tapping the title or reaching the top
            if (kotlin.math.abs(travel) > dp(90) && y > dp(120)) { live.shrink(true); travel = 0 }
        }
        // Tap the screen title (above the undo bar) to bring a hidden preview back
        window.decorView.post {
            fun inScroll(v: android.view.View): Boolean { var p: android.view.ViewParent? = v.parent; while (p != null) { if (p === scroll) return true; p = p.parent }; return false }
            fun find(v: android.view.View) {
                if (v is TextView && v.text?.toString() == "Look & colors" && !inScroll(v)) { v.setOnClickListener { live.reveal() }; return }
                if (v is ViewGroup) for (i in 0 until v.childCount) find(v.getChildAt(i))
            }
            find(window.decorView)
        }
        openSections.addAll(keepOpen)
        keepOpen = emptySet()
        build()
        applyFilter()
        applyTheme()
        if (keepScroll > 0) { val y = keepScroll; keepScroll = 0; scroll.post { scroll.scrollTo(0, y) } }
    }

    // Settings' own undo list (the pencil screens keep a separate one): every change made here can be taken back one step at a time
    private lateinit var tracker: ChangeTracker
    private lateinit var undoBar: UndoBar

    // Opened from the widget's gear: Back returns straight to the home screen (the widget), not to a screen behind it.
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        openSections.clear()
        if (intent.getBooleanExtra("from_widget", false)) {
            try {
                startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                // fall back to the normal Back
            }
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        live.start()
        childOpen = false
        tracker.start()
        undoBar.refresh()
    }

    override fun onPause() {
        // Opening a screen from Settings (contacts, shortcuts, notes, Arrange...) keeps tracking, so what is changed there is also one undo step
        if (!childOpen) tracker.stop()
        live.stop()
        // Alive icons: the widget's gear spins when you leave Settings (you land on the widget)
        if (!childOpen) Alive.go(this, "gear", 1800)
        super.onPause()
    }

    private var childOpen = false

    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        childOpen = true
        super.startActivityForResult(intent, requestCode, options)
    }


    // Leaving Settings (Home, recents) closes every section, so it always opens tidy next time.
    override fun onUserLeaveHint() {
        openSections.clear()
        super.onUserLeaveHint()
    }

    override fun onDestroy() {
        tracker.stop()
        openSections.clear()
        super.onDestroy()
    }

    override fun onRestart() {
        super.onRestart()
        rebuild()
    }

    // True after a section was pressed open: the preview stays visible while you work in it, scrolling does not hide it
    private var editing = false
    private val headerViews = HashMap<String, View>()

    /** Rebuilds the list. With [focus] the list scrolls so that section's title is at the top and its options are in view. */
    private fun rebuild(focus: String? = null) {
        val y = scroll.scrollY
        col.removeAllViews()
        headerViews.clear()
        build()
        applyFilter()
        applyTheme()
        if (focus == null) { scroll.post { scroll.scrollTo(0, y) }; return }
        // After the new list is laid out and before it is drawn: put the pressed title at the top (no jump to the very top)
        val vto = scroll.viewTreeObserver
        vto.addOnPreDrawListener(object : android.view.ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val o = if (scroll.viewTreeObserver.isAlive) scroll.viewTreeObserver else vto
                o.removeOnPreDrawListener(this)
                val hv = headerViews[focus]
                if (hv != null && hv.height > 0) scroll.scrollTo(0, (hv.top - dp(8)).coerceAtLeast(0)) else scroll.scrollTo(0, y)
                return true
            }
        })
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

    // No group is open when Settings starts, and Back closes them all again.
    private val openSections = mutableSetOf<String>()

    private fun showCheckEverything() {
        val lines = SelfCheck.run(this)
        val b = AlertDialog.Builder(this).setTitle("Check everything")
            .setMessage(SelfCheck.text(lines)).setPositiveButton("OK", null)
        val fix = lines.firstOrNull { it.level >= 1 && it.fix != null }
        if (fix != null) {
            b.setNeutralButton("Fix: " + fix.short) { _, _ ->
                try { startActivity(fix.fix!!) } catch (e: Exception) { Toast.makeText(this, "Could not open that screen", Toast.LENGTH_SHORT).show() }
            }
        }
        b.show()
    }

    private fun category(t: String, sub: String = "") {
        val h = TextView(this)
        h.text = t
        h.textSize = 14f
        h.setTypeface(null, Typeface.BOLD)
        h.letterSpacing = 0.12f
        h.alpha = 0.85f
        h.setPadding(dp(4), dp(30), dp(4), dp(0))
        col.addView(h)
        catViews.add(h)
        if (sub.isNotEmpty()) {
            val d = TextView(this)
            d.text = sub
            d.textSize = 12f
            d.alpha = 0.55f
            d.setPadding(dp(4), dp(0), dp(4), dp(0))
            col.addView(d)
            catViews.add(d)
        }
    }

    // ---- Search: while something is typed, every group is built and only the matching rows stay visible.
    private var query = ""
    private class Grp(val header: View, val rows: List<View>)
    private val grps = ArrayList<Grp>()
    private val topViews = ArrayList<View>()
    private val catViews = ArrayList<View>()
    private var noResults: TextView? = null

    private val searchBox: android.widget.EditText by lazy {
        android.widget.EditText(this).apply {
            hint = "Search settings…"
            isSingleLine = true
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
            background = SettingsTheme.card(this@SettingsActivity, 12)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            // Using the search box: the preview steps out of the way (the list is rebuilt while you type)
            setOnFocusChangeListener { _, has -> if (has) { editing = false; live.hideNow() } }
            setOnEditorActionListener { v, _, _ ->
                v.clearFocus()
                (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager)
                    .hideSoftInputFromWindow(v.windowToken, 0)
                true
            }
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
                override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    val was = query.isNotBlank()
                    query = s?.toString()?.trim() ?: ""
                    val now = query.isNotBlank()
                    if (was != now) {
                        // Switching between the normal list and the search list: build it again, keep typing.
                        scroll.post {
                            rebuild()
                            scroll.post { scroll.scrollTo(0, 0) }
                            searchBox.requestFocus()
                            searchBox.setSelection(searchBox.text.length)
                        }
                    } else if (now) {
                        applyFilter()
                    }
                }
            })
        }
    }

    private fun textOf(v: View): String {
        val sb = StringBuilder()
        fun walk(x: View) {
            if (x is TextView) {
                sb.append(x.text).append(' ')
                if (x.text.isNullOrEmpty()) sb.append(x.hint ?: "").append(' ')
            }
            if (x is ViewGroup) for (i in 0 until x.childCount) walk(x.getChildAt(i))
        }
        walk(v)
        return sb.toString().lowercase()
    }

    private fun applyFilter() {
        if (query.isBlank()) return
        val q = query.lowercase()
        for (v in topViews) v.visibility = View.GONE
        for (v in catViews) v.visibility = View.GONE
        var any = false
        for (g in grps) {
            val headHit = textOf(g.header).contains(q)
            var showRest = false
            var visible = 0
            for (r in g.rows) {
                val t = textOf(r)
                val hit = headHit || t.contains(q)
                // A row with no text of its own (colour circles, ...) belongs to the matching row above it.
                val show = hit || (t.isBlank() && showRest)
                showRest = if (t.isNotBlank()) hit else showRest
                r.visibility = if (show) View.VISIBLE else View.GONE
                if (show) visible++
            }
            val showHead = headHit || visible > 0
            g.header.visibility = if (showHead) View.VISIBLE else View.GONE
            if (showHead) any = true
        }
        noResults?.visibility = if (any) View.GONE else View.VISIBLE
    }

    private val allSections = LinkedHashSet<String>()

    private fun section(name: String, sub: String = "", body: () -> Unit) {
        allSections.add(name)
        val open = query.isNotBlank() || name in openSections
        val h = TextView(this)
        val head = (if (open) "▾  " else "▸  ") + name
        val sb = android.text.SpannableStringBuilder(head)
        if (sub.isNotEmpty()) {
            sb.append("\n").append(sub)
            val st = head.length + 1
            sb.setSpan(android.text.style.RelativeSizeSpan(0.7f), st, sb.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sb.setSpan(android.text.style.ForegroundColorSpan(0x99808080.toInt()), st, sb.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        sb.setSpan(android.text.style.StyleSpan(Typeface.BOLD), 0, head.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        h.text = sb
        h.textSize = 17f
        h.setPadding(dp(14), dp(14), dp(14), dp(14))
        h.background = SettingsTheme.card(this, 12)
        h.setOnClickListener {
            if (query.isNotBlank()) return@setOnClickListener
            if (open) openSections.remove(name) else {
                if (Prefs.oneSection(this)) openSections.clear()
                openSections.add(name)
            }
            editing = openSections.isNotEmpty()
            if (editing) live.reveal()   // pressing a section brings the preview back and keeps it while you work in the section
            rebuild(if (open) null else name)
        }
        headerViews[name] = h
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = dp(12)
        col.addView(h, lp)
        if (open) {
            val start = col.childCount
            body()
            if (query.isNotBlank()) grps.add(Grp(h, (start until col.childCount).map { col.getChildAt(it) }))
        }
    }

    private fun build() {
        col.addView(title("Look & colors", 22f, 0))
        try {
            val vn = packageManager.getPackageInfo(packageName, 0).versionName
            col.addView(hint("Notif Tiles version $vn"))
        } catch (e: Exception) {
        }

        grps.clear(); topViews.clear(); catViews.clear(); allSections.clear()
        col.addView(searchBox, LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })
        noResults = TextView(this).apply {
            text = "No settings match that."
            visibility = View.GONE
            setPadding(dp(4), dp(16), dp(4), dp(0))
        }
        col.addView(noResults)
        val topStart = col.childCount
        col.addView(hint("Tap a section to open or close it. Sections are grouped under LOOK, WIDGET AND PANEL, MESSAGES, TOOLS, SYSTEM and HELP. Use the search box to jump straight to any setting."))
        val openRow = LinearLayout(this)
        openRow.orientation = LinearLayout.HORIZONTAL
        val openAll = Button(this)
        openAll.isAllCaps = false
        openAll.text = "Open all sections"
        openAll.setOnClickListener { openSections.addAll(allSections); editing = false; rebuild() }
        val closeAll = Button(this)
        closeAll.isAllCaps = false
        closeAll.text = "Close all sections"
        closeAll.setOnClickListener { openSections.clear(); editing = false; rebuild() }
        openRow.addView(openAll, LinearLayout.LayoutParams(0, WRAP, 1f))
        openRow.addView(closeAll, LinearLayout.LayoutParams(0, WRAP, 1f))
        col.addView(openRow)
        col.addView(toggleRaw("Open one section at a time", Prefs.oneSection(this)) { Prefs.setOneSection(this, it); openSections.clear(); rebuild() })
        val chkAll = Button(this)
        chkAll.isAllCaps = false
        chkAll.text = "Check everything"
        chkAll.setOnClickListener { showCheckEverything() }
        col.addView(chkAll)
        val btnLog = Button(this)
        btnLog.isAllCaps = false
        btnLog.text = "Button log"
        btnLog.setOnClickListener { ActionLog.show(this) }
        col.addView(btnLog)
        val btnSnap = Button(this)
        btnSnap.isAllCaps = false
        btnSnap.text = "Allow opening Snapchat (answer once)"
        btnSnap.setOnClickListener {
            Toast.makeText(this, "If the phone asks, choose Allow for 30 days", Toast.LENGTH_LONG).show()
            try {
                packageManager.getLaunchIntentForPackage("com.snapchat.android")
                    ?.let { startActivity(it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            } catch (e: Exception) {
            }
        }
        col.addView(btnSnap)

        for (i in topStart until col.childCount) topViews.add(col.getChildAt(i))

        category("✨  HELPER", "Type what you want to change. Works offline.")
        section("Ask the helper", "Type a change or a setting name in plain words") {
            col.addView(hint("Works without internet. It makes the changes it knows, and for any other setting it opens the one with the closest name."))
            val box = EditText(this)
            box.hint = "e.g. make the widget more transparent"
            box.isSingleLine = true
            helperBox = box
            col.addView(box, LinearLayout.LayoutParams(MATCH, WRAP))
            col.addView(smallBtn("🎤 Speak a change") { startVoice() })
            col.addView(smallBtn("Make this change") {
                // One sentence can hold several changes: "make the text bigger and turn animations off"
                for (part in Understand.parts(box.text.toString())) runHelper(part)
            })
        }

        category("🎨  LOOK", "Colours, fonts, text sizes, shapes and borders")
        section("Themes & accent colour", "Ready-made looks, accent colour, auto day / night") {
        // ---- Themes
        col.addView(title("Themes", 16f, 12))
        col.addView(themeRow())

        // ---- Accent colour
        col.addView(title("Accent colour (buttons, badge)", 16f, 20))
        col.addView(swatchRow(accents, Prefs.accent(this)) { Prefs.setAccent(this, it); applyLook(); rebuild() })
        col.addView(title("Icons & Clear all colour (mic, settings, refresh)", 16f, 20))
        col.addView(hint("Themes set this too. Auto = the normal grey. A colour picked for one part under Text colour per part still wins."))
        col.addView(swatchRow(intArrayOf(0) + accents, Prefs.iconColor(this)) { Prefs.setIconColor(this, it); applyLook(); rebuild() })

        // ---- Emoji icons
        col.addView(title("Emoji icons (notes, mic, reset, Clear all, pencil)", 16f, 20))
        col.addView(hint("Put an emoji in place of the normal icon or word. Now: " + EmojiPicker.summary(this) + "  (note, mic, reset, clear, pencil). Some themes bring their own emoji."))
        col.addView(smallBtn("Choose emoji icons...") { EmojiPicker.show(this) { applyLook(); rebuild() } })

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

        section("Settings screen look", "Make this screen your own: light / dark, colours, text size, font") {
        col.addView(hint("This changes only the Settings screen (not the panel or the widget). Auto = as your phone draws it. Pick a mode, then change any colour on top of it."))
        col.addView(title("Mode", 16f, 12))
        col.addView(choiceRow(listOf(
            SettingsTheme.MODE_PHONE.toString() to "Follow phone", SettingsTheme.MODE_LIGHT.toString() to "Light",
            SettingsTheme.MODE_DARK.toString() to "Dark", SettingsTheme.MODE_AMOLED.toString() to "AMOLED black",
            SettingsTheme.MODE_PANEL.toString() to "Match my panel theme"
        ), SettingsTheme.mode(this).toString(), false) { SettingsTheme.setMode(this, it.toInt()); restyle() })
        col.addView(title("Colours", 16f, 16))
        col.addView(colorRow("Background", SettingsTheme.bgOwn(this)) { SettingsTheme.setBg(this, it); restyle() })
        col.addView(colorRow("Text", SettingsTheme.textOwn(this)) { SettingsTheme.setText(this, it); rebuild() })
        col.addView(colorRow("Buttons, switches and sliders", SettingsTheme.accentOwn(this)) { SettingsTheme.setAccent(this, it); rebuild() })
        col.addView(colorRow("Section bars and search box", SettingsTheme.cardOwn(this)) { SettingsTheme.setCard(this, it); rebuild() })
        col.addView(title("Text size", 16f, 16))
        col.addView(choiceRow(listOf("85" to "Small", "100" to "Normal", "115" to "Large", "130" to "Extra large", "145" to "Huge"),
            SettingsTheme.size(this).let { s -> listOf(85, 100, 115, 130, 145).minByOrNull { Math.abs(it - s) }.toString() }, false) {
            SettingsTheme.setSize(this, it.toInt()); rebuild()
        })
        col.addView(title("Font", 16f, 16))
        col.addView(choiceRow(listOf("" to "Phone default") + Look.FONTS, SettingsTheme.font(this), true) { SettingsTheme.setFont(this, it); rebuild() })
        col.addView(title("Corners of section bars", 16f, 16))
        col.addView(choiceRow(listOf("0" to "Square", "6" to "Slight", "-1" to "Normal", "18" to "Round", "30" to "Pill"),
            SettingsTheme.radius(this).toString(), false) { SettingsTheme.setRadius(this, it.toInt()); rebuild() })
        col.addView(toggleRaw("Hide the live preview while I scroll down the settings", live.autoShrink()) { live.setAutoShrink(it) })
        col.addView(smallBtn("Reset the Settings screen look") { SettingsTheme.reset(this); restyle() })
        }

        section("Backgrounds & pictures", "Panel and widget backgrounds, photos, fast-dial strip") {
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

        section("Fonts & overall text size", "Font, weight, italic, letter case, one size for all text") {
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
        }

        section("Text size per part", "Its own size for every text of the panel and the widget") {
        col.addView(title("Text size per part of the panel", 14f, 12))
        col.addView(hint("On top of the overall text size (Fonts & overall text size). 100% = as designed."))
        for ((g, label) in Prefs.PANEL_GROUPS) {
            col.addView(seekRange(label, Prefs.grpSize(this, g), 60, 220, "%") { Prefs.setGrpSize(this, g, it) })
        }


        col.addView(title("Text size per part of the widget", 14f, 12))
        col.addView(hint("Every text on the home-screen widget has its own size, on top of the overall text size (Fonts & overall text size). 100% = as designed. The Updated line has its own slider under Widget wording."))
        for ((k, label) in Prefs.WIDGET_SIZE_PARTS) {
            col.addView(seekRange(label, Prefs.partSize(this, k), 50, 300, "%") { Prefs.setPartSize(this, k, it); applyLook() })
        }
        }

        section("Text colours", "One colour for all text, or a colour for each part") {
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

        section("Wording & time stamp", "Empty message, Updated word, message time style") {
        // ---- Wording of the widget
        col.addView(title("Widget wording", 16f, 20))
        col.addView(hint("Change the text shown when there are no messages, and the word before the weather update time. Empty + Save = original wording. Colours: see Text colour per part below."))
        col.addView(toggleRaw("Show the \"Nothing new\" message in the widget", Prefs.showEmptyText(this)) { Prefs.setShowEmptyText(this, it); applyLook() })
        col.addView(textPref("Empty message text", Prefs.emptyText(this)) { Prefs.setEmptyText(this, it) })
        col.addView(textPref("Update word (before the time)", Prefs.updatedLabel(this)) { Prefs.setUpdatedLabel(this, it) })
        col.addView(seekRange("Size of the Updated line", Prefs.updSize(this), 50, 250, "%") { Prefs.setUpdSize(this, it); applyLook() })


        // ---- Message time stamp
        col.addView(title("Message time stamp", 16f, 20))
        col.addView(hint("The [time] in front of each message line. Smart: today shows only the time, yesterday shows Yesterday, older shows the date. It uses the same font, size and colour as the message text below."))
        col.addView(choiceRow(listOf("0" to "Smart", "1" to "Date + time", "2" to "Time only", "3" to "None"), Prefs.stampStyle(this).toString(), false) {
            Prefs.setStampStyle(this, it.toInt()); applyLook(); rebuild()
        })
        }

        section("Buttons: + Note, Clear all, Settings", "Colour of the three buttons") {
            col.addView(hint("One colour per button, used on the home-screen widget and in the panel together. Auto = follows the theme and the icon colour. Tap a row to pick."))
            for ((k, label) in listOf("note" to "+ Note", "clear" to "Clear all / Read all", "gear" to "Settings icon")) {
                val cur = Prefs.partColor(this, k).let { if (it != 0) it else Prefs.grpColor(this, if (k == "note") "hdr" else k) }
                col.addView(colorRow(label, cur) {
                    Prefs.setPartColor(this, k, it)
                    Prefs.setGrpColor(this, if (k == "note") "hdr" else k, it)
                    applyLook(); rebuild()
                })
            }
            col.addView(hint("Text size of these buttons in the panel: Text size per part > Text size per part of the panel."))
        }

        section("Shapes & borders", "Circle shape and size, icons, borders") {
        // ---- Sizes and shape
        col.addView(title("Sizes & shape", 16f, 20))
        col.addView(choiceRow(Look.SHAPES, Prefs.shape(this).toString(), false) {
            Prefs.setShape(this, it.toInt()); applyLook(); rebuild()
        })
        col.addView(seekRange("Clock / weather / date circles", Prefs.topSize(this), 36, 72, " dp") { Prefs.setTopSize(this, it) })
        col.addView(seekRange("Widget settings icon", Prefs.gearSize(this), 14, 40, " dp") { Prefs.setGearSize(this, it) })
        col.addView(toggleRaw("Refresh icon (centre of the widget)", Prefs.refreshShow(this)) { Prefs.setRefreshShow(this, it); applyLook() })
        col.addView(toggleRaw("Refresh icon at the top, right under the buttons (off: middle of the widget)", Prefs.refreshTop(this)) { Prefs.setRefreshTop(this, it); TilesWidget.rebuildAll(applicationContext); applyLook() })
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
        col.addView(hint("Each border has its own switch, thickness and colour. Thickness 0 = no border."))
        for ((kind, label) in listOf("panel" to "Panel", "tiles" to "Message tiles", "widget" to "Widget (home screen)")) {
            col.addView(title(label, 14f, 12))
            col.addView(toggleRaw(
                "Border around " + (if (kind == "panel") "the panel" else if (kind == "tiles") "message tiles" else "the widget (home screen)"),
                when (kind) { "panel" -> Prefs.borderPanel(this); "tiles" -> Prefs.borderTiles(this); else -> Prefs.borderWidget(this) }
            ) {
                when (kind) { "panel" -> Prefs.setBorderPanel(this, it); "tiles" -> Prefs.setBorderTiles(this, it); else -> Prefs.setBorderWidget(this, it) }
                if (kind == "widget") applyLook()
            })
            col.addView(seekRange("Thickness", Prefs.borderWidthOf(this, kind), 0, 6, " dp") { Prefs.setBorderWidthOf(this, kind, it) })
            col.addView(hint("Colour (first one = automatic)"))
            col.addView(swatchRow(borderColors, Prefs.borderColorOf(this, kind)) {
                Prefs.setBorderColorOf(this, kind, it); TilesWidget.refresh(applicationContext); rebuild()
            })
        }

        // ---- Message tiles (panel): roundness, fill colour, solidity, border style
        col.addView(title("Message tiles in the panel", 16f, 20))
        col.addView(hint("Extra looks for the message cards. Left alone, every tile looks exactly as before."))
        col.addView(seekRange("Corner roundness (0 = square, 16 = as designed)", Prefs.tileRadius(this).let { if (it >= 0) it else 16 }, 0, 30, " dp") { Prefs.setTileRadius(this, it) })
        col.addView(seekRange("Fill solidity (100 = as designed)", Prefs.tileFillAlpha(this), 20, 100, " %") { Prefs.setTileFillAlpha(this, it) })
        col.addView(hint("Tile colour (first one = each app's own colour)"))
        col.addView(swatchRow(bgColors, Prefs.tileFill(this)) { Prefs.setTileFill(this, it); rebuild() })
        col.addView(hint("Border style"))
        col.addView(smallBtn("Message tiles back to as designed") {
            Prefs.setTileRadius(this, -1); Prefs.setTileFill(this, 0); Prefs.setTileFillAlpha(this, 100); Prefs.setTileBorderStyle(this, 0); rebuild()
        })
        col.addView(smallBtn("Border style: " + listOf("Solid", "Dashed", "Dotted")[Prefs.tileBorderStyle(this).coerceIn(0, 2)] + "  (tap to change)") {
            Prefs.setTileBorderStyle(this, (Prefs.tileBorderStyle(this) + 1) % 3); rebuild()
        })

        }

        category("🧩  WIDGET AND PANEL", "What is on the widget, where it sits, fast dial and shortcuts")
        section("Arrange the widget", "Drag items, order of the circles, hide or show them") {
        // ---- Arrange the widget
        val dragBtn = Button(this)
        dragBtn.text = "Arrange & resize by dragging…"
        dragBtn.setOnClickListener { startActivity(Intent(this, ArrangeActivity::class.java).putExtra("scope", Prefs.SCOPE_SETTINGS)) }
        col.addView(dragBtn)
        col.addView(seekRange("Space between the widget rows", Prefs.rowGap(this), 0, 30, " dp") { Prefs.setRowGap(this, it); applyLook() })
        col.addView(hint("Top row, fast dial, shortcuts row and Inbox row. 6 dp = as designed. When items are placed freely, use Move > Tidy up: pack items closer instead."))
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
        col.addView(toggleRaw("Maps search box at the start (left)", Prefs.pillPos(this) == 0) { Prefs.setPillPos(this, if (it) 0 else 3); applyLook(); rebuild() })
        col.addView(toggle("Fast-dial circles", "dial"))

        }

        section("Clock & date", "12 / 24 hour and the date style") {
        col.addView(hint("Clock"))
        col.addView(choiceRow(listOf("0" to "Phone setting", "1" to "12-hour", "2" to "24-hour"), Prefs.clockFmt(this).toString(), false) {
            Prefs.setClockFmt(this, it.toInt()); applyLook(); rebuild()
        })
        col.addView(hint("Date"))
        col.addView(choiceRow(Prefs.DATE_FORMATS.mapIndexed { i, p -> i.toString() to p.second }, Prefs.dateFmt(this).toString(), false) {
            Prefs.setDateFmt(this, it.toInt()); applyLook(); rebuild()
        })
        }

        section("Weather", "City, source, temperature and the Updated line") {
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
        col.addView(title("Updated line (under the refresh icon)", 14f, 12))
        col.addView(toggleRaw("Show \"Updated\" time in the widget", Weather.showUpdated(this)) { Weather.setShowUpdated(this, it); TilesWidget.rebuildAll(applicationContext); applyLook(); rebuild() })
        col.addView(toggleRaw("Updated line: show the time (Updated 12:50 AM)", Weather.showUpdTime(this)) { Weather.setShowUpdTime(this, it); TilesWidget.rebuildAll(applicationContext); applyLook() })
        col.addView(toggleRaw("Updated line: show the source (Open-Meteo / Phone weather app)", Weather.showUpdSource(this)) { Weather.setShowUpdSource(this, it); TilesWidget.rebuildAll(applicationContext); applyLook() })
        col.addView(toggleRaw("Updated line: show the \"to match phone\" note", Weather.showUpdNote(this)) { Weather.setShowUpdNote(this, it); TilesWidget.rebuildAll(applicationContext); applyLook() })

        }

        section("Show / hide parts", "Switch any part of the widget on or off") {
        col.addView(hint("Switch off anything you do not want to see. Nothing is deleted: switch it on again any time. You can always open Look & colors from the Notif Tiles app itself."))
        col.addView(title("Widget top row and boxes", 16f, 12))
        col.addView(toggle("Clock circle", "clock"))
        col.addView(toggle("Weather circle", "weather"))
        col.addView(toggle("Date circle", "date"))
        col.addView(toggle("Maps \"Where to?\" box", "maps"))
        col.addView(toggle("Fast-dial strip", "dial"))
        col.addView(toggleRaw("Names under fast-dial circles", Prefs.dialNames(this)) { Prefs.setDialNames(this, it); applyLook() })
        col.addView(toggleRaw("Refresh icon (centre of the widget)", Prefs.refreshShow(this)) { Prefs.setRefreshShow(this, it); applyLook() })
        col.addView(toggleRaw("\"Updated\" time under the refresh icon", Weather.showUpdated(this)) { Weather.setShowUpdated(this, it); TilesWidget.rebuildAll(applicationContext); applyLook(); rebuild() })
        col.addView(toggleRaw("Updated line: show the time (Updated 12:50 AM)", Weather.showUpdTime(this)) { Weather.setShowUpdTime(this, it); TilesWidget.rebuildAll(applicationContext); applyLook() })
        col.addView(toggleRaw("Updated line: show the source (Open-Meteo / Phone weather app)", Weather.showUpdSource(this)) { Weather.setShowUpdSource(this, it); TilesWidget.rebuildAll(applicationContext); applyLook() })
        col.addView(toggleRaw("Updated line: show the \"to match phone\" note", Weather.showUpdNote(this)) { Weather.setShowUpdNote(this, it); TilesWidget.rebuildAll(applicationContext); applyLook() })
        col.addView(toggleRaw("\"Nothing new\" message", Prefs.showEmptyText(this)) { Prefs.setShowEmptyText(this, it); applyLook() })
        col.addView(title("Widget header, tiles and panel", 16f, 16))
        for ((k, label) in Prefs.SHOW_ITEMS) {
            col.addView(toggleRaw(label, Prefs.showItem(this, k)) { Prefs.setShowItem(this, k, it); applyLook() })
        }
        col.addView(hint("The mail icon changes with the Inbox (📬 → 📭 while it is empty). Your own emoji is shown as you picked it; set a separate one under 'when empty' to change it. How the icons and the panel animate is in the Animation section below."))
        col.addView(title("Only when there are messages", 16f, 16))
        col.addView(hint("A part set here shows while the Inbox has messages and goes away when it is empty (after Clear all, Read all or Mark as read). It comes back with the next message. The count badge already works this way. The Move screen always shows every part, so you can still place it."))
        for ((k, label) in Prefs.HIDE_EMPTY_PARTS) {
            col.addView(toggleRaw(label, Prefs.hideWhenEmpty(this, k)) { Prefs.setHideWhenEmpty(this, k, it); TilesWidget.rebuildAll(applicationContext); applyLook() })
        }

        }

        section("Animation", "Every animation in the panel: style, speed, on or off") {
        col.addView(hint("Pick a style for each part, or Off. The speed applies to all of them. Press an icon in the panel to see its animation. Widget icons have their own switch."))
        col.addView(Button(this).apply {
            isAllCaps = false
            text = "🎞️  Open the animation picker (tabs + live preview)"
            setOnClickListener { AnimPicker.show(this@SettingsActivity, { k, st -> if (Prefs.aliveOn(this@SettingsActivity)) live.demo(k, st) }, { rebuild() }) }
        })
        col.addView(toggleRaw("Animation (master switch)", Prefs.aliveOn(this)) { Prefs.setAliveOn(this, it); TilesWidget.rebuildAll(applicationContext); applyLook(); if (it) live.demo("open", Alive.chosen(this, "open")) })
        col.addView(toggleRaw("Panel animations", Prefs.alivePanel(this)) { Prefs.setAlivePanel(this, it); if (it) live.demo("open", Alive.chosen(this, "open")) })
        col.addView(toggleRaw("Widget: icons move when something happens", Prefs.aliveWave(this)) { Prefs.setAliveWave(this, it) })
        col.addView(title("Speed", 16f, 16))
        col.addView(choiceRow(listOf("0" to "Slow", "1" to "Normal", "2" to "Fast"), Prefs.animSpeed(this).toString(), false) { Prefs.setAnimSpeed(this, it.toInt()); rebuild(); live.demo("open", Alive.chosen(this, "open")) })
        for (e in Alive.ELEMENTS) {
            col.addView(title(e.label, 15f, 16))
            col.addView(choiceRow(e.styles + listOf("off" to "Off"), Alive.chosen(this, e.key), false) { Prefs.setAnimStyle(this, e.key, it); rebuild(); live.demo(e.key, it) })
        }
        }

        section("Fast dial", "Contacts, calling and the + circles") {
        // ---- Fast dial contacts
        col.addView(title("Fast-dial contacts", 16f, 20))
        col.addView(toggleRaw("Ask before calling (Call / Change / Remove)", Prefs.dialAsk(this)) { Prefs.setDialAsk(this, it) })
        col.addView(toggleRaw("Show the + (Add) circles for empty fast-dial places", Prefs.dialAdd(this)) { Prefs.setDialAdd(this, it); applyLook() })
        col.addView(hint("Put a wrong person on a circle? Change or clear it here. With \"Ask before calling\" on, tapping a circle on the widget also offers Change and Remove."))
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

        section("Shortcuts row (under fast dial)", "Apps and screens opened from circles under the fast dial") {
        col.addView(hint("A second row of circles under the fast dial. Each circle can open an app, one screen (activity) inside an app, or a shortcut an app offers (like a direct WhatsApp chat). Tap the Add circle on the widget, or use Pick here. Circle size, colour, shape and names follow the fast-dial settings."))
        col.addView(toggleRaw("Show the shortcuts row", Prefs.quickOn(this)) { Prefs.setQuickOn(this, it); applyLook() })
        col.addView(toggleRaw("Show the + (Add) circle in the shortcuts row", Prefs.quickAdd(this)) { Prefs.setQuickAdd(this, it); applyLook() })
        col.addView(toggleRaw("Pinned pictures sit inside the circle like other icons (off = picture fills the whole circle)", Prefs.picInside(this)) { Prefs.setPicInside(this, it); applyLook() })
        col.addView(toggleRaw("Free layout: after I choose a shortcut, hide the + circle (so no new item appears)", Prefs.keepPlusHidden(this)) { Prefs.setKeepPlusHidden(this, it) })
        for (i in 0 until Prefs.QUICK_SLOTS) {
            val q = Prefs.quick(this, i)
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val label = TextView(this)
            label.text = "${i + 1}.  " + (if (q == null) "Empty" else q.name + "  (" + (when (q.type) { "app" -> "app"; "act" -> "screen"; "pic" -> "pinned pic"; else -> "shortcut" }) + ")")
            label.textSize = 15f
            row.addView(label, LinearLayout.LayoutParams(0, WRAP, 1f))
            val ch = Button(this)
            ch.isAllCaps = false
            ch.text = if (q == null) "Pick" else "Change"
            ch.setOnClickListener {
                startActivity(Intent(this, QuickActivity::class.java).putExtra("slot", i).putExtra("pick", true))
            }
            row.addView(ch)
            if (q != null && q.type == "pic") {
                val ic = Button(this)
                ic.isAllCaps = false
                ic.text = "Icon"
                ic.setOnClickListener { EmojiPicker.pickQuickPic(this, i) { applyLook(); rebuild() } }
                row.addView(ic)
            }
            if (q != null) {
                val cl = Button(this)
                cl.isAllCaps = false
                cl.text = "Clear"
                cl.setOnClickListener {
                    Prefs.clearQuick(this, i)
                    PinnedPic.cleanUp(this)
                    applyLook()
                    rebuild()
                }
                row.addView(cl)
            }
            col.addView(row)
        }
        }

        category("💬  MESSAGES", "How notifications are grouped, paged, counted and silenced")
        section("Pages, shortcuts & grouping", "Scrolling or pages, grouping and order of the tiles") {
        col.addView(title("Scrolling", 16f, 12))
        col.addView(hint("Scroll = the list moves up and down (as before). My pages = pages you add yourself, each with its own apps and shortcuts, even when there are no messages. Pages = a few tiles at a time: ‹ › buttons under the widget; in the panel you can also swipe left or right. (A home-screen widget cannot be swiped sideways, that is an Android limit.)"))
        col.addView(choiceRow(listOf("0" to "Scroll up / down", "1" to "Pages", "2" to "My pages"), Prefs.scrollMode(this).toString(), false) {
            Prefs.setScrollMode(this, it.toInt()); Prefs.setPage(this, "w", 0); Prefs.setPage(this, "p", 0)
            if (it == "2" && Prefs.customPages(this).isEmpty()) Prefs.setCustomPages(this, listOf(CPage("Page 1", emptyList(), emptyList())))
            applyLook(); rebuild()
        })
        if (Prefs.scrollMode(this) == 2) pagesEditor()
        if (Prefs.scrollMode(this) == 1) {
            col.addView(seekRange("Tiles per page", Prefs.pageSize(this), 2, 20, "") {
                Prefs.setPageSize(this, it); Prefs.setPage(this, "w", 0); Prefs.setPage(this, "p", 0)
            })
        }
        col.addView(toggleRaw("Endless: pages wrap around, and the widget list repeats", Prefs.loop(this)) { Prefs.setLoop(this, it); applyLook() })
        col.addView(toggleRaw("Swipe-through: swiping down over the tiles reaches your launcher", Prefs.swipeThrough(this)) {
            Prefs.setSwipeThrough(this, it); TilesWidget.rebuildAll(applicationContext); applyLook()
        })
        col.addView(hint("On: the widget's tiles are fixed instead of a scrolling list, so your launcher (for example Nova's swipe down) still gets swipes that start on them. The widget then shows the first 8 tiles (or your \"Most tiles on the widget\" number, up to 12); use Pages to reach the rest. Tap the top line of a tile (app name and count) to open that message; swipe over the rest of the tile, the rows and the empty space to use your launcher gesture. Off: the list scrolls and tiles open as before."))
        if (Prefs.swipeThrough(this)) {
            col.addView(toggleRaw("Tap anywhere on a tile to open it (a swipe that starts on a tile then stays with the widget)", Prefs.swipeTapAll(this)) {
                Prefs.setSwipeTapAll(this, it); TilesWidget.rebuildAll(applicationContext); applyLook()
            })
            col.addView(toggleRaw("Tap the name line too (sender / chat name under the app name opens it; the rest still passes swipes)", Prefs.swipeTapName(this)) {
                Prefs.setSwipeTapName(this, it); TilesWidget.rebuildAll(applicationContext); applyLook()
            })
        }

        col.addView(toggleRaw("Back button in an opened message goes to the tiles first (off: Back closes the panel)", Prefs.backToTiles(this)) {
            Prefs.setBackToTiles(this, it)
        })

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

        section("Apps: pin, mute, colour", "Pin, mute and colour each app") {
        col.addView(hint("Pinned apps are listed first. Muted apps are hidden from the widget, the panel and the counts. A colour changes that app's tiles."))
        val pkgs = Prefs.enabled(this).sortedBy { Feed.label(this, it).lowercase() }
        if (pkgs.isEmpty()) col.addView(hint("No apps chosen yet. Choose them on the main screen first."))
        for (p in pkgs) {
            col.addView(title(Feed.label(this, p), 14f, 12))
            col.addView(toggleRaw("Pin to top", p in Prefs.pinned(this)) { Prefs.setPinned(this, p, it); applyLook() })
            col.addView(toggleRaw("Mute", p in Prefs.muted(this)) { Prefs.setMuted(this, p, it); applyLook() })
            col.addView(swatchRow(bgColors, Prefs.appColor(this, p)) { Prefs.setAppColor(this, p, it); applyLook(); rebuild() })
            col.addView(hint("Shortcuts for this app (shown on its card in the panel)"))
            for ((si, sc) in Prefs.appShortcuts(this, p).withIndex()) {
                val r = LinearLayout(this)
                r.orientation = LinearLayout.HORIZONTAL
                r.gravity = Gravity.CENTER_VERTICAL
                val nm = TextView(this)
                nm.text = sc.name + "  (" + (if (sc.type == "app") "app" else if (sc.type == "act") "screen" else if (sc.type == "pic") "pinned pic" else "shortcut") + ")"
                r.addView(nm, LinearLayout.LayoutParams(0, WRAP, 1f))
                r.addView(moveButton("Remove", true) { Prefs.removeAppShortcut(this, p, si); rebuild() })
                col.addView(r)
            }
            val addSc = Button(this)
            addSc.text = "Add a shortcut for " + Feed.label(this, p)
            addSc.setOnClickListener { startActivity(Intent(this, QuickActivity::class.java).putExtra("for_pkg", p)) }
            col.addView(addSc)
        }

        }

        section("Message buttons", "Order of the buttons under a message, hide the ones you do not use") {
        col.addView(hint("These are the buttons under each message in the panel. Tick = shown. Use the arrows to move a button up or down, or let them sort themselves with the most used first."))
        col.addView(toggleRaw("Sort by how often I use them (most used first)", Prefs.cardBtnAuto(this)) { Prefs.setCardBtnAuto(this, it); rebuild() })
        val order = Prefs.cardBtnOrder(this)
        val hidden = Prefs.cardBtnHidden(this)
        val uses = Prefs.cardBtnUses(this)
        val names = Prefs.CARD_BTNS.toMap()
        val manual = !Prefs.cardBtnAuto(this)
        for ((i, k) in order.withIndex()) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            val cb = android.widget.CheckBox(this)
            cb.text = (names[k] ?: k) + "  (used " + (uses[k] ?: 0) + "x)"
            cb.isChecked = k !in hidden
            cb.setOnCheckedChangeListener { _, v ->
                val h = Prefs.cardBtnHidden(this).toMutableSet()
                if (v) h.remove(k) else h.add(k)
                Prefs.setCardBtnHidden(this, h)
            }
            row.addView(cb, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            if (manual) {
                val up = Button(this)
                up.text = "▲"
                up.isEnabled = i > 0
                up.setOnClickListener {
                    val l = Prefs.cardBtnOrder(this).toMutableList()
                    val j = l.indexOf(k)
                    if (j > 0) { l.removeAt(j); l.add(j - 1, k); Prefs.setCardBtnOrder(this, l); rebuild() }
                }
                row.addView(up, LinearLayout.LayoutParams(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT))
                val down = Button(this)
                down.text = "▼"
                down.isEnabled = i < order.size - 1
                down.setOnClickListener {
                    val l = Prefs.cardBtnOrder(this).toMutableList()
                    val j = l.indexOf(k)
                    if (j >= 0 && j < l.size - 1) { l.removeAt(j); l.add(j + 1, k); Prefs.setCardBtnOrder(this, l); rebuild() }
                }
                row.addView(down, LinearLayout.LayoutParams(dp(52), ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            col.addView(row)
        }
        val resetUse = Button(this)
        resetUse.text = "Reset the use counts"
        resetUse.isAllCaps = false
        resetUse.setOnClickListener { Prefs.resetBtnUses(this); rebuild() }
        col.addView(resetUse)
        }

        section("Counts & clearing", "When the count drops and what Clear all does") {
        // ---- Counts
        col.addView(title("Counts", 16f, 20))
        col.addView(toggleRaw("Count drops after I open the app, mark read or tap the photo", Prefs.countRead(this)) { Prefs.setCountRead(this, it) })
        col.addView(toggleRaw("Mark as read also hides the same notification when the app posts it again with only the numbers changed (e.g. Facebook \"Waiting for you\")", Prefs.readRepeatNumbers(this)) { Prefs.setReadRepeatNumbers(this, it) })
        col.addView(toggleRaw("Count drops just from opening and closing the message card", Prefs.countLeave(this)) { Prefs.setCountLeave(this, it) })
        col.addView(toggleRaw("Count drops after I reply here", Prefs.countReply(this)) { Prefs.setCountReply(this, it) })
        col.addView(toggleRaw("Also remove it from the phone's notification panel", Prefs.dismissHandled(this)) { Prefs.setDismissHandled(this, it) })
        col.addView(hint("The Clear all button (widget and panel) does"))
        col.addView(choiceRow(listOf("0" to "Clear all", "1" to "Mark all read"), Prefs.clearMode(this).toString(), false) {
            Prefs.setClearMode(this, it.toInt()); applyLook(); rebuild()
        })
        col.addView(toggleRaw("Close the bottom popup after Clear all / Read all", Prefs.closeAfterClear(this)) { Prefs.setCloseAfterClear(this, it) })
        col.addView(hint("Mark all read counts every message as read and, with the option above, clears it from the phone's shade, the same as opening it here. The button then says Read all.\n\nWhen you read, reply to or mark a message as read here, its notification is cleared from the phone's shade too. A new message from the same chat counts again."))

        }

        section("Filters & limits", "Hide words, old messages, muted chats, VIP names, reply templates, fewer tiles") {
        col.addView(hint("These apply to the widget and to the panel. Nothing is deleted from your phone, the messages just stay out of the way."))
        col.addView(title("Hide messages containing these words", 14f, 12))
        col.addView(hint("Separate words with commas, for example: promo, offer, verification code. Capital letters do not matter."))
        col.addView(textPref("Words to hide", Prefs.muteWords(this)) { Prefs.setMuteWords(this, it) })
        col.addView(title("VIP names (always first)", 14f, 12))
        col.addView(hint("Messages whose title (the sender or chat name) contains one of these names are listed first, before pinned apps."))
        col.addView(textPref("VIP names", Prefs.vipNames(this)) { Prefs.setVipNames(this, it) })
        col.addView(title("Hide messages older than", 14f, 12))
        col.addView(choiceRow(listOf("0" to "Never", "1" to "1 hour", "6" to "6 hours", "24" to "1 day", "72" to "3 days"), Prefs.maxAgeHours(this).toString(), false) {
            Prefs.setMaxAgeHours(this, it.toInt()); applyLook(); rebuild()
        })
        col.addView(title("Most tiles on the widget", 14f, 12))
        col.addView(hint("A shorter list is lighter and faster. Only for the scrolling list; pages already show a few at a time."))
        col.addView(choiceRow(listOf("0" to "All", "5" to "5", "10" to "10", "20" to "20"), Prefs.maxTiles(this).toString(), false) {
            Prefs.setMaxTiles(this, it.toInt()); applyLook(); rebuild()
        })
        col.addView(title("Reply templates", 14f, 12))
        col.addView(hint("Quick replies shown above the reply box in the panel. One per line (up to 12). Tapping one only fills the box; nothing is sent until you tap Send. Leave empty for none."))
        col.addView(multiLinePref("Reply templates", Prefs.replyTemplatesRaw(this)) { Prefs.setReplyTemplates(this, it) })
        col.addView(title("Muted chats", 14f, 12))
        val mutedList = Prefs.mutedChats(this).sorted()
        col.addView(hint(if (mutedList.isEmpty()) "No chat is muted. In the panel, open a message and tap Mute chat to hide just that chat (the rest of the app stays). Muting only hides it here; nothing is deleted from your phone."
        else "These chats are hidden from the widget and the panel. Tap Unmute to bring one back."))
        for (key in mutedList) {
            val pkg = key.substringBefore("|")
            val chat = key.substringAfter("|", key)
            val b = Button(this)
            b.isAllCaps = false
            b.text = "Unmute  " + Feed.label(this, pkg) + " · " + chat
            b.setOnClickListener {
                Prefs.setChatMuted(this, key, false)
                applyLook(); rebuild()
            }
            col.addView(b)
        }
        }

        section("Quiet hours & snooze", "Silence the widget for a while or at night") {
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

        category("🛠  TOOLS", "Notes, voice recorder and Where to?")
        section("Notes", "The + Note button and your notes app") {
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

        section("Voice recorder", "The mic button and the helper") {
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

        section("Where to? (maps & location)", "Maps search, navigation and Location switching") {
        // ---- The icon of the Where to? box
        col.addView(title("Where to? icon (the pin)", 16f, 12))
        col.addView(hint("Now: " + when (Prefs.pinMode(this)) {
            1 -> "red pin"
            2 -> "emoji " + Prefs.emojiOf(this, "pin")
            3 -> if (Prefs.pinPic(this) != null) "my own picture" else "my own picture (not found, red pin shown)"
            else -> "Google Maps icon"
        }))
        col.addView(smallBtn("Change the Where to? icon") { EmojiPicker.pickPin(this, { pickPinPicture() }) { applyLook(); rebuild() } })
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
        col.addView(hint(if (LocationAuto.canWrite(this)) "Permission granted: Location is switched on and off by Notif Tiles with no pop-up. Location is switched off again once the Google Maps navigation notification is gone (if it was already on before, only with the next switch on)." else "Needs a one-time permission. Without it Android always shows its Turn on pop-up and Location stays on until you switch it off yourself. Run the command below once (Developer options only needed for that moment, you can switch them off afterwards). It stays through every update installed over the old version; only an uninstall removes it."))
        col.addView(toggleRaw("...even if Location was already on before", Prefs.locOffAlways(this)) { Prefs.setLocOffAlways(this, it) })
        val locTest = Button(this)
        locTest.isAllCaps = false
        locTest.text = "Test: switch Location off now"
        locTest.setOnClickListener {
            LocationAuto.testOff(this) { msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
        }
        col.addView(locTest)
        if (!LocationAuto.canWrite(this)) {
            val cp = Button(this)
            cp.isAllCaps = false
            cp.text = "Copy the adb command"
            cp.setOnClickListener {
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("adb", "adb shell pm grant com.example.notiftiles android.permission.WRITE_SECURE_SETTINGS"))
                Toast.makeText(this, "Copied. Paste it in Termux (adb connected).", Toast.LENGTH_LONG).show()
            }
            col.addView(cp)
        }
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

        category("💾  SYSTEM", "Backup, restore and reset")
        section("Backup & restore", "Save or load all your settings") {
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

        // Same reset as Pencil > Reset (same function, same full redraw), but it is kept in Settings' own undo list.
        val reset = Button(this)
        reset.text = "Reset everything to automatic"
        reset.isAllCaps = false
        reset.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage("Reset the widget to the standard layout and remove ALL themes, colours, fonts, sizes, shapes and moved places? Your apps, contacts, shortcuts, notes and message filters are kept. This is the same as Pencil > Reset, and you can undo it with Undo last change.")
                .setPositiveButton("Reset all") { _, _ ->
                    tracker.flush()
                    Prefs.resetAllLook(this, Prefs.SCOPE_SETTINGS)
                    tracker.resync()
                    TilesWidget.rebuildAll(applicationContext)
                    applyLook()
                    rebuild()
                    Toast.makeText(this, "Everything is back to default. Undo last change (at the top) brings it back.", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = dp(24)
        col.addView(reset, lp)
        col.addView(hint("Does the same as Pencil > Reset: back to the standard widget (top row, fast dial, shortcuts row, Inbox row). It removes themes, colours, fonts, sizes, shapes and moved places. Your apps, contacts, shortcuts, notes and message filters are kept. Custom background pictures are removed and are not brought back by Undo. Settings has its own Undo (button under the preview): it takes back every change made here, one step at a time, including a Reset. The pencil has a separate one."))

        category("📖  HELP", "The full user manual")
        section("User manual (read me)", "How to use everything in Notif Tiles") { Manual.addTo(this, col) }

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

    private fun multiLinePref(label: String, current: String, onSave: (String) -> Unit): View {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        val et = EditText(this)
        et.hint = label
        et.setText(current)
        et.minLines = 3
        et.maxLines = 8
        et.gravity = Gravity.TOP or Gravity.START
        et.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        row.addView(et, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, WRAP))
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
        c(0xFFFACC15), c(0xFF22D3EE), c(0xFF4ADE80), c(0xFFFB923C), c(0xFFF472B6),
        c(0xFFFF2BD6), c(0xFF00F5D4), c(0xFFA78BFA), c(0xFFFFD700), c(0xFFFF7F50), c(0xFF84CC16)
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

    /** The controls of one Settings row: the switch, the number box, the choice buttons and the colour swatches next to its name. */
    private class RowControls(
        val switch: android.widget.CompoundButton?, val number: EditText?,
        val choices: List<Button>, val swatches: List<TextView>
    )

    /** Every text on the Settings screen (short ones are what the helper looks at). */
    private fun collectLabels(v: View = col, out: MutableList<String> = ArrayList()): List<String> {
        if (v is TextView && v !is EditText && v.text.isNotBlank() && v.text.length < 160) out.add(v.text.toString())
        if (v is ViewGroup) for (i in 0 until v.childCount) collectLabels(v.getChildAt(i), out)
        return out
    }

    /** Opens the section whose name shares words with [text]. Returns its name, or null when none fits or it is open already. */
    private fun openSectionFor(text: String): String? {
        val skip = setOf("the", "and", "for", "with", "set", "change", "make", "move", "turn", "show", "hide", "this", "that", "its",
            "your", "all", "from", "into", "bigger", "smaller", "increase", "decrease", "please", "want", "can")
        val keys = text.lowercase().split(Regex("[^a-z]+")).filter { it.length >= 3 && it !in skip }.map { it.removeSuffix("s") }
        if (keys.isEmpty()) return null
        val best = allSections.maxByOrNull { name -> keys.count { name.lowercase().contains(it) } } ?: return null
        if (keys.none { best.lowercase().contains(it) } || best in openSections) return null
        openSections.add(best)
        return best
    }

    /** Scrolls to the row called [label] and highlights it for a moment. Returns false when it is not on the screen. */
    private fun revealRow(label: String): Boolean {
        val lbl = findLabel(col, label) ?: return false
        lbl.post {
            lbl.requestRectangleOnScreen(android.graphics.Rect(0, 0, lbl.width, lbl.height), true)
            lbl.setBackgroundColor(0x55FFEB3B)
            lbl.postDelayed({ lbl.setBackgroundColor(0) }, 2000)
        }
        return true
    }

    /** Opens the phone's speech input; the words go into the helper box. */
    private fun startVoice() {
        val i = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        i.putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Say the change")
        try {
            startActivityForResult(i, REQ_VOICE)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input is not available on this phone.", Toast.LENGTH_LONG).show()
        }
    }

    private fun rowControls(row: View): RowControls {
        val sw = ArrayList<android.widget.CompoundButton>()
        val nums = ArrayList<EditText>()
        val choices = ArrayList<Button>()
        val swatches = ArrayList<TextView>()
        fun scan(v: View, inSwatchRow: Boolean) {
            when {
                v is android.widget.CompoundButton -> sw.add(v)
                v is EditText -> if ((v.inputType and android.text.InputType.TYPE_CLASS_NUMBER) != 0) nums.add(v)
                v is Button -> choices.add(v)
                v is TextView && inSwatchRow && v.tag is Int -> swatches.add(v)
            }
            if (v is ViewGroup) {
                val next = inSwatchRow || v is HorizontalScrollView
                for (i in 0 until v.childCount) scan(v.getChildAt(i), next)
            }
        }
        if (row is android.widget.CompoundButton) sw.add(row)
        val p = row.parent as? ViewGroup
        if (p != null) {
            var idx = -1
            for (i in 0 until p.childCount) if (p.getChildAt(i) === row) idx = i
            var j = idx + 1
            // The controls sit in the next few views, up to the next bold title
            while (idx >= 0 && j < p.childCount && j <= idx + 3) {
                val sib = p.getChildAt(j)
                if (sib is TextView && sib !is Button && sib.typeface?.style == Typeface.BOLD) break
                scan(sib, false)
                j++
            }
        }
        return RowControls(sw.firstOrNull(), nums.firstOrNull(), choices, swatches)
    }

    private fun nearestSwatch(list: List<TextView>, argb: Int): TextView? {
        var best: TextView? = null
        var bestD = Long.MAX_VALUE
        for (v in list) {
            val c = v.tag as? Int ?: continue
            if (c == 0) continue
            val dr = ((c shr 16) and 0xFF) - ((argb shr 16) and 0xFF)
            val dg = ((c shr 8) and 0xFF) - ((argb shr 8) and 0xFF)
            val db = (c and 0xFF) - (argb and 0xFF)
            val d = (dr * dr + dg * dg + db * db).toLong()
            if (d < bestD) {
                bestD = d
                best = v
            }
        }
        return best
    }

    /** Runs a command sentence on the Settings controls. Returns (done, message). */
    private fun sentenceCommand(mode: String, target: String, value: String, labels: List<String>): Pair<Boolean, String> {
        val tl = Helper.bestLabel(target, labels) ?: return Pair(false, "I could not find a setting called \"$target\".")
        val row = findLabel(col, tl) ?: return Pair(false, "I could not find \"$tl\" on this screen.")
        val rc = rowControls(row)
        // A colour row (with a colour picker) takes a colour name straight to its own setter
        if (mode == "set") {
            val cb = (row.parent as? View)?.tag as? ((Int) -> Unit)
            if (cb != null) {
                val argb = if (value == "auto") 0 else value.split(" ").firstNotNullOfOrNull { Helper.colourOf(it) }
                if (argb != null) {
                    cb(argb)
                    return Pair(true, "\"$tl\" is now $value.")
                }
                return Pair(false, "Say a colour name such as red, blue or green for \"$tl\".")
            }
        }
        if (mode == "copy") {
            val vl = Helper.bestLabel(value, labels) ?: return Pair(false, "I could not find a setting called \"$value\".")
            val vrow = findLabel(col, vl) ?: return Pair(false, "I could not find \"$vl\" on this screen.")
            val vc = rowControls(vrow)
            val vsw = vc.switch
            if (vsw != null) {
                val s = rc.switch ?: return Pair(false, "\"$tl\" is not a switch, so it cannot match \"$vl\".")
                s.isChecked = vsw.isChecked
                return Pair(true, "\"$tl\" now matches \"$vl\".")
            }
            val vnum = vc.number
            if (vnum != null) {
                val n = rc.number ?: return Pair(false, "\"$tl\" has no number box, so it cannot match \"$vl\".")
                n.setText(vnum.text.toString())
                return Pair(true, "\"$tl\" now matches \"$vl\".")
            }
            val sel = vc.choices.firstOrNull { it.text.toString().startsWith("✓") }
            if (sel != null) {
                val name = sel.text.toString().removePrefix("✓ ").trim()
                val b = rc.choices.firstOrNull { it.text.toString().removePrefix("✓ ").trim().equals(name, true) && !Helper.isDestructive(name) }
                    ?: return Pair(false, "\"$tl\" has no \"$name\" choice.")
                b.performClick()
                return Pair(true, "\"$tl\" now matches \"$vl\" ($name).")
            }
            return Pair(false, "\"$vl\" has no value I can copy. Colours cannot be copied yet.")
        }
        // mode "set": on, off, a number, a choice name, or a colour
        val sw = rc.switch
        if (sw != null && (value == "on" || value == "off")) {
            sw.isChecked = value == "on"
            return Pair(true, "\"$tl\" is now $value.")
        }
        val num = rc.number
        val n = Helper.numberIn(value)
        if (num != null && n != null) {
            num.setText(n.toString())
            return Pair(true, "\"$tl\" is now $n.")
        }
        val btn = rc.choices.firstOrNull { b ->
            val bt = b.text.toString().removePrefix("✓ ").trim().lowercase()
            bt.isNotEmpty() && !Helper.isDestructive(bt) && (value == bt || value.split(" ").contains(bt))
        }
        if (btn != null) {
            btn.performClick()
            return Pair(true, "\"$tl\" is now ${btn.text.toString().removePrefix("✓ ").trim()}.")
        }
        if (rc.swatches.isNotEmpty()) {
            if (value == "auto") {
                val auto = rc.swatches.firstOrNull { it.text.toString() == "Auto" }
                if (auto != null) {
                    auto.performClick()
                    return Pair(true, "\"$tl\" is now automatic.")
                }
            }
            val argb = value.split(" ").firstNotNullOfOrNull { Helper.colourOf(it) }
            if (argb != null) {
                val pick = nearestSwatch(rc.swatches, argb)
                if (pick != null) {
                    pick.performClick()
                    return Pair(true, "\"$tl\" is now $value.")
                }
            }
        }
        return Pair(false, "I could not set \"$tl\" to \"$value\". It is shown below, so you can set it there.")
    }

    /** The view whose first line of text is [label] (a setting's name on the Settings screen), or null. */
    private fun findLabel(v: View, label: String): View? {
        if (v is TextView && v !is EditText && v.text.lineSequence().firstOrNull()?.trim() == label) return v
        if (v is ViewGroup) for (i in 0 until v.childCount) findLabel(v.getChildAt(i), label)?.let { return it }
        return null
    }

    /** The controls (switches, sliders, number boxes, buttons) under [v], in order. */
    private fun controls(v: View, out: MutableList<View>) {
        if (v is android.widget.CompoundButton || v is SeekBar || v is EditText || v is Button) { out.add(v); return }
        if (v is ViewGroup) for (i in 0 until v.childCount) controls(v.getChildAt(i), out)
    }

    /**
     * Changes the setting named [label] the way [input] asks, as if its control were tapped: a switch turns on or off,
     * a number box takes the number in the sentence. Returns false when there is no such control (the row is then shown instead).
     */
    private fun changeRow(label: String, input: String): Boolean {
        val lbl = findLabel(col, label) ?: return false
        var scope: View = lbl
        for (level in 0 until 4) {
            val p = scope.parent as? ViewGroup ?: break
            val found = ArrayList<View>()
            controls(p, found)
            if (found.isNotEmpty() && found.size <= 8) {
                for (ctl in found) {
                    if (ctl is android.widget.CompoundButton) {
                        ctl.isChecked = Helper.onOffIn(input) ?: !ctl.isChecked
                        return true
                    }
                }
                for (ctl in found) {
                    if (ctl is EditText && (ctl.inputType and android.text.InputType.TYPE_CLASS_NUMBER) != 0) {
                        val n = Helper.numberIn(input) ?: return false
                        ctl.setText(n.toString())
                        return true
                    }
                }
                return false
            }
            scope = p
        }
        return false
    }

    private fun seekRange(label: String, value: Int, min: Int, max: Int, unit: String, onChange: (Int) -> Unit): View {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val t = TextView(this)
        t.text = "$label: $value$unit"
        t.setPadding(0, dp(8), 0, 0)
        box.addView(t)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        val sb = SeekBar(this)
        sb.max = max - min
        sb.progress = (value - min).coerceIn(0, max - min)
        // A number box beside the slider: type a value (it moves the slider) or drag the slider (it fills the box)
        val num = EditText(this)
        num.setSingleLine(true)
        num.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        num.gravity = Gravity.CENTER
        num.setText(value.toString())
        num.setSelectAllOnFocus(true)
        var typing = false
        num.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val v = s?.toString()?.toIntOrNull() ?: return
                if (v < min || v > max || v == sb.progress + min) return
                typing = true
                sb.progress = v - min
                typing = false
                t.text = "$label: $v$unit"
                onChange(v)
                TilesWidget.refresh(applicationContext)
            }
        })
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                t.text = "$label: ${p + min}$unit"
                if (!typing && num.text.toString() != (p + min).toString()) num.setText((p + min).toString())
                if (fromUser) onChange(p + min)
            }

            override fun onStartTrackingTouch(s: SeekBar) {}

            override fun onStopTrackingTouch(s: SeekBar) {
                TilesWidget.refresh(applicationContext)
            }
        })
        row.addView(sb, LinearLayout.LayoutParams(0, WRAP, 1f))
        row.addView(num, LinearLayout.LayoutParams(dp(72), WRAP))
        box.addView(row)
        return box
    }

    /**
     * Runs one sentence typed into "Ask the helper". [said] is first put into plainer words by [Understand] (politeness, complaints such as
     * "the text is too small", number words), then tried against the Where to? icon, the helper's actions, the offline model and the setting rules in turn.
     */
    private fun runHelper(said: String) {
        val raw = Understand.rewrite(said)
        // The Where to? icon (the pin)
        val pin = Understand.pinIcon(this, raw)
        if (pin != null) {
            when (pin.openSlot) {
                "pin" -> EmojiPicker.pickPin(this, { pickPinPicture() }) { applyLook(); rebuild() }
                "pin_pic" -> pickPinPicture()
                else -> {
                    Toast.makeText(this, pin.message, Toast.LENGTH_LONG).show()
                    if (pin.changed) { applyLook(); rebuild() }
                }
            }
            return
        }
        // "it" and "that" mean the setting changed last
        var input = lastTarget?.let { t -> raw.replace(Regex("\\b(it|that)\\b", RegexOption.IGNORE_CASE), t) } ?: raw
        // Things the helper DOES: tidy the rows, add / change / remove fast dials and shortcuts, add a note
        // (the rules first, then the offline model's idea of what was meant)
        val act = HelperActions.handle(this, input) ?: Router.intent(this, input)?.let { HelperActions.handle(this, input, it) }
        if (act != null) {
            Toast.makeText(this, act.message, Toast.LENGTH_LONG).show()
            if (act.changed) { applyLook(); rebuild() }
            return
        }
        // Sentences the rules do not know: the offline model works out the meaning and rewrites it as a sentence they know
        if (Helper.parseSentence(input) == null && Helper.firstEmoji(input) == null && Helper.bestLabel(input, collectLabels()) == null) {
            Router.route(this, input)?.let { input = it }
        }
        val inWords = input.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }
        if (Helper.wordIn(inWords, "undo", "redo")) {
            val undoIt = Helper.wordIn(inWords, "undo")
            tracker.flush()
            val ok = if (undoIt) tracker.undo(1) else tracker.redo(1)
            applyLook()
            rebuild()
            Toast.makeText(this, if (ok) (if (undoIt) "Undone." else "Redone.") else (if (undoIt) "Nothing to undo." else "Nothing to redo."), Toast.LENGTH_SHORT).show()
            return
        }
        // Every text on the Settings screen, so the helper can find any setting by its name
        var labels = collectLabels()
        // A setting in a closed section is not on the screen yet: open the section it belongs to
        val probe = Helper.parseSentence(input)?.second ?: input
        if (Helper.bestLabel(probe, labels) == null) {
            val sec = openSectionFor(input)
            if (sec != null) {
                rebuild(sec)
                labels = collectLabels()
            }
        }
        // Layout sentences: gaps, sizes, rows, merging, show / hide
        val lay = LayoutHelper.handle(this, input)
        if (lay != null) {
            Toast.makeText(this, lay.message, Toast.LENGTH_LONG).show()
            if (lay.changed) {
                LayoutHelper.itemOf(input)?.let { lastTarget = it }
                rebuild()
            }
            return
        }
        // Any setting by its name: "dial size 50", "panel border off", "accent red", "make the widget more transparent"
        val ctl = HelperControl.handle(this, input)
        if (ctl != null) {
            Toast.makeText(this, ctl.message, Toast.LENGTH_LONG).show()
            if (ctl.changed) { applyLook(); rebuild() }
            return
        }
        // "change X to Y", "match X with Y", "turn X on": done on the screen's own controls first
        var note: String? = null
        val cmd = Helper.parseSentence(input)
        if (cmd != null && Helper.firstEmoji(input) == null) {
            val res = sentenceCommand(cmd.first, cmd.second, cmd.third, labels)
            if (res.first) {
                lastTarget = cmd.second
                Toast.makeText(this, res.second, Toast.LENGTH_LONG).show()
                rebuild()
                return
            }
            note = res.second
        }
        val r = Helper.handle(this, input, labels)
        val slot = r.openSlot
        val row = r.rowLabel
        when {
            slot != null -> EmojiPicker.pick(this, EmojiPicker.SLOTS.first { it.key == slot }) { applyLook(); rebuild() }
            row != null -> {
                if (changeRow(row, input)) {
                    Toast.makeText(this, "Changed: $row", Toast.LENGTH_LONG).show()
                    rebuild()
                } else {
                    val shown = revealRow(row)
                    Toast.makeText(this, note ?: (if (shown) "Found \"$row\" and highlighted it." else "I found \"$row\" but could not show it."), Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                val text = if (r.changed) r.message else (note ?: r.message)
                Toast.makeText(this, text, Toast.LENGTH_LONG).show()
                if (r.changed) rebuild() else searchBox.setText(r.search)
            }
        }
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

    /** Opens the gallery to choose the picture for the Where to? icon. */
    private fun pickPinPicture() {
        try {
            val i = Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
            startActivityForResult(Intent.createChooser(i, "Choose the Where to? icon"), REQ_PIN_PIC)
        } catch (e: Exception) {
            Toast.makeText(this, "No gallery app found", Toast.LENGTH_SHORT).show()
        }
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
        if (requestCode == REQ_VOICE && resultCode == RESULT_OK) {
            val heard = data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (heard != null) {
                helperBox?.setText(heard)
                Toast.makeText(this, "Heard: $heard. Press Make this change.", Toast.LENGTH_LONG).show()
            }
        }
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
        if (requestCode == REQ_PIN_PIC && resultCode == RESULT_OK && uri != null) {
            Thread {
                val id = PinnedPic.save(applicationContext, uri)
                if (id != null) {
                    Prefs.setPinPic(applicationContext, id)
                    Prefs.setPinMode(applicationContext, 3)
                    PinnedPic.cleanUp(applicationContext)
                    TilesWidget.rebuildAll(applicationContext)
                }
                runOnUiThread {
                    Toast.makeText(this, if (id != null) "Where to? icon set" else "Could not use that picture", Toast.LENGTH_SHORT).show()
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
        val rnd = Button(this)
        rnd.text = "🎲 Random"
        rnd.isAllCaps = false
        rnd.setOnClickListener {
            Themes.apply(this, Themes.LIST.drop(1).random())
            applyLook()
            rebuild()
        }
        row.addView(rnd)
        for (t in Themes.LIST) {
            val b = Button(this)
            b.text = t.name
            b.isAllCaps = false
            if (t.panel != 0) {
                // each theme button shows the theme's own colours
                val bgc = t.panel or 0xFF000000.toInt()
                val g = GradientDrawable()
                g.cornerRadius = dp(10).toFloat()
                g.setColor(bgc)
                g.setStroke(dp(2), t.accent or 0xFF000000.toInt())
                b.background = g
                b.setTextColor(Look.contrast(bgc))
            }
            b.setOnClickListener {
                Themes.apply(this, t)
                applyLook()
                rebuild()
            }
            row.addView(b)
        }
        return hs
    }

    // ---- My pages editor ----
    private fun smallBtn(label: String, action: () -> Unit): View {
        val b = Button(this)
        b.text = label
        b.isAllCaps = false
        b.minWidth = 0
        b.minimumWidth = 0
        b.setOnClickListener { action() }
        return b
    }

    private fun savePages(l: List<CPage>) {
        Prefs.setCustomPages(this, l)
        applyLook(); rebuild()
    }

    private fun pagesEditor() {
        val pages = Prefs.customPages(this)
        col.addView(title("My pages", 16f, 20))
        col.addView(hint("Add pages any time, even with no messages. Put apps on a page (their messages show there), add app shortcuts, and move pages left or right. Apps you do not put on a page show on page 1. In the panel: long-press a tile to move its app to another page, and use + in the page bar to add a page. The widget changes page with its ‹ › buttons; the panel also swipes left and right. Inside a page the list scrolls up and down."))
        for ((i, p) in pages.withIndex()) {
            col.addView(title("${i + 1}. ${p.name}", 15f, 14))
            val apps = if (p.apps.isEmpty()) "none" else p.apps.joinToString { Feed.label(this, it) }
            val sc = if (p.shortcuts.isEmpty()) "none" else p.shortcuts.joinToString { Feed.label(this, it) }
            col.addView(hint("Apps: $apps\nShortcuts: $sc"))
            val r1 = LinearLayout(this)
            r1.orientation = LinearLayout.HORIZONTAL
            r1.addView(smallBtn("Apps") { pickPageApps(i) })
            r1.addView(smallBtn("Shortcuts") { pickPageShortcuts(i) })
            r1.addView(smallBtn("Rename") { askPageName(p.name) { n -> savePages(pages.mapIndexed { j, q -> if (j == i) q.copy(name = n) else q }) } })
            col.addView(r1)
            val r2 = LinearLayout(this)
            r2.orientation = LinearLayout.HORIZONTAL
            r2.addView(moveButton("‹ Left", i > 0) { swapPages(pages, i, i - 1) })
            r2.addView(moveButton("Right ›", i < pages.size - 1) { swapPages(pages, i, i + 1) })
            r2.addView(smallBtn("Delete") {
                AlertDialog.Builder(this).setTitle("Delete page \"${p.name}\"?")
                    .setMessage("Its apps go back to page 1. Nothing else is lost.")
                    .setPositiveButton("Delete") { _, _ ->
                        Prefs.setPage(this, "w", 0); Prefs.setPage(this, "p", 0)
                        savePages(pages.filterIndexed { j, _ -> j != i })
                    }.setNegativeButton("Cancel", null).show()
            })
            col.addView(r2)
        }
        val add = Button(this)
        add.isAllCaps = false
        add.text = "+ Add page"
        add.setOnClickListener {
            askPageName("") { n -> savePages(pages + CPage(n.ifEmpty { "Page ${pages.size + 1}" }, emptyList(), emptyList())) }
        }
        col.addView(add)
    }

    private fun askPageName(current: String, done: (String) -> Unit) {
        val et = EditText(this)
        et.setText(current)
        et.hint = "Page name"
        AlertDialog.Builder(this).setTitle("Page name").setView(et)
            .setPositiveButton("OK") { _, _ -> done(et.text.toString().trim()) }
            .setNegativeButton("Cancel", null).show()
    }

    private fun swapPages(pages: List<CPage>, a: Int, b: Int) {
        val l = pages.toMutableList()
        val t = l[a]; l[a] = l[b]; l[b] = t
        Prefs.setPage(this, "w", 0); Prefs.setPage(this, "p", 0)
        savePages(l)
    }

    private fun pickPageApps(i: Int) {
        val pages = Prefs.customPages(this)
        val pkgs = Prefs.enabled(this).sortedBy { Feed.label(this, it).lowercase() }
        if (pkgs.isEmpty()) { Toast.makeText(this, "No apps chosen yet. Choose them on the main screen first.", Toast.LENGTH_LONG).show(); return }
        val checked = BooleanArray(pkgs.size) { pkgs[it] in pages[i].apps }
        AlertDialog.Builder(this).setTitle("Apps on \"${pages[i].name}\"")
            .setMultiChoiceItems(pkgs.map { Feed.label(this, it) }.toTypedArray(), checked) { _, which, on -> checked[which] = on }
            .setPositiveButton("OK") { _, _ ->
                val chosen = pkgs.filterIndexed { k, _ -> checked[k] }
                // An app lives on one page: choosing it here takes it off the others.
                savePages(pages.mapIndexed { j, p ->
                    if (j == i) p.copy(apps = p.apps.filter { it !in pkgs } + chosen)
                    else p.copy(apps = p.apps.filter { it !in chosen })
                })
            }.setNegativeButton("Cancel", null).show()
    }

    private fun pickPageShortcuts(i: Int) {
        val pages = Prefs.customPages(this)
        val launch = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val all = packageManager.queryIntentActivities(launch, 0).map { it.activityInfo.packageName }.distinct()
            .filter { it != packageName }.sortedBy { Feed.label(this, it).lowercase() }
        if (all.isEmpty()) { Toast.makeText(this, "No apps found", Toast.LENGTH_SHORT).show(); return }
        val checked = BooleanArray(all.size) { all[it] in pages[i].shortcuts }
        AlertDialog.Builder(this).setTitle("Shortcuts on \"${pages[i].name}\"")
            .setMultiChoiceItems(all.map { Feed.label(this, it) }.toTypedArray(), checked) { _, which, on -> checked[which] = on }
            .setPositiveButton("OK") { _, _ ->
                val chosen = all.filterIndexed { k, _ -> checked[k] }
                savePages(pages.mapIndexed { j, p -> if (j == i) p.copy(shortcuts = p.shortcuts.filter { it !in all } + chosen) else p })
            }.setNegativeButton("Cancel", null).show()
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
        row.tag = onPick
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
            tv.tag = color
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

    private fun seek(label: String, value: Int, onChange: (Int) -> Unit): View =
        seekRange(label, value, 0, 100, "%", onChange)
}
