package be.student.cityshare.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import be.student.cityshare.model.Message
import be.student.cityshare.model.User
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessagingViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val currentUserId: String?
        get() = auth.currentUser?.uid

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

    // Set van afzender-ids met ongelezen berichten (voor rode badges in lijst)
    private val _unreadFrom = MutableStateFlow<Set<String>>(emptySet())
    val unreadFrom: StateFlow<Set<String>> = _unreadFrom.asStateFlow()

    private var unreadListener: ListenerRegistration? = null
    private var messagesListener: ListenerRegistration? = null
    private var notificationListener: ListenerRegistration? = null

    init {
        fetchAllUsers()
        listenForAllUnreadMessages()
        auth.addAuthStateListener {
            resetState()
            fetchAllUsers()
            listenForAllUnreadMessages()
        }
    }

    private fun resetState() {
        unreadListener?.remove()
        messagesListener?.remove()
        notificationListener?.remove()
        _messages.value = emptyList()
        _unreadFrom.value = emptySet()
        _hasUnreadMessages.value = false
    }

    private fun fetchAllUsers() {
        db.collection("users").get().addOnSuccessListener { result ->
            val allUsers = result.toObjects(User::class.java)
            val myId = currentUserId
            _users.value = if (myId != null) {
                allUsers.filter { it.uid != myId }
            } else {
                allUsers
            }
            _userMap.value = allUsers.associateBy(
                { it.uid },
                { user -> user.displayName.ifBlank { user.email.ifBlank { user.uid } } }
            )
        }
    }

    private fun listenForAllUnreadMessages() {
        if (currentUserId == null) return
        unreadListener?.remove()
        unreadListener = db.collectionGroup("messages")
            .whereEqualTo("receiverId", currentUserId)
            .addSnapshotListener { snapshot, _ ->
                val docs = snapshot?.documents.orEmpty()
                val unreadDocs = docs.filter { doc -> doc.getBoolean("isRead") != true }
                _hasUnreadMessages.value = unreadDocs.isNotEmpty()
                _unreadFrom.value = unreadDocs.mapNotNull { it.getString("senderId") }.toSet()
            }
    }

    fun startMessageNotifications(context: Context) {
        if (currentUserId == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "chat_messages",
                "Chatberichten",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
        notificationListener?.remove()
        notificationListener = db.collectionGroup("messages")
            .whereEqualTo("receiverId", currentUserId)
            .addSnapshotListener { snapshot, _ ->
                val docs = snapshot?.documents.orEmpty()
                docs.filter { it.getBoolean("isRead") != true }
                    .forEach { doc ->
                        val senderId = doc.getString("senderId") ?: return@forEach
                        if (senderId == currentUserId) return@forEach
                        val text = doc.getString("text") ?: ""
                        val senderName = _userMap.value[senderId] ?: "Nieuw bericht"
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        val pendingIntent = launchIntent?.let {
                            PendingIntent.getActivity(
                                context,
                                doc.id.hashCode(),
                                it,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        }
                        val notification = NotificationCompat.Builder(context, "chat_messages")
                            .setSmallIcon(android.R.drawable.ic_dialog_email)
                            .setContentTitle(senderName)
                            .setContentText(text.take(50))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setContentIntent(pendingIntent)
                            .setAutoCancel(true)
                            .build()
                        NotificationManagerCompat.from(context)
                            .notify(doc.id.hashCode(), notification)
                    }
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
        val senderId = currentUserId ?: return
        if (text.isBlank()) return
        val conversationId = listOf(senderId, receiverId).sorted().joinToString("_")
        val message = Message(
            senderId = senderId,
            receiverId = receiverId,
            text = text,
            isRead = false
        )
        db.collection("conversations").document(conversationId)
            .collection("messages").add(message)
    }

    fun ensureFcmToken() {
        val uid = currentUserId ?: return
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (token.isNullOrBlank()) return@addOnSuccessListener
            db.collection("users").document(uid)
                .update("fcmTokens", com.google.firebase.firestore.FieldValue.arrayUnion(token))
                .addOnFailureListener {
                    // fallback: create doc if missing
                    db.collection("users").document(uid).set(
                        mapOf("fcmTokens" to listOf(token)),
                        com.google.firebase.firestore.SetOptions.merge()
                    )
                }
        }
    }

    fun markMessagesAsRead(conversationId: String, otherUserId: String) {
        if (currentUserId == null) return
        viewModelScope.launch {
            val query = db.collection("conversations").document(conversationId)
                .collection("messages")
                .whereEqualTo("receiverId", currentUserId)

            query.get().addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { doc ->
                    doc.reference.update("isRead", true)
                }
                // lokaal badge leegmaken voor deze afzender
                val current = _unreadFrom.value.toMutableSet()
                current.remove(otherUserId)
                _unreadFrom.value = current
                _hasUnreadMessages.value = current.isNotEmpty()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        unreadListener?.remove()
        messagesListener?.remove()
        notificationListener?.remove()
    }
}
