package com.workhubui.screens.chat

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.workhubui.model.ChatMessage // Đảm bảo import đúng

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavHostController, currentUser: String, chatWith: String) {
    val application = LocalContext.current.applicationContext as Application
    val vm: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(application, currentUser, chatWith) // Truyền currentUser và chatWith
    )

    val messages by vm.messages.collectAsState()
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Chat with $chatWith") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message") }
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (text.isNotBlank()) {
                        vm.sendMessage(text) // Không cần truyền currentUser và chatWith nữa
                        text = ""
                    }
                }) {
                    Text("Send")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 8.dp), // Thêm padding ngang cho đẹp hơn
            reverseLayout = true // Để tin nhắn mới nhất ở dưới cùng và tự cuộn xuống
        ) {
            items(messages.reversed()) { msg -> // Đảo ngược danh sách để hiển thị đúng thứ tự với reverseLayout
                MessageRow(msg, currentUser)
            }
        }
    }
}

@Composable
fun MessageRow(msg: ChatMessage, currentUser: String) {
    val isMe = msg.sender == currentUser
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp), // Điều chỉnh padding
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp // Thêm chút độ nổi cho tin nhắn
        ) {
            Text(
                text = msg.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), // Điều chỉnh padding
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}