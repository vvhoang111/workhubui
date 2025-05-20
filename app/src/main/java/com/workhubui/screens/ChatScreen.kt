package com.workhubui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle

@Composable
fun ChatScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Chat", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("Search") })
        Spacer(Modifier.height(16.dp))
        ChatItem("Alice", "Sure!", "9:40 AM")
        ChatItem("Bob", "I updated the doc", "Today")
        ChatItem("Carol", "Great, thanks!", "Yesterday")
        ChatItem("Dave", "Hello!", "")
    }
}

@Composable
fun ChatItem(name: String, message: String, time: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = name, modifier = Modifier.size(40.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge)
            Text(message, color = Color.Gray, fontSize = 14.sp)
        }
        if (time.isNotEmpty()) Text(time, fontSize = 12.sp, color = Color.Gray)
    }
}