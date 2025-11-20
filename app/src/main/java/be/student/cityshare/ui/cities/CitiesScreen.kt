package be.student.cityshare.ui.cities

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestore

data class City(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val description: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitiesScreen(
    onAddCity: () -> Unit,
    onBack: () -> Unit
) {
    var cities by remember { mutableStateOf(listOf<City>()) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val registration = FirebaseFirestore.getInstance().collection("cities")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap: QuerySnapshot?, e: FirebaseFirestoreException? ->
                if (e != null) {
                    error = e.message
                    return@addSnapshotListener
                }
                val items = snap?.documents?.map { doc ->
                    City(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        country = doc.getString("country") ?: "",
                        description = doc.getString("description") ?: ""
                    )
                } ?: emptyList()
                cities = items
            }

        onDispose { registration.remove() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Steden") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                FloatingActionButton(onClick = onAddCity) {
                    Icon(Icons.Default.Add, contentDescription = "Toevoegen")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Start
    )
    { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            if (cities.isEmpty()) {
                Text("Nog geen steden. Voeg er eentje toe!")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cities) { city ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(city.name, style = MaterialTheme.typography.titleMedium)
                                val subtitle = listOf(city.country, city.description)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" — ")
                                if (subtitle.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
