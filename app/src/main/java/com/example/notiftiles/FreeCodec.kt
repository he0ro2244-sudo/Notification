package com.example.notiftiles

/** Reads and writes the saved places of the free layout ("key=x,y,w;key=x,y,w"). Plain Kotlin, so it can be unit-tested. */
object FreeCodec {
    class Spot(val x: Float, val y: Float, val w: Float)

    fun parse(raw: String, validKeys: Collection<String>): Map<String, Spot> {
        val out = LinkedHashMap<String, Spot>()
        for (part in raw.split(";")) {
            val kv = part.split("=")
            if (kv.size != 2 || kv[0] !in validKeys) continue
            val n = kv[1].split(",").mapNotNull { it.toFloatOrNull() }
            if (n.size >= 2) out[kv[0]] = Spot(n[0], n[1], n.getOrElse(2) { 0f })
        }
        return out
    }

    fun encode(m: Map<String, Spot>, validKeys: Collection<String>): String =
        m.entries.filter { it.key in validKeys }.joinToString(";") { "${it.key}=${it.value.x},${it.value.y},${it.value.w}" }
}
