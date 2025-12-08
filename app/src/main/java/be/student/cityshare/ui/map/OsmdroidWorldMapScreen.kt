package be.student.cityshare.ui.map

import android.Manifest
import android.widget.Toast
import android.content.pm.PackageManager
import android.location.Location
import android.location.Geocoder
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import be.student.cityshare.model.TripReview
import be.student.cityshare.ui.places.PlacesViewModel
import be.student.cityshare.ui.trips.TripsViewModel
import com.google.android.gms.location.LocationServices
import java.net.URLEncoder
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import be.student.cityshare.model.Trip
import be.student.cityshare.R
import kotlin.math.roundToInt

@Composable
fun OsmdroidWorldMapScreen(
    navController: NavController,
    placesViewModel: PlacesViewModel,
    tripsViewModel: TripsViewModel
) {
    val context = LocalContext.current
    val trips by tripsViewModel.trips.collectAsState()

    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var cityQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    // Vaste basislocatie: Ellermanstraat 33, 2060 Antwerpen
    val userLocation = remember {
        Location("default").apply {
            latitude = 51.2303
            longitude = 4.4092
        }
    }

    val filteredTrips = trips.filter { trip ->
        val matchesCategory =
            selectedCategories.isEmpty() || trip.category in selectedCategories
        val matchesCity = cityQuery.isBlank() ||
                (trip.cityName.contains(cityQuery, ignoreCase = true)) ||
                (trip.address.contains(cityQuery, ignoreCase = true))
        matchesCategory && matchesCity
    }

    val tripPins by produceState(initialValue = emptyList<TripPin>(), key1 = filteredTrips, key2 = context) {
        value = withContext(Dispatchers.IO) {
            val geo = Geocoder(context, java.util.Locale.getDefault())
            filteredTrips.mapNotNull { trip ->
                val latLng = when {
                    trip.latitude != 0.0 || trip.longitude != 0.0 -> trip.latitude to trip.longitude
                    trip.address.isNotBlank() -> {
                        try {
                            val res = geo.getFromLocationName(trip.address, 1)
                            val loc = res?.firstOrNull()
                            if (loc != null) loc.latitude to loc.longitude else null
                        } catch (_: Exception) {
                            null
                        }
                    }
                    trip.cityName.isNotBlank() -> {
                        try {
                            val res = geo.getFromLocationName(trip.cityName, 1)
                            val loc = res?.firstOrNull()
                            if (loc != null) loc.latitude to loc.longitude else null
                        } catch (_: Exception) {
                            null
                        }
                    }
                    else -> null
                }
                latLng?.let { (lat, lng) ->
                    TripPin(
                        id = trip.id,
                        title = trip.cityName.ifBlank { "Trip" },
                        lat = lat,
                        lng = lng
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)

            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(51.2194, 4.4025)) // Antwerpen

            zoomController.setVisibility(
                CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT
            )
        }
    }

    val mapTapOverlay = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                val distanceText = p?.let { point ->
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        userLocation.latitude,
                        userLocation.longitude,
                        point.latitude,
                        point.longitude,
                        results
                    )
                    val km = results.firstOrNull()?.div(1000) ?: 0f
                    "Afstand vanaf Ellermanstraat 33: ${"%.1f".format(java.util.Locale.getDefault(), km)} km"
                }

                distanceText?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }

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

    LaunchedEffect(mapView, tripPins) {
        updateTripMarkers(mapView, tripPins, navController, userLocation)
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

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 140.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val actions = listOf(
                MapFab(Icons.Default.FilterList, "Filter trips") { showFilterDialog = true },
                MapFab(Icons.Default.Add, "Zoom in") { mapView.controller.zoomIn() },
                MapFab(Icons.Default.Remove, "Zoom uit") { mapView.controller.zoomOut() }
            )
            actions.forEach { item ->
                Surface(
                    tonalElevation = 6.dp,
                    shape = CircleShape,
                    color = Color.White
                ) {
                    IconButton(
                        onClick = item.onClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(imageVector = item.icon, contentDescription = item.description, tint = Color.Black)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 16.dp, end = 16.dp),
            tonalElevation = 6.dp,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = cityQuery,
                    onValueChange = { cityQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Zoek stad of adres") },
                    singleLine = true,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            scope.launch {
                                moveToFirstResult(filteredTrips, mapView, context, cityQuery)
                                keyboardController?.hide()
                            }
                        }
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    )
                )
                Button(
                    onClick = {
                        scope.launch {
                            moveToFirstResult(filteredTrips, mapView, context, cityQuery)
                            keyboardController?.hide()
                        }
                    }
                ) {
                    Text("Ga")
                }
            }
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
                Text("Trips", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.size(4.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredTrips.isEmpty()) {
                        item { Text("Geen trips in deze filter.") }
                    } else {
                        items(filteredTrips) { trip ->
                            TripListRow(trip = trip, onClick = {
                                if (trip.id.isNotBlank()) {
                                    navController.navigate("trip_detail/${trip.id}")
                                }
                            })
                        }
                    }
                }
            }
        }

        if (showFilterDialog) {
            val categories = remember(filteredTrips) {
                listOf("Alle categorieën") +
                        filteredTrips.map { it.category }.distinct().sorted()
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

@Composable
private fun TripListRow(trip: Trip, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Text(trip.cityName.ifBlank { "Trip" }, style = MaterialTheme.typography.titleSmall)
        Text(trip.category, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        if (trip.address.isNotBlank()) {
            Text(trip.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun updateTripMarkers(
    mapView: MapView,
    trips: List<TripPin>,
    navController: NavController,
    userLocation: Location
) {
    mapView.overlays.removeAll { it is Marker && it.snippet == "trip_marker" }

    trips.forEach { trip ->
        val marker = Marker(mapView).apply {
            position = GeoPoint(trip.lat, trip.lng)
            title = trip.title
            snippet = "trip_marker"
            icon = mapView.context.getDrawable(R.drawable.ic_location_pin)
            setOnMarkerClickListener { _, _ ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude,
                    userLocation.longitude,
                    trip.lat,
                    trip.lng,
                    results
                )
                val km = results.firstOrNull()?.div(1000)?.roundToInt() ?: 0
                Toast.makeText(mapView.context, "Afstand: ${km} km", Toast.LENGTH_SHORT).show()
                if (trip.id.isNotBlank()) {
                    navController.navigate("trip_detail/${trip.id}")
                }
                true
            }
        }
        mapView.overlays.add(marker)
    }

    mapView.invalidate()
}

private data class TripPin(
    val id: String,
    val title: String,
    val lat: Double,
    val lng: Double
)

private data class MapFab(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit
)

private suspend fun moveToFirstResult(
    trips: List<Trip>,
    mapView: MapView,
    context: android.content.Context,
    fallbackQuery: String
) {
    val geo = Geocoder(context, java.util.Locale.getDefault())
    var targetLatLng: Pair<Double, Double>? = trips
        .firstOrNull { it.latitude != 0.0 || it.longitude != 0.0 }
        ?.let { it.latitude to it.longitude }

    if (targetLatLng == null) {
        val first = trips.firstOrNull()
        val query = when {
            first?.address?.isNotBlank() == true -> first.address
            first?.cityName?.isNotBlank() == true -> first.cityName
            fallbackQuery.isNotBlank() -> fallbackQuery
            else -> null
        }
        if (query != null) {
            try {
                val res = withContext(Dispatchers.IO) { geo.getFromLocationName(query, 1) }
                val loc = res?.firstOrNull()
                if (loc != null) {
                    targetLatLng = loc.latitude to loc.longitude
                }
            } catch (_: Exception) {
            }
        }
    }

    val (lat, lng) = targetLatLng ?: 51.2194 to 4.4025
    mapView.controller.setZoom(13.0)
    mapView.controller.animateTo(GeoPoint(lat, lng))
}
