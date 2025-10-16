package com.example.cstore.data.chat

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private fun getConversationId(userA: String, userB: String): String {
        return if (userA < userB) "${userA}_${userB}" else "${userB}_${userA}"
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

    suspend fun sendMessage(senderId: String, receiverId: String, text: String) {
        val conversationId = getConversationId(senderId, receiverId)
        val messagesRef = db.collection("chats")
            .document(conversationId)
            .collection("messages")

        val newMsg = ChatMessage(
            id = messagesRef.document().id,
            senderId = senderId,
            receiverId = receiverId,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        messagesRef.document(newMsg.id).set(newMsg).await()
    }
}
