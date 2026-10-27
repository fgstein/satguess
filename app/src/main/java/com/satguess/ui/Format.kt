package com.satguess.ui

import com.satguess.data.Units
import java.util.Locale

fun formatScore(score: Int): String = String.format(Locale.getDefault(), "%,d", score)

fun formatDistance(km: Double, units: Units): String {
    val locale = Locale.getDefault()
    return when (units) {
        Units.KM -> when {
            km < 1 -> String.format(locale, "%d m", (km * 1000).toInt())
            km < 100 -> String.format(locale, "%.1f km", km)
            else -> String.format(locale, "%,d km", km.toInt())
        }
        Units.MI -> {
            val mi = km / 1.609344
            when {
                mi < 0.1 -> String.format(locale, "%d ft", (mi * 5280).toInt())
                mi < 100 -> String.format(locale, "%.1f mi", mi)
                else -> String.format(locale, "%,d mi", mi.toInt())
            }
        }
    }
}
