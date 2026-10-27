package com.satguess.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.satguess.R
import com.satguess.data.Location
import com.satguess.game.Game
import com.satguess.game.GeoPoint
import com.satguess.game.Mode
import com.satguess.map.GUESS_STYLE_URL
import com.satguess.map.MAX_VIEW_WIDTH_KM
import com.satguess.map.MIN_VIEW_WIDTH_KM
import com.satguess.map.MapHandle
import com.satguess.map.MapLibreView
import com.satguess.map.PAN_LIMIT_KM
import com.satguess.map.satelliteStyleJson
import com.satguess.map.showGuess
import com.satguess.map.zoomForWidth
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import kotlin.math.cos

@Composable
fun GameScreen(game: Game, onGuess: (Double, Double) -> Unit, onSubmit: () -> Unit) {
    var satellite by remember { mutableStateOf<MapHandle?>(null) }

    Box(Modifier.fillMaxSize().background(Night)) {
        // Une nouvelle vue par manche : la caméra et ses limites dépendent du lieu.
        key(game.roundIndex) {
            MapLibreView(Style.Builder().fromJson(satelliteStyleJson()), Modifier.fillMaxSize()) { handle ->
                setUpSatellite(handle, game.location, game.seed.mode)
                satellite = handle
            }
        }

        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(12.dp)) {
            TopBar(game)
            if (game.seed.mode == Mode.MOVE) {
                FilledTonalIconButton(
                    onClick = { satellite?.let { resetSatellite(it, game.location) } },
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                ) { Icon(Icons.Default.MyLocation, stringResource(R.string.back_to_start)) }
            }
            Box(Modifier.weight(1f))
            key(game.roundIndex) { GuessPanel(game.guess, onGuess, onSubmit) }
        }
    }
}

@Composable
private fun TopBar(game: Game) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Night.copy(alpha = 0.75f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(stringResource(R.string.round_x, game.roundIndex + 1, game.locations.size), fontWeight = FontWeight.Bold)
            Text(
                stringResource(R.string.seed_x, game.seed.encode()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        game.timeLeft?.let { left ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, Modifier.size(18.dp), tint = if (left <= 10) GuessRed else Color.White)
                Text(
                    stringResource(R.string.time_left, left),
                    color = if (left <= 10) GuessRed else Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        Text(stringResource(R.string.score_x, formatScore(game.totalScore)), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GuessPanel(guess: GeoPoint?, onGuess: (Double, Double) -> Unit, onSubmit: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var handle by remember { mutableStateOf<MapHandle?>(null) }
    LaunchedEffect(handle, guess) { handle?.style?.showGuess(guess) }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val mapModifier = if (expanded) Modifier.fillMaxWidth().height(maxHeight * 0.6f)
        else Modifier.size(width = 180.dp, height = 130.dp)
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
            Card(modifier = mapModifier.animateContentSize(), shape = RoundedCornerShape(12.dp)) {
                Box {
                    MapLibreView(Style.Builder().fromUri(GUESS_STYLE_URL), Modifier.fillMaxSize()) { h ->
                        h.map.uiSettings.apply {
                            isRotateGesturesEnabled = false
                            isTiltGesturesEnabled = false
                            isCompassEnabled = false
                        }
                        h.map.cameraPosition = CameraPosition.Builder().target(LatLng(20.0, 0.0)).zoom(0.0).build()
                        h.map.addOnMapClickListener { point ->
                            onGuess(point.latitude, point.longitude.wrapLongitude())
                            true
                        }
                        handle = h
                    }
                    if (!expanded) {
                        // En mode réduit, un appui agrandit la carte au lieu de poser l'épingle.
                        Box(Modifier.fillMaxSize().clickable { expanded = true })
                    }
                    FilledTonalIconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(36.dp),
                    ) {
                        Icon(
                            if (expanded) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                            stringResource(if (expanded) R.string.collapse_map else R.string.expand_map),
                            Modifier.size(18.dp),
                        )
                    }
                }
            }
            Button(
                onClick = onSubmit,
                enabled = guess != null,
                modifier = Modifier.padding(top = 8.dp).then(if (expanded) Modifier.fillMaxWidth() else Modifier.size(width = 180.dp, height = 48.dp)),
            ) {
                Text(stringResource(if (guess == null) R.string.place_pin else R.string.validate), maxLines = 1)
            }
        }
    }
}

private fun Double.wrapLongitude(): Double = ((this + 180) % 360 + 360) % 360 - 180

private fun startCamera(handle: MapHandle, location: Location): CameraPosition {
    val width = handle.widthDp.takeIf { it > 0 } ?: 400.0
    return CameraPosition.Builder()
        .target(LatLng(location.lat, location.lon))
        .zoom(zoomForWidth(location.viewKm, location.lat, width))
        .bearing(0.0)
        .tilt(0.0)
        .build()
}

private fun setUpSatellite(handle: MapHandle, location: Location, mode: Mode) {
    val map = handle.map
    map.uiSettings.apply {
        isCompassEnabled = false
        isRotateGesturesEnabled = false
        isTiltGesturesEnabled = false
    }
    map.cameraPosition = startCamera(handle, location)
    when (mode) {
        Mode.NO_MOVE -> map.uiSettings.setAllGesturesEnabled(false)
        Mode.MOVE -> {
            val width = handle.widthDp.takeIf { it > 0 } ?: 400.0
            map.setMinZoomPreference(zoomForWidth(MAX_VIEW_WIDTH_KM, location.lat, width))
            map.setMaxZoomPreference(zoomForWidth(MIN_VIEW_WIDTH_KM, location.lat, width))
            map.setLatLngBoundsForCameraTarget(boundsAround(location.lat, location.lon, PAN_LIMIT_KM))
        }
    }
}

private fun resetSatellite(handle: MapHandle, location: Location) {
    handle.map.easeCamera(CameraUpdateFactory.newCameraPosition(startCamera(handle, location)), 600)
}

private fun boundsAround(lat: Double, lon: Double, km: Double): LatLngBounds {
    val dLat = km / 111.32
    val dLon = km / (111.32 * cos(Math.toRadians(lat)).coerceAtLeast(0.05))
    return LatLngBounds.from(
        (lat + dLat).coerceAtMost(85.0),
        (lon + dLon).coerceAtMost(180.0),
        (lat - dLat).coerceAtLeast(-85.0),
        (lon - dLon).coerceAtLeast(-180.0),
    )
}
