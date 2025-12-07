package be.student.cityshare.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToPlaces: () -> Unit,
    onNavigateToCities: () -> Unit,
    onNavigateToAddTrip: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToMessaging: () -> Unit,
    hasUnreadMessages: Boolean
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("CityShare") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HomeCard(
                title = "Wereldkaart",
                icon = Icons.Default.Map,
                onClick = onNavigateToMap
            )
            HomeCard(
                title = "Trip toevoegen",
                icon = Icons.Default.FlightTakeoff,
                onClick = onNavigateToAddTrip
            )
            HomeCard(
                title = "Mijn Plaatsen",
                icon = Icons.AutoMirrored.Filled.List,
                onClick = onNavigateToPlaces
            )
            HomeCard(
                title = "Mijn Steden",
                icon = Icons.Default.LocationCity,
                onClick = onNavigateToCities
            )
            HomeCard(
                title = "Berichten",
                icon = Icons.AutoMirrored.Filled.Message,
                onClick = onNavigateToMessaging,
                showBadge = hasUnreadMessages
            )
            HomeCard(
                title = "Profiel",
                icon = Icons.Default.Person,
                onClick = onNavigateToProfile
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    showBadge: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, contentDescription = title, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (showBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(16.dp)
                        .background(Color.Red, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                )
            }
        }
    }
}
