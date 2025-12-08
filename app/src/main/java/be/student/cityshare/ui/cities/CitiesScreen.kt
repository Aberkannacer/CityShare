package be.student.cityshare.ui.cities

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel

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
    onCityClick: (City) -> Unit,
    onLogout: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenChat: () -> Unit,
    unreadCount: Int,
    viewModel: CitiesViewModel = viewModel()
) {
    val cities by viewModel.cities.collectAsState()
    val error by viewModel.error.collectAsState()
    var query by remember { mutableStateOf("") }

    val filtered = cities.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) || it.country.contains(query, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Steden") },
                actions = {
                    IconButton(onClick = onOpenMap) {
                        Icon(Icons.Default.Map, contentDescription = "Kaart")
                    }
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge { Text(unreadCount.coerceAtMost(99).toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = onOpenChat) {
                            Icon(Icons.Default.Message, contentDescription = "Chat")
                        }
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh steden")
                    }
                    TextButton(onClick = onLogout) { Text("Logout") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Ontdek steden", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Selecteer een stad of voeg er eentje toe",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onAddCity,
                    modifier = Modifier.shadow(0.dp, CircleShape)
                ) { Text("Nieuwe stad") }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Zoek stad of land") },
                singleLine = true
            )

            if (filtered.isEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Geen steden gevonden.", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Gebruik 'Nieuwe stad' of open de kaart om te verkennen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered) { city ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onCityClick(city) }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(city.name, style = MaterialTheme.typography.titleMedium)
                                val subtitle = listOf(city.country, city.description)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" – ")
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
