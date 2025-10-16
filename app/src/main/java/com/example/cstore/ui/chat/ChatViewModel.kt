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
    private var otherUserId: String? = null

    fun loadConversation(currentUser: String, otherUser: String) {
        currentUserId = currentUser
        otherUserId = otherUser

        viewModelScope.launch {
            repository.getMessagesForConversation(currentUser, otherUser)
                .collectLatest { list -> _messages.value = list }
        }
    }

    fun sendMessage(text: String) {
        val sender = currentUserId ?: return
        val receiver = otherUserId ?: return
        viewModelScope.launch {
            repository.sendMessage(sender, receiver, text)
        }
    }
}
