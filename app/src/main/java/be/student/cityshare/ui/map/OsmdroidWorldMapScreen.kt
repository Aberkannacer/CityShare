package be.student.cityshare.ui.map

import android.Manifest
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import be.student.cityshare.model.SavedPlace
import be.student.cityshare.ui.places.PlacesViewModel
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.heightIn

@Composable
fun OsmdroidWorldMapScreen(
    navController: NavController,
    placesViewModel: PlacesViewModel
) {
    val context = LocalContext.current
    val places by placesViewModel.places.collectAsState()

    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFilterDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)

            controller.setZoom(17.5)
            controller.setCenter(GeoPoint(51.230063993584075, 4.4161542423283064))

            zoomController.setVisibility(
                CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
            )

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val myPoint = GeoPoint(location.latitude, location.longitude)
                        controller.setZoom(17.5)
                        controller.animateTo(myPoint)
                    }
                }
            }
        }
    }

    val lastTapTime = remember { mutableStateOf(0L) }

    val mapTapOverlay = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                val now = System.currentTimeMillis()

                if (now - lastTapTime.value < 1500 && p != null) {
                    val lat = p.latitude
                    val lng = p.longitude
                    navController.navigate("add_place/$lat/$lng")
                    lastTapTime.value = 0L
                } else {
                    lastTapTime.value = now
                    Toast.makeText(
                        context,
                        "Tik nog een keer om hier een plaats toe te voegen",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
    }

    DisposableEffect(Unit) {
        mapView.overlays.add(mapTapOverlay)
        mapView.onResume()
        onDispose {
            mapView.overlays.remove(mapTapOverlay)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    val filteredPlaces =
        if (selectedCategories.isEmpty()) {
            places
        } else {
            places.filter { it.category in selectedCategories }
        }

    LaunchedEffect(mapView, filteredPlaces) {
        updateOsmdroidMarkers(mapView, filteredPlaces, navController)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(40.dp)
                .background(Color.White, CircleShape)
                .shadow(4.dp, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Terug",
                tint = Color.Black
            )
        }

        FloatingActionButton(
            onClick = { showFilterDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter plaatsen"
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(8.dp),
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Text("Gefilterde plaatsen", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.size(4.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredPlaces.isEmpty()) {
                        item { Text("Geen plaatsen in deze filter.") }
                    } else {
                        items(filteredPlaces) { place ->
                            PlaceListRow(place = place, onClick = { navController.navigate("place_detail/${place.id}") })
                        }
                    }
                }
            }
        }

        if (showFilterDialog) {
            val categories = remember(places) {
                listOf("Alle categorieën") +
                        places.map { it.category }.distinct().sorted()
            }

            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filter op categorie") },
                text = {
                    LazyColumn {
                        items(categories) { category ->
                            val isSelected = if (category == "Alle categorieën") {
                                selectedCategories.isEmpty()
                            } else {
                                selectedCategories.contains(category)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (category == "Alle categorieën") {
                                            // Leeg setje = alles tonen
                                            selectedCategories = emptySet()
                                        } else {
                                            selectedCategories =
                                                if (selectedCategories.contains(category)) {
                                                    selectedCategories - category
                                                } else {
                                                    selectedCategories + category
                                                }
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (category == "Alle categorieën") {
                                            selectedCategories = emptySet()
                                        } else {
                                            selectedCategories =
                                                if (selectedCategories.contains(category)) {
                                                    selectedCategories - category
                                                } else {
                                                    selectedCategories + category
                                                }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(text = category)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showFilterDialog = false }) {
                        Text("Toepassen")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedCategories = emptySet()
                        showFilterDialog = false
                    }) {
                        Text("Reset")
                    }
                }
            )
        }
    }
}

private fun updateOsmdroidMarkers(
    mapView: MapView,
    places: List<SavedPlace>,
    navController: NavController
) {
    mapView.overlays.removeAll { it is Marker }

    places.forEach { place ->
        val marker = Marker(mapView).apply {
            position = GeoPoint(place.latitude, place.longitude)
            title = place.title
            snippet = place.category
            setOnMarkerClickListener { _, _ ->
                navController.navigate("place_detail/${place.id}")
                true
            }
        }
    mapView.overlays.add(marker)
}

    mapView.invalidate()
}

@Composable
private fun PlaceListRow(place: SavedPlace, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Text(place.title, style = MaterialTheme.typography.titleSmall)
        Text(place.category, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

        if (place.rating > 0) {
            Row {
                (1..5).forEach { i ->
                    Icon(
                        imageVector = if (i <= place.rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = if (i <= place.rating) Color(0xFFFFC107) else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (place.comment.isNotBlank()) {
            Text(
                text = place.comment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
