package com.example.cstore.data.chat

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val senderEmail: String = "",
    val receiverEmail: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSummary(
    val participants: List<String> = emptyList(),
    val participantEmails: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastTimestamp: Long = 0
)
class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun getConversationId(userA: String, userB: String): String {
        return if (userA < userB) "${userA}_${userB}" else "${userB}_${userA}"
    }

    fun getChatList(currentUserId: String) = callbackFlow {
        val listener = db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot: QuerySnapshot?, error: FirebaseFirestoreException? ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val chats = snapshot?.toObjects(ChatSummary::class.java).orEmpty()
                trySend(chats)
            }

        awaitClose { listener.remove() }
    }

    fun getMessagesForConversation(userA: String, userB: String) = callbackFlow {
        val conversationId = getConversationId(userA, userB)
        val messagesRef = db.collection("chats")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val listener = messagesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val messages = snapshot?.toObjects(ChatMessage::class.java).orEmpty()
            trySend(messages)
        }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(senderId: String, senderEmail: String,
                            receiverId: String, receiverEmail: String,
                            text: String) {
        val conversationId = getConversationId(senderId, receiverId)
        val chatRef = db.collection("chats").document(conversationId)
        val messagesRef = chatRef.collection("messages")

        val newMsg = ChatMessage(
            id = messagesRef.document().id,
            senderId = senderId,
            receiverId = receiverId,
            senderEmail = senderEmail,
            receiverEmail = receiverEmail,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        messagesRef.document(newMsg.id).set(newMsg).await()
        // Update chat summary
        val summary = mapOf(
            "participants" to listOf(senderId, receiverId),
            "participantEmails" to listOf(senderEmail, receiverEmail),
            "lastMessage" to text,
            "lastTimestamp" to newMsg.timestamp
        )
        chatRef.set(summary, com.google.firebase.firestore.SetOptions.merge()).await()
    }
}
