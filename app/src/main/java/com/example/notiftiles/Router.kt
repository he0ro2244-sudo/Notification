package com.example.notiftiles

import android.content.Context
import android.util.JsonReader
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * The offline understanding model. It reads a sentence and decides which kind of change it means ("make X wider",
 * "fix the gap", "undo", "add a fast dial", "tidy the rows"...). For the layout kinds the answer is written as a plain sentence the
 * rules already understand ([route]); for the action kinds (fast dial, shortcuts, tidy, notes) the kind itself is returned ([intent])
 * and HelperActions does the work. The model is res/raw/router.json, made by tools/router/train.py. It needs no internet.
 * The file is read piece by piece, so even a model of several MB does not need a big block of memory.
 */
object Router {
    private var loaded = false
    private var classes: List<String> = emptyList()
    private var canon: Map<String, String> = emptyMap()
    private var featIndex: HashMap<String, Int> = HashMap()
    private var weights: Array<FloatArray> = emptyArray()
    private var bias: FloatArray = FloatArray(0)
    private var words: HashSet<String> = HashSet()
    private var concepts: HashMap<String, List<String>> = HashMap()

    /** Below this confidence the model says nothing and the rules and Settings search take over. */
    private const val MIN_CONFIDENCE = 0.6

    /** An action (adding or removing a dial, tidying) needs more certainty than a layout sentence. */
    private const val MIN_ACTION_CONFIDENCE = 0.65

    /** The kinds that are done by HelperActions (they have no sentence for the rules). */
    private val ACTIONS = setOf("dial_add", "dial_remove", "dial_show", "dial_hide", "shortcut_add", "shortcut_remove",
        "shortcut_show", "shortcut_hide", "tidy", "tidy_even", "tidy_pack", "note_add")

    @Synchronized
    private fun load(c: Context) {
        if (loaded) return
        loaded = true
        try {
            JsonReader(c.resources.openRawResource(R.raw.router).bufferedReader(Charsets.UTF_8)).use { r ->
                val cl = ArrayList<String>()
                val fi = HashMap<String, Int>(1 shl 17)
                val ws = ArrayList<FloatArray>()
                var b = FloatArray(0)
                val cn = HashMap<String, String>()
                val wd = HashSet<String>()
                val cp = HashMap<String, List<String>>()
                r.beginObject()
                while (r.hasNext()) {
                    when (r.nextName()) {
                        "classes" -> { r.beginArray(); while (r.hasNext()) cl.add(r.nextString()); r.endArray() }
                        "features" -> { r.beginArray(); var i = 0; while (r.hasNext()) { fi[r.nextString()] = i; i++ }; r.endArray() }
                        "weights" -> {
                            r.beginArray()
                            while (r.hasNext()) {
                                val row = FloatArray(fi.size)
                                var i = 0
                                r.beginArray()
                                while (r.hasNext()) {
                                    val v = r.nextDouble().toFloat()
                                    if (i < row.size) row[i] = v
                                    i++
                                }
                                r.endArray()
                                ws.add(row)
                            }
                            r.endArray()
                        }
                        "bias" -> {
                            val l = ArrayList<Float>()
                            r.beginArray(); while (r.hasNext()) l.add(r.nextDouble().toFloat()); r.endArray()
                            b = FloatArray(l.size) { l[it] }
                        }
                        "canon" -> { r.beginObject(); while (r.hasNext()) { val k = r.nextName(); cn[k] = r.nextString() }; r.endObject() }
                        "words" -> { r.beginArray(); while (r.hasNext()) wd.add(r.nextString()); r.endArray() }
                        "concepts" -> {
                            r.beginObject()
                            while (r.hasNext()) {
                                val k = r.nextName()
                                val l = ArrayList<String>()
                                r.beginArray(); while (r.hasNext()) l.add(r.nextString()); r.endArray()
                                cp[k] = l
                            }
                            r.endObject()
                        }
                        else -> r.skipValue()
                    }
                }
                r.endObject()
                classes = cl
                featIndex = fi
                weights = ws.toTypedArray()
                bias = b
                canon = cn
                words = wd
                concepts = cp
            }
        } catch (e: Throwable) {
            classes = emptyList()
        }
    }

    /**
     * The same features the training script makes: whole words, 3- to 5-letter pieces of each word, and pairs of
     * neighbouring words (so "fast dial" and "dial fast" are not the same).
     */
    private fun features(text: String): Map<String, Double> {
        val counts = HashMap<String, Double>()
        val ws = text.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }
        for ((idx, w) in ws.withIndex()) {
            counts["w:$w"] = (counts["w:$w"] ?: 0.0) + 1.0
            val p = " $w "
            for (n in 3..5) {
                for (i in 0..p.length - n) {
                    val g = "c:" + p.substring(i, i + n)
                    counts[g] = (counts[g] ?: 0.0) + 1.0
                }
            }
            if (idx > 0) {
                val g = "b:" + ws[idx - 1] + " " + w
                counts[g] = (counts[g] ?: 0.0) + 1.0
            }
            val cs = concepts[w]
            if (cs != null) {
                for (cc in cs) {
                    val g = "k:$cc"
                    counts[g] = (counts[g] ?: 0.0) + 1.0
                }
            }
        }
        var sq = 0.0
        for (v in counts.values) sq += v * v
        val norm = if (sq == 0.0) 1.0 else sqrt(sq)
        return counts.mapValues { it.value / norm }
    }

    /** The sentence with each misspelt word changed to the closest word the model was taught (the same rule as the training script). */
    private fun correct(text: String): String {
        val out = ArrayList<String>()
        for (w in text.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }) {
            if (w.length < 4 || words.contains(w)) { out.add(w); continue }
            val cands = words.filter { it.length >= 4 && Helper.near(w, it) }.sorted()
            out.add(cands.firstOrNull { it[0] == w[0] } ?: cands.firstOrNull() ?: w)
        }
        return out.joinToString(" ")
    }

    /** The kind the model picks for [input] and how sure it is, or null when it has no model or picks "none". */
    private fun classify(c: Context, input: String): Pair<String, Double>? {
        load(c)
        if (classes.isEmpty() || input.isBlank() || weights.size != classes.size) return null
        val x = features(if (words.isEmpty()) input else correct(input))
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
        val name = classes[best]
        return if (name == "none") null else Pair(name, confidence)
    }

    /** The action the model sees in [input] (dial_add, shortcut_remove, tidy, ...), or null. HelperActions then does it. */
    fun intent(c: Context, input: String): String? {
        val (name, confidence) = classify(c, input) ?: return null
        return if (name in ACTIONS && confidence >= MIN_ACTION_CONFIDENCE) name else null
    }

    /**
     * A plain sentence the rules understand for [input], or null when the model is not sure or the sentence needs
     * items (clock, mic, ...) that the sentence does not name.
     */
    fun route(c: Context, input: String): String? {
        val (name, confidence) = classify(c, input) ?: return null
        if (confidence < MIN_CONFIDENCE) return null
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
