package be.student.cityshare.ui.places

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import be.student.cityshare.utils.toBitmap
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

            val bitmap = remember(place!!.imageBase64) {
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
                        // Fallback oude foto's
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

                if (place.rating > 0) {
                    Row {
                        (1..5).forEach { i ->
                            Icon(
                                imageVector = if (i <= place.rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Rating",
                                tint = if (i <= place.rating) Color(0xFFFFC107) else Color.Gray
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
            Text("Laden...", modifier = Modifier.padding(padding))
        }
    }
}