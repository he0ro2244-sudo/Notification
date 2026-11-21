package com.example.notiftiles

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast

// Sends a note you type to the notes app on the phone instead of keeping it here.
object Notes {
    val KNOWN = listOf(
        "com.google.android.keep" to "Google Keep",
        "com.samsung.android.app.notes" to "Samsung Notes",
        "com.microsoft.office.onenote" to "OneNote",
        "com.evernote" to "Evernote",
        "com.automattic.simplenote" to "Simplenote",
        "md.obsidian" to "Obsidian"
    )

    private const val CREATE_NOTE = "android.intent.action.CREATE_NOTE"

    private fun sp(c: Context) = c.getSharedPreferences("nt", Context.MODE_PRIVATE)

    /** "auto" (phone's default notes app), "ask" (system chooser), "inapp" (keep inside Notif Tiles) or a package. */
    fun mode(c: Context): String = sp(c).getString("note_app", "auto") ?: "auto"
    fun setMode(c: Context, v: String) = sp(c).edit().putString("note_app", v).apply()

    private fun installed(c: Context, pkg: String): Boolean = try {
        c.packageManager.getApplicationInfo(pkg, 0)
        true
    } catch (e: Exception) {
        false
    }

    fun installedApps(c: Context): List<Pair<String, String>> = KNOWN.filter { installed(c, it.first) }

    fun keepInApp(c: Context): Boolean = mode(c) == "inapp"

    fun label(c: Context, pkg: String): String = try {
        c.packageManager.getApplicationLabel(c.packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    private fun createIntent(text: String) =
        Intent(CREATE_NOTE).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)

    private fun sendIntent(text: String) =
        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)

    private fun start(c: Context, i: Intent): Boolean = try {
        c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    /** Auto mode and the phone offers no notes app we can reach: the person picks one (once). */
    fun needsPick(c: Context): Boolean {
        if (mode(c) != "auto") return false
        if (installedApps(c).isNotEmpty()) return false
        return try {
            createIntent("x").resolveActivity(c.packageManager) == null
        } catch (e: Exception) {
            true
        }
    }

    /** Hands the text to the notes app. Returns false if nothing could take it. */
    fun send(c: Context, text: String): Boolean {
        val m = mode(c)
        val chooser = { start(c, Intent.createChooser(sendIntent(text), "Save note to")) }
        return when (m) {
            "ask" -> chooser()
            "auto" -> {
                if (start(c, createIntent(text))) return true
                val known = installedApps(c).firstOrNull()?.first
                if (known != null && start(c, sendIntent(text).setPackage(known))) return true
                chooser()
            }
            else -> {
                if (start(c, sendIntent(text).setPackage(m))) return true
                if (start(c, createIntent(text).setPackage(m))) return true
                copyAndOpen(c, text, m)
            }
        }
    }

    // The app doesn't take shared text: copy the note and open the app so it can be pasted.
    private fun copyAndOpen(c: Context, text: String, pkg: String): Boolean {
        val launch = c.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        try {
            val cm = c.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("note", text))
            Toast.makeText(c, "Note copied: paste it in ${label(c, pkg)}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
        }
        return start(c, launch)
    }

    /** Every app you can open from the launcher (plus any that accept shared text), notes-like names first. */
    fun targets(c: Context): List<Pair<String, String>> {
        val pm = c.packageManager
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val share = Intent(Intent.ACTION_SEND).setType("text/plain")
        val all = (pm.queryIntentActivities(launch, 0) + pm.queryIntentActivities(share, 0))
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != c.packageName }
            .distinctBy { it.first }
        fun notesLike(p: Pair<String, String>): Boolean {
            val s = (p.first + " " + p.second).lowercase()
            return s.contains("note") || s.contains("memo") || s.contains("keep")
        }
        return all.sortedWith(compareBy({ !notesLike(it) }, { it.second.lowercase() }))
    }

    /** Pick the app once; it is remembered and notes then go straight to it. */
    fun pickDialog(a: Activity, onPicked: () -> Unit) {
        val t = targets(a)
        if (t.isEmpty()) {
            Toast.makeText(a, "No apps found", Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(a)
            .setTitle("Always save notes to…")
            .setItems(t.map { it.second }.toTypedArray()) { _, i ->
                setMode(a, t[i].first)
                onPicked()
            }
            .show()
    }
}
