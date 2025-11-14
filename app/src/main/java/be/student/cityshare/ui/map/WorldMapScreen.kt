package be.student.cityshare.ui.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
fun WorldMapScreen() {
    val context = LocalContext.current

    val mapView = remember {
        MapView(context).apply { onCreate(null) }
    }

    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    DisposableEffect(Unit) {
        mapView.onStart()
        mapView.onResume()

        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { mapView }
        ) { view ->
            view.getMapAsync { mapLibreMap ->
                map = mapLibreMap

                mapLibreMap.setStyle("https://tiles.openfreemap.org/styles/liberty") {
                    val worldPosition = CameraPosition.Builder()
                        .target(LatLng(0.0, 0.0))
                        .zoom(1.5)
                        .build()

                    mapLibreMap.cameraPosition = worldPosition
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    map?.let { m ->
                        val current = m.cameraPosition
                        val newPosition = CameraPosition.Builder(current)
                            .zoom(current.zoom + 1.0)
                            .build()
                        m.cameraPosition = newPosition
                    }
                }
            ) {
                Text("+")
            }

            FloatingActionButton(
                onClick = {
                    map?.let { m ->
                        val current = m.cameraPosition
                        val newPosition = CameraPosition.Builder(current)
                            .zoom(current.zoom - 1.0)
                            .build()
                        m.cameraPosition = newPosition
                    }
                }
            ) {
                Text("−")
            }
        }
    }
}