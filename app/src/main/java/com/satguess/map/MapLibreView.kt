package com.satguess.map

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/** Carte prête à l'emploi : la vue (pour ses dimensions), la carte et son style chargé. */
class MapHandle(val view: MapView, val map: MapLibreMap, val style: Style) {
    val widthDp: Double get() = view.width / view.resources.displayMetrics.density.toDouble()
}

/**
 * MapView MapLibre intégrée à Compose. [onReady] est appelé une fois le style chargé.
 * Le cycle de vie de la vue suit celui de l'écran ; elle est détruite quand le composable disparaît.
 */
@Composable
fun MapLibreView(
    style: Style.Builder,
    modifier: Modifier = Modifier,
    onReady: (MapHandle) -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
            getMapAsync { map -> map.setStyle(style) { loaded -> onReady(MapHandle(this, map, loaded)) } }
        }
    }

    DisposableEffect(lifecycle, mapView) {
        // État atteint par la vue, pour ne la ramener en arrière que d'autant que nécessaire.
        var current = Lifecycle.State.CREATED
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> { mapView.onStart(); current = Lifecycle.State.STARTED }
                Lifecycle.Event.ON_RESUME -> { mapView.onResume(); current = Lifecycle.State.RESUMED }
                Lifecycle.Event.ON_PAUSE -> { mapView.onPause(); current = Lifecycle.State.STARTED }
                Lifecycle.Event.ON_STOP -> { mapView.onStop(); current = Lifecycle.State.CREATED }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (current == Lifecycle.State.RESUMED) mapView.onPause()
            if (current.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
