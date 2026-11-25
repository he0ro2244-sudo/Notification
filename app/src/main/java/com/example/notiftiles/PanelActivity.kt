package com.example.notiftiles

import android.app.Activity
import android.app.ActivityOptions
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
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
    private lateinit var back: TextView
    private lateinit var titleView: TextView
    private var aliveIcons: List<View> = emptyList()
    private var expanded: String? = null
    private var dark = false

    private var cBg = 0
    private var cText = 0
    private var cSub = 0
    private var viewing: StatusBarNotification? = null

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, r: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(r).toFloat()
    }

    private fun tileBg(bg: Int, fg: Int, r: Int): GradientDrawable {
        // Message tile extras: own corner roundness, own fill colour / solidity, solid / dashed / dotted border
        val fill = Prefs.tileFill(this).let { if (it != 0) it else bg }
        val fa = Prefs.tileFillAlpha(this)
        val fillC = if (fa < 100) Feed.alpha(fill, (0xFF * fa / 100).coerceIn(0, 0xFF)) else fill
        val rad = Prefs.tileRadius(this).let { if (it >= 0) it else r }
        val d = rounded(fillC, rad)
        if (Prefs.borderTiles(this) && Prefs.borderWidthOf(this, "tiles") > 0) {
            val w = dp(Prefs.borderWidthOf(this, "tiles")).coerceAtLeast(1)
            val col = Look.borderColorOf(this, "tiles", Feed.alpha(fg, 0x99))
            when (Prefs.tileBorderStyle(this)) {
                1 -> d.setStroke(w, col, w * 4f, w * 3f)
                2 -> d.setStroke(w, col, w.toFloat(), w * 2f)
                else -> d.setStroke(w, col)
            }
        }
        return d
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark = Feed.isDark(this)
        if (Prefs.panelBg(this) != 0) dark = android.graphics.Color.luminance(Prefs.panelBg(this)) < 0.5f
        cBg = Prefs.panelBgColor(this)
        cText = if (dark) 0xFFECECEF.toInt() else 0xFF1B1B1F.toInt()
        cSub = if (dark) 0xFFA8A8B0.toInt() else 0xFF5E5E66.toInt()
        ActionLog.add(this, "panel opened by a tap: " + (intent?.getStringExtra("key") ?: intent?.getStringExtra("launch") ?: "no message"))
        intent?.getStringExtra("launch")?.let { launchApp(it); finish(); return }
        expanded = intent?.getStringExtra("key")
        if (expanded == "note:new" && openNotesDirect()) expanded = null

        val root = FrameLayout(this)
        root.setBackgroundColor(Prefs.scrimArgb(this))
        root.setOnClickListener { finish() }
        root.setOnApplyWindowInsetsListener { v, insets ->
            val b = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
            v.setPadding(0, b.top, 0, b.bottom)
            insets
        }

        val sheet = LinearLayout(this)
        sheet.orientation = LinearLayout.VERTICAL
        sheet.background = Look.panelBackground(this, cBg, dp(24))
        sheet.setPadding(dp(12), dp(12), dp(12), dp(8))
        sheet.setOnClickListener { }
        val lp = FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM)
        lp.setMargins(dp(8), dp(8), dp(8), dp(8))
        root.addView(sheet, lp)

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.setPadding(dp(4), 0, dp(4), dp(8))

        back = TextView(this)
        back.text = "‹ Back"
        back.textSize = 15f
        back.setTag(R.id.nt_grp, "head")
        back.setTextColor(Prefs.accent(this))
        back.setTypeface(null, Typeface.BOLD)
        back.setPadding(0, dp(6), dp(12), dp(6))
        back.visibility = View.GONE
        back.setOnClickListener {
            finishViewing()
            expanded = null
            refresh()
        }
        header.addView(back)

        val title = TextView(this)
        titleView = title
        val noMsgs = Feed.items(this, true).isEmpty()
        title.text = Look.inboxLabel(this, noMsgs)
        title.textSize = 20f
        title.setTag(R.id.nt_grp, "head")
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(cText)
        title.visibility = if (Prefs.showItem(this, "p_title")) View.VISIBLE else View.GONE
        header.addView(title)

        badge = TextView(this)
        badge.textSize = 12f
        badge.setTag(R.id.nt_grp, "small")
        badge.setTypeface(null, Typeface.BOLD)
        badge.setTextColor(0xFFFFFFFF.toInt())
        badge.background = rounded(Prefs.accent(this), 10)
        badge.setPadding(dp(8), dp(2), dp(8), dp(2))
        val blp = LinearLayout.LayoutParams(WRAP, WRAP)
        blp.marginStart = dp(8)
        header.addView(badge, blp)

        header.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))



        val addNote = TextView(this)
        addNote.text = Look.noteLabel(this, noMsgs)
        addNote.textSize = 14f
        addNote.setTag(R.id.nt_grp, "hdr")
        addNote.setTypeface(null, Typeface.BOLD)
        addNote.setTextColor(Prefs.accent(this))
        addNote.setPadding(dp(8), dp(6), dp(8), dp(6))
        addNote.visibility = if (Prefs.showItem(this, "p_note")) View.VISIBLE else View.GONE
        addNote.setOnClickListener {
            if (openNotesDirect()) return@setOnClickListener
            expanded = "note:new"
            refresh()
        }
        header.addView(addNote)

        val clear = TextView(this)
        clear.text = Look.withEmoji(Prefs.emojiFor(this, "clear", noMsgs), if (Prefs.clearMode(this) == 1) "Read all" else "Clear all")
        clear.textSize = 14f
        clear.setTag(R.id.nt_grp, "clear")
        clear.setTextColor(if (Prefs.iconColor(this) != 0) Prefs.iconColor(this) else cSub)
        clear.setPadding(dp(8), dp(6), dp(8), dp(6))
        clear.visibility = if (Prefs.showItem(this, "p_clear")) View.VISIBLE else View.GONE
        clear.setOnClickListener {
            if (Prefs.clearMode(this) == 1) {
                Extras.markAllRead(this)
                TilesWidget.refresh(applicationContext)
                refresh()
            } else {
                clearAll()
            }
            if (Prefs.closeAfterClear(this)) finish()
        }
        header.addView(clear)

        // Extra header buttons (hidden until switched on in Settings > Show / hide parts)
        fun extraBtn(key: String, show: String, glyph: String, emojiKey: String, onTap: () -> Unit) {
            val b = TextView(this)
            b.text = Prefs.emojiOf(this, emojiKey).ifEmpty { glyph }
            b.textSize = 18f
            b.setTag(R.id.nt_grp, "hdr")
            b.setTextColor(if (Prefs.iconColor(this) != 0) Prefs.iconColor(this) else cSub)
            b.setPadding(dp(8), dp(4), dp(8), dp(4))
            b.visibility = if (Prefs.showItem(this, show)) View.VISIBLE else View.GONE
            b.setOnClickListener { onTap() }
            header.addView(b)
        }
        extraBtn("mic", "p_mic", "🎤", "mic") {
            startActivity(Intent(this, RecordActivity::class.java))
            finish()
        }
        extraBtn("edit", "p_edit", "✎", "edit") {
            startActivity(Intent(this, EditMenuActivity::class.java))
            finish()
        }
        extraBtn("refresh", "p_refresh", "↻", "reset") {
            sendBroadcast(Intent(this, TilesWidget::class.java).setAction(TilesWidget.ACTION_REFRESH_ALL))
            refresh()
        }

        val gear = ImageView(this)
        gear.setImageResource(R.drawable.ic_gear)
        val gsz = dp((22 * Prefs.grpSize(this, "gear") / 100f).toInt().coerceAtLeast(10))
        gear.setColorFilter(Prefs.grpColor(this, "gear").let { if (it != 0) it else if (Prefs.iconColor(this) != 0) Prefs.iconColor(this) else cSub })
        Prefs.emojiOf(this, "gear").let { eg ->
            if (eg.isNotEmpty()) Look.emojiBitmap(this, eg, gsz / resources.displayMetrics.density)?.let {
                gear.setImageBitmap(it)
                gear.clearColorFilter()
            }
        }
        gear.setPadding(dp(4), dp(2), dp(2), dp(2))
        gear.visibility = if (Prefs.showItem(this, "p_gear")) View.VISIBLE else View.GONE
        gear.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
        header.addView(gear, LinearLayout.LayoutParams(gsz + dp(6), gsz + dp(4)))
        sheet.addView(header)
        // Alive icons: the header icons that pop when the panel opens
        aliveIcons = (0 until header.childCount).map { header.getChildAt(it) }
            .filter { (it is TextView || it is ImageView) && it !== back && it !== titleView }

        val scroll = ScrollView(this)
        list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        scroll.addView(list)
        sheet.addView(scroll, LinearLayout.LayoutParams(MATCH, WRAP))

        setContentView(root)
        Look.applyTree(root, this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        ActionLog.add(this, "panel opened by a tap: " + (intent.getStringExtra("key") ?: intent.getStringExtra("launch") ?: "no message"))
        intent.getStringExtra("launch")?.let { launchApp(it); return }
        finishViewing()
        expanded = intent.getStringExtra("key")
        if (expanded == "note:new" && openNotesDirect()) expanded = null
        refresh()
    }

    private fun openChat(sbn: StatusBarNotification, msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        try {
            sbn.notification.contentIntent?.send()
        } catch (e: Exception) {
        }
    }

    // Plays the video inside the card, with the usual play/pause bar.
    private fun playVideo(box: FrameLayout, uri: android.net.Uri, sbn: StatusBarNotification) {
        box.removeAllViews()
        val vv = android.widget.VideoView(this)
        box.addView(vv, FrameLayout.LayoutParams(MATCH, dp(260)))
        val mc = android.widget.MediaController(this)
        mc.setAnchorView(vv)
        vv.setMediaController(mc)
        vv.setOnPreparedListener { vv.start() }
        vv.setOnErrorListener { _, _, _ ->
            openChat(sbn, "Can't play this video here, opening the app")
            true
        }
        vv.setVideoURI(uri)
    }

    /** "+ Note" with the straight-to-notes-app option on: opens the notes app and closes the panel. */
    private fun openNotesDirect(): Boolean {
        if (!Notes.direct(this) || Notes.keepInApp(this)) return false
        if (Notes.needsPick(this)) {
            Notes.pickDialog(this) { if (Notes.openNew(this)) finish() }
            return true
        }
        if (Notes.openNew(this)) {
            finish()
            return true
        }
        return false
    }

    // The phone's Back button closes the panel; with "Back goes to the tiles first" on, an opened message goes back to the tiles first
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (expanded != null && Prefs.backToTiles(this)) {
            finishViewing()
            expanded = null
            refresh()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        Access.rebindIfNeeded(this)
        NotifListener.onChange = { runOnUiThread { if (!hasDraft() && screenSig() != lastSig) refresh() } }
        refresh()
        // Alive icons: the title breathes while there are messages, the other icons pop once
        if (Feed.items(this, true).isNotEmpty()) Alive.breathe(this, titleView) else Alive.stop(titleView)
        Alive.pop(this, aliveIcons)
    }

    override fun onPause() {
        super.onPause()
        finishViewing()
        NotifListener.onChange = null
        Alive.stop(titleView)
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
        t.setTag(R.id.nt_grp, "text")
        t.setTextColor(cSub)
        t.setPadding(dp(8), dp(12), dp(8), dp(16))
        return t
    }

    // Removes a handled notification from the phone's shade too (only ones Android lets you swipe away).
    private fun dismissFromShade(sbn: StatusBarNotification, delayMs: Long = 0L) {
        if (!Prefs.dismissHandled(this) || !sbn.isClearable) return
        val key = sbn.key
        if (delayMs <= 0L) {
            Feed.cancelKeys(NotifListener.instance, listOf(key))
        } else {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                Feed.cancelKeys(NotifListener.instance, listOf(key))
            }, delayMs)
        }
    }

    // Leaving an opened message counts it as read: it stops being counted until something newer arrives.
    private fun finishViewing() {
        val v = viewing ?: return
        viewing = null
        if (Prefs.countRead(this) && Prefs.countLeave(this)) {
            Feed.markDone(this, v, Feed.handledStamp(v))
            dismissFromShade(v)
            TilesWidget.refresh(applicationContext)
        }
    }

    // What the panel shows, in short: lets a notification update that changes nothing visible skip the redraw (no flicker)
    private var lastSig = ""
    private fun screenSig(): String = try {
        val svc = NotifListener.instance
        val items = if (svc == null) emptyList() else Feed.items(this, true)
        expanded + "|" + Prefs.notes(this).size + "|" + items.joinToString(";") {
            it.key + "," + it.postTime + "," + (it.notification.extras.getCharSequence("android.text")?.toString()?.hashCode() ?: 0) + "," + it.notification.number
        }
    } catch (e: Exception) {
        System.nanoTime().toString()
    }

    private fun refresh() {
        refreshInner()
        Look.applyTree(list, this)
        lastSig = screenSig()
    }

    private fun refreshInner() {
        list.removeAllViews()
        val svc = NotifListener.instance
        val items = if (svc == null) emptyList() else Feed.items(this, true)
        val notes = Prefs.notes(this)
        badge.text = items.size.toString()
        badge.visibility = if (items.isEmpty() || !Prefs.showItem(this, "p_badge")) View.GONE else View.VISIBLE

        val key = expanded
        if (key != null) {
            viewing = if (key.startsWith("note:")) null else items.firstOrNull { it.key == key }
            val view: View? = when {
                key == "note:new" -> composer(null)
                key.startsWith("note:") ->
                    notes.firstOrNull { "note:" + it.id == key }?.let { composer(it) }
                else ->
                    items.firstOrNull { it.key == key }
                        ?.let { s -> expandedCard(s, items.count { x -> x.packageName == s.packageName }) }
            }
            if (view != null) {
                back.visibility = if (Prefs.showItem(this, "p_back")) View.VISIBLE else View.GONE
                list.addView(view)
                return
            }
            expanded = null
            viewing = null
        }
        back.visibility = View.GONE

        if (svc == null) {
            list.addView(
                note(
                    if (Access.enabled(this)) "Connecting to notifications…"
                    else "Notification access is off. Open Notif Tiles setup and turn it on."
                )
            )
        }
        val allTiles = Arrange.tiles(this, items, 3)
        val pagesOn = Prefs.pagesOn(this)
        val cells = ArrayList<View>()
        for (t in Arrange.slice(this, "p", allTiles)) cells.add(compact(t.sbn, t.count, t.title, t.text))
        for (pkg in Arrange.shortcuts(this, "p")) cells.add(shortcutTile(pkg))
        val onLastPage = Arrange.pageNow(this, "p", allTiles.size) >= Arrange.pageCount(this, allTiles.size) - 1
        if (!pagesOn || onLastPage) for (n in notes) cells.add(noteTile(n))
        if (cells.isEmpty()) {
            if (svc != null) list.addView(note("Nothing new from your apps. Tap + Note to write something down."))
            if (pagesOn && Arrange.customOn(this)) list.addView(pageBar(allTiles.size))
            return
        }
        var i = 0
        while (i < cells.size) {
            list.addView(rowOf(cells[i], cells.getOrNull(i + 1)))
            i += 2
        }
        if (pagesOn && (Arrange.pageCount(this, allTiles.size) > 1 || Prefs.loop(this) || Arrange.customOn(this))) list.addView(pageBar(allTiles.size))
    }

    // ‹ 1/3 › under the tiles when pages are on (swiping left / right does the same)
    private fun pageBar(total: Int): View {
        val n = Arrange.pageCount(this, total)
        val now = Arrange.pageNow(this, "p", total)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        fun btn(t: String, delta: Int): TextView {
            val b = TextView(this)
            b.text = t
            b.textSize = 24f
            b.setTextColor(Prefs.accent(this))
            b.setPadding(dp(24), dp(2), dp(24), dp(2))
            b.setOnClickListener {
                Arrange.move(this, "p", total, delta)
                refresh()
            }
            return b
        }
        row.addView(btn("‹", -1))
        val label = TextView(this)
        label.text = (Arrange.pageName(this, "p")?.let { "$it  " } ?: "") + "${now + 1}/$n"
        label.textSize = 13f
        label.setTextColor(cSub)
        label.setTag(R.id.nt_grp, "small")
        row.addView(label)
        row.addView(btn("›", 1))
        if (Arrange.customOn(this)) {
            val plus = TextView(this)
            plus.text = "＋"
            plus.textSize = 22f
            plus.setTextColor(Prefs.accent(this))
            plus.setPadding(dp(16), dp(2), dp(16), dp(2))
            plus.setOnClickListener { addPageDialog() }
            row.addView(plus)
        }
        return row
    }

    // ---- My pages helpers ----
    private fun launchApp(pkg: String) {
        packageManager.getLaunchIntentForPackage(pkg)?.let {
            try { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {}
        }
    }

    private fun addPageDialog() {
        val et = EditText(this)
        et.hint = "Page name"
        android.app.AlertDialog.Builder(this).setTitle("Add a page").setView(et)
            .setPositiveButton("Add") { _, _ ->
                Arrange.addPage(this, et.text.toString())
                Prefs.setPage(this, "p", Prefs.customPages(this).size - 1)
                TilesWidget.refresh(applicationContext)
                refresh()
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun movePicker(pkg: String) {
        val names = Prefs.customPages(this).map { it.name }.toTypedArray()
        if (names.isEmpty()) return
        android.app.AlertDialog.Builder(this).setTitle("Move " + Feed.label(this, pkg) + " to page")
            .setItems(names) { _, i ->
                Arrange.moveApp(this, pkg, i)
                TilesWidget.refresh(applicationContext)
                refresh()
            }.show()
    }

    private fun shortcutTile(pkg: String): View {
        val st = Feed.style(this, pkg)
        val bg = st.bg(dark)
        val fg = st.fg(dark)
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = tileBg(bg, fg, 16)
        card.setPadding(dp(10), dp(10), dp(10), dp(10))
        card.minimumHeight = dp(104)
        appIcon(pkg, 28)?.let { card.addView(it) }
        val t = TextView(this)
        t.text = Feed.label(this, pkg)
        t.textSize = 14f
        t.setTypeface(null, Typeface.BOLD)
        t.setTextColor(fg)
        t.maxLines = 1
        t.ellipsize = TextUtils.TruncateAt.END
        t.setPadding(0, dp(8), 0, 0)
        card.addView(t)
        val m = TextView(this)
        m.text = "Shortcut"
        m.textSize = 12f
        m.setTextColor(Feed.alpha(fg, 0xDD))
        card.addView(m)
        card.setOnClickListener { launchApp(pkg) }
        card.setOnLongClickListener {
            android.app.AlertDialog.Builder(this).setTitle("Remove " + Feed.label(this, pkg) + " shortcut?")
                .setPositiveButton("Remove") { _, _ ->
                    Arrange.removeShortcut(this, Arrange.pageNow(this, "p", 0), pkg)
                    TilesWidget.refresh(applicationContext)
                    refresh()
                }.setNegativeButton("Cancel", null).show()
            true
        }
        return card
    }

    private val swipe by lazy {
        android.view.GestureDetector(this, object : android.view.GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: android.view.MotionEvent?, e2: android.view.MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (!Prefs.pagesOn(this@PanelActivity) || expanded != null) return false
                val dx = e2.x - (e1?.x ?: e2.x)
                if (Math.abs(dx) > dp(80) && Math.abs(velocityX) > Math.abs(velocityY) * 1.5f) {
                    val total = Arrange.tileCount(this@PanelActivity, Feed.items(this@PanelActivity, true))
                    Arrange.move(this@PanelActivity, "p", total, if (dx < 0) 1 else -1)
                    refresh()
                    return true
                }
                return false
            }
        })
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean {
        swipe.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun rowOf(a: View, b: View?): View {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        val rlp = LinearLayout.LayoutParams(MATCH, WRAP)
        rlp.bottomMargin = dp(8)
        r.layoutParams = rlp
        r.addView(a, cell(true))
        r.addView(b ?: View(this), cell(false))
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
        t.setTag(R.id.nt_grp, "small")
        t.setTypeface(null, Typeface.BOLD)
        t.setTextColor(bg)
        t.background = rounded(fg, 10)
        t.setPadding(dp(7), dp(1), dp(7), dp(1))
        return t
    }

    private fun compact(sbn: StatusBarNotification, count: Int, title: String? = null, text: String? = null): View {
        val st = Feed.style(this, sbn.packageName)
        val bg = st.bg(dark)
        val fg = st.fg(dark)
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = tileBg(bg, fg, 16)
        card.setPadding(dp(10), dp(10), dp(10), dp(10))
        card.minimumHeight = dp(104)

        val head = LinearLayout(this)
        head.orientation = LinearLayout.HORIZONTAL
        head.gravity = Gravity.CENTER_VERTICAL
        val ic = appIcon(sbn.packageName, 22)
        if (ic != null && Prefs.showItem(this, "p_icon")) head.addView(ic)
        head.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        if (Prefs.showItem(this, "p_cnt")) head.addView(countBadge(count, bg, fg))
        card.addView(head)

        val t = TextView(this)
        t.text = title ?: Feed.title(sbn)
        t.textSize = 14f
        t.setTag(R.id.nt_grp, "title")
        t.setTypeface(null, Typeface.BOLD)
        t.setTextColor(fg)
        t.maxLines = 1
        t.ellipsize = TextUtils.TruncateAt.END
        t.setPadding(0, dp(8), 0, 0)
        if (Prefs.showItem(this, "p_title_t")) card.addView(t)

        val m = TextView(this)
        m.text = text ?: Feed.recentText(this, sbn, 3)
        m.textSize = 13f
        m.setTag(R.id.nt_grp, "text")
        m.setTextColor(Feed.alpha(fg, 0xDD))
        m.maxLines = 6
        m.ellipsize = TextUtils.TruncateAt.END
        if (Prefs.showItem(this, "p_text")) card.addView(m)

        if (Arrange.customOn(this)) card.setOnLongClickListener { movePicker(sbn.packageName); true }
        card.setOnClickListener {
            ActionLog.add(this, "tile tapped: " + Feed.label(this, sbn.packageName))
            expanded = sbn.key
            refresh()
        }
        return card
    }

    private fun noteTile(n: Note): View {
        val st = Feed.NOTES
        val bg = st.bg(dark)
        val fg = st.fg(dark)
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = tileBg(bg, fg, 16)
        card.setPadding(dp(10), dp(10), dp(10), dp(10))
        card.minimumHeight = dp(104)

        val label = TextView(this)
        label.text = "Note"
        label.textSize = 12f
        label.setTag(R.id.nt_grp, "small")
        label.setTypeface(null, Typeface.BOLD)
        label.setTextColor(Feed.alpha(fg, 0xCC))
        card.addView(label)

        val m = TextView(this)
        m.text = n.text
        m.textSize = 14f
        m.setTag(R.id.nt_grp, "text")
        m.setTextColor(fg)
        m.maxLines = 4
        m.ellipsize = TextUtils.TruncateAt.END
        m.setPadding(0, dp(6), 0, 0)
        card.addView(m)

        card.setOnClickListener {
            expanded = "note:" + n.id
            refresh()
        }
        return card
    }

    private fun pill(label: String, fg: Int, bg: Int, filled: Boolean, onClick: () -> Unit): Button {
        val b = Button(this, null, android.R.attr.borderlessButtonStyle)
        b.text = label
        b.isAllCaps = false
        b.setSingleLine(true)
        b.textSize = 13f
        b.setTag(R.id.nt_grp, "buttons")
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

    private fun composer(existing: Note?): View {
        val st = Feed.NOTES
        val bg = st.bg(dark)
        val fg = st.fg(dark)

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = tileBg(bg, fg, 18)
        card.setPadding(dp(12), dp(10), dp(12), dp(12))
        val clp = LinearLayout.LayoutParams(MATCH, WRAP)
        clp.bottomMargin = dp(8)
        card.layoutParams = clp

        val label = TextView(this)
        label.text = if (existing == null) "New note" else "Note"
        label.textSize = 13f
        label.setTag(R.id.nt_grp, "small")
        label.setTypeface(null, Typeface.BOLD)
        label.setTextColor(Feed.alpha(fg, 0xCC))
        card.addView(label)

        val et = EditText(this)
        et.hint = "Type a note…"
        et.setHintTextColor(Feed.alpha(fg, 0x99))
        et.setTextColor(fg)
        et.textSize = 15f
        et.setTag(R.id.nt_grp, "text")
        et.minLines = 4
        et.gravity = Gravity.TOP or Gravity.START
        et.background = rounded(if (dark) 0x33FFFFFF else 0xAAFFFFFF.toInt(), 12)
        et.setPadding(dp(12), dp(10), dp(12), dp(10))
        if (existing != null) et.setText(existing.text)
        val elp = LinearLayout.LayoutParams(MATCH, WRAP)
        elp.topMargin = dp(8)
        card.addView(et, elp)

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.setPadding(0, dp(10), 0, 0)
        val plp = { LinearLayout.LayoutParams(WRAP, WRAP).apply { marginEnd = dp(6) } }
        bar.addView(pill(if (existing == null && !Notes.keepInApp(this)) "Send to notes app" else "Save", fg, bg, true) {
            val txt = et.text.toString().trim()
            if (txt.isEmpty()) {
                toast("Type something first")
            } else {
                if (existing == null && !Notes.keepInApp(this)) {
                    val sendNow = {
                        if (Notes.send(this, txt)) {
                            et.setText("")
                            expanded = null
                            finish()
                        } else {
                            Prefs.addNote(this, txt)
                            et.setText("")
                            expanded = null
                            refresh()
                            toast("No notes app found, saved here")
                        }
                    }
                    if (Notes.needsPick(this)) Notes.pickDialog(this) { sendNow() } else sendNow()
                } else {
                    if (existing == null) Prefs.addNote(this, txt) else Prefs.updateNote(this, existing.id, txt)
                    et.setText("")
                    expanded = null
                    refresh()
                    toast("Saved")
                }
            }
        }, plp())
        if (existing != null) {
            bar.addView(pill("Delete", fg, bg, false) {
                Prefs.deleteNote(this, existing.id)
                et.setText("")
                expanded = null
                refresh()
            }, plp())
        }
        card.addView(bar)

        if (existing == null) {
            et.post {
                et.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        return card
    }

    private fun expandedCard(sbn: StatusBarNotification, count: Int): View {
        val n = sbn.notification
        val st = Feed.style(this, sbn.packageName)
        val bg = st.bg(dark)
        val fg = st.fg(dark)

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.background = tileBg(bg, fg, 18)
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
        name.setTag(R.id.nt_grp, "small")
        name.setTextColor(Feed.alpha(fg, 0xCC))
        name.setPadding(dp(8), 0, 0, 0)
        head.addView(name, LinearLayout.LayoutParams(0, WRAP, 1f))
        val x = TextView(this)
        x.text = "×"
        x.textSize = 26f
        x.setTag(R.id.nt_grp, "title")
        x.setTextColor(fg)
        x.setPadding(dp(14), 0, dp(2), 0)
        x.setOnClickListener {
            // Like swiping it away: hidden for now (apps that re-post, like WhatsApp, cannot bring it back), but not read.
            // A newer message shows again together with the older unread ones. The panel closes.
            if (count > 1) {
                // Several chats of this app on one tile: the x hides all of them together, like swiping the whole app away.
                val all = Feed.items(this, true).filter { it.packageName == sbn.packageName }
                for (o in all) Feed.markDismissed(this, o, Feed.handledStamp(o))
                val keys = (all.map { it.key } + sbn.key).distinct().toTypedArray()
                ActionLog.add(this, "x on " + Feed.label(this, sbn.packageName) + ": all " + keys.size + " chats of this app hidden like a swipe, nothing marked read, panel closed")
                Feed.cancelKeys(NotifListener.instance, keys.toList())
            } else {
            Feed.markDismissed(this, sbn, Feed.handledStamp(sbn))
            ActionLog.add(this, "x on " + Feed.label(this, sbn.packageName) + ": hidden like a swipe, nothing marked read, panel closed")
            Feed.cancelKeys(NotifListener.instance, listOf(sbn.key))
            }
            viewing = null
            expanded = null
            TilesWidget.refresh(applicationContext)
            finish()
        }
        head.addView(x)
        card.addView(head)

        val tt = Feed.title(sbn)
        if (tt.isNotEmpty()) {
            val t = TextView(this)
            t.text = tt
            t.textSize = 16f
            t.setTag(R.id.nt_grp, "title")
            t.setTypeface(null, Typeface.BOLD)
            t.setTextColor(fg)
            t.setPadding(0, dp(8), 0, 0)
            card.addView(t)
        }
        val tx = Feed.fullText(this, sbn)
        if (tx.isNotEmpty()) {
            val m = TextView(this)
            m.text = tx
            m.textSize = 14f
            m.setTag(R.id.nt_grp, "text")
            m.setTextColor(Feed.alpha(fg, 0xEE))
            m.maxLines = 60
            m.ellipsize = TextUtils.TruncateAt.END
            m.setPadding(0, dp(2), 0, 0)
            card.addView(m)
        }

        // Photo or video sent with the message, shown (and played) here so you do not have to open the app,
        // when the app lets us read it.
        val mediaBox = FrameLayout(this)
        mediaBox.visibility = View.GONE
        val pic = ImageView(this)
        pic.adjustViewBounds = true
        pic.scaleType = ImageView.ScaleType.FIT_CENTER
        pic.maxHeight = dp(280)
        pic.background = rounded(0x22808080, 12)
        pic.clipToOutline = true
        mediaBox.addView(pic, FrameLayout.LayoutParams(MATCH, WRAP))
        val play = TextView(this)
        play.text = "▶"
        play.textSize = 30f
        play.gravity = Gravity.CENTER
        play.setTextColor(0xFFFFFFFF.toInt())
        play.background = rounded(0x99000000.toInt(), 32)
        play.visibility = View.GONE
        mediaBox.addView(play, FrameLayout.LayoutParams(dp(64), dp(64), Gravity.CENTER))
        val picLp = LinearLayout.LayoutParams(MATCH, WRAP)
        picLp.topMargin = dp(8)
        card.addView(mediaBox, picLp)
        val hasPhotoWord = Feed.photoHint(this, sbn)
        Thread {
            val md = if (sbn.packageName == "com.snapchat.android") null else Feed.media(this, sbn, 1024)
            val isSnapCard = sbn.packageName == "com.snapchat.android"
            if (md == null && !hasPhotoWord && (isSnapCard || (tt.isEmpty() && tx.isEmpty()))) {
                // The app gave no text and no picture (for example Snapchat, which only sends a short alert, or a
                // "someone is typing" alert that was already replaced): say so instead of showing an empty card.
                runOnUiThread {
                    val ph = TextView(this)
                    ph.text = if (isSnapCard) "👻  Snapchat doesn't share the snap or chat here · tap to open Snapchat"
                    else "💬  The app gave no text for this message · tap to open it"
                    ph.textSize = 14f
                    ph.gravity = Gravity.CENTER
                    ph.setTextColor(fg)
                    ph.setPadding(dp(12), dp(20), dp(12), dp(20))
                    ph.setOnClickListener { open(sbn) }
                    ph.setOnLongClickListener { showDump(sbn); true }
                    mediaBox.removeAllViews()
                    mediaBox.addView(ph, FrameLayout.LayoutParams(MATCH, WRAP))
                    mediaBox.background = rounded(0x22808080, 12)
                    mediaBox.visibility = View.VISIBLE
                }
            }
            if (md == null && hasPhotoWord) {
                // Instagram / Snapchat do not hand the photo to other apps: say so clearly and open the app on tap.
                runOnUiThread {
                    val ph = TextView(this)
                    ph.text = "📷  Photo · tap to view in the app"
                    ph.textSize = 15f
                    ph.gravity = Gravity.CENTER
                    ph.setTextColor(fg)
                    ph.setPadding(dp(12), dp(28), dp(12), dp(28))
                    ph.setOnClickListener { open(sbn) }
                    ph.setOnLongClickListener { showDump(sbn); true }
                    mediaBox.removeAllViews()
                    mediaBox.addView(ph, FrameLayout.LayoutParams(MATCH, WRAP))
                    mediaBox.background = rounded(0x22808080, 12)
                    mediaBox.visibility = View.VISIBLE
                }
            }
            if (md != null) {
                runOnUiThread {
                    if (md.bmp != null) pic.setImageBitmap(md.bmp) else pic.minimumHeight = dp(180)
                    mediaBox.visibility = View.VISIBLE
                    if (!md.isVideo) pic.setOnClickListener { open(sbn) }
                    if (md.isVideo) {
                        play.visibility = View.VISIBLE
                        val tap = View.OnClickListener {
                            val vu = md.videoUri
                            if (vu != null) playVideo(mediaBox, vu, sbn) else openChat(sbn, "Opening the app to play the video")
                        }
                        play.setOnClickListener(tap)
                        pic.setOnClickListener(tap)
                    }
                }
            }
        }.start()

        val actions = n.actions?.toList() ?: emptyList()
        val snap = sbn.packageName == "com.snapchat.android"
        ActionLog.add(
            this,
            "card " + Feed.label(this, sbn.packageName) + ": tap on card = " + ActionLog.kind(n.contentIntent) + "; buttons: " +
                (if (actions.isEmpty()) "none" else actions.joinToString(", ") {
                    "'" + it.title + "' (" + ActionLog.kind(it.actionIntent) +
                        (if (it.remoteInputs?.isNotEmpty() == true) ", typing box" + (if (it.remoteInputs?.any { r -> r.allowFreeFormInput } == true) "" else " choices-only") else "") + ")"
                })
        )
        val reply = actions.firstOrNull { a ->
            a.actionIntent != null && a.remoteInputs?.any { it.allowFreeFormInput } == true
        } ?: (if (snap) actions.firstOrNull { a ->
            a.actionIntent != null && a.remoteInputs?.isNotEmpty() == true
        } else null)

        // One tile per app: also show the other chats of this app here, newest first (tap a name to open that chat).
        if (Prefs.groupMode(this) == 1) {
            val siblings = Feed.items(this, true)
                .filter { it.packageName == sbn.packageName && it.key != sbn.key }
                .sortedByDescending { it.postTime }
            for (o in siblings) {
                val line = View(this)
                line.setBackgroundColor(Feed.alpha(fg, 0x33))
                val llp = LinearLayout.LayoutParams(MATCH, dp(1))
                llp.topMargin = dp(12)
                card.addView(line, llp)
                val ot = TextView(this)
                ot.text = (Feed.title(o).ifEmpty { Feed.label(this, o.packageName) }) + "  ›"
                ot.textSize = 16f
                ot.setTag(R.id.nt_grp, "title")
                ot.setTypeface(null, Typeface.BOLD)
                ot.setTextColor(fg)
                ot.setPadding(0, dp(10), 0, 0)
                ot.setOnClickListener { expanded = o.key; refresh() }
                card.addView(ot)
                val otx = Feed.fullText(this, o)
                if (otx.isNotEmpty()) {
                    val om = TextView(this)
                    om.text = otx
                    om.textSize = 14f
                    om.setTag(R.id.nt_grp, "text")
                    om.setTextColor(Feed.alpha(fg, 0xEE))
                    om.maxLines = 30
                    om.ellipsize = TextUtils.TruncateAt.END
                    om.setPadding(0, dp(2), 0, 0)
                    card.addView(om)
                }
            }
            if (siblings.isNotEmpty() && reply != null) {
                val hintTv = TextView(this)
                hintTv.text = "Reply goes to " + Feed.title(sbn).ifEmpty { Feed.label(this, sbn.packageName) } + " (tap another name to reply to it)"
                hintTv.textSize = 12f
                hintTv.setTag(R.id.nt_grp, "small")
                hintTv.setTextColor(Feed.alpha(fg, 0xAA))
                hintTv.setPadding(0, dp(12), 0, 0)
                card.addView(hintTv)
            }
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
            et.setTag(R.id.nt_grp, "buttons")
            et.maxLines = 3
            et.background = rounded(if (dark) 0x33FFFFFF else 0xAAFFFFFF.toInt(), 12)
            et.setPadding(dp(12), dp(10), dp(12), dp(10))
            row.addView(et, LinearLayout.LayoutParams(0, WRAP, 1f))
            val send = pill("Send", fg, bg, true) { sendReply(reply, et) }
            val slp = LinearLayout.LayoutParams(WRAP, WRAP)
            slp.marginStart = dp(8)
            row.addView(send, slp)
            // Reply templates: tap one to put it in the box (nothing is sent until you tap Send).
            val tpls = Prefs.replyTemplates(this)
            if (tpls.isNotEmpty()) {
                val tplBar = LinearLayout(this)
                tplBar.orientation = LinearLayout.HORIZONTAL
                tplBar.setPadding(0, dp(10), 0, 0)
                for (t in tpls) {
                    val lp = LinearLayout.LayoutParams(WRAP, WRAP)
                    lp.marginEnd = dp(6)
                    tplBar.addView(pill(t, fg, bg, false) {
                        et.setText(t)
                        et.setSelection(et.text.length)
                        et.requestFocus()
                    }, lp)
                }
                val tplScroll = android.widget.HorizontalScrollView(this)
                tplScroll.isHorizontalScrollBarEnabled = false
                tplScroll.addView(tplBar)
                card.addView(tplScroll)
            }
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
        // The buttons under a message. Their order and which are shown is chosen in Settings > Message buttons
        // (or they sort themselves by how often each is used).
        val hasRead = actions.any { a -> a.title.toString().lowercase().let { it.contains("read") && !it.contains("unread") } }
        val adders = HashMap<String, () -> Unit>()
        // Snapchat: View / Reply / Open all only open Snapchat, so one honest button.
        adders["app"] = {
            if (!snap) {
                for (a in others) {
                    bar.addView(pill(a.title.toString(), fg, bg, false) { Prefs.countBtnUse(this, "app"); runAction(sbn, a) }, blp())
                }
            }
        }
        // Apps like Teams give no "Mark as read" button of their own: offer one here (hides this chat, like the app's own button would).
        adders["read"] = {
            if (!snap && !hasRead) {
                bar.addView(pill("Mark as read", fg, bg, false) {
                    Prefs.countBtnUse(this, "read")
                    ActionLog.add(this, "Mark as read (own button) on " + Feed.label(this, sbn.packageName) + ": chat marked done and hidden")
                    Feed.markDone(this, sbn, maxOf(Feed.handledStamp(sbn), System.currentTimeMillis()) + 4000)
                    Feed.markReadSticky(this, sbn)
                    dismissFromShade(sbn, 800L)
                    if (viewing?.key == sbn.key) viewing = null
                    expanded = null
                    TilesWidget.refresh(applicationContext)
                    finish()
                }, blp())
            }
        }
        adders["snooze"] = { bar.addView(pill("Snooze", fg, bg, false) { Prefs.countBtnUse(this, "snooze"); snoozeDialog(sbn) }, blp()) }
        adders["mute"] = {
            if (Feed.chatKey(sbn) != null) {
                bar.addView(pill("Mute chat", fg, bg, false) { Prefs.countBtnUse(this, "mute"); muteChatDialog(sbn) }, blp())
            }
        }
        adders["open"] = { bar.addView(pill(if (snap) "Open Snapchat" else "Open", fg, bg, false) { Prefs.countBtnUse(this, "open"); open(sbn) }, blp()) }
        adders["clear"] = {
            if (count > 1) {
                bar.addView(pill("Clear $count", fg, bg, false) { Prefs.countBtnUse(this, "clear"); clearApp(sbn.packageName) }, blp())
            }
        }
        val hiddenBtns = Prefs.cardBtnHidden(this)
        for (k in Prefs.cardBtnOrder(this)) {
            if (k !in hiddenBtns) adders[k]?.invoke()
        }

        // In a sideways scroll like the shortcuts row: a long row of buttons never squeezes one into "Mar / k as / read"
        val barScroll = android.widget.HorizontalScrollView(this)
        barScroll.isHorizontalScrollBarEnabled = false
        barScroll.addView(bar)
        card.addView(barScroll)
        // Shortcuts that belong to this app (add them in Settings > Apps, or with the + here). Long-press one to remove it.
        val scBar = LinearLayout(this)
        scBar.orientation = LinearLayout.HORIZONTAL
        scBar.setPadding(0, dp(6), 0, 0)
        val pkgNow = sbn.packageName
        for ((idx, sc) in Prefs.appShortcuts(this, pkgNow).withIndex()) {
            val b = pill(sc.name, fg, bg, false) { QuickActivity.go(this, sc); finish() }
            b.setOnLongClickListener {
                android.app.AlertDialog.Builder(this).setMessage("Remove shortcut \"" + sc.name + "\"?")
                    .setPositiveButton("Remove") { _, _ -> Prefs.removeAppShortcut(this, pkgNow, idx); b.visibility = View.GONE }
                    .setNegativeButton("Cancel", null).show()
                true
            }
            scBar.addView(b, blp())
        }
        scBar.addView(pill("＋ Shortcut", fg, bg, false) {
            startActivity(Intent(this, QuickActivity::class.java).putExtra("for_pkg", pkgNow).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }, blp())
        val scScroll = android.widget.HorizontalScrollView(this)
        scScroll.isHorizontalScrollBarEnabled = false
        scScroll.addView(scBar)
        card.addView(scScroll)
        return card
    }

    // Snooze one chat: it leaves the widget and the panel for a while and comes back by itself (still in the phone's shade).
    private fun snoozeDialog(sbn: StatusBarNotification) {
        val names = arrayOf("15 minutes", "1 hour", "3 hours", "Until tomorrow 8:00")
        android.app.AlertDialog.Builder(this).setTitle("Snooze this chat")
            .setItems(names) { _, which ->
                val until = when (which) {
                    0 -> System.currentTimeMillis() + 15 * 60000L
                    1 -> System.currentTimeMillis() + 60 * 60000L
                    2 -> System.currentTimeMillis() + 180 * 60000L
                    else -> Extras.nextAt(8 * 60)
                }
                Feed.snoozeChat(this, sbn, until)
                ActionLog.add(this, "Snooze on " + Feed.label(this, sbn.packageName) + ": chat hidden until " + Extras.fmtTime(until))
                if (viewing?.key == sbn.key) viewing = null
                expanded = null
                TilesWidget.refresh(applicationContext)
                finish()
            }
            .setNegativeButton("Cancel", null).show()
    }

    // Mute one chat for good (until you unmute it in Settings > Messages > Filters & limits).
    private fun muteChatDialog(sbn: StatusBarNotification) {
        val key = Feed.chatKey(sbn) ?: return
        android.app.AlertDialog.Builder(this)
            .setMessage("Mute \"" + Feed.title(sbn) + "\"? It will no longer show on the widget or here. Other chats of this app are not affected. You can unmute it in Settings > Messages > Filters & limits.")
            .setPositiveButton("Mute") { _, _ ->
                Prefs.setChatMuted(this, key, true)
                ActionLog.add(this, "Mute chat on " + Feed.label(this, sbn.packageName) + ": this chat is hidden until unmuted in Settings")
                if (viewing?.key == sbn.key) viewing = null
                expanded = null
                TilesWidget.refresh(applicationContext)
                finish()
            }
            .setNegativeButton("Cancel", null).show()
    }

    // Long press on the photo box: what the app offers in its notification (to see whether it gives any picture).
    private fun showDump(sbn: StatusBarNotification) {
        val tv = TextView(this)
        tv.text = Feed.debugDump(sbn)
        tv.typeface = Typeface.MONOSPACE
        tv.textSize = 12f
        tv.setTextIsSelectable(true)
        tv.setPadding(dp(20), dp(12), dp(20), dp(12))
        val sv = android.widget.ScrollView(this)
        sv.addView(tv)
        android.app.AlertDialog.Builder(this).setTitle("What this app gives").setView(sv)
            .setPositiveButton("Close", null).show()
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
            ActionLog.add(this, "Send: reply handed to the app ('" + a.title + "'); the app gives no answer whether it arrived")
            et.setText("")
            // No pop-up toast (it stayed on screen for seconds): a short "Sent ✓" in the reply box instead.
            val oldHint = et.hint
            et.hint = "Sent ✓"
            et.postDelayed({ et.hint = oldHint }, 1200)
            val v = viewing
            if (v != null) Feed.rememberSent(v, msg)
            if (v != null && Prefs.countReply(this)) {
                // Everything in this chat up to now counts as answered: the app's "you replied" re-post carries only
                // messages up to now, so it stays hidden; only a message that arrives later shows again.
                val until = maxOf(Feed.handledStamp(v), System.currentTimeMillis()) +
                    (if (Feed.hasMessages(v)) 10000L else 60000L)
                Feed.markDone(this, v, until)
                dismissFromShade(v, 1500L)
                sweepEcho(v.key)
                viewing = null
                expanded = null
                refresh()
                TilesWidget.refresh(applicationContext)
            }
            // The panel closes by itself once the reply is sent.
            finish()
        } catch (e: PendingIntent.CanceledException) {
            ActionLog.add(this, "Send FAILED: the app's reply button expired (" + e.javaClass.simpleName + ")")
            toast("Couldn't send. Open the app to reply.")
        }
    }

    // Apps re-post the chat a few seconds after a reply (with our own reply inside). Remove that copy again, but only
    // when its newest message is ours, so a real new message from the other person is never swept away.
    private fun sweepEcho(key: String) {
        val app = applicationContext
        val h = android.os.Handler(android.os.Looper.getMainLooper())
        for (d in longArrayOf(1500L, 5000L, 12000L, 30000L)) {
            h.postDelayed({
                val svc = NotifListener.instance ?: return@postDelayed
                val n = try {
                    svc.activeNotifications?.firstOrNull { it.key == key }
                } catch (e: Exception) {
                    null
                }
                if (n != null && n.isClearable && Prefs.dismissHandled(app) &&
                    Feed.newestTs(n) <= Feed.handledUntil(app, n)
                ) {
                    Feed.cancelKeys(svc, listOf(key))
                }
                TilesWidget.refresh(app)
            }, d)
        }
    }

    private fun balOptions(): Bundle? {
        if (Build.VERSION.SDK_INT < 34) return null
        return try {
            val o = ActivityOptions.makeBasic()
            ActivityOptions::class.java
                .getMethod("setPendingIntentBackgroundActivityStartMode", Int::class.javaPrimitiveType)
                .invoke(o, 1)
            o.toBundle()
        } catch (e: Throwable) {
            null
        }
    }

    private fun fire(pi: PendingIntent): Boolean = try {
        pi.send(this, 0, null, null, null, null, balOptions())
        true
    } catch (e: PendingIntent.CanceledException) {
        false
    }

    // Snapchat's own buttons only work while Snapchat is already running (that is why View / Reply / Open did nothing).
    // So: start Snapchat first, then send the chat / button a moment later.
    private fun openSnapchat(sbn: StatusBarNotification, pi: PendingIntent?) {
        try {
            packageManager.getLaunchIntentForPackage(sbn.packageName)
                ?.let { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        } catch (e: Exception) {
        }
        val h = android.os.Handler(android.os.Looper.getMainLooper())
        val target = pi ?: sbn.notification.contentIntent
        if (target == null) {
            ActionLog.add(this, "Snapchat: started the app; Snapchat gave nothing to send to the chat")
            return
        }
        ActionLog.add(this, "Snapchat: started the app, sending its " + (if (pi == null) "chat" else "button") + " (" + ActionLog.kind(target) + ") after 0.9 s and 3.5 s")
        val opts = balOptions()
        val app = applicationContext
        for (d in longArrayOf(900L, 3500L)) {
            h.postDelayed({
                try {
                    target.send(app, 0, null, null, null, null, opts)
                    ActionLog.add(app, "Snapchat: sent to Snapchat OK after " + (d / 1000.0) + " s (if nothing opened, Snapchat ignored it)")
                } catch (e: Exception) {
                    ActionLog.add(app, "Snapchat: sending FAILED: " + e.javaClass.simpleName + " " + (e.message ?: ""))
                }
            }, d)
        }
    }

    private fun runAction(sbn: StatusBarNotification, a: Notification.Action) {
        val title = a.title.toString().lowercase()
        if (sbn.packageName == "com.snapchat.android" && !(title.contains("read") && !title.contains("unread"))) {
            if (Prefs.countRead(this)) {
                Feed.markDone(this, sbn, Feed.handledStamp(sbn))
                dismissFromShade(sbn, 4000L)
            }
            viewing = null
            openSnapchat(sbn, a.actionIntent)
            finish()
            return
        }
        val pi = a.actionIntent
        val fired = fire(pi)
        ActionLog.add(this, "Button '" + a.title + "' on " + Feed.label(this, sbn.packageName) + " (" + ActionLog.kind(pi) + "): " + (if (fired) "sent" else "FAILED, the button expired"))
        val isRead = title.contains("read") && !title.contains("unread")
        if (isRead) {
            // The app's own "Mark as read": whatever the app does with it (it may only open its screen, or the button may have
            // expired), it counts as opened and read here, and the same notification re-posted by the app stays hidden.
            Feed.markDone(this, sbn, maxOf(Feed.handledStamp(sbn), System.currentTimeMillis()) + 4000)
            Feed.markReadSticky(this, sbn)
            ActionLog.add(this, "Mark as read on " + Feed.label(this, sbn.packageName) + ": counted as opened and read, repeats of it stay hidden")
            dismissFromShade(sbn, 800L)
            if (viewing?.key == sbn.key) viewing = null
            expanded = null
            TilesWidget.refresh(applicationContext)
            finish()
            return
        }
        if (!fired) {
            toast("That action expired, opening the app instead")
            open(sbn)
            return
        }
        if (pi.isActivity) {
            finish()
            return
        }
        val t = a.title.toString().lowercase()
        if (t.contains("read") && !t.contains("unread")) {
            // "Mark as read" from the app's own button: handled here as well.
            Feed.markDone(this, sbn, maxOf(Feed.handledStamp(sbn), System.currentTimeMillis()) + 4000)
            dismissFromShade(sbn, 800L)
            if (viewing?.key == sbn.key) viewing = null
            expanded = null
            TilesWidget.refresh(applicationContext)
            finish()
            return
        }
        if (t.contains("view") || t.contains("open") || t.contains("play") || t.contains("watch")) {
            open(sbn)
            return
        }
        // Any other quick action (Like, ...) is done: close the panel.
        finish()
    }

    private fun open(sbn: StatusBarNotification) {
        if (Prefs.countRead(this)) {
            Feed.markDone(this, sbn, Feed.handledStamp(sbn))
            Feed.markReadSticky(this, sbn)
            dismissFromShade(sbn, if (sbn.packageName == "com.snapchat.android") 4000L else 1500L)
        }
        viewing = null
        if (sbn.packageName == "com.snapchat.android") {
            openSnapchat(sbn, null)
            finish()
            return
        }
        try {
            val pi = sbn.notification.contentIntent
            if (pi != null) {
                val opened = fire(pi)
                ActionLog.add(this, "Open " + Feed.label(this, sbn.packageName) + " (" + ActionLog.kind(pi) + "): " + (if (opened) "sent" else "FAILED, the chat link expired"))
                if (!opened) toast("Couldn't open")
                if ((sbn.notification.flags and Notification.FLAG_AUTO_CANCEL) != 0) {
                    Feed.cancelKeys(NotifListener.instance, listOf(sbn.key))
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
        val keys = Feed.items(this, true).filter { it.packageName == pkg && it.isClearable }
            .map { it.key }.toTypedArray()
        expanded = null
        if (keys.isNotEmpty()) Feed.cancelKeys(svc, keys.toList())
    }

    private fun clearAll() {
        val svc = NotifListener.instance ?: return
        val keys = Feed.items(this, true).filter { it.isClearable }.map { it.key }.toTypedArray()
        if (keys.isNotEmpty()) Feed.cancelKeys(svc, keys.toList())
    }

    private var lastToast: Toast? = null

    private fun toast(m: String) {
        lastToast?.cancel()
        lastToast = Toast.makeText(this, m, Toast.LENGTH_SHORT).also { it.show() }
    }
}
