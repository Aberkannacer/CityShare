package be.student.cityshare.ui.messaging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import be.student.cityshare.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    messagingViewModel: MessagingViewModel,
    onUserClick: (User) -> Unit,
    onBack: () -> Unit
) {
    val users by messagingViewModel.users.collectAsState()
    val unreadFrom by messagingViewModel.unreadFrom.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Start een gesprek") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug")
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
            items(users) {
                val hasUnread = unreadFrom.contains(it.uid)
                UserListItem(user = it, hasUnread = hasUnread, onClick = { onUserClick(it) })
            }
        }
    }
}

@Composable
private fun UserListItem(user: User, hasUnread: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp)) {
                val name = user.displayName.ifBlank { user.email.ifBlank { user.uid } }
                Text(text = name)
                if (user.email.isNotBlank()) {
                    Text(text = user.email, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }
            if (hasUnread) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(12.dp)
                        .background(Color.Red, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}
