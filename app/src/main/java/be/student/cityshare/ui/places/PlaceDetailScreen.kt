package be.student.cityshare.ui.places

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceDetailScreen(
    placeId: String,
    placesViewModel: PlacesViewModel,
    onBack: () -> Unit
) {
    val places by placesViewModel.places.collectAsState()
    val place = places.find { it.id == placeId }

    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }

    // Initialiseer de staat met de waarden van de plaats zodra deze geladen is
    LaunchedEffect(place) {
        place?.let {
            rating = it.rating
            comment = it.comment
        }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (place.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = place.imageUrl,
                        contentDescription = place.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Text(text = place.category, style = MaterialTheme.typography.labelLarge)

                // Interactieve sterrenbeoordeling
                Row {
                    (1..5).forEach { starIndex ->
                        Icon(
                            imageVector = if (starIndex <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Rating",
                            modifier = Modifier.clickable { rating = starIndex },
                            tint = if (starIndex <= rating) Color(0xFFFFC107) else Color.Gray
                        )
                    }
                }

                // Interactief commentaarveld
                TextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Voeg commentaar toe of wijzig het") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        placesViewModel.updatePlaceDetails(place.id, rating, comment)
                        onBack() // Keer terug na het opslaan
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Beoordeling Opslaan")
                }
            }
        } else {
            Text("Laden...", modifier = Modifier.padding(padding))
        }
    }
}
