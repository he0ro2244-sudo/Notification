package com.example.notiftiles

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// Export / import of every setting (plus the background pictures) as one JSON file.
object Backup {
    private val SKIP = setOf("w_try", "w_has", "w_temp", "w_code", "w_day", "w_time", "handled", "chat_snz", "undo_look", "hist_pencil", "hist_settings", "h2_pencil", "h2_settings", "r2_pencil", "r2_settings")
    private val IMAGES = listOf("widget", "dial", "panel")

    private fun sp(c: Context) = c.getSharedPreferences("nt", Context.MODE_PRIVATE)

    fun export(c: Context): String {
        val prefs = JSONObject()
        for ((k, v) in sp(c).all) {
            if (k in SKIP || v == null) continue
            val o = JSONObject()
            when (v) {
                is Boolean -> o.put("t", "b").put("v", v)
                is Int -> o.put("t", "i").put("v", v)
                is Long -> o.put("t", "l").put("v", v)
                is Float -> o.put("t", "f").put("v", v.toDouble())
                is String -> o.put("t", "s").put("v", v)
                is Set<*> -> o.put("t", "ss").put("v", JSONArray(v.map { it.toString() }))
                else -> continue
            }
            prefs.put(k, o)
        }
        val imgs = JSONObject()
        for (n in IMAGES) {
            val f = File(c.filesDir, "img_$n.jpg")
            if (f.exists()) imgs.put(n, Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
        }
        return JSONObject().put("app", "NotifTiles").put("version", 1).put("prefs", prefs).put("images", imgs).toString(1)
    }

    /** Returns how many settings were restored, or -1 if the text is not a backup. */
    fun import(c: Context, text: String): Int {
        val root = try { JSONObject(text) } catch (e: Exception) { return -1 }
        if (root.optString("app") != "NotifTiles") return -1
        val prefs = root.optJSONObject("prefs") ?: return -1
        val e = sp(c).edit()
        var n = 0
        val keys = prefs.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (k in SKIP) continue
            val o = prefs.optJSONObject(k) ?: continue
            when (o.optString("t")) {
                "b" -> e.putBoolean(k, o.getBoolean("v"))
                "i" -> e.putInt(k, o.getInt("v"))
                "l" -> e.putLong(k, o.getLong("v"))
                "f" -> e.putFloat(k, o.getDouble("v").toFloat())
                "s" -> e.putString(k, o.getString("v"))
                "ss" -> {
                    val a = o.getJSONArray("v")
                    e.putStringSet(k, HashSet<String>().also { s -> for (i in 0 until a.length()) s.add(a.getString(i)) })
                }
                else -> continue
            }
            n++
        }
        e.putLong("w_try", 0L).apply()
        val imgs = root.optJSONObject("images")
        if (imgs != null) {
            for (name in IMAGES) {
                val b64 = imgs.optString(name, "")
                if (b64.isNotEmpty()) {
                    try {
                        File(c.filesDir, "img_$name.jpg").writeBytes(Base64.decode(b64, Base64.DEFAULT))
                    } catch (ex: Exception) {
                        // skip this picture
                    }
                }
            }
        }
        return n
    }
}
