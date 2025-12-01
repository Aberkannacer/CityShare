package be.student.cityshare.ui.places

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.compose.foundation.Image
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
import coil.compose.AsyncImage
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

    var distanceMeters by remember { mutableStateOf<Double?>(null) }
    var distanceError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(place) {
        distanceMeters = null
        distanceError = null

        if (place == null) return@LaunchedEffect

        val hasFine =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        val hasCoarse =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            distanceError = "Locatietoestemming niet gegeven"
            return@LaunchedEffect
        }

        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager

        val userLocation: Location? =
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (userLocation == null) {
            distanceError = "Kan je huidige locatie niet bepalen"
            return@LaunchedEffect
        }

        val results = FloatArray(1)
        Location.distanceBetween(
            userLocation.latitude,
            userLocation.longitude,
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        if (place != null) {

            val bitmap = remember(place.imageBase64) {
                place.imageBase64?.toBitmap()
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                when {
                    bitmap != null -> {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = place.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    place.imageUrl.isNotBlank() -> {
                        AsyncImage(
                            model = place.imageUrl,
                            contentDescription = place.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Text(text = place.category, style = MaterialTheme.typography.labelLarge)

                if (!place.address.isNullOrBlank()) {
                    Text(
                        text = "Adres",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = place.address!!,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "Afstand tot jouw locatie",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )

                when {
                    distanceError != null -> {
                        Text(
                            text = distanceError ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Red
                        )
                    }

                    distanceMeters == null -> {
                        Text(
                            text = "Afstand wordt berekend...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    else -> {
                        val km = distanceMeters!! / 1000.0
                        val kmText = String.format(Locale.getDefault(), "%.1f km", km)
                        Text(
                            text = kmText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (place.rating > 0) {
                    Row {
                        (1..5).forEach { starIndex ->
                            Icon(
                                imageVector = if (starIndex <= place.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Rating",
                                tint = if (starIndex <= place.rating) Color(0xFFFFC107) else Color.Gray
                            )
                        }
                    }
                }

                if (place.comment.isNotBlank()) {
                    Text(
                        text = place.comment,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            Text(
                "Laden...",
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
            )
        }
    }
}