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

data class ChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val isUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage("1", "Test", "Hey there!", false)
            )
        )
    }

    var inputText by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conversations") }
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSendClick = {
                    if (inputText.text.isNotBlank()) {
                        messages = messages + ChatMessage(
                            id = (messages.size + 1).toString(),
                            sender = "You",
                            text = inputText.text,
                            isUser = true
                        )
                        inputText = TextFieldValue("")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        ChatMessagesList(
            messages = messages,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
    }
}

@Composable
fun ChatMessagesList(messages: List<ChatMessage>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = true // To show the latest message at the bottom
    ) {
        items(messages.reversed()) { message ->
            ChatBubble(message = message)
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val bubbleColor =
        if (message.isUser) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    val alignment =
        if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart

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
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isUser)
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

