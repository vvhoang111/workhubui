package com.cdcs.screens.chat

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.cdcs.model.ChatMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
// << THAY ĐỔI: Nhận UID thay vì email >>
fun ChatScreen(navController: NavHostController, currentUserUid: String, friendUid: String) {
    val application = LocalContext.current.applicationContext as Application
    // << THAY ĐỔI: Sử dụng Factory mới với UID >>
    val vm: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(application, currentUserUid, friendUid)
    )

    val messages by vm.messages.collectAsState()
    var text by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                // Có thể hiển thị tên bạn bè ở đây thay vì UID
                title = { Text(text = "Chat") },
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
                    placeholder = { Text("Type an encrypted message") }
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (text.isNotBlank()) {
                        vm.sendMessage(text)
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
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { msg ->
                MessageRow(msg, currentUserUid)
            }
        }
    }
}

@Composable
fun MessageRow(msg: ChatMessage, currentUserUid: String) {
    val isMe = msg.sender == currentUserUid
    // ... phần còn lại của MessageRow giữ nguyên
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp
        ) {
            Text(
                text = msg.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}