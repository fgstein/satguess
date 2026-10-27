package com.satguess.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class SeedTest {
    @Test
    fun encodeParseRoundTrip() {
        val random = Random(42)
        repeat(1000) {
            val seed = Seed.random(if (it % 2 == 0) Mode.MOVE else Mode.NO_MOVE, random.nextInt(0, 128), random)
            assertEquals(seed, Seed.parse(seed.encode()))
        }
    }

    @Test
    fun formatAndExtremes() {
        assertEquals("0000-0000", Seed(Mode.MOVE, 0, 0).encode())
        assertEquals("ZZZZ-ZZZZ", Seed(Mode.NO_MOVE, 127, 0xFFFF_FFFFL).encode())
    }

    @Test
    fun parseIsLenient() {
        val seed = Seed(Mode.NO_MOVE, 1, 123456789)
        val code = seed.encode()
        assertEquals(seed, Seed.parse(code.lowercase()))
        assertEquals(seed, Seed.parse(code.replace("-", "")))
        assertEquals(seed, Seed.parse(" ${code.take(4)} ${code.drop(5)} "))
        assertEquals(Seed.parse("1000-0000"), Seed.parse("I000-OOOO"))
        assertEquals(Seed.parse("1000-0000"), Seed.parse("l000-0000"))
    }

    @Test
    fun parseRejectsInvalid() {
        assertNull(Seed.parse(""))
        assertNull(Seed.parse("ABC"))
        assertNull(Seed.parse("ABCD-EFGHJ"))
        assertNull(Seed.parse("ABCD-EFGU")) // U n'est pas dans l'alphabet Crockford
    }

    @Test
    fun splitMixKnownValues() {
        // Valeurs de référence de SplitMix64 pour la graine 0.
        val rng = SplitMix64(0)
        assertEquals(-0x1ddf57c684e23251L, rng.nextLong()) // 0xE220A8397B1DCDAF
        assertEquals(0x6e789e6aa1b965f4L, rng.nextLong())
    }

    @Test
    fun pickIsDeterministicAndIndependentOfInputOrder() {
        val ids = (1..10_000).map { "Q$it" }
        val seed = Seed(Mode.MOVE, 1, 987654321)
        val a = pickForSeed(ids, seed, 5) { it }
        val b = pickForSeed(ids.shuffled(Random(7)), seed, 5) { it }
        assertEquals(a, b)
        assertEquals(5, a.toSet().size)
        assertNotEquals(a, pickForSeed(ids, seed.copy(value = 1), 5) { it })
    }
}
