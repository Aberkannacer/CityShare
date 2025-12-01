package be.student.cityshare.ui.places

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import be.student.cityshare.model.SavedPlace
import be.student.cityshare.utils.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun PlaceItem(place: SavedPlace) {
    val context = LocalContext.current

    val bitmap = remember(place.imageBase64) {
        place.imageBase64?.toBitmap()
    }

    Row(modifier = Modifier.padding(8.dp)) {

        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Foto",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                )
            }

            place.imageUrl.isNotBlank() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(File(place.imageUrl))
                        .build(),
                    contentDescription = "Foto",
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Crop
                )
            }

            else -> {
                Box(modifier = Modifier.size(80.dp))
            }
        }

        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(place.title, style = MaterialTheme.typography.titleMedium)
            Text(place.category, style = MaterialTheme.typography.bodySmall)

            if (!place.address.isNullOrBlank()) {
                Text(
                    text = place.address!!,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}