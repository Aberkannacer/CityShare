package be.student.cityshare.ui.trips

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.location.Geocoder
import android.widget.Toast
import java.util.Locale
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import be.student.cityshare.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTripScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    tripsViewModel: TripsViewModel,
    prefillCity: String? = null,
    prefillAddress: String? = null
){
    val cities by tripsViewModel.cities.collectAsState()
    val categories by tripsViewModel.categories.collectAsState()
    val saving by tripsViewModel.saving.collectAsState()

    var selectedCity by remember { mutableStateOf<CityOption?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var address by remember { mutableStateOf(prefillAddress ?: "Ellermanstraat 33, 2060 Antwerpen") }
    var manualCityName by remember { mutableStateOf(prefillCity?.takeIf { it.isNotBlank() }) }
    var notes by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var showMapPicker by remember { mutableStateOf(false) }
    var selectedLatLng by remember { mutableStateOf<GeoPoint?>(null) }

    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imageUri = uri }

    LaunchedEffect(cities) {
        if (prefillCity != null) {
            val match = cities.firstOrNull { it.name.equals(prefillCity, ignoreCase = true) }
            if (match != null) {
                selectedCity = match
                manualCityName = match.name
            } else if (manualCityName.isNullOrBlank()) {
                manualCityName = prefillCity
            }
        }
        if (selectedCity == null && cities.isNotEmpty()) {
            selectedCity = cities.first()
            if (manualCityName.isNullOrBlank()) {
                manualCityName = selectedCity?.name
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Trip toevoegen") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Trip details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Stad: ${manualCityName ?: selectedCity?.name ?: "Onbekend"}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                    ExposedDropdown(
                        label = "Kies een categorie",
                        options = categories,
                        expanded = categoryMenuExpanded,
                        selectedLabel = selectedCategory ?: "Selecteer",
                        onExpandChange = { categoryMenuExpanded = it },
                        onSelect = { index ->
                            categoryMenuExpanded = false
                            selectedCategory = categories.getOrNull(index)
                        },
                        enabled = categories.isNotEmpty()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Locatie of adres") },
                        placeholder = { Text("Bijv. Amphitheatre Parkway, Mountain View") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            imeAction = ImeAction.Next
                        )
                    )
                    OutlinedButton(
                        onClick = { showMapPicker = true },
                        enabled = !saving,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kies op kaart")
                    }
                    selectedLatLng?.let { point ->
                        Text(
                            text = "Geselecteerde locatie: %.4f, %.4f".format(point.latitude, point.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Omschrijving (optioneel)") },
                        minLines = 3
                    )

                    Text(
                        text = "Rating",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { i ->
                            Icon(
                                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Ster $i",
                                tint = if (i <= rating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { rating = i }
                            )
                        }
                    }

                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Comment") },
                            minLines = 2
                        )
                    }
                }

                Surface(
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Herinnering",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Button(
                            onClick = { pickImage.launch("image/*") },
                            enabled = !saving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Neem of kies een foto")
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (imageUri != null) {
                                    AsyncImage(
                                        model = imageUri,
                                        contentDescription = "Geselecteerde foto",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Image(
                                            imageVector = Icons.Default.FlightTakeoff,
                                            contentDescription = null,
                                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Nog geen afbeelding gekozen",
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Button(
                    onClick = {
                        error = null
                        if (address.isBlank()) {
                            error = "Adres is verplicht"
                            return@Button
                        }
                        tripsViewModel.saveTrip(
                            context = context,
                            city = selectedCity,
                            category = selectedCategory,
                            address = address,
                            notes = notes,
                            rating = rating,
                            comment = comment,
                            imageUri = imageUri,
                            cityNameOverride = manualCityName,
                            latitude = selectedLatLng?.latitude,
                            longitude = selectedLatLng?.longitude,
                            onSuccess = onSaved,
                            onError = { error = it }
                        )
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (saving) "Bezig..." else "Trip toevoegen")
                }
            }
        }

        if (showMapPicker) {
            TripMapPickerOverlay(
                centerPoint = selectedLatLng,
                centerQuery = manualCityName ?: selectedCity?.name ?: address,
                onDismiss = { showMapPicker = false },
                onPointSelected = { point, resolvedCity, resolvedAddress ->
                    selectedLatLng = point
                    if (resolvedAddress.isNotBlank()) {
                        address = resolvedAddress
                    }
                    if (resolvedCity.isNotBlank()) {
                        manualCityName = resolvedCity
                        val match = cities.firstOrNull { it.name.equals(resolvedCity, ignoreCase = true) }
                        if (match != null) selectedCity = match
                    }
                    showMapPicker = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdown(
    label: String,
    options: List<String>,
    expanded: Boolean,
    selectedLabel: String,
    onExpandChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit,
    enabled: Boolean
) {
    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandChange
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface
            ),
            trailingIcon = {
                androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            }
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) }
        ) {
            options.forEachIndexed { index, option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun TripMapPickerOverlay(
    centerPoint: GeoPoint? = null,
    centerQuery: String? = null,
    onDismiss: () -> Unit,
    onPointSelected: (GeoPoint, String, String) -> Unit
) {
    val context = LocalContext.current
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val lastTapTime = remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        onDispose { }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(51.2194, 4.4025))
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            addOnFirstLayoutListener { _, _, _, _, _ ->
                loading = false
            }
        }
    }

    LaunchedEffect(centerPoint, centerQuery) {
        when {
            centerPoint != null -> {
                mapView.controller.setCenter(centerPoint)
                mapView.controller.setZoom(14.0)
            }
            !centerQuery.isNullOrBlank() -> {
                val geo = Geocoder(context, Locale.getDefault())
                val res = withContext(Dispatchers.IO) {
                    try {
                        geo.getFromLocationName(centerQuery, 1)
                    } catch (_: Exception) {
                        null
                    }
                }
                val loc = res?.firstOrNull()
                if (loc != null) {
                    val point = GeoPoint(loc.latitude, loc.longitude)
                    mapView.controller.setCenter(point)
                    mapView.controller.setZoom(14.0)
                }
            }
        }
    }

    val mapEvents = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                if (p == null) return true
                val now = System.currentTimeMillis()
                if (now - lastTapTime.value < 1200) {
                    loading = true
                    scope.launch(Dispatchers.IO) {
                        val geo = Geocoder(context, Locale.getDefault())
                        val res = try {
                            geo.getFromLocation(p.latitude, p.longitude, 1)
                        } catch (_: Exception) {
                            null
                        }
                        val cityName = res?.firstOrNull()?.locality
                            ?: res?.firstOrNull()?.subAdminArea
                            ?: ""
                        val address = res?.firstOrNull()?.getAddressLine(0) ?: ""

                        withContext(Dispatchers.Main) {
                            loading = false
                            error = null
                        mapView.overlays.removeAll { it is Marker }
                        val marker = Marker(mapView).apply {
                            position = p
                            title = if (cityName.isNotBlank()) cityName else "Locatie"
                            icon = context.getDrawable(R.drawable.ic_location_pin)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                            mapView.overlays.add(marker)
                            mapView.invalidate()
                            onPointSelected(p, cityName, address)
                            lastTapTime.value = 0L
                        }
                    }
                } else {
                    lastTapTime.value = now
                    Toast.makeText(context, "Tik nog een keer om deze locatie te kiezen", Toast.LENGTH_SHORT).show()
                }
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        })
    }

    DisposableEffect(Unit) {
        mapView.overlays.add(mapEvents)
        mapView.onResume()
        onDispose {
            mapView.overlays.remove(mapEvents)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(420.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { mapView }
                )
                if (loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x66000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Tik op de kaart om een locatie te kiezen", color = Color.White)
                    if (error != null) {
                        Text(error ?: "", color = MaterialTheme.colorScheme.error)
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Sluiten",
                        tint = Color.Black
                    )
                }
            }
        }
    }
}
