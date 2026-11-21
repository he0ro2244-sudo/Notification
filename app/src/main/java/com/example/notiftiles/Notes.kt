package com.example.notiftiles

import android.content.Context
import android.content.Intent

// Sends a note you type to the notes app on the phone (Keep, Samsung Notes, ...) instead of keeping it here.
object Notes {
    val KNOWN = listOf(
        "com.google.android.keep" to "Google Keep",
        "com.samsung.android.app.notes" to "Samsung Notes",
        "com.microsoft.office.onenote" to "OneNote",
        "com.evernote" to "Evernote",
        "com.automattic.simplenote" to "Simplenote",
        "md.obsidian" to "Obsidian"
    )

    private fun sp(c: Context) = c.getSharedPreferences("nt", Context.MODE_PRIVATE)

    /** "auto" (default), "ask" (system chooser), "inapp" (keep inside Notif Tiles) or a package name. */
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

    /** Hands the text to the notes app. Returns false if nothing could take it. */
    fun send(c: Context, text: String): Boolean {
        val base = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        val m = mode(c)
        val pkg = when (m) {
            "ask" -> null
            "auto" -> installedApps(c).firstOrNull()?.first
            else -> m
        }
        return try {
            if (pkg != null) {
                c.startActivity(Intent(base).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                c.startActivity(Intent.createChooser(base, "Save note to").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            true
        } catch (e: Exception) {
            try {
                c.startActivity(Intent.createChooser(base, "Save note to").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (x: Exception) {
                false
            }
        }
    }
}
