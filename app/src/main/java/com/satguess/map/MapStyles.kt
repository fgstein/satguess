package com.satguess.map

import com.satguess.BuildConfig
import kotlin.math.cos
import kotlin.math.log2

/** Carte de réponse (OpenStreetMap via OpenFreeMap, sans clé). */
const val GUESS_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

/** Limites de la vue satellite en mode Move (specs §3). */
const val MAX_VIEW_WIDTH_KM = 50.0
const val MIN_VIEW_WIDTH_KM = 0.2

/** Demi-côté de la zone dans laquelle le centre de la vue satellite peut se déplacer. */
const val PAN_LIMIT_KM = 50.0

/**
 * Style de la vue satellite : une seule couche raster, aucun label.
 * Mapbox Satellite si une clé est fournie, sinon Sentinel-2 cloudless (EOX, CC BY 4.0).
 */
fun satelliteStyleJson(): String {
    val token = BuildConfig.MAPBOX_TOKEN
    val (tiles, tileSize, maxZoom, attribution) = if (token.isNotBlank()) {
        listOf(
            "https://api.mapbox.com/v4/mapbox.satellite/{z}/{x}/{y}@2x.jpg90?access_token=$token",
            512, 19, "© Mapbox © Maxar",
        )
    } else {
        listOf(
            "https://tiles.maps.eox.at/wmts/1.0.0/s2cloudless-2024_3857/default/g/{z}/{y}/{x}.jpg",
            256, 15,
            "Sentinel-2 cloudless – s2maps.eu by EOX IT Services GmbH (Contains modified Copernicus Sentinel data 2024)",
        )
    }
    return """
        {
          "version": 8,
          "sources": {
            "sat": {
              "type": "raster",
              "tiles": ["$tiles"],
              "tileSize": $tileSize,
              "maxzoom": $maxZoom,
              "attribution": "$attribution"
            }
          },
          "layers": [
            {"id": "bg", "type": "background", "paint": {"background-color": "#0b1a2a"}},
            {"id": "sat", "type": "raster", "source": "sat"}
          ]
        }
    """.trimIndent()
}

/**
 * Niveau de zoom MapLibre pour qu'une vue de [widthDp] dp de large couvre [widthKm] km à la latitude [lat].
 * À un zoom z, le monde fait 512 × 2^z dp de large.
 */
fun zoomForWidth(widthKm: Double, lat: Double, widthDp: Double): Double {
    val circumferenceM = 40_075_016.686 * cos(Math.toRadians(lat))
    return log2(circumferenceM * widthDp / (512.0 * widthKm * 1000.0))
}
