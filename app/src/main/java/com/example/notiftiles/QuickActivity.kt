package com.example.notiftiles

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast

/** type: "app" (open an app), "act" (open one screen of an app), "sc" (a shortcut made by an app, like Nova's Shortcuts). */
data class Quick(val type: String, val name: String, val pkg: String, val target: String)

// Invisible activity behind the shortcut circles under the fast dial.
// Empty slot (or "pick") -> choose what to add. Filled slot -> open it.
class QuickActivity : Activity() {

    private var slot = 0
    private var creatorPkg = ""
    private var forPkg = ""   // set = the shortcut is added to that app (shown on its card), not to a slot

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slot = intent.getIntExtra("slot", 0).coerceIn(0, Prefs.QUICK_SLOTS - 1)
        forPkg = intent.getStringExtra("for_pkg") ?: ""
        if (savedInstanceState != null) {
            creatorPkg = savedInstanceState.getString("creator", "") ?: ""
            forPkg = savedInstanceState.getString("for_pkg", forPkg) ?: forPkg
            return
        }
        if (forPkg.isNotEmpty()) { chooseKind(); return }
        val q = Prefs.quick(this, slot)
        if (q == null || intent.getBooleanExtra("pick", false)) chooseKind() else launch(q)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("creator", creatorPkg)
        outState.putString("for_pkg", forPkg)
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun launch(q: Quick) {
        go(this, q)
        finish()
    }

    private fun chooseKind() {
        AlertDialog.Builder(this)
            .setTitle(if (forPkg.isNotEmpty()) "Add a shortcut for " + Feed.label(this, forPkg) else "Add to shortcuts row")
            .setItems(arrayOf("App", "Activity (a screen inside an app)", "Shortcut", "Link (open a screen of an app, e.g. Instagram camera)", "Link finder (shows what an app really accepts)")) { _, w ->
                when (w) {
                    0 -> pickApp { pkg -> nameIt(Feed.label(this, pkg)) { n -> save(Quick("app", n, pkg, "")) } }
                    1 -> pickApp { pkg -> pickActivity(pkg) }
                    3 -> pickLink()
                    4 -> pickApp { pkg -> showLinks(pkg) }
                    else -> pickAllShortcuts()
                }
            }
            .setOnCancelListener { finish() }
            .show()
    }

    /** A list with a search box on top. Typing filters the lines; tapping one calls onPick with its index in `lines`. */
    private fun pickFrom(title: String, lines: List<String>, onPick: (Int) -> Unit) {
        val box = android.widget.LinearLayout(this)
        box.orientation = android.widget.LinearLayout.VERTICAL
        box.setPadding(40, 8, 40, 0)
        val et = EditText(this)
        et.hint = "Search"
        et.isSingleLine = true
        val lv = android.widget.ListView(this)
        val shown = ArrayList(lines.indices.toList())
        val ad = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList(lines))
        lv.adapter = ad
        box.addView(et)
        val h = (resources.displayMetrics.heightPixels * 0.55).toInt()
        box.addView(lv, android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, h))
        et.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = s.toString().trim().lowercase()
                shown.clear()
                ad.clear()
                for (i in lines.indices) if (q.isEmpty() || lines[i].lowercase().contains(q)) { shown.add(i); ad.add(lines[i]) }
                ad.notifyDataSetChanged()
            }
        })
        val dlg = AlertDialog.Builder(this).setTitle(title).setView(box)
            .setOnCancelListener { finish() }
            .show()
        lv.setOnItemClickListener { _, _, pos, _ -> dlg.dismiss(); onPick(shown[pos]) }
    }

    private fun launcherApps(): List<String> {
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launch, 0).map { it.activityInfo.packageName }.distinct()
            .filter { it != packageName }.sortedBy { Feed.label(this, it).lowercase() }
    }

    private fun pickApp(done: (String) -> Unit) {
        val all = launcherApps()
        if (all.isEmpty()) { toast("No apps found"); finish(); return }
        pickFrom("Choose an app", all.map { Feed.label(this, it) }) { w -> done(all[w]) }
    }

    @Suppress("DEPRECATION")
    private fun pickActivity(pkg: String) {
        var all: List<android.content.pm.ActivityInfo> = emptyList()
        try {
            all = packageManager.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES).activities?.toList() ?: emptyList()
        } catch (e: Exception) {
        }
        val acts = all.filter { it.exported && it.isEnabled && it.permission == null }
            .map { ai -> Pair(ai, ai.loadLabel(packageManager).toString()) }
            .sortedBy { it.second.lowercase() }
        if (acts.isEmpty()) { toast("This app has no screens that can be opened from here"); finish(); return }
        val lines = acts.map { (ai, lbl) -> lbl + "  ·  " + ai.name.substringAfterLast('.') }
        pickFrom(Feed.label(this, pkg) + ": choose a screen", lines) { w ->
            val (ai, lbl) = acts[w]
            val def = if (lbl.isBlank() || lbl == Feed.label(this, pkg)) ai.name.substringAfterLast('.') else lbl
            nameIt(def) { n -> save(Quick("act", n, pkg, ai.name)) }
        }
    }

    private class StaticSc(val label: String, val uri: String)

    /** The shortcuts an app lists in its own manifest (what a long-press on its icon shows), read from the app's resources. */
    @Suppress("DEPRECATION")
    private fun staticShortcuts(pkg: String): List<StaticSc> {
        val out = ArrayList<StaticSc>()
        try {
            val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg)
            val res = packageManager.getResourcesForApplication(pkg)
            val ns = "http://schemas.android.com/apk/res/android"
            for (ri in packageManager.queryIntentActivities(main, PackageManager.GET_META_DATA)) {
                val resId = ri.activityInfo.metaData?.getInt("android.app.shortcuts") ?: 0
                if (resId == 0) continue
                val xml = res.getXml(resId)
                var label = ""
                var enabled = true
                var intent: Intent? = null
                var ev = xml.eventType
                while (ev != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                    if (ev == org.xmlpull.v1.XmlPullParser.START_TAG) {
                        when (xml.name) {
                            "shortcut" -> {
                                enabled = xml.getAttributeBooleanValue(ns, "enabled", true)
                                val lid = xml.getAttributeResourceValue(ns, "shortcutShortLabel", 0)
                                label = if (lid != 0) try { res.getString(lid) } catch (e: Exception) { "" }
                                        else xml.getAttributeValue(ns, "shortcutShortLabel") ?: ""
                                intent = null
                            }
                            "intent" -> if (intent == null) {
                                val i = Intent()
                                xml.getAttributeValue(ns, "action")?.let { i.action = it }
                                val tp = xml.getAttributeValue(ns, "targetPackage") ?: pkg
                                val tc = xml.getAttributeValue(ns, "targetClass")
                                if (tc != null) i.setClassName(tp, tc) else i.setPackage(tp)
                                xml.getAttributeValue(ns, "data")?.let { i.data = android.net.Uri.parse(it) }
                                intent = i
                            }
                            "extra" -> intent?.let { i ->
                                val k = xml.getAttributeValue(ns, "name")
                                val v = xml.getAttributeValue(ns, "value")
                                if (k != null && v != null) i.putExtra(k, v)
                            }
                        }
                    } else if (ev == org.xmlpull.v1.XmlPullParser.END_TAG && xml.name == "shortcut") {
                        val i = intent
                        if (enabled && i != null && label.isNotBlank()) out.add(StaticSc(label, i.toUri(Intent.URI_INTENT_SCHEME)))
                        intent = null
                    }
                    ev = xml.next()
                }
            }
        } catch (e: Exception) {
        }
        return out.distinctBy { it.uri }
    }

    /** One list with every kind of app shortcut: the long-press ones (read from manifests) and the ones an app creates on request. */
    private fun pickAllShortcuts() {
        toast("Looking for shortcuts...")
        Thread {
            val items = ArrayList<Pair<String, () -> Unit>>()
            for (pkg in launcherApps()) {
                for (sc in staticShortcuts(pkg)) {
                    items.add(Pair(Feed.label(this, pkg) + ": " + sc.label) { nameIt(sc.label) { n -> save(Quick("sc", n, pkg, sc.uri)) } })
                }
            }
            val creators = try { packageManager.queryIntentActivities(Intent(Intent.ACTION_CREATE_SHORTCUT), 0) } catch (e: Exception) { emptyList() }
            for (ri in creators) {
                val ai = ri.activityInfo
                items.add(Pair(Feed.label(this, ai.packageName) + ": " + ri.loadLabel(packageManager) + " (choose...)") { startCreator(ai) })
            }
            items.sortBy { it.first.lowercase() }
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                if (items.isEmpty()) { toast("No app offers shortcuts here"); finish(); return@runOnUiThread }
                pickFrom("Choose a shortcut", items.map { it.first }) { w -> items[w].second() }
            }
        }.start()
    }

    private fun startCreator(ai: android.content.pm.ActivityInfo) {
        creatorPkg = ai.packageName
        try {
            startActivityForResult(
                Intent(Intent.ACTION_CREATE_SHORTCUT).setComponent(ComponentName(ai.packageName, ai.name)), REQ_SC
            )
        } catch (e: Exception) {
            toast("That shortcut could not be started")
            finish()
        }
    }

    private fun saveLink(pkg: String, link: String, def: String) {
        val uri = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link)).setPackage(pkg).toUri(Intent.URI_INTENT_SCHEME)
        nameIt(def) { n -> save(Quick("sc", n, pkg, uri)) }
    }

    private fun saveChain(pkg: String, uris: List<String>, def: String) {
        nameIt(def) { n -> save(Quick("sc", n, pkg, uris.joinToString("\n"))) }
    }

    private fun askText(title: String, hint: String, done: (String) -> Unit) {
        val et = EditText(this)
        et.hint = hint
        et.isSingleLine = true
        AlertDialog.Builder(this).setTitle(title).setView(et)
            .setPositiveButton("OK") { _, _ ->
                val t = et.text.toString().trim()
                if (t.isEmpty()) finish() else done(t)
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private class Preset(val label: String, val pkg: String, val link: String, val ask: String, val name: String)

    /** Opens one screen of an app through a link (not every app supports every link; apps can change them). */
    private fun pickLink() {
        val ig = "com.instagram.android"
        val fb = "com.facebook.katana"
        val sn = "com.snapchat.android"
        val wa = "com.whatsapp"
        val gm = "com.google.android.gm"
        val tm = "com.microsoft.teams"
        val presets = listOf(
            Preset("Instagram: Story screen (test)", ig, "", "igstory", "Story"),
            Preset("Instagram: Share / new post (test)", ig, "instagram://share", "", "New post"),
            Preset("Instagram: Messages", ig, "instagram://direct-inbox", "", "Messages"),
            Preset("Instagram: Feed", ig, "instagram://mainfeed", "", "Feed"),
            Preset("Instagram: Explore", ig, "instagram://explore", "", "Explore"),
            Preset("Facebook: Feed", fb, "fb://feed", "", "Feed"),
            Preset("Facebook: Notifications", fb, "fb://notifications", "", "Alerts"),
            Preset("Facebook: My profile", fb, "fb://profile", "", "Profile"),
            Preset("Snapchat: Camera", sn, "snapchat://", "", "Camera"),
            Preset("Snapchat: Add a friend (username)", sn, "snapchat://add/", "user", "Add"),
            Preset("WhatsApp: chat with a phone number", wa, "https://wa.me/", "phone", "Chat"),
            Preset("Gmail: Write a new email", gm, "mailto:", "", "Compose"),
            Preset("Gmail: Write to an address", gm, "mailto:", "mail", "Email"),
            Preset("Teams: Open", tm, "msteams://", "", "Teams"),
            Preset("Teams: Chat with an email", tm, "https://teams.microsoft.com/l/chat/0/0?users=", "mail", "Chat"),
            Preset("Type my own link", "", "", "own", "")
        )
        pickFrom("Choose a link", presets.map { it.label }) { w ->
            val p = presets[w]
            when (p.ask) {
                "" -> saveLink(p.pkg, p.link, p.name)
                "igstory" -> saveChain(p.pkg, listOf(
                    Intent("com.instagram.share.ADD_TO_STORY").setPackage(p.pkg).setType("image/*").toUri(Intent.URI_INTENT_SCHEME)
                ), p.name)
                "phone" -> askText("Phone number with country code", "e.g. 966501234567") { num ->
                    val digits = num.filter { it.isDigit() }
                    if (digits.isEmpty()) finish() else saveLink(p.pkg, p.link + digits, p.name)
                }
                "user" -> askText("Snapchat username", "username") { u -> saveLink(p.pkg, p.link + u, p.name) }
                "mail" -> askText("Email address", "name@example.com") { m ->
                    saveLink(p.pkg, p.link + m, p.name)
                }
                else -> askText("Link", "e.g. instagram://camera") { link ->
                    pickApp { pkg -> saveLink(pkg, link, Feed.label(this, pkg)) }
                }
            }
        }
    }

    /** Reads the app's own manifest on this phone and lists every link and action its screens declare (nothing is guessed). */
    private fun dumpManifest(pkg: String): String {
        val custom = java.util.TreeSet<String>()
        val web = java.util.TreeSet<String>()
        try {
            val xml = packageManager.getResourcesForApplication(pkg).assets.openXmlResourceParser("AndroidManifest.xml")
            val ns = "http://schemas.android.com/apk/res/android"
            var cur = ""
            var exported = ""
            var inFilter = false
            val actions = ArrayList<String>()
            val datas = ArrayList<String>()
            var ev = xml.eventType
            while (ev != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (ev == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    when (xml.name) {
                        "activity", "activity-alias" -> { cur = xml.getAttributeValue(ns, "name") ?: ""; exported = xml.getAttributeValue(ns, "exported") ?: "" }
                        "intent-filter" -> { inFilter = true; actions.clear(); datas.clear() }
                        "action" -> if (inFilter) actions.add(xml.getAttributeValue(ns, "name") ?: "")
                        "data" -> if (inFilter) {
                            val sch = xml.getAttributeValue(ns, "scheme")
                            val host = xml.getAttributeValue(ns, "host")
                            val path = xml.getAttributeValue(ns, "path") ?: xml.getAttributeValue(ns, "pathPrefix")?.let { it + "*" } ?: xml.getAttributeValue(ns, "pathPattern")
                            val mime = xml.getAttributeValue(ns, "mimeType")
                            if (sch != null || host != null || path != null) datas.add((sch ?: "") + "://" + (host ?: "") + (path ?: ""))
                            if (mime != null) datas.add("type=" + mime)
                        }
                    }
                } else if (ev == org.xmlpull.v1.XmlPullParser.END_TAG && xml.name == "intent-filter") {
                    inFilter = false
                    val acts = actions.filter { it != "android.intent.action.MAIN" }
                    if (acts.isNotEmpty()) {
                        val line = cur.substringAfterLast('.') + " [" + exported + "] " + acts.joinToString(",") + "  " + datas.joinToString(" ")
                        if (datas.any { it.startsWith("http") }) web.add(line) else custom.add(line)
                    }
                }
                ev = xml.next()
            }
        } catch (e: Exception) {
            return "Could not read this app: " + e.javaClass.simpleName + " " + (e.message ?: "")
        }
        return "== custom links and actions (" + custom.size + ") ==\n" + custom.joinToString("\n") +
            "\n\n== web links (" + web.size + ") ==\n" + web.joinToString("\n")
    }

    private fun showLinks(pkg: String) {
        toast("Reading...")
        Thread {
            val t = pkg + "\n" + dumpManifest(pkg)
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                val tv = android.widget.TextView(this)
                tv.text = t
                tv.typeface = android.graphics.Typeface.MONOSPACE
                tv.textSize = 11f
                tv.setTextIsSelectable(true)
                tv.setPadding(32, 24, 32, 24)
                val sv = android.widget.ScrollView(this)
                sv.addView(tv)
                AlertDialog.Builder(this).setTitle(Feed.label(this, pkg) + ": what it accepts").setView(sv)
                    .setPositiveButton("Close") { _, _ -> finish() }
                    .setNeutralButton("Copy") { _, _ ->
                        val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("links", t))
                        toast("Copied")
                        finish()
                    }
                    .setOnCancelListener { finish() }
                    .show()
            }
        }.start()
    }

    private fun pickStaticApp() {
        toast("Looking for app shortcuts...")
        Thread {
            val withSc = launcherApps().filter { staticShortcuts(it).isNotEmpty() }
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                if (withSc.isEmpty()) { toast("No app lists shortcuts of its own on this phone"); finish(); return@runOnUiThread }
                AlertDialog.Builder(this).setTitle("Apps with shortcuts")
                    .setItems(withSc.map { Feed.label(this, it) }.toTypedArray()) { _, w -> pickStaticShortcut(withSc[w]) }
                    .setOnCancelListener { finish() }
                    .show()
            }
        }.start()
    }

    private fun pickStaticShortcut(pkg: String) {
        val list = staticShortcuts(pkg)
        AlertDialog.Builder(this).setTitle(Feed.label(this, pkg) + ": choose a shortcut")
            .setItems(list.map { it.label }.toTypedArray()) { _, w ->
                val s = list[w]
                nameIt(s.label) { n -> save(Quick("sc", n, pkg, s.uri)) }
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun pickCreator() {
        val list = packageManager.queryIntentActivities(Intent(Intent.ACTION_CREATE_SHORTCUT), 0)
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        if (list.isEmpty()) { toast("No apps offer shortcuts here"); finish(); return }
        val lines = list.map { it.loadLabel(packageManager).toString() + "  (" + Feed.label(this, it.activityInfo.packageName) + ")" }
        AlertDialog.Builder(this).setTitle("Choose a shortcut")
            .setItems(lines.toTypedArray()) { _, w ->
                val ai = list[w].activityInfo
                creatorPkg = ai.packageName
                try {
                    startActivityForResult(
                        Intent(Intent.ACTION_CREATE_SHORTCUT).setComponent(ComponentName(ai.packageName, ai.name)), REQ_SC
                    )
                } catch (e: Exception) {
                    toast("That shortcut could not be started")
                    finish()
                }
            }
            .setOnCancelListener { finish() }
            .show()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_SC) { finish(); return }
        val sc: Intent? = if (resultCode == RESULT_OK) data?.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT) else null
        if (sc == null) {
            if (resultCode == RESULT_OK) toast("That shortcut cannot be added here (the app gave a newer kind of shortcut)")
            finish()
            return
        }
        val def = data?.getStringExtra(Intent.EXTRA_SHORTCUT_NAME).orEmpty().ifBlank { Feed.label(this, creatorPkg) }
        val uri = sc.toUri(Intent.URI_INTENT_SCHEME)
        val pkg = creatorPkg
        nameIt(def) { n -> save(Quick("sc", n, pkg, uri)) }
    }

    private fun nameIt(def: String, done: (String) -> Unit) {
        val et = EditText(this)
        et.setText(def)
        et.setSelection(et.text.length)
        et.isSingleLine = true
        AlertDialog.Builder(this).setTitle("Name under the circle")
            .setView(et)
            .setPositiveButton("Save") { _, _ -> done(et.text.toString().trim().ifBlank { def }) }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun save(q: Quick) {
        if (forPkg.isNotEmpty()) {
            Prefs.addAppShortcut(this, forPkg, q)
            toast("Saved: " + q.name)
            finish()
            return
        }
        Prefs.setQuick(this, slot, q)
        Prefs.setQuickOn(this, true)
        toast("Saved: " + q.name)
        TilesWidget.refresh(applicationContext)
        finish()
    }

    companion object {
        private const val REQ_SC = 1

        /** A shortcut can hold several ways to reach one screen (one per line): the first one the app understands is used; if none, the app itself opens. */
        private fun chainIntent(ctx: android.content.Context, q: Quick): Intent {
            val parts = q.target.split("\n").filter { it.isNotBlank() }
            if (parts.size <= 1) return Intent.parseUri(q.target, 0)
            for (t in parts) {
                val ii = try { Intent.parseUri(t, 0) } catch (e: Exception) { continue }
                if (ctx.packageManager.resolveActivity(ii, 0) != null) return ii
            }
            return ctx.packageManager.getLaunchIntentForPackage(q.pkg) ?: throw IllegalStateException("app gone")
        }

        /** Opens what a shortcut points at (an app, one screen of an app, or an app's own shortcut). */
        fun go(ctx: android.content.Context, q: Quick) {
            try {
                val i: Intent = when (q.type) {
                    "app" -> ctx.packageManager.getLaunchIntentForPackage(q.pkg) ?: throw IllegalStateException("app gone")
                    "act" -> Intent().setComponent(ComponentName(q.pkg, q.target))
                    else -> chainIntent(ctx, q)
                }
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(i)
            } catch (e: Exception) {
                Toast.makeText(ctx, "Could not open " + q.name + " (it may have been removed or changed)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
