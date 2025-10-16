package com.example.cstore.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cstore.ui.auth.AuthViewModel
import com.example.cstore.data.chat.ChatSummary
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatSelected: (String) -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    chatListViewModel: ChatListViewModel = viewModel()
) {
    val currentUserId = authViewModel.currentUserUid() ?: return
    val chatList by chatListViewModel.chatList.collectAsState()

    LaunchedEffect(Unit) {
        chatListViewModel.loadChatList(currentUserId)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Chats") }) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(8.dp)
        ) {
            items(chatList) { chat ->
                ChatListItem(chat, currentUserId, onChatSelected)
            }
        }
    }
}

@Composable
fun ChatListItem(chat: ChatSummary, currentUserId: String, onChatSelected: (String) -> Unit) {
    val otherUserId = chat.participants.firstOrNull { it != currentUserId } ?: "Unknown"
    val formattedTime = remember(chat.lastTimestamp) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(chat.lastTimestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onChatSelected(otherUserId) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "Chat with: $otherUserId", style = MaterialTheme.typography.titleMedium)
            Text(text = chat.lastMessage, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
