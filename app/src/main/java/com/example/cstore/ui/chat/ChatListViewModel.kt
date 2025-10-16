package com.example.cstore.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cstore.data.chat.ChatRepository
import com.example.cstore.data.chat.ChatSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val repository: ChatRepository = ChatRepository()
) : ViewModel() {

    private val _chatList = MutableStateFlow<List<ChatSummary>>(emptyList())
    val chatList: StateFlow<List<ChatSummary>> = _chatList

    fun loadChatList(currentUserId: String) {
        viewModelScope.launch {
            repository.getChatList(currentUserId).collectLatest { chats ->
                _chatList.value = chats
            }
        }
    }
}
