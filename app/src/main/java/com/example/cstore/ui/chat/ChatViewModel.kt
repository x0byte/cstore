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

    init {
        viewModelScope.launch {
            repository.getMessages().collectLatest { list ->
                _messages.value = list
            }
        }
    }

    fun sendMessage(sender: String, text: String) {
        viewModelScope.launch {
            repository.sendMessage(sender, text)
        }
    }
}
