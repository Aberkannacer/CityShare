package be.student.cityshare.ui.places

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun AddPlaceScreen(
    lat: Double,
    lng: Double,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    placesViewModel: PlacesViewModel
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Anders") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Terug")
            }
            Text("Nieuwe locatie opslaan", modifier = Modifier.align(Alignment.Center))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Lat: $lat")
            Text(text = "Lng: $lng")

            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titel of beschrijving") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(text = "Categorie")

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val categories = listOf("Eten", "Winkel", "Bezienswaardigheid", "Werk", "Anders")
                categories.forEach { cat ->
                    Text(
                        text = cat,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (category == cat) Color.LightGray else Color.Transparent
                            )
                            .clickable { category = cat }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Button(
                onClick = { pickImageLauncher.launch("image/*") }
            ) {
                Text(text = "Kies foto")
            }

            imageUri?.let { uri ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
                        .build(),
                    contentDescription = "Gekozen foto",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Annuleer")
            }
            Button(
                onClick = {
                    placesViewModel.savePlace(
                        context = context,
                        title = title,
                        lat = lat,
                        lng = lng,
                        category = category,
                        imageUri = imageUri
                    )
                    onSaved()
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Opslaan")
            }
        }
    }
}
