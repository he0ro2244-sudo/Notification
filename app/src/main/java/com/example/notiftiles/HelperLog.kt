package com.example.notiftiles

import android.content.Context
import java.io.File

/**
 * Keeps the last sentences the helper could not understand, on this phone only (app private storage, never sent anywhere).
 * Read them from Settings > Helper > help > "Sentences it missed". They show which words people really use.
 */
object HelperLog {
    private const val NAME = "helper_misses.txt"
    private const val MAX = 200

    fun add(c: Context, sentence: String) {
        val clean = sentence.trim().replace("\n", " ")
        if (clean.isEmpty()) return
        try {
            val f = File(c.filesDir, NAME)
            val lines = (if (f.exists()) f.readLines() else emptyList()) + clean
            f.writeText(lines.takeLast(MAX).joinToString("\n") + "\n")
        } catch (e: Exception) {
            // Logging must never stop the helper from answering
        }
    }

    fun recent(c: Context, n: Int = 30): List<String> = try {
        val f = File(c.filesDir, NAME)
        if (f.exists()) f.readLines().filter { it.isNotBlank() }.takeLast(n) else emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    fun clear(c: Context) {
        try { File(c.filesDir, NAME).delete() } catch (e: Exception) { }
    }
}
