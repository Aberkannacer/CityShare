package be.student.cityshare.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.student.cityshare.model.Message
import be.student.cityshare.model.User
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessagingViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val currentUserId = auth.currentUser?.uid

    // Lijst met gebruikers voor het UserListScreen
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    // Map om gebruikers-ID's om te zetten naar namen voor het ChatScreen
    private val _userMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val userMap: StateFlow<Map<String, String>> = _userMap.asStateFlow()

    // Lijst met berichten voor het huidige actieve gesprek
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // Boolean voor de rode stip op de HomeScreen
    private val _hasUnreadMessages = MutableStateFlow(false)
    val hasUnreadMessages: StateFlow<Boolean> = _hasUnreadMessages.asStateFlow()

    private var unreadListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null

    init {
        if (currentUserId != null) {
            fetchAllUsers()
            listenForAllUnreadMessages()
        }
    }

    private fun fetchAllUsers() {
        db.collection("users").get().addOnSuccessListener { result ->
            val userList = result.toObjects(User::class.java)
            _users.value = userList.filter { it.uid != currentUserId }
            _userMap.value = userList.associateBy({ it.uid }, { it.displayName })
        }
    }

    private fun listenForAllUnreadMessages() {
        if (currentUserId == null) return
        unreadListener?.remove()
        unreadListener = db.collectionGroup("messages")
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshot, _ ->
                _hasUnreadMessages.value = !(snapshot?.isEmpty ?: true)
            }
    }

    fun loadMessages(conversationId: String) {
        messagesListener?.remove()
        messagesListener = db.collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                _messages.value = snapshot?.toObjects(Message::class.java) ?: emptyList()
            }
    }

    fun sendMessage(receiverId: String, text: String) {
        if (currentUserId == null || text.isBlank()) return
        val conversationId = listOf(currentUserId, receiverId).sorted().joinToString("_")
        val message = Message(
            senderId = currentUserId,
            receiverId = receiverId,
            text = text,
            isRead = false
        )
        db.collection("conversations").document(conversationId)
            .collection("messages").add(message)
    }

    fun markMessagesAsRead(conversationId: String) {
        if (currentUserId == null) return
        viewModelScope.launch {
            val query = db.collection("conversations").document(conversationId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)

            query.get().addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    doc.reference.update("isRead", true)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        unreadListener?.remove()
        messagesListener?.remove()
    }
}
