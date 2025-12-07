package be.student.cityshare.ui.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import be.student.cityshare.model.Trip
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsListScreen(
    tripsViewModel: TripsViewModel,
    onBack: () -> Unit
) {
    val trips by tripsViewModel.trips.collectAsState()
    val userId = Firebase.auth.currentUser?.uid
    val myTrips = trips.filter { it.userId == userId }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Mijn trips") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (myTrips.isEmpty()) {
                item { Text("Nog geen trips opgeslagen.") }
            } else {
                items(myTrips) { trip ->
                    TripListItem(trip = trip)
                }
            }
        }
    }
}

@Composable
private fun TripListItem(trip: Trip) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = trip.cityName.ifBlank { "Onbekende stad" }, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = trip.category, style = MaterialTheme.typography.bodySmall)

            if (trip.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(trip.address, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (trip.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(trip.notes, style = MaterialTheme.typography.bodyMedium)
            }

            if (trip.rating > 0 || trip.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    (1..5).forEach { starIndex ->
                        Icon(
                            imageVector = if (starIndex <= trip.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Rating",
                            tint = if (starIndex <= trip.rating) Color(0xFFFFC107) else Color.Gray
                        )
                    }
                }
                if (trip.comment.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = trip.comment,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
