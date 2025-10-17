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
fun ChatScreen(
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    otherUserId: String,
    listingId: String? = null
) {
    val currentUserId = authViewModel.currentUserUid() ?: return
    val currentUserEmail = authViewModel.currentUserEmail().orEmpty()
    val messages by chatViewModel.messages.collectAsState()
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var otherEmail by remember { mutableStateOf<String?>(null) }
    var listingTitle by remember { mutableStateOf<String?>(null) }
    var showExpressInterest by remember { mutableStateOf(false) }

    // Fetch user email
    LaunchedEffect(otherUserId) {
        otherEmail = fetchUserEmail(otherUserId)
    }
    
    // Fetch listing details if provided
    LaunchedEffect(listingId) {
        if (listingId != null) {
            listingTitle = fetchListingTitle(listingId)
            // Show "Express Interest" button only when first opening chat from item
            showExpressInterest = messages.isEmpty()
        }
    }

    LaunchedEffect(currentUserId, currentUserEmail, otherUserId, otherEmail) {
        val oe = otherEmail ?: return@LaunchedEffect
        chatViewModel.loadConversation(
            currentId = currentUserId,
            currentEmail = currentUserEmail,
            otherId = otherUserId,
            otherEmail = oe,
            listingId = listingId,
            listingTitle = listingTitle
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = otherEmail ?: otherUserId,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (listingTitle != null) {
                            Text(
                                text = "About: $listingTitle",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Express Interest button
            if (showExpressInterest && listingTitle != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Quick Actions",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                chatViewModel.sendMessage("Hey! I'm interested in \"$listingTitle\". Is it still available?")
                                showExpressInterest = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("💬 Express Interest")
                        }
                    }
                }
            }
            
            ChatMessagesList(
                messages = messages,
                currentUserId = currentUserId,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
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
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    
    val shape = if (isUser) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 4.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 4.dp,
            bottomEnd = 18.dp
        )
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            modifier = Modifier
                .padding(4.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
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

suspend fun fetchListingTitle(listingId: String): String? {
    return try {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val snap = db.collection("listings").document(listingId).get().await()
        snap.getString("title")
    } catch (e: Exception) {
        null
    }
}

private fun String?.orElse(fallback: String) = this ?: fallback