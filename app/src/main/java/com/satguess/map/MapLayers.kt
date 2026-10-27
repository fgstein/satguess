package com.satguess.map

import com.satguess.game.GeoPoint
import com.satguess.game.RoundResult
import com.satguess.game.greatCircle
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineDasharray
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

const val GUESS_COLOR = "#E53935"
const val ANSWER_COLOR = "#2E7D32"

private const val GUESS_SOURCE = "guess-points"
private const val ANSWER_SOURCE = "answer-points"
private const val LINE_SOURCE = "result-lines"

/** Ajoute (une seule fois) les sources et couches des épingles et des lignes de résultat. */
private fun Style.ensurePinLayers() {
    if (getSource(GUESS_SOURCE) != null) return
    addSource(GeoJsonSource(LINE_SOURCE))
    addSource(GeoJsonSource(ANSWER_SOURCE))
    addSource(GeoJsonSource(GUESS_SOURCE))
    addLayer(
        LineLayer("result-lines", LINE_SOURCE).withProperties(
            lineColor("#263238"), lineWidth(2.5f), lineDasharray(arrayOf(2f, 1.5f)),
        )
    )
    addLayer(pinLayer("answer-pins", ANSWER_SOURCE, ANSWER_COLOR))
    addLayer(pinLayer("guess-pins", GUESS_SOURCE, GUESS_COLOR))
}

private fun pinLayer(id: String, source: String, color: String) = CircleLayer(id, source).withProperties(
    circleColor(color), circleRadius(8f), circleStrokeColor("#FFFFFF"), circleStrokeWidth(2.5f),
)

private fun points(list: List<GeoPoint>) =
    FeatureCollection.fromFeatures(list.map { Feature.fromGeometry(Point.fromLngLat(it.lon, it.lat)) })

/** Affiche (ou retire) l'épingle du joueur sur la carte de réponse. */
fun Style.showGuess(guess: GeoPoint?) {
    ensurePinLayers()
    getSourceAs<GeoJsonSource>(GUESS_SOURCE)?.setGeoJson(points(listOfNotNull(guess)))
}

/** Affiche les épingles, les vrais lieux et les lignes qui les relient. */
fun Style.showResults(results: List<RoundResult>) {
    ensurePinLayers()
    getSourceAs<GeoJsonSource>(GUESS_SOURCE)?.setGeoJson(points(results.mapNotNull { it.guess }))
    getSourceAs<GeoJsonSource>(ANSWER_SOURCE)?.setGeoJson(
        points(results.map { GeoPoint(it.location.lat, it.location.lon) })
    )
    val lines = results.mapNotNull { r ->
        r.guess?.let { g ->
            val path = greatCircle(g.lat, g.lon, r.location.lat, r.location.lon)
            Feature.fromGeometry(LineString.fromLngLats(path.map { (lat, lon) -> Point.fromLngLat(lon, lat) }))
        }
    }
    getSourceAs<GeoJsonSource>(LINE_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(lines))
}

/** Cadre la caméra sur un ensemble de points (zoom raisonnable s'ils sont confondus). */
fun MapLibreMap.fitTo(list: List<GeoPoint>, paddingPx: Int) {
    if (list.isEmpty()) return
    val distinct = list.distinctBy { it.lat to it.lon }
    if (distinct.size == 1) {
        moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(distinct[0].lat, distinct[0].lon), 6.0))
        return
    }
    val bounds = LatLngBounds.Builder().apply { distinct.forEach { include(LatLng(it.lat, it.lon)) } }.build()
    moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx))
    if (cameraPosition.zoom > 12) moveCamera(CameraUpdateFactory.zoomTo(12.0))
}
