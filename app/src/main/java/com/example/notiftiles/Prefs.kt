package com.example.notiftiles

import android.content.Context

class Note(val id: String, val text: String)

object Prefs {
    val USUAL = listOf(
        // WhatsApp
        "com.whatsapp", "com.whatsapp.w4b",
        // Facebook
        "com.facebook.katana", "com.facebook.orca", "com.facebook.lite",
        // Phone / calls
        "com.google.android.dialer", "com.samsung.android.dialer", "com.samsung.android.incallui",
        "com.android.server.telecom", "com.android.dialer", "com.android.incallui",
        "com.android.phone", "com.android.contacts",
        // Messages / SMS
        "com.google.android.apps.messaging", "com.samsung.android.messaging",
        "com.android.mms", "com.android.messaging",
        // Mail and work
        "com.google.android.gm", "com.microsoft.office.outlook", "com.microsoft.teams",
        // Social
        "com.instagram.android", "com.snapchat.android"
    )

    private fun sp(c: Context) = c.getSharedPreferences("nt", Context.MODE_PRIVATE)

    fun enabled(c: Context): Set<String> =
        HashSet(sp(c).getStringSet("enabled", emptySet()) ?: emptySet())

    fun setEnabled(c: Context, pkg: String, on: Boolean) {
        val s = HashSet(enabled(c))
        if (on) s.add(pkg) else s.remove(pkg)
        sp(c).edit().putStringSet("enabled", s).apply()
    }

    fun enableAll(c: Context, pkgs: Collection<String>) {
        val s = HashSet(enabled(c))
        s.addAll(pkgs)
        sp(c).edit().putStringSet("enabled", s).apply()
    }

    fun seen(c: Context): Set<String> =
        HashSet(sp(c).getStringSet("seen", emptySet()) ?: emptySet())

    fun addSeen(c: Context, pkg: String) {
        val s = HashSet(seen(c))
        if (s.add(pkg)) sp(c).edit().putStringSet("seen", s).apply()
    }

    fun notes(c: Context): List<Note> {
        val raw = sp(c).getString("notes", "[]") ?: "[]"
        return try {
            val arr = org.json.JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Note(o.getString("id"), o.getString("text"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveNotes(c: Context, list: List<Note>) {
        val arr = org.json.JSONArray()
        for (n in list) {
            val o = org.json.JSONObject()
            o.put("id", n.id)
            o.put("text", n.text)
            arr.put(o)
        }
        sp(c).edit().putString("notes", arr.toString()).apply()
    }

    fun addNote(c: Context, text: String) {
        saveNotes(c, listOf(Note(System.currentTimeMillis().toString(), text)) + notes(c))
    }

    fun updateNote(c: Context, id: String, text: String) {
        saveNotes(c, notes(c).map { if (it.id == id) Note(id, text) else it })
    }

    fun deleteNote(c: Context, id: String) {
        saveNotes(c, notes(c).filter { it.id != id })
    }
}
