package be.student.cityshare.ui.places

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
import be.student.cityshare.model.SavedPlace
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesListScreen(
    placesViewModel: PlacesViewModel,
    onBack: () -> Unit,
    onPlaceClick: (SavedPlace) -> Unit
) {
    val places by placesViewModel.places.collectAsState()
    val userId = Firebase.auth.currentUser?.uid

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Trips") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                    }
                }
            )
        }
    ) { padding ->
        val myTrips = places.filter { it.userId == userId }

        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (myTrips.isEmpty()) {
                item { Text("Nog geen trips opgeslagen.") }
            } else {
                items(myTrips) {
                    PlaceListItem(place = it, onClick = { onPlaceClick(it) })
                }
            }
        }
    }
}

@Composable
fun PlaceListItem(
    place: SavedPlace,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = place.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = place.category, style = MaterialTheme.typography.bodySmall)

            if (place.rating > 0) {
                Spacer(modifier = Modifier.height(8.dp))
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = place.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
