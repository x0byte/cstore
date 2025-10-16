package com.example.cstore.data.chat

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class ChatMessage(
    val id: String = "",
    val sender: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val messagesRef = db.collection("messages")

    fun getMessages() = callbackFlow {
        val listener = messagesRef
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(ChatMessage::class.java).orEmpty()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(sender: String, text: String) {
        val newMsg = ChatMessage(
            id = messagesRef.document().id,
            sender = sender,
            text = text,
            timestamp = System.currentTimeMillis()
        )
        messagesRef.document(newMsg.id).set(newMsg).await()
    }
}
