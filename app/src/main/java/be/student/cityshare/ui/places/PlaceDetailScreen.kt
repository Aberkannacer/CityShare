package be.student.cityshare.ui.places

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import be.student.cityshare.utils.toBitmap
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    placeId: String,
    placesViewModel: PlacesViewModel,
    onBack: () -> Unit
) {
    val places by placesViewModel.places.collectAsState()
    val place = places.find { it.id == placeId }

    val context = LocalContext.current

    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }

    var distanceMeters by remember { mutableStateOf<Double?>(null) }
    var distanceError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(place) {
        place?.let {
            rating = it.rating
            comment = it.comment
        }
    }

    LaunchedEffect(place) {
        if (place == null) return@LaunchedEffect

        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
            distanceError = "Locatietoestemming niet gegeven"
            return@LaunchedEffect
        }

        val lm = context.getSystemService(LocationManager::class.java)
        val userLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (userLoc == null) {
            distanceError = "Kan jouw locatie niet bepalen"
            return@LaunchedEffect
        }

        val results = FloatArray(1)
        Location.distanceBetween(
            userLoc.latitude,
            userLoc.longitude,
            place.latitude,
            place.longitude,
            results
        )

        distanceMeters = results[0].toDouble()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(place?.title ?: "Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->

        if (place == null) {
            Text("Laden...", modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            val bmp = place.imageBase64?.toBitmap()
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = place.title,
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Text(place.category, style = MaterialTheme.typography.titleMedium)

            if (!place.address.isNullOrBlank()) {
                Text("Adres", color = Color.Gray)
                Text(place.address!!)
            }

            Text("Afstand tot jouw locatie", color = Color.Gray)
            when {
                distanceError != null -> Text(distanceError!!, color = Color.Red)
                distanceMeters == null -> Text("Berekenen…")
                else -> {
                    val km = distanceMeters!! / 1000
                    Text(String.format(Locale.getDefault(), "%.1f km", km))
                }
            }

            Row {
                (1..5).forEach { i ->
                    Icon(
                        imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "",
                        modifier = Modifier.clickable { rating = i },
                        tint = if (i <= rating) Color(0xFFFFC107) else Color.Gray
                    )
                }
            }

            TextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Comment") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Button(
                onClick = {
                    placesViewModel.updatePlaceDetails(place.id, rating, comment)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Opslaan")
            }
        }
    }
}