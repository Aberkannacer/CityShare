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
import be.student.cityshare.model.TripReview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: String,
    tripsViewModel: TripsViewModel,
    onBack: () -> Unit
) {
    val trips by tripsViewModel.trips.collectAsState()
    val userMap by tripsViewModel.userMap.collectAsState()
    val trip = trips.find { it.id == tripId }
    val reviewsByTrip by tripsViewModel.reviews.collectAsState()
    val reviews = reviewsByTrip[tripId].orEmpty()

    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }

    LaunchedEffect(trip) {
        trip?.let {
            rating = it.rating
            comment = it.comment
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
            val ownerName = userMap[trip.userId] ?: "Onbekende gebruiker"
            Text(text = "Door: $ownerName", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = trip.category, style = MaterialTheme.typography.titleMedium)

            if (trip.address.isNotBlank()) {
                Text(text = trip.address, style = MaterialTheme.typography.bodyMedium)
            }
            if (trip.notes.isNotBlank()) {
                Text(text = trip.notes, style = MaterialTheme.typography.bodyMedium)
            }

            Text(text = "Geef een rating", style = MaterialTheme.typography.titleSmall)
            Row {
                (1..5).forEach { i ->
                    Icon(
                        imageVector = if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "Ster $i",
                        tint = if (i <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
