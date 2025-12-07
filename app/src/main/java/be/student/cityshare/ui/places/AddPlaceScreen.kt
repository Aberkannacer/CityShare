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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import be.student.cityshare.utils.getAddressFromLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPlaceScreen(
    lat: Double,
    lng: Double,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    placesViewModel: PlacesViewModel
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var address by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val categories by placesViewModel.categories.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        loading = true
        address = withContext(Dispatchers.IO) {
            getAddressFromLocation(context, lat, lng) ?: "Ellermanstraat 33, 2060 Antwerpen"
        }
        loading = false
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { picked ->
        imageUri = picked
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Box(Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Terug")
            }
            Text("Nieuwe locatie opslaan", modifier = Modifier.align(Alignment.Center))
        }

        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text("Adres", color = Color.Gray)
            when {
                loading -> Text("Adres laden…")
                address != null -> Text(address!!)
                else -> Text("Geen adres gevonden", color = Color.Red)
            }

            TextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titel of beschrijving") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Categorie")
            if (categories.isEmpty()) {
                Text("Geen categorieën in Firebase", color = Color.Red)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        Text(
                            text = cat,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (selectedCategory == cat)
                                        Color.LightGray
                                    else Color.Transparent
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Button(onClick = { pickImage.launch("image/*") }) {
                Text("Kies foto")
            }

            imageUri?.let { uri ->
                AsyncImage(
                    model = ImageRequest.Builder(context).data(uri).build(),
                    contentDescription = "Foto",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Annuleer")
            }

            Button(
                enabled = title.isNotBlank() && selectedCategory != null,
                onClick = {
                    placesViewModel.savePlace(
                        context = context,
                        title = title,
                        lat = lat,
                        lng = lng,
                        category = selectedCategory!!,
                        imageUri = imageUri
                    )
                    onSaved()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Opslaan")
            }
        }
    }
}
