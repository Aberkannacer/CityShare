package be.student.cityshare.ui.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import be.student.cityshare.model.Trip
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObjects
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityDetailScreen(
    cityId: String,
    cityName: String,
    onBack: () -> Unit,
    onTripClick: (String) -> Unit,
    onAddTrip: (String) -> Unit
) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val userId = auth.currentUser?.uid

    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(cityId, userId) {
        if (userId == null) {
            trips = emptyList()
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("trips")
            .whereEqualTo("cityId", cityId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    trips = emptyList()
                    return@addSnapshotListener
                }
                trips = snap?.toObjects<Trip>() ?: emptyList()
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(cityName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Terug"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Trips", style = MaterialTheme.typography.titleLarge)
                        OutlinedButton(onClick = { onAddTrip(cityName) }) {
                            Text("Trip toevoegen")
                        }
                    }
                }
                if (trips.isEmpty()) {
                    item { Text("Geen trips in deze stad.", style = MaterialTheme.typography.bodyMedium) }
                } else {
                    items(trips) { trip ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth(),
                            onClick = { onTripClick(trip.id) }) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(trip.cityName.ifBlank { cityName }, style = MaterialTheme.typography.titleMedium)
                                Text(trip.category, style = MaterialTheme.typography.bodySmall)
                                if (trip.address.isNotBlank()) {
                                    Text(trip.address, style = MaterialTheme.typography.bodySmall)
                                }
                                if (trip.notes.isNotBlank()) {
                                    Text(trip.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (trip.rating > 0 || trip.comment.isNotBlank()) {
                                    Row {
                                        (1..5).forEach { i ->
                                            Icon(
                                                imageVector = if (i <= trip.rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                                contentDescription = null,
                                                tint = if (i <= trip.rating) Color(0xFFFFC107) else Color.Gray
                                            )
                                        }
                                    }
                                    if (trip.comment.isNotBlank()) {
                                        Text(trip.comment, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
