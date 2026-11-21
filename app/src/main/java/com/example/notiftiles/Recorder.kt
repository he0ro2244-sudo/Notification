package com.example.notiftiles

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast

// Opens the phone's own Recorder app (the one you already use) from the panel.
object Recorder {
    val KNOWN = listOf(
        "com.coloros.soundrecorder" to "Recorder",
        "com.oplus.soundrecorder" to "Recorder",
        "com.oneplus.soundrecorder" to "Recorder",
        "com.google.android.apps.recorder" to "Recorder",
        "com.sec.android.app.voicenote" to "Voice Recorder",
        "com.android.soundrecorder" to "Sound Recorder",
        "com.miui.soundrecorder" to "Recorder",
        "com.huawei.soundrecorder" to "Recorder",
        "com.motorola.soundrecorder" to "Sound Recorder",
        "com.sonymobile.androidapp.audiorecorder" to "Sound Recorder",
        "com.nothing.soundrecorder" to "Voice Recorder",
        "com.lge.voicerecorder" to "Voice Recorder"
    )

    private const val RECORD_SOUND = "android.provider.MediaStore.RECORD_SOUND"

    private fun sp(c: Context) = c.getSharedPreferences("nt", Context.MODE_PRIVATE)

    /** "auto" (the phone's recorder) or a package picked once. */
    fun mode(c: Context): String = sp(c).getString("rec_app", "auto") ?: "auto"
    fun setMode(c: Context, v: String) = sp(c).edit().putString("rec_app", v).apply()

    private fun installed(c: Context, pkg: String): Boolean = try {
        c.packageManager.getApplicationInfo(pkg, 0)
        true
    } catch (e: Exception) {
        false
    }

    fun installedApps(c: Context): List<Pair<String, String>> = KNOWN.filter { installed(c, it.first) }

    fun label(c: Context, pkg: String): String = try {
        c.packageManager.getApplicationLabel(c.packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    private fun start(c: Context, i: Intent): Boolean = try {
        c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }

    private fun launchPkg(c: Context, pkg: String): Boolean {
        val i = c.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        return start(c, i)
    }

    private fun recordIntent() = Intent(RECORD_SOUND)

    fun needsPick(c: Context): Boolean {
        if (mode(c) != "auto") return false
        if (installedApps(c).isNotEmpty()) return false
        return try {
            recordIntent().resolveActivity(c.packageManager) == null
        } catch (e: Exception) {
            true
        }
    }

    fun open(c: Context): Boolean {
        val m = mode(c)
        if (m != "auto") return launchPkg(c, m)
        for ((pkg, _) in installedApps(c)) if (launchPkg(c, pkg)) return true
        return start(c, recordIntent())
    }

    fun targets(c: Context): List<Pair<String, String>> {
        val pm = c.packageManager
        val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val all = pm.queryIntentActivities(launch, 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .filter { it.first != c.packageName }
            .distinctBy { it.first }
        fun recLike(p: Pair<String, String>): Boolean {
            val s = (p.first + " " + p.second).lowercase()
            return s.contains("record") || s.contains("voice") || s.contains("sound") || s.contains("audio")
        }
        return all.sortedWith(compareBy({ !recLike(it) }, { it.second.lowercase() }))
    }

    fun pickDialog(a: Activity, onPicked: () -> Unit) {
        val t = targets(a)
        if (t.isEmpty()) {
            Toast.makeText(a, "No apps found", Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(a)
            .setTitle("Open this recorder…")
            .setItems(t.map { it.second }.toTypedArray()) { _, i ->
                setMode(a, t[i].first)
                onPicked()
            }
            .show()
    }
}
