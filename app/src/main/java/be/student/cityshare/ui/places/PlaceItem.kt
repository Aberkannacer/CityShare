package be.student.cityshare.ui.places

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import be.student.cityshare.model.SavedPlace
import java.io.File

@Composable
fun PlaceItem(place: SavedPlace) {
    val context = LocalContext.current

    Row(modifier = Modifier.padding(8.dp)) {

        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(place.imageUrl))
                .build(),
            contentDescription = "Foto",
            modifier = Modifier.size(80.dp)
        )

        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(place.title)
            Text(place.category)
        }
    }
}