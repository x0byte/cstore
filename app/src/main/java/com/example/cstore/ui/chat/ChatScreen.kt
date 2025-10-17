package com.example.cstore.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.cstore.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cstore.ui.auth.AuthViewModel
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(modifier: Modifier = Modifier,
               chatViewModel: ChatViewModel = viewModel(),
               authViewModel: AuthViewModel = viewModel(),
               otherUserId: String
) {
    val currentUserId = authViewModel.currentUserUid() ?: return
    val currentUserEmail = authViewModel.currentUserEmail().orEmpty()
    val messages by chatViewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var otherEmail by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(otherUserId) {
        otherEmail = fetchUserEmail(otherUserId)
    }

    LaunchedEffect(currentUserId, currentUserEmail, otherUserId, otherEmail) {
        val oe = otherEmail ?: return@LaunchedEffect
        chatViewModel.loadConversation(
            currentId = currentUserId,
            currentEmail = currentUserEmail,
            otherId = otherUserId,
            otherEmail = oe
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(otherEmail ?: otherUserId)  }
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSendClick = {
                    if (inputText.text.isNotBlank()) {
                        chatViewModel.sendMessage(inputText.text)
                        inputText = TextFieldValue("")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        ChatMessagesList(
            messages = messages,
            currentUserId = currentUserId,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

@Composable
fun ChatMessagesList(messages: List<com.example.cstore.data.chat.ChatMessage>, currentUserId: String, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = true // To show the latest message at the bottom
    ) {
        items(messages) { message ->
            ChatBubble(
                text = message.text,
                isUser = message.senderId == currentUserId
            )
        }
    }
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    val bubbleColor =
        if (isUser) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    val alignment =
        if (isUser) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bubbleColor,
            tonalElevation = 2.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .padding(horizontal = 4.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun ChatInputBar(
    text: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Type a message...") },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            maxLines = 3
        )
        IconButton(onClick = onSendClick) {
            Icon(Icons.Default.Send, contentDescription = "Send")
        }
    }
}

suspend fun fetchUserEmail(uid: String): String {
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val snap = db.collection("users").document(uid).get().await()
    return snap.getString("email").orElse("")
}

private fun String?.orElse(fallback: String) = this ?: fallback