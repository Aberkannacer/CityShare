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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onBack: () -> Unit,
    onPlaceClick: (SavedPlace) -> Unit
) {
    val auth = Firebase.auth
    val db = Firebase.firestore
    val userId = auth.currentUser?.uid

    var places by remember { mutableStateOf<List<SavedPlace>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(cityId, userId) {
        if (userId == null) {
            places = emptyList()
            isLoading = false
            return@LaunchedEffect
        }

        db.collection("places")
            .whereEqualTo("userId", userId)
            .whereEqualTo("cityId", cityId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    places = emptyList()
                    isLoading = false
                    return@addSnapshotListener
                }
                places = snap?.toObjects(SavedPlace::class.java) ?: emptyList()
                isLoading = false
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
        if (isLoading) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (places.isEmpty()) {
                    item {
                        Text(
                            text = "Geen bezienswaardigheden in deze stad.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    items(places) { place ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onPlaceClick(place) }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    place.title,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (place.category.isNotBlank()) {
                                    Text(
                                        place.category,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (!place.address.isNullOrBlank()) {
                                    Text(
                                        place.address!!,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}