package be.student.cityshare.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.location.Geocoder
import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.DisposableEffect
import be.student.cityshare.model.TripReview
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import be.student.cityshare.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    tripsViewModel: TripsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val trips by tripsViewModel.trips.collectAsState()
    val userMap by tripsViewModel.userMap.collectAsState()
    val trip = trips.find { it.id == tripId }
    val reviewsByTrip by tripsViewModel.reviews.collectAsState()
    val reviews = reviewsByTrip[tripId].orEmpty()
    val userId = Firebase.auth.currentUser?.uid

    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var distanceKm by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(trip, reviews) {
        trip?.let {
            distanceKm = computeDistance(context, it)
        }
        if (userId != null) {
            val myReview = reviews.firstOrNull { it.userId == userId }
            if (myReview != null) {
                rating = myReview.rating
                comment = myReview.comment
            } else {
                rating = 0
                comment = ""
            }
        }
    }

    LaunchedEffect(tripId) {
        tripsViewModel.listenToReviews(tripId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(trip?.cityName ?: "Trip") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        if (trip == null) {
            Text("Trip niet gevonden", modifier = Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val ownerName = userMap[trip.userId]
                ?: if (trip.userId == Firebase.auth.currentUser?.uid) {
                    Firebase.auth.currentUser?.displayName
                        ?: Firebase.auth.currentUser?.email
                        ?: "Onbekende gebruiker"
                } else "Onbekende gebruiker"

            Text(
                text = "Door: $ownerName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (trip.latitude != 0.0 || trip.longitude != 0.0) {
                TripMiniMap(
                    latitude = trip.latitude,
                    longitude = trip.longitude
                )
            }

            Text(
                text = trip.category,
                style = MaterialTheme.typography.titleMedium
            )

            if (trip.address.isNotBlank()) {
                Text(
                    text = trip.address,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            distanceKm?.let { km ->
                Text(
                    text = "Afstand vanaf Ellermanstraat 33: $km km",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (trip.notes.isNotBlank()) {
                Text(
                    text = trip.notes,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!trip.imageBase64.isNullOrBlank()) {
                Text("Foto", style = MaterialTheme.typography.titleSmall)
                AsyncImage(
                    model = android.util.Base64.decode(trip.imageBase64, android.util.Base64.DEFAULT),
                    contentDescription = "Trip foto",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = "Geef een rating",
                style = MaterialTheme.typography.titleSmall
            )

            Row {
                (1..5).forEach { i ->
                    Icon(
                        imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Ster $i",
                        tint = if (i <= rating) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { rating = i }
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

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    tripsViewModel.addReview(trip.id, rating, comment)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Opslaan")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Reviews", style = MaterialTheme.typography.titleMedium)

            if (reviews.isEmpty()) {
                Text("Nog geen reviews.")
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reviews.forEach { review ->
                        ReviewItem(
                            review = review,
                            fallbackName = userMap[review.userId] ?: "Onbekend"
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ReviewItem(review: TripReview, fallbackName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val name = review.userName.ifBlank { fallbackName }
        Text(text = name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row {
            (1..5).forEach { i ->
                Icon(
                    imageVector = if (i <= review.rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = if (i <= review.rating) Color(0xFFFFC107) else Color.Gray,
                    modifier = Modifier.padding(end = 2.dp)
                )
            }
        }
        if (review.comment.isNotBlank()) {
            Text(text = review.comment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@Composable
private fun TripMiniMap(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
        .clip(RoundedCornerShape(16.dp))
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            controller.setZoom(13.0)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            val point = GeoPoint(latitude, longitude)
            map.controller.setCenter(point)

            map.overlays.clear()
            val marker = Marker(map).apply {
                position = point
                icon = map.context.getDrawable(R.drawable.ic_location_pin)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            }
            map.overlays.add(marker)
            map.invalidate()
        }
    )
}

private suspend fun computeDistance(context: android.content.Context, trip: be.student.cityshare.model.Trip): String? {
    val startLat = 51.2303
    val startLng = 4.4092
    var targetLat = trip.latitude
    var targetLng = trip.longitude

    if (targetLat == 0.0 && targetLng == 0.0) {
        val geocoder = Geocoder(context, java.util.Locale.getDefault())
        val addressToGeocode = when {
            trip.address.isNotBlank() -> trip.address
            trip.cityName.isNotBlank() -> trip.cityName
            else -> null
        }
        if (addressToGeocode != null) {
            try {
                val res = withContext(Dispatchers.IO) { geocoder.getFromLocationName(addressToGeocode, 1) }
                val loc = res?.firstOrNull()
                if (loc != null) {
                    targetLat = loc.latitude
                    targetLng = loc.longitude
                }
            } catch (_: Exception) {
                // ignore and fall through to unknown distance
            }
        }
    }

    if (targetLat == 0.0 && targetLng == 0.0) return null

    val results = FloatArray(1)
    Location.distanceBetween(startLat, startLng, targetLat, targetLng, results)
    val km = results.firstOrNull()?.div(1000) ?: return null
    return String.format(java.util.Locale.getDefault(), "%.1f", km)
}
