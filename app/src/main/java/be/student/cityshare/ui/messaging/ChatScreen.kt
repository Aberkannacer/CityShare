package be.student.cityshare.ui.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import be.student.cityshare.model.Message
import java.text.SimpleDateFormat
import java.util.Locale

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
        messagingViewModel.markMessagesAsRead(conversationId, receiverId)
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    MessageBubble(
                        message = msg,
                        isMe = msg.senderId == senderId,
                        senderName = userMap[msg.senderId] ?: "Onbekend"
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
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

@Composable
private fun MessageBubble(message: Message, isMe: Boolean, senderName: String) {
    val bgColor = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val time = remember(message.timestamp) {
        message.timestamp?.let {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(it)
        } ?: ""
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 320.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!isMe) {
                    Text(senderName, style = MaterialTheme.typography.labelSmall, color = textColor)
                }
                Text(message.text, color = textColor)
                if (time.isNotBlank()) {
                    Text(time, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f))
                }
            }
        }
    }
}
