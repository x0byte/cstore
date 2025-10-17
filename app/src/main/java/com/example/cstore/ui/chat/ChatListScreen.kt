package com.example.cstore.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    onChatSelected: (otherUserId: String, otherEmail: String) -> Unit, //add email
    authViewModel: AuthViewModel = viewModel(),
    chatListViewModel: ChatListViewModel = viewModel()
) {
    val currentUserId = authViewModel.currentUserUid() ?: return
    val currentUserEmail = authViewModel.currentUserEmail().orEmpty()
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
                ChatListItem(chat = chat,
                    currentUserId = currentUserId,
                    currentUserEmail = currentUserEmail,
                    onChatSelected = onChatSelected)
            }
        }
    }
}

@Composable
fun ChatListItem(chat: ChatSummary, currentUserId: String, currentUserEmail: String, onChatSelected: (String, String) -> Unit) {
    val otherUserId = chat.participants.firstOrNull { it != currentUserId } ?: "Unknown"
    val otherEmail = chat.participantEmails.firstOrNull { it != currentUserEmail } ?: "Unknown"

    val formattedTime = remember(chat.lastTimestamp) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(chat.lastTimestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onChatSelected(otherUserId, otherEmail) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Show item title if available
                if (!chat.listingTitle.isNullOrBlank()) {
                    Text(
                        text = chat.listingTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                
                Text(
                    text = otherEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
