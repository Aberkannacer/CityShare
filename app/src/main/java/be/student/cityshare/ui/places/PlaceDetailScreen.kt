package be.student.cityshare.ui.places

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import be.student.cityshare.model.SavedPlace
import java.io.File

@Composable
fun PlaceDetailScreen(
    placeId: String,
    onBack: () -> Unit,
    placesViewModel: PlacesViewModel
) {
    val places by placesViewModel.places.collectAsState()
    val place = places.firstOrNull { it.id == placeId }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Button(onClick = onBack) {
            Text("Terug")
        }

        if (place == null) {
            Spacer(Modifier.height(16.dp))
            Text("Locatie niet gevonden.")
            return@Column
        }

        Spacer(Modifier.height(8.dp))

        Text(text = place.title)
        Text(text = "Categorie: ${place.category}")
        Text(text = "Lat: ${place.latitude}")
        Text(text = "Lng: ${place.longitude}")

        Spacer(Modifier.height(16.dp))

        if (place.imageUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(place.imageUrl))
                    .build(),
                contentDescription = "Foto van locatie",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
        } else {
            Text("Geen foto beschikbaar.")
        }
    }
}