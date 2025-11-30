package be.student.cityshare.ui.places

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import be.student.cityshare.model.SavedPlace
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityDetailScreen(
    cityId: String,
    cityName: String,
    onBack: () -> Unit
) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val userId = auth.currentUser?.uid

    var places by remember { mutableStateOf<List<SavedPlace>>(emptyList()) }

    LaunchedEffect(cityId, userId) {
        if (userId == null) return@LaunchedEffect

        db.collection("places")
            .whereEqualTo("userId", userId)
            .whereEqualTo("cityId", cityId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    places = emptyList()
                    return@addSnapshotListener
                }
                places = snap?.toObjects(SavedPlace::class.java) ?: emptyList()
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
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (places.isEmpty()) {
                item {
                    Text("Nog geen plaatsen in deze stad.")
                }
            } else {
                items(places) { place ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(place.title, style = MaterialTheme.typography.titleMedium)
                            if (place.category.isNotBlank()) {
                                Text(place.category, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}