package be.student.cityshare.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
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

                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val fusedClient = LocationServices.getFusedLocationProviderClient(context)

                        fusedClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                val myPos = CameraPosition.Builder()
                                    .target(LatLng(location.latitude, location.longitude))
                                    .zoom(15.0)
                                    .build()
                                mapLibreMap.cameraPosition = myPos
                            }
                        }
                    }
                }
            }
        }

        val MIN_ZOOM = 1.0
        val MAX_ZOOM = 18.0

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
                        val newZoom = (current.zoom + 1.0).coerceAtMost(MAX_ZOOM)
                        val newPosition = CameraPosition.Builder(current)
                            .zoom(newZoom)
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
                        val newZoom = (current.zoom - 1.0).coerceAtLeast(MIN_ZOOM)
                        val newPosition = CameraPosition.Builder(current)
                            .zoom(newZoom)
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