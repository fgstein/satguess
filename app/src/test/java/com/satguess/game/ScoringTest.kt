package com.satguess.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringTest {
    @Test
    fun parisLondon() {
        assertEquals(343.5, haversineKm(48.8566, 2.3522, 51.5074, -0.1278), 1.0)
    }

    @Test
    fun scoreCurve() {
        assertEquals(5000, roundScore(0.0))
        assertEquals(5000, roundScore(0.99))
        assertEquals(3033, roundScore(1000.0))
        assertEquals(410, roundScore(5000.0))
        assertEquals(0, roundScore(20_000.0))
    }

    @Test
    fun greatCircleCrossingAntimeridianStaysContinuous() {
        val line = greatCircle(35.0, 170.0, 40.0, -170.0)
        line.zipWithNext().forEach { (a, b) -> assertTrue(kotlin.math.abs(a.second - b.second) < 5) }
        assertEquals(190.0, line.last().second, 1e-6)
    }
}
