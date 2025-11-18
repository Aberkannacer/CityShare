package be.student.cityshare.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import be.student.cityshare.model.SavedPlace
import be.student.cityshare.ui.places.PlacesViewModel
import com.google.android.gms.location.LocationServices
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
fun WorldMapScreen(
    navController: NavController,
    placesViewModel: PlacesViewModel
) {
    val context = LocalContext.current
    val places by placesViewModel.places.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val mapView = remember {
        MapView(context).apply { onCreate(null) }
    }

    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    val markerPlaceMap = remember { mutableStateMapOf<Long, String>() }

    DisposableEffect(Unit) {
        mapView.onStart()
        mapView.onResume()

        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(map, places, selectedCategory) {
        val placesToDisplay = if (selectedCategory == null) {
            places
        } else {
            places.filter { it.category == selectedCategory }
        }
        updateMarkers(map, placesToDisplay, markerPlaceMap, context, selectedCategory != null)

        // Stop elke lopende animatie voordat we een nieuwe starten
        map?.cancelTransitions()

        // Alleen animeren als er een filter is geselecteerd
        if (selectedCategory != null) {
            when {
                // Eén resultaat: zoom direct naar die locatie (lost crash op)
                placesToDisplay.size == 1 -> {
                    val singlePlace = placesToDisplay.first()
                    val target = LatLng(singlePlace.latitude, singlePlace.longitude)
                    val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(target)
                            .zoom(15.0) // Goed zoomniveau voor één locatie
                            .build()
                    )
                    map?.easeCamera(cameraUpdate, 1000)
                }
                // Meerdere resultaten: bouw een kader en zoom daar naartoe
                placesToDisplay.size > 1 -> {
                    val boundsBuilder = LatLngBounds.Builder()
                    placesToDisplay.forEach { place ->
                        boundsBuilder.include(LatLng(place.latitude, place.longitude))
                    }
                    val bounds = boundsBuilder.build()
                    map?.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150), 1000)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        ) { view ->
            view.getMapAsync { mapLibreMap ->
                map = mapLibreMap

                mapLibreMap.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                    val worldPosition = CameraPosition.Builder()
                        .target(LatLng(0.0, 0.0))
                        .zoom(1.5)
                        .build()
                    mapLibreMap.cameraPosition = worldPosition

                    if (
                        ContextCompat.checkSelfPermission(
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

                    mapLibreMap.addOnMapClickListener { point ->
                        val lat = point.latitude
                        val lng = point.longitude
                        navController.navigate("add_place/$lat/$lng")
                        true
                    }

                    mapLibreMap.setOnInfoWindowClickListener { marker ->
                        val placeId = markerPlaceMap[marker.id]
                        if (placeId != null) {
                            navController.navigate("place_detail/$placeId")
                        }
                        true
                    }
                }
            }
        }

        if (showFilterDialog) {
            val categories = remember(places) {
                listOf("Alle Categorieën") + places.map { it.category }.distinct().sorted()
            }
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filter op categorie") },
                text = {
                    LazyColumn {
                        items(categories) { category ->
                            Text(
                                text = category,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCategory = if (category == "Alle Categorieën") null else category
                                        showFilterDialog = false
                                    }
                                    .padding(16.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showFilterDialog = false }) {
                        Text("Annuleren")
                    }
                }
            )
        }

        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Ga naar home")
        }

        val MIN_ZOOM = 1.0
        val MAX_ZOOM = 18.0

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(onClick = { showFilterDialog = true }) {
                Icon(Icons.Default.FilterList, contentDescription = "Filter plaatsen")
            }

            FloatingActionButton(
                onClick = {
                    map?.let { m ->
                        val current = m.cameraPosition
                        val newZoom = (current.zoom + 1.0).coerceAtMost(MAX_ZOOM)
                        val newPosition = CameraPosition.Builder(current).zoom(newZoom).build()
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
                        val newPosition = CameraPosition.Builder(current).zoom(newZoom).build()
                        m.cameraPosition = newPosition
                    }
                }
            ) {
                Text("−")
            }
        }
    }
}

private fun updateMarkers(
    map: MapLibreMap?,
    places: List<SavedPlace>,
    markerPlaceMap: MutableMap<Long, String>,
    context: Context,
    isFiltered: Boolean
) {
    if (map == null) return

    map.clear()
    markerPlaceMap.clear()

    if (places.isEmpty()) return

    val markerIcon = if (isFiltered) {
        val size = 75
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = android.graphics.Color.BLUE
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        IconFactory.getInstance(context).fromBitmap(bitmap)
    } else {
        null // Use default marker
    }

    places.forEach { place ->
        val latLng = LatLng(place.latitude, place.longitude)

        val markerOptions = MarkerOptions()
            .position(latLng)
            .title(place.title)
            .snippet(place.category)

        markerIcon?.let { markerOptions.icon(it) }

        val marker = map.addMarker(markerOptions)
        markerPlaceMap[marker.id] = place.id
    }
}
