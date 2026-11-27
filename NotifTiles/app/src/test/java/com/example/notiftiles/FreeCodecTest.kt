package com.example.notiftiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeCodecTest {
    private val keys = listOf("clock", "maps", "d0")

    @Test
    fun roundTrip() {
        val m = mapOf("clock" to FreeCodec.Spot(8f, 4f, 0f), "maps" to FreeCodec.Spot(100.5f, 12f, 150f))
        val back = FreeCodec.parse(FreeCodec.encode(m, keys), keys)
        assertEquals(2, back.size)
        assertEquals(100.5f, back.getValue("maps").x, 0.001f)
        assertEquals(150f, back.getValue("maps").w, 0.001f)
    }

    @Test
    fun ignoresUnknownAndBrokenParts() {
        val back = FreeCodec.parse("clock=1,2,3;nope=1,2,3;maps=abc;d0=5,6", keys)
        assertTrue(back.containsKey("clock"))
        assertTrue(back.containsKey("d0"))
        assertFalse(back.containsKey("nope"))
        assertFalse(back.containsKey("maps"))
        assertEquals(0f, back.getValue("d0").w, 0.001f)
    }

    @Test
    fun emptyTextGivesNothing() {
        assertTrue(FreeCodec.parse("", keys).isEmpty())
    }
}
