package be.student.cityshare.ui.messaging

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    receiverId: String,
    receiverEmail: String,
    onBack: () -> Unit,
    messagingViewModel: MessagingViewModel
) {
    val senderId = Firebase.auth.currentUser?.uid ?: return
    val messages by messagingViewModel.messages.collectAsState()
    val userMap by messagingViewModel.userMap.collectAsState()

    var messageText by remember { mutableStateOf("") }

    val conversationId = listOf(senderId, receiverId).sorted().joinToString("_")

    LaunchedEffect(conversationId) {
        messagingViewModel.loadMessages(conversationId)
        messagingViewModel.markMessagesAsRead(conversationId)
    }

    val receiverName = userMap[receiverId] ?: receiverEmail

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(receiverName) },
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
        ) {
            LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                items(messages) {
                    val senderName = userMap[it.senderId] ?: "Onbekend"
                    Text(
                        text = "$senderName: ${it.text}",
                        fontWeight = if (it.senderId == senderId) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Typ een bericht") }
                )
                IconButton(onClick = {
                    if (messageText.isNotBlank()) {
                        messagingViewModel.sendMessage(receiverId, messageText)
                        messageText = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Verstuur")
                }
            }
        }
    }
}
