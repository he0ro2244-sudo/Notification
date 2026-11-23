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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        slot = intent.getIntExtra("slot", 0).coerceIn(0, Prefs.QUICK_SLOTS - 1)
        if (savedInstanceState != null) {
            creatorPkg = savedInstanceState.getString("creator", "") ?: ""
            return
        }
        val q = Prefs.quick(this, slot)
        if (q == null || intent.getBooleanExtra("pick", false)) chooseKind() else launch(q)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("creator", creatorPkg)
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun launch(q: Quick) {
        try {
            val i: Intent = when (q.type) {
                "app" -> packageManager.getLaunchIntentForPackage(q.pkg) ?: throw IllegalStateException("app gone")
                "act" -> Intent().setComponent(ComponentName(q.pkg, q.target))
                else -> Intent.parseUri(q.target, 0)
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        } catch (e: Exception) {
            toast("Could not open " + q.name + " (it may have been removed or changed)")
        }
        finish()
    }

    private fun chooseKind() {
        AlertDialog.Builder(this)
            .setTitle("Add to shortcuts row")
            .setItems(arrayOf("App", "Activity (a screen inside an app)", "Shortcut (like Nova, e.g. a WhatsApp chat)")) { _, w ->
                when (w) {
                    0 -> pickApp { pkg -> nameIt(Feed.label(this, pkg)) { n -> save(Quick("app", n, pkg, "")) } }
                    1 -> pickApp { pkg -> pickActivity(pkg) }
                    else -> pickCreator()
                }
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun launcherApps(): List<String> {
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(launch, 0).map { it.activityInfo.packageName }.distinct()
            .filter { it != packageName }.sortedBy { Feed.label(this, it).lowercase() }
    }

    private fun pickApp(done: (String) -> Unit) {
        val all = launcherApps()
        if (all.isEmpty()) { toast("No apps found"); finish(); return }
        AlertDialog.Builder(this).setTitle("Choose an app")
            .setItems(all.map { Feed.label(this, it) }.toTypedArray()) { _, w -> done(all[w]) }
            .setOnCancelListener { finish() }
            .show()
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
        AlertDialog.Builder(this).setTitle(Feed.label(this, pkg) + ": choose a screen")
            .setItems(lines.toTypedArray()) { _, w ->
                val (ai, lbl) = acts[w]
                val def = if (lbl.isBlank() || lbl == Feed.label(this, pkg)) ai.name.substringAfterLast('.') else lbl
                nameIt(def) { n -> save(Quick("act", n, pkg, ai.name)) }
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
        Prefs.setQuick(this, slot, q)
        Prefs.setQuickOn(this, true)
        toast("Saved: " + q.name)
        TilesWidget.refresh(applicationContext)
        finish()
    }

    companion object {
        private const val REQ_SC = 1
    }
}
