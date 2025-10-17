package com.example.cstore.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cstore.data.chat.ChatRepository
import com.example.cstore.data.chat.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private var currentUserId: String? = null
    private var currentUserEmail: String? = null
    private var otherUserId: String? = null
    private var otherUserEmail: String? = null

    fun loadConversation(currentId: String,
                         currentEmail: String,
                         otherId: String,
                         otherEmail: String) {
        currentUserId = currentId
        currentUserEmail = currentEmail
        otherUserId = otherId
        otherUserEmail = otherEmail

        viewModelScope.launch {
            repository.getMessagesForConversation(currentId, otherId)
                .collectLatest { list -> _messages.value = list }
        }
    }

    fun sendMessage(text: String) {
        val sId = currentUserId ?: return
        val sEmail = currentUserEmail ?: return
        val rId = otherUserId ?: return
        val rEmail = otherUserEmail ?: return
        viewModelScope.launch {
            repository.sendMessage(senderId = sId,
                senderEmail = sEmail,
                receiverId = rId,
                receiverEmail = rEmail,
                text = text)
        }
    }


}
