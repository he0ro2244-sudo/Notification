package com.example.notiftiles

import android.content.Context
import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * The offline understanding model. It reads a sentence and decides which kind of change it means ("make X wider",
 * "fix the gap", "undo"...). The answer is then written as a plain sentence the rules already understand, so the rest of
 * the helper does the work. The model is res/raw/router.json, made by tools/router/train.py. It is about 2 MB and needs no internet.
 */
object Router {
    private var loaded = false
    private var classes: List<String> = emptyList()
    private var canon: Map<String, String> = emptyMap()
    private var featIndex: HashMap<String, Int> = HashMap()
    private var weights: Array<FloatArray> = emptyArray()
    private var bias: FloatArray = FloatArray(0)

    /** Below this confidence the model says nothing and the rules and Settings search take over. */
    private const val MIN_CONFIDENCE = 0.5

    @Synchronized
    private fun load(c: Context) {
        if (loaded) return
        loaded = true
        try {
            val text = c.resources.openRawResource(R.raw.router).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val j = JSONObject(text)
            val cl = j.getJSONArray("classes")
            classes = (0 until cl.length()).map { cl.getString(it) }
            val fs = j.getJSONArray("features")
            featIndex = HashMap(fs.length() * 2)
            for (i in 0 until fs.length()) featIndex[fs.getString(i)] = i
            val ws = j.getJSONArray("weights")
            weights = Array(ws.length()) { k ->
                val row = ws.getJSONArray(k)
                FloatArray(row.length()) { i -> row.getDouble(i).toFloat() }
            }
            val b = j.getJSONArray("bias")
            bias = FloatArray(b.length()) { b.getDouble(it).toFloat() }
            val cn = j.getJSONObject("canon")
            canon = cn.keys().asSequence().associateWith { cn.getString(it) }
        } catch (e: Exception) {
            classes = emptyList()
        }
    }

    /** The same features the training script makes: whole words, and 3- and 4-letter pieces of each word. */
    private fun features(text: String): Map<String, Double> {
        val counts = HashMap<String, Double>()
        for (w in text.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }) {
            counts["w:$w"] = (counts["w:$w"] ?: 0.0) + 1.0
            val p = " $w "
            for (n in 3..4) {
                for (i in 0..p.length - n) {
                    val g = "c:" + p.substring(i, i + n)
                    counts[g] = (counts[g] ?: 0.0) + 1.0
                }
            }
        }
        var sq = 0.0
        for (v in counts.values) sq += v * v
        val norm = if (sq == 0.0) 1.0 else sqrt(sq)
        return counts.mapValues { it.value / norm }
    }

    /**
     * A plain sentence the rules understand for [input], or null when the model is not sure or the sentence needs
     * items (clock, mic, ...) that the sentence does not name.
     */
    fun route(c: Context, input: String): String? {
        load(c)
        if (classes.isEmpty() || input.isBlank()) return null
        val x = features(input)
        val scores = DoubleArray(classes.size) { k ->
            var s = bias[k].toDouble()
            for ((f, v) in x) {
                val i = featIndex[f] ?: continue
                s += weights[k][i] * v
            }
            s
        }
        var mx = scores[0]
        for (v in scores) if (v > mx) mx = v
        val exps = DoubleArray(scores.size) { exp(scores[it] - mx) }
        val total = exps.sum()
        var best = 0
        for (k in exps.indices) if (exps[k] > exps[best]) best = k
        val confidence = exps[best] / total
        if (confidence < MIN_CONFIDENCE) return null
        val name = classes[best]
        if (name == "none") return null
        val template = canon[name]?.takeIf { it.isNotEmpty() } ?: return null
        val items = LayoutHelper.itemsIn(input)
        var out = template
        if (out.contains("{a}")) {
            if (items.isEmpty()) return null
            out = out.replace("{a}", items[0])
        }
        if (out.contains("{b}")) {
            if (items.size < 2) return null
            out = out.replace("{b}", items[1])
        }
        return out
    }
}
