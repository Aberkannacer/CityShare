package be.student.cityshare.ui.cities

import android.location.Location
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import be.student.cityshare.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import android.location.Geocoder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun AddCityMapScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    var selectedPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var selectedLabel by remember { mutableStateOf<String?>(null) }
    var cityName by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var mapLoading by remember { mutableStateOf(true) }
    // Default locatie: Ellermanstraat 33, 2060 Antwerpen
    val userLocation = remember {
        Location("default").apply {
            latitude = 51.2303
            longitude = 4.4092
        }
    }
    var distanceText by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(11.0)
            controller.setCenter(GeoPoint(51.2194, 4.4025)) // Antwerpen als startpositie
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            addOnFirstLayoutListener { _, _, _, _, _ ->
                mapLoading = false
            }
        }
    }

    val mapEvents = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    selectedPoint = p
                    selectedLabel = null
                    distanceText = null
                    error = null

                    // Bereken afstand direct op basis van vaste (of gevonden) userLocation
                    userLocation?.let { loc ->
                        val km = calculateDistanceKm(
                            loc.latitude,
                            loc.longitude,
                            p.latitude,
                            p.longitude
                        )
                        distanceText = String.format("%.1f km", km)
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        val geo = Geocoder(context, Locale.getDefault())
                        val res = try {
                            geo.getFromLocation(p.latitude, p.longitude, 1)
                        } catch (_: Exception) {
                            null
                        }

                        val geoCityName = res?.firstOrNull()?.locality
                            ?: res?.firstOrNull()?.subAdminArea
                            ?: ""
                        val country = res?.firstOrNull()?.countryName ?: ""

                        val label = listOf(geoCityName, country)
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                        launch(Dispatchers.Main) {
                            val suggestedName = if (geoCityName.isNotBlank()) geoCityName else label
                            selectedLabel = if (label.isNotBlank()) label else "Onbekende locatie"
                            cityName = suggestedName
                            selectedCountry = country
                            mapView.controller.setZoom(11.0)
                            mapView.controller.animateTo(p)
                            mapView.overlays.removeAll { it is Marker }
                            val marker = Marker(mapView).apply {
                                position = p
                                title = selectedLabel
                                icon = context.getDrawable(R.drawable.ic_location_pin)
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            mapView.overlays.add(marker)
                            mapView.invalidate()
                            // afstand is hierboven al gezet; laat staan
                        }
                    }
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
    }

    DisposableEffect(Unit) {
        mapView.overlays.add(mapEvents)
        mapView.onResume()
        onDispose {
            mapView.overlays.remove(mapEvents)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        if (mapLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        IconButton(
            onClick = onBack,
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

        Surface(
            tonalElevation = 4.dp,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tik op de kaart om een stad te kiezen",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = selectedLabel ?: "Nog geen selectie",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = cityName,
                    onValueChange = { cityName = it },
                    enabled = selectedPoint != null,
                    label = { Text("Naam van de stad") },
                    placeholder = { Text("Voorgestelde naam of eigen keuze") },
                    modifier = Modifier.fillMaxWidth()
                )
                distanceText?.let {
                    Text(
                        text = "Afstand tot jou: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        val point = selectedPoint
                        val nameToSave = cityName.trim()
                        if (saving || point == null) return@Button
                        if (nameToSave.isBlank()) {
                            error = "Geef een naam voor de stad."
                            return@Button
                        }

                        saving = true
                        error = null

                        CoroutineScope(Dispatchers.IO).launch {
                            val data = hashMapOf(
                                "name" to nameToSave,
                                "searchName" to nameToSave.lowercase(),
                                "country" to selectedCountry,
                                "description" to "",
                                "latitude" to point.latitude,
                                "longitude" to point.longitude,
                                "createdBy" to Firebase.auth.currentUser?.uid
                            )

                            try {
                                FirebaseFirestore.getInstance()
                                    .collection("cities")
                                    .add(data)
                                    .addOnSuccessListener {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            saving = false
                                            Toast.makeText(context, "Stad opgeslagen", Toast.LENGTH_SHORT).show()
                                            onSaved()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        CoroutineScope(Dispatchers.Main).launch {
                                            saving = false
                                            error = e.message ?: "Opslaan mislukt"
                                        }
                                    }
                            } catch (e: Exception) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    saving = false
                                    error = e.message ?: "Opslaan mislukt"
                                }
                            }
                        }
                    },
                    enabled = selectedPoint != null && !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (saving) "Bezig..." else "Voeg stad toe")
                }
            }
        }
    }
}

private fun calculateDistanceKm(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val r = 6371.0 // aardstraal in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) *
            cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return r * c
}
