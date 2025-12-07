package be.student.cityshare.ui.cities

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import android.location.Geocoder
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
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(12.0)
            controller.setCenter(GeoPoint(51.2194, 4.4025)) // Antwerpen als startpositie
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
        }
    }

    val mapEvents = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    selectedPoint = p
                    selectedLabel = null
                    error = null
                    CoroutineScope(Dispatchers.IO).launch {
                        val geo = Geocoder(context, Locale.getDefault())
                        val res = try {
                            geo.getFromLocation(p.latitude, p.longitude, 1)
                        } catch (_: Exception) {
                            null
                        }

                        val cityName = res?.firstOrNull()?.locality
                            ?: res?.firstOrNull()?.subAdminArea
                            ?: ""
                        val country = res?.firstOrNull()?.countryName ?: ""

                        val label = listOf(cityName, country)
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                        launch(Dispatchers.Main) {
                            selectedLabel = if (label.isNotBlank()) label else "Onbekende locatie"
                            mapView.controller.setZoom(11.0)
                            mapView.controller.animateTo(p)
                            mapView.overlays.removeAll { it is Marker }
                            val marker = Marker(mapView).apply {
                                position = p
                                title = selectedLabel
                            }
                            mapView.overlays.add(marker)
                            mapView.invalidate()
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

                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        val point = selectedPoint
                        if (saving || point == null) return@Button

                        saving = true
                        error = null

                        CoroutineScope(Dispatchers.IO).launch {
                            val geo = Geocoder(context, Locale.getDefault())
                            val res = try {
                                geo.getFromLocation(point.latitude, point.longitude, 1)
                            } catch (_: Exception) {
                                null
                            }

                            val name = res?.firstOrNull()?.locality
                                ?: res?.firstOrNull()?.subAdminArea
                                ?: ""
                            val country = res?.firstOrNull()?.countryName ?: ""

                            if (name.isBlank()) {
                                launch(Dispatchers.Main) {
                                    saving = false
                                    error = "Geen stad gevonden op deze locatie."
                                }
                                return@launch
                            }

                            val data = hashMapOf(
                                "name" to name,
                                "searchName" to name.lowercase(),
                                "country" to country,
                                "description" to "",
                                "latitude" to point.latitude,
                                "longitude" to point.longitude
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
