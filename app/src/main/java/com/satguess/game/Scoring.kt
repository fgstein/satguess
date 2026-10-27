package com.satguess.game

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

const val ROUNDS = 5
const val MAX_ROUND_SCORE = 5000
const val EARTH_RADIUS_KM = 6371.0088

/** Distance de grand cercle (haversine) en km. */
fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val p1 = Math.toRadians(lat1)
    val p2 = Math.toRadians(lat2)
    val dp = p2 - p1
    val dl = Math.toRadians(lon2 - lon1)
    val h = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
    return 2 * EARTH_RADIUS_KM * asin(sqrt(h.coerceIn(0.0, 1.0)))
}

/** Points d'une manche (specs §5) : 5000 × e^(−d/2000), 5000 d'office sous 1 km. */
fun roundScore(distanceKm: Double): Int =
    if (distanceKm < 1.0) MAX_ROUND_SCORE
    else (MAX_ROUND_SCORE * exp(-distanceKm / 2000.0)).roundToInt()

/**
 * Points intermédiaires du grand cercle entre deux positions, pour tracer la ligne de résultat.
 * Les longitudes sont « déroulées » (peuvent dépasser ±180) pour éviter un trait à travers toute la carte.
 */
fun greatCircle(lat1: Double, lon1: Double, lat2: Double, lon2: Double, segments: Int = 64): List<Pair<Double, Double>> {
    val p1 = Math.toRadians(lat1); val l1 = Math.toRadians(lon1)
    val p2 = Math.toRadians(lat2); val l2 = Math.toRadians(lon2)
    val d = haversineKm(lat1, lon1, lat2, lon2) / EARTH_RADIUS_KM
    if (d < 1e-9) return listOf(lat1 to lon1, lat2 to lon2)
    val points = ArrayList<Pair<Double, Double>>(segments + 1)
    var previousLon = lon1
    for (i in 0..segments) {
        val f = i.toDouble() / segments
        val a = sin((1 - f) * d) / sin(d)
        val b = sin(f * d) / sin(d)
        val x = a * cos(p1) * cos(l1) + b * cos(p2) * cos(l2)
        val y = a * cos(p1) * sin(l1) + b * cos(p2) * sin(l2)
        val z = a * sin(p1) + b * sin(p2)
        val lat = Math.toDegrees(atan2(z, sqrt(x * x + y * y)))
        var lon = Math.toDegrees(atan2(y, x))
        while (lon - previousLon > 180) lon -= 360
        while (lon - previousLon < -180) lon += 360
        previousLon = lon
        points += lat to lon
    }
    return points
}
