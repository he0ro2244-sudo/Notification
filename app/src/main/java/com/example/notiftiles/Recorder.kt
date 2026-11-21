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

    // Components of the recorder that look like "start recording" entry points.
    private val START_NAME = Regex("(start|quick|shortcut|direct|fast).*(record|rec)|(record|rec).*(start|quick|shortcut|direct|fast)", RegexOption.IGNORE_CASE)

    @Suppress("DEPRECATION")
    private fun components(c: Context, pkg: String): List<Triple<String, String, Boolean>> = try {
        val pi = c.packageManager.getPackageInfo(
            pkg,
            android.content.pm.PackageManager.GET_ACTIVITIES or
                android.content.pm.PackageManager.GET_SERVICES or
                android.content.pm.PackageManager.GET_RECEIVERS
        )
        val out = ArrayList<Triple<String, String, Boolean>>()
        pi.activities?.forEach { out.add(Triple("activity", it.name, it.exported)) }
        pi.services?.forEach { out.add(Triple("service", it.name, it.exported)) }
        pi.receivers?.forEach { out.add(Triple("receiver", it.name, it.exported)) }
        out
    } catch (e: Exception) {
        emptyList()
    }

    private fun recorderPkg(c: Context): String? {
        val m = mode(c)
        if (m != "auto") return m
        return installedApps(c).firstOrNull()?.first
    }

    /** Tries to start recording right away in the phone's Recorder; falls back to just opening it. */
    fun startRecording(c: Context): Boolean {
        val pkg = recorderPkg(c)
        if (pkg != null) {
            // 1) the standard Android "record sound" request, aimed at the recorder
            if (start(c, recordIntent().setPackage(pkg))) return true
            // 2) an exported activity of the recorder named like a start-recording shortcut
            for ((kind, name, exported) in components(c, pkg)) {
                if (kind != "activity" || !exported || !START_NAME.containsMatchIn(name)) continue
                if (start(c, Intent().setClassName(pkg, name))) return true
            }
        }
        return open(c)
    }

    /** Text listing the recorder's entry points, for finding the right "start recording" command. */
    fun diagnose(c: Context): String {
        val pkg = recorderPkg(c) ?: return "No known recorder app found. Pick one first."
        val sb = StringBuilder("Recorder: $pkg (${label(c, pkg)})\n")
        sb.append("RECORD_SOUND answered: ")
            .append(try { recordIntent().setPackage(pkg).resolveActivity(c.packageManager) != null } catch (e: Exception) { false })
            .append("\n\n")
        val dump = manifestDump(c, pkg)
        if (dump.isNotEmpty()) {
            sb.append("Entry points and their actions:\n").append(dump)
        } else {
            sb.append("Exported entry points:\n")
            for ((kind, name, exported) in components(c, pkg)) if (exported) sb.append("$kind $name\n")
        }
        return sb.toString()
    }

    // ---- Reads the recorder's AndroidManifest.xml (binary XML) and lists components with their intent filters ----
    private fun manifestDump(c: Context, pkg: String): String = try {
        val apk = c.packageManager.getApplicationInfo(pkg, 0).sourceDir
        val bytes = java.util.zip.ZipFile(apk).use { z ->
            z.getInputStream(z.getEntry("AndroidManifest.xml")).readBytes()
        }
        parseManifest(bytes)
    } catch (e: Throwable) {
        "(could not read manifest: $e)\n"
    }

    private fun readStrings(bb: java.nio.ByteBuffer, start: Int): List<String> {
        val count = bb.getInt(start + 8)
        val flags = bb.getInt(start + 16)
        val strStart = bb.getInt(start + 20)
        val utf8 = (flags and 0x100) != 0
        val out = ArrayList<String>()
        for (i in 0 until count) {
            var p = start + strStart + bb.getInt(start + 28 + i * 4)
            if (utf8) {
                val l0 = bb.get(p).toInt() and 0xFF
                p += if ((l0 and 0x80) != 0) 2 else 1
                var bl = bb.get(p).toInt() and 0xFF
                if ((bl and 0x80) != 0) {
                    bl = ((bl and 0x7F) shl 8) or (bb.get(p + 1).toInt() and 0xFF)
                    p += 2
                } else {
                    p += 1
                }
                val arr = ByteArray(bl)
                for (j in 0 until bl) arr[j] = bb.get(p + j)
                out.add(String(arr, Charsets.UTF_8))
            } else {
                var l = bb.getShort(p).toInt() and 0xFFFF
                p += 2
                if ((l and 0x8000) != 0) {
                    l = ((l and 0x7FFF) shl 16) or (bb.getShort(p).toInt() and 0xFFFF)
                    p += 2
                }
                val t = StringBuilder()
                for (j in 0 until l) t.append(bb.getShort(p + j * 2).toInt().toChar())
                out.add(t.toString())
            }
        }
        return out
    }

    private fun parseManifest(b: ByteArray): String {
        val bb = java.nio.ByteBuffer.wrap(b).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        var strings: List<String> = emptyList()
        val comps = setOf("activity", "activity-alias", "service", "receiver", "provider")
        val out = StringBuilder()
        var cur: StringBuilder? = null
        var keep = false
        var pos = 8
        while (pos + 8 <= b.size) {
            val type = bb.getShort(pos).toInt() and 0xFFFF
            val size = bb.getInt(pos + 4)
            if (size <= 0) break
            when (type) {
                0x0001 -> strings = readStrings(bb, pos)
                0x0102 -> {
                    val name = strings.getOrElse(bb.getInt(pos + 20)) { "" }
                    val attrStart = bb.getShort(pos + 24).toInt() and 0xFFFF
                    val attrCount = bb.getShort(pos + 28).toInt() and 0xFFFF
                    val base = pos + 16 + attrStart
                    val attrs = LinkedHashMap<String, String>()
                    for (i in 0 until attrCount) {
                        val o = base + i * 20
                        val an = strings.getOrElse(bb.getInt(o + 4)) { "" }
                        val raw = bb.getInt(o + 8)
                        val dtype = bb.get(o + 15).toInt() and 0xFF
                        val data = bb.getInt(o + 16)
                        val v = when {
                            raw != -1 -> strings.getOrElse(raw) { "" }
                            dtype == 0x12 -> if (data != 0) "true" else "false"
                            dtype == 0x03 -> strings.getOrElse(data) { "" }
                            else -> data.toString()
                        }
                        attrs[an] = v
                    }
                    val open = cur
                    if (name in comps) {
                        val nb = StringBuilder("\n$name ${attrs["name"] ?: ""}")
                        val ta = attrs["targetActivity"]
                        if (ta != null) nb.append(" -> ").append(ta)
                        val ex = attrs["exported"]
                        if (ex != null) nb.append(" exported=").append(ex)
                        cur = nb
                        keep = ex == "true"
                    } else if (open != null) {
                        when (name) {
                            "intent-filter" -> { keep = true; open.append("\n  [filter]") }
                            "action" -> open.append("\n    action ").append(attrs["name"] ?: "")
                            "category" -> open.append("\n    category ").append(attrs["name"] ?: "")
                            "data" -> open.append("\n    data ").append(attrs.entries.joinToString(" ") { it.key + "=" + it.value })
                        }
                    }
                }
                0x0103 -> {
                    val name = strings.getOrElse(bb.getInt(pos + 20)) { "" }
                    val done = cur
                    if (name in comps && done != null) {
                        if (keep) out.append(done).append("\n")
                        cur = null
                    }
                }
            }
            pos += size
        }
        return out.toString()
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

    fun pickDialog(a: Activity, onPicked: () -> Unit, onCancel: () -> Unit = {}) {
        val t = targets(a)
        if (t.isEmpty()) {
            Toast.makeText(a, "No apps found", Toast.LENGTH_SHORT).show()
            return
        }
        android.app.AlertDialog.Builder(a)
            .setTitle("Record with…")
            .setOnCancelListener { onCancel() }
            .setItems(t.map { it.second }.toTypedArray()) { _, i ->
                setMode(a, t[i].first)
                onPicked()
            }
            .show()
    }
}
