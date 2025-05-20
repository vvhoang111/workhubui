package com.workhubui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.ui.Alignment

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Good evening", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Today's Schedule", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("9:00 AM - Design Review")
        Text("11:00 AM - Client Presentation")
        Text("2:00 PM - Team Meeting")
        Spacer(modifier = Modifier.height(16.dp))
        Text("Recent Chats", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        ChatRow(name = "Alice", message = "Are you free?", time = "")
        ChatRow(name = "Bob", message = "I'll update the doc", time = "")
        ChatRow(name = "Emma", message = "See you tomorrow!", time = "3h")
    }
}

@Composable
fun ChatRow(name: String, message: String, time: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.AccountCircle, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(name)
            Text(message)
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(time)
    }
}
