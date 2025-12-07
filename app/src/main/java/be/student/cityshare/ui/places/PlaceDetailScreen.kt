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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import be.student.cityshare.model.PlaceReview
import be.student.cityshare.utils.toBitmap
import coil.compose.AsyncImage
import java.util.Locale
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    placeId: String,
    placesViewModel: PlacesViewModel,
    onBack: () -> Unit
) {

    val places by placesViewModel.places.collectAsState()
    val place = places.find { it.id == placeId }
    val reviewsByPlace by placesViewModel.placeReviews.collectAsState()
    val reviews = reviewsByPlace[placeId].orEmpty()
    val currentUserId = Firebase.auth.currentUser?.uid
    val userMap by placesViewModel.userMap.collectAsState()

    val context = LocalContext.current

    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var distanceMeters by remember { mutableStateOf<Double?>(null) }
    var distanceError by remember { mutableStateOf<String?>(null) }
    // Vast startpunt: Ellermanstraat 33, 2060 Antwerpen
    val defaultLocation = remember {
        Location("default").apply {
            latitude = 51.2303
            longitude = 4.4092
        }
    }

    LaunchedEffect(place) {
        place?.let {
            rating = it.rating
            comment = it.comment
        }
    }

    LaunchedEffect(placeId) {
        placesViewModel.listenToPlaceReviews(placeId)
    }

    LaunchedEffect(reviews) {
        val mine = reviews.firstOrNull { it.userId == currentUserId }
        if (mine != null) {
            rating = mine.rating
            comment = mine.comment
        }
    }

    LaunchedEffect(place) {
        if (place == null) return@LaunchedEffect

        // Startlocatie altijd Ellermanstraat
        val userLoc = defaultLocation

        val targetLatLng = withContext(Dispatchers.IO) {
            when {
                place.latitude != 0.0 || place.longitude != 0.0 -> place.latitude to place.longitude
                !place.address.isNullOrBlank() -> {
                    try {
                        val geo = android.location.Geocoder(context, Locale.getDefault())
                        val res = geo.getFromLocationName(place.address, 1)
                        val loc = res?.firstOrNull()
                        (loc?.latitude ?: 0.0) to (loc?.longitude ?: 0.0)
                    } catch (_: Exception) {
                        0.0 to 0.0
                    }
                }
                else -> 0.0 to 0.0
            }
        }

        if (targetLatLng.first == 0.0 && targetLatLng.second == 0.0) {
            distanceError = "Geen coördinaten beschikbaar"
            distanceMeters = null
            return@LaunchedEffect
        }

        val result = FloatArray(1)
        Location.distanceBetween(
            userLoc.latitude,
            userLoc.longitude,
            targetLatLng.first,
            targetLatLng.second,
            result
        )

        distanceMeters = result[0].toDouble()
        distanceError = null
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(place?.title ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
            Modifier.padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            val bmp = place.imageBase64?.toBitmap()
            when {
                bmp != null -> Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "",
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    contentScale = ContentScale.Crop
                )
                place.imageUrl.isNotBlank() -> AsyncImage(
                    model = place.imageUrl,
                    contentDescription = "",
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Text(place.category, style = MaterialTheme.typography.titleMedium)

            place.address?.let {
                Text("Adres", color = Color.Gray)
                Text(it)
            }

            Text("Afstand", color = Color.Gray)
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
                    placesViewModel.addPlaceReview(place.id, rating, comment)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Opslaan")
            }

            Divider()
            Text("Reviews", style = MaterialTheme.typography.titleMedium)
            if (reviews.isEmpty()) {
                Text("Nog geen reviews.")
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reviews.forEach { review ->
                        ReviewRow(
                            name = review.userName.ifBlank { userMap[review.userId] ?: "Onbekende gebruiker" },
                            rating = review.rating,
                            comment = review.comment
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(name: String, rating: Int, comment: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row {
            (1..5).forEach { i ->
                Icon(
                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (i <= rating) Color(0xFFFFC107) else Color.Gray
                )
            }
        }
        if (comment.isNotBlank()) {
            Text(comment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
